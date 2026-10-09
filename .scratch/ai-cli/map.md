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

## Not yet specified

- **Packaging and distribution**: how users install and launch the CLI (jlink/jpackage, fat jar, shipped inside the PP install). Hangs on the architecture decision.
- **Format and version support mechanics**: how non-XML formats and future file versions are handled, and the cost of tracking upstream format changes. Hangs on the architecture decision.
- **MCP surface**: whether the spec adds an MCP server over the same operations, and at what cost once the CLI shape is known.
- **Open-in-app guard**: whether and how the CLI detects that the desktop app has the client file open before writing.

## Out of scope

- AI skills that teach agents to use the CLI: to be written later, possibly in an external repository.
- Live sync with a running desktop app (e.g. an MCP server inside PP): a possible later effort.
