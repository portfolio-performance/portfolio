"""Shared parameter types and helpers of the tool modules."""

import re
from decimal import Decimal
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
InvestmentAccountFilter = Annotated[
    list[str] | None, Field(description="Restrict to these investment accounts (UUIDs)")
]
CashAccountFilter = Annotated[list[str] | None, Field(description="Restrict to these cash accounts (UUIDs)")]


def uuid_list(values: list[str] | None) -> str | None:
    """A report filter query value: the UUIDs as a comma-separated list, None when empty."""
    if not values:
        return None
    return ",".join(values)


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


def no_floats(value: Any, field: str) -> Any:
    """Reject binary floats anywhere in a free-form value (N6); returns the value."""
    if isinstance(value, float):
        raise ToolError(f'{field}: pass numbers as decimal strings such as "12.34", not floats')
    if isinstance(value, dict):
        for k, v in value.items():
            no_floats(v, f"{field}.{k}")
    elif isinstance(value, list):
        for n, v in enumerate(value):
            no_floats(v, f"{field}[{n}]")
    return value


NUMERIC_ATTRIBUTE_TYPES = {"amount", "quote", "shares", "percent", "number"}
_PLAIN_DECIMAL = re.compile(r"^[+-]?\d+(?:\.\d+)?$")


def typed_attributes(attributes: dict[str, Any] | None, numeric_ids: set[str] | None) -> dict[str, Any] | None:
    """Custom attribute values as the API expects them: numeric attributes as JSON numbers.

    Numeric values are passed to the tools as decimal strings (N6) and sent as exact JSON
    numbers. `numeric_ids` are the ids of the numeric attributes; None means unknown, and
    then every plain decimal string is treated as a number.
    """
    if attributes is None:
        return None
    no_floats(attributes, "attributes")
    typed: dict[str, Any] = {}
    for key, value in attributes.items():
        numeric = key in numeric_ids if numeric_ids is not None else isinstance(value, str)
        if numeric and isinstance(value, str) and _PLAIN_DECIMAL.match(value.strip()):
            typed[key] = Decimal(value.strip())
        elif numeric and isinstance(value, int) and not isinstance(value, bool):
            typed[key] = Decimal(value)
        else:
            typed[key] = value
    return typed


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
