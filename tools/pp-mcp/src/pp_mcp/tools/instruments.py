"""Instruments (securities): read, find, create, update, delete, prices and events."""

from typing import Annotated, Any, Literal

from fastmcp.exceptions import ToolError
from pydantic import BaseModel, Field

from pp_mcp.app import DESTRUCTIVE, READ_ONLY, WRITE, mcp
from pp_mcp.money import QUOTE, date_str, decimal_str
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

Uuid = Annotated[str, Field(description="Instrument UUID")]
Attributes = Annotated[
    dict[str, Any] | None,
    Field(
        description="Custom attributes keyed by attribute id (see GET attribute types); values typed per "
        "attribute: string, boolean, ISO date string, or a decimal string for numeric types. "
        "On update, a null value clears that attribute."
    ),
]

_CLEARABLE = {
    "currency": "currencyCode",
    "isin": "isin",
    "wkn": "wkn",
    "ticker": "tickerSymbol",
    "note": "note",
    "feed": "feed",
    "feed_url": "feedUrl",
    "latest_feed": "latestFeed",
    "latest_feed_url": "latestFeedUrl",
    "target_currency": "targetCurrencyCode",
    "calendar": "calendar",
}


class PriceInput(BaseModel):
    date: Annotated[str, Field(description="YYYY-MM-DD")]
    value: Annotated[str, Field(description="Quote per share in the instrument currency, decimal string, max 8 decimals")]


def _matches(instrument: dict[str, Any], needle: str) -> bool:
    for key in ("name", "isin", "wkn", "tickerSymbol"):
        value = instrument.get(key)
        if isinstance(value, str) and needle in value.casefold():
            return True
    return False


def _find_duplicate(items: list[dict[str, Any]], name: str, currency: str | None, isin: str | None):
    if isin:
        wanted = isin.strip().casefold()
        return next((i for i in items if (i.get("isin") or "").strip().casefold() == wanted), None)
    wanted_name = name.strip().casefold()
    return next(
        (
            i
            for i in items
            if (i.get("name") or "").strip().casefold() == wanted_name and i.get("currencyCode") == currency
        ),
        None,
    )


@mcp.tool(annotations=READ_ONLY)
async def list_instruments(file: FileParam = None) -> dict[str, Any]:
    """List all instruments (securities) of a file: uuid, name, currencyCode, isin, wkn,
    tickerSymbol, note, attributes, feed settings and `retired`."""
    async with api() as pp:
        f = await pp.resolve_file(file)
        return out(await pp.get(fpath(f, "instruments")))


@mcp.tool(annotations=READ_ONLY)
async def get_instrument(
    uuid: Uuid,
    file: FileParam = None,
    include_prices: Annotated[bool, Field(description="Also return the historical quotes")] = False,
    from_date: Annotated[str | None, Field(description="First price date (YYYY-MM-DD), inclusive")] = None,
    to_date: Annotated[str | None, Field(description="Last price date (YYYY-MM-DD), inclusive")] = None,
) -> dict[str, Any]:
    """Read one instrument. With `include_prices` the result has a `prices` object:
    `{uuid, currency, from, to, items: [{date, value}]}`, quotes per share in the
    instrument currency as decimal strings (up to 8 decimals), oldest first."""
    date_str(from_date, "from_date")
    date_str(to_date, "to_date")
    async with api() as pp:
        f = await pp.resolve_file(file)
        instrument = await pp.get(fpath(f, "instruments", uuid))
        if include_prices:
            instrument["prices"] = await pp.get(
                fpath(f, "instruments", uuid, "prices"), params={"from": from_date, "to": to_date}
            )
        return out(instrument)


@mcp.tool(annotations=READ_ONLY)
async def find_instrument(
    query: Annotated[str, Field(description="Case-insensitive substring of name, ISIN, WKN or ticker symbol")],
    file: FileParam = None,
) -> dict[str, Any]:
    """Find instruments whose name, ISIN, WKN or ticker contains `query`."""
    needle = query.strip().casefold()
    if not needle:
        raise ToolError("query: must not be empty")
    async with api() as pp:
        f = await pp.resolve_file(file)
        listed = await pp.get(fpath(f, "instruments"))
    return out({"items": [i for i in listed.get("items", []) if _matches(i, needle)]})


