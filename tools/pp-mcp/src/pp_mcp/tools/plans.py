"""Investment plans, addressed by name (PP plans have no UUID)."""

from typing import Annotated, Any, Literal

from fastmcp.exceptions import ToolError
from pydantic import Field

from pp_mcp.app import DESTRUCTIVE, READ_ONLY, WRITE, mcp
from pp_mcp.money import MONEY, date_str, decimal_str
from pp_mcp.tools._common import Clear, DryRun, FileParam, api, apply_clear, compact, deleted, dry, fpath, out

Name = Annotated[str, Field(description="Investment plan name (plans are identified by name)")]
Kind = Literal["purchase", "deposit", "removal", "interest"]
Money = Annotated[str | None, Field(description="Decimal string with at most 2 decimals")]
IntervalMonths = Annotated[int | None, Field(description="Every N months (1-99); exclusive with interval_weeks", ge=1, le=99)]
IntervalWeeks = Annotated[int | None, Field(description="Every N weeks; exclusive with interval_months", ge=1)]


def _plan_fields(kind, instrument, investment_account, cash_account, start, interval_months, interval_weeks,
                 amount, fees, taxes, auto_generate, note) -> dict[str, Any]:
    if interval_months is not None and interval_weeks is not None:
        raise ToolError("pass either interval_months or interval_weeks, not both")
    return compact(
        kind=kind,
        instrument=instrument,
        investmentAccount=investment_account,
        cashAccount=cash_account,
        start=date_str(start, "start"),
        intervalMonths=interval_months,
        intervalWeeks=interval_weeks,
        amount=decimal_str(amount, MONEY, "amount"),
        fees=decimal_str(fees, MONEY, "fees"),
        taxes=decimal_str(taxes, MONEY, "taxes"),
        autoGenerate=auto_generate,
        note=note,
    )


@mcp.tool(annotations=READ_ONLY)
async def list_investment_plans(file: FileParam = None) -> dict[str, Any]:
    """List the investment plans (savings plans) of a file, incl. their next due date."""
    async with api() as pp:
        f = await pp.resolve_file(file)
        return out(await pp.get(fpath(f, "investment-plans")))


@mcp.tool(annotations=WRITE)
async def create_investment_plan(
    name: Name,
    kind: Annotated[Kind, Field(description="purchase buys an instrument; deposit/removal/interest book cash")],
    start: Annotated[str, Field(description="First execution date YYYY-MM-DD")],
    amount: Annotated[str, Field(description="Amount per execution in the account currency, 2 decimals")],
    file: FileParam = None,
    instrument: Annotated[str | None, Field(description="Instrument UUID (purchase plans)")] = None,
    investment_account: Annotated[str | None, Field(description="Investment account UUID (purchase plans)")] = None,
    cash_account: Annotated[str | None, Field(description="Cash account UUID")] = None,
    interval_months: IntervalMonths = None,
    interval_weeks: IntervalWeeks = None,
    fees: Money = None,
    taxes: Money = None,
    auto_generate: Annotated[bool | None, Field(description="Let PP generate due transactions on file open")] = None,
    note: str | None = None,
    dry_run: DryRun = False,
) -> dict[str, Any]:
    """Create an investment plan. Default interval is monthly when neither interval is
    given. The change is in memory only until save_file."""
    body = {"name": name} | _plan_fields(kind, instrument, investment_account, cash_account, start,
                                         interval_months, interval_weeks, amount, fees, taxes, auto_generate, note)
    async with api() as pp:
        f = await pp.resolve_file(file)
        return out(await pp.post(fpath(f, "investment-plans"), params=dry(dry_run), body=body))


@mcp.tool(annotations=WRITE)
async def update_investment_plan(
    name: Name,
    file: FileParam = None,
    new_name: Annotated[str | None, Field(description="Rename the plan")] = None,
    kind: Kind | None = None,
    instrument: str | None = None,
    investment_account: str | None = None,
    cash_account: str | None = None,
    start: Annotated[str | None, Field(description="YYYY-MM-DD")] = None,
    interval_months: IntervalMonths = None,
    interval_weeks: IntervalWeeks = None,
    amount: Money = None,
    fees: Money = None,
    taxes: Money = None,
    auto_generate: bool | None = None,
    note: str | None = None,
    clear: Clear = None,
    dry_run: DryRun = False,
) -> dict[str, Any]:
    """Update an investment plan (merge patch; `clear` accepts `note`, `instrument`,
    `investment_account`, `cash_account`). The change is in memory only until save_file."""
    patch = compact(name=new_name) | _plan_fields(kind, instrument, investment_account, cash_account, start,
                                                  interval_months, interval_weeks, amount, fees, taxes,
                                                  auto_generate, note)
    apply_clear(
        patch,
        clear,
        {
            "note": "note",
            "instrument": "instrument",
            "investment_account": "investmentAccount",
            "cash_account": "cashAccount",
        },
    )
    if not patch:
        raise ToolError("nothing to update: pass at least one field")
    async with api() as pp:
        f = await pp.resolve_file(file)
        return out(await pp.patch(fpath(f, "investment-plans", name), params=dry(dry_run), body=patch))


@mcp.tool(annotations=DESTRUCTIVE)
async def delete_investment_plan(name: Name, file: FileParam = None, dry_run: DryRun = False) -> dict[str, Any]:
    """Delete an investment plan; transactions it generated stay.
    The change is in memory only until save_file."""
    async with api() as pp:
        f = await pp.resolve_file(file)
        return deleted(await pp.delete(fpath(f, "investment-plans", name), params=dry(dry_run)), name=name)


@mcp.tool(annotations=WRITE)
async def generate_plan_transactions(name: Name, file: FileParam = None, dry_run: DryRun = False) -> dict[str, Any]:
    """Generate the due transactions of an investment plan up to today. A dry run lists
    the due dates only. Fails with `missing-price` when an instrument has no price for
    a due date. The change is in memory only until save_file."""
    async with api() as pp:
        f = await pp.resolve_file(file)
        return out(await pp.post(fpath(f, "investment-plans", name, "actions", "generate"), params=dry(dry_run)))
