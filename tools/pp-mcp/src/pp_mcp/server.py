"""Entry point: the FastMCP server with every tool module registered."""

import importlib

from pp_mcp.app import mcp

TOOL_MODULES = (
    "files",
    "instruments",
    "accounts",
    "transactions",
    "watchlists",
    "plans",
    "taxonomies",
    "reports",
    "actions",
    "imports",
)

for _module in TOOL_MODULES:
    importlib.import_module(f"pp_mcp.tools.{_module}")

importlib.import_module("pp_mcp.prompts")

__all__ = ["main", "mcp"]


def main() -> None:
    """Run the MCP server on stdio."""
    mcp.run()


if __name__ == "__main__":
    main()
