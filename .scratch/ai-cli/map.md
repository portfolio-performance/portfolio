# AI access to client files

Labels: wayfinder:map

## Destination

A hand-off-ready spec for a CLI that lets AI agents (GitHub Copilot, Claude Code) explore a client file, including Portfolio Performance's calculated figures, and update it through domain operations. The spec settles where the tool lives, its read and write command surface, write safety, and packaging.

## Notes

- Read `GLOSSARY.md` first; "portfolio" means the depot entity, the data file is the **client file**.
- Audience: personal use only. Upstream acceptance is not a goal; do not wait on PR #5870. If upstream ships a full REST API with writes, the project is archived; if only #5870 merges, continue by adding endpoints for personal use (contributing them is allowed but not a goal).
- Vocabulary on the external surface follows #5870: *instrument*, *investment account*, *cash account* (see `GLOSSARY.md`).
- Reads work headlessly on a closed client file; live reads from the running app are an optional later add-on.
- Required clients: Copilot (VS Code) and Claude Code via shell. MCP is nice-to-have.
- Writes assume the desktop app has the client file closed; reads may happen while it is open.
- Writes are domain operations only (no free-form XML editing), with automatic backup and dry-run.
- Calculated figures (valuation, IRR, TTWROR, gains, currency conversion) are in scope for reading.
- Plain XML is required; compressed, binary and encrypted formats are nice-to-have, added gradually.
- Must handle current and future file versions, refusing cleanly on a newer-than-supported file.
- A Java runtime (or bundled JRE) is acceptable.
- Skills to consult: `grilling` + `domain-modeling` for grilling tickets; `research` for research tickets.

## Decisions so far

<!-- one line per closed ticket: [title](issues/NN-slug.md): gist -->

- [Can PP core load, calculate and save a client file outside the workbench?](issues/01-headless-core-feasibility.md): yes, on a plain classpath from the built bundle jar plus target-platform jars; no OSGi launch needed, with caveats (stdout logging, ECB rate cache, online feeds).
- [What prior art exists for programmatic access to client files?](issues/02-prior-art.md): ~25 tools, none runs PP's engine headlessly with validation; the maintainer is building an in-app REST API (PR #5870) and endorsed a read-only CLI over the same API as a starting point.
- [Where does the CLI live and what code does it build on?](issues/03-architecture.md): a new additive Java 21 module in this fork running the core engine headlessly on a closed file, one JVM per command, no code dependency on #5870; see ADR 0002.
- [What does the read command surface look like?](issues/04-read-surface.md): read-only `pp-cli` commands (info, instruments, accounts, transactions as logical records, holdings, prices, performance, allocation, describe); UUID-canonical addressing; JSON envelope `{data, meta, warnings}`; native currency for records, base currency for aggregates; FIFO default.
- [Which domain write operations does v1 support?](issues/05-write-operations.md): one atomic `pp-cli apply --input` batch of independent JSON operations (transaction add/update/delete as whole records, instrument and account add/update, taxonomy assign); no price writes or cascading deletes; core import checks reused plus positivity, overselling and master-data rules; all validation errors reported at once.
- [How do backup and dry-run behave on writes?](issues/06-write-safety.md): one safe-write sequence (lock, `Checker` no-new-issues check, timestamped backup in `backups/`, temp file plus atomic rename, hash/mtime re-check, `lsof` warning); writes applied by default with opt-in `--dry-run` reporting entity-level changes; format kept, newer files refused; minimal `restore`.
- [Which client file formats can core read and write headlessly, and what does tracking upstream format changes cost?](issues/07-format-support-facts.md): all seven formats load and save headlessly with format preserved; unencrypted binary skips the newer-version check, so the CLI must guard it; about 3.4 version bumps a year, so the CLI's core must match the installed desktop release.
- [What are the options for packaging a headless Java 21 CLI built from this Tycho repo?](issues/09-packaging-options.md): about 0.4 to 0.5 s per call; pdfbox 1.8/3.x clash on any flat classpath; Tycho `list-dependencies` collects jars; jlink only for the runtime; reusing the installed app's JRE and jars is possible from outside the `.app`.

## Not yet specified

<!-- empty: packaging, format/version support and MCP graduated into tickets 07 to 11 -->

## Out of scope

- AI skills that teach agents to use the CLI: to be written later, possibly in an external repository.
- Live sync with a running desktop app (e.g. an MCP server inside PP): a possible later effort.
