"""Transactions: list, get, create (task-oriented, one tool per kind), update, delete."""

from typing import Annotated, Any, Literal

from fastmcp.exceptions import ToolError
from pydantic import Field

from pp_mcp.app import DESTRUCTIVE, READ_ONLY, WRITE, mcp
from pp_mcp.money import MONEY, QUOTE, RATE, SHARES, date_str, decimal_str
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
    out,
)

Uuid = Annotated[str, Field(description="Transaction UUID (either leg of a buy/sell or transfer)")]
TxDate = Annotated[str, Field(description="Booking date YYYY-MM-DD or date and time YYYY-MM-DDTHH:MM")]
Note = Annotated[str | None, Field(description="Free-text note")]
Money = Annotated[str | None, Field(description="Amount, decimal string with at most 2 decimals")]
Shares = Annotated[str, Field(description="Number of shares, decimal string with at most 8 decimals")]
Quote = Annotated[
    str | None, Field(description="Price per share in the instrument currency, decimal string, max 8 decimals")
]
Rate = Annotated[
    str | None,
    Field(
        description="Exchange rate (transaction currency per 1 unit of instrument currency), decimal string, "
        "max 10 decimals. Only needed when the currencies differ and PP has no rate for the date."
    ),
]

TRANSACTION_TYPES = (
    "deposit",
    "removal",
    "interest",
    "interest-charge",
    "dividends",
    "fees",
    "fees-refund",
    "taxes",
    "tax-refund",
    "buy",
    "sell",
    "transfer-in",
    "transfer-out",
    "delivery-inbound",
    "delivery-outbound",
)
CASH_KINDS = {
    "deposit": "deposit",
    "removal": "removal",
    "interest": "interest",
    "interest_charge": "interest-charge",
    "fees": "fees",
    "fees_refund": "fees-refund",
    "taxes": "taxes",
    "tax_refund": "tax-refund",
}


def _money(value: str | None, field: str) -> str | None:
    return decimal_str(value, MONEY, field)


def _one_of_quote_gross(quote: str | None, gross_value: str | None) -> dict[str, str]:
    if (quote is None) == (gross_value is None):
        raise ToolError("pass exactly one of quote or gross_value")
    return compact(quote=decimal_str(quote, QUOTE, "quote"), grossValue=_money(gross_value, "gross_value"))


async def _create(file: str | None, body: dict[str, Any], dry_run: bool) -> dict[str, Any]:
    async with api() as pp:
        f = await pp.resolve_file(file)
        return out(await pp.post(fpath(f, "transactions"), params=dry(dry_run), body=body))


@mcp.tool(annotations=READ_ONLY)
async def list_transactions(
    file: FileParam = None,
    from_date: Annotated[str | None, Field(description="First date YYYY-MM-DD, inclusive")] = None,
    to_date: Annotated[str | None, Field(description="Last date YYYY-MM-DD, inclusive")] = None,
    type: Annotated[
        str | None, Field(description="Transaction type, e.g. buy, sell, dividends, deposit, transfer-out")
    ] = None,
    instrument: Annotated[str | None, Field(description="Instrument UUID")] = None,
    cash_account: Annotated[str | None, Field(description="Cash account UUID")] = None,
    investment_account: Annotated[str | None, Field(description="Investment account UUID")] = None,
    limit: Annotated[int | None, Field(description="Return at most this many (newest first)", ge=1)] = None,
) -> dict[str, Any]:
    """List transactions, newest first. A buy/sell appears once (investment-account
    leg), a transfer once (outbound leg). `value` is the signed net cash flow;
    `grossValue`, `fees`, `taxes` are unsigned, all `{value, currency}` with decimal
    strings (2 decimals); `shares` has up to 8 decimals. With `limit`, `total` tells
    how many matched."""
    date_str(from_date, "from_date")
    date_str(to_date, "to_date")
    if type is not None and type not in TRANSACTION_TYPES:
        raise ToolError(f"type: {type!r} is not one of {', '.join(TRANSACTION_TYPES)}")
    params = {
        "from": from_date,
        "to": to_date,
        "type": type,
        "instrument": instrument,
        "cashAccount": cash_account,
        "investmentAccount": investment_account,
    }
    async with api() as pp:
        f = await pp.resolve_file(file)
        result = await pp.get(fpath(f, "transactions"), params=params)
    items = result.get("items", [])
    if limit is not None and len(items) > limit:
        result = {**result, "items": items[:limit], "total": len(items), "truncated": True}
    return out(result)


