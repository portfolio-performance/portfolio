"""Cash accounts (PP "accounts") and investment accounts (PP "portfolios")."""

from typing import Annotated, Any

from fastmcp.exceptions import ToolError
from pydantic import Field

from pp_mcp.app import DESTRUCTIVE, READ_ONLY, WRITE, mcp
from pp_mcp.money import date_str
from pp_mcp.tools._common import (
    Clear,
    ClientRef,
    DryRun,
    FileParam,
    api,
    apply_clear,
    compact,
    deleted,
    dry,
    fpath,
    no_floats,
    out,
)

Uuid = Annotated[str, Field(description="Account UUID")]
Attributes = Annotated[
    dict[str, Any] | None,
    Field(description="Custom attributes keyed by attribute id; numbers as decimal strings; null clears one on update"),
]
AllowDuplicate = Annotated[
    bool, Field(description="Create even if an account with the same name (and currency) already exists")
]


async def _existing(pp, f: str, collection: str, name: str, currency: str | None = None):
    listed = await pp.get(fpath(f, collection))
    wanted = name.strip().casefold()
    for item in listed.get("items", []):
        if (item.get("name") or "").strip().casefold() != wanted:
            continue
        if currency is None or item.get("currencyCode") == currency:
            return item
    return None


def _duplicate_result(existing: dict[str, Any], what: str) -> dict[str, Any]:
    return out(
        {
            "created": False,
            "account": existing,
            "note": f"{what} with this name already exists; returned it instead of creating a duplicate. "
            "Pass allow_duplicate=true to create another one.",
        }
    )


def _created(result: Any, dry_run: bool) -> dict[str, Any]:
    replayed = isinstance(result, dict) and bool(result.get("replayed"))
    return out({"created": not dry_run and not replayed, "account": result})


@mcp.tool(annotations=READ_ONLY)
async def list_accounts(
    file: FileParam = None,
    date: Annotated[str | None, Field(description="Valuation date YYYY-MM-DD (default today)")] = None,
    currency: Annotated[str | None, Field(description="Reporting currency for investment account values")] = None,
) -> dict[str, Any]:
    """List cash accounts and investment accounts.

    Result: `{cashAccounts: [...], investmentAccounts: [...]}`. A cash account's
    `balance` is in its own currency; an investment account's `value` is the market
    value of its positions at `date` in `currency` (default: the file's base
    currency). Amounts are decimal strings with 2 decimals.
    """
    date_str(date, "date")
    async with api() as pp:
        f = await pp.resolve_file(file)
        cash = await pp.get(fpath(f, "cash-accounts"), params={"date": date})
        invest = await pp.get(fpath(f, "investment-accounts"), params={"date": date, "currency": currency})
    return out({"cashAccounts": cash.get("items", []), "investmentAccounts": invest.get("items", [])})


@mcp.tool(annotations=WRITE)
async def create_cash_account(
    name: str,
    currency: Annotated[str, Field(description="ISO 4217 currency code")],
    file: FileParam = None,
    note: str | None = None,
    attributes: Attributes = None,
    allow_duplicate: AllowDuplicate = False,
    client_ref: ClientRef = None,
    dry_run: DryRun = False,
) -> dict[str, Any]:
    """Create a cash account. Returns `{created, account, note?}`; an existing account
    with the same name and currency is returned instead of creating a duplicate
    (unless `allow_duplicate`). The change is in memory only until save_file."""
    async with api() as pp:
        f = await pp.resolve_file(file)
        if not allow_duplicate and (existing := await _existing(pp, f, "cash-accounts", name, currency)):
            return _duplicate_result(existing, "A cash account")
        body = compact(
            name=name,
            currencyCode=currency,
            note=note,
            attributes=no_floats(attributes, "attributes"),
            clientRef=client_ref,
        )
        return _created(await pp.post(fpath(f, "cash-accounts"), params=dry(dry_run), body=body), dry_run)


