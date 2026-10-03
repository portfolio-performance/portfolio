import pytest
from fastmcp.exceptions import ToolError

from pp_mcp.tools import plans

from conftest import F, problem, raw_json, sent_json, sent_query

P = "/v1/files/main/investment-plans"
ETF = (
    '{"name":"ETF","kind":"purchase","instrument":"i1","investmentAccount":"p1","cashAccount":"c1",'
    '"start":"2026-01-15","intervalMonths":1,"amount":{"value":250.00,"currency":"EUR"},'
    '"fees":{"value":1.50,"currency":"EUR"},"taxes":{"value":0,"currency":"EUR"},"autoGenerate":true,'
    '"transactionCount":8,"nextTransactionDate":"2026-10-15"}'
)


async def test_list_plans_through_mcp(api, one_file, mcp_client):
    api.get(f"{F}/investment-plans").mock(return_value=raw_json('{"items":[' + ETF + "]}"))
    result = await mcp_client.call_tool("list_investment_plans", {})
    assert result.data["items"][0]["amount"] == {"value": "250.00", "currency": "EUR"}
    assert result.data["items"][0]["nextTransactionDate"] == "2026-10-15"


async def test_get_plan(api):
    api.get(f"{P}/ETF%20Plan").mock(return_value=raw_json(ETF))
    assert (await plans.get_investment_plan("ETF Plan", "main"))["transactionCount"] == 8


async def test_create_plan(api):
    route = api.post(P).respond(200, json={"name": "ETF", "dryRun": True})
    await plans.create_investment_plan(
        "ETF", "purchase", "250.00", "main", start="2026-01-15", instrument="i1", investment_account="p1",
        cash_account="c1", interval_months=1, fees="1.50", auto_generate=True, note="n", client_ref="plan-1",
        dry_run=True,
    )
    assert sent_json(route) == {
        "name": "ETF",
        "kind": "purchase",
        "instrument": "i1",
        "investmentAccount": "p1",
        "cashAccount": "c1",
        "start": "2026-01-15",
        "intervalMonths": 1,
        "amount": "250.00",
        "fees": "1.50",
        "autoGenerate": True,
        "note": "n",
        "clientRef": "plan-1",
    }
    assert sent_query(route) == {"dry_run": "true"}


async def test_create_plan_start_defaults_to_today(api):
    route = api.post(P).respond(201, json={"name": "Save"})
    await plans.create_investment_plan("Save", "deposit", "100.00", "main", cash_account="c1")
    assert sent_json(route) == {"name": "Save", "kind": "deposit", "cashAccount": "c1", "amount": "100.00"}


async def test_create_plan_rejects_both_intervals_and_bad_amount():
    with pytest.raises(ToolError, match="either interval_months or interval_weeks"):
        await plans.create_investment_plan("x", "deposit", "1", "main", interval_months=1, interval_weeks=2)
    with pytest.raises(ToolError, match="amount: "):
        await plans.create_investment_plan("x", "deposit", "1.001", "main")


async def test_update_plan_rename_and_clear(api):
    route = api.patch(f"{P}/ETF%20Plan").respond(200, json={"name": "World"})
    await plans.update_investment_plan("ETF Plan", "main", new_name="World", interval_weeks=2, clear=["note"])
    assert sent_json(route) == {"name": "World", "intervalWeeks": 2, "note": None}


async def test_delete_plan(api):
    route = api.delete(f"{P}/ETF").respond(200, json={"dryRun": True, "removed": [{"name": "ETF"}]})
    assert await plans.delete_investment_plan("ETF", "main", dry_run=True) == {
        "dryRun": True, "removed": [{"name": "ETF"}]
    }
    assert sent_query(route) == {"dry_run": "true"}
    api.delete(f"{P}/ETF").respond(204)
    assert await plans.delete_investment_plan("ETF", "main") == {"deleted": True, "name": "ETF"}


async def test_generate_dry_run(api, mcp_client):
    route = api.post(f"{P}/ETF/actions/generate").respond(
        200, json={"name": "ETF", "dryRun": True, "dates": ["2026-02-15"], "count": 1}
    )
    result = await mcp_client.call_tool("generate_plan_transactions", {"file": "main", "name": "ETF", "dry_run": True})
    assert result.data["dates"] == ["2026-02-15"] and result.data["count"] == 1
    assert sent_query(route) == {"dry_run": "true"}


async def test_generate(api):
    api.post(f"{P}/ETF/actions/generate").respond(
        200,
        json={"name": "ETF", "count": 1, "transactions": [{"uuid": "t1", "type": "buy"}],
              "nextTransactionDate": "2026-03-15"},
    )
    result = await plans.generate_plan_transactions("ETF", "main")
    assert result["transactions"][0]["uuid"] == "t1" and result["nextTransactionDate"] == "2026-03-15"


async def test_generate_missing_price(api):
    api.post(f"{P}/ETF/actions/generate").mock(
        return_value=problem(409, "missing-price", "An instrument price is missing",
                             detail="ETF has no price on or before 2026-02-15")
    )
    with pytest.raises(
        ToolError, match="^missing-price: An instrument price is missing — ETF has no price on or before 2026-02-15$"
    ):
        await plans.generate_plan_transactions("ETF", "main")