@mcp.tool(annotations=READ_ONLY)
async def get_transaction(uuid: Uuid, file: FileParam = None) -> dict[str, Any]:
    """Read one transaction with its `units` (gross value, fees, taxes incl. forex
    amounts and exchange rate), `exDate`, `source` and the `linked` other leg
    (uuid and owner) of a buy/sell or transfer."""
    async with api() as pp:
        f = await pp.resolve_file(file)
        return out(await pp.get(fpath(f, "transactions", uuid)))


async def _buy_sell(kind: str, file, investment_account, cash_account, instrument, date, shares, quote, gross_value,
                    fees, taxes, forex_fees, forex_taxes, exchange_rate, total, note, client_ref, dry_run):
    body = {
        "type": kind,
        "date": date_str(date, "date", allow_time=True),
        "investmentAccount": investment_account,
        "cashAccount": cash_account,
        "instrument": instrument,
        "shares": decimal_str(shares, SHARES, "shares"),
        **_one_of_quote_gross(quote, gross_value),
    }
    body |= compact(
        exchangeRate=decimal_str(exchange_rate, RATE, "exchange_rate"),
        fees=_money(fees, "fees"),
        taxes=_money(taxes, "taxes"),
        forexFees=_money(forex_fees, "forex_fees"),
        forexTaxes=_money(forex_taxes, "forex_taxes"),
        amount=_money(total, "total"),
        note=note,
        clientRef=client_ref,
    )
    return await _create(file, body, dry_run)


_BUY_SELL_DOC = """{verb} shares of an instrument: creates the investment-account leg and the
cash-account leg together.

Units: `shares` up to 8 decimals; `quote` (price per share, up to 8 decimals) or
`gross_value` (shares × price, 2 decimals) in the instrument currency — pass exactly
one. `fees`/`taxes` are in the cash-account (transaction) currency, `forex_fees`/
`forex_taxes` in the instrument currency, all with 2 decimals. `exchange_rate` is
needed only when instrument and cash-account currencies differ and PP has no rate.
`total` is optional; when given it is checked against the computed total.
Returns both legs' UUIDs (`uuid` and `linked.uuid`). `client_ref` makes the call
idempotent. The change is in memory only until save_file."""


@mcp.tool(annotations=WRITE, description=_BUY_SELL_DOC.format(verb="Buy"))
async def create_buy(
    investment_account: Annotated[str, Field(description="Investment account UUID")],
    cash_account: Annotated[str, Field(description="Cash account UUID that pays")],
    instrument: Annotated[str, Field(description="Instrument UUID")],
    date: TxDate,
    shares: Shares,
    file: FileParam = None,
    quote: Quote = None,
    gross_value: Money = None,
    fees: Money = None,
    taxes: Money = None,
    forex_fees: Money = None,
    forex_taxes: Money = None,
    exchange_rate: Rate = None,
    total: Money = None,
    note: Note = None,
    client_ref: ClientRef = None,
    dry_run: DryRun = False,
) -> dict[str, Any]:
    return await _buy_sell("buy", file, investment_account, cash_account, instrument, date, shares, quote,
                           gross_value, fees, taxes, forex_fees, forex_taxes, exchange_rate, total, note,
                           client_ref, dry_run)


