import json
from typing import Any

import httpx
import pytest
import respx
from fastmcp import Client

import pp_mcp.client as client_module
from pp_mcp.server import mcp

BASE = "http://pp.test:5712"
TOKEN = "test-token"
FILE_ID = "5f3c1e2a-0b7d-4a1e-9c2f-8d6b3a1e0f42"
F = f"/v1/files/{FILE_ID}"
PROBLEM = "https://portfolio-performance.info/rest/problems/"


@pytest.fixture(autouse=True)
def env(monkeypatch):
    monkeypatch.setenv("PP_API_URL", BASE)
    monkeypatch.setenv("PP_API_TOKEN", TOKEN)


@pytest.fixture(autouse=True)
def sleeps(monkeypatch) -> list[float]:
    """Records the waits between 423 retries instead of sleeping."""
    recorded: list[float] = []

    async def fake_sleep(seconds: float) -> None:
        recorded.append(seconds)

    monkeypatch.setattr(client_module, "_sleep", fake_sleep)
    return recorded


@pytest.fixture
def api():
    with respx.mock(base_url=BASE, assert_all_called=True) as router:
        yield router


@pytest.fixture
def one_file(api):
    """GET /v1/files answers exactly one file, so `file` may be omitted."""
    return api.get("/v1/files").respond(
        200, json={"items": [{"id": FILE_ID, "alias": "main", "label": "p.xml", "path": "/p.xml", "dirty": False}]}
    )


@pytest.fixture
async def mcp_client():
    async with Client(mcp) as c:
        yield c


def problem(status: int, kind: str, title: str, detail: str | None = None, errors=None, headers=None):
    body: dict[str, Any] = {"type": PROBLEM + kind, "title": title, "status": status}
    if detail:
        body["detail"] = detail
    if errors:
        body["errors"] = errors
    return httpx.Response(
        status,
        content=json.dumps(body).encode(),
        headers={"Content-Type": "application/problem+json", **(headers or {})},
    )


def raw_json(text: str, status: int = 200) -> httpx.Response:
    """A JSON response given as literal text, so decimal literals are kept exactly."""
    return httpx.Response(status, content=text.encode(), headers={"Content-Type": "application/json"})


def sent_json(route) -> Any:
    """Body of the last request a respx route received."""
    return json.loads(route.calls.last.request.content)


def sent_query(route) -> dict[str, str]:
    return dict(route.calls.last.request.url.params)
