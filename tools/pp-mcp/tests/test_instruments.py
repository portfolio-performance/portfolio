import pytest
from fastmcp.exceptions import ToolError

from pp_mcp.tools import instruments
from pp_mcp.tools.instruments import PriceInput

from conftest import F, problem, raw_json, sent_json, sent_query

APPLE = {"uuid": "a1", "name": "Apple Inc.", "currencyCode": "USD", "isin": "US0378331005", "tickerSymbol": "AAPL"}
SAP = {"uuid": "s1", "name": "SAP SE", "currencyCode": "EUR", "isin": "DE0007164600", "wkn": "716460"}
INDEX = {"uuid": "i1", "name": "My Fund", "currencyCode": "EUR"}


@pytest.fixture
def listing(api, one_file):
    return api.get(f"{F}/instruments").respond(200, json={"items": [APPLE, SAP, INDEX]})


@pytest.mark.parametrize(
    "query,expected",
    [("apple", ["a1"]), ("de000716", ["s1"]), ("716460", ["s1"]), ("aapl", ["a1"]), ("FUND", ["i1"]), ("inc", ["a1"])],
)
async def test_find_instrument(listing, query, expected):
    result = await instruments.find_instrument(query)
    assert [i["uuid"] for i in result["items"]] == expected


async def test_find_instrument_through_mcp(listing, mcp_client):
    result = await mcp_client.call_tool("find_instrument", {"query": "sap"})
    assert result.data["items"][0]["uuid"] == "s1"


async def test_get_instrument_with_prices_keeps_decimal_text(api):
    api.get("/v1/files/main/instruments/a1").respond(200, json=APPLE)
    prices = api.get("/v1/files/main/instruments/a1/prices").mock(
        return_value=raw_json('{"uuid":"a1","currency":"USD","items":[{"date":"2024-01-02","value":185.64000000}]}')
    )
    result = await instruments.get_instrument("a1", "main", include_prices=True, from_date="2024-01-01")
    assert sent_query(prices) == {"from": "2024-01-01"}
    assert result["prices"]["items"][0]["value"] == "185.64000000"


async def test_create_instrument_returns_existing_by_isin(listing, api):
    result = await instruments.create_instrument("Apple", "USD", isin="us0378331005")
    assert result["created"] is False
    assert result["instrument"]["uuid"] == "a1"
    assert "already exists" in result["note"]


async def test_create_instrument_returns_existing_by_name_and_currency(listing):
    result = await instruments.create_instrument("my fund", "EUR")
    assert result["created"] is False and result["instrument"]["uuid"] == "i1"


async def test_create_instrument_same_name_other_currency_is_created(listing, api):
    route = api.post(f"{F}/instruments").respond(201, json={"uuid": "n1", "name": "My Fund", "currencyCode": "USD"})
    result = await instruments.create_instrument("My Fund", "USD")
    assert result == {"created": True, "instrument": {"uuid": "n1", "name": "My Fund", "currencyCode": "USD"}}
    assert sent_json(route) == {"name": "My Fund", "currencyCode": "USD"}


async def test_create_instrument_full_body_and_dry_run_through_mcp(api, mcp_client):
    route = api.post("/v1/files/main/instruments").respond(200, json={"uuid": "n1", "dryRun": True})
    result = await mcp_client.call_tool(
        "create_instrument",
        {
            "file": "main",
            "name": "Siemens",
            "currency": "EUR",
            "isin": "DE0007236101",
            "wkn": "723610",
            "ticker": "SIE.DE",
            "note": "n",
            "feed": "YAHOO",
            "feed_url": "u",
            "latest_feed": "YAHOO",
            "latest_feed_url": "lu",
            "attributes": {"ter": "0.0020", "flag": True},
            "allow_duplicate": True,
            "client_ref": "ref-1",
            "dry_run": True,
        },
    )
    assert sent_query(route) == {"dry_run": "true"}
    assert sent_json(route) == {
        "name": "Siemens",
        "currencyCode": "EUR",
        "isin": "DE0007236101",
        "wkn": "723610",
        "tickerSymbol": "SIE.DE",
        "note": "n",
        "feed": "YAHOO",
        "feedUrl": "u",
        "latestFeed": "YAHOO",
        "latestFeedUrl": "lu",
        "attributes": {"ter": "0.0020", "flag": True},
        "clientRef": "ref-1",
    }
    assert result.data["created"] is False


