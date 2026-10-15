# Does the spec include an MCP server over the CLI operations, and in what shape?

Type: grilling
Blocked by: 10

## Question

MCP is nice-to-have. Does the hand-off spec include an MCP server in v1, defer it, or rule it out? If included: a stdio server that shells out to `pp-cli` per call, or one JVM using a Java MCP SDK and calling the same command code; which commands become tools (reads only, or `apply` too); and how tool names and schemas map onto the read surface (ticket 04) and the `apply` batch (ticket 05), aligned with #5870's `pp_*` tools where it costs nothing.
