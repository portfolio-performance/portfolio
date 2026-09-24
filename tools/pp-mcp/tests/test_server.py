from pp_mcp.server import TOOL_MODULES

EXPECTED_TOOLS = {
    "list_files", "get_file", "open_file", "save_file",
    "list_instruments", "get_instrument", "find_instrument", "create_instrument", "update_instrument",
    "delete_instrument", "set_prices", "delete_prices", "add_instrument_event", "delete_instrument_event",
    "apply_stock_split",
    "list_accounts", "create_cash_account", "update_cash_account", "delete_cash_account",
    "create_investment_account", "update_investment_account", "delete_investment_account",
    "list_transactions", "get_transaction", "create_buy", "create_sell", "create_delivery", "create_dividend",
    "create_cash_transaction", "create_transfer", "create_security_transfer", "update_transaction",
    "delete_transaction",
    "list_watchlists", "create_watchlist", "rename_watchlist", "delete_watchlist", "add_to_watchlist",
    "remove_from_watchlist",
    "list_investment_plans", "create_investment_plan", "update_investment_plan", "delete_investment_plan",
    "generate_plan_transactions",
    "list_taxonomies", "get_taxonomy_allocation", "create_taxonomy", "rename_taxonomy", "delete_taxonomy",
    "create_classification", "update_classification", "delete_classification", "assign_classification",
    "unassign_classification",
    "get_holdings", "get_performance", "get_security_performance", "get_trades", "get_earnings",
    "update_quotes", "get_job", "import_pdf", "import_csv", "commit_import",
}
NO_DRY_RUN_WRITES = {"open_file", "save_file", "update_quotes", "import_pdf", "import_csv"}


async def test_all_tools_registered(mcp_client):
    tools = {t.name: t for t in await mcp_client.list_tools()}
    assert set(tools) == EXPECTED_TOOLS
    assert len(TOOL_MODULES) == 10
    for name, tool in tools.items():
        assert tool.description, name
        props = tool.inputSchema["properties"]
        if "file" in props:
            assert "file" not in tool.inputSchema.get("required", []), name
        read_only = tool.annotations and tool.annotations.readOnlyHint
        if not read_only and name not in NO_DRY_RUN_WRITES:
            assert "dry_run" in props, name
        if name.startswith("create_") and name not in {
            "create_watchlist", "create_investment_plan", "create_taxonomy", "create_classification"
        }:
            assert "client_ref" in props, name


async def test_numeric_parameters_are_strings(mcp_client):
    tools = {t.name: t for t in await mcp_client.list_tools()}
    props = tools["create_buy"].inputSchema["properties"]
    for field in ("shares", "quote", "gross_value", "fees", "exchange_rate", "total"):
        schema = props[field]
        types = {s.get("type") for s in schema.get("anyOf", [schema])}
        assert "number" not in types and "string" in types, field
