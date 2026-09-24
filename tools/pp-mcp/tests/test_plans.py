import pytest
from fastmcp.exceptions import ToolError

from pp_mcp.tools import plans

from conftest import F, problem, sent_json, sent_query

P = "/v1/files/main/investment-plans"


async def test_list_plans_through_mcp(api, one_file, mcp_client):
    api.get(f"{F}/investment-plans").respond(200, json={"items": [{"name": "ETF"}]})
    result = await mcp_client.call_tool("list_investment_plans", {})
    assert result.data == {"items": [{"name": "ETF"}]}


async def test_create_plan(api):
    route = api.post(P).respond(201, json={"name": "ETF"})
    await plans.create_investment_plan(
        "ETF", "purchase", "2026-01-15", "250.00", "main", instrument="i1", investment_account="p1",
        cash_account="c1", interval_months=1, fees="1.50", auto_generate=True, note="n", dry_run=True,
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
    }
    assert sent_query(route) == {"dry_run": "true"}


async def test_create_plan_rejects_both_intervals_and_bad_amount():
    with pytest.raises(ToolError, match="either interval_months or interval_weeks"):
        await plans.create_investment_plan("x", "deposit", "2026-01-01", "1", "main", interval_months=1,
                                           interval_weeks=2)
    with pytest.raises(ToolError, match="amount: "):
        await plans.create_investment_plan("x", "deposit", "2026-01-01", "1.001", "main")


async def test_update_plan_rename_and_clear(api):
    route = api.patch(f"{P}/ETF%20Plan").respond(200, json={"name": "World"})
    await plans.update_investment_plan("ETF Plan", "main", new_name="World", interval_weeks=2, clear=["note"])
    assert sent_json(route) == {"name": "World", "intervalWeeks": 2, "note": None}


async def test_delete_plan(api):
    route = api.delete(f"{P}/ETF").respond(204)
    assert await plans.delete_investment_plan("ETF", "main", dry_run=True) == {"deleted": True, "name": "ETF"}
    assert sent_query(route) == {"dry_run": "true"}


async def test_generate(api, mcp_client):
    route = api.post(f"{P}/ETF/actions/generate").respond(200, json={"dryRun": True, "dates": ["2026-02-15"]})
    result = await mcp_client.call_tool("generate_plan_transactions", {"file": "main", "name": "ETF", "dry_run": True})
    assert result.data["dates"] == ["2026-02-15"]
    assert sent_query(route) == {"dry_run": "true"}


async def test_generate_missing_price(api):
    api.post(f"{P}/ETF/actions/generate").mock(
        return_value=problem(409, "missing-price", "Missing price", detail="no price on 2026-02-15")
    )
    with pytest.raises(ToolError, match="^missing-price: Missing price — no price on 2026-02-15$"):
        await plans.generate_plan_transactions("ETF", "main")
