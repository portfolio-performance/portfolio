"""PDF and CSV import: extract (preview) first, then commit selected items."""

import os
from typing import Annotated, Any

from fastmcp.exceptions import ToolError
from pydantic import Field

from pp_mcp.app import WRITE, mcp
from pp_mcp.tools._common import DryRun, FileParam, api, compact, dry, fpath, no_floats, out

CashAccount = Annotated[str | None, Field(description="Cash account UUID for items without their own account")]
InvestmentAccount = Annotated[str | None, Field(description="Investment account UUID for security items")]
AccountsByCurrency = Annotated[
    dict[str, str] | None,
    Field(description="Cash account UUID per currency, e.g. {'EUR': '<uuid>', 'USD': '<uuid>'}"),
]
SecondaryCash = Annotated[str | None, Field(description="Target cash account UUID of transfers")]
SecondaryInvestment = Annotated[str | None, Field(description="Target investment account UUID of transfers")]
ConvertToDelivery = Annotated[bool | None, Field(description="Import buys/sells as deliveries (no cash leg)")]
RemoveDividends = Annotated[bool | None, Field(description="Skip dividend items")]
ImportNotes = Annotated[bool | None, Field(description="Keep the notes the extractor generated (default true)")]
IncludeWarnings = Annotated[
    bool,
    Field(
        description="With dry_run=false: also import items with status warning (e.g. a probable duplicate); "
        "by default only items with status ok are imported"
    ),
]
Select = Annotated[
    list[int] | None,
    Field(
        description="Indexes of the items to import (ok or warning items); omit for PP's preselection: the "
        "items with status ok (and investment plan items with a warning)"
    ),
]

_PREVIEW_DOC = """The preview is `{importId, kind, expiresAt, items, counts: {ok, warning,
error}, errors}`: each item has an `index`, a `kind` (buy-sell, transaction,
cash-transfer, security-transfer, instrument, instrument-update, price, skipped), a
`status` (ok, warning e.g. a probable duplicate, or error = not importable), its
`checks` [{code, status, message?}] and what it would book: the `transaction` with the
accounts (`cashAccount`, `investmentAccount`, ... as {uuid, name}) or the
`instrument`. The checks run with the given target accounts or, where none is given,
with the accounts PP's import wizard preselects."""


_PDF_DOC = f"""Extract transactions from broker PDF documents with PP's PDF importers.

With the default `dry_run=true` this returns the preview; commit it with commit_import.
Documents no importer recognizes are listed in `errors`. With `dry_run=false` the items
with status ok (plus warning items with `include_warnings`) are imported into the given
accounts right away. Imported data changes the in-memory file only until save_file.

{_PREVIEW_DOC}"""

_CSV_DOC = f"""Import a CSV file with an explicit column mapping (the same configuration
PP's CSV import wizard saves). Two steps like import_pdf: the default `dry_run=true`
returns the preview; commit with commit_import, or pass `dry_run=false` to import the
items with status ok (plus warning items with `include_warnings`) directly. A line that
cannot be read fails the request with `csv-parse-error`, one error per line
(`line[N]`). A price CSV (target investment-vehicle-price) yields one `price` item per
line and needs `instrument` on commit. Imported data changes the in-memory file only
until save_file.

{_PREVIEW_DOC}"""


def importable(item: dict[str, Any], include_warnings: bool = False) -> bool:
    """True for an item with status `ok` (or `warning`, when included)."""
    status = str(item.get("status") or "").lower()
    return status == "ok" or (include_warnings and status == "warning")


def _targets(cash_account, accounts_by_currency, investment_account, secondary_cash_account,
             secondary_investment_account, instrument=None) -> dict[str, Any]:
    return compact(
        cashAccount=cash_account,
        cashAccountsByCurrency=accounts_by_currency,
        investmentAccount=investment_account,
        secondaryCashAccount=secondary_cash_account,
        secondaryInvestmentAccount=secondary_investment_account,
        instrument=instrument,
    )


def _options(convert_buy_sell_to_delivery, remove_dividends, import_notes) -> dict[str, Any]:
    return compact(
        convertBuySellToDelivery=convert_buy_sell_to_delivery,
        removeDividends=remove_dividends,
        importNotes=import_notes,
    )


def _commit_body(select, targets, options) -> dict[str, Any]:
    body: dict[str, Any] = {}
    if select is not None:
        body["select"] = select
    if targets:
        body["targets"] = targets
    if options:
        body["options"] = options
    return body


async def _extract(pp, f: str, kind: str, body: dict[str, Any], targets: dict[str, Any]) -> dict[str, Any]:
    if targets:
        body = body | {"targets": targets}
    return await pp.post(fpath(f, "imports", kind), body=body)


async def _preview_or_commit(pp, f: str, preview: dict[str, Any], targets, options, dry_run: bool,
                             include_warnings: bool) -> dict[str, Any]:
    if dry_run:
        result = out(preview)
        result["note"] = (
            "Preview only, nothing was imported. Review the items and their check statuses, then call "
            f"commit_import(import_id={preview.get('importId')!r}, select=[...]) with the target accounts."
        )
        return result
    select = [item["index"] for item in preview.get("items", []) if importable(item, include_warnings)]
    if not select:
        result = out(preview)
        wanted = "ok or warning" if include_warnings else "ok"
        result["note"] = f"No items with status {wanted}; nothing was imported."
        return result
    committed = await pp.post(
        fpath(f, "imports", str(preview["importId"]), "commit"), body=_commit_body(select, targets, options)
    )
    return out({"preview": preview, "commit": committed})


