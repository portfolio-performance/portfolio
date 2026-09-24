# pp-mcp

An MCP server that gives an AI agent read and write access to the portfolio files open in a running Portfolio Performance (PP) desktop application. It is a thin adapter over PP's local REST API (`name.abuchen.portfolio.rest`). PP's own model, validation and calculations do the work.

## Setup

Requirements: [uv](https://docs.astral.sh/uv/) and a running PP with the REST API enabled.

1. In PP, open Preferences → REST API. Enable the API, enable the files the agent may access (optionally give each an alias), and click "Add client" to create a token.
2. Install the adapter:

   ```bash
   cd tools/pp-mcp
   uv sync
   ```

3. Run it (stdio transport):

   ```bash
   PP_API_TOKEN=<token> uv run pp-mcp
   ```

## Configuration

The adapter reads only these environment variables:

| Variable | Default | Meaning |
| --- | --- | --- |
| `PP_API_URL` | `http://127.0.0.1:5712` | Base URL of the PP REST API |
| `PP_API_TOKEN` | – | Bearer token created in Preferences → REST API |

## Claude Code

```bash
claude mcp add pp --env PP_API_TOKEN=<token> --env PP_API_URL=http://127.0.0.1:5712 \
  -- uv --directory <path>/tools/pp-mcp run pp-mcp
```

## Tools

| Tool | Purpose |
| --- | --- |
| `list_files`, `get_file` | Open, API-enabled files and their `dirty` flag |
| `open_file` | Open an already enabled file in PP |
| `save_file` | Persist a file |

## Development

```bash
uv run pytest -q
```

The tests mock the REST API with `respx` and call the tools both directly and through FastMCP's in-memory client.
