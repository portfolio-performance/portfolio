import httpx
import pytest
from fastmcp.exceptions import ToolError

from pp_mcp.tools import transactions as tx

from conftest import F, problem, raw_json, sent_json, sent_query

T = "/v1/files/main/transactions"


# the 201 answer of the buyInForeignCurrency example in openapi.yaml (trimmed)
BUY_CREATED = (
    '{"uuid":"2b3c4d5e-6f7a-4b1c-8d2e-3f4a5b6c7d8e","date":"2026-03-02T09:30","type":"buy",'
    '"value":{"value":1157.32,"currency":"EUR"},"grossValue":{"value":1152.42,"currency":"EUR"},'
    '"fees":{"value":4.9,"currency":"EUR"},"taxes":{"value":0,"currency":"EUR"},"shares":12.5,'
    '"units":[{"type":"fee","amount":{"value":4.9,"currency":"EUR"}},'
    '{"type":"gross-value","amount":{"value":1152.42,"currency":"EUR"},"forex":{"value":1265,"currency":"USD"},'
    '"exchangeRate":0.911}],"source":"api:broker-2026-000123","clientRef":"broker-2026-000123",'
    '"linked":{"uuid":"3c4d5e6f-7a8b-4c1d-8e2f-3a4b5c6d7e8f","type":"buy",'
    '"owner":{"uuid":"c4b2a1e0-3f6d-4a2e-8b1c-5d7f9a0e2c46","name":"Broker Cash","type":"cash-account"}}}'
)


@pytest.fixture
def created(api):
    return api.post(T).mock(return_value=raw_json(BUY_CREATED, 201))


async def test_list_transactions_filters_and_limit(api, one_file):
    route = api.get(f"{F}/transactions").respond(200, json={"items": [{"uuid": "a"}, {"uuid": "b"}, {"uuid": "c"}]})
    result = await tx.list_transactions(
        from_date="2026-01-01",
        to_date="2026-06-30",
        type=["buy", "sell"],
        instrument="i1",
        cash_account="c1",
        investment_account="p1",
        limit=2,
    )
    assert sent_query(route) == {
        "from": "2026-01-01",
        "to": "2026-06-30",
        "type": "buy,sell",
        "instrument": "i1",
        "cashAccount": "c1",
        "investmentAccount": "p1",
    }
    assert result == {"items": [{"uuid": "a"}, {"uuid": "b"}], "total": 3, "truncated": True}


async def test_list_transactions_rejects_unknown_type():
    with pytest.raises(ToolError, match="type: 'purchase'"):
        await tx.list_transactions("main", type=["buy", "purchase"])


async def test_get_transaction_through_mcp(api, mcp_client):
    api.get(f"{T}/t1").mock(return_value=raw_json('{"uuid":"t1","shares":12.50000000}'))
    result = await mcp_client.call_tool("get_transaction", {"file": "main", "uuid": "t1"})
    assert result.data == {"uuid": "t1", "shares": "12.50000000"}


async def test_create_buy_body_and_decimal_strings_through_mcp(created, mcp_client):
    result = await mcp_client.call_tool(
        "create_buy",
        {
            "file": "main",
            "investment_account": "p1",
            "cash_account": "c1",
            "instrument": "i1",
            "date": "2026-03-02T09:30",
            "shares": "12.5",
            "quote": "101.2",
            "fees": "4.90",
            "taxes": "0",
            "forex_fees": "1.00",
            "forex_taxes": "0.50",
            "exchange_rate": "0.911",
            "total": "1157.32",
            "note": "n",
            "client_ref": "broker-2026-000123",
        },
    )
    assert sent_json(created) == {
        "type": "buy",
        "date": "2026-03-02T09:30",
        "investmentAccount": "p1",
        "cashAccount": "c1",
        "instrument": "i1",
        "shares": "12.5",
        "quote": "101.2",
        "amount": "1157.32",
        "exchangeRate": "0.911",
        "fees": "4.90",
        "taxes": "0",
        "forexFees": "1.00",
        "forexTaxes": "0.50",
        "note": "n",
        "clientRef": "broker-2026-000123",
    }
    assert b'"shares": "12.5"' in created.calls.last.request.content
    assert sent_query(created) == {}
    assert result.data["value"]["value"] == "1157.32"
    assert result.data["units"][1]["exchangeRate"] == "0.911"
    assert result.data["linked"]["uuid"] == "3c4d5e6f-7a8b-4c1d-8e2f-3a4b5c6d7e8f"


async def test_create_sell_with_gross_value_dry_run(created):
    await tx.create_sell("p1", "c1", "i1", "2026-03-02", "5", "main", gross_value="600.00", dry_run=True)
    assert sent_json(created) == {
        "type": "sell",
        "date": "2026-03-02",
        "investmentAccount": "p1",
        "cashAccount": "c1",
        "instrument": "i1",
        "shares": "5",
        "grossValue": "600.00",
    }
    assert sent_query(created) == {"dry_run": "true"}


