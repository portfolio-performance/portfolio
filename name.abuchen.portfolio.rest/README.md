# REST API

A local HTTP/JSON API into the *running* Portfolio Performance application, so that scripts and
agents can read and edit the data of open portfolio files. The server listens on the loopback
interface only and is off by default.

The design decisions behind the API — and the alternatives that were rejected — are recorded in
[ADR 0002](../docs/adr/0002-local-rest-api-for-scripts-and-agents.md).

## Enabling the API

Preferences → **REST API**:

1. Tick **Enable REST API (localhost only)**. Optionally change the port (default **5712**).
2. Copy the **access token**.
3. Enable the individual files you want to expose, and optionally give each an **alias**.

Both switches are required: the server only serves a file that is globally enabled *and*
individually enabled. A file that has never been saved has no path and therefore cannot be enabled —
save it first.

**Regenerate** issues a new token and thereby revokes access for every existing client.

## Talking to it

Base URL `http://127.0.0.1:5712/v1`, bearer token on every request:

```bash
TOKEN=<paste from preferences>
curl -s -H "Authorization: Bearer $TOKEN" http://127.0.0.1:5712/v1/files
```

The API can only be addressed as loopback (`127.0.0.1`, `[::1]`, `localhost`), and any request that
carries an `Origin` header is rejected — a web page cannot reach this API, by design.

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

All list responses are an envelope — `{"items": [...]}`, never a bare array — so that pagination can
be added later without breaking clients.

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
UI's own rule). Everything else — prices, quote feeds, attributes, events — is read-only in v1.

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
| 400 | `invalid-request` | body is not a JSON object |
| 401 | `unauthorized` | missing or wrong bearer token |
| 403 | `forbidden-host` | not addressed as loopback |
| 403 | `browser-origin-forbidden` | request carries an `Origin` header |
| 404 | `not-found` | unknown file, **file not enabled**, or unknown entity |
| 409 | `file-not-open` | file is enabled but not currently open — a human has to open it |
| 409 | `ambiguous-alias` | alias matches several records; use the UUID |
| 409 | `delete-blocked` | instrument is referenced by transactions or plans |
| 422 | `validation` | one or more fields rejected; see `errors` |
| 423 | `user-interaction` | a dialog is open in the app — **retry**, see `Retry-After` |

Two of these regularly surprise clients:

**404 does not mean "does not exist."** A file that exists but is not enabled answers exactly like a
file that does not exist. That is deliberate: the token must not let a client enumerate files the
user did not share.

**423 is normal and temporary.** While the user has a modal dialog open, writes are rejected rather
than silently clobbered by whatever the dialog writes back on OK. Reads still work. Respect
`Retry-After` and try again.

## For contributors

The plugin depends on the model plugin and **must not depend on the UI plugin**. The UI side is
reached through the two SPI interfaces in `rest/spi/` (`HostApplication`, `OpenFile`), implemented by
`name.abuchen.portfolio.ui/…/ui/addons/RestApiAddon.java`, which also starts and stops the server.

Adding an endpoint means writing a handler and a serializer, and registering the route in
`ApiRoutes`. The cross-cutting concerns are in the pipeline and cannot be forgotten per endpoint:
authentication and the loopback/Origin checks in `RestApiServer`, file-scope resolution in
`FileResolver`, and — via the `read(…)` / `write(…)` wrappers in `ApiRoutes` — UI-thread marshalling
plus the modal-dialog gate on writes. Note that handler classes keep the *model's* vocabulary
(`SecuritiesHandler`), while the routes and user-facing strings use the API vocabulary.

Run the tests:

```bash
mvn -f portfolio-app/pom.xml verify -Plocal-dev -o \
  -pl :portfolio-target-definition,:name.abuchen.portfolio.pdfbox1,:name.abuchen.portfolio.pdfbox3,\
:name.abuchen.portfolio,:name.abuchen.portfolio.junit,:name.abuchen.portfolio.rest,\
:name.abuchen.portfolio.rest.tests -am -amd
```
