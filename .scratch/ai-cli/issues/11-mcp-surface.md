# Does the spec include an MCP server over the CLI operations, and in what shape?

Type: grilling
Status: resolved
Blocked by: 10

## Question

MCP is nice-to-have. Does the hand-off spec include an MCP server in v1, defer it, or rule it out? If included: a stdio server that shells out to `pp-cli` per call, or one JVM using a Java MCP SDK and calling the same command code; which commands become tools (reads only, or `apply` too); and how tool names and schemas map onto the read surface (ticket 04) and the `apply` batch (ticket 05), aligned with #5870's `pp_*` tools where it costs nothing.

## Answer

**MCP is out of scope for v1.** The CLI ships with no MCP server. Both required clients reach the tool by shelling out to `pp-cli` on `PATH` (ticket 10): Claude Code runs it directly, Copilot runs it through the VS Code terminal. Ticket 04's `describe` command already publishes machine-readable command, option and response schemas, so an agent can self-document the surface without a tool registry. Nothing further is recorded as an intended MCP shape; a future MCP effort would re-grill it from scratch.

### Why ruling it out, not deferring with a recorded shape

- **Neither required client needs it.** Both reach `pp-cli` through `PATH`; MCP's marginal value (registered tool discovery, schema-validated arguments) is real but not load-bearing for the two named clients, and `describe` covers discovery.
- **Upstream #5870 does not make our CLI redundant, nor does it give us a reusable MCP.** #5870 (merged 2026-10-10) ships an *in-process* `POST /mcp` Streamable-HTTP endpoint inside the running desktop app (hand-rolled JSON-RPC 2.0, no SDK), exposing 16 `pp_*` tools. It is structurally the opposite of this CLI:
  - It needs the **app running with the file open** (open-file registry + UI-thread executor); our CLI is **headless, closed file, one JVM per command** (ticket 03).
  - Its writes are **two instrument-only tools** (`pp_update_instrument`, `pp_delete_instrument`), **in-memory only, no save** — a human persists; our `apply` (ticket 05) writes the file atomically offline.
  - Its read set is **smaller** than ticket 04 (no transactions, prices, allocation or taxonomies tools).
  So #5870 does not trigger the map's "full REST API with writes → archive the project" clause, and building a *second*, stdio, headless MCP in v1 would be new work, not reuse.
- **Any v1 MCP would necessarily diverge from #5870** on transport (stdio over `pp-cli`, not in-app HTTP) and tool set (ticket 04 reads + the `apply` batch). Alignment would be cosmetic only (`pp_*` tool names, camelCase JSON fields, kebab-case enums), not structural — not enough value to pull into v1.

### If MCP is revisited later (non-binding notes, not a v1 commitment)

- Transport would be **stdio shelling out to `pp-cli` per call**, matching the one-JVM-per-command model, rather than an in-app HTTP server (that is upstream's job and lives inside the running app).
- Tools would wrap ticket 04's read commands plus the ticket 05 `apply` batch — a superset of #5870's reads and a different write model.
- Cosmetic alignment with #5870 where free: `pp_*` tool names, camelCase JSON fields, kebab-case path/enum segments.

### How it was resolved

Grilled 2026-10-16. Round 1 put the pivot (include / defer-with-recorded-shape / rule out); a background research agent confirmed #5870's MCP is an in-app HTTP endpoint that cannot read a closed file or write one offline. The user chose to **rule it out**.

| Decision | Reasoning |
| --- | --- |
| MCP out of v1 (user's answer) | Both required clients already shell out to `pp-cli`; `describe` covers discovery; upstream's in-app MCP serves the live-app case and is not reusable headless. |
| No recorded "intended shape" either | Ruling out, not deferring; a later effort re-grills it rather than inheriting a paper design that may not fit then. |

Rejected options:

- **Include an MCP server in v1:** low value when neither required client needs it and `describe` already documents the surface; it would be net-new stdio work, not reuse of #5870.
- **Defer with a recorded shape:** the user chose a clean out-of-scope rather than carrying a non-binding design forward.
