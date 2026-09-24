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
- **Dry runs.** Every write tool accepts `dry_run=true`: PP validates the request and returns the fully resolved result without changing anything. `import_pdf` and `import_csv` default to a preview.
- **Numbers are decimal strings**, never floats: money 2 decimals, shares 8, quotes 8, exchange rates up to 10, taxonomy weights (percent) 2. Results carry decimals as strings too.
- **Identifiers.** Entities are addressed by PP UUIDs; watchlists and investment plans by name.
- **`file`** is a file id or alias from `list_files`. It may be omitted when exactly one file is available.
- **Idempotency.** Transaction and master-data creates accept `client_ref`; repeating a create with the same key returns the first result. `create_instrument` and the account creates also return an existing entity with the same ISIN or name instead of creating a duplicate, unless `allow_duplicate=true`.
- **Errors** are PP's problem responses as `type: title — detail`, followed by one `field (code): message` line per field error. While the user has a dialog open or edits a table cell, PP answers `user-interaction`; the adapter retries up to 3 times, honoring `Retry-After` (at most 5 s each).

## Tools

| Area | Tools |
| --- | --- |
| Files | `list_files`, `get_file`, `open_file`, `save_file` |
| Instruments | `list_instruments`, `get_instrument`, `find_instrument`, `create_instrument`, `update_instrument`, `delete_instrument`, `set_prices`, `delete_prices`, `add_instrument_event`, `delete_instrument_event`, `apply_stock_split` |
| Accounts | `list_accounts`, `create_cash_account`, `update_cash_account`, `delete_cash_account`, `create_investment_account`, `update_investment_account`, `delete_investment_account` |
| Transactions | `list_transactions`, `get_transaction`, `create_buy`, `create_sell`, `create_delivery`, `create_dividend`, `create_cash_transaction`, `create_transfer`, `create_security_transfer`, `update_transaction`, `delete_transaction` |
| Watchlists | `list_watchlists`, `create_watchlist`, `rename_watchlist`, `delete_watchlist`, `add_to_watchlist`, `remove_from_watchlist` |
| Investment plans | `list_investment_plans`, `create_investment_plan`, `update_investment_plan`, `delete_investment_plan`, `generate_plan_transactions` |
| Taxonomies | `list_taxonomies`, `get_taxonomy_allocation`, `create_taxonomy`, `rename_taxonomy`, `delete_taxonomy`, `create_classification`, `update_classification`, `delete_classification`, `assign_classification`, `unassign_classification` |
| Reports | `get_holdings`, `get_performance`, `get_security_performance`, `get_trades`, `get_earnings` |
| Actions | `update_quotes`, `get_job` |
| Import | `import_pdf`, `import_csv`, `commit_import` |

Update tools are merge patches: only the given fields change, and `clear` names optional fields to remove.

## Development

```bash
uv run pytest -q
```

The tests mock the REST API with `respx` and call the tools both directly and through FastMCP's in-memory client.
