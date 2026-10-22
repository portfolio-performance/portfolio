# REST API and MCP server

A local HTTP/JSON API into the *running* Portfolio Performance application, so that scripts and
agents can read and edit the data of open portfolio files. The server listens on the loopback
interface only and is off by default.

It has **two front doors on one port**: this REST API, for `curl`, scripts and the CLI plugin; and
an [MCP endpoint](#the-mcp-endpoint) at `POST /mcp`, for chat clients that cannot run a script. They
are the same operations, and they must answer the same question the same way.

The design decisions behind the API — and the alternatives that were rejected — are recorded in
[ADR 0002](../docs/adr/0002-local-rest-api-for-scripts-and-agents.md); the MCP endpoint's
authentication choice is recorded in
[ADR 0005](../docs/adr/0005-authenticate-the-mcp-endpoint-with-rest-api-tokens.md).

## Enabling it

Preferences → **MCP Server & REST API**:

1. Tick **Enable MCP server and REST API (localhost only)**. Optionally change the port
   (default **5712**).
2. Enable the individual files you want to expose, and optionally give each an **alias**.

Both switches are required: the server only serves a file that is globally enabled *and*
individually enabled. A file that has never been saved has no path and therefore cannot be enabled —
save it first.

## Getting a token (interactive pairing)

Every client holds its **own** bearer token. A program obtains one by asking the user:

```bash
# 1. file an access request — no token needed for this
curl -s -X POST -d '{"clientName": "My Script"}' http://127.0.0.1:5712/v1/auth/requests
# → {"id": "…", "status": "pending"}

# 2. the user approves or declines inside Portfolio Performance; poll every 1–2s
curl -s http://127.0.0.1:5712/v1/auth/requests/<id>
# → {"status": "pending"} … then {"status": "approved", "token": "…"}
```

The approval prompt offers **Allow for this session** (the token dies when the application quits),
**Always allow** (persistent), and **Decline**. The token is delivered **exactly once** — store it.
A request expires after 2 minutes; only one may be pending at a time, and a decline imposes a
cool-down (both reported `429` with `Retry-After`). Every `401` names the pairing endpoint in its
`pairingEndpoint` field, so the flow is discoverable at runtime.

The preference page lists all authorized clients with their last use, lets the user **revoke** each
individually (effective immediately), and can mint a token manually via **Add client** — the path
for headless setups where nobody watches the screen. Tokens are stored hashed; a lost token cannot
be re-displayed, only replaced.

## Talking to it

Base URL `http://127.0.0.1:5712/v1`, bearer token on every request:

```bash
TOKEN=<from pairing, or Preferences → MCP Server & REST API → Add client>
curl -s -H "Authorization: Bearer $TOKEN" http://127.0.0.1:5712/v1/files
```

The API can only be addressed as loopback (`127.0.0.1`, `[::1]`, `localhost`), and any request that
carries an `Origin` header is rejected — a web page cannot reach this API, by design.

## The MCP endpoint

`POST /mcp` speaks the Model Context Protocol over Streamable HTTP, offering sixteen of the
operations above as named tools. Point a client at `http://127.0.0.1:5712/mcp` and give it an
`Authorization: Bearer …` header — **Add client** assembles both, at the one moment the token
exists. Same switch, same port, same tokens; there is nothing extra to install.

- **No session.** No `Mcp-Session-Id` is issued, responses are plain `application/json`, and `GET`
  and `DELETE` answer `405` — an SSE stream would park one of the server's two worker threads, which
  is the reason ADR 0002 already rejected long-polling.
- **Authorisation is per JSON-RPC method.** `initialize`, `tools/list` and `ping` answer without a
  token; `tools/call` requires one and, lacking it, returns HTTP 200 with `isError: true`. A `401`
  is invisible in a chat client's UI, so the refusal has to reach the chat. `/.well-known/*` answers
  `404`: there is no authorization server here.
- **Deliberately outside `/v1`,** because MCP negotiates its own version, and therefore outside the
  additive-only promise.
- **The last refused connection is shown on the preference page.** A client with a wrong token never
  becomes an entry in the client list and reports nothing itself.

The tools themselves — names, descriptions, `inputSchema`, annotations — are `mcp-tools.json` in
this bundle. See [For contributors](#for-contributors) before editing a word of it.

## Vocabulary

The API is deliberately more general than the application's own English wording, because an
"investment account" can just as well be a broker account or a crypto exchange account as a bank
Depot:

| API resource | Model class | Holds |
|---|---|---|
| instrument | `Security` | — |
| cash account | `Account` | cash, in one currency |
| investment account | `Portfolio` | positions in instruments |

An investment account holds **only** instrument positions. Its cash side is a separate cash account,
named by the `referenceCashAccount` field.

## Endpoints

All list responses are an envelope — `{"items": [...]}`, never a bare array — so that fields beside
`items` can be added later without breaking clients. A list response is always complete: v1 has no
pagination and will not start truncating results. Any later paging must be opt-in by the caller.

### `GET /v1/files` — the files you may address

```json
{"items": [
  {"id": "5f3c…", "alias": "main", "label": "portfolio.xml", "path": "/Users/me/portfolio.xml"}
]}
```

Lists only files that are open *and* enabled. Address a file by its `id` or its `alias` in the
`{file}` segment below. Check `path` if it matters to your script which file it is writing to: the
identity is keyed by path, so a *different* file copied over an enabled path inherits that path's
identity and enablement.

### `GET /v1/files/{file}/instruments[/{uuid}]`

```json
{"uuid": "8a1e…", "name": "Apple Inc.", "currencyCode": "USD",
 "isin": "US0378331005", "wkn": "865985", "tickerSymbol": "AAPL", "note": "…"}
```

`isin`, `wkn`, `tickerSymbol` and `note` are omitted when not set.

### `PATCH /v1/files/{file}/instruments/{uuid}`

A **JSON Merge Patch** (RFC 7386), *not* the full target state: fields you omit stay untouched, and
an explicit `null` clears an optional field. Returns the updated instrument.

```bash
curl -s -X PATCH -H "Authorization: Bearer $TOKEN" \
  -d '{"name": "Apple", "note": null}' \
  http://127.0.0.1:5712/v1/files/main/instruments/8a1e…
```

Writable: `name` (non-empty), `isin`, `wkn`, `tickerSymbol`, `note` (string or `null`), and
`currencyCode` (a known currency; **rejected while the instrument has transactions**, matching the
UI's own rule), and `attributes` — a nested merge patch over the custom attributes keyed by
attribute id (`null` clears one; see `openapi.yaml`). Everything else — prices, quote feeds,
events — is read-only in v1.

Any field that is unknown or not writable is a **422, never a silent no-op**: a typo must not look
like success. All violations come back at once so you can fix them in one round-trip.

### `DELETE /v1/files/{file}/instruments/{uuid}`

`204 No Content` on success. **409** if transactions or investment plans reference the instrument —
deleting it in the application would cascade into transaction history, which the API refuses to do
on a client's behalf. Watchlist and taxonomy membership do not block the delete.

### `GET /v1/files/{file}/cash-accounts[/{uuid}]`

```json
{"uuid": "c4b2…", "name": "Cash Account", "currencyCode": "EUR", "note": "…"}
```

### `GET /v1/files/{file}/investment-accounts[/{uuid}]`

```json
{"uuid": "d9f0…", "name": "Broker", "referenceCashAccount": "c4b2…", "note": "…"}
```

## Compatibility

Everything under `/v1` is additive: nothing changes meaning, changes type or disappears, and a
breaking change means `/v2` at a new path. `GET /v1/version` reports the contract version. In return,
a client must **ignore properties it does not know**, and must **accept unknown values in an open
set**: a response field marked `x-extensible-enum` in `openapi.yaml` — problem `type`, `FieldError`
`code`, trade warning `code`, attribute `type`, holding `type` — can gain values within v1, and each
says what an unknown value means. A field with a plain `enum` is closed for v1.

## Writes are not saved

A write mutates the in-memory file and marks it dirty, exactly as if you had edited it in the UI —
the change is visible immediately, and the user saves it (or discards it by closing without saving).
**There is no save endpoint in v1.** If your script needs the change on disk, the user has to press
save.

## Errors

`application/problem+json` (RFC 9457):

```json
{"type": "https://portfolio-performance.info/rest/problems/validation",
 "title": "Validation failed", "status": 422,
 "errors": [{"field": "currencyCode", "code": "unknown-currency", "message": "XYZ is not a known currency"}]}
```

| Status | `type` | When |
|---|---|---|
| 400 | `invalid-request` | body is not a JSON object, or an unknown query parameter — see `errors` |
| 401 | `unauthorized` | missing or wrong bearer token; the body's `pairingEndpoint` says where to pair |
| 403 | `forbidden-host` | not addressed as loopback |
| 403 | `browser-origin-forbidden` | request carries an `Origin` header |
| 404 | `not-found` | unknown file, **file not enabled**, or unknown entity |
| 409 | `file-not-open` | file is enabled but not currently open — a human has to open it |
| 409 | `ambiguous-alias` | alias matches several records; use the UUID |
| 409 | `delete-blocked` | instrument is referenced by transactions or plans |
| 422 | `validation` | one or more fields rejected; see `errors` |
| 423 | `user-interaction` | a dialog is open in the app — **retry**, see `Retry-After` |
| 429 | `pairing-pending` | another pairing request awaits the user — retry after `Retry-After` |
| 429 | `pairing-cooldown` | a pairing request was declined — retry after `Retry-After` |

Two of these regularly surprise clients:

**404 does not mean "does not exist."** A file that exists but is not enabled answers exactly like a
file that does not exist. That is deliberate: the token must not let a client enumerate files the
user did not share.

**423 is normal and temporary.** While the user has a modal dialog open, writes are rejected rather
than silently clobbered by whatever the dialog writes back on OK. Reads still work. Respect
`Retry-After` and try again.

## Computed collections

Some collections are not stored in the file but computed per request — `trades` is the first. Two
rules apply to all of them.

**They have no ids.** A computed item gets no `uuid`, and there is no `…/trades/{id}`: an id minted
per request would not survive the next one. Address such an item by the filter that produced it
(`GET .../instruments/{uuid}/trades?status=closed`). Ordering carries the weight identity otherwise
would, so these collections sort stably — repeated identical requests return identical lists.

**They can half succeed.** Where the computation runs over independent units and can fail on some of
them, the response is `200` with the results it has plus a `warnings` array naming each unit that
failed and why. One unreconcilable security must not make the resource unreadable for the other four
hundred, and `problem+json` stays reserved for a *total* failure — a response is either a problem or
a result, never both. `warnings` is always present, empty when nothing failed, so that "nothing went
wrong" is distinguishable from "this endpoint cannot report problems". **A client that ignores
`warnings` reads a partial list as a complete one.**

## Query parameters are strict

An endpoint accepts only the query parameters documented for it, and one that documents none accepts
none. Anything else is `400 invalid-request` with one `errors` entry per offending name
(`code: unknown-parameter`) naming what the endpoint does accept:

```json
{"type": "https://portfolio-performance.info/rest/problems/invalid-request",
 "title": "Invalid request", "status": 400,
 "errors": [{"field": "currency", "code": "unknown-parameter",
             "message": "unknown query parameter 'currency'; this endpoint accepts: date, reportingCurrency"}]}
```

Ignoring the parameter instead would be worse than it sounds: the endpoint still computes, from
defaults, and answers `200` with a full report. `?currency=USD` — a plausible misremembering of
`reportingCurrency` — would return the base-currency statement of assets, and only a client that
compared the echoed `reportingCurrency` against what it sent would ever notice.

**Names are case-sensitive.** `reportingcurrency` is rejected rather than read as
`reportingCurrency`; accepting it would mean guessing, which is the failure being avoided. The
message points at the name that was meant, since working from memory is exactly how the case gets
lost.

**Each name at most once.** A repeated name is rejected (`code: duplicate-parameter`) rather than
reduced to one of its values: `?metrics=risk&metrics=gains` would otherwise answer as if only `gains`
had been asked for. A list parameter takes one comma-separated value — `?metrics=risk,gains`.

## Naming

Three registers, each uniform. What you *name* is `camelCase`; what you *choose* and what appears in
a URL is `lower-kebab`.

| | Style | Examples |
|---|---|---|
| Query parameter names | `camelCase` | `reportingCurrency`, `costMethod`, `openingDate`, `taxesAndFees` |
| JSON field names | `camelCase` | `holdingPeriodDays`, `periodCostBasis`, `referenceCashAccount` |
| Path segments | `lower-kebab` | `cash-accounts`, `investment-accounts`, `attribute-types` |
| Enum values | `lower-kebab` | `moving-average`, `cash-account`, `per-lot`, `limit-price` |
| `FieldError` codes, problem types | `lower-kebab` | `unknown-parameter`, `no-activity-in-period` |

A query string therefore shows both registers at once — `?costMethod=moving-average` — and the `=` is
the boundary. That looks mixed and is not: no identifier anywhere is kebab, and no value is camel
except the case below.

**The exception: a value that names a field is spelled like a field.** The `metrics` values are
*field references*, not ordinary enum values — each one is literally the response property it
switches on, so `?metrics=timeWeighted` turns on the `timeWeighted` object and the echoed
`"metrics": ["timeWeighted"]` matches the key it enabled. Spelling them `time-weighted` would make a
caller translate between what it asks for and what it gets back. Five of the seven are single words,
so only `moneyWeighted` and `timeWeighted` show the difference.

This applies only where the value genuinely *is* a field name. `per-lot`, `moving-average` and
`cash-account` name no property and are kebab like every other value.

## For contributors

The plugin depends on the model plugin and **must not depend on the UI plugin**. The UI side is
reached through the SPI interfaces in `rest/spi/` (`HostApplication`, `OpenFile`,
`ApiAccessRequest`), implemented by `name.abuchen.portfolio.ui/…/ui/addons/RestApiAddon.java`, which
also starts and stops the server and shows the pairing approval dialog. Tokens live in
`ClientStore` (persistent clients as SHA-256 hashes in an owner-only JSON file in the plugin state
location, session clients in memory only); the pairing state machine is `PairingService`.

`mcp-tools.json` is **hand-authored and guarded by a test**, like `openapi.yaml` — there is no
generator and no `package.json`, and none is to be introduced. `McpToolsDriftTest` holds it against
the routing table: a tool pointing at a route that does not exist, a query binding the route would
reject, or an annotation that contradicts the method all fail the build. Two things about that file
are not ordinary code:

- **It is published contract.** Claude Desktop hashes a tool's description and parameters into the
  user's "always allow" grant, so revising wording silently revokes it. Change the words
  deliberately, in a release, on purpose.
- **It is English only** and excluded from translation: it is read by a model, not by a user.

The endpoint that serves it lives in `rest/internal/mcp/` and dispatches *into* `ApiRoutes` rather
than beside it, so UI-thread marshalling, file-scope resolution and the modal write gate are solved
once. `McpEnvelope` and `McpExplain` hold the result envelope and problem explanations;
`McpEndpoint`'s javadoc carries the transport reasoning.

Adding an endpoint means writing a handler and a serializer, and registering the route in
`ApiRoutes`. The cross-cutting concerns are in the pipeline and cannot be forgotten per endpoint:
authentication and the loopback/Origin checks in `RestApiServer`, the query-parameter check in
`Router.add(…)`, file-scope resolution in `FileResolver`, and — via the `read(…)` / `write(…)`
wrappers in `ApiRoutes` — UI-thread marshalling plus the modal-dialog gate on writes. Note that
handler classes keep the *model's* vocabulary (`SecuritiesHandler`), while the routes and
user-facing strings use the API vocabulary.

A route's query parameters are the trailing arguments of `router.add(…)`; naming none means the
endpoint takes none, so a new endpoint is strict without doing anything. Document them in
`openapi.yaml` in the same change, and give the operation a `400` response — every endpoint can
now return one. `OpenApiSpecDriftTest` fails if any of that drifts apart, and `McpToolsDriftTest`
fails until the new route either has a tool or is listed as one that deliberately has none.

The drift tests compare the specification with the code; the `openapi-compat` Maven profile
compares it with the published contract. It runs [oasdiff](https://github.com/oasdiff/oasdiff)
against `openapi.yaml` as tagged `rest-api-v1.0.0`, and fails on anything that would break a
1.0 client: a removed path, parameter or response property, a changed type, or a new value in
a closed enum (if the set is meant to grow, declare it `x-extensible-enum` instead). It needs Go
on the `PATH`; add `-Popenapi-compat` to any `verify` command that builds this bundle. When the
tag is pushed, add `-Popenapi-compat` next to `-Popenapi-lint` in both Maven jobs of
`.github/workflows/main.yml`. Until then the profile fails, because there is no baseline.

Run the tests:

```bash
mvn -f portfolio-app/pom.xml verify -Plocal-dev -o \
  -pl :portfolio-target-definition,:name.abuchen.portfolio.pdfbox1,:name.abuchen.portfolio.pdfbox3,\
:name.abuchen.portfolio,:name.abuchen.portfolio.junit,:name.abuchen.portfolio.rest,\
:name.abuchen.portfolio.rest.tests -am -amd
```
