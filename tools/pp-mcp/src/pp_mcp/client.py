"""Async HTTP client for the Portfolio Performance REST API."""

import asyncio
import json
import re
from decimal import Decimal
from typing import Any
from urllib.parse import quote

import httpx
from fastmcp.exceptions import ToolError

from pp_mcp.config import Settings
from pp_mcp.errors import PPApiError

MAX_LOCKED_RETRIES = 3
MAX_RETRY_AFTER_SECONDS = 5.0
DEFAULT_TIMEOUT_SECONDS = 60.0

# Indirection so tests can replace the wait between 423 retries.
_sleep = asyncio.sleep


def _retry_after(response: httpx.Response) -> float:
    try:
        seconds = float(response.headers.get("Retry-After", "1"))
    except ValueError:
        seconds = 1.0
    return max(0.0, min(seconds, MAX_RETRY_AFTER_SECONDS))


def _parse_json(text: str) -> Any:
    return json.loads(text, parse_float=Decimal)


_NUMBER_MARK = "\x00pp-number:"
_NUMBER_TOKEN = re.compile(r'"\\u0000pp-number:([-+0-9.]+)"')


def _dump_json(body: Any) -> str:
    """JSON text of a request body; a `Decimal` becomes an exact JSON number (never a float)."""

    def default(value: Any) -> Any:
        if isinstance(value, Decimal):
            return _NUMBER_MARK + format(value, "f")
        raise TypeError(f"{type(value).__name__} is not JSON serializable")

    return _NUMBER_TOKEN.sub(r"\1", json.dumps(body, default=default))


def _encode_query(params: dict[str, Any] | None) -> dict[str, str] | None:
    if not params:
        return None
    encoded = {}
    for key, value in params.items():
        if value is None:
            continue
        if isinstance(value, bool):
            encoded[key] = "true" if value else "false"
        else:
            encoded[key] = str(value)
    return encoded or None


def file_path(file: str, *segments: str) -> str:
    """`/v1/files/{file}/seg1/seg2`, each part percent-encoded as one path segment."""
    parts = ["v1", "files", file, *segments]
    return "/" + "/".join(quote(str(p), safe="") for p in parts)


class PPClient:
    """Bearer-authenticated client; problem+json answers raise `PPApiError`."""

    def __init__(self, settings: Settings | None = None):
        self.settings = settings or Settings.from_env()
        headers = {"Accept": "application/json"}
        if self.settings.api_token:
            headers["Authorization"] = f"Bearer {self.settings.api_token}"
        self._http = httpx.AsyncClient(
            base_url=self.settings.api_url, headers=headers, timeout=DEFAULT_TIMEOUT_SECONDS
        )

    async def __aenter__(self) -> "PPClient":
        return self

    async def __aexit__(self, *exc: object) -> None:
        await self._http.aclose()

    async def request(
        self,
        method: str,
        path: str,
        *,
        params: dict[str, Any] | None = None,
        body: Any = None,
        content_type: str = "application/json",
        timeout: float | None = None,
    ) -> Any:
        headers = {}
        content = None
        if body is not None:
            content = _dump_json(body).encode("utf-8")
            headers["Content-Type"] = content_type
        query = _encode_query(params)
        attempt = 0
        while True:
            try:
                response = await self._http.request(
                    method,
                    path,
                    params=query,
                    content=content,
                    headers=headers,
                    timeout=timeout if timeout is not None else httpx.USE_CLIENT_DEFAULT,
                )
            except httpx.TimeoutException as e:
                raise ToolError(f"timeout: Portfolio Performance did not answer {method} {path} in time ({e})") from e
            except httpx.TransportError as e:
                raise ToolError(
                    f"unreachable: cannot reach Portfolio Performance at {self.settings.api_url} — "
                    "is PP running with the REST API enabled (Preferences → REST API)? "
                    f"Set PP_API_URL if it listens elsewhere. ({e})"
                ) from e
            if response.status_code == 423 and attempt < MAX_LOCKED_RETRIES:
                attempt += 1
                await _sleep(_retry_after(response))
                continue
            return self._handle(response)

    def _handle(self, response: httpx.Response) -> Any:
        text = response.text
        if response.status_code >= 400:
            problem: dict[str, Any]
            try:
                parsed = _parse_json(text) if text else {}
                problem = parsed if isinstance(parsed, dict) else {}
            except ValueError:
                problem = {}
            if not problem.get("title"):
                problem.setdefault("title", response.reason_phrase or f"HTTP {response.status_code}")
                if text and not problem.get("detail") and "json" not in response.headers.get("content-type", ""):
                    problem["detail"] = text[:500]
            raise PPApiError(response.status_code, problem)
        if response.status_code == 204 or not text:
            return None
        if "json" in response.headers.get("content-type", "application/json"):
            return _parse_json(text)
        return text

    async def get(self, path: str, **kw: Any) -> Any:
        return await self.request("GET", path, **kw)

    async def post(self, path: str, **kw: Any) -> Any:
        return await self.request("POST", path, **kw)

    async def put(self, path: str, **kw: Any) -> Any:
        return await self.request("PUT", path, **kw)

    async def patch(self, path: str, **kw: Any) -> Any:
        kw.setdefault("content_type", "application/merge-patch+json")
        return await self.request("PATCH", path, **kw)

    async def delete(self, path: str, **kw: Any) -> Any:
        return await self.request("DELETE", path, **kw)

    async def resolve_file(self, file: str | None) -> str:
        """The given file id/alias, or the only listed file when `file` is omitted."""
        if file:
            return file
        listed = await self.get("/v1/files")
        items = (listed or {}).get("items", [])
        if len(items) == 1:
            return items[0]["id"]
        if not items:
            raise ToolError(
                "no-file: no portfolio file is open and enabled for the REST API — open the file in "
                "Portfolio Performance and enable it in Preferences → REST API"
            )
        names = ", ".join(f"{i.get('alias') or i.get('label')} (id {i['id']})" for i in items)
        raise ToolError(f"file-required: several files are available, pass `file` as one of: {names}")
