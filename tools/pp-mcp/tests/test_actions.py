import httpx
import pytest
from fastmcp.exceptions import ToolError

from pp_mcp.tools import actions

from conftest import F, problem, sent_json, sent_query

M = "/v1/files/main"


async def test_update_quotes_polls_until_done_through_mcp(api, one_file, mcp_client, sleeps):
    start = api.post(f"{F}/actions/update-quotes").respond(202, json={"jobId": "j1", "status": "running"})
    poll = api.get(f"{F}/jobs/j1").mock(
        side_effect=[
            httpx.Response(200, json={"jobId": "j1", "status": "running"}),
            httpx.Response(200, json={"jobId": "j1", "status": "done", "summary": {"latestUpdated": 3}}),
        ]
    )
    result = await mcp_client.call_tool("update_quotes", {"instruments": ["a1", "s1"], "targets": ["latest"]})
    assert sent_json(start) == {"targets": ["latest"], "instruments": ["a1", "s1"]}
    assert sent_query(start) == {"wait": "30"}
    assert poll.call_count == 2
    assert sleeps == [1.0, 1.0]
    assert result.data == {"jobId": "j1", "status": "done", "summary": {"latestUpdated": 3}}


async def test_update_quotes_done_on_post_does_not_poll(api):
    start = api.post(f"{M}/actions/update-quotes").respond(200, json={"jobId": "j1", "status": "done"})
    result = await actions.update_quotes("main", wait_seconds=120)
    assert sent_json(start) == {"targets": ["latest", "historic"]}
    assert sent_query(start) == {"wait": "60"}
    assert result == {"jobId": "j1", "status": "done"}


async def test_update_quotes_gives_up_after_wait_seconds(api, sleeps):
    api.post(f"{M}/actions/update-quotes").respond(202, json={"jobId": "j1", "status": "running"})
    poll = api.get(f"{M}/jobs/j1").respond(200, json={"jobId": "j1", "status": "running"})
    result = await actions.update_quotes("main", wait_seconds=3)
    assert poll.call_count == 3
    assert result["status"] == "running"
    assert "get_job(job_id='j1')" in result["note"]


async def test_update_quotes_no_wait(api):
    start = api.post(f"{M}/actions/update-quotes").respond(202, json={"jobId": "j1", "status": "running"})
    result = await actions.update_quotes("main", wait_seconds=0)
    assert sent_query(start) == {}
    assert result["status"] == "running"


async def test_update_quotes_failed_job_is_terminal(api):
    api.post(f"{M}/actions/update-quotes").respond(202, json={"jobId": "j1", "status": "running"})
    api.get(f"{M}/jobs/j1").respond(200, json={"jobId": "j1", "status": "FAILED", "summary": {"error": "x"}})
    result = await actions.update_quotes("main", wait_seconds=5)
    assert result["status"] == "FAILED" and "note" not in result


async def test_update_quotes_unknown_instrument(api):
    api.post(f"{M}/actions/update-quotes").mock(
        return_value=problem(422, "validation", "Validation failed",
                             errors=[{"field": "instruments[0]", "code": "invalid-value", "message": "unknown"}])
    )
    with pytest.raises(ToolError, match=r"instruments\[0\] \(invalid-value\): unknown"):
        await actions.update_quotes("main", instruments=["zz"])


async def test_get_job(api):
    api.get(f"{M}/jobs/j1").respond(200, json={"jobId": "j1", "status": "done"})
    assert (await actions.get_job("j1", "main"))["status"] == "done"


async def test_stock_split(api, mcp_client):
    route = api.post(f"{M}/instruments/a1/actions/split").respond(
        200, json={"dryRun": True, "transactionsAffected": 4, "pricesAffected": 250}
    )
    result = await mcp_client.call_tool(
        "apply_stock_split",
        {"file": "main", "uuid": "a1", "ex_date": "2024-06-10", "new_shares": "4", "old_shares": "1",
         "adjust_prices": False, "dry_run": True},
    )
    assert sent_json(route) == {
        "exDate": "2024-06-10",
        "newShares": "4",
        "oldShares": "1",
        "adjustTransactions": True,
        "adjustPrices": False,
    }
    assert sent_query(route) == {"dry_run": "true"}
    assert result.data["transactionsAffected"] == 4


async def test_stock_split_locked_retried(api, sleeps):
    locked = problem(423, "user-interaction", "User interaction in progress", headers={"Retry-After": "5"})
    route = api.post(f"{M}/instruments/a1/actions/split").mock(side_effect=[locked, httpx.Response(200, json={})])
    await actions.apply_stock_split("a1", "2024-06-10", "3", "2", "main")
    assert route.call_count == 2 and sleeps == [5.0]
