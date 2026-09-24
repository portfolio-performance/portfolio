import pytest
from fastmcp.exceptions import ToolError

from pp_mcp.tools import watchlists as wl

from conftest import F, problem, sent_json, sent_query

W = "/v1/files/main/watchlists"
TECH = {"name": "Tech", "instruments": [{"uuid": "a1", "name": "Apple"}]}


async def test_list_watchlists_through_mcp(api, one_file, mcp_client):
    api.get(f"{F}/watchlists").respond(200, json={"items": [TECH]})
    result = await mcp_client.call_tool("list_watchlists", {})
    assert result.data["items"][0]["instruments"][0]["uuid"] == "a1"


async def test_get_watchlist(api):
    api.get(f"{W}/My%20list%2F2").respond(200, json=TECH)
    assert await wl.get_watchlist("My list/2", "main") == TECH


async def test_create_watchlist(api):
    route = api.post(W).respond(201, json=TECH)
    await wl.create_watchlist("Tech", "main", instruments=["a1", "s1"], client_ref="wl-1", dry_run=True)
    assert sent_json(route) == {"name": "Tech", "instruments": ["a1", "s1"], "clientRef": "wl-1"}
    assert sent_query(route) == {"dry_run": "true"}


async def test_create_watchlist_duplicate(api):
    api.post(W).mock(
        return_value=problem(
            422, "validation", "Validation failed",
            errors=[{"field": "name", "code": "already-exists", "message": "Tech exists"}],
        )
    )
    with pytest.raises(ToolError, match=r"name \(already-exists\): Tech exists"):
        await wl.create_watchlist("Tech", "main")


async def test_update_encodes_name_in_path(api):
    route = api.patch(f"{W}/My%20list%2F2").respond(200, json={"name": "New", "instruments": []})
    await wl.update_watchlist("My list/2", "main", new_name="New")
    assert route.calls.last.request.url.raw_path == b"/v1/files/main/watchlists/My%20list%2F2"
    assert sent_json(route) == {"name": "New"}


async def test_update_replaces_instruments(api):
    route = api.patch(f"{W}/Tech").respond(200, json={"name": "Tech", "instruments": []})
    await wl.update_watchlist("Tech", "main", instruments=[], dry_run=True)
    assert sent_json(route) == {"instruments": []} and sent_query(route) == {"dry_run": "true"}
    with pytest.raises(ToolError, match="nothing to update"):
        await wl.update_watchlist("Tech", "main")


async def test_ambiguous_name(api):
    api.get(f"{W}/Tech").mock(return_value=problem(409, "ambiguous-name", "Several entities have this name"))
    with pytest.raises(ToolError, match="^ambiguous-name: "):
        await wl.get_watchlist("Tech", "main")


async def test_delete_add_remove(api):
    api.delete(f"{W}/Tech").respond(204)
    assert await wl.delete_watchlist("Tech", "main") == {"deleted": True, "name": "Tech"}
    # PUT and DELETE of a member answer 200 with the watchlist
    put = api.put(f"{W}/Tech/instruments/s1").respond(
        200, json={"name": "Tech", "instruments": [*TECH["instruments"], {"uuid": "s1", "name": "SAP"}], "dryRun": True}
    )
    result = await wl.add_to_watchlist("Tech", "s1", "main", dry_run=True)
    assert result["instruments"][1] == {"uuid": "s1", "name": "SAP"}
    assert sent_query(put) == {"dry_run": "true"}
    assert put.calls.last.request.content == b""
    api.delete(f"{W}/Tech/instruments/a1").respond(200, json={"name": "Tech", "instruments": []})
    assert await wl.remove_from_watchlist("Tech", "a1", "main") == {"name": "Tech", "instruments": []}