async def test_create_instrument_rejects_float_attribute(api):
    with pytest.raises(ToolError, match="attributes.ter"):
        await instruments.create_instrument("X", "EUR", file="main", attributes={"ter": 0.2}, allow_duplicate=True)


async def test_update_instrument_merge_patch_with_clear(api):
    route = api.patch("/v1/files/main/instruments/a1").respond(200, json=APPLE)
    await instruments.update_instrument(
        "a1", "main", name="Apple", retired=True, feed_properties={"k": "v"}, clear=["note", "ticker"]
    )
    assert route.calls.last.request.headers["Content-Type"] == "application/merge-patch+json"
    assert sent_json(route) == {
        "name": "Apple",
        "feedProperties": {"k": "v"},
        "retired": True,
        "note": None,
        "tickerSymbol": None,
    }
    assert sent_query(route) == {}


async def test_update_instrument_rejects_bad_clear_and_empty(api):
    with pytest.raises(ToolError, match="cannot be cleared"):
        await instruments.update_instrument("a1", "main", clear=["name"])
    with pytest.raises(ToolError, match="nothing to update"):
        await instruments.update_instrument("a1", "main")


async def test_update_instrument_validation_error_text(api, mcp_client):
    api.patch("/v1/files/main/instruments/a1").mock(
        return_value=problem(
            422,
            "validation",
            "Validation failed",
            errors=[{"field": "currencyCode", "code": "locked-by-transactions", "message": "has transactions"}],
        )
    )
    with pytest.raises(ToolError) as e:
        await mcp_client.call_tool("update_instrument", {"file": "main", "uuid": "a1", "currency": "EUR"})
    assert "validation: Validation failed\ncurrencyCode (locked-by-transactions): has transactions" in str(e.value)


async def test_delete_instrument(api):
    route = api.delete("/v1/files/main/instruments/a1").respond(204)
    assert await instruments.delete_instrument("a1", "main") == {"deleted": True, "uuid": "a1"}
    assert sent_query(route) == {}


async def test_delete_instrument_dry_run_and_blocked(api):
    route = api.delete("/v1/files/main/instruments/a1").mock(
        return_value=problem(409, "delete-blocked", "Delete blocked", detail="3 transactions reference it")
    )
    with pytest.raises(ToolError, match="^delete-blocked: Delete blocked — 3 transactions reference it$"):
        await instruments.delete_instrument("a1", "main", dry_run=True)
    assert sent_query(route) == {"dry_run": "true"}


async def test_set_prices_sends_decimal_strings(api):
    route = api.put("/v1/files/main/instruments/a1/prices").respond(200, json={"upserted": 2})
    await instruments.set_prices(
        "a1",
        [PriceInput(date="2024-01-02", value="185.64"), PriceInput(date="2024-01-03", value="0.00012345")],
        "main",
        dry_run=True,
    )
    assert sent_json(route) == {
        "items": [{"date": "2024-01-02", "value": "185.64"}, {"date": "2024-01-03", "value": "0.00012345"}]
    }
    assert sent_query(route) == {"dry_run": "true"}


async def test_set_prices_through_mcp_validates_precision(api, mcp_client):
    with pytest.raises(ToolError, match=r"prices\[0\].value: .* more than 8 decimal places"):
        await mcp_client.call_tool(
            "set_prices", {"file": "main", "uuid": "a1", "prices": [{"date": "2024-01-02", "value": "1.123456789"}]}
        )


async def test_delete_prices_range_and_guard(api):
    route = api.delete("/v1/files/main/instruments/a1/prices").respond(204)
    await instruments.delete_prices("a1", "main", from_date="2024-01-01", to_date="2024-01-31")
    assert sent_query(route) == {"from": "2024-01-01", "to": "2024-01-31"}
    with pytest.raises(ToolError, match="delete_all"):
        await instruments.delete_prices("a1", "main")
    await instruments.delete_prices("a1", "main", delete_all=True)
    assert sent_query(route) == {}


async def test_events(api):
    add = api.post("/v1/files/main/instruments/a1/events").respond(201, json={"type": "note"})
    await instruments.add_instrument_event("a1", "note", "2024-05-01", "AGM", "main")
    assert sent_json(add) == {"type": "note", "date": "2024-05-01", "details": "AGM"}
    remove = api.delete("/v1/files/main/instruments/a1/events").respond(204)
    await instruments.delete_instrument_event("a1", "stock-split", "2024-06-10", "main", details="4:1", dry_run=True)
    assert sent_query(remove) == {"date": "2024-06-10", "type": "stock-split", "details": "4:1", "dry_run": "true"}
