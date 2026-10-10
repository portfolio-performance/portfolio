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
Total = Annotated[
    str | None,
    Field(description="Total cash amount incl. fees and taxes in the transaction currency, 2 decimals"),
]
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


def _price(quote: str | None, gross_value: str | None, total: str | None) -> dict[str, str]:
    """`quote`, `grossValue` and the total `amount` of a buy, sell or delivery; at least one is required."""
    if quote is None and gross_value is None and total is None:
        raise ToolError("pass at least one of quote, gross_value or total")
    return compact(
        quote=decimal_str(quote, QUOTE, "quote"),
        grossValue=_money(gross_value, "gross_value"),
        amount=_money(total, "total"),
    )


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
        list[str] | None,
        Field(description="Transaction types (any of), e.g. ['buy', 'sell'], ['dividends'], ['transfer-out']"),
    ] = None,
    instrument: Annotated[str | None, Field(description="Instrument UUID")] = None,
    cash_account: Annotated[str | None, Field(description="Cash account UUID")] = None,
    investment_account: Annotated[str | None, Field(description="Investment account UUID")] = None,
    limit: Annotated[int | None, Field(description="Return at most this many (newest first)", ge=1)] = None,
) -> dict[str, Any]:
    """List transactions, newest first. A buy/sell appears once (investment-account
    leg), a transfer once (outbound leg); the account filters match either leg.
    `value` is the signed net cash flow; `grossValue`, `fees`, `taxes` are unsigned,
    all `{value, currency}` with decimal strings (2 decimals); `shares` has up to 8
    decimals. With `limit`, `total` tells how many matched."""
    date_str(from_date, "from_date")
    date_str(to_date, "to_date")
    for wanted in type or []:
        if wanted not in TRANSACTION_TYPES:
            raise ToolError(f"type: {wanted!r} is not one of {', '.join(TRANSACTION_TYPES)}")
    params = {
        "from": from_date,
        "to": to_date,
        "type": ",".join(type) if type else None,
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
    amounts and exchange rate), `exDate`, `source`, `clientRef` and the `linked`
    other leg (uuid, type and owner) of a buy/sell or transfer. Either leg's UUID
    resolves; the answer is the leg the list reports."""
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
        **_price(quote, gross_value, total),
    }
    body |= compact(
        exchangeRate=decimal_str(exchange_rate, RATE, "exchange_rate"),
        fees=_money(fees, "fees"),
        taxes=_money(taxes, "taxes"),
        forexFees=_money(forex_fees, "forex_fees"),
        forexTaxes=_money(forex_taxes, "forex_taxes"),
        note=note,
        clientRef=client_ref,
    )
    return await _create(file, body, dry_run)


_BUY_SELL_DOC = """{verb} shares of an instrument: creates the investment-account leg and the
cash-account leg together.

Units: `shares` up to 8 decimals; `quote` (price per share, up to 8 decimals) and/or
`gross_value` (shares × price, 2 decimals) in the instrument currency, and/or `total`
(the cash amount incl. fees and taxes, cash-account currency, 2 decimals) — pass at
least one. With `total` alone PP derives the gross value as when typing the total in
its dialog; together with quote/gross_value a plausible total wins and an implausible
one is `total-mismatch`. `fees`/`taxes` are in the cash-account (transaction)
currency, `forex_fees`/`forex_taxes` in the instrument currency (only when the
currencies differ), all with 2 decimals. `exchange_rate` is needed only when
instrument and cash-account currencies differ and PP has no rate.
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
    total: Total = None,
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
    total: Total = None,
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
    total: Total = None,
    currency: Annotated[
        str | None, Field(description="Transaction currency (default: the reference cash account's currency)")
    ] = None,
    exchange_rate: Rate = None,
    fees: Money = None,
    taxes: Money = None,
    forex_fees: Money = None,
    forex_taxes: Money = None,
    note: Note = None,
    client_ref: ClientRef = None,
    dry_run: DryRun = False,
) -> dict[str, Any]:
    """Deliver shares into or out of an investment account without a cash leg
    (e.g. a transfer from another broker, a spin-off, a gift).

    Units: `shares` up to 8 decimals; `quote` (per share, 8 decimals) and/or
    `gross_value` (2 decimals) in the instrument currency, and/or `total` (value incl.
    fees and taxes, transaction currency) — at least one; `fees`/`taxes` in the
    transaction currency, `forex_fees`/`forex_taxes` in the instrument currency (only
    when it differs), 2 decimals. The change is in memory only until save_file."""
    body = {
        "type": f"delivery-{direction}",
        "date": date_str(date, "date", allow_time=True),
        "investmentAccount": investment_account,
        "instrument": instrument,
        "shares": decimal_str(shares, SHARES, "shares"),
        **_price(quote, gross_value, total),
    }
    body |= compact(
        currency=currency,
        exchangeRate=decimal_str(exchange_rate, RATE, "exchange_rate"),
        fees=_money(fees, "fees"),
        taxes=_money(taxes, "taxes"),
        forexFees=_money(forex_fees, "forex_fees"),
        forexTaxes=_money(forex_taxes, "forex_taxes"),
        note=note,
        clientRef=client_ref,
    )
    return await _create(file, body, dry_run)


