import httpx
import pytest
from fastmcp.exceptions import ToolError

from pp_mcp.tools import actions

from conftest import F, problem, sent_json, sent_query

M = "/v1/files/main"
RUNNING = {"jobId": "j1", "kind": "update-quotes", "status": "running", "startedAt": "2026-09-24T08:00:00Z"}
SUMMARY = {
    "instruments": 3,
    "latestUpdated": 2,
    "historicUpdated": 1,
    "updated": [
        {"uuid": "a1", "name": "Apple", "latest": True, "latestDate": "2026-09-23", "historicPricesAdded": 5},
        {"uuid": "s1", "name": "SAP", "latest": True, "latestDate": "2026-09-23", "historicPricesAdded": 0},
    ],
    "dirty": True,
}
DONE = RUNNING | {"status": "done", "finishedAt": "2026-09-24T08:00:07Z", "summary": SUMMARY}


async def test_update_quotes_polls_until_done_through_mcp(api, one_file, mcp_client, sleeps):
    start = api.post(f"{F}/actions/update-quotes").respond(
        202, json=RUNNING, headers={"Location": f"{F}/jobs/j1"}
    )
    poll = api.get(f"{F}/jobs/j1").mock(side_effect=[httpx.Response(200, json=RUNNING), httpx.Response(200, json=DONE)])
    result = await mcp_client.call_tool("update_quotes", {"instruments": ["a1", "s1"], "targets": ["latest"]})
    assert sent_json(start) == {"targets": ["latest"], "instruments": ["a1", "s1"]}
    assert sent_query(start) == {"wait": "30"}
    assert poll.call_count == 2
    assert sleeps == [1.0, 1.0]
    assert result.data == DONE


async def test_update_quotes_done_within_wait_answers_200(api):
    start = api.post(f"{M}/actions/update-quotes").respond(200, json=DONE)
    result = await actions.update_quotes("main", wait_seconds=120)
    assert sent_json(start) == {"targets": ["latest", "historic"]}
    assert sent_query(start) == {"wait": "60"}
    assert result == DONE


async def test_update_quotes_gives_up_after_wait_seconds(api, sleeps):
    api.post(f"{M}/actions/update-quotes").respond(202, json=RUNNING)
    poll = api.get(f"{M}/jobs/j1").respond(200, json=RUNNING)
    result = await actions.update_quotes("main", wait_seconds=3)
    assert poll.call_count == 3
    assert result["status"] == "running"
    assert "get_job(job_id='j1')" in result["note"]


async def test_update_quotes_no_wait(api):
    start = api.post(f"{M}/actions/update-quotes").respond(202, json=RUNNING)
    result = await actions.update_quotes("main", wait_seconds=0)
    assert sent_query(start) == {}
    assert result["status"] == "running"


async def test_update_quotes_failed_job_is_terminal(api):
    api.post(f"{M}/actions/update-quotes").respond(202, json=RUNNING)
    failed = RUNNING | {"status": "failed", "finishedAt": "2026-09-24T08:00:03Z", "error": "cancelled"}
    api.get(f"{M}/jobs/j1").respond(200, json=failed)
    result = await actions.update_quotes("main", wait_seconds=5)
    assert result == failed


async def test_update_quotes_dry_run(api):
    preview = {"dryRun": True, "instruments": [{"uuid": "a1", "name": "Apple"}], "targets": ["latest"]}
    start = api.post(f"{M}/actions/update-quotes").respond(200, json=preview)
    result = await actions.update_quotes("main", instruments=["a1"], targets=["latest"], dry_run=True)
    assert sent_query(start) == {"dry_run": "true"}
    assert result == preview


async def test_update_quotes_rejects_empty_lists():
    with pytest.raises(ToolError, match="instruments: omit it"):
        await actions.update_quotes("main", instruments=[])
    with pytest.raises(ToolError, match="targets"):
        await actions.update_quotes("main", targets=[])


async def test_update_quotes_unknown_instrument(api):
    api.post(f"{M}/actions/update-quotes").mock(
        return_value=problem(422, "validation", "Validation failed",
                             errors=[{"field": "instruments[0]", "code": "unknown-reference",
                                      "message": "zz is not an instrument of the file"}])
    )
    with pytest.raises(ToolError, match=r"instruments\[0\] \(unknown-reference\): zz is not"):
        await actions.update_quotes("main", instruments=["zz"])


async def test_get_job(api):
    api.get(f"{M}/jobs/j1").respond(200, json=DONE)
    result = await actions.get_job("j1", "main")
    assert result["status"] == "done" and result["summary"]["latestUpdated"] == 2


async def test_stock_split(api, mcp_client):
    route = api.post(f"{M}/instruments/a1/actions/split").respond(
        200,
        json={
            "dryRun": True,
            "instrument": "a1",
            "event": {"date": "2024-06-10", "type": "stock-split", "details": "4:1"},
            "adjustTransactions": True,
            "adjustPrices": False,
            "transactionsAffected": 4,
            "pricesAffected": 0,
            "transactions": [
                {"uuid": "t1", "date": "2024-01-02T00:00", "type": "buy", "sharesBefore": 10, "sharesAfter": 40}
            ],
        },
    )
    result = await mcp_client.call_tool(
        "apply_stock_split",
        {"file": "main", "uuid": "a1", "ex_date": "2024-06-10", "new_shares": "4", "old_shares": "1",
         "adjust_prices": False, "client_ref": "split-1", "dry_run": True},
    )
    assert sent_json(route) == {
        "exDate": "2024-06-10",
        "newShares": "4",
        "oldShares": "1",
        "adjustTransactions": True,
        "adjustPrices": False,
        "clientRef": "split-1",
    }
    assert sent_query(route) == {"dry_run": "true"}
    assert result.data["transactionsAffected"] == 4
    assert result.data["transactions"][0]["sharesAfter"] == 40


async def test_stock_split_locked_retried(api, sleeps):
    locked = problem(423, "user-interaction", "User interaction in progress, retry later", headers={"Retry-After": "5"})
    route = api.post(f"{M}/instruments/a1/actions/split").mock(side_effect=[locked, httpx.Response(200, json={})])
    await actions.apply_stock_split("a1", "2024-06-10", "3", "2", "main")
    assert route.call_count == 2 and sleeps == [5.0]
