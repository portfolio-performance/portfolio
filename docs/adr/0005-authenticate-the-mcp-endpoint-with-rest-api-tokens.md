# Authenticate `/mcp` with REST API bearer tokens

`POST /mcp` runs on loopback, but it can read and mutate the in-memory portfolio. The existing REST
API already has per-client bearer tokens, hashed storage, revocation, and a preference page where a
user can mint a token manually.

**Decision**

The MCP endpoint uses the same bearer tokens as the REST API, supplied as an `Authorization` header
in the client's connector settings. The application serves no OAuth metadata and runs no OAuth
authorization flow; `/.well-known/*` answers `404`.

`initialize`, `tools/list`, and `ping` answer without a token, because they disclose only the public
tool surface. `tools/call` requires a valid token, but a missing or invalid token is returned as an
MCP tool error at HTTP 200: the tested client showed no useful UI for a transport-level `401`, while
tool text reaches the chat. The preference page records the last rejected MCP call because a wrong
token never becomes an authorized client entry.

**Rejected**

OAuth discovery and a local authorization server are too much machinery for a single-user loopback
API whose clients are still self-declared local processes. Unauthenticated loopback access is also
too weak: any local process could read shared portfolios or mutate the live in-memory model, including
encrypted files that are decrypted only inside the application.