@mcp.tool(annotations=WRITE)
async def update_cash_account(
    uuid: Uuid,
    file: FileParam = None,
    name: str | None = None,
    currency: Annotated[str | None, Field(description="ISO 4217; refused once the account has transactions")] = None,
    note: str | None = None,
    retired: Annotated[bool | None, Field(description="Retire (true) or reactivate (false)")] = None,
    attributes: Attributes = None,
    clear: Clear = None,
    dry_run: DryRun = False,
) -> dict[str, Any]:
    """Update a cash account (merge patch; `clear` accepts `note`).
    The change is in memory only until save_file."""
    patch = compact(
        name=name, currencyCode=currency, note=note, retired=retired, attributes=no_floats(attributes, "attributes")
    )
    apply_clear(patch, clear, {"note": "note"})
    if not patch:
        raise ToolError("nothing to update: pass at least one field")
    async with api() as pp:
        f = await pp.resolve_file(file)
        return out(await pp.patch(fpath(f, "cash-accounts", uuid), params=dry(dry_run), body=patch))


@mcp.tool(annotations=DESTRUCTIVE)
async def delete_cash_account(uuid: Uuid, file: FileParam = None, dry_run: DryRun = False) -> dict[str, Any]:
    """Delete a cash account. Refused (`delete-blocked`) while it has transactions or
    is used by an investment plan or as reference account; retire it instead.
    The change is in memory only until save_file."""
    async with api() as pp:
        f = await pp.resolve_file(file)
        return deleted(await pp.delete(fpath(f, "cash-accounts", uuid), params=dry(dry_run)), uuid=uuid)


@mcp.tool(annotations=WRITE)
async def create_investment_account(
    name: str,
    reference_cash_account: Annotated[str, Field(description="UUID of the cash account that settles its trades")],
    file: FileParam = None,
    note: str | None = None,
    attributes: Attributes = None,
    allow_duplicate: AllowDuplicate = False,
    client_ref: ClientRef = None,
    dry_run: DryRun = False,
) -> dict[str, Any]:
    """Create an investment account (securities account). Returns
    `{created, account, note?}`; an existing investment account with the same name is
    returned instead of creating a duplicate (unless `allow_duplicate`).
    The change is in memory only until save_file."""
    async with api() as pp:
        f = await pp.resolve_file(file)
        if not allow_duplicate and (existing := await _existing(pp, f, "investment-accounts", name)):
            return _duplicate_result(existing, "An investment account")
        body = compact(
            name=name,
            referenceCashAccount=reference_cash_account,
            note=note,
            attributes=no_floats(attributes, "attributes"),
            clientRef=client_ref,
        )
        return _created(await pp.post(fpath(f, "investment-accounts"), params=dry(dry_run), body=body), dry_run)


@mcp.tool(annotations=WRITE)
async def update_investment_account(
    uuid: Uuid,
    file: FileParam = None,
    name: str | None = None,
    reference_cash_account: Annotated[str | None, Field(description="UUID of a non-retired cash account")] = None,
    note: str | None = None,
    retired: Annotated[bool | None, Field(description="Retire (true) or reactivate (false)")] = None,
    attributes: Attributes = None,
    clear: Clear = None,
    dry_run: DryRun = False,
) -> dict[str, Any]:
    """Update an investment account (merge patch; `clear` accepts `note`).
    The change is in memory only until save_file."""
    patch = compact(
        name=name,
        referenceCashAccount=reference_cash_account,
        note=note,
        retired=retired,
        attributes=no_floats(attributes, "attributes"),
    )
    apply_clear(patch, clear, {"note": "note"})
    if not patch:
        raise ToolError("nothing to update: pass at least one field")
    async with api() as pp:
        f = await pp.resolve_file(file)
        return out(await pp.patch(fpath(f, "investment-accounts", uuid), params=dry(dry_run), body=patch))


@mcp.tool(annotations=DESTRUCTIVE)
async def delete_investment_account(uuid: Uuid, file: FileParam = None, dry_run: DryRun = False) -> dict[str, Any]:
    """Delete an investment account. Refused (`delete-blocked`) while it has
    transactions or is used by an investment plan; retire it instead.
    The change is in memory only until save_file."""
    async with api() as pp:
        f = await pp.resolve_file(file)
        return deleted(await pp.delete(fpath(f, "investment-accounts", uuid), params=dry(dry_run)), uuid=uuid)
