# Where does the CLI live and what code does it build on?

Type: grilling
Status: open
Blocked by: 01, 02

## Question

Should the CLI be (a) a new module/bundle inside this repo, (b) a separate project depending on PP core artifacts, (c) a separate project that reads the client file independently (and in which language), or (d) a CLI aligned with the maintainer's in-app REST API (PR #5870), sharing its resource names and response shapes, possibly contributed upstream as the "read-only CLI" he suggested? Weigh calculated-figure reuse, file-version tracking, upstream acceptance, the maintainer's stance against writes that bypass input validation, and build/packaging cost.

Inputs: [Can PP core load, calculate and save a client file outside the workbench?](01-headless-core-feasibility.md), [What prior art exists for programmatic access to client files?](02-prior-art.md).

## Comments

**Findings from the map-charting session (2026-10-09), not a decision.** The user leaned towards building on PR #5870 and contributing missing endpoints upstream; grill this before resolving.

Candidate approaches raised:

1. Build on #5870 and limit writes to what it exposes, growing as upstream ships more.
2. Contribute missing endpoints (especially writes) upstream in #5870's style, one small PR at a time. (User's current leaning.)
3. Headless core for writes to a closed file, #5870 for live reads.

Facts about PR #5870 (as of 2026-10-09, head `b5c59f7`, open, 42 commits):

- New plugin `name.abuchen.portfolio.rest`; loopback HTTP on port 5712, off by default; per-file opt-in; per-client bearer tokens via an interactive pairing flow; rejects browser `Origin` and non-loopback `Host`.
- Reads: files, instruments (+ attribute types), cash accounts, investment accounts, holdings, performance (IRR, TTWROR), per-instrument performance, trades.
- Writes: only `PATCH` and `DELETE` of an instrument. Writes mutate the in-memory model and mark the file dirty; there is no save endpoint, the user saves. Writes return 423 while a modal dialog or cell editor is open.
- Ships an MCP endpoint (`pp_*` tools, `mcp-tools.json`) dispatching into the same routes; plus `openapi.yaml` and agent skills (`npx skills@latest add portfolio-performance/skills`).
- ADR 0002 rejects generic reflection-driven CRUD ("no request may introduce inconsistent data") and full-state PUT; API vocabulary: Security → *instrument*, Portfolio → *investment account*.
- Maintainer: API surface "still very limited because I am considering every interface carefully"; a CLI could expose the same API; a read-only CLI is "a reasonable starting point"; a CLI writing an open file risks dual edits since PP has no conflict detection.

manueldeprada's fork (`feature/rest-api-transactions-and-holdings-metrics`, 20 commits, ~5k lines, based on #5870 as of 2026-07-31, self-described "mostly vibecoded"):

- Read-only additions: `GET /transactions`, enriched holdings (cost basis, P/L, TTWROR/IRR, dividends, taxonomy classifications, FX, technical metrics), `/performance/series`, `/performance/calendar`, `/performance/securities`, `/trades`, `/instruments/{uuid}/prices`, `/taxonomies`, balances on accounts.
- No writes. Follow-up branches (not inspected): caching + pagination, and a headless mode (`feature/rest-api-headless`).
- Unmerged, no maintainer response, behind #5870.

GUI automation of the running app (Playwright/Selenium-style), raised and argued against: Playwright/Selenium don't drive SWT; SWTBot runs in-process; OS accessibility exposes SWT poorly; scraping virtualized tables/charts is brittle and slow; #5870 gives structured live access.

Open tensions for the grilling: building on #5870 conflicts with the map's "writes assume the client file is closed" note and "live sync out of scope"; it would make file-format/version tracking and an open-in-app guard largely moot; whether a headless mode is needed so agents work without the desktop app open.
