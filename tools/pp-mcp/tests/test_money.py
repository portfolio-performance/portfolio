import pytest
from decimal import Decimal
from fastmcp.exceptions import ToolError

from pp_mcp.money import MONEY, QUOTE, RATE, SHARES, date_str, decimal_str, to_jsonable


@pytest.mark.parametrize(
    "value,scale,expected",
    [
        ("12.34", MONEY, "12.34"),
        ("12", MONEY, "12"),
        (" 12.30 ", MONEY, "12.30"),
        ("+5", MONEY, "5"),
        ("-4.90", MONEY, "-4.90"),
        ("0.12345678", SHARES, "0.12345678"),
        ("101.12345678", QUOTE, "101.12345678"),
        ("1.0850123456", RATE, "1.0850123456"),
        (7, SHARES, "7"),
        (None, MONEY, None),
    ],
)
def test_decimal_accepts(value, scale, expected):
    assert decimal_str(value, scale, "x") == expected


@pytest.mark.parametrize(
    "value,scale",
    [
        ("12.345", MONEY),
        ("0.123456789", SHARES),
        ("1.08501234567", RATE),
        ("1e2", MONEY),
        ("1,5", MONEY),
        ("1.000,50", MONEY),
        ("", MONEY),
        ("abc", MONEY),
        ("NaN", MONEY),
        (".5", MONEY),
    ],
)
def test_decimal_rejects(value, scale):
    with pytest.raises(ToolError, match="^amount: "):
        decimal_str(value, scale, "amount")


def test_decimal_rejects_float():
    with pytest.raises(ToolError, match="not a float"):
        decimal_str(12.5, MONEY, "amount")  # type: ignore[arg-type]


def test_dates():
    assert date_str("2026-03-02", "d") == "2026-03-02"
    assert date_str("2026-03-02T09:30", "d", allow_time=True) == "2026-03-02T09:30"
    with pytest.raises(ToolError):
        date_str("2026-03-02T09:30", "d")
    with pytest.raises(ToolError):
        date_str("02.03.2026", "d")
    with pytest.raises(ToolError):
        date_str("20260302", "d")


def test_to_jsonable_keeps_decimal_text():
    assert to_jsonable({"a": [Decimal("12.30"), 3, None]}) == {"a": ["12.30", 3, None]}
