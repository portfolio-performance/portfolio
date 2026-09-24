# pp-mcp

An MCP server that gives an AI agent read and write access to the portfolio files open in a running Portfolio Performance (PP) desktop application. It is a thin adapter over PP's local REST API (`name.abuchen.portfolio.rest`). PP's own model, validation and calculations do the work.

## Setup

Requirements: [uv](https://docs.astral.sh/uv/) and a running PP with the REST API enabled.

1. In PP, open Preferences → REST API. Enable the API, enable the files the agent may access (optionally give each an alias), and click "Add client" to create a token.
2. Install the adapter:

   ```bash
   cd tools/pp-mcp
   uv sync
   ```

3. Run it (stdio transport):

   ```bash
   PP_API_TOKEN=<token> uv run pp-mcp
   ```

## Configuration

The adapter reads only these environment variables:

| Variable | Default | Meaning |
| --- | --- | --- |
| `PP_API_URL` | `http://127.0.0.1:5712` | Base URL of the PP REST API |
| `PP_API_TOKEN` | – | Bearer token created in Preferences → REST API |

## Claude Code

```bash
claude mcp add pp --env PP_API_TOKEN=<token> --env PP_API_URL=http://127.0.0.1:5712 \
  -- uv --directory <path>/tools/pp-mcp run pp-mcp
```

## Conventions

- **Nothing is saved until `save_file`.** Every write changes only PP's in-memory file and marks it dirty, like an edit in the UI. Closing the file without saving discards all changes.
- **`save_file` waits for background updates.** PP updates quotes online when it opens a file and every 30 minutes. `save_file` waits up to `wait_for_updates` seconds (default 30) for such updates to finish, then saves. If one is still running, the answer has `backgroundUpdatesPending: true` and possibly `dirty: true`; save again later.
- **Dry runs.** Every write tool accepts `dry_run=true`: PP validates the request and returns the fully resolved result without changing anything. `import_pdf` and `import_csv` default to a preview; with `dry_run=false` they import only the items with status `ok` (`include_warnings=true` adds `warning` items such as probable duplicates).
- **Numbers are decimal strings**, never floats: money 2 decimals, shares 8, quotes 8, exchange rates up to 10, taxonomy weights (percent) 2. Results carry decimals as strings too. Numeric custom attributes are passed as decimal strings as well; the adapter sends them to PP as exact JSON numbers.
- **Identifiers.** Entities are addressed by PP UUIDs; watchlists and investment plans by name.
- **`file`** is a file id or alias from `list_files`. It may be omitted when exactly one file is available.
- **Idempotency.** Every create, `add_instrument_event` and `apply_stock_split` accept `client_ref`; repeating a create with the same key returns the first result. `create_instrument` and the account creates also return an existing entity with the same ISIN or name instead of creating a duplicate, unless `allow_duplicate=true`.
- **Errors** are PP's problem responses as `type: title — detail`, followed by one `field (code): message` line per field error. While the user has a dialog open or edits a table cell, PP answers `user-interaction`; the adapter retries up to 3 times, honoring `Retry-After` (at most 5 s each).

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