async def _import(kind: str, body: dict[str, Any], file, targets, options, dry_run: bool,
                  include_warnings: bool) -> dict[str, Any]:
    if kind == "pdf" and not body["paths"]:
        raise ToolError("paths: pass at least one PDF path")
    async with api() as pp:
        f = await pp.resolve_file(file)
        preview = await _extract(pp, f, kind, body, targets)
        return await _preview_or_commit(pp, f, preview, targets, options, dry_run, include_warnings)


@mcp.tool(annotations=WRITE, description=_PDF_DOC)
async def import_pdf(
    paths: Annotated[list[str], Field(description="Paths of broker PDF documents on this machine (at most 100)")],
    file: FileParam = None,
    cash_account: CashAccount = None,
    investment_account: InvestmentAccount = None,
    accounts_by_currency: AccountsByCurrency = None,
    secondary_cash_account: SecondaryCash = None,
    secondary_investment_account: SecondaryInvestment = None,
    convert_buy_sell_to_delivery: ConvertToDelivery = None,
    remove_dividends: RemoveDividends = None,
    import_notes: ImportNotes = None,
    include_warnings: IncludeWarnings = False,
    dry_run: Annotated[bool, Field(description="Only extract and preview (default); false imports directly")] = True,
) -> dict[str, Any]:
    return await _import(
        "pdf", {"paths": [os.path.abspath(p) for p in paths]}, file,
        _targets(cash_account, accounts_by_currency, investment_account, secondary_cash_account,
                 secondary_investment_account),
        _options(convert_buy_sell_to_delivery, remove_dividends, import_notes), dry_run, include_warnings,
    )


@mcp.tool(annotations=WRITE, description=_CSV_DOC)
async def import_csv(
    path: Annotated[str, Field(description="Path of the CSV file on this machine")],
    config: Annotated[
        dict[str, Any],
        Field(description="PP CSV import configuration as the wizard saves it: target (account-transaction, "
              "portfolio-transaction, investment-vehicle, investment-vehicle-price, portfolio), delimiter "
              "(',', ';' or tab), encoding (default UTF-8), skipLines, isFirstLineHeader, and columns "
              "[{label, field, format}] in file order"),
    ],
    file: FileParam = None,
    cash_account: CashAccount = None,
    investment_account: InvestmentAccount = None,
    accounts_by_currency: AccountsByCurrency = None,
    secondary_cash_account: SecondaryCash = None,
    secondary_investment_account: SecondaryInvestment = None,
    instrument: Annotated[str | None, Field(description="Instrument UUID, required for a price CSV")] = None,
    convert_buy_sell_to_delivery: ConvertToDelivery = None,
    remove_dividends: RemoveDividends = None,
    import_notes: ImportNotes = None,
    include_warnings: IncludeWarnings = False,
    dry_run: Annotated[bool, Field(description="Only extract and preview (default); false imports directly")] = True,
) -> dict[str, Any]:
    return await _import(
        "csv", {"path": os.path.abspath(path), "config": no_floats(config, "config")}, file,
        _targets(cash_account, accounts_by_currency, investment_account, secondary_cash_account,
                 secondary_investment_account, instrument),
        _options(convert_buy_sell_to_delivery, remove_dividends, import_notes), dry_run, include_warnings,
    )


@mcp.tool(annotations=WRITE)
async def commit_import(
    import_id: Annotated[str, Field(description="importId returned by import_pdf or import_csv")],
    file: FileParam = None,
    select: Select = None,
    cash_account: CashAccount = None,
    investment_account: InvestmentAccount = None,
    accounts_by_currency: AccountsByCurrency = None,
    secondary_cash_account: SecondaryCash = None,
    secondary_investment_account: SecondaryInvestment = None,
    instrument: Annotated[str | None, Field(description="Instrument UUID, required for a price CSV")] = None,
    convert_buy_sell_to_delivery: ConvertToDelivery = None,
    remove_dividends: RemoveDividends = None,
    import_notes: ImportNotes = None,
    dry_run: DryRun = False,
) -> dict[str, Any]:
    """Import previewed items into the chosen accounts; PP re-runs the checks with these
    targets. Items with status error are refused (`item-not-importable`) when selected.
    Returns `{importId, count, items: [{index, kind}], transactions: [new transactions],
    instruments: [{uuid, name} of new instruments]}`; a dry run returns `{dryRun: true,
    importId, count, items: [preview items]}` and keeps the preview. Previews expire
    after 15 minutes and are consumed by a successful commit. Imported data changes the
    in-memory file only until save_file."""
    targets = _targets(cash_account, accounts_by_currency, investment_account, secondary_cash_account,
                       secondary_investment_account, instrument)
    options = _options(convert_buy_sell_to_delivery, remove_dividends, import_notes)
    async with api() as pp:
        f = await pp.resolve_file(file)
        return out(
            await pp.post(
                fpath(f, "imports", import_id, "commit"),
                params=dry(dry_run),
                body=_commit_body(select, targets, options),
            )
        )
