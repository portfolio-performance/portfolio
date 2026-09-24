"""The FastMCP server instance that every tool module registers with."""

from fastmcp import FastMCP

INSTRUCTIONS = """\
Tools to read and edit the portfolio files open in a running Portfolio Performance (PP)
desktop application through its local REST API.

- Numbers are decimal strings, never floats: money 2 decimals, shares 8, quotes 8,
  exchange rates up to 10, taxonomy weights (percent) 2.
- Entities are addressed by PP UUIDs; watchlists and investment plans by name.
- Writes change only PP's in-memory file and mark it dirty, exactly like an edit in
  the UI. Nothing is persisted until you call save_file. The user can discard all
  changes by closing the file without saving.
- Every write accepts dry_run=True, which validates and returns the fully resolved
  result without changing anything. Prefer a dry run before a real write.
- `file` is the file id or alias from list_files; it may be omitted when exactly one
  file is available.
"""

mcp = FastMCP("portfolio-performance", instructions=INSTRUCTIONS)

READ_ONLY = {"readOnlyHint": True, "openWorldHint": False}
WRITE = {"readOnlyHint": False, "destructiveHint": False, "openWorldHint": False}
DESTRUCTIVE = {"readOnlyHint": False, "destructiveHint": True, "openWorldHint": False}
