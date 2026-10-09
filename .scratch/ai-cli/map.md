# AI access to client files

Labels: wayfinder:map

## Destination

A hand-off-ready spec for a CLI that lets AI agents (GitHub Copilot, Claude Code) explore a client file, including Portfolio Performance's calculated figures, and update it through domain operations. The spec settles where the tool lives, its read and write command surface, write safety, and packaging.

## Notes

- Read `GLOSSARY.md` first; "portfolio" means the depot entity, the data file is the **client file**.
- Audience: own use + community; upstream acceptance is a factor, not a driver.
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

## Not yet specified

- **Packaging and distribution**: how users install and launch the CLI (jlink/jpackage, fat jar, shipped inside the PP install). Hangs on the architecture decision.
- **Format and version support mechanics**: how non-XML formats and future file versions are handled, and the cost of tracking upstream format changes. Hangs on the architecture decision.
- **MCP surface**: whether the spec adds an MCP server over the same operations, and at what cost once the CLI shape is known.
- **Open-in-app guard**: whether and how the CLI detects that the desktop app has the client file open before writing.

## Out of scope

- AI skills that teach agents to use the CLI: to be written later, possibly in an external repository.
- Live sync with a running desktop app (e.g. an MCP server inside PP): a possible later effort.
