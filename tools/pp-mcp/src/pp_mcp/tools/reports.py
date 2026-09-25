"""Calculations and reports (read only): holdings, performance, trades, earnings."""

from typing import Annotated, Any, Literal

from pydantic import Field

from pp_mcp.app import READ_ONLY, mcp
from pp_mcp.money import date_str
from pp_mcp.tools._common import CashAccountFilter, FileParam, InvestmentAccountFilter, api, fpath, out, uuid_list

CostMethod = Annotated[
    Literal["fifo", "moving-average"] | None, Field(description="Cost basis method (default fifo)")
]
Currency = Annotated[str | None, Field(description="Reporting currency, ISO 4217 (default: the file's base currency)")]
InvestmentAccount = InvestmentAccountFilter
CashAccount = CashAccountFilter
OpeningDate = Annotated[
    str, Field(description="Opening valuation date YYYY-MM-DD; activity on this day belongs to the opening balance")
]
ClosingDate = Annotated[str | None, Field(description="Closing valuation date YYYY-MM-DD (default today)")]


@mcp.tool(annotations=READ_ONLY)
async def get_holdings(
    file: FileParam = None,
    date: Annotated[str | None, Field(description="Valuation date YYYY-MM-DD (default today)")] = None,
    opening_date: Annotated[
        str | None, Field(description="Start of the period for cost/return metrics (default: since inception)")
    ] = None,
    currency: Currency = None,
    cost_method: CostMethod = None,
    investment_account: InvestmentAccount = None,
    cash_account: CashAccount = None,
) -> dict[str, Any]:
    """Statement of assets: every position and cash balance valued at `date` in the
    reporting currency, plus per-position cost basis, gains, returns and dividends
    over (opening_date, date]. Money values are `{value, currency}` with decimal
    strings (2 decimals); shares up to 8 decimals; returns are fractions
    (0.05 = 5 %). An investment-account filter brings its reference cash account's
    security transactions along (a buy is then an inbound delivery unless the cash
    account is selected as well)."""
    params = {
        "date": date_str(date, "date"),
        "openingDate": date_str(opening_date, "opening_date"),
        "currency": currency,
        "costMethod": cost_method,
        "investmentAccount": uuid_list(investment_account),
        "cashAccount": uuid_list(cash_account),
    }
    async with api() as pp:
        f = await pp.resolve_file(file)
        return out(await pp.get(fpath(f, "holdings"), params=params))


@mcp.tool(annotations=READ_ONLY)
async def get_performance(
    from_date: OpeningDate,
    file: FileParam = None,
    to_date: ClosingDate = None,
    currency: Currency = None,
    cost_method: CostMethod = None,
    include_series: Annotated[bool, Field(description="Add the daily index series and risk metrics")] = False,
    include_calendar: Annotated[bool, Field(description="Add per-month returns and cash flows")] = False,
    investment_account: InvestmentAccount = None,
    cash_account: CashAccount = None,
) -> dict[str, Any]:
    """Performance between two valuation dates: TTWROR, IRR (fractions, 0.05 = 5 %) and
    the value-change breakdown (opening value, capital gains, income, fees, taxes,
    currency gains, net deposits, closing value; decimal strings, 2 decimals).
    Result: `{performance, series?, calendar?}`."""
    base = {
        "openingDate": date_str(from_date, "from_date"),
        "closingDate": date_str(to_date, "to_date"),
        "currency": currency,
        "investmentAccount": uuid_list(investment_account),
        "cashAccount": uuid_list(cash_account),
    }
    async with api() as pp:
        f = await pp.resolve_file(file)
        result = {"performance": await pp.get(fpath(f, "performance"), params=base | {"costMethod": cost_method})}
        if include_series:
            result["series"] = await pp.get(fpath(f, "performance", "series"), params=base)
        if include_calendar:
            result["calendar"] = await pp.get(fpath(f, "performance", "calendar"), params=base)
    return out(result)


@mcp.tool(annotations=READ_ONLY)
async def get_security_performance(
    from_date: OpeningDate,
    file: FileParam = None,
    to_date: ClosingDate = None,
    currency: Currency = None,
    cost_method: CostMethod = None,
    investment_account: InvestmentAccount = None,
    cash_account: CashAccount = None,
) -> dict[str, Any]:
    """Per-instrument performance between two valuation dates (returns as fractions,
    money as decimal strings with 2 decimals)."""
    params = {
        "openingDate": date_str(from_date, "from_date"),
        "closingDate": date_str(to_date, "to_date"),
        "currency": currency,
        "costMethod": cost_method,
        "investmentAccount": uuid_list(investment_account),
        "cashAccount": uuid_list(cash_account),
    }
    async with api() as pp:
        f = await pp.resolve_file(file)
        return out(await pp.get(fpath(f, "performance", "securities"), params=params))


@mcp.tool(annotations=READ_ONLY)
async def get_trades(
    file: FileParam = None,
    currency: Currency = None,
    only_closed: Annotated[bool, Field(description="Only completed round trips")] = False,
) -> dict[str, Any]:
    """Buy/sell round trips with entry/exit value, profit/loss, holding period and IRR.
    Open trades are valued at today's price. The trade list cannot be filtered by
    account."""
    params = {"currency": currency, "onlyClosed": True if only_closed else None}
    async with api() as pp:
        f = await pp.resolve_file(file)
        return out(await pp.get(fpath(f, "trades"), params=params))


@mcp.tool(annotations=READ_ONLY)
async def get_earnings(
    file: FileParam = None,
    from_date: Annotated[str | None, Field(description="First date YYYY-MM-DD, inclusive")] = None,
    to_date: Annotated[str | None, Field(description="Last date YYYY-MM-DD, inclusive")] = None,
    instrument: Annotated[str | None, Field(description="Instrument UUID")] = None,
    cash_account: Annotated[str | None, Field(description="Only bookings on this cash account (UUID)")] = None,
    investment_account: Annotated[
        str | None, Field(description="Only earnings of this investment account's instruments (UUID)")
    ] = None,
) -> dict[str, Any]:
    """Dividends, interest and interest charges in [from_date, to_date]:
    `{items: [transactions], totals: [{currency, value, dividends, interest,
    interestCharge, taxes, fees, count}]}`. Totals are per booking currency, not
    converted; `value` = dividends + interest − interestCharge (decimal strings)."""
    params = {
        "from": date_str(from_date, "from_date"),
        "to": date_str(to_date, "to_date"),
        "instrument": instrument,
        "cashAccount": cash_account,
        "investmentAccount": investment_account,
    }
    async with api() as pp:
        f = await pp.resolve_file(file)
        return out(await pp.get(fpath(f, "earnings"), params=params))