@mcp.tool(annotations=WRITE, description=_BUY_SELL_DOC.format(verb="Sell"))
async def create_sell(
    investment_account: Annotated[str, Field(description="Investment account UUID")],
    cash_account: Annotated[str, Field(description="Cash account UUID that receives the proceeds")],
    instrument: Annotated[str, Field(description="Instrument UUID")],
    date: TxDate,
    shares: Shares,
    file: FileParam = None,
    quote: Quote = None,
    gross_value: Money = None,
    fees: Money = None,
    taxes: Money = None,
    forex_fees: Money = None,
    forex_taxes: Money = None,
    exchange_rate: Rate = None,
    total: Money = None,
    note: Note = None,
    client_ref: ClientRef = None,
    dry_run: DryRun = False,
) -> dict[str, Any]:
    return await _buy_sell("sell", file, investment_account, cash_account, instrument, date, shares, quote,
                           gross_value, fees, taxes, forex_fees, forex_taxes, exchange_rate, total, note,
                           client_ref, dry_run)


@mcp.tool(annotations=WRITE)
async def create_delivery(
    investment_account: Annotated[str, Field(description="Investment account UUID")],
    instrument: Annotated[str, Field(description="Instrument UUID")],
    direction: Annotated[Literal["inbound", "outbound"], Field(description="inbound adds, outbound removes shares")],
    date: TxDate,
    shares: Shares,
    file: FileParam = None,
    quote: Quote = None,
    gross_value: Money = None,
    currency: Annotated[
        str | None, Field(description="Transaction currency (default: the reference cash account's currency)")
    ] = None,
    exchange_rate: Rate = None,
    fees: Money = None,
    taxes: Money = None,
    note: Note = None,
    client_ref: ClientRef = None,
    dry_run: DryRun = False,
) -> dict[str, Any]:
    """Deliver shares into or out of an investment account without a cash leg
    (e.g. a transfer from another broker, a spin-off, a gift).

    Units: `shares` up to 8 decimals; `quote` (per share, 8 decimals) or `gross_value`
    (2 decimals) in the instrument currency, exactly one; `fees`/`taxes` in the
    transaction currency, 2 decimals. The change is in memory only until save_file."""
    body = {
        "type": f"delivery-{direction}",
        "date": date_str(date, "date", allow_time=True),
        "investmentAccount": investment_account,
        "instrument": instrument,
        "shares": decimal_str(shares, SHARES, "shares"),
        **_one_of_quote_gross(quote, gross_value),
    }
    body |= compact(
        currency=currency,
        exchangeRate=decimal_str(exchange_rate, RATE, "exchange_rate"),
        fees=_money(fees, "fees"),
        taxes=_money(taxes, "taxes"),
        note=note,
        clientRef=client_ref,
    )
    return await _create(file, body, dry_run)


@mcp.tool(annotations=WRITE)
async def create_dividend(
    cash_account: Annotated[str, Field(description="Cash account UUID that receives the dividend")],
    instrument: Annotated[str, Field(description="Instrument UUID")],
    date: TxDate,
    gross_value: Annotated[str, Field(description="Gross dividend before taxes and fees, 2 decimals")],
    file: FileParam = None,
    ex_date: Annotated[str | None, Field(description="Ex-dividend date YYYY-MM-DD, not after `date`")] = None,
    shares: Annotated[str | None, Field(description="Shares entitled, max 8 decimals (0 allowed)")] = None,
    exchange_rate: Rate = None,
    taxes: Money = None,
    fees: Money = None,
    note: Note = None,
    client_ref: ClientRef = None,
    dry_run: DryRun = False,
) -> dict[str, Any]:
    """Book a dividend on a cash account. `gross_value`, `taxes` and `fees` are in the
    cash-account currency with 2 decimals (when the instrument currency differs, pass
    `exchange_rate` unless PP knows the rate); the net amount is gross − taxes − fees.
    The change is in memory only until save_file."""
    body = {
        "type": "dividends",
        "date": date_str(date, "date", allow_time=True),
        "cashAccount": cash_account,
        "instrument": instrument,
        "grossValue": _money(gross_value, "gross_value"),
    }
    body |= compact(
        exDate=date_str(ex_date, "ex_date"),
        shares=decimal_str(shares, SHARES, "shares"),
        exchangeRate=decimal_str(exchange_rate, RATE, "exchange_rate"),
        taxes=_money(taxes, "taxes"),
        fees=_money(fees, "fees"),
        note=note,
        clientRef=client_ref,
    )
    return await _create(file, body, dry_run)