async def test_buy_needs_a_price_or_total():
    with pytest.raises(ToolError, match="at least one of quote, gross_value or total"):
        await tx.create_buy("p1", "c1", "i1", "2026-03-02", "5", "main")


async def test_buy_with_total_only(created):
    await tx.create_buy("p1", "c1", "i1", "2026-03-02", "5", "main", total="505.00", fees="5.00")
    assert sent_json(created) == {
        "type": "buy",
        "date": "2026-03-02",
        "investmentAccount": "p1",
        "cashAccount": "c1",
        "instrument": "i1",
        "shares": "5",
        "amount": "505.00",
        "fees": "5.00",
    }


async def test_buy_with_quote_and_gross_value(created):
    await tx.create_buy("p1", "c1", "i1", "2026-03-02", "5", "main", quote="1", gross_value="5.00")
    assert sent_json(created)["quote"] == "1" and sent_json(created)["grossValue"] == "5.00"


@pytest.mark.parametrize(
    "kwargs,message",
    [
        ({"shares": "1.123456789"}, "shares: .* more than 8"),
        ({"fees": "4.901"}, "fees: .* more than 2"),
        ({"exchange_rate": "1.12345678901"}, "exchange_rate: .* more than 10"),
        ({"shares": "1e3"}, "shares: .* not a plain decimal"),
        ({"date": "2026-3-2"}, "date: .* not a date"),
    ],
)
async def test_buy_validates_precision(kwargs, message):
    args = {
        "investment_account": "p1",
        "cash_account": "c1",
        "instrument": "i1",
        "date": "2026-03-02",
        "shares": "1",
        "file": "main",
        "quote": "1",
    } | kwargs
    with pytest.raises(ToolError, match=message):
        await tx.create_buy(**args)


async def test_validation_problem_through_mcp(api, mcp_client):
    api.post(T).mock(
        return_value=problem(
            422,
            "validation",
            "Validation failed",
            errors=[
                {"field": "amount", "code": "total-mismatch", "message": "expected 1377.60"},
                {"field": "exchangeRate", "code": "exchange-rate-required", "message": "no rate for USD/EUR"},
            ],
        )
    )
    with pytest.raises(ToolError) as e:
        await mcp_client.call_tool(
            "create_buy",
            {
                "file": "main",
                "investment_account": "p1",
                "cash_account": "c1",
                "instrument": "i1",
                "date": "2026-03-02",
                "shares": "1",
                "quote": "1",
            },
        )
    assert str(e.value).endswith(
        "validation: Validation failed\n"
        "amount (total-mismatch): expected 1377.60\n"
        "exchangeRate (exchange-rate-required): no rate for USD/EUR"
    )


async def test_create_retries_423(api, sleeps):
    locked = problem(423, "user-interaction", "User interaction in progress", headers={"Retry-After": "1"})
    route = api.post(T).mock(side_effect=[locked, httpx.Response(201, json={"uuid": "t1"})])
    result = await tx.create_cash_transaction("c1", "deposit", "2026-01-01", "100", "main")
    assert result == {"uuid": "t1"}
    assert route.call_count == 2 and sleeps == [1.0]


async def test_create_delivery(created):
    await tx.create_delivery(
        "p1", "i1", "outbound", "2026-01-05", "3", "main", quote="10.5", total="28.50", currency="USD",
        exchange_rate="0.9", fees="1.00", taxes="2.00", forex_fees="0.50", forex_taxes="0.25", note="gift",
        client_ref="d1",
    )
    assert sent_json(created) == {
        "type": "delivery-outbound",
        "date": "2026-01-05",
        "investmentAccount": "p1",
        "instrument": "i1",
        "shares": "3",
        "quote": "10.5",
        "amount": "28.50",
        "currency": "USD",
        "exchangeRate": "0.9",
        "fees": "1.00",
        "taxes": "2.00",
        "forexFees": "0.50",
        "forexTaxes": "0.25",
        "note": "gift",
        "clientRef": "d1",
    }


async def test_create_dividend(created):
    await tx.create_dividend(
        "c1", "i1", "2026-07-20", "main", gross_value="50.00", ex_date="2026-07-15", shares="10", taxes="7.50",
        client_ref="div-1", dry_run=True,
    )
    # the "dividend" request example of openapi.yaml, plus clientRef
    assert sent_json(created) == {
        "type": "dividends",
        "date": "2026-07-20",
        "exDate": "2026-07-15",
        "cashAccount": "c1",
        "instrument": "i1",
        "shares": "10",
        "grossValue": "50.00",
        "taxes": "7.50",
        "clientRef": "div-1",
    }
    assert sent_query(created) == {"dry_run": "true"}


async def test_create_dividend_net_amount_and_forex(created):
    await tx.create_dividend(
        "c1", "i1", "2026-07-20", "main", total="42.50", exchange_rate="0.92", forex_taxes="1.00", forex_fees="0.20"
    )
    assert sent_json(created) == {
        "type": "dividends",
        "date": "2026-07-20",
        "cashAccount": "c1",
        "instrument": "i1",
        "amount": "42.50",
        "exchangeRate": "0.92",
        "forexTaxes": "1.00",
        "forexFees": "0.20",
    }


