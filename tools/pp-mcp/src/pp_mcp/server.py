"""Entry point: the FastMCP server with every tool module registered."""

import importlib

from pp_mcp.app import mcp

TOOL_MODULES = ("files",)

for _module in TOOL_MODULES:
    importlib.import_module(f"pp_mcp.tools.{_module}")

__all__ = ["main", "mcp"]


def main() -> None:
    """Run the MCP server on stdio."""
    mcp.run()


if __name__ == "__main__":
    main()