@mcp.tool(annotations=WRITE)
async def create_cash_transaction(
    cash_account: Annotated[str, Field(description="Cash account UUID")],
    kind: Annotated[
        Literal["deposit", "removal", "interest", "interest_charge", "fees", "fees_refund", "taxes", "tax_refund"],
        Field(description="Kind of cash booking"),
    ],
    date: TxDate,
    amount: Annotated[str, Field(description="Positive amount in the account currency, 2 decimals")],
    file: FileParam = None,
    instrument: Annotated[
        str | None, Field(description="Optional instrument UUID; only for fees, fees_refund, taxes, tax_refund")
    ] = None,
    taxes: Annotated[str | None, Field(description="Withheld taxes, only for interest; 2 decimals")] = None,
    note: Note = None,
    client_ref: ClientRef = None,
    dry_run: DryRun = False,
) -> dict[str, Any]:
    """Book a deposit, removal, interest, interest charge, fee, fee refund, tax or tax
    refund on a cash account. `amount` is positive; PP applies the sign by kind.
    The change is in memory only until save_file."""
    body = {
        "type": CASH_KINDS[kind],
        "date": date_str(date, "date", allow_time=True),
        "cashAccount": cash_account,
        "amount": _money(amount, "amount"),
    }
    body |= compact(instrument=instrument, taxes=_money(taxes, "taxes"), note=note, clientRef=client_ref)
    return await _create(file, body, dry_run)


@mcp.tool(annotations=WRITE)
async def create_transfer(
    from_cash_account: Annotated[str, Field(description="Source cash account UUID")],
    to_cash_account: Annotated[str, Field(description="Target cash account UUID")],
    date: TxDate,
    amount: Annotated[str, Field(description="Amount leaving the source account, its currency, 2 decimals")],
    file: FileParam = None,
    target_amount: Annotated[
        str | None,
        Field(description="Amount arriving in the target account currency; required when the currencies differ"),
    ] = None,
    note: Note = None,
    client_ref: ClientRef = None,
    dry_run: DryRun = False,
) -> dict[str, Any]:
    """Transfer cash between two cash accounts (both legs are created and returned).
    The change is in memory only until save_file."""
    body = {
        "type": "cash-transfer",
        "date": date_str(date, "date", allow_time=True),
        "fromCashAccount": from_cash_account,
        "toCashAccount": to_cash_account,
        "amount": _money(amount, "amount"),
    }
    body |= compact(targetAmount=_money(target_amount, "target_amount"), note=note, clientRef=client_ref)
    return await _create(file, body, dry_run)


@mcp.tool(annotations=WRITE)
async def create_security_transfer(
    from_investment_account: Annotated[str, Field(description="Source investment account UUID")],
    to_investment_account: Annotated[str, Field(description="Target investment account UUID")],
    instrument: Annotated[str, Field(description="Instrument UUID")],
    date: TxDate,
    shares: Shares,
    amount: Annotated[str, Field(description="Book value in the instrument currency, 2 decimals")],
    file: FileParam = None,
    note: Note = None,
    client_ref: ClientRef = None,
    dry_run: DryRun = False,
) -> dict[str, Any]:
    """Move shares between two investment accounts (both legs are created and returned).
    The change is in memory only until save_file."""
    body = {
        "type": "security-transfer",
        "date": date_str(date, "date", allow_time=True),
        "fromInvestmentAccount": from_investment_account,
        "toInvestmentAccount": to_investment_account,
        "instrument": instrument,
        "shares": decimal_str(shares, SHARES, "shares"),
        "amount": _money(amount, "amount"),
    }
    body |= compact(note=note, clientRef=client_ref)
    return await _create(file, body, dry_run)


