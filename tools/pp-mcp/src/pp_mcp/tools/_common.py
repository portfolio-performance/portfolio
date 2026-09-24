"""Shared parameter types and helpers of the tool modules."""

from typing import Annotated, Any

from fastmcp.exceptions import ToolError
from pydantic import Field

from pp_mcp.client import PPClient, file_path
from pp_mcp.money import to_jsonable

FileParam = Annotated[
    str | None,
    Field(description="PP file id or alias (see list_files). Optional when exactly one file is available."),
]
DryRun = Annotated[
    bool,
    Field(description="Validate and return the fully resolved result without changing anything."),
]
ClientRef = Annotated[
    str | None,
    Field(
        description="Optional idempotency key (max 128 chars). Repeating a create with the same key "
        "returns the object created the first time instead of a duplicate.",
        max_length=128,
    ),
]
Clear = Annotated[
    list[str] | None,
    Field(description="Names of optional fields (tool parameter names) to clear, i.e. set to null."),
]


def api() -> PPClient:
    """A client configured from PP_API_URL / PP_API_TOKEN."""
    return PPClient()


def fpath(file: str, *segments: str) -> str:
    return file_path(file, *segments)


def dry(dry_run: bool, **params: Any) -> dict[str, Any]:
    """Query parameters incl. `dry_run=true` when requested; None values are dropped."""
    out = {k: v for k, v in params.items() if v is not None}
    if dry_run:
        out["dry_run"] = "true"
    return out


def compact(**fields: Any) -> dict[str, Any]:
    """A request body without the fields that are None."""
    return {k: v for k, v in fields.items() if v is not None}


def apply_clear(patch: dict[str, Any], clear: list[str] | None, mapping: dict[str, str]) -> dict[str, Any]:
    """Add explicit nulls for the fields named in `clear` (tool parameter names → wire names)."""
    for name in clear or []:
        wire = mapping.get(name)
        if wire is None:
            raise ToolError(f"clear: {name!r} cannot be cleared; clearable fields: {', '.join(sorted(mapping))}")
        if wire in patch:
            raise ToolError(f"clear: {name!r} is both set and cleared")
        patch[wire] = None
    return patch


def out(value: Any) -> dict[str, Any]:
    """Tool result: Decimals as strings; non-object answers wrapped."""
    value = to_jsonable(value)
    if isinstance(value, dict):
        return value
    return {"result": value}


def deleted(value: Any, **ids: Any) -> dict[str, Any]:
    """Result of a DELETE: the server's body (dry run) or a confirmation for 204."""
    if value is None:
        return {"deleted": True, **ids}
    return out(value)
