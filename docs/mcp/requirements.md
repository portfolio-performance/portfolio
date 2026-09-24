# Read/write MCP access to a running Portfolio Performance instance

Status: agreed 2026-09-24. Branch: `feature/mcp-rw`.

## Goal

An AI agent connected over MCP can read and modify the data of portfolio files open in a running Portfolio Performance (PP) instance, and can perform the operations a user performs in the UI. PP's own model, validation and calculation engine do the work. Nothing edits the `.portfolio` file behind PP's back.

## Architecture decisions

| ID | Decision |
| --- | --- |
| A1 | Base on upstream PR #5870 (`upstream/feature/rest-api`, plugin `name.abuchen.portfolio.rest`), rebased onto current `master`. |
| A2 | Merge the read endpoints from `manueldeprada/rest-ext` (20 commits: transactions, holdings metrics, performance series/securities/calendar, trades, prices, taxonomies). |
| A3 | Add write endpoints ourselves. `andreevdm/mcp` (closed upstream PR #5823) is a reference for transaction write semantics (linked legs, FX, fees/taxes, transfers, deliveries). Reuse logic where it fits, do not import its server. |
| A4 | The MCP server is a separate Python process (FastMCP, stdio transport, `uv`-managed) that calls the REST API. It lives in this repo under `tools/pp-mcp/`. No MCP SDK inside the OSGi build. |
| A5 | Writes change the in-memory model and mark the file dirty, as UI edits do. A new explicit save endpoint/tool persists a file. The API reports dirty state per file. |
| A6 | Every write endpoint and tool accepts `dry_run`. A dry run validates and returns the fully resolved result (resolved security/accounts, date, shares, currencies, exchange rate, gross/net, fees, taxes, linked legs) without changing the model. |
| A7 | Stay PR-able upstream: follow #5870 conventions (UI-thread executor, RFC 9457 problem+json, per-file authorization, bearer token, `messages*.properties`), keep work in focused commits. |

## Functional scope (v1)

All four groups are in scope for v1. Order of implementation follows the list.

### F1 Transactions CRUD

- List, get, create, update and delete transactions of every existing PP type:
  - Account: deposit, removal, interest, interest charge, dividends, fees, fee refund, taxes, tax refund, buy, sell, transfer in/out.
  - Portfolio: buy, sell, transfer in/out, delivery inbound/outbound.
- Buy/sell create both the portfolio and the account side (`BuySellEntry`). Transfers create both legs (`AccountTransferEntry`, `PortfolioTransferEntry`). Updates keep linked legs consistent. Deletes remove both legs.
- Support units: fees, taxes, gross value with FX (currency, amount, exchange rate), as the UI dialogs do.
- Validation matches the UI dialogs (currency consistency, required exchange rate, positive amounts, share precision).

### F2 Master data CRUD

- Securities: create, update, delete, retire. Fields include name, currency, ISIN/WKN/ticker, note, quote feed and feed URL/properties, latest feed, attributes, events (splits, notes), historical prices (add/replace/delete).
- Accounts and portfolios: create, update (name, note, reference account, retired, attributes), delete when empty.
- Watchlists: CRUD, add/remove securities.
- Investment plans: CRUD. Generating due transactions is part of F4.
- Taxonomies: read, create/rename/delete classifications, assign/unassign securities and accounts with weights.

### F3 Calculations and reports (read)

- Holdings/statement of assets at a date, per file, portfolio or account filter.
- Performance: TTWROR, IRR, absolute change, fees, taxes, series, calendar (monthly/yearly), risk (volatility, max drawdown, Sharpe).
- Per-security performance, trades (open/closed, P&L), dividends/earnings, taxonomy views, rebalancing targets.
- Exposed mostly by A2. Gaps are filled in the same style.

### F4 Application actions

- List open files and their dirty state. Save a file (A5).
- Trigger online quote update (latest and/or historical) for all or selected securities. Report completion.
- Run a stock split (the existing split wizard logic: adjust transactions and/or prices).
- Import a PDF: run PP's PDF extractors on a file path and return the extracted items; commit selected items into chosen account/portfolio. Dry run is the extraction preview.
- Import CSV with an explicit column mapping config (same two-step preview/commit).
- Generate due transactions for investment plans.

## Non-functional requirements

- N1 Security: keep #5870's model (loopback bind, bearer token, Host/Origin checks, per-file opt-in). The MCP adapter reads URL and token from environment variables (`PP_API_URL`, `PP_API_TOKEN`).
- N2 Concurrency: every model access runs on the UI thread. Writes during a modal dialog or cell edit return 423 (as #5870 does).
- N3 Undo safety: writes take effect only in memory until saved. The user can discard changes by closing without saving.
- N4 Identifiers: all entities are addressed by PP UUIDs. Create responses return the new UUIDs of every created object, including linked legs.
- N5 Idempotency: create endpoints accept an optional client key (`Idempotency-Key` header or `client_ref` field) stored on the transaction's `source` or an attribute. A repeat with the same key returns the existing object instead of creating a duplicate.
- N6 Numbers: amounts are decimal strings or integers in PP's native precision, never binary floats, in both REST and MCP.
- N7 Errors: problem+json with a stable error code. The MCP adapter forwards code and message as a tool error.
- N8 The MCP tool set is task-oriented (for example `create_buy`, `create_dividend`, `create_transfer`, `save_file`, `get_performance`), not a 1:1 mirror of REST routes where that would make tools awkward to use.

## Testing

- T1 JUnit tests for each REST handler against in-memory `Client` fixtures, in the existing test plug-in layout.
- T2 pytest for the adapter: unit tests with a mocked HTTP layer.
- T3 Live end-to-end: build PP, launch it with a synthetic test `.portfolio` file, enable the API for that file, run the adapter and exercise every MCP tool including save, then reopen the saved file and verify its content. Never use real user data.

## Assumptions (defaults chosen without asking, override if wrong)

- Work branch `feature/mcp-rw` in the `dgehriger/portfolio` fork. No upstream PR unless requested.
- The ledger model draft (#5874) is not used. Writes go through PP's existing transaction classes. Revisit if upstream merges the ledger API.
- No headless mode. PP must be running with the GUI.
- REST port and enablement stay in PP preferences (#5870 defaults: off, port 5712).

## Out of scope

- New transaction or corporate-action types PP does not already have.
- Headless/CLI operation of PP.
- Remote (non-loopback) access.
- Editing PP application preferences through the API.