@mcp.tool(annotations=WRITE)
async def update_transaction(
    uuid: Uuid,
    file: FileParam = None,
    type: Annotated[
        str | None, Field(description="New type within the same family, e.g. buy↔sell, deposit↔removal")
    ] = None,
    date: Annotated[str | None, Field(description="YYYY-MM-DD or YYYY-MM-DDTHH:MM")] = None,
    ex_date: Annotated[str | None, Field(description="Ex-date YYYY-MM-DD (dividends)")] = None,
    shares: Annotated[str | None, Field(description="Shares, max 8 decimals")] = None,
    quote: Quote = None,
    gross_value: Money = None,
    amount: Annotated[str | None, Field(description="Total/amount, 2 decimals")] = None,
    target_amount: Money = None,
    fees: Money = None,
    taxes: Money = None,
    forex_fees: Money = None,
    forex_taxes: Money = None,
    exchange_rate: Rate = None,
    currency: str | None = None,
    instrument: Annotated[str | None, Field(description="Instrument UUID")] = None,
    cash_account: Annotated[str | None, Field(description="Move to this cash account (UUID)")] = None,
    investment_account: Annotated[str | None, Field(description="Move to this investment account (UUID)")] = None,
    from_cash_account: str | None = None,
    to_cash_account: str | None = None,
    from_investment_account: str | None = None,
    to_investment_account: str | None = None,
    note: Note = None,
    clear: Clear = None,
    dry_run: DryRun = False,
) -> dict[str, Any]:
    """Update a transaction (merge patch): only the given fields change; the linked leg
    of a buy/sell or transfer is kept consistent and UUIDs are preserved, also when
    moving it to another account. Units and precision as in the create tools.
    `clear` accepts `note`, `ex_date`, `fees`, `taxes`, `forex_fees`, `forex_taxes`,
    `instrument`. The change is in memory only until save_file."""
    patch = compact(
        type=type,
        date=date_str(date, "date", allow_time=True),
        exDate=date_str(ex_date, "ex_date"),
        shares=decimal_str(shares, SHARES, "shares"),
        quote=decimal_str(quote, QUOTE, "quote"),
        grossValue=_money(gross_value, "gross_value"),
        amount=_money(amount, "amount"),
        targetAmount=_money(target_amount, "target_amount"),
        fees=_money(fees, "fees"),
        taxes=_money(taxes, "taxes"),
        forexFees=_money(forex_fees, "forex_fees"),
        forexTaxes=_money(forex_taxes, "forex_taxes"),
        exchangeRate=decimal_str(exchange_rate, RATE, "exchange_rate"),
        currency=currency,
        instrument=instrument,
        cashAccount=cash_account,
        investmentAccount=investment_account,
        fromCashAccount=from_cash_account,
        toCashAccount=to_cash_account,
        fromInvestmentAccount=from_investment_account,
        toInvestmentAccount=to_investment_account,
        note=note,
    )
    apply_clear(
        patch,
        clear,
        {
            "note": "note",
            "ex_date": "exDate",
            "fees": "fees",
            "taxes": "taxes",
            "forex_fees": "forexFees",
            "forex_taxes": "forexTaxes",
            "instrument": "instrument",
        },
    )
    if not patch:
        raise ToolError("nothing to update: pass at least one field")
    async with api() as pp:
        f = await pp.resolve_file(file)
        return out(await pp.patch(fpath(f, "transactions", uuid), params=dry(dry_run), body=patch))


@mcp.tool(annotations=DESTRUCTIVE)
async def delete_transaction(uuid: Uuid, file: FileParam = None, dry_run: DryRun = False) -> dict[str, Any]:
    """Delete a transaction including its linked leg (buy/sell, transfer). A dry run
    reports the legs that would be removed. The change is in memory only until
    save_file."""
    async with api() as pp:
        f = await pp.resolve_file(file)
        return deleted(await pp.delete(fpath(f, "transactions", uuid), params=dry(dry_run)), uuid=uuid)
