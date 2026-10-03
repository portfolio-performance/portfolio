"""Decimal and date handling (N6): numbers are decimal strings, never binary floats.

Every numeric tool parameter is validated against the precision PP stores and is
sent to the API as a JSON string, which the REST API accepts in place of a number.
Responses are parsed with `parse_float=Decimal` and returned as decimal strings.
"""

import re
from datetime import date, datetime
from decimal import Decimal
from typing import Any

from fastmcp.exceptions import ToolError

MONEY = 2  # Values.Amount / Money
SHARES = 8  # Values.Share
QUOTE = 8  # Values.Quote
RATE = 10  # exchange rates
WEIGHT = 2  # taxonomy weights in percent

_DECIMAL = re.compile(r"^[+-]?(\d+)(?:\.(\d+))?$")


def decimal_str(value: str | int | None, scale: int, field: str) -> str | None:
    """Validate a decimal literal with at most `scale` fraction digits.

    Returns the canonical string (no leading '+', no blanks) or None for None.
    Scientific notation, thousands separators, decimal commas and floats are rejected.
    """
    if value is None:
        return None
    if isinstance(value, (bool, float)):
        raise ToolError(f'{field}: pass the number as a decimal string such as "12.34", not a float')
    text = str(value).strip()
    match = _DECIMAL.match(text)
    if not match:
        raise ToolError(f'{field}: {value!r} is not a plain decimal number such as "12.34"')
    fraction = match.group(2) or ""
    if len(fraction) > scale:
        raise ToolError(f"{field}: {value!r} has more than {scale} decimal places")
    return text.lstrip("+")


def date_str(value: str | None, field: str, allow_time: bool = False) -> str | None:
    """Validate `YYYY-MM-DD` (or `YYYY-MM-DDTHH:MM` when `allow_time`)."""
    if value is None:
        return None
    text = value.strip()
    try:
        if allow_time and "T" in text:
            datetime.strptime(text, "%Y-%m-%dT%H:%M")
        else:
            date.fromisoformat(text)
            if len(text) != 10:
                raise ValueError(text)
    except ValueError:
        shape = "YYYY-MM-DD or YYYY-MM-DDTHH:MM" if allow_time else "YYYY-MM-DD"
        raise ToolError(f"{field}: {value!r} is not a date in the form {shape}") from None
    return text


def to_jsonable(value: Any) -> Any:
    """Turn parsed API JSON into tool output: Decimals become plain decimal strings."""
    if isinstance(value, Decimal):
        return format(value, "f")
    if isinstance(value, dict):
        return {k: to_jsonable(v) for k, v in value.items()}
    if isinstance(value, list):
        return [to_jsonable(v) for v in value]
    return value
