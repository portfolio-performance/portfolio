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
ImportNotes = Annotated[bool | None, Field(description="Keep the notes the extractor generated")]
Select = Annotated[
    list[int] | None,
    Field(description="Indexes of the items to import; omit for every item without an error status"),
]


def _status_is_error(status: Any) -> bool:
    if isinstance(status, dict):
        status = status.get("status") or status.get("severity") or status.get("code")
    return isinstance(status, str) and status.lower() == "error"


def importable(item: dict[str, Any]) -> bool:
    """True unless the item or one of its checks has status `error`."""
    if _status_is_error(item.get("status")):
        return False
    return not any(_status_is_error(check) for check in item.get("checks") or [])


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
    body["targets"] = targets
    body["options"] = options
    return body


async def _preview_or_commit(pp, f: str, preview: dict[str, Any], targets, options, dry_run: bool) -> dict[str, Any]:
    items = preview.get("items", [])
    if dry_run:
        result = out(preview)
        result["note"] = (
            "Preview only, nothing was imported. Review the items and their check statuses, then call "
            f"commit_import(import_id={preview.get('importId')!r}, select=[...]) with the target accounts."
        )
        return result
    select = [item["index"] for item in items if importable(item)]
    if not select:
        result = out(preview)
        result["note"] = "No importable items (all have an error status); nothing was imported."
        return result
    committed = await pp.post(
        fpath(f, "imports", str(preview["importId"]), "commit"), body=_commit_body(select, targets, options)
    )
    return out({"preview": preview, "commit": committed})


@mcp.tool(annotations=WRITE)
async def import_pdf(
    paths: Annotated[list[str], Field(description="Paths of broker PDF documents on this machine")],
    file: FileParam = None,
    cash_account: CashAccount = None,
    investment_account: InvestmentAccount = None,
    accounts_by_currency: AccountsByCurrency = None,
    secondary_cash_account: SecondaryCash = None,
    secondary_investment_account: SecondaryInvestment = None,
    convert_buy_sell_to_delivery: ConvertToDelivery = None,
    remove_dividends: RemoveDividends = None,
    import_notes: ImportNotes = None,
    dry_run: Annotated[bool, Field(description="Only extract and preview (default); false imports directly")] = True,
) -> dict[str, Any]:
    """Extract transactions from broker PDF documents with PP's PDF importers.

    With the default `dry_run=true` this returns the preview `{importId, items}`: each
    item has an `index`, its kind, the resolved transaction or security and the
    status of PP's checks (ok, warning, error, e.g. a likely duplicate). Commit with
    commit_import. With `dry_run=false` every item without an error status is
    imported into the given accounts right away. Imported data changes the in-memory
    file only until save_file.
    """
    if not paths:
        raise ToolError("paths: pass at least one PDF path")
    body = {"paths": [os.path.abspath(p) for p in paths]}
    targets = _targets(cash_account, accounts_by_currency, investment_account, secondary_cash_account,
                       secondary_investment_account)
    options = _options(convert_buy_sell_to_delivery, remove_dividends, import_notes)
    async with api() as pp:
        f = await pp.resolve_file(file)
        preview = await pp.post(fpath(f, "imports", "pdf"), body=body)
        return await _preview_or_commit(pp, f, preview, targets, options, dry_run)


@mcp.tool(annotations=WRITE)
async def import_csv(
    path: Annotated[str, Field(description="Path of the CSV file on this machine")],
    config: Annotated[
        dict[str, Any],
        Field(description="PP CSV import configuration (CSVConfig JSON: extractor, delimiter, encoding, "
              "skipLines, isFirstLineHeader, columns with field mapping and formats)"),
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
    dry_run: Annotated[bool, Field(description="Only extract and preview (default); false imports directly")] = True,
) -> dict[str, Any]:
    """Import a CSV file with an explicit column mapping (the same configuration PP's
    CSV import wizard saves). Two steps like import_pdf: the default `dry_run=true`
    returns the preview `{importId, items}`; commit with commit_import, or pass
    `dry_run=false` to import every item without an error status directly. Parse
    errors are reported with line numbers. Imported data changes the in-memory file
    only until save_file."""
    body = {"path": os.path.abspath(path), "config": no_floats(config, "config")}
    targets = _targets(cash_account, accounts_by_currency, investment_account, secondary_cash_account,
                       secondary_investment_account, instrument)
    options = _options(convert_buy_sell_to_delivery, remove_dividends, import_notes)
    async with api() as pp:
        f = await pp.resolve_file(file)
        preview = await pp.post(fpath(f, "imports", "csv"), body=body)
        return await _preview_or_commit(pp, f, preview, targets, options, dry_run)


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
    """Import previewed items into the chosen accounts. Items with an error status are
    refused (`item-not-importable`) when selected explicitly. Previews expire after
    15 minutes and are consumed by a successful commit. Imported data changes the
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
