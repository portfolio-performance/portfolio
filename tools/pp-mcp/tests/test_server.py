from pp_mcp.server import TOOL_MODULES

EXPECTED_TOOLS = {
    "list_files", "get_file", "open_file", "save_file",
    "list_instruments", "get_instrument", "find_instrument", "list_attribute_types", "create_instrument",
    "update_instrument", "delete_instrument", "set_prices", "delete_prices", "list_instrument_events",
    "add_instrument_event", "delete_instrument_event", "apply_stock_split",
    "list_accounts", "get_account", "create_cash_account", "update_cash_account", "delete_cash_account",
    "create_investment_account", "update_investment_account", "delete_investment_account",
    "list_transactions", "get_transaction", "create_buy", "create_sell", "create_delivery", "create_dividend",
    "create_cash_transaction", "create_transfer", "create_security_transfer", "update_transaction",
    "delete_transaction",
    "list_watchlists", "get_watchlist", "create_watchlist", "update_watchlist", "delete_watchlist",
    "add_to_watchlist", "remove_from_watchlist",
    "list_investment_plans", "get_investment_plan", "create_investment_plan", "update_investment_plan", "delete_investment_plan",
    "generate_plan_transactions",
    "list_taxonomies", "get_taxonomy", "get_classification", "get_taxonomy_allocation", "create_taxonomy", "rename_taxonomy", "delete_taxonomy",
    "create_classification", "update_classification", "delete_classification", "assign_classification",
    "unassign_classification",
    "get_holdings", "get_performance", "get_security_performance", "get_trades", "get_earnings",
    "update_quotes", "get_job", "import_pdf", "import_csv", "commit_import",
}
NO_DRY_RUN_WRITES = {"open_file", "save_file", "import_pdf", "import_csv"}


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
        if name.startswith("create_") or name in {"add_instrument_event", "apply_stock_split"}:
            assert "client_ref" in props, name


async def test_report_filters_take_uuid_lists(mcp_client):
    tools = {t.name: t for t in await mcp_client.list_tools()}
    for name in ("get_holdings", "get_performance", "get_security_performance", "get_taxonomy_allocation"):
        props = tools[name].inputSchema["properties"]
        for field in ("investment_account", "cash_account"):
            types = {s.get("type") for s in props[field].get("anyOf", [props[field]])}
            assert "array" in types, (name, field)


async def test_numeric_parameters_are_strings(mcp_client):
    tools = {t.name: t for t in await mcp_client.list_tools()}
    props = tools["create_buy"].inputSchema["properties"]
    for field in ("shares", "quote", "gross_value", "fees", "exchange_rate", "total"):
        schema = props[field]
        types = {s.get("type") for s in schema.get("anyOf", [schema])}
        assert "number" not in types and "string" in types, field


async def test_prompts_registered(mcp_client):
    prompts = {p.name for p in await mcp_client.list_prompts()}
    assert prompts == {"record_documents", "review_portfolio"}

    result = await mcp_client.get_prompt("record_documents", {"paths": "C:/a.pdf", "file": "main"})
    text = result.messages[0].content.text
    assert "C:/a.pdf" in text and "`main`" in text and "dry_run=True" in text


async def test_instructions_sent_on_connect(mcp_client):
    assert "save_file" in mcp_client.initialize_result.instructions

