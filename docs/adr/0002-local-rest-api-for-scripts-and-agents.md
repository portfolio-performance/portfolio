# Expose a local REST API for scripts and agents

Scripts and agents need to read and manipulate the data of portfolio files that are open in the
running desktop application. Three questions drove the design: how to address multiple open files,
how a client deterministically picks its target file, and how the user authorizes which files are
exposed. The API is an authorization boundary and a published contract — both are hard to take back
once clients exist.

**Decision**

A loopback-only HTTP/JSON server (`com.sun.net.httpserver`, precedent:
`name.abuchen.portfolio/…/oauth/impl/CallbackServer.java`) runs inside the RCP process in a new
plugin `name.abuchen.portfolio.rest`, which depends on the model plugin but **never** on the UI
plugin: the UI contributes the open-file registry and the UI-thread executor through SPI interfaces
(`rest/spi/HostApplication`, `rest/spi/OpenFile`) that the rest plugin defines. MCP for agents
becomes a later thin adapter over the same service layer. The individual decisions:

- **File identity lives in workspace preferences, keyed by absolute file path** — never inside the
  portfolio file. Move or rename ⇒ identity lost ⇒ fails closed. Files may additionally carry a
  user-assigned alias (`[a-z0-9-]{1,32}`, may not parse as a UUID, unique case-insensitively across
  all records) so an alias can never shadow a file UUID in the shared `/files/{id}` segment.
- **Explicit targeting only.** Every data route is scoped `/v1/files/{id}/…`. There is no "active
  file": the user switching tabs must never retarget a running script.
- **Per-file opt-in is the authorization grant.** Server off by default; a global preference enables
  it and each file is enabled individually. Requests for a file that is not enabled return 404 —
  deliberately indistinguishable from an unknown ID, so a token holder cannot enumerate the files
  the user did not share. Consequently 403 is *never* used for file scoping.
- **Stable bearer token**, generated once per workspace, held owner-only in the plugin state
  location; "regenerate" is the revocation mechanism. Requests carrying a browser `Origin` header
  are rejected (403), and — because a DNS-rebinding attacker's page is *same-origin* and therefore
  sends no `Origin` at all — the `Host` header must also be a literal loopback authority (403
  `forbidden-host`).
- **Writes are in-memory only; there is no save endpoint.** API mutations behave exactly like UI
  edits: mutate the `Client`, mark it dirty, let the UI refresh live. Only the user saves. An agent's
  changes can be reviewed and discarded by closing without saving.
- **All model access is marshalled to the UI thread** (`Display.syncExec`): one mutator thread, the
  same guarantee UI code relies on, rather than retrofitting locking onto the model.
- **Writes are rejected while an application-modal dialog is open** (423 + `Retry-After`). Modal edit
  dialogs such as `EditSecurityDialog` hold a stale copy of the entity that is written back wholesale
  on OK, and `syncExec` runnables *do* execute during a modal event loop — so an API write landing
  mid-dialog would be silently clobbered. Reads stay allowed.
- **PATCH is JSON Merge Patch (RFC 7386)** against a strict writable-field whitelist; unknown or
  read-only fields are rejected with 422 rather than ignored, and all violations are reported at once
  so an agent self-corrects in one round-trip.
- **DELETE of an instrument is blocked (409) when transactions or investment plans reference it**,
  because `Client.removeSecurity` cascades into transaction history the API client may never have
  seen. Watchlist and taxonomy membership do not block; those references are cleaned up.
- **The API vocabulary is deliberately more general than the model:** `Security` → *instrument*,
  `Account` → *cash account*, `Portfolio` → *investment account*. An investment account may be a bank
  Depot, a broker account or a crypto exchange account, so "securities account" — the term the English
  UI uses — would bake bank vocabulary into a contract we cannot rename without breaking clients. The
  pairing mirrors the industry split (Plaid: `depository` vs `investment`). Java class names keep the
  model's vocabulary; only the wire format and user-facing strings use the API vocabulary.
- **Errors are RFC 9457 problem+json** with field-level `errors: [{field, code, message}]`.

**Considered Options**

- *UUID persisted inside the portfolio file*: the obvious identity scheme, rejected because users
  copy files to experiment (main vs. test copy). The copy would inherit the UUID and a client could
  silently write to the wrong file. Path-keying accepts a narrower residual risk instead: a *different*
  file copied over an enabled path inherits that path's identity and enablement. Accepted for v1 —
  replacing a file at a path is a deliberate act, the file must still be opened in the UI to be
  reachable, and `GET /v1/files` exposes label and path for clients to sanity-check. The designed
  hardening, if it ever bites, is a lineage nonce written into the file on first enable and mirrored
  in the prefs record; identity would stay path-keyed.
- *Ephemeral IDs for never-saved files*: rejected. A file that was never saved has no path, hence no
  prefs record, hence cannot be enabled and never appears in `GET /v1/files`. This falls out of
  path-keyed identity for free and fails closed; the alternative adds a second identity class through
  discovery, enablement and prefs lifecycle to serve a buffer whose API changes evaporate on close.
- *Full-state PUT instead of merge patch*: rejected. It forces read-modify-write, which races with
  concurrent UI edits (lost update), and makes every future model property a hazard for clients
  echoing back stale state.
- *Generic, reflection-driven CRUD over the model*: rejected. Generic CRUD can express invalid states;
  the whole point of the narrow write surface is that no request may introduce inconsistent data.
- *MCP as the transport*: deferred, not rejected. Scripts want plain HTTP, and the service layer seam
  lets MCP plug in later without a second model-access path.
- *A save endpoint*: deferred as too dangerous for v1. May follow.

**Consequences**

The pipeline is built so a new resource costs a handler plus a serializer: authentication, file-scope
resolution, the modal write gate, UI-thread marshalling and problem+json mapping all live in the
filter chain and cannot be forgotten per endpoint. The wire conventions that would force a `/v2` if
changed later — money shape, date format, list responses as an `{items: […]}` envelope rather than a
bare array, and the problem `code` vocabulary — are fixed from day one.

The port is fixed (default 5712) and never hops on bind failure; a conflict is reported rather than
silently worked around, because a client that finds the API on an unexpected port cannot know whose
API it is.
