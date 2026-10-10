import pytest
from fastmcp.exceptions import ToolError

from pp_mcp.tools import accounts

from conftest import F, problem, raw_json, sent_json, sent_query

CASH = {"uuid": "c1", "name": "Broker Cash", "currencyCode": "EUR", "balance": {"value": "1.00", "currency": "EUR"}}
DEPOT = {"uuid": "p1", "name": "Broker", "referenceCashAccount": "c1"}


async def test_list_accounts_merges_both_kinds_through_mcp(api, one_file, mcp_client):
    cash = api.get(f"{F}/cash-accounts").mock(
        return_value=raw_json('{"items":[{"uuid":"c1","name":"Cash","currencyCode":"EUR","balance":{"value":1234.50,"currency":"EUR"}}]}')
    )
    invest = api.get(f"{F}/investment-accounts").respond(200, json={"items": [DEPOT]})
    result = await mcp_client.call_tool("list_accounts", {"date": "2026-01-31", "currency": "CHF"})
    assert sent_query(cash) == {"date": "2026-01-31"}
    assert sent_query(invest) == {"date": "2026-01-31", "currency": "CHF"}
    assert result.data["cashAccounts"][0]["balance"]["value"] == "1234.50"
    assert result.data["investmentAccounts"] == [DEPOT]


async def test_create_cash_account_returns_existing(api):
    api.get("/v1/files/main/cash-accounts").respond(200, json={"items": [CASH]})
    result = await accounts.create_cash_account("broker cash", "EUR", "main")
    assert result["created"] is False and result["account"]["uuid"] == "c1"


async def test_create_cash_account(api):
    api.get("/v1/files/main/cash-accounts").respond(200, json={"items": [CASH]})
    route = api.post("/v1/files/main/cash-accounts").respond(201, json={"uuid": "c2"})
    result = await accounts.create_cash_account(
        "Broker Cash", "USD", "main", note="usd", client_ref="r1", dry_run=True
    )
    assert sent_json(route) == {"name": "Broker Cash", "currencyCode": "USD", "note": "usd", "clientRef": "r1"}
    assert sent_query(route) == {"dry_run": "true"}
    assert result == {"created": False, "account": {"uuid": "c2"}}


async def test_update_and_delete_cash_account(api):
    patch = api.patch("/v1/files/main/cash-accounts/c1").respond(200, json=CASH)
    await accounts.update_cash_account("c1", "main", retired=True, clear=["note"])
    assert sent_json(patch) == {"retired": True, "note": None}
    delete = api.delete("/v1/files/main/cash-accounts/c1").mock(
        return_value=problem(409, "delete-blocked", "Delete blocked", detail="has transactions")
    )
    with pytest.raises(ToolError, match="^delete-blocked"):
        await accounts.delete_cash_account("c1", "main")
    assert delete.called


async def test_create_investment_account_skip_duplicate_check(api):
    route = api.post("/v1/files/main/investment-accounts").respond(201, json={"uuid": "p2"})
    result = await accounts.create_investment_account("Broker", "c1", "main", allow_duplicate=True)
    assert sent_json(route) == {"name": "Broker", "referenceCashAccount": "c1"}
    assert result == {"created": True, "account": {"uuid": "p2"}}


async def test_create_investment_account_duplicate(api):
    api.get("/v1/files/main/investment-accounts").respond(200, json={"items": [DEPOT]})
    result = await accounts.create_investment_account("Broker", "c9", "main")
    assert result["created"] is False and result["account"]["uuid"] == "p1"


async def test_get_account(api):
    cash = api.get("/v1/files/main/cash-accounts/c1").respond(200, json=CASH)
    assert await accounts.get_account("c1", "cash", "main", date="2026-01-31") == CASH
    assert sent_query(cash) == {"date": "2026-01-31"}
    depot = api.get("/v1/files/main/investment-accounts/p1").respond(200, json=DEPOT)
    await accounts.get_account("p1", "investment", "main", currency="USD")
    assert sent_query(depot) == {"currency": "USD"}


async def test_account_attributes_numbers_and_retired(api):
    route = api.post("/v1/files/main/cash-accounts").respond(201, json={"uuid": "c2"})
    await accounts.create_cash_account(
        "Savings", "EUR", "main", retired=False, attributes={"rate": "0.0125", "bank": "ING", "on": True},
        allow_duplicate=True,
    )
    content = route.calls.last.request.content
    assert b'"rate": 0.0125' in content and b'"bank": "ING"' in content
    assert sent_json(route)["retired"] is False
    with pytest.raises(ToolError, match="attributes.rate"):
        await accounts.update_cash_account("c1", "main", attributes={"rate": 0.5})


async def test_update_and_delete_investment_account(api):
    patch = api.patch("/v1/files/main/investment-accounts/p1").respond(200, json=DEPOT)
    await accounts.update_investment_account("p1", "main", name="Depot", reference_cash_account="c2", dry_run=True)
    assert sent_json(patch) == {"name": "Depot", "referenceCashAccount": "c2"}
    assert sent_query(patch) == {"dry_run": "true"}
    api.delete("/v1/files/main/investment-accounts/p1").respond(204)
    assert await accounts.delete_investment_account("p1", "main") == {"deleted": True, "uuid": "p1"}