@mcp.tool(annotations=WRITE)
async def create_dividend(
    cash_account: Annotated[str, Field(description="Cash account UUID that receives the dividend")],
    instrument: Annotated[str, Field(description="Instrument UUID")],
    date: TxDate,
    file: FileParam = None,
    gross_value: Annotated[
        str | None, Field(description="Gross dividend before taxes and fees, cash-account currency, 2 decimals")
    ] = None,
    total: Annotated[
        str | None, Field(description="Net amount credited (gross − taxes − fees), cash-account currency, 2 decimals")
    ] = None,
    ex_date: Annotated[str | None, Field(description="Ex-dividend date YYYY-MM-DD, not after `date`")] = None,
    shares: Annotated[str | None, Field(description="Shares entitled, max 8 decimals (0 allowed)")] = None,
    exchange_rate: Rate = None,
    taxes: Money = None,
    fees: Money = None,
    forex_taxes: Money = None,
    forex_fees: Money = None,
    note: Note = None,
    client_ref: ClientRef = None,
    dry_run: DryRun = False,
) -> dict[str, Any]:
    """Book a dividend on a cash account. `gross_value`, `total`, `taxes` and `fees` are
    in the cash-account currency with 2 decimals; pass `gross_value` and/or `total`
    (the net amount = gross − taxes − fees; both given are checked against each other).
    When the instrument currency differs, pass `exchange_rate` unless PP knows the rate;
    `forex_taxes`/`forex_fees` are then amounts in the instrument currency.
    The change is in memory only until save_file."""
    if gross_value is None and total is None:
        raise ToolError("pass gross_value and/or total")
    body = {
        "type": "dividends",
        "date": date_str(date, "date", allow_time=True),
        "cashAccount": cash_account,
        "instrument": instrument,
    }
    body |= compact(
        grossValue=_money(gross_value, "gross_value"),
        amount=_money(total, "total"),
        exDate=date_str(ex_date, "ex_date"),
        shares=decimal_str(shares, SHARES, "shares"),
        exchangeRate=decimal_str(exchange_rate, RATE, "exchange_rate"),
        taxes=_money(taxes, "taxes"),
        fees=_money(fees, "fees"),
        forexTaxes=_money(forex_taxes, "forex_taxes"),
        forexFees=_money(forex_fees, "forex_fees"),
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
    amount: Annotated[str, Field(description="Positive booked (net) amount in the account currency, 2 decimals")],
    file: FileParam = None,
    instrument: Annotated[
        str | None, Field(description="Optional instrument UUID; only for fees, fees_refund, taxes, tax_refund")
    ] = None,
    taxes: Annotated[str | None, Field(description="Withheld taxes, only for interest; 2 decimals")] = None,
    exchange_rate: Annotated[
        str | None,
        Field(
            description="Only for fees, fees_refund, taxes, tax_refund with an instrument in another currency: "
            "account currency per 1 unit of instrument currency, max 10 decimals (default: PP's rate)"
        ),
    ] = None,
    note: Note = None,
    client_ref: ClientRef = None,
    dry_run: DryRun = False,
) -> dict[str, Any]:
    """Book a deposit, removal, interest, interest charge, fee, fee refund, tax or tax
    refund on a cash account. `amount` is the positive booked (net) amount; PP applies
    the sign by kind. For interest, `taxes` come on top (gross = amount + taxes).
    The change is in memory only until save_file."""
    body = {
        "type": CASH_KINDS[kind],
        "date": date_str(date, "date", allow_time=True),
        "cashAccount": cash_account,
        "amount": _money(amount, "amount"),
    }
    body |= compact(
        instrument=instrument,
        taxes=_money(taxes, "taxes"),
        exchangeRate=decimal_str(exchange_rate, RATE, "exchange_rate"),
        note=note,
        clientRef=client_ref,
    )
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
    Between currencies the rate follows from `amount` and `target_amount`; there is no
    separate exchange rate. The change is in memory only until save_file."""
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
    file: FileParam = None,
    amount: Annotated[str | None, Field(description="Book value in the instrument currency, 2 decimals")] = None,
    quote: Quote = None,
    note: Note = None,
    client_ref: ClientRef = None,
    dry_run: DryRun = False,
) -> dict[str, Any]:
    """Move shares between two investment accounts (both legs are created and returned).
    Pass `amount` and/or `quote` (amount = shares × quote); both legs are booked in the
    instrument currency. The change is in memory only until save_file."""
    if amount is None and quote is None:
        raise ToolError("pass amount and/or quote")
    body = {
        "type": "security-transfer",
        "date": date_str(date, "date", allow_time=True),
        "fromInvestmentAccount": from_investment_account,
        "toInvestmentAccount": to_investment_account,
        "instrument": instrument,
        "shares": decimal_str(shares, SHARES, "shares"),
    }
    body |= compact(
        amount=_money(amount, "amount"),
        quote=decimal_str(quote, QUOTE, "quote"),
        note=note,
        clientRef=client_ref,
    )
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
    moving it to another account (a cash leg only to an account in the same currency).
    Units and precision as in the create tools. Amounts are recomputed only when the
    patch has `type`, `instrument`, `currency` or an amount field. A type change stays
    within buy/sell, the two deliveries, or the cash kinds; values the new type has no
    field for must be cleared in the same call. `clear` accepts `note`, `ex_date`,
    `shares`, `quote`, `gross_value`, `fees`, `taxes`, `forex_fees`, `forex_taxes`,
    `exchange_rate`, `target_amount`, `currency`, `instrument`. The change is in memory
    only until save_file."""
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
            "shares": "shares",
            "quote": "quote",
            "gross_value": "grossValue",
            "fees": "fees",
            "taxes": "taxes",
            "forex_fees": "forexFees",
            "forex_taxes": "forexTaxes",
            "exchange_rate": "exchangeRate",
            "target_amount": "targetAmount",
            "currency": "currency",
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
    answers `{dryRun: true, removed: [transaction, linked leg]}`. The change is in
    memory only until save_file."""
    async with api() as pp:
        f = await pp.resolve_file(file)
        return deleted(await pp.delete(fpath(f, "transactions", uuid), params=dry(dry_run)), uuid=uuid)