async def test_create_dividend_needs_gross_or_total():
    with pytest.raises(ToolError, match="gross_value and/or total"):
        await tx.create_dividend("c1", "i1", "2026-07-20", "main")


@pytest.mark.parametrize(
    "kind,wire",
    [("interest_charge", "interest-charge"), ("fees_refund", "fees-refund"), ("tax_refund", "tax-refund"),
     ("removal", "removal")],
)
async def test_cash_transaction_kinds(created, kind, wire):
    await tx.create_cash_transaction("c1", kind, "2026-01-01", "12.34", "main", instrument="i1", note="x")
    assert sent_json(created) == {
        "type": wire, "date": "2026-01-01", "cashAccount": "c1", "amount": "12.34", "instrument": "i1", "note": "x"
    }


async def test_interest_with_taxes(created):
    await tx.create_cash_transaction("c1", "interest", "2026-01-01", "10.00", "main", taxes="2.64")
    assert sent_json(created)["taxes"] == "2.64"


async def test_deposit_matches_openapi_example(created):
    await tx.create_cash_transaction("c4b2a1e0-3f6d-4a2e-8b1c-5d7f9a0e2c46", "deposit", "2026-01-02", "1000", "main")
    assert sent_json(created) == {
        "type": "deposit",
        "date": "2026-01-02",
        "cashAccount": "c4b2a1e0-3f6d-4a2e-8b1c-5d7f9a0e2c46",
        "amount": "1000",
    }


async def test_fee_with_foreign_instrument_exchange_rate(created):
    await tx.create_cash_transaction("c1", "fees", "2026-01-01", "3.00", "main", instrument="i1", exchange_rate="0.9")
    assert sent_json(created)["exchangeRate"] == "0.9"


async def test_create_transfer(created):
    await tx.create_transfer("c1", "c2", "2026-05-01", "500.00", "main", target_amount="545.00", client_ref="tr")
    # the "cashTransfer" request example of openapi.yaml: no exchange rate, it follows from the amounts
    assert sent_json(created) == {
        "type": "cash-transfer",
        "date": "2026-05-01",
        "fromCashAccount": "c1",
        "toCashAccount": "c2",
        "amount": "500.00",
        "targetAmount": "545.00",
        "clientRef": "tr",
    }


async def test_create_security_transfer(created):
    await tx.create_security_transfer("p1", "p2", "i1", "2026-02-01", "10", "main", amount="1500.00", note="move")
    assert sent_json(created) == {
        "type": "security-transfer",
        "date": "2026-02-01",
        "fromInvestmentAccount": "p1",
        "toInvestmentAccount": "p2",
        "instrument": "i1",
        "shares": "10",
        "amount": "1500.00",
        "note": "move",
    }


async def test_create_security_transfer_with_quote(created):
    await tx.create_security_transfer("p1", "p2", "i1", "2026-02-01", "10", "main", quote="150.5")
    assert sent_json(created) == {
        "type": "security-transfer",
        "date": "2026-02-01",
        "fromInvestmentAccount": "p1",
        "toInvestmentAccount": "p2",
        "instrument": "i1",
        "shares": "10",
        "quote": "150.5",
    }
    with pytest.raises(ToolError, match="amount and/or quote"):
        await tx.create_security_transfer("p1", "p2", "i1", "2026-02-01", "10", "main")


async def test_update_transaction(api):
    route = api.patch(f"{T}/t1").respond(200, json={"uuid": "t1"})
    await tx.update_transaction(
        "t1", "main", date="2026-03-03", shares="13", fees="5.00", cash_account="c2",
        clear=["note", "ex_date", "exchange_rate"], dry_run=True,
    )
    assert route.calls.last.request.headers["Content-Type"] == "application/merge-patch+json"
    assert sent_json(route) == {
        "date": "2026-03-03",
        "shares": "13",
        "fees": "5.00",
        "cashAccount": "c2",
        "note": None,
        "exDate": None,
        "exchangeRate": None,
    }
    assert sent_query(route) == {"dry_run": "true"}


async def test_update_transaction_needs_a_field():
    with pytest.raises(ToolError, match="nothing to update"):
        await tx.update_transaction("t1", "main")


async def test_delete_transaction(api):
    preview = {"dryRun": True, "removed": [{"uuid": "t1", "type": "buy"}, {"uuid": "t2", "type": "buy"}]}
    route = api.delete(f"{T}/t1").respond(200, json=preview)
    result = await tx.delete_transaction("t1", "main", dry_run=True)
    assert result == preview
    assert sent_query(route) == {"dry_run": "true"}
    api.delete(f"{T}/t1").respond(204)
    assert await tx.delete_transaction("t1", "main") == {"deleted": True, "uuid": "t1"}