@mcp.tool(annotations=WRITE)
async def create_instrument(
    name: Annotated[str, Field(description="Display name")],
    currency: Annotated[str | None, Field(description="ISO 4217 code; omit only for an index without currency")],
    file: FileParam = None,
    isin: str | None = None,
    wkn: str | None = None,
    ticker: Annotated[str | None, Field(description="Ticker symbol, e.g. AAPL or SAP.DE")] = None,
    note: str | None = None,
    feed: Annotated[str | None, Field(description="Historical quote feed id, e.g. MANUAL, YAHOO")] = None,
    feed_url: str | None = None,
    latest_feed: Annotated[str | None, Field(description="Latest quote feed id")] = None,
    latest_feed_url: str | None = None,
    attributes: Attributes = None,
    allow_duplicate: Annotated[
        bool, Field(description="Create even if an instrument with the same ISIN (or name+currency) exists")
    ] = False,
    client_ref: ClientRef = None,
    dry_run: DryRun = False,
) -> dict[str, Any]:
    """Create an instrument (security).

    First checks for an existing instrument with the same ISIN, or with the same name
    and currency when no ISIN is given; if one exists it is returned with
    `created: false` and a note instead of creating a duplicate (unless
    `allow_duplicate`). Result: `{created, instrument, note?}`.
    The change is in memory only until save_file.
    """
    async with api() as pp:
        f = await pp.resolve_file(file)
        if not allow_duplicate:
            listed = await pp.get(fpath(f, "instruments"))
            existing = _find_duplicate(listed.get("items", []), name, currency, isin)
            if existing is not None:
                key = f"ISIN {isin}" if isin else f"name {name!r} and currency {currency}"
                return out(
                    {
                        "created": False,
                        "instrument": existing,
                        "note": f"An instrument with {key} already exists; returned it instead of creating a "
                        "duplicate. Pass allow_duplicate=true to create another one.",
                    }
                )
        body = compact(
            name=name,
            currencyCode=currency,
            isin=isin,
            wkn=wkn,
            tickerSymbol=ticker,
            note=note,
            feed=feed,
            feedUrl=feed_url,
            latestFeed=latest_feed,
            latestFeedUrl=latest_feed_url,
            attributes=no_floats(attributes, "attributes"),
            clientRef=client_ref,
        )
        created = await pp.post(fpath(f, "instruments"), params=dry(dry_run), body=body)
        replayed = isinstance(created, dict) and bool(created.get("replayed"))
        return out({"created": not dry_run and not replayed, "instrument": created})


@mcp.tool(annotations=WRITE)
async def update_instrument(
    uuid: Uuid,
    file: FileParam = None,
    name: str | None = None,
    currency: Annotated[str | None, Field(description="ISO 4217; refused while the instrument has transactions")] = None,
    isin: str | None = None,
    wkn: str | None = None,
    ticker: str | None = None,
    note: str | None = None,
    feed: str | None = None,
    feed_url: str | None = None,
    latest_feed: str | None = None,
    latest_feed_url: str | None = None,
    feed_properties: Annotated[dict[str, str | None] | None, Field(description="Feed properties; null clears one")] = None,
    target_currency: Annotated[str | None, Field(description="Target currency of an exchange-rate instrument")] = None,
    calendar: Annotated[str | None, Field(description="Trading calendar id")] = None,
    retired: Annotated[bool | None, Field(description="Retire (true) or reactivate (false) the instrument")] = None,
    attributes: Attributes = None,
    clear: Clear = None,
    dry_run: DryRun = False,
) -> dict[str, Any]:
    """Update an instrument (JSON merge patch): only the given fields change; fields
    named in `clear` are removed. Returns the updated instrument.
    The change is in memory only until save_file."""
    patch = compact(
        name=name,
        currencyCode=currency,
        isin=isin,
        wkn=wkn,
        tickerSymbol=ticker,
        note=note,
        feed=feed,
        feedUrl=feed_url,
        latestFeed=latest_feed,
        latestFeedUrl=latest_feed_url,
        feedProperties=feed_properties,
        targetCurrencyCode=target_currency,
        calendar=calendar,
        retired=retired,
        attributes=no_floats(attributes, "attributes"),
    )
    apply_clear(patch, clear, _CLEARABLE)
    if not patch:
        raise ToolError("nothing to update: pass at least one field")
    async with api() as pp:
        f = await pp.resolve_file(file)
        return out(await pp.patch(fpath(f, "instruments", uuid), params=dry(dry_run), body=patch))


