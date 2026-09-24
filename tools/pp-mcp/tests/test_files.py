import pytest
from fastmcp.exceptions import ToolError

from pp_mcp.tools import files

from conftest import F, FILE_ID, problem, raw_json, sent_json


async def test_list_files_through_mcp(one_file, mcp_client):
    result = await mcp_client.call_tool("list_files", {})
    assert result.data["items"][0]["id"] == FILE_ID
    assert one_file.called


async def test_get_file_default(one_file, api):
    route = api.get(F).respond(200, json={"id": FILE_ID, "dirty": True})
    assert (await files.get_file())["dirty"] is True
    assert route.called


async def test_open_file(api):
    route = api.post("/v1/files/open").respond(200, json={"id": FILE_ID, "path": "/x.xml", "dirty": False})
    result = await files.open_file("/x.xml")
    assert sent_json(route) == {"path": "/x.xml"}
    assert result["id"] == FILE_ID


async def test_save_file_single_file_default_through_mcp(one_file, api, mcp_client):
    route = api.post(f"{F}/save").mock(
        return_value=raw_json('{"id":"%s","dirty":false,"savedAt":"2026-09-24T10:00:00Z"}' % FILE_ID)
    )
    result = await mcp_client.call_tool("save_file", {})
    assert result.data == {"id": FILE_ID, "dirty": False, "savedAt": "2026-09-24T10:00:00Z"}
    assert route.calls.last.request.content == b""


async def test_save_file_by_alias(api):
    route = api.post("/v1/files/main/save").respond(200, json={"dirty": False})
    await files.save_file("main")
    assert route.called


async def test_save_file_locked_reports_user_interaction(api, mcp_client, sleeps):
    api.post("/v1/files/main/save").mock(
        return_value=problem(423, "user-interaction", "User interaction in progress", headers={"Retry-After": "5"})
    )
    with pytest.raises(ToolError, match="user-interaction: User interaction in progress"):
        await mcp_client.call_tool("save_file", {"file": "main"})
    assert sleeps == [5.0, 5.0, 5.0]


async def test_save_failed_through_mcp(api, mcp_client):
    api.post("/v1/files/main/save").mock(
        return_value=problem(500, "save-failed", "Saving the file failed", detail="disk full")
    )
    with pytest.raises(ToolError, match="save-failed: Saving the file failed — disk full"):
        await mcp_client.call_tool("save_file", {"file": "main"})


async def test_save_file_passes_wait_for_updates(api):
    route = api.post("/v1/files/main/save").respond(
        200, json={"dirty": False, "backgroundUpdatesPending": False}
    )
    result = await files.save_file("main", wait_for_updates=0)
    assert route.calls.last.request.url.params["waitForUpdates"] == "0"
    assert result["backgroundUpdatesPending"] is False


async def test_save_file_default_leaves_wait_to_server(api):
    route = api.post("/v1/files/main/save").respond(200, json={"dirty": False})
    await files.save_file("main")
    assert "waitForUpdates" not in route.calls.last.request.url.params

