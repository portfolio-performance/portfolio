# Using Portfolio Performance with an AI agent (MCP)

An AI agent such as Claude Code or Claude Desktop can read and edit the portfolio files open in Portfolio Performance (PP). The agent talks to the MCP server `pp-mcp` (`tools/pp-mcp`), which calls PP's local REST API. PP's own model, validation and calculations do the work, so the result is the same as an edit in the UI.

```
agent --MCP (stdio)--> pp-mcp --HTTP 127.0.0.1--> Portfolio Performance (running, file open)
```

What the agent can do:

- Read securities, accounts, transactions, watchlists, investment plans, taxonomies and prices.
- Read reports: statement of assets, performance (TTWROR, IRR, series, calendar), per-security performance, trades, earnings, taxonomy allocation.
- Create, change and delete transactions of every type (buy, sell, dividend, deposit, removal, interest, fees, taxes, transfers, deliveries), securities, prices, events, accounts, watchlists, investment plans and taxonomies.
- Apply a stock split, generate due investment plan transactions, update quotes online, import broker PDFs and CSV files, open and save files.

## Set up Portfolio Performance

1. Start PP from this repository's build (the REST API is not part of the official release):

   ```bash
   mvn -f portfolio-app/pom.xml verify -DskipTests -Djacoco.skip=true -Dcheckstyle.skip=true
   ```

   The application is in `portfolio-product/target/products/name.abuchen.portfolio.product/<os>/<ws>/<arch>/portfolio/`.
2. Open Preferences → REST API:
   - Enable the API. It listens only on `127.0.0.1`, port 5712 by default.
   - Enable each file the agent may access. Files that are not enabled are invisible to the agent. An alias (for example `main`) makes the file easier to address.
   - Click "Add client" and copy the token. It is shown only once.

Password-protected files work once you have opened them in PP. The agent cannot open them itself.

## Set up the MCP server

Requirements: [uv](https://docs.astral.sh/uv/).

```bash
cd tools/pp-mcp
uv sync
```

Register it with Claude Code:

```bash
claude mcp add pp --env PP_API_TOKEN=<token> --env PP_API_URL=http://127.0.0.1:5712 -- uv --directory <repo>/tools/pp-mcp run pp-mcp
```

For Claude Desktop, add this to `claude_desktop_config.json`:

```json
{
  "mcpServers": {
    "pp": {
      "command": "uv",
      "args": ["--directory", "<repo>/tools/pp-mcp", "run", "pp-mcp"],
      "env": { "PP_API_TOKEN": "<token>", "PP_API_URL": "http://127.0.0.1:5712" }
    }
  }
}
```

The server sends its usage rules to the agent when it connects, so no extra prompt is needed. It also offers two prompt templates, `record_documents` (import broker PDFs with a review step) and `review_portfolio` (a read-only summary). In Claude Code they appear as `/mcp__pp__record_documents` and `/mcp__pp__review_portfolio`.

## How changes are applied

- **Nothing is written to disk until the agent calls `save_file`.** Every change marks the file as unsaved (the tab shows `*`), exactly like an edit in the UI. To throw away the agent's changes, close the file in PP without saving.
- **Dry runs.** Every write accepts `dry_run`. PP validates the request and returns the complete result, for example both sides of a buy, the fees, taxes, exchange rate and total, without changing anything.
- **Duplicates.** Transaction creates accept a `client_ref`, stored as the transaction's source `api:<client_ref>`. Repeating a create with the same key returns the existing transaction. Imports flag probable duplicates the same way the import wizard does.
- **Your edits win.** While a dialog is open in PP or you are editing a table cell, PP refuses API writes with `user-interaction`. The agent retries for a few seconds and then reports it.
- **Saving waits for quote updates.** PP updates quotes online after opening a file and every 30 minutes. `save_file` waits up to 30 seconds for a running update, so the saved file includes the new prices.
- **Log.** Every change made through the API is written to PP's log file, `<workspace>/.metadata/.log` (on Windows the workspace is `%LOCALAPPDATA%\PortfolioPerformance\workspace`), with the file, entity and changed fields.

## Large updates

For many bookings at once, such as a reconciliation against bank exports, let the agent write the bookings into a plan file and run `uv run pp-apply-plan plan.json` in `tools/pp-mcp`. Without `--commit` every step is a dry run. Rehearse on a copy of the file first: enable the copy for API access, apply the plan with `--commit`, save, and compare the balances with the bank statements before applying the same plan to the real file. Keep a backup of the real file.

## Troubleshooting

| Symptom | Cause |
| --- | --- |
| `unauthorized` | Token wrong or revoked. Create a new one in Preferences → REST API. |
| `not-found` for a file | The file is not enabled in Preferences → REST API, or the id/alias is wrong. Enabled files that are not open can be opened with `open_file`. |
| `file-not-open` | The file is enabled but not open. Use `open_file` or open it in PP. |
| `user-interaction` | A dialog is open or you are editing a cell in PP. Close it. |
| `password-required` | Encrypted file. Open it in PP yourself. |
| `backgroundUpdatesPending: true` after `save_file` | A quote update took longer than the wait. Save again later. |
| Connection refused | PP is not running, the API is disabled, or the port differs from `PP_API_URL`. |

The REST API itself is documented in `name.abuchen.portfolio.rest/openapi.yaml` and served at `GET /v1/openapi.yaml`.
