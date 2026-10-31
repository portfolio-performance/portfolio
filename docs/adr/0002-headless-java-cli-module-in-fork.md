# Build the AI-agent CLI as a headless Java module in this fork

For personal use, AI agents (Copilot, Claude Code) need to read and update client files with Portfolio Performance's own calculated figures. We build a new, additive Java 21 module in this fork that runs the core engine on a plain classpath against a closed client file, one JVM per command and no daemon. It never modifies existing core files. It takes no code dependency on the in-app REST API (PR #5870), whose unmerged, in-app OSGi plugin gives no way to write to a closed file, and it follows that API's vocabulary (*instrument*, *investment account*, *cash account*) and JSON shapes only where cheap.

**Considered Options**

- *CLI on top of #5870*: rejected. Writes there only mutate the open file's in-memory model, and the maintainer limits the surface deliberately, so every operation would wait on upstream. The project is archived if upstream ships full writes; if only #5870 merges, a live-read mode or new endpoints for personal use can be added later.
- *Separate repo consuming the core jar*: rejected. The core bundle is not published to Maven and part of its dependencies come from p2 repositories, so a separate repo would need its own artifact plumbing.
- *Independent re-implementation in another language*: rejected. It would lose PP's calculated figures and validation, and have to track file-format changes by hand.

**Consequences**

- Writes assume the desktop app has the client file closed, so an open-in-app guard is required (ticket 06).
- Issue 01's caveats (stdout logging, ECB rate cache) are handled inside the CLI module, not by patching the core.
- Staying additive keeps merges from `upstream` conflict-free.