@mcp.tool(annotations=DESTRUCTIVE)
async def delete_instrument(uuid: Uuid, file: FileParam = None, dry_run: DryRun = False) -> dict[str, Any]:
    """Delete an instrument. Refused (`delete-blocked`) while transactions or investment
    plans reference it; retire it with update_instrument(retired=true) instead.
    The change is in memory only until save_file."""
    async with api() as pp:
        f = await pp.resolve_file(file)
        return deleted(await pp.delete(fpath(f, "instruments", uuid), params=dry(dry_run)), uuid=uuid)


@mcp.tool(annotations=WRITE)
async def set_prices(
    uuid: Uuid,
    prices: Annotated[list[PriceInput], Field(description="Quotes to add or overwrite (same date is replaced)")],
    file: FileParam = None,
    dry_run: DryRun = False,
) -> dict[str, Any]:
    """Add or replace historical quotes of an instrument. `value` is the price per share
    in the instrument currency, a decimal string with at most 8 decimals.
    The change is in memory only until save_file."""
    if not prices:
        raise ToolError("prices: pass at least one price")
    items = [
        {
            "date": date_str(p.date, f"prices[{n}].date"),
            "value": decimal_str(p.value, QUOTE, f"prices[{n}].value"),
        }
        for n, p in enumerate(prices)
    ]
    async with api() as pp:
        f = await pp.resolve_file(file)
        return out(await pp.put(fpath(f, "instruments", uuid, "prices"), params=dry(dry_run), body={"items": items}))


@mcp.tool(annotations=DESTRUCTIVE)
async def delete_prices(
    uuid: Uuid,
    file: FileParam = None,
    from_date: Annotated[str | None, Field(description="First date to delete (YYYY-MM-DD), inclusive")] = None,
    to_date: Annotated[str | None, Field(description="Last date to delete (YYYY-MM-DD), inclusive")] = None,
    delete_all: Annotated[bool, Field(description="Required to delete every price when no range is given")] = False,
    dry_run: DryRun = False,
) -> dict[str, Any]:
    """Delete the historical quotes of an instrument within [from_date, to_date].
    Without any bound all prices are deleted, which requires `delete_all=true`.
    The change is in memory only until save_file."""
    date_str(from_date, "from_date")
    date_str(to_date, "to_date")
    if from_date is None and to_date is None and not delete_all:
        raise ToolError("pass from_date and/or to_date, or delete_all=true to delete every price")
    async with api() as pp:
        f = await pp.resolve_file(file)
        result = await pp.delete(
            fpath(f, "instruments", uuid, "prices"), params=dry(dry_run, **{"from": from_date, "to": to_date})
        )
        return deleted(result, uuid=uuid)


@mcp.tool(annotations=WRITE)
async def add_instrument_event(
    uuid: Uuid,
    type: Annotated[Literal["stock-split", "note"], Field(description="Event type")],
    date: Annotated[str, Field(description="YYYY-MM-DD")],
    details: Annotated[str, Field(description="Free text; for a split the ratio such as '2:1'")],
    file: FileParam = None,
    dry_run: DryRun = False,
) -> dict[str, Any]:
    """Add an event to an instrument's chart/history. This records the event only; to
    adjust shares and prices for a split use apply_stock_split.
    The change is in memory only until save_file."""
    body = {"type": type, "date": date_str(date, "date"), "details": details}
    async with api() as pp:
        f = await pp.resolve_file(file)
        return out(await pp.post(fpath(f, "instruments", uuid, "events"), params=dry(dry_run), body=body))


@mcp.tool(annotations=DESTRUCTIVE)
async def delete_instrument_event(
    uuid: Uuid,
    type: Annotated[Literal["stock-split", "note"], Field(description="Event type")],
    date: Annotated[str, Field(description="YYYY-MM-DD")],
    file: FileParam = None,
    details: Annotated[str | None, Field(description="Only delete events with exactly this text")] = None,
    dry_run: DryRun = False,
) -> dict[str, Any]:
    """Delete the instrument events matching type and date (and details, if given).
    The change is in memory only until save_file."""
    params = dry(dry_run, date=date_str(date, "date"), type=type, details=details)
    async with api() as pp:
        f = await pp.resolve_file(file)
        return deleted(await pp.delete(fpath(f, "instruments", uuid, "events"), params=params), uuid=uuid)
