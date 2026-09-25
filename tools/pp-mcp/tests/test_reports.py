import pytest
from fastmcp.exceptions import ToolError

from pp_mcp.tools import reports

from conftest import F, problem, raw_json, sent_query

M = "/v1/files/main"


async def test_holdings_through_mcp_with_default_file(api, one_file, mcp_client):
    route = api.get(f"{F}/holdings").mock(
        return_value=raw_json('{"date":"2026-01-31","totalAssets":{"value":10520.75,"currency":"EUR"},"items":[]}')
    )
    result = await mcp_client.call_tool(
        "get_holdings",
        {"date": "2026-01-31", "opening_date": "2025-01-31", "currency": "EUR", "cost_method": "moving-average",
         "investment_account": ["p1", "p2"], "cash_account": ["c1"]},
    )
    assert sent_query(route) == {
        "date": "2026-01-31",
        "openingDate": "2025-01-31",
        "currency": "EUR",
        "costMethod": "moving-average",
        "investmentAccount": "p1,p2",
        "cashAccount": "c1",
    }
    assert result.data["totalAssets"]["value"] == "10520.75"


async def test_performance_only(api):
    perf = api.get(f"{M}/performance").mock(return_value=raw_json('{"ttwror":0.0531,"irr":0.049}'))
    result = await reports.get_performance("2025-01-01", "main")
    assert sent_query(perf) == {"openingDate": "2025-01-01"}
    assert result == {"performance": {"ttwror": "0.0531", "irr": "0.049"}}


async def test_performance_with_series_and_calendar(api):
    perf = api.get(f"{M}/performance").respond(200, json={"ttwror": 0})
    series = api.get(f"{M}/performance/series").respond(200, json={"days": []})
    cal = api.get(f"{M}/performance/calendar").respond(200, json={"months": []})
    result = await reports.get_performance(
        "2025-01-01", "main", to_date="2025-12-31", currency="USD", cost_method="fifo",
        include_series=True, include_calendar=True, investment_account=["p1"],
    )
    base = {"openingDate": "2025-01-01", "closingDate": "2025-12-31", "currency": "USD", "investmentAccount": "p1"}
    assert sent_query(perf) == base | {"costMethod": "fifo"}
    assert sent_query(series) == base
    assert sent_query(cal) == base
    assert set(result) == {"performance", "series", "calendar"}


async def test_unknown_filter_account(api):
    api.get(f"{M}/holdings").mock(
        return_value=problem(400, "invalid-request", "Invalid request",
                             errors=[{"field": "investmentAccount", "code": "unknown-reference",
                                      "message": "zz is not a known investmentAccount"}])
    )
    with pytest.raises(ToolError, match=r"investmentAccount \(unknown-reference\)"):
        await reports.get_holdings("main", investment_account=["zz"])


async def test_performance_invalid_range(api):
    api.get(f"{M}/performance").mock(
        return_value=problem(400, "invalid-request", "Invalid request",
                             errors=[{"field": "closingDate", "code": "invalid-range", "message": "must be after"}])
    )
    with pytest.raises(ToolError, match=r"^invalid-request: Invalid request\nclosingDate \(invalid-range\)"):
        await reports.get_performance("2025-01-01", "main", to_date="2024-01-01")


async def test_security_performance(api):
    route = api.get(f"{M}/performance/securities").respond(200, json={"items": []})
    await reports.get_security_performance("2025-01-01", "main", to_date="2025-06-30", cost_method="fifo")
    assert sent_query(route) == {"openingDate": "2025-01-01", "closingDate": "2025-06-30", "costMethod": "fifo"}
    await reports.get_security_performance("2025-01-01", "main", investment_account=["p1"], cash_account=["c1", "c2"])
    assert sent_query(route) == {"openingDate": "2025-01-01", "investmentAccount": "p1", "cashAccount": "c1,c2"}


async def test_trades(api):
    route = api.get(f"{M}/trades").respond(200, json={"currency": "EUR", "items": []})
    await reports.get_trades("main")
    assert sent_query(route) == {}
    await reports.get_trades("main", currency="EUR", only_closed=True)
    assert sent_query(route) == {"currency": "EUR", "onlyClosed": "true"}


async def test_earnings(api):
    route = api.get(f"{M}/earnings").mock(
        return_value=raw_json(
            '{"items":[{"uuid":"t1","type":"dividends","value":{"value":42.50,"currency":"EUR"}}],'
            '"totals":[{"currency":"EUR","value":42.50,"dividends":42.50,"interest":0,"interestCharge":0,'
            '"taxes":7.50,"fees":0,"count":1}]}'
        )
    )
    result = await reports.get_earnings(
        "main", from_date="2026-01-01", to_date="2026-12-31", instrument="i1", cash_account="c1",
        investment_account="p1",
    )
    assert sent_query(route) == {
        "from": "2026-01-01", "to": "2026-12-31", "instrument": "i1", "cashAccount": "c1", "investmentAccount": "p1"
    }
    assert result["totals"][0]["value"] == "42.50" and result["totals"][0]["count"] == 1


async def test_date_validation():
    with pytest.raises(ToolError, match="from_date"):
        await reports.get_performance("01.01.2025", "main")
