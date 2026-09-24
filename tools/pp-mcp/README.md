# pp-mcp

An MCP server that gives an AI agent read and write access to the portfolio files open in a running Portfolio Performance (PP) desktop application. It is a thin adapter over PP's local REST API (`name.abuchen.portfolio.rest`). PP's own model, validation and calculations do the work.

Setup, client configuration and usage rules are in [docs/mcp/usage.md](../../docs/mcp/usage.md). The rules the agent receives on connect are `INSTRUCTIONS` in `src/pp_mcp/app.py`.

The adapter reads only two environment variables: `PP_API_URL` (default `http://127.0.0.1:5712`) and `PP_API_TOKEN`.

## Tools

| Area | Tools |
| --- | --- |
| Files | `list_files`, `get_file`, `open_file`, `save_file` |
| Instruments | `list_instruments`, `get_instrument`, `find_instrument`, `list_attribute_types`, `create_instrument`, `update_instrument`, `delete_instrument`, `set_prices`, `delete_prices`, `list_instrument_events`, `add_instrument_event`, `delete_instrument_event`, `apply_stock_split` |
| Accounts | `list_accounts`, `get_account`, `create_cash_account`, `update_cash_account`, `delete_cash_account`, `create_investment_account`, `update_investment_account`, `delete_investment_account` |
| Transactions | `list_transactions`, `get_transaction`, `create_buy`, `create_sell`, `create_delivery`, `create_dividend`, `create_cash_transaction`, `create_transfer`, `create_security_transfer`, `update_transaction`, `delete_transaction` |
| Watchlists | `list_watchlists`, `get_watchlist`, `create_watchlist`, `update_watchlist`, `delete_watchlist`, `add_to_watchlist`, `remove_from_watchlist` |
| Investment plans | `list_investment_plans`, `get_investment_plan`, `create_investment_plan`, `update_investment_plan`, `delete_investment_plan`, `generate_plan_transactions` |
| Taxonomies | `list_taxonomies`, `get_taxonomy`, `get_classification`, `get_taxonomy_allocation`, `create_taxonomy`, `rename_taxonomy`, `delete_taxonomy`, `create_classification`, `update_classification`, `delete_classification`, `assign_classification`, `unassign_classification` |
| Reports | `get_holdings`, `get_performance`, `get_security_performance`, `get_trades`, `get_earnings` |
| Actions | `update_quotes`, `get_job` |
| Import | `import_pdf`, `import_csv`, `commit_import` |

Update tools are merge patches: only the given fields change, and `clear` names optional fields to remove. The report tools (`get_holdings`, `get_performance`, `get_security_performance`, `get_taxonomy_allocation`) take lists of investment and cash account UUIDs to narrow the file; `list_transactions` takes a list of transaction types.

## Prompts

| Prompt | Purpose |
| --- | --- |
| `record_documents` | Import broker PDFs: preview, review with the user, commit, save on approval. |
| `review_portfolio` | Read-only summary of holdings, performance, earnings and allocation. |

## Booking plans

`pp-apply-plan` runs a batch of tool calls from a JSON plan. Use it when many bookings are prepared at once, for example from bank exports, so the batch can be reviewed and dry-run before it changes anything:

```bash
uv run pp-apply-plan plan.json            # dry run of every step
uv run pp-apply-plan plan.json --commit   # apply, then call save_file
```

Plan format, placeholders for account and instrument names (`@acct:`, `@cash:`, `@inv:`, `@isin:`, `@inst:`) and output are described in `src/pp_mcp/plan.py`.

## Development

```bash
uv run pytest -q
```

The tests mock the REST API with `respx` and call the tools both directly and through FastMCP's in-memory client.

### Live end-to-end test

`tests/e2e/` holds a test that runs every tool against a real PP instance. It is not part of `pytest`.

1. Build the product: `mvn -f portfolio-app/pom.xml verify -DskipTests -Djacoco.skip=true -Dcheckstyle.skip=true` (the `local-dev` profile does not build the product).
2. Create a workspace with the API enabled for a test file and a known token:

   ```bash
   uv run python tests/e2e/seed_workspace.py --workspace <ws> --port 5799 --file <abs-path-to-test-file>=e2e
   ```

   It prints the token.
3. Start PP with that workspace:

   ```bash
   portfolio-product/target/products/name.abuchen.portfolio.product/win32/win32/x86_64/portfolio/PortfolioPerformance.exe -data <ws>
   ```

4. Run the test. `--mode full` creates, changes and deletes entities of every kind, then saves. Use it only on a throwaway file, for example a copy of `name.abuchen.portfolio.tests/src/fileversions/client69.xml`. `--mode copy` reads everything and makes a few small writes. Use it only on a copy of a real file.

   ```bash
   PP_API_URL=http://127.0.0.1:5799 PP_API_TOKEN=<token> uv run python tests/e2e/run_e2e.py --path <abs-path> --mode full --state state.json
   ```

5. Restart PP and check that the saved entities are still there:

   ```bash
   PP_API_URL=http://127.0.0.1:5799 PP_API_TOKEN=<token> uv run python tests/e2e/run_e2e.py --path <abs-path> --verify --state state.json
   ```
