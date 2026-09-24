from decimal import Decimal

import httpx
import pytest
from fastmcp.exceptions import ToolError

from pp_mcp.client import PPClient, file_path
from pp_mcp.config import DEFAULT_API_URL, Settings
from pp_mcp.errors import PPApiError

from conftest import BASE, F, FILE_ID, TOKEN, problem, raw_json, sent_json


def test_settings_default(monkeypatch):
    monkeypatch.delenv("PP_API_URL")
    monkeypatch.delenv("PP_API_TOKEN")
    s = Settings.from_env()
    assert s.api_url == DEFAULT_API_URL == "http://127.0.0.1:5712"
    assert s.api_token is None


def test_settings_from_env():
    s = Settings.from_env()
    assert s.api_url == BASE
    assert s.api_token == TOKEN


def test_file_path_encodes_segments():
    assert file_path("main", "watchlists", "My list/2") == "/v1/files/main/watchlists/My%20list%2F2"


async def test_bearer_and_decimal_parsing(api):
    route = api.get(f"{F}/instruments").mock(return_value=raw_json('{"items":[{"price": 0.1, "n": 3}]}'))
    async with PPClient() as pp:
        data = await pp.get(f"{F}/instruments")
    assert route.calls.last.request.headers["Authorization"] == f"Bearer {TOKEN}"
    price = data["items"][0]["price"]
    assert isinstance(price, Decimal) and price == Decimal("0.1")
    assert data["items"][0]["n"] == 3


async def test_body_is_json_and_query_bools(api):
    route = api.post(f"{F}/x").respond(201, json={"ok": True})
    async with PPClient() as pp:
        await pp.post(f"{F}/x", params={"dry_run": True, "skip": None}, body={"amount": "1.10"})
    req = route.calls.last.request
    assert dict(req.url.params) == {"dry_run": "true"}
    assert req.headers["Content-Type"] == "application/json"
    assert sent_json(route) == {"amount": "1.10"}


async def test_patch_uses_merge_patch_content_type(api):
    route = api.patch(f"{F}/x").respond(200, json={})
    async with PPClient() as pp:
        await pp.patch(f"{F}/x", body={"note": None})
    assert route.calls.last.request.headers["Content-Type"] == "application/merge-patch+json"
    assert sent_json(route) == {"note": None}


async def test_problem_is_formatted(api):
    api.post(f"{F}/transactions").mock(
        return_value=problem(
            422,
            "validation",
            "Validation failed",
            detail="2 fields rejected",
            errors=[
                {"field": "shares", "code": "must-be-positive", "message": "shares must be > 0"},
                {"field": "cashAccount", "code": "required", "message": "cashAccount is required"},
            ],
        )
    )
    async with PPClient() as pp:
        with pytest.raises(PPApiError) as e:
            await pp.post(f"{F}/transactions", body={})
    assert str(e.value) == (
        "validation: Validation failed — 2 fields rejected\n"
        "shares (must-be-positive): shares must be > 0\n"
        "cashAccount (required): cashAccount is required"
    )
    assert e.value.status == 422 and e.value.type == "validation"
    assert isinstance(e.value, ToolError)


async def test_problem_without_detail(api):
    api.get(f"{F}/instruments/u").mock(return_value=problem(404, "not-found", "Not found"))
    async with PPClient() as pp:
        with pytest.raises(PPApiError, match=r"^not-found: Not found$"):
            await pp.get(f"{F}/instruments/u")


async def test_401_includes_token_hint(api):
    api.get("/v1/files").mock(return_value=problem(401, "unauthorized", "Missing or invalid bearer token"))
    async with PPClient() as pp:
        with pytest.raises(PPApiError) as e:
            await pp.get("/v1/files")
    text = str(e.value)
    assert text.startswith("unauthorized: Missing or invalid bearer token")
    assert "Preferences → REST API" in text and "PP_API_TOKEN" in text


async def test_non_problem_error(api):
    api.get("/v1/files").respond(502, text="bad gateway")
    async with PPClient() as pp:
        with pytest.raises(PPApiError, match=r"^http-502: Bad Gateway — bad gateway$"):
            await pp.get("/v1/files")


async def test_423_is_retried_honoring_retry_after(api, sleeps):
    locked = problem(423, "user-interaction", "The user is editing", headers={"Retry-After": "2"})
    route = api.post(f"{F}/save").mock(side_effect=[locked, locked, httpx.Response(200, json={"dirty": False})])
    async with PPClient() as pp:
        assert await pp.post(f"{F}/save") == {"dirty": False}
    assert route.call_count == 3
    assert sleeps == [2.0, 2.0]


async def test_423_gives_up_after_three_retries_and_caps_wait(api, sleeps):
    locked = problem(423, "user-interaction", "The user is editing", headers={"Retry-After": "30"})
    route = api.post(f"{F}/save").mock(return_value=locked)
    async with PPClient() as pp:
        with pytest.raises(PPApiError, match="^user-interaction: The user is editing"):
            await pp.post(f"{F}/save")
    assert route.call_count == 4
    assert sleeps == [5.0, 5.0, 5.0]


async def test_unreachable(api):
    api.get("/v1/files").mock(side_effect=httpx.ConnectError("refused"))
    async with PPClient() as pp:
        with pytest.raises(ToolError, match="^unreachable: .*REST API enabled"):
            await pp.get("/v1/files")


async def test_resolve_file_explicit_needs_no_request(api):
    async with PPClient() as pp:
        assert await pp.resolve_file("main") == "main"


async def test_resolve_file_single(one_file):
    async with PPClient() as pp:
        assert await pp.resolve_file(None) == FILE_ID


async def test_resolve_file_none_open(api):
    api.get("/v1/files").respond(200, json={"items": []})
    async with PPClient() as pp:
        with pytest.raises(ToolError, match="^no-file"):
            await pp.resolve_file(None)


async def test_resolve_file_ambiguous(api):
    api.get("/v1/files").respond(
        200,
        json={"items": [{"id": "a", "alias": "one", "label": "1", "path": "/1"}, {"id": "b", "label": "2", "path": "/2"}]},
    )
    async with PPClient() as pp:
        with pytest.raises(ToolError, match=r"^file-required: .*one \(id a\), 2 \(id b\)"):
            await pp.resolve_file(None)
