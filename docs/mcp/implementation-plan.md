# Implementation plan: read/write MCP access to a running PP instance

Status: draft 2026-09-24, derived from `docs/mcp/requirements.md`. Branch `feature/mcp-rw` (== `master` e7fead842 at the time of writing).

Conventions in this document:

- `rest/…` abbreviates `name.abuchen.portfolio.rest/src/name/abuchen/portfolio/rest/…`; `rest.tests/…` abbreviates `name.abuchen.portfolio.rest.tests/src/name/abuchen/portfolio/rest/…`; `ui/…` abbreviates `name.abuchen.portfolio.ui/src/name/abuchen/portfolio/ui/…`; `model/…` abbreviates `name.abuchen.portfolio/src/name/abuchen/portfolio/model/…`.
- Line numbers for REST files refer to `upstream/feature/rest-api` (#5870) or, where a file only exists there, `manueldeprada/rest-ext`. Line numbers for PP core/UI files refer to `master`. Rebasing does not move lines inside the REST files.
- "UI thread" means the SWT display thread reached through `HostApplication#syncExec`.

## 1. Findings summary

### 1.1 Rebase result (tested in a throwaway worktree)

| Step | Result |
| --- | --- |
| `git rebase master` of the 17 commits of `upstream/feature/rest-api` | 2 conflicts, both in the first commit `c7a08f162`, both "both sides added a line": `name.abuchen.portfolio.bootstrap/Application.e4xmi` (master added `UpdateIndicatorAddon`, #5870 adds `RestApiAddon` — keep both `<addons>` lines) and `name.abuchen.portfolio.ui/META-INF/MANIFEST.MF` (`Require-Bundle` list: keep master's `bootstrap;bundle-version="0.87.1"` and add `name.abuchen.portfolio.rest;bundle-version="0.87.1"`). The remaining 16 commits applied cleanly. |
| `git rebase --onto <rebased #5870> upstream/feature/rest-api` of the 20 commits of `manueldeprada/rest-ext` | 0 conflicts. |
| Version bump | Required, otherwise Tycho refuses the build: `name.abuchen.portfolio.rest/pom.xml:9` and `name.abuchen.portfolio.rest.tests/pom.xml:9` (`0.85.1-SNAPSHOT` → `0.87.1-SNAPSHOT`), `name.abuchen.portfolio.rest/META-INF/MANIFEST.MF:5,8` and `name.abuchen.portfolio.rest.tests/META-INF/MANIFEST.MF:5` (`0.85.1.qualifier` → `0.87.1.qualifier`, `bundle-version="0.85.1"` → `"0.87.1"`). |
| Build of the rebased tree | `mvn -f portfolio-app/pom.xml verify -Plocal-dev -pl :portfolio-target-definition,:name.abuchen.portfolio.pdfbox1,:name.abuchen.portfolio.pdfbox3,:name.abuchen.portfolio,:name.abuchen.portfolio.junit,:name.abuchen.portfolio.rest,:name.abuchen.portfolio.rest.tests -am -amd` → BUILD SUCCESS, **256 tests, 0 failures**. |
| Compile of `name.abuchen.portfolio.ui` against the rebased tree | `mvn -f portfolio-app/pom.xml compile -Plocal-dev -pl :portfolio-target-definition,:name.abuchen.portfolio.pdfbox1,:name.abuchen.portfolio.pdfbox3,:name.abuchen.portfolio,:name.abuchen.portfolio.rest,:name.abuchen.portfolio.bootstrap,:name.abuchen.portfolio.ui -am -amd` → BUILD SUCCESS (655 UI sources, including `RestApiAddon`, `RestApiPreferencePage`, `EditorActivationState`). The UI tests were not run. |

The rebased result is kept as local branches in this repository: `scratch/rebase-test` (rebased #5870 head) and `scratch/rest-ext` (rebased rest-ext plus one commit "scratch: bump rest plugin versions to 0.87.1"). Nothing was committed to `feature/mcp-rw`. The implementer can either replay the recipe in step 1 or `git reset --hard scratch/rest-ext` on `feature/mcp-rw`; both produce the same tree.

**Recommendation: rebase, not merge.** The conflicts are two one-line hunks, the result is linear and every #5870/rest-ext commit stays a focused, upstream-PR-able commit (A7). A merge of master into the branch would resolve the same two hunks but leave a merge commit and 130 unrelated master commits in the branch history.

### 1.2 What #5870 provides (reusable as is)

- Plugin `name.abuchen.portfolio.rest` (depends on `name.abuchen.portfolio`, `com.google.gson`, `org.eclipse.core.runtime`, `org.eclipse.equinox.preferences`; must not depend on the UI plugin — `rest/README.md` "For contributors"). Test fragment `name.abuchen.portfolio.rest.tests` (JUnit 4, hamcrest, `name.abuchen.portfolio.junit` builders).
- `RestApiServer` (`rest/RestApiServer.java`): `com.sun.net.httpserver.HttpServer` bound to loopback (l.53), 2 worker threads (l.55), Host check (l.122-155), Origin check (l.157-161), bearer check with pairing/OpenAPI exemptions (l.163-186), `ApiException` → problem+json (l.188-192), any other exception → 500 `internal-error` and `PortfolioLog.error` (l.96-100).
- `Router` (`rest/internal/Router.java`): first-match segment router, `{param}` placeholders, 405 when path matches but method does not (l.30-48), `routeSignatures()` for the drift test (l.55-60).
- `Request` record (`rest/internal/Request.java:8-10`): method, path, pathParams, queryParams, body. **No headers** are exposed to handlers.
- `Response` record (`rest/internal/Response.java`): `json(status, JsonElement)`, `of(...)`, `noContent()`.
- `ApiException` (`rest/internal/ApiException.java`): RFC 9457 fields; factories `unauthorized`, `forbiddenHost`, `forbiddenOrigin`, `notFound`, `badRequest(detail|errors)`, `validation(errors)` (422), `conflict(type,title,detail,errors)` (409), `tooManyRequests`, `locked()` (423 `user-interaction`, `Retry-After: 5`, l.101-105). `FieldError(field, code, message)` (l.15-17).
- `ApiRoutes` (`rest/ApiRoutes.java`): three wrappers — `read(resolver, host, (client, req) -> …)` runs on the UI thread after resolving `{file}` (l.113-120); `calc(...)` resolves the file on the UI thread and computes on the worker thread with a `CalcContext(client, factory)` (l.135-145); `write(resolver, host, (file, req) -> …)` resolves, then throws `ApiException.locked()` if `host.isUserEditing()`, then runs the body on the UI thread (l.147-159). `parseObject(request)` turns the body into a `JsonObject` or a 400 (l.161-174).
- `FileResolver` (`rest/internal/FileResolver.java:29-48`): `{file}` = UUID or alias from `FileAccessRegistry`; not enabled/unknown → 404, alias ambiguous → 409 `ambiguous-alias`, enabled but not open → 409 `file-not-open`.
- `FileAccessRegistry` (`rest/FileAccessRegistry.java`): instance-scope preferences node `name.abuchen.portfolio.rest/files/<base64url(absolutePath)>` with keys `uuid`, `enabled`, `alias` (l.57-62, 158-163, 178-186).
- `ClientStore` (`rest/ClientStore.java`): tokens as SHA-256/base64url hashes in `<plugin state location>/api-clients.json` (l.74, 183-195, 276-305). `PairingService` (`rest/PairingService.java`): interactive approval through `HostApplication#requestApiAccessApproval`.
- SPI (`rest/spi/`): `HostApplication` (`listOpenFiles`, `syncExec`, `isUserEditing`, `requestApiAccessApproval`; `HostApplication.java:10-30`) and `OpenFile` (`getPath`, `getLabel`, `getClient`, `getExchangeRateProviderFactory`; `OpenFile.java:10-26`). Implemented by `ui/addons/RestApiAddon.java` (`ClientInputOpenFile` l.52-86, `Host` l.88-141: `Display.getDefault().syncExec`, `isUserEditing = modal shell open || EditorActivationState.isAnyEditorActive()`).
- JSON: Gson tree API (`JsonObject`/`JsonArray`), no DTO classes. Serializers live in `rest/internal/EntityJson.java`; numbers are emitted with `EntityJson.decimal(long value, int precision)` which produces a plain JSON number literal via `BigDecimal.toPlainString()` (rest-ext `EntityJson.java:916-935`), money as `{"value": 12.34, "currency": "EUR"}` (l.524-530). Lists are enveloped `{"items": [...]}` (l.63-76).
- Write handler pattern (`rest/internal/SecuritiesHandler.java`): a `WRITABLE_FIELDS` map of validator+setter+getter (l.60-74); `patch()` collects all `FieldError`s first and applies nothing unless everything validates (l.125-153); an empty patch does not mark the file dirty (l.155-159); after applying, `client.markDirty()` (l.199, 336); changes are logged to the application log via `InstrumentChangeLog` (`rest/internal/InstrumentChangeLog.java`, `rest/messages*.properties`). Delete refuses referenced entities with 409 `delete-blocked` (l.321-338).
- OpenAPI: hand-written `name.abuchen.portfolio.rest/openapi.yaml`, served at `GET /v1/openapi.yaml`; `rest.tests/OpenApiSpecDriftTest.java` fails the build if a route is undocumented or a documented operation has no route (l.65-79) and if a `FieldError` code is emitted but not listed in the spec's `FieldError.code` enum, or vice versa (l.81-97; scan pattern l.49 requires the code to be a same-line string literal in `new ApiException.FieldError(field, "code", …)`).
- Tests (`rest.tests/…`): handler unit tests call the static handler methods directly on in-memory `Client`s built with `SecurityBuilder`/`AccountBuilder`/`PortfolioBuilder` (`PatchSecurityTest.java:38-50`); routing/423 tests build a `Router` with `ApiRoutes.create(new FileAccessRegistry(node), new FakeHost(...), new PairingService(...))` and call `match(...).handler().handle(new Request(...))` (`PatchSecurityTest.java:172-206`); `FakeHost` (`rest.tests/testsupport/FakeHost.java`) records `syncExec` use so tests can assert that the model was only touched on the UI thread; `RestApiServerTest` starts a real server on port 0 (`RestApiServerTest.java:34-57`).
- UI side: `ui/preferences/RestApiPreferencePage.java` (enable, port, per-file enable/alias, client list, revoke, "Add client" mint) registered as page id `restapi` (`ui/handlers/OpenPreferenceDialogHandler.java:101`); `ui/dialogs/ApiAccessApprovalDialog.java`; `ui/editor/EditorActivationState.java` gains static `isAnyEditorActive()`.
- Build: `portfolio-app/pom.xml` modules (l.52-53 after rebase), `name.abuchen.portfolio.feature/feature.xml` plugin entry, `portfolio-build-tools/pom.xml` and `portfolio-app/releng/translation-config.xml` entries for `rest/messages`, `portfolio-app/eclipse/launches.lc` launch configs, `.github/workflows/main.yml` `openapi-lint` job, `redocly.yaml`.

### 1.3 What `manueldeprada/rest-ext` adds (reusable as is)

Read endpoints, all `GET`: `/transactions` (`TransactionsHandler`, uses `Client#getAllTransactions()` which de-duplicates buy/sell and transfer pairs, `model/Client.java:529-547`), `/instruments/{uuid}/prices` (`SecurityPricesHandler`), `/taxonomies` (`TaxonomiesHandler`), `/trades` (`TradesHandler`), `/performance/series`, `/performance/securities`, `/performance/calendar` (`PerformanceHandler`, `SecurityPerformanceHandler`, `PerformanceCalendarHandler`), holdings enriched with cost basis, P&L, TTWROR/IRR, dividends, classifications, FX and technicals (`HoldingsHandler`, `HoldingsContext`), balances on `/cash-accounts` and values on `/investment-accounts`. Transaction wire format: `uuid, date, type (kebab-case), value (signed), grossValue, fees, taxes, shares, security{uuid,name}, owner{uuid,name,type}, note` (`EntityJson.java:963-1064`).

### 1.4 What `andreevdm/mcp` is useful for (reference only)

`name.abuchen.portfolio/src/name/abuchen/portfolio/mcp/MCPTools.java` shows the minimal create code for every transaction kind (l.286-450): `AccountTransferEntry` with a `GROSS_VALUE` unit on the source leg for FX transfers (l.331-333), `PortfolioTransferEntry` (l.352-366), deliveries (l.369-391), `BuySellEntry` with fee/tax units on the portfolio leg (l.393-418), simple account transactions (l.420-450), and update/sync-leg logic (l.481-570, 777-860). Its numbers are `double`s (`Values.Amount.factorize(double)`), which violates N6; its validation is thinner than the UI dialogs. We reuse the structure, not the code. Its save path (`ui/mcp/UIClientSource.java:33-45`) calls `ClientFactory.save` directly and bypasses `ClientInput` (no backup, no dirty reset, no `SAVED` event) — do not copy that.

### 1.5 PP internals that matter (evidence)

- **Dirty/refresh chain.** `Client#markDirty()` fires property `"dirty"` (`model/Client.java:683-686`). `ClientInput` registers one listener for all properties (`ui/editor/ClientInput.java:712-719`) → `scheduleDirty(recalculate)`, which runs synchronously when already on the display thread (l.152-156) → `setDirty(true, true)` (l.183-190) → every `PortfolioPart.onDirty` (tab asterisk, `ui/editor/PortfolioPart.java:380-383`) and `onRecalculationNeeded` (l.386-390) → `AbstractFinanceView.onRecalculationNeeded` → `notifyModelUpdated()` deferred until no cell editor is active (`ui/editor/AbstractFinanceView.java:105-113`). Therefore a REST write running inside `syncExec` only needs `client.markDirty()`; when `syncExec` returns, `ClientInput#isDirty()` is already true and the visible view has been asked to refresh. `Client#addAccount/removeAccount/addPortfolio/removePortfolio/addPlan/removePlan` fire no event (`Client.java:201-209, 326-336, 357-366`), `addSecurity/removeSecurity/add-removeWatchlist/add-removeTaxonomy` do (l.234-255, 305-324, 419-438); calling `markDirty()` after every write is the uniform rule.
- **Save.** `ClientInput.save(Shell)` (`ClientInput.java:238-288`) only falls back to "save as" when `clientFile == null` (l.240-253); encryption never forces a dialog because the derived key is kept on the client (`model/ClientFactory.java:399, 426`) and `ClientFactory.save(client, file)` re-uses `client.getSaveFlags()` (XML/BINARY/COMPRESSED/ENCRYPTED, `ClientFactory.java:677-688`). The non-dialog part is l.258-280: optional backup (preference `CREATE_BACKUP_BEFORE_SAVING`), `ClientFactory.save`, `storePreferences(false)`, `broker.post(UIConstants.Event.File.SAVED, path)`, reset of pending dirty flags, `listeners.forEach(ClientInputListener::onSaved)`. The method wraps everything in `BusyIndicator.showWhile` and swallows `IOException` into an `ErrorDialog` (l.283-286), so it cannot be called from the API as is. `ClientInputFactory#listOpenClients()` (`ui/editor/ClientInputFactory.java:86-89`) enumerates open inputs; new-unsaved files (`getFile()==null`) are filtered out by `RestApiAddon.Host.listOpenFiles` (l.93-96).
- **Transaction construction rules** (mirrored from the dialogs, all in `ui/dialogs/transactions/`): units are added after `setCurrencyCode` because `Transaction#addUnit` rejects a currency mismatch (`model/Transaction.java:457-468`); a `GROSS_VALUE` unit must carry forex+rate (`Transaction.java:55-64`) and is written only when security currency ≠ transaction currency (`AbstractSecurityTransactionModel.java:157-192`); FEE/TAX units are in the transaction currency, with forex variants when FX applies (same lines); buy/sell units go on the **portfolio leg only** (`BuySellModel.java:60-103`); `BuySellEntry#setShares` writes only the portfolio leg (`model/BuySellEntry.java:81`); `BuySellEntry#insert()` adds to the account first because `Account#addTransaction` throws on currency mismatch (`BuySellEntry.java:131-137`, `model/Account.java:138-148`); total = converted gross ± fees ± taxes by type (`AbstractSecurityTransactionModel.java:651-666`, `AccountTransactionModel.java:693-698`); a BUY/SELL requires shares > 0 and a non-zero gross and total, SELL with zero total is refused with the hint to use an outbound delivery, DELIVERY_OUTBOUND may have zero amount (`AbstractSecurityTransactionModel.java:208-242`); DIVIDENDS requires a security but allows 0 shares, TAXES/TAX_REFUND/FEES/FEES_REFUND accept an optional security, DEPOSIT/REMOVAL/INTEREST/INTEREST_CHARGE have none, `exDate` must not be after the date and needs a security (`AccountTransactionModel.java:87-97, 100-172, 192-222, 299-315`); account transfers: both accounts required and different, `GROSS_VALUE` on the source leg with the inverse rate when currencies differ (`AccountTransferModel.java:62-129, 183-196`); security transfers: shares ≠ 0, amount ≠ 0, currency = security currency (`SecurityTransferModel.java:62-105, 118-135`). Tolerances: gross within `shares × (quote ± 0.01)`, converted within `gross × (rate ± 0.0001)`, `Transaction.Unit` itself tolerates `rate ± 0.003` (`Transaction.java:99-139`). Precision: `Values.Share` 8 decimals, `Values.Amount`/`Money` 2, `Values.Quote` 8, `Values.Weight` 2 (`money/Values.java:227, 239, 288, 302, 406`).
- **Delete/edit of linked legs.** `TransactionOwner#deleteTransaction(tx, client)` removes the cross leg and both plan links (`model/TransactionOwner.java:30-43`); `CrossEntry#updateFrom(tx)` syncs date/security/note/source to the other leg (`BuySellEntry.java:140-160`, `AccountTransferEntry.java:151-166`, `PortfolioTransferEntry.java:141-158`). Changing the owner keeps the objects and UUIDs with delete → `setOwner` → `insert()` (`ui/util/viewers/TransactionOwnerListEditingSupport.java:199-203`).
- **Quotes.** `UpdateQuotesJob` no longer exists; the job is `ui/jobs/priceupdate/UpdatePricesJob.java` (constructors l.62-83: `(Client, Set<Target>)`, `(Client, List<Security>)`, `(Client, Predicate<Security>, Set<Target>)`; `Target {LATEST, HISTORIC}` l.48-51; `suppressAuthenticationDialog(boolean)` l.85). It depends on SWT `Display` (l.120, 260-264) and its task classes are package-private, so the rest plugin cannot use it; the UI addon can. Completion is observed with `Job#join()` or an `IJobChangeListener` (`ui/jobs/priceupdate/PeriodicUpdatePricesJob.java:56`); `client.markDirty()` is called by the job itself (l.158-162, 182-183). `AbstractClientJob#belongsTo(client)` allows `Job.getJobManager().join(client, monitor)` (`ui/jobs/AbstractClientJob.java:7-27`).
- **Stock split.** `ui/wizards/splits/StockSplitModel.java:109-137`: adds `SecurityEvent(exDate, STOCK_SPLIT, newShares + ":" + oldShares)`, multiplies shares of every transaction dated before `exDate` (portfolio transactions and dividend/interest/tax/fee account transactions from `security.getTransactions(client)`), divides every `SecurityPrice` before `exDate`; latest price untouched; no `markDirty` (caller does it). Validation `SelectSplitPage.java:132-178`: exDate required, both ratios > 0, new ≠ old. Only UI import is `BindingHelper`, so the 25 lines port to the rest plugin unchanged.
- **PDF import.** `name.abuchen.portfolio/src/name/abuchen/portfolio/datatransfer/pdf/PDFImportAssistant.java`: `new PDFImportAssistant(client, List<File>)`, `run(IProgressMonitor, Map<File, List<Exception>> errors)` returns `Map<Extractor, List<Extractor.Item>>` (l.167-260); first extractor with a non-empty result wins, PDFBox 1 fallback; no UI dependency. Items: `TransactionItem`, `BuySellEntryItem`, `AccountTransferItem`, `PortfolioTransferItem`, `SecurityItem`, `SecurityUpdateItem`, `SecurityPriceItem`, `SkippedItem` (`datatransfer/Extractor.java:59-810`). `Item#apply(ImportAction, ImportAction.Context)` resolves the owner from the item's own primary/secondary account/portfolio or from `Context#getAccount(currency)`, `getPortfolio()`, `getSecondaryAccount(currency)`, `getSecondaryPortfolio()` (`datatransfer/ImportAction.java:17-26`). Checks the wizard runs, in order (`ui/wizards/datatransfer/ReviewExtractedItemsPage.java:654-710`): `CheckTransactionDateAction`, `CheckValidTypesAction`, `CheckSecurityRelatedValuesAction`, `DetectDuplicatesAction(client)`, `CheckCurrenciesAction`, `CheckForexGrossValueAction` (all in `datatransfer/actions/`). Commit: `InsertAction(client)` with `setConvertBuySellToDelivery/setRemoveDividends/setInvestmentPlanItem` (`datatransfer/actions/InsertAction.java:33-46`), driven by `ui/wizards/datatransfer/ImportController.java:107-140`, then `client.markDirty()`.
- **CSV import.** `datatransfer/csv/CSVImporter.java:831-840` (`new CSVImporter(client, file)`), `CSVConfig#fromJSON(org.json.simple.JSONObject)` / `writeTo(importer)` (`CSVConfig.java:213, 133`), then `importer.processFile(false)` and `importer.createItems(errors)` (`CSVImporter.java:1003, 1061-1070`). Same items and checks as PDF. Extractor codes: `account-transaction`, `portfolio-transaction`, `investment-vehicle`, `investment-vehicle-price`, `portfolio`. Requires `org.json.simple` (`CSVConfig.java:12-13`) — the rest plugin needs an `Import-Package` for it.
- **Investment plans.** `model/InvestmentPlan.java`: no UUID; `generateTransactions(CurrencyConverter)` inserts directly into the model and returns the new pairs (l.413-431), throws `IOException` when a price is missing; `getDateOfNextTransactionToBeGenerated()` l.391; `next(LocalDate)` is private (l.325). UI trigger `ui/views/InvestmentPlanListView.java:380-410` calls `markDirty()` afterwards.
- **Identifiers.** `Security`, `Account`, `Portfolio`, `Transaction` have UUIDs; `Taxonomy#getId()` and `Classification#getId()` exist; **`Watchlist` (`model/Watchlist.java`) and `InvestmentPlan` have no UUID**; `SecurityEvent` has no identifier (fields date/type/details/source, `model/SecurityEvent.java:127-190`).
- **Master data deletion rules.** Securities: refused if `security.getTransactions(client)` is non-empty (`ui/views/SecuritiesTable.java:1055-1087`; #5870 additionally refuses plan references). Accounts/portfolios: delete only enabled when `getTransactions().isEmpty()` (`ui/views/AccountListView.java:338-352`, `ui/views/PortfolioListView.java:356-369`).
- **Product/workspace.** Default workspace on Windows `%LOCALAPPDATA%\PortfolioPerformance\workspace` (`portfolio-product/name.abuchen.portfolio.product:76`), overridable with the standard Eclipse launcher argument `-data <dir>`. The product is materialized by `tycho-p2-director-plugin` into `portfolio-product/target/products/name.abuchen.portfolio.product/win32/win32/x86_64/portfolio/` (`portfolio-product/pom.xml:240-262`, root folder `portfolio`, launcher `PortfolioPerformance`). Open files are restored from the part's persisted state key `file` (`ui/editor/PortfolioPart.java:104-111`, `name.abuchen.portfolio.bootstrap/src/name/abuchen/portfolio/bootstrap/LifeCycleManager.java:236-243`); there is no command-line argument to open a file.

UI compile check: passed (table in 1.1). The #5870 UI files that touch master-changed code are `ui/editor/EditorActivationState.java` (static `activeViewer`, `isAnyEditorActive()`) and `ui/util/viewers/ColumnEditingSupport.java:165` (`createListener()` → `createListener(viewer)`); both are adjusted inside commit `c7a08f162` and compile on master. Note that `EditorActivationState.deferUntilNotEditing` lacks a `return` after the feature-disabled branch (master l.43-49, #5870 l.84-89), so with the experiment off the runnable runs twice; harmless for us, worth an upstream fix.

### 1.6 Gaps versus the requirements

| Requirement | Delivered by | Gap |
| --- | --- | --- |
| F1 transactions list | rest-ext `GET /transactions` | no filters, no single-GET, no create/update/delete |
| F2 instruments | #5870 PATCH (6 fields, attributes), DELETE; rest-ext prices GET | no POST, no feed/retired/events/prices writes |
| F2 accounts/portfolios/watchlists/plans/taxonomies | GETs only (taxonomies, accounts, portfolios) | all writes; watchlist and plan reads |
| F3 | rest-ext | per-portfolio/account filtering, taxonomy allocation, dividends list, rebalancing |
| F4 | nothing | dirty flag, save, quotes, split, PDF, CSV, plan generation |
| N5 idempotency, A6 dry_run | nothing | everything |
| A4 Python adapter | nothing | everything |

## 2. Design

### 2.1 Architecture

```
Claude / MCP client  --stdio-->  tools/pp-mcp (FastMCP, Python)  --HTTP loopback-->  name.abuchen.portfolio.rest
                                                                                          |  (HostApplication SPI)
                                                                                    name.abuchen.portfolio.ui (RestApiAddon)
                                                                                          |  ClientInput / Display.syncExec
                                                                                    name.abuchen.portfolio (model)
```

Nothing changes in this picture relative to #5870 except that the SPI gains a handful of methods (dirty, save, price update, job status) and the rest plugin gains write handlers, an import-session registry and a job registry. All new handlers keep the `read/calc/write` wrappers of `ApiRoutes` so UI-thread marshalling and the 423 gate cannot be forgotten.

### 2.2 Conventions template

**GET handler** (mirrors `SecuritiesHandler.list/get`, `ApiRoutes.java:59-66`):

```java
// rest/internal/WatchlistsHandler.java
public final class WatchlistsHandler {
    private WatchlistsHandler() {}
    public static JsonElement list(Client client) {
        return EntityJson.envelope(client.getWatchlists(), EntityJson::toJson);
    }
    public static JsonElement get(Client client, String name) {
        return EntityJson.toJson(find(client, name));
    }
    /* package */ static Watchlist find(Client client, String name) {
        return client.getWatchlists().stream().filter(w -> w.getName().equals(name))
               .findFirst().orElseThrow(ApiException::notFound);
    }
}
// ApiRoutes
router.add("GET", "/v1/files/{file}/watchlists", read(resolver, host,
        (client, req) -> Response.json(200, WatchlistsHandler.list(client))));
```

**Write handler** (create; mirrors `SecuritiesHandler.patch` two-phase validate/apply, plus dry-run and idempotency):

```java
// rest/internal/TransactionsHandler.java
public record Created(JsonObject entity, boolean replayed) {}

public static Created create(Client client, JsonObject body, boolean dryRun) {
    var clientRef = Json.optString(body, "clientRef");            // N5
    if (clientRef != null) {
        var existing = IdempotencyIndex.findTransaction(client, clientRef);
        if (existing.isPresent())
            return new Created(EntityJson.toJson(existing.get()), true);
    }
    var plan = TransactionPlanner.plan(client, body);              // phase 1: resolve + validate, throws ApiException.validation(errors)
    if (dryRun)
        return new Created(EntityJson.toJson(plan.preview()), false); // detached objects, nothing inserted
    var pair = plan.apply(clientRef);                              // phase 2: insert(), setSource("api:" + clientRef), client.markDirty()
    return new Created(EntityJson.toJson(pair), false);
}
// ApiRoutes
router.add("POST", "/v1/files/{file}/transactions", write(resolver, host, (file, req) -> {
    var result = TransactionsHandler.create(file.getClient(), parseObject(req), isDryRun(req));
    ChangeLog.recordTransaction(file.getLabel(), result);          // application log, like InstrumentChangeLog
    return Response.json(isDryRun(req) || result.replayed() ? 200 : 201, result.entity());
}));
```

Rules that every new write handler follows: (1) collect all `FieldError`s before touching the model; (2) every `FieldError` code is a same-line string literal and is listed in `openapi.yaml` `FieldError.code` (drift test); (3) `client.markDirty()` after the mutation, never before; (4) no `markDirty()` for a no-op; (5) entity lookup via `Entities.byUuid` → 404; (6) blocked deletes → `ApiException.conflict("delete-blocked", …)`; (7) log the change via a `PortfolioLog.info` summary with a `Messages` key (translation config already covers `rest/messages*.properties`).

`isDryRun(req)`: `"true".equals(req.queryParam("dry_run"))`. The query parameter form is chosen because DELETE has no body and because it keeps the body schemas identical between dry and real runs. A dry run answers `200` with the same entity shape as the real response plus `"dryRun": true`; `Location` is only set on real creates.

**Decimal input parsing** (N6): a new helper `rest/internal/Json.java` with `BigDecimal decimal(JsonObject, field, errors)` that accepts a JSON number or a JSON string and uses `JsonPrimitive#getAsBigDecimal()` (Gson keeps the literal text, so `12.30` does not become a binary float), then `Values.Amount.factorize`-equivalents done with `BigDecimal.movePointRight(precision).setScale(0, HALF_UP)` and an `invalid-value` error when the input has more decimals than the target precision (shares: 8, amounts: 2, quotes: 8, rates: up to 10 with `Values.MC`). Never `Double`.

### 2.3 Save and dirty state

SPI additions (`rest/spi/OpenFile.java`):

```java
/** true if the file has unsaved changes; must be called on the UI thread */
boolean isDirty();
/** persists the file in its current format at its current path; must be called on the UI thread */
void save() throws IOException;
```

UI implementation: `ui/editor/ClientInput.java` gets

```java
/** Saves without any UI: no busy indicator, no error dialog. Used by the REST API. */
public void saveWithoutUI() throws IOException   // throws IllegalStateException if clientFile == null
```

whose body is the extracted l.258-280 of `save(Shell)` (backup if preferred, `ClientFactory.save`, `storePreferences(false)`, `broker.post(File.SAVED)`, pending-dirty reset, `onSaved`), and `save(Shell)` calls it inside its `BusyIndicator`/`try-catch`. `RestApiAddon.ClientInputOpenFile` implements `isDirty()` → `input.isDirty()` and `save()` → `input.saveWithoutUI()`.

Refusals: a file with `clientFile == null` is never listed (RestApiAddon l.94), so a save can never turn into "save as"; `ClientFactory.save` throws `IOException(MsgPasswordMissing)` only if an encrypted client has no secret, which cannot happen after a successful load — the handler still maps any `IOException` to `ApiException(500, "save-failed", "Saving the file failed", e.getMessage(), …)` so the reason reaches the client, and logs it. "Save as", export, format changes and password changes are out of scope; the endpoint has no body.

Endpoints: `GET /v1/files` items gain `"dirty": file.isDirty()` (`EntityJson.toJson(FileAccess, OpenFile)`, `EntityJson.java:161-176`, already inside `onUiThread`); new `GET /v1/files/{file}` (same object); new `POST /v1/files/{file}/save` through `write(...)` so the 423 gate protects against saving during a half-finished cell edit; response `200 {"id","alias","label","path","dirty":false,"savedAt":"<ISO instant>"}`. The `openapi.yaml` "Writes are not saved" paragraph (l.66-75) is rewritten.

### 2.4 dry_run design

Every write handler is split into a pure planner and an applier:

- The planner resolves UUIDs to model objects, parses decimals, computes derived values exactly as the dialogs do (gross ↔ converted ↔ total, forex units, exchange rate lookup via `file.getExchangeRateProviderFactory()` when `exchangeRate` is omitted and currencies differ — `AbstractSecurityTransactionModel.java:329-352` looks the rate up only for new transactions), and returns a `Plan` holding **detached** model objects: `new BuySellEntry(portfolio, account)` is fully populated but `insert()` is not called; a `PortfolioTransaction` is not added to the portfolio; a `Security` is built but not added. The planner never mutates existing objects.
- For updates, the planner copies the existing transaction's fields into the same field set, merges the patch (RFC 7386 semantics as in `SecuritiesHandler.patch`), validates, and yields the "after" preview as a detached object; the applier then writes the validated fields into the existing objects (`setDateTime`, `setShares`, `clearUnits`+`addUnit`, `crossEntry.updateFrom`), or performs delete → `setOwner` → `insert()` when the owner changes (keeps UUIDs).
- The preview is serialized with the same `EntityJson` code as a real entity; `TransactionPair` accepts an owner that does not yet contain the transaction, so `EntityJson.toJson(TransactionPair)` works unchanged. Preview UUIDs are the UUIDs the objects would get (`Transaction()` assigns one in the constructor, `Transaction.java:271`); they are reported so a client can see the shape, but they are not stable across a subsequent real call.
- Actions (split, plan generation, imports) implement dry run as their preview: split returns the counts and the first/last affected transaction and price; plan generation returns the due dates; imports return the extracted items with their check statuses (this is the natural first step of the two-step flow anyway).

### 2.5 Idempotency design (N5)

- Request field `clientRef` (string, ≤ 128 chars) on every POST that creates a transaction. The requirements allow a header alternative; `Request` does not expose headers (`Request.java:8-10`) and `RestApiServer.dispatch` would need to pass them (l.87-89). The body field is chosen to avoid touching the request pipeline; a header can be added later without breaking clients.
- Storage: `Transaction#setSource("api:" + clientRef)` on the created transaction (and via `BuySellEntry/AccountTransferEntry/PortfolioTransferEntry#setSource`, which write both legs — `BuySellEntry.java:124`). `source` is persisted, shown in the UI's "Source" column and otherwise unused by PP logic (duplicate detection compares date/amount/shares/security only, `DetectDuplicatesAction.java:147-165`).
- Lookup: `IdempotencyIndex.findTransaction(client, ref)` scans `client.getAllTransactions()` for `"api:" + ref` (O(n) on the UI thread; a per-client `WeakHashMap<Client, Map<String, String>>` cache keyed by source is added if profiling shows it matters). A hit returns the existing entity with `200` and `"replayed": true`; no second object is created.
- Master-data creates (instrument, account, portfolio, watchlist, plan, taxonomy) have no `source`: they accept `clientRef` and keep it in an in-memory `Map<(fileUuid, clientRef), uuid>` for the lifetime of the server. This survives neither PP restart nor server restart; the adapter additionally de-duplicates by natural key (ISIN/name) before creating. Recorded as open question Q3.

### 2.6 Async action design (F4)

- New SPI method on `HostApplication`: `String startPriceUpdate(OpenFile file, List<Security> securities, Set<PriceUpdateTarget> targets, Consumer<JobOutcome> onDone)`; the UI implements it with `new UpdatePricesJob(client, securities)` / `(client, predicate, targets)`, `suppressAuthenticationDialog(true)`, an `IJobChangeListener` that calls `onDone` with the `IStatus`, then `schedule()`. Returns nothing else; the rest plugin owns the id.
- `rest/internal/JobRegistry`: in-memory map jobId → `JobRecord(kind, fileUuid, state PENDING|RUNNING|DONE|FAILED, startedAt, finishedAt, summary JsonObject)`, entries expire 1 h after completion. `POST /v1/files/{file}/actions/update-quotes` (body `{"instruments":[uuid…] | omitted = all, "targets":["latest","historic"]}`) answers `202 {"jobId": …, "status": "running"}` with `Location: /v1/files/{file}/jobs/{jobId}`; `GET /v1/files/{file}/jobs/{jobId}` reports the state; the optional query `wait=<seconds ≤ 60>` on the POST blocks on the worker thread (never on the UI thread) up to that long and returns the final state when it finishes in time. `UpdatePricesJob` marks the client dirty itself (l.158-162, 182-183), so the summary can include `dirty`. Per-security results are not available from the job (it returns `OK_STATUS` unconditionally); the summary reports the number of securities that gained a newer latest/historic price by comparing `getLatest()/getPrices().size()` snapshots taken on the UI thread before and after.
- Imports (PDF/CSV) are two-step and synchronous per step: extraction runs on the HTTP worker thread (the `calc` pattern: `PDFImportAssistant` reads only `client.getSecurities()` through `SecurityCache`, the same racy-but-read-only trade-off `ApiRoutes.java:128-134` already accepts), the check actions and the commit run on the UI thread through `write`. Extracted items are kept in `rest/internal/ImportSessionRegistry` (importId → items, file uuid, created; TTL 15 min) so the commit applies the exact objects that were previewed.
- Stock split and plan generation are fast and run synchronously inside `write`.

### 2.7 Wire vocabulary

Keep #5870's names: `instrument` (Security), `cash-account`/`cashAccount` (Account), `investment-account`/`investmentAccount` (Portfolio). Transaction types use rest-ext's kebab-case values (`EntityJson.java:1020-1055`) plus `cash-transfer` and `security-transfer` for the two entry kinds in create bodies. Dates: `date` is `YYYY-MM-DD` or `YYYY-MM-DDTHH:MM` (`LocalDateTime`, time defaults to 00:00 as the dialogs do). Money: `{"value": <decimal>, "currency": "EUR"}`. Shares/quotes/rates: decimal literals.

## 3. Endpoint catalogue

Status: **5870** = in `upstream/feature/rest-api`, **fork** = in `manueldeprada/rest-ext`, **new** = to write. All under `/v1/files/{file}` unless noted. Writes accept `?dry_run=true`.

| Method | Path | Purpose | Status | Source classes |
| --- | --- | --- | --- | --- |
| GET | `/v1/openapi.yaml` | spec | 5870 | `OpenApiHandler` |
| POST/GET | `/v1/auth/requests[/{id}]` | pairing | 5870 | `PairingHandler`, `PairingService` |
| GET | `/v1/files` | open+enabled files, now with `dirty` | 5870 (+new field) | `FilesHandler`, `EntityJson.toJson(FileAccess, OpenFile)` |
| GET | `/{file}` | one file incl. `dirty` | new | `FilesHandler.get` |
| POST | `/{file}/save` | persist | new | `FilesHandler.save`, `OpenFile#save`, `ClientInput#saveWithoutUI` |
| GET | `/instruments`, `/instruments/{uuid}`, `/instruments/attribute-types` | read | 5870 | `SecuritiesHandler` |
| POST | `/instruments` | create security | new | `SecuritiesHandler.create` (`new Security(name, currency)`, `client.addSecurity`) |
| PATCH | `/instruments/{uuid}` | merge patch; add `feed`, `feedUrl`, `latestFeed`, `latestFeedUrl`, `feedProperties{}`, `retired`, `targetCurrencyCode`, `calendar` | 5870 (+new fields) | `SecuritiesHandler.WRITABLE_FIELDS` |
| DELETE | `/instruments/{uuid}` | delete unreferenced | 5870 | `SecuritiesHandler.delete` |
| GET | `/instruments/{uuid}/prices?from&to` | prices | fork | `SecurityPricesHandler` |
| PUT | `/instruments/{uuid}/prices` | upsert `{items:[{date,value}]}` | new | `SecurityPricesHandler.upsert` → `Security#addPrice(p, true)` |
| DELETE | `/instruments/{uuid}/prices?from&to` | delete range (both absent = all) | new | `Security#removePrice/removeAllPrices` |
| GET/POST | `/instruments/{uuid}/events` | list / add `{type: stock-split|note, date, details}` | new | `Security#getEvents/addEvent` |
| DELETE | `/instruments/{uuid}/events?date&type[&details]` | remove matching | new | `Security#removeEventIf` |
| POST | `/instruments/{uuid}/actions/split` | stock split `{exDate,newShares,oldShares,adjustTransactions,adjustPrices}` | new | `StockSplitAction` (port of `StockSplitModel.applyChanges`) |
| GET | `/cash-accounts[/{uuid}]?date` | with balance | fork | `AccountsHandler` |
| POST/PATCH/DELETE | `/cash-accounts[/{uuid}]` | `{name,currencyCode,note,retired,attributes}`; delete only when empty | new | `AccountsHandler.create/patch/delete` |
| GET | `/investment-accounts[/{uuid}]?date&currency` | with value | fork | `PortfoliosHandler` |
| POST/PATCH/DELETE | `/investment-accounts[/{uuid}]` | `{name,referenceCashAccount,note,retired,attributes}` | new | `PortfoliosHandler.create/patch/delete` |
| GET | `/transactions` (+ filters `from,to,type,instrument,cashAccount,investmentAccount`) | list | fork (+new filters) | `TransactionsHandler.list` |
| GET | `/transactions/{uuid}` | one; either leg's uuid resolves; response adds `units[]`, `exDate`, `linked{uuid,owner}` | new | `TransactionsHandler.get`, `EntityJson.toJsonDetailed` |
| POST | `/transactions` | create any type (`type` discriminator) | new | `TransactionsHandler.create`, `TransactionPlanner` |
| PATCH | `/transactions/{uuid}` | merge patch incl. owner change | new | `TransactionsHandler.patch` |
| DELETE | `/transactions/{uuid}` | delete incl. linked leg | new | `TransactionOwner#deleteTransaction` |
| GET/POST | `/watchlists` | list / create `{name, instruments[]}` | new | `WatchlistsHandler` |
| PATCH/DELETE | `/watchlists/{name}` | rename, replace `instruments` | new | `Watchlist#setName/getSecurities`, `Client#removeWatchlist` |
| PUT/DELETE | `/watchlists/{name}/instruments/{uuid}` | add / remove one | new | `Watchlist#addSecurity`, `getSecurities().remove` |
| GET/POST | `/investment-plans` | list / create | new | `InvestmentPlansHandler` |
| PATCH/DELETE | `/investment-plans/{name}` | update / delete | new | `InvestmentPlan` setters, `Client#removePlan` |
| POST | `/investment-plans/{name}/actions/generate` | generate due transactions | new | `InvestmentPlan#generateTransactions` |
| GET | `/taxonomies` | list with trees | fork (+`id`,`weight`,`assignments` per node) | `TaxonomiesHandler`, `EntityJson.toJson(Taxonomy)` |
| POST/PATCH/DELETE | `/taxonomies[/{id}]` | create/rename/delete | new | `Taxonomy`, `Classification` root |
| POST/PATCH/DELETE | `/taxonomies/{id}/classifications[/{cid}]` | `{parent,name,color,weight,note}` | new | `Classification(parent,id,name,color)` + `parent.addChild` |
| PUT/DELETE | `/taxonomies/{id}/classifications/{cid}/assignments/{vehicleUuid}` | `{weight}` (percent, 2 dp) | new | `Classification.Assignment` |
| GET | `/taxonomies/{id}/allocation?date&currency` | value per classification | new (F3 gap) | `ClientClassificationFilter` + `ClientSnapshot` |
| GET | `/holdings?date&openingDate&currency&costMethod` (+`investmentAccount`,`cashAccount`) | statement of assets | fork (+filter params) | `HoldingsHandler`, `PortfolioClientFilter` |
| GET | `/performance`, `/performance/series`, `/performance/securities`, `/performance/calendar` (+filter params) | performance | fork (+filter params) | `PerformanceHandler`, `PerformanceCalendarHandler`, `SecurityPerformanceHandler` |
| GET | `/trades?currency&onlyClosed` | trades | fork | `TradesHandler` |
| GET | `/earnings?from&to&instrument` | dividends/interest list | new (F3 gap; thin wrapper over the transactions filter with `type in (dividends, interest, interest-charge)`) | `TransactionsHandler.list` |
| POST | `/actions/update-quotes` | quote update job | new | `ActionsHandler`, `HostApplication#startPriceUpdate`, `JobRegistry` |
| GET | `/jobs/{jobId}` | job status | new | `JobRegistry` |
| POST | `/imports/pdf` | extract `{paths[]}` → `{importId, items[]}` | new | `PdfImportHandler`, `PDFImportAssistant`, check actions |
| POST | `/imports/csv` | extract `{path, config}` → same | new | `CsvImportHandler`, `CSVImporter`, `CSVConfig` |
| POST | `/imports/{importId}/commit` | `{select[], targets{}, options{}}` | new | `ImportCommitHandler`, `InsertAction` |

Transaction create body (union by `type`):

```json
{ "type": "buy", "date": "2026-03-02T09:30", "clientRef": "broker-2026-000123",
  "investmentAccount": "<uuid>", "cashAccount": "<uuid>", "instrument": "<uuid>",
  "shares": "12.5", "quote": "101.2",  "grossValue": "1265.00",         // one of quote|grossValue in the instrument currency
  "exchangeRate": "1.0850",                                             // required only when instrument ccy != cash-account ccy and no rate is available
  "fees": "4.90", "taxes": "0", "forexFees": "0", "forexTaxes": "0",    // fees/taxes in the transaction currency; forex* in the instrument currency
  "amount": "1377.60",                                                   // optional total; if given, validated against the computed total (tolerance as in the dialog)
  "note": "…" }
{ "type": "dividends", "cashAccount": "…", "instrument": "…", "date": "…", "exDate": "…", "shares": "…", "grossValue": "…", "exchangeRate": "…", "taxes": "…", "fees": "…", "note": "…" }
{ "type": "deposit"|"removal"|"interest"|"interest-charge"|"fees"|"fees-refund"|"taxes"|"tax-refund", "cashAccount": "…", "date": "…", "amount": "…", "instrument": "…"(optional for fees/taxes kinds), "taxes": "…"(interest only), "note": "…" }
{ "type": "delivery-inbound"|"delivery-outbound", "investmentAccount": "…", "instrument": "…", "date": "…", "shares": "…", "quote"|"grossValue": "…", "currency": "EUR"(optional, default reference account ccy), "exchangeRate": "…", "fees": "…", "taxes": "…" }
{ "type": "cash-transfer", "fromCashAccount": "…", "toCashAccount": "…", "date": "…", "amount": "…", "targetAmount": "…"(required when currencies differ), "note": "…" }
{ "type": "security-transfer", "fromInvestmentAccount": "…", "toInvestmentAccount": "…", "instrument": "…", "date": "…", "shares": "…", "amount": "…"(book value), "note": "…" }
```

Create response (`201`, also the `GET /transactions/{uuid}` shape): rest-ext's transaction object plus `"units": [{"type":"gross-value|fee|tax","amount":{…},"forex":{…},"exchangeRate":"…"}]`, `"exDate"`, `"source"`, `"linked": {"uuid": "<other leg>", "owner": {…}}` for buy/sell and transfers, `"clientRef"`, and `"dryRun"`/`"replayed"` flags when applicable. The UUIDs of both legs are therefore always returned (N4).

## 4. MCP tool catalogue

FastMCP 3.x (`fastmcp>=3.2,<4`, Context7 docs for `/prefecthq/fastmcp` v3.2.x: `@mcp.tool`, typed parameters, `fastmcp.exceptions.ToolError`, `mcp.run()` stdio default, in-memory `Client(server)` for tests). Tool names are task-oriented (N8). Every parameter that carries a number is a `str` decimal (N6); every tool that writes has `dry_run: bool = False` and creates accept `client_ref: str | None`. `file` is the PP file id or alias; when the API lists exactly one file it may be omitted.

| Tool | Parameters | Maps to |
| --- | --- | --- |
| `list_files` | – | `GET /v1/files` |
| `save_file` | `file` | `POST /{file}/save` |
| `list_instruments` / `get_instrument` | `file`, `[uuid]`, `include_prices: bool`, `from`, `to` | `GET /instruments[/{uuid}]`, `/prices` |
| `find_instrument` | `file`, `query` (name/ISIN/WKN/ticker substring) | `GET /instruments` filtered client-side |
| `create_instrument` | `file`, `name`, `currency`, `isin`, `wkn`, `ticker`, `note`, `feed`, `feed_url`, `latest_feed`, `attributes`, `client_ref`, `dry_run` | `POST /instruments` |
| `update_instrument` | `file`, `uuid`, any of the fields above, `retired` | `PATCH /instruments/{uuid}` |
| `delete_instrument` | `file`, `uuid`, `dry_run` | `DELETE /instruments/{uuid}` |
| `set_prices` / `delete_prices` | `file`, `uuid`, `prices: list[{date,value}]` / `from`, `to` | `PUT/DELETE /instruments/{uuid}/prices` |
| `add_instrument_event` / `delete_instrument_event` | `file`, `uuid`, `type`, `date`, `details` | `POST/DELETE /instruments/{uuid}/events` |
| `apply_stock_split` | `file`, `uuid`, `ex_date`, `new_shares`, `old_shares`, `adjust_transactions=True`, `adjust_prices=True`, `dry_run` | `POST /instruments/{uuid}/actions/split` |
| `list_accounts` | `file`, `date` | `GET /cash-accounts`, `GET /investment-accounts` (merged) |
| `create_cash_account` / `update_cash_account` / `delete_cash_account` | `file`, `[uuid]`, `name`, `currency`, `note`, `retired`, `dry_run` | `/cash-accounts` |
| `create_investment_account` / `update_investment_account` / `delete_investment_account` | `file`, `[uuid]`, `name`, `reference_cash_account`, `note`, `retired`, `dry_run` | `/investment-accounts` |
| `list_transactions` | `file`, `from`, `to`, `type`, `instrument`, `cash_account`, `investment_account`, `limit` | `GET /transactions?…` |
| `get_transaction` | `file`, `uuid` | `GET /transactions/{uuid}` |
| `create_buy` / `create_sell` | `file`, `investment_account`, `cash_account`, `instrument`, `date`, `shares`, `quote` or `gross_value`, `fees`, `taxes`, `forex_fees`, `forex_taxes`, `exchange_rate`, `total`, `note`, `client_ref`, `dry_run` | `POST /transactions` type buy/sell |
| `create_delivery` | `file`, `investment_account`, `instrument`, `direction: inbound|outbound`, `date`, `shares`, `quote`/`gross_value`, `currency`, `fees`, `taxes`, `exchange_rate`, `note`, … | `POST /transactions` type delivery-* |
| `create_dividend` | `file`, `cash_account`, `instrument`, `date`, `ex_date`, `shares`, `gross_value`, `exchange_rate`, `taxes`, `fees`, `note`, … | `POST /transactions` type dividends |
| `create_cash_transaction` | `file`, `cash_account`, `kind: deposit|removal|interest|interest_charge|fees|fees_refund|taxes|tax_refund`, `date`, `amount`, `instrument`, `taxes`, `note`, … | `POST /transactions` |
| `create_transfer` | `file`, `from_cash_account`, `to_cash_account`, `date`, `amount`, `target_amount`, `note`, … | `POST /transactions` type cash-transfer |
| `create_security_transfer` | `file`, `from_investment_account`, `to_investment_account`, `instrument`, `date`, `shares`, `amount`, `note`, … | `POST /transactions` type security-transfer |
| `update_transaction` | `file`, `uuid`, any writable field, `dry_run` | `PATCH /transactions/{uuid}` |
| `delete_transaction` | `file`, `uuid`, `dry_run` | `DELETE /transactions/{uuid}` |
| `list_watchlists` / `create_watchlist` / `rename_watchlist` / `delete_watchlist` / `add_to_watchlist` / `remove_from_watchlist` | `file`, `name`, `[new_name]`, `[instrument]` | `/watchlists…` |
| `list_investment_plans` / `create_investment_plan` / `update_investment_plan` / `delete_investment_plan` | `file`, `name`, `kind`, `instrument`, `investment_account`, `cash_account`, `start`, `interval_months`/`interval_weeks`, `amount`, `fees`, `taxes`, `auto_generate`, `note` | `/investment-plans…` |
| `generate_plan_transactions` | `file`, `name`, `dry_run` | `POST /investment-plans/{name}/actions/generate` |
| `list_taxonomies` / `get_taxonomy_allocation` | `file`, `[taxonomy_id]`, `date`, `currency` | `GET /taxonomies`, `/taxonomies/{id}/allocation` |
| `create_taxonomy` / `rename_taxonomy` / `delete_taxonomy` | `file`, `name`/`id` | `/taxonomies` |
| `create_classification` / `update_classification` / `delete_classification` | `file`, `taxonomy_id`, `[classification_id]`, `parent_id`, `name`, `color`, `weight`, `note` | `/taxonomies/{id}/classifications` |
| `assign_classification` / `unassign_classification` | `file`, `taxonomy_id`, `classification_id`, `vehicle_uuid`, `weight` | `PUT/DELETE …/assignments/{vehicleUuid}` |
| `get_holdings` | `file`, `date`, `opening_date`, `currency`, `cost_method`, `investment_account`, `cash_account` | `GET /holdings` |
| `get_performance` | `file`, `from`, `to`, `currency`, `cost_method`, `include_series`, `include_calendar`, `investment_account` | `GET /performance[/series|/calendar]` |
| `get_security_performance` | `file`, `from`, `to`, `currency`, `cost_method` | `GET /performance/securities` |
| `get_trades` | `file`, `currency`, `only_closed` | `GET /trades` |
| `get_earnings` | `file`, `from`, `to`, `instrument` | `GET /earnings` |
| `update_quotes` | `file`, `instruments: list[str] | None`, `targets: list[latest|historic]`, `wait_seconds: int = 30` | `POST /actions/update-quotes?wait`, then `GET /jobs/{id}` |
| `import_pdf` | `file`, `paths: list[str]`, `cash_account`, `investment_account`, `accounts_by_currency`, `secondary_*`, `options`, `dry_run=True` | `POST /imports/pdf`; when `dry_run=False` also `POST /imports/{id}/commit` with all OK/WARNING items |
| `commit_import` | `file`, `import_id`, `select: list[int]`, targets, options | `POST /imports/{id}/commit` |
| `import_csv` | `file`, `path`, `config: dict` (CSVConfig JSON), targets, `dry_run=True` | `POST /imports/csv` (+ commit) |

Error mapping (N7): the HTTP client raises `PPApiError(status, type, title, detail, errors)`; the tool layer converts it to `ToolError(f"{type}: {title}" + (f" — {detail}") + one line per `errors[]` entry `field (code): message`)`. 423 is retried up to 3 times with the `Retry-After` value (max 5 s each) before it becomes a `ToolError("user-interaction: …")`. 401 includes the pairing hint.

Package layout:

```
tools/pp-mcp/
  pyproject.toml            # [project] name="pp-mcp", requires-python>=3.12, dependencies: fastmcp>=3.2,<4, httpx>=0.27; [project.scripts] pp-mcp = "pp_mcp.server:main"; [tool.uv] dev-dependencies: pytest, pytest-asyncio, respx
  README.md                 # setup: `uv run pp-mcp`, env PP_API_URL (default http://127.0.0.1:5712), PP_API_TOKEN
  src/pp_mcp/__init__.py
  src/pp_mcp/config.py      # Settings from env (N1); no other config source
  src/pp_mcp/client.py      # PPClient(httpx.Client): get/post/patch/put/delete, bearer header, json.loads(..., parse_float=Decimal), problem+json → PPApiError, 423 retry
  src/pp_mcp/errors.py      # PPApiError, to_tool_error()
  src/pp_mcp/money.py       # Decimal helpers: parse "12.34"/"12,34"? -> Decimal only; validate scale (2 for money, 8 for shares); serialize as str
  src/pp_mcp/server.py      # mcp = FastMCP("portfolio-performance"); registers tool modules; main() -> mcp.run()
  src/pp_mcp/tools/files.py, instruments.py, accounts.py, transactions.py, watchlists.py, plans.py, taxonomies.py, reports.py, actions.py, imports.py
  tests/conftest.py         # respx-mocked PPClient fixture, FastMCP in-memory Client fixture
  tests/test_*.py           # one per tool module
  tests/e2e/                # T3 scripts (see step 22): make_fixture.py, seed_workspace.py, run_e2e.py
```

## 5. Ordered implementation steps

Each step compiles and its tests pass on its own. Commit messages follow #5870's style (`REST API: …`). `REST_TEST` below abbreviates the rest test command from the README:

```
mvn -f portfolio-app/pom.xml verify -Plocal-dev -o -pl :portfolio-target-definition,:name.abuchen.portfolio.pdfbox1,:name.abuchen.portfolio.pdfbox3,:name.abuchen.portfolio,:name.abuchen.portfolio.junit,:name.abuchen.portfolio.rest,:name.abuchen.portfolio.rest.tests -am -amd
```

(drop `-o` the first time on a machine without a populated `~/.m2`; the target platform must be downloaded once). `UI_COMPILE` abbreviates:

```
mvn -f portfolio-app/pom.xml compile -Plocal-dev -o -pl :portfolio-target-definition,:name.abuchen.portfolio.pdfbox1,:name.abuchen.portfolio.pdfbox3,:name.abuchen.portfolio,:name.abuchen.portfolio.rest,:name.abuchen.portfolio.bootstrap,:name.abuchen.portfolio.ui -am -amd
```

### Step 1 — Integrate #5870 and rest-ext

- Goal: `feature/mcp-rw` = master + rebased #5870 + rebased rest-ext, building.
- Commands: `git rebase master` on a branch at `upstream/feature/rest-api`, resolve the two hunks as in 1.1; `git rebase --onto <that> upstream/feature/rest-api` on a branch at `manueldeprada/rest-ext`; then `git reset --hard <result>` on `feature/mcp-rw` (or reuse `scratch/rest-ext`).
- Files: the version bump in `name.abuchen.portfolio.rest/pom.xml`, `name.abuchen.portfolio.rest.tests/pom.xml`, both `META-INF/MANIFEST.MF`.
- Tests: none new. Verify: `REST_TEST` (256 tests) and `UI_COMPILE`.
- Commit: `REST API: bump plugin versions to 0.87.1 after rebase onto master`.

### Step 2 — Build integration and docs

- Goal: builders and agents know about the rest modules.
- Modify `CLAUDE.md` and `AGENTS.md`: add `:name.abuchen.portfolio.rest` to the "core and UI" compile command (the UI bundle requires it), add a "Run REST tests" command (= `REST_TEST`) and a "single REST test" variant with `-Dtest=`. Note in CLAUDE.md that `openapi.yaml` and `FieldError` codes are guarded by `OpenApiSpecDriftTest`.
- No product/feature/pom change is needed: `feature.xml` already lists the plugin and the product includes the feature (`name.abuchen.portfolio.product:41`). Check `portfolio-app/eclipse/launches.lc` still parses.
- Verify: `REST_TEST`. Commit: `docs: build commands for the REST API plugin`.

### Step 3 — Dirty state and save

- Goal: A5/F4 save; dirty flag per file.
- Modify `rest/spi/OpenFile.java` (+`isDirty`, `save`), `rest.tests/testsupport/FakeHost.java` (`FakeOpenFile` gains a mutable `dirty` flag and a `saved` counter; `save()` clears dirty), `ui/editor/ClientInput.java` (`saveWithoutUI()` extracted from `save(Shell)`), `ui/addons/RestApiAddon.java` (`ClientInputOpenFile.isDirty/save`), `rest/internal/FilesHandler.java` (`get`, `save`), `rest/internal/EntityJson.java` (`dirty` field), `rest/ApiRoutes.java` (routes `GET /v1/files/{file}`, `POST /v1/files/{file}/save`), `rest/internal/ApiException.java` (`saveFailed(detail)` → 500 `save-failed`), `openapi.yaml` (two operations, `dirty` property, rewrite "Writes are not saved"), `rest/README.md`.
- Tests: `rest.tests/internal/FilesHandlerTest.java` (dirty flag in list and get; save clears dirty; save through router is 423 while editing; save runs inside `syncExec` — assert `!host.hasAccessedOutsideUIThread()`; IOException → 500 `save-failed`). `OpenApiSpecDriftTest` passes.
- Verify: `REST_TEST`, `UI_COMPILE`. Commit: `REST API: report dirty state and add POST /v1/files/{file}/save`.

### Step 4 — Shared write infrastructure

- Goal: helpers every write handler needs.
- Create `rest/internal/Json.java` (typed field access: `optString`, `requireString`, `decimal(field, precision)`, `date`, `dateTime`, `bool`, all collecting `FieldError`s), `rest/internal/Amounts.java` (`long toAmount(BigDecimal)`, `long toShares(BigDecimal)`, `long toQuote(BigDecimal)`, inverse `BigDecimal` getters, tolerance checks copied from `AbstractSecurityTransactionModel.calculateStatus`), `rest/internal/WriteContext.java` (`OpenFile file`, `boolean dryRun`, `String clientRef`, factory `WriteContext.of(OpenFile, Request)`), `rest/internal/IdempotencyIndex.java` (2.5), `rest/internal/ChangeLog.java` (generalizes `InstrumentChangeLog` to any entity; keep the old class as a thin delegate so its tests stay green).
- Extend `rest/ApiRoutes.java` with `write(resolver, host, (ctx, req) -> …)` overload taking a `WriteContext`, and `isDryRun(Request)`.
- Tests: `rest.tests/internal/JsonTest.java` (number vs string decimals, scale overflow → `invalid-value`, `1e2` rejected), `AmountsTest.java`, `IdempotencyIndexTest.java`.
- Verify: `REST_TEST`. Commit: `REST API: shared decimal parsing, write context and idempotency index`.

### Step 5 — F1: transaction read enhancements

- Goal: filters on `GET /transactions`, `GET /transactions/{uuid}`, detailed serialization.
- Modify `rest/internal/TransactionsHandler.java` (`list(client, from, to, type, instrument, cashAccount, investmentAccount)`, `get(client, uuid)` that searches all owners including the hidden legs and returns the canonical pair — portfolio leg for buy/sell, outbound leg for transfers — with `linked`), `rest/internal/EntityJson.java` (`toJsonDetailed(TransactionPair)` adding `units`, `exDate`, `source`, `linked`, `updatedAt`), `ApiRoutes`, `openapi.yaml`.
- Tests: `rest.tests/internal/TransactionsReadTest.java` (filters, each-leg lookup, units serialization incl. FX gross value, 404).
- Verify: `REST_TEST`. Commit: `REST API: filter transactions and add GET /v1/files/{file}/transactions/{uuid}`.

### Step 6 — F1: transaction planner (create, dry run)

- Goal: `POST /transactions` for all types.
- Create `rest/internal/TransactionPlanner.java` (one nested planner per kind: `BuySellPlan`, `DeliveryPlan`, `AccountTxPlan`, `CashTransferPlan`, `SecurityTransferPlan`; each `plan(client, factory, body) → Plan`, `Plan.preview() → TransactionPair<?>`, `Plan.apply(clientRef) → TransactionPair<?>`), `rest/internal/TransactionTypes.java` (wire type ↔ enum, moved out of `EntityJson.wireType`). Validation codes (add to the spec enum): `required`, `invalid-type`, `invalid-value`, `unknown-currency`, `currency-mismatch`, `exchange-rate-required`, `must-be-positive`, `not-allowed-for-type`, `same-account`, `same-investment-account`, `total-mismatch`, `gross-mismatch`, `ex-date-after-date`, `zero-total-use-delivery`, `too-long`.
- Modify `TransactionsHandler.create`, `ApiRoutes` (`POST /v1/files/{file}/transactions` via `write`), `openapi.yaml` (request union, response, codes), `rest/messages*.properties` (`MsgApiTransactionCreated`, `…Updated`, `…Deleted` — add the English key to every locale file, as #5870 does).
- Tests: `rest.tests/internal/CreateBuySellTest.java` (same-currency buy: units on portfolio leg only, account leg 0 shares, total = gross+fees+taxes; FX buy: GROSS_VALUE unit with forex/rate, forex fee unit; explicit `amount` within/outside tolerance; sell with zero total → `zero-total-use-delivery`; missing account → `required`; `hasAccessedOutsideUIThread` false via router), `CreateAccountTransactionTest.java` (dividends need instrument, 0 shares allowed, exDate rules, FX dividend, interest with taxes, fees with optional instrument, deposit rejects instrument with `not-allowed-for-type`), `CreateTransferTest.java` (same/different currency cash transfer, `targetAmount` required, GROSS_VALUE on source leg with inverse rate, same-account rejected; security transfer), `CreateDeliveryTest.java`, `DryRunTest.java` (dry run leaves `client.getAllTransactions()` and dirty flag untouched, returns full preview with both leg uuids), `IdempotencyTest.java` (second create with same `clientRef` returns first entity, 200, `replayed:true`, no duplicate; `source` equals `api:<ref>` on both legs).
- Verify: `REST_TEST`. Commit: `REST API: create transactions of every type, with dry run and idempotency`.

### Step 7 — F1: update and delete

- Goal: `PATCH`/`DELETE /transactions/{uuid}`.
- Modify `TransactionPlanner` (`planUpdate(existingPair, patch)` merges and validates; `applyUpdate` writes fields, `clearUnits`+re-add, `crossEntry.updateFrom`, owner change via delete → `setOwner` → `insert()`; type changes only within the same family — buy↔sell, deposit↔removal etc. — else `not-allowed-for-type`), `TransactionsHandler.patch/delete` (delete uses `pair.getOwner().deleteTransaction(tx, client)`; dry run reports the legs that would be removed), `ApiRoutes`, `openapi.yaml`.
- Tests: `rest.tests/internal/UpdateTransactionTest.java` (date/note propagate to both legs; shares only on portfolio leg; owner change keeps both UUIDs and moves both legs; empty patch does not dirty; unknown field 422), `DeleteTransactionTest.java` (both legs gone, plan link removed, 404 after).
- Verify: `REST_TEST`. Commit: `REST API: update and delete transactions incl. linked legs`.

### Step 8 — F2: instruments

- Goal: create, extended patch (incl. `retired`), prices and events writes.
- Modify `rest/internal/SecuritiesHandler.java` (`create`; `WRITABLE_FIELDS` += `feed`, `feedUrl`, `latestFeed`, `latestFeedUrl`, `retired`, `targetCurrencyCode`, `calendar`; nested `feedProperties` object → `Security#setPropertyValue(FEED, name, value)`; validation: feed id must be known to `Factory.getQuoteFeedProvider` or `QuoteFeed.MANUAL`, `targetCurrencyCode` only for exchange-rate instruments), `rest/internal/SecurityPricesHandler.java` (`upsert`, `delete`), create `rest/internal/SecurityEventsHandler.java`, `EntityJson` (`feed*`, `retired`, `events` in the instrument JSON; `toJson(SecurityEvent)`), `ApiRoutes`, `openapi.yaml`, `rest.tests/internal/ReadEndpointsTest.java:36-62` (`testRetiredFlagIsNotExposed` is inverted: `retired` is now exposed and writable — see Q1).
- Tests: `CreateSecurityTest.java`, `PatchSecurityFeedTest.java`, `SecurityPricesWriteTest.java` (upsert overwrites same-date, sorted, dry run), `SecurityEventsTest.java`.
- Verify: `REST_TEST`. Commit: `REST API: create instruments, edit feeds, retirement, prices and events`.

### Step 9 — F2: cash and investment accounts

- Modify `rest/internal/AccountsHandler.java` and `PortfoliosHandler.java` (`create`, `patch`, `delete` — delete only when `getTransactions().isEmpty()` and no plan references it, else 409 `delete-blocked`; `referenceCashAccount` must exist and not be retired; `currencyCode` immutable once the account has transactions — mirrors `AccountListView`), `EntityJson` (`retired`, `attributes` via `AttributeCodec` for `Account`/`Portfolio`), `ApiRoutes`, `openapi.yaml`.
- Tests: `AccountsWriteTest.java`, `PortfoliosWriteTest.java`.
- Verify: `REST_TEST`. Commit: `REST API: create, update and delete cash and investment accounts`.

### Step 10 — F2: watchlists and investment plans

- Create `rest/internal/WatchlistsHandler.java` (addressed by name; create refuses a duplicate name with `already-exists`), `rest/internal/InvestmentPlansHandler.java` (addressed by name; fields per `InvestmentPlan` setters; `interval` as `intervalMonths` xor `intervalWeeks` mapped to the 1–99 / 101+ encoding, `model/InvestmentPlan.java:60`; `kind` = `purchase|deposit|removal|interest`), `EntityJson` (`toJson(Watchlist)`, `toJson(InvestmentPlan)`), `ApiRoutes`, `openapi.yaml`. `Client#addPlan/removePlan` fire no event → `markDirty()`.
- Tests: `WatchlistsTest.java`, `InvestmentPlansTest.java`.
- Verify: `REST_TEST`. Commit: `REST API: watchlists and investment plans CRUD`.

### Step 11 — F2: taxonomies

- Modify `rest/internal/TaxonomiesHandler.java` (+`create`, `rename`, `delete`, classification CRUD, assignment put/delete), `EntityJson.toJson(Taxonomy/Classification)` (+`id`, `weight`, `assignments[{vehicle uuid, type, weight}]`), `ApiRoutes`, `openapi.yaml`. Rules: new taxonomy gets a root `Classification(UUID, name)` (`ui/handlers/NewDomainElementHandler.java:255-256`); child created with the parent constructor **and** `parent.addChild` (`model/Classification.java:157-175, 252`); weight in percent with 2 decimals → `Values.Weight.factorize`; the sum of a vehicle's assignment weights across one taxonomy must not exceed 100 % (`ui/views/taxonomy/TaxonomyModel.java:215-222`), else `weight-exceeds-100`; deleting a classification with children or assignments requires `?cascade=true`.
- Tests: `TaxonomiesWriteTest.java`.
- Verify: `REST_TEST`. Commit: `REST API: taxonomy and classification CRUD with assignments`.

### Step 12 — F3 gaps

- Modify `HoldingsHandler`, `PerformanceHandler`, `PerformanceCalendarHandler`, `SecurityPerformanceHandler` to accept `investmentAccount`/`cashAccount` filters by wrapping the client in `name.abuchen.portfolio.snapshot.filter.PortfolioClientFilter` (same filter the UI's reporting uses); create `rest/internal/TaxonomyAllocationHandler.java` (`GET /taxonomies/{id}/allocation` using `ClientClassificationFilter` per classification → `ClientSnapshot.create(...).getMonetaryAssets()`); add `GET /earnings` as a typed transactions filter with a `total` per currency. `openapi.yaml`.
- Tests: `HoldingsFilterTest.java`, `TaxonomyAllocationTest.java`, `EarningsTest.java`.
- Verify: `REST_TEST`. Commit: `REST API: filter reports by account, taxonomy allocation and earnings`.

### Step 13 — F4: stock split

- Create `rest/internal/StockSplitAction.java` (port of `StockSplitModel.applyChanges` l.109-137 with HALF_EVEN math; dry run returns `{transactionsAffected, pricesAffected, event}`; validation as `SelectSplitPage.java:132-178`), route `POST /instruments/{uuid}/actions/split`, `openapi.yaml`.
- Tests: `StockSplitActionTest.java` (shares and prices before exDate adjusted, after untouched, event added, dividends' shares adjusted, dry run no-op).
- Verify: `REST_TEST`. Commit: `REST API: stock split action`.

### Step 14 — F4: investment plan generation

- Add to `model/InvestmentPlan.java` a pure `public List<LocalDate> getDatesOfTransactionsToBeGenerated()` (extracts the loop of l.413-431 without inserting) so the dry run can list due dates; `generateTransactions` uses it. Create `rest/internal/InvestmentPlansHandler.generate` (`CurrencyConverterImpl(file.getExchangeRateProviderFactory(), client.getBaseCurrency())`; `IOException` → 409 `missing-price` with the date in `detail`; returns the created pairs; `markDirty`). Route, spec.
- Tests: `name.abuchen.portfolio.tests/src/name/abuchen/portfolio/model/InvestmentPlanTest.java` (+ due-dates test, existing test class), `rest.tests/internal/GeneratePlanTest.java`.
- Verify: `REST_TEST` plus the core test command for `InvestmentPlanTest`. Commit: `REST API: generate due investment plan transactions`.

### Step 15 — F4: quote update job

- Modify `rest/spi/HostApplication.java` (`startPriceUpdate(...)`, plus `enum PriceUpdateTarget {LATEST, HISTORIC}` in `rest/spi/`), `FakeHost` (records the request; test can complete it synchronously), `ui/addons/RestApiAddon.java` (implementation with `UpdatePricesJob`, `suppressAuthenticationDialog(true)`, `IJobChangeListener`), create `rest/internal/JobRegistry.java`, `rest/internal/ActionsHandler.java` (`updateQuotes`, `wait` handling on the worker thread), `rest/internal/JobsHandler.java`, routes, spec.
- Tests: `JobRegistryTest.java`, `UpdateQuotesActionTest.java` (202 + job id; completion through FakeHost; `wait` returns done; unknown instrument 422; snapshot-based summary).
- Verify: `REST_TEST`, `UI_COMPILE`. Commit: `REST API: trigger online quote updates as a job`.

### Step 16 — F4: PDF import (preview + commit)

- Add `Import-Package: org.json.simple` to `name.abuchen.portfolio.rest/META-INF/MANIFEST.MF` (needed by step 17 too; the core bundle already exports/uses it — check its `MANIFEST.MF`). Create `rest/internal/ImportSessionRegistry.java`, `rest/internal/ImportContext.java` (implements `ImportAction.Context` from a `targets` JSON: `cashAccount`, `cashAccountsByCurrency{}`, `investmentAccount`, `secondaryCashAccount`, `secondaryInvestmentAccount`), `rest/internal/ImportItemJson.java` (item → JSON: index, kind, status code/message per check, the transaction preview via `EntityJson.toJsonDetailed` for entry items, security for `SecurityItem`), `rest/internal/PdfImportHandler.java` (paths must exist and end in `.pdf`; extraction on worker thread; checks on UI thread), `rest/internal/ImportCommitHandler.java` (`InsertAction` with `options.convertBuySellToDelivery|removeDividends|importNotes`; skips items whose check status is ERROR unless `select` names them explicitly, then 422 `item-not-importable`; `markDirty`; removes the session), routes `POST /imports/pdf`, `POST /imports/{importId}/commit`, spec.
- Tests: `PdfImportHandlerTest.java` using a small sanitized PDF already present under `name.abuchen.portfolio.tests/src/.../datatransfer/pdf/**` (copy one fixture into `rest.tests` resources; the PDF extractors are deterministic), `ImportCommitTest.java` (items land in the chosen accounts, duplicates flagged, security created, session consumed).
- Verify: `REST_TEST`. Commit: `REST API: PDF import with preview and commit`.

### Step 17 — F4: CSV import

- Create `rest/internal/CsvImportHandler.java` (`CSVConfig.fromJSON(new JSONParser().parse(config.toString()))`, `writeTo(importer)`, `processFile(false)`, `createItems(errors)`; parse errors → 422 `csv-parse-error` with line numbers in `errors[]`; price CSV requires `targets.instrument` and applies `addPrice`), reuse the session/commit path. Route, spec.
- Tests: `CsvImportHandlerTest.java` with an inline CSV written to a temp file.
- Verify: `REST_TEST`. Commit: `REST API: CSV import with explicit column mapping`.

### Step 18 — Change log and application messages

- Goal: every write kind logs a human-readable entry like `InstrumentChangeLog`.
- Modify `rest/internal/ChangeLog.java`, `rest/Messages.java`, all `rest/messages*.properties` (keys sorted, as CI's translation check expects), tests `ChangeLogMessagesWellFormedTest` extended.
- Verify: `REST_TEST`. Commit: `REST API: log API writes in the application log`.

### Step 19 — Python adapter skeleton

- Create `tools/pp-mcp/pyproject.toml`, `README.md`, `src/pp_mcp/{__init__,config,client,errors,money,server}.py`, `tests/conftest.py`, `tests/test_client.py`, `tests/test_money.py`. `uv init --package`, `uv add "fastmcp>=3.2,<4" httpx`, `uv add --dev pytest pytest-asyncio respx`. Implement `list_files`, `save_file` as the first tools.
- Verify: `cd tools/pp-mcp && uv run pytest`. Commit: `pp-mcp: FastMCP adapter skeleton with HTTP client, error mapping and file tools`.

### Step 20 — Python tools: master data and transactions

- Create `src/pp_mcp/tools/{instruments,accounts,transactions,watchlists,plans,taxonomies}.py` and tests (`respx` routes asserting the exact JSON body sent, decimals as strings, `dry_run` propagation, error text for 422 with `errors[]`, 423 retry). Tool docstrings state units and precision.
- Verify: `uv run pytest`. Commit: `pp-mcp: master data and transaction tools`.

### Step 21 — Python tools: reports and actions

- Create `src/pp_mcp/tools/{reports,actions,imports}.py` and tests (job polling with mocked `202` then `200 done`; import preview → commit selection).
- Verify: `uv run pytest`. Commit: `pp-mcp: report, quote update and import tools`.

### Step 22 — Live end-to-end (T3)

Goal: exercise every tool against a real PP with a synthetic file and verify the saved file. No source changes except the E2E scripts under `tools/pp-mcp/tests/e2e/` and a fixture generator test.

1. **Build the product** (needs network the first time): `export MAVEN_OPTS=-Xmx4g; mvn -f portfolio-app/pom.xml clean verify -Plocal-dev -DskipTests` from the repo root (the `-Plocal-dev` profile only skips coverage/checkstyle/protobuf, `portfolio-app/pom.xml:105-112`; Windows exe signing is already skipped via `exesigner.skip`, `portfolio-product/pom.xml:138`; the installer step is skipped via `installer.skip`, l.167). Result: `portfolio-product\target\products\name.abuchen.portfolio.product\win32\win32\x86_64\portfolio\PortfolioPerformance.exe`.
2. **Create the synthetic file**: `mvn ... -pl ...,:name.abuchen.portfolio.rest.tests -am -amd -Dtest=name.abuchen.portfolio.rest.e2e.E2EFixtureWriterTest -De2e.fixture=<scratch>\e2e.xml` — a test class that builds a client with `AccountBuilder`/`PortfolioBuilder`/`SecurityBuilder` (EUR and USD accounts, two securities with prices, a few transactions, one taxonomy, one plan) and writes it with `ClientFactory.saveAs(client, file, null, EnumSet.of(SaveFlag.XML))`; it is skipped unless `-De2e.fixture` is set. Never a real user file.
3. **Seed the workspace non-interactively** (`tests/e2e/seed_workspace.py --workspace <scratch>\ws --file <scratch>\e2e.xml --token <random>`):
   - `<ws>\.metadata\.plugins\org.eclipse.core.runtime\.settings\name.abuchen.portfolio.rest.prefs`:
     ```
     eclipse.preferences.version=1
     enabled=true
     port=5712
     files/<B64>/uuid=<uuid4>
     files/<B64>/enabled=true
     files/<B64>/alias=e2e
     ```
     where `<B64>` = base64url without padding of the UTF-8 bytes of the **absolute path exactly as Java's `File#getAbsolutePath()` returns it** (backslashes, drive letter case as typed when opening) — `FileAccessRegistry.java:178-181`, `RestApiAddon.java:64`. (Instance-scope preference files store child nodes as `child/key` lines.)
   - `<ws>\.metadata\.plugins\name.abuchen.portfolio.rest\api-clients.json`:
     ```json
     {"clients":[{"id":"<uuid4>","tokenHash":"<base64url-nopad(sha256(token))>","name":"e2e","created":"2026-09-24T00:00:00Z"}]}
     ```
     (`ClientStore.java:183-195, 276-294`; `Platform.getStateLocation(bundle)` resolves to that folder).
4. **Launch**: `PortfolioPerformance.exe -data <scratch>\ws -consoleLog`. Open `<scratch>\e2e.xml` once via File → Open (there is no CLI argument for this; the part persists the path and later launches restore it automatically — `PortfolioPart.java:104-111`). Confirm in Preferences → REST API that the file is listed as enabled, or simply `curl -H "Authorization: Bearer $TOKEN" http://127.0.0.1:5712/v1/files` → one item with `alias: e2e`, `dirty: false`.
5. **Run the adapter E2E** (`PP_API_URL=http://127.0.0.1:5712 PP_API_TOKEN=<token> uv run python tests/e2e/run_e2e.py`): drives the FastMCP server in-process (`Client(mcp)`) and calls every tool in dependency order: reads → create instrument/account → create buy (dry run, then real, then replay with same `client_ref`) → dividend, transfer, delivery, update, delete → watchlist/plan/taxonomy CRUD → split (dry run) → `update_quotes` (targets `latest` only, expects a job to finish; network optional) → `import_csv` with an inline CSV → `generate_plan_transactions` → `list_files` shows `dirty: true` → `save_file` → `dirty: false`. Each step asserts the response shape; the script fails fast.
6. **Verify the saved content**: (a) `run_e2e.py` re-reads `e2e.xml` with `xml.etree` and asserts the created transaction UUIDs, the `source` `api:<client_ref>`, and the split event are present (XML is the plain XStream format); (b) close PP, relaunch with the same `-data`, and `GET /transactions` must return the same UUIDs; (c) optional: `E2EFixtureVerifyTest` loads the file with `ClientFactory.load(file, null, new NullProgressMonitor())` and asserts counts.
7. **Modal gate check**: with a dialog open in PP (e.g. Preferences), `save_file` must return the `user-interaction` tool error after retries.

- Commit: `pp-mcp: live end-to-end harness and fixture generator`.

## 6. Risks and open questions

**Q1 — `retired` in the instrument API.** #5870 deliberately withholds it until upstream settles the vocabulary ("retired" model vs "deactivated" UI, `SecuritiesHandler.java:55-58`, `ReadEndpointsTest.java:36-62`). F2 requires retire. Recommendation: expose the model's word, `retired: boolean`, on instruments, cash accounts and investment accounts alike, and keep this in a separate commit so it can be dropped or renamed when upstreaming.

**Q2 — Watchlists and investment plans have no UUID** (N4 asks for UUIDs everywhere). Options: (a) address by name (unique name enforced on create; rename via PATCH), (b) add a `uuid` field to both model classes (touches the XML/protobuf persistence and `ClientFactory` migrations — invasive, upstream-sensitive). Recommendation: (a) for v1; the adapter exposes `name` as the identifier for these two entity kinds.

**Q3 — Idempotency for master-data creates** cannot be durable without a place to store the key. Recommendation: durable `source`-based keys for transactions (the only place where duplicates are costly), in-memory keys for master data plus natural-key de-duplication in the adapter (ISIN/name). Revisit if upstream adds `source` or attributes to more entities.

**Q4 — Quote updates need the UI plugin.** `UpdatePricesJob` is UI-bound; the rest plugin reaches it only through the SPI. Alternative: re-implement the ~40 lines of `Task` (`ui/jobs/priceupdate/Task.java:28-126`) against `QuoteFeed` in the rest plugin. Recommendation: SPI method (keeps one implementation of the update policy, respects the price-update progress UI). Per-security outcome reporting is limited to before/after snapshots.

**Q5 — Extraction off the UI thread.** PDF/CSV extraction reads `client.getSecurities()` on the worker thread. This is the trade-off #5870 already accepts for calculations (`ApiRoutes.java:128-134`). If a concurrent UI edit ever corrupts an extraction, the retry is cheap; the commit itself is always on the UI thread and re-validates. Recommendation: accept.

**Q6 — Save while a background price update is running.** `ClientInput.save` keeps the file dirty when a background modification arrived during the save (l.266-278); the API save inherits that behaviour, so `dirty` may still be `true` right after a successful save. The adapter's `save_file` reports the returned `dirty` flag and the docstring explains it.

**Q7 — `Request` has no headers.** Choosing the `clientRef` body field avoids changing the request pipeline. If a header is wanted later, `Request` gains a `headers` map and `RestApiServer.dispatch` (l.87-89) passes `exchange.getRequestHeaders()`; the existing 5-arg constructor stays for tests.

**Q8 — First-launch file open in T3 is manual.** PP has no command-line file argument; pre-seeding `workbench.xmi` is fragile. Recommendation: accept the single manual step (documented in step 22.4); subsequent launches are non-interactive.

**Q9 — Windows path identity.** The `{file}` identity is the absolute path string; the E2E seed must match `File#getAbsolutePath()` byte-for-byte (case of the drive letter, no trailing separator). The seed script normalizes with `os.path.abspath` and upper-cases the drive letter, and the E2E run asserts `GET /v1/files` lists the file before doing anything else.

**Q10 — Upstream drift.** `upstream/feature/rest-api` is an open PR and will move. Every #5870/rest-ext commit is kept verbatim (only the version bump is ours); our work is on top in focused commits, so a later `git rebase --onto` of our commits onto a newer #5870 is mechanical.

## 7. Decisions (2026-09-24)

- Q1: expose `retired` on instruments, cash and investment accounts, in its own commit.
- Q2: watchlists and investment plans are addressed by name.
- Q3: durable `clientRef` for transactions only; in-memory for master data plus adapter-side natural-key checks.
- Q8: add `POST /v1/files/open {path}`, allowed only for paths already enabled in `FileAccessRegistry`. Implemented through a new `HostApplication#openFile(Path)` SPI method (UI: `ClientInputFactory` + open a `PortfolioPart`, as File > Open does). Added to step 3.

## 8. Implementation deviations

- Step 3 / Q8 (`POST /v1/files/open`): besides the agreed `404` (path not enabled, unknown, or missing on disk) and `409 password-required`, the endpoint answers `409 open-failed` (loading failed, reason in `detail`), `503 file-loading` with `Retry-After: 5` (the file did not finish loading within 30 s; it keeps loading and a retry answers `200`), and `423 user-interaction` (a modal dialog or cell editor is active, as for writes). A `201` carries `Location: /v1/files/{id}`. The SPI method is `CompletableFuture<OpenFile> HostApplication#openFile(Path) throws IOException`, with `spi/PasswordRequiredException` for encrypted files. Opening and saving are logged to the application log (`MsgApiFileOpened`, `MsgApiFileSaved`).
- Step 6 (`POST /transactions`):
  - Field names follow the adapter contract (`tools/pp-mcp`): buy/sell total is `amount`; cash kinds book `amount` as the booked (net) amount, `interest` adds `taxes` on top (gross = amount + taxes); dividend `grossValue`, `fees`, `taxes` are in the cash-account currency (the foreign gross is derived with `exchangeRate`), `amount` may replace or check `grossValue`. Buy/sell/deliveries also accept `amount` alone (gross derived as when typing the total in the dialog); `security-transfer` accepts `quote` instead of `amount`. `cash-transfer` has no `exchangeRate`: the rate follows from `amount`/`targetAmount`.
  - Extra `FieldError` codes: `unknown-reference` (a UUID that resolves to nothing) and `unknown-field` (also for `clientRef` in a PATCH). `exchangeRate`/`forexFees`/`forexTaxes`/`targetAmount` given where the currencies are equal are `currency-mismatch`; negative values are `must-be-positive`.
  - A replayed create answers `200 replayed: true` even on a dry run (with `dryRun: true`). `clientRef` is also reported by `GET /transactions/{uuid}` (derived from `source`). `Location` uses the `{file}` segment as addressed.
  - `TransactionTypes` holds `wireType`/`WIRE_TYPES`; `EntityJson.wireType` delegates to it.
  - Application log: `MsgApiTransactionCreated/Updated/Deleted` name type, UUID and file (English text in every locale file).
- Step 7 (`PATCH`/`DELETE /transactions/{uuid}`):
  - A cash-account leg can only move to an account in the same currency (`currency-mismatch`), as `TransactionOwnerListEditingSupport` offers only those. Moving removes the transaction from investment plans (as the UI does).
  - Type changes are allowed within buy/sell, the two deliveries, and the eight cash kinds as one group; a value the new type has no field for must be cleared with `null` in the same patch (`not-allowed-for-type`), and a `null` for such a field is accepted as clearing.
  - Amounts/units are recomputed only if the patch contains `type`, `instrument`, `currency` or an amount field; otherwise (date, note, ex-date, owners) they are left as stored. `shares` alone keeps the gross value; `quote` or `amount` alone re-derive it; the stored exchange rate is dropped (looked up again) when the currency pair changes.
  - No-op detection compares the detailed JSON before/after (without `updatedAt`); a no-op answers `200` without marking dirty.
  - Dry-run `DELETE` answers `200 {"dryRun": true, "removed": [<Transaction>…]}`.
- Step 8 (instruments):
  - The instrument JSON (`GET /instruments[/{uuid}]` and every instrument write) adds `targetCurrencyCode`, `feed`, `feedUrl`, `latestFeed`, `latestFeedUrl`, `feedProperties{}`, `calendar` and `events[]`, each omitted when unset/empty. The holdings enrichment is unchanged.
  - `POST /instruments` takes the patch fields plus `clientRef`; `name` is required, `currencyCode` defaults to the base currency (explicit `null` = index), `feed` to `MANUAL`. It also accepts `targetCurrencyCode` (the adapter contract lists it for patch only), which creates an exchange rate. The application's post-create quote update is not triggered.
  - `PATCH /instruments/{uuid}` gains `?dry_run=true` and no-op detection: the patch is applied to a detached copy first; a patch that changes nothing no longer marks the file dirty (before, any non-empty patch did). `targetCurrencyCode` is writable only on an exchange rate and cannot be cleared. New `FieldError` codes `unknown-feed` (feed id unknown to `Factory.getQuoteFeedProvider` and not `MANUAL`) and `unknown-calendar`.
  - `DELETE /instruments/{uuid}?dry_run=true` answers `200 {"dryRun": true, "removed": [<Instrument>]}` (shared `DeletePreview` schema).
  - `PUT /instruments/{uuid}/prices` answers `200 {uuid, currency, inserted, updated, unchanged}`; values must be positive, a date may occur once per request, errors are reported as `items[i].<field>`. `DELETE …/prices` answers `200 {uuid, removed: <count>}` rather than `204`.
  - Events: `GET …/events` was added; `POST` creates only `stock-split` (details `new:old`, validated like the split wizard) and `note`, answers `201` without `Location` (events have no identifier) and accepts `clientRef` (in-memory). `DELETE …/events?date&type[&details]` answers `200 {"removed": [...]}`, `404` if nothing matches; `dividend-payment` events can be deleted but not created.
  - Application log: new generic keys `MsgApiEntityCreated/Changed/Deleted` (`{0}` entity kind, `{1}` name, `{2}` file); price and event writes are logged as instrument changes.
  - `ApiRoutes` keeps one `IdempotencyIndex` per router for the in-memory master-data keys, keyed by file path; it gained `findObject/rememberObject` for entities without a UUID.
- Step 9 (cash and investment accounts):
  - Bodies as in the adapter contract (`{name, currencyCode, note, attributes, clientRef}` and `{name, referenceCashAccount, note, attributes, clientRef}`); `retired` follows in its own commit (see Q1). Unknown fields are `unknown-field`.
  - `referenceCashAccount` is required on create (the application picks or creates one itself); an unknown UUID is `unknown-reference`, a retired account `invalid-value` unless it is already the current one.
  - A cash account's delete is also blocked while it is the reference account of an investment account (`errors[].field` = `investmentAccounts`): `Client#removeAccount` would silently clear that reference, which the application's own delete allows.
  - `GET` and every write report `attributes` for both kinds (generalized from instruments, `AttributePatch`). A write answers `balance`/`value` as of today (value in the base currency). Patches have dry run and no-op detection (JSON before/after compare); a dry-run delete answers `DeletePreview`.
- Step 10 (watchlists and investment plans, addressed by name per Q2):
  - Path parameters are now percent-decoded per segment by `Router` (`RestApiServer` matches `getRawPath()`), so a name may contain `/`; `+` in a path is literal. A name shared by several entities (possible in files edited by the application) answers `409 ambiguous-name`. New `FieldError` code `already-exists` for a create or rename to a taken name. `GET /watchlists/{name}` and `GET /investment-plans/{name}` were added (the `Location` of a create).
  - Watchlist JSON is `{name, instruments: [{uuid, name}]}` (the request takes UUID strings). `PUT`/`DELETE …/instruments/{uuid}` answer `200` with the watchlist rather than `204`; adding a member twice is a no-op, removing a non-member is `404`.
  - Investment plan JSON: `{name, kind, instrument, investmentAccount, cashAccount, start, intervalMonths|intervalWeeks, amount, fees, taxes (money objects in the plan currency), autoGenerate, note, transactionCount, nextTransactionDate}`; references are UUID strings. `amount` is the stored plan amount (purchase: gross + fees + taxes; interest: net after taxes). Rules from `InvestmentPlanModel`: purchase needs `instrument` and `investmentAccount` and `amount` > `fees` + `taxes`, `cashAccount` optional (delivery); deposit/removal need `cashAccount` and take no fees/taxes, interest takes taxes only (`not-allowed-for-type`). The kind is immutable (`not-allowed-for-type`). Intervals: 1-99 months or 1-99 weeks (the dialog offers 1-12 months and 1-2 weeks). `start` defaults to today. Deleting a plan keeps its transactions, as `InvestmentPlanListView` does.
- Step 11 (taxonomies):
  - Path parameters `{id}` (taxonomy), `{cid}` (category) and `{vehicleUuid}`; `GET /taxonomies/{id}` and `GET …/classifications/{cid}` were added. A single category answers the tree node plus `taxonomy` and `parent` (omitted at the top level). The root category, which names the taxonomy, is not addressable.
  - Every category in the tree now reports `id`, `weight` (percent), `note` and `assignments[{vehicle, type, name, weight}]` (omitted when empty, like `children`).
  - Rename changes only the taxonomy's name, as `Navigation`'s "rename" does (a new taxonomy's root gets the name on create). Taxonomy delete is not blocked.
  - Category create mirrors `TaxonomyNode.addChild`: default weight = 100 % minus the siblings' weights (not below 0), rank after the last child or assignment, random color if none is given; `color` must be `#RRGGBB`; `weight` is 0-100 with 2 decimals. A move via `parent` must not target the category or its subtree (`invalid-value`). `name`, `color` and `weight` cannot be cleared (`required`).
  - `DELETE` of a non-empty category without `?cascade=true` is `409 delete-blocked` (`errors[].field` `children`/`assignments`); with cascade the subtree and its assignments go, as in the application.
  - `PUT …/assignments/{vehicleUuid}` takes an optional body `{weight}` (default: 100 % minus what the vehicle has elsewhere in the taxonomy), always answers `200` with the assignment (also when it created one), rejects a weight of 0 (`must-be-positive`; the UI deletes on 0) and a total above 100 % (`weight-exceeds-100`). `OpenApiSpecDriftTest`'s `FieldError` scan now accepts digits in codes.
- Q1 (`retired`): kept in its own commit, `REST API: expose the retired flag`, on top of steps 8-11, so it can be dropped or renamed when upstreaming. Instruments, cash accounts and investment accounts always report `retired: <boolean>` and accept it on create and patch (`invalid-type` for anything but `true`/`false`, including `null`). `ReadEndpointsTest.testRetiredFlagIsNotExposed` became `testRetiredFlagIsExposedAndWritable`. The step 8 commit message still names "retirement" as in the plan, but the flag lands only in this commit.
- Step 12 (report filters, allocation, earnings):
  - `investmentAccount`/`cashAccount` take one UUID or a comma-separated list, on holdings and all four performance endpoints (not on `/trades`, which the adapter does not filter); an unknown UUID is `400 unknown-reference` like other query errors. A filtered report lists the file's own cash account UUIDs (not those of the filter's read-only copies) and keeps the file's taxonomies and risk-free rate.
  - `GET /taxonomies/{id}/allocation` does not run `ClientClassificationFilter` per category (one snapshot per category): it computes like the application's taxonomy view (`TaxonomyModel`), from one `ClientSnapshot` with each assignment weighting its vehicle's valuation. It answers `{taxonomy{id,name}, date, currency, totalAssets, categories[{id,name,color,weight,value,share,targetValue,deviation,children,assignments}], unassigned{value,share,assignments}}`; targets follow `RecalculateTargetsAttachedModel`. It also accepts the report filters.
  - `GET /earnings` answers `{items: [Transaction], totals: [{currency, value, dividends, interest, interestCharge, taxes, fees, count}]}`; totals are per booking currency and not converted. It also accepts `cashAccount`/`investmentAccount` (transaction-list semantics).
- Step 13 (stock split): `POST /instruments/{uuid}/actions/split` answers `200` (also for a real run) with `{instrument, event, adjustTransactions, adjustPrices, transactionsAffected, pricesAffected, transactions[{uuid, date, type, sharesBefore, sharesAfter}], firstPrice/lastPrice{date, before, after}}` (+ `dryRun`/`replayed`). The counts are transaction legs (both legs of a security transfer). `newShares`/`oldShares` accept up to 8 decimals; non-positive values are `must-be-positive`, equal values `invalid-value` on `newShares`. The event details are the ratio as given (`2:1`, `1.5:1`). A `clientRef` is kept in memory (like master data) and guards against applying the same split twice. Logged as `MsgApiStockSplitApplied`.
