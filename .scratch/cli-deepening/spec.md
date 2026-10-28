# Deepen the pp-cli modules

Status: resolved

Source: the wayfinding map in [map.md](map.md), its resolved tickets 01–06 in [issues/](issues/), and the [architecture review](architecture-review.html). Each section below gists one decision and links the ticket that holds it in full. **The ticket is the authority**; this spec doesn't restate its interfaces in full. Vocabulary: [GLOSSARY.md](../../GLOSSARY.md) (AI/CLI vocabulary) and the `codebase-design` skill (module, interface, seam, adapter, depth, leverage, locality). Implementation tickets: [issues/08](issues/08-guard-rails.md) to [issues/22](issues/22-declared-failure-codes.md).

## Problem Statement

`name.abuchen.portfolio.cli` (about 10,800 lines) grew one command and one operation at a time. The architecture review found six places where a single concept is spread over shallow modules:

- A **Transaction record** is keyed three times: schema, render and parse. Its per-type rules exist twice, once as prose and once as code. Update renders the record, runs it through Gson, overlays the patch and re-adds it. A threaded `replacing` context caused the bug fixed in c84342165.
- A command repeats each option in three or four places. Error-code scope is a list of hard-coded command names, and a regex over the source is what checks it.
- Each converting command has about seven touch points for the **Reporting currency**, and enforces "never guess a rate" in three different ways.
- A **Master data** kind is described in four registries. `Change.entity` is a free string and has already drifted: `taxonomy-assignment` is emitted but undocumented.
- About 54 write failure codes are free strings that no help text lists.
- The scattered rounding sites were suspected of being one hidden module. The review ruled that out (ticket 04, [ADR 0007](../../docs/adr/0007-cli-arithmetic-mirrors-core-rounding-per-site.md)).

About 1,500 lines of tests run end to end through `CliFixture`. Because they test through that path, the modules above have no interface tests of their own.

## Solution

Five deepened modules, each with a small interface and its tests at that interface. The external contract stays fixed except for four allowed additions (see [Contract-change ripple](#contract-change-ripple)).

1. **Command facets** (`cli.command`): a command is `CommandSpec.of(name, summary, dataSchema, handler, facets…)`. Each facet declares its options, meta, warnings and error codes, and emits its meta in the same place. Error-code scope follows from use. `ErrorCode` is an enum.
2. **Valuation** (`cli.currency`): `ReportingCurrency` becomes a facet and the one way to get converted figures. Core-calculated figures pass through `coreFigures().trusted(...)`. A `RateCache` seam has a desktop adapter and an in-memory adapter.
3. **Transaction records** (`cli.transaction`): `TransactionRecords.open(client, existedBeforeBatch)` with `all/find/render/add/update/delete`. Writes return `Applied | Unchanged | Refused`, driven by an internal per-type rule table. Updates take a direct typed-Draft path.
4. **Master data kinds** (`cli.masterdata`): one `MasterDataKind<T>` per kind. Its member list *is* the read item, and schema, render, writes, references, the list command and the existing-entity check all derive from it.
5. **Declared failure codes**: field codes join `ErrorCode`. Each operation's code set is derived from its input schema and its owning module, and is listed in `apply --help <op>`.

Dependencies end up running root → `apply` → {`masterdata`, `transaction`} → `command`, with no cycles.

## Implementation Decisions

### Command facets ([ticket 02](issues/02-command-facets.md))

- `Facet<T>` has three parts: a `Declaration` (options, meta members, warnings, `ErrorCode`s, sample, `needsClient`), `resolve(CommandContext)` and `report(T, Report)`. `CommandSpec.of(...)` is the only way to construct a command. Handlers return data only and read facet values with `run.get(facet)`.
- **Ordering**:
  - Facets resolve in two stages: first those without `needsClient`, then those with it, so usage errors come before file errors. The client loads lazily between the two stages.
  - `report` runs after the handler, in facet order.
  - Meta order is: base envelope, paging, sorting, then the other facets in list order.
  - Help lists options in facet order, followed by `--file`, `--format` and `--verbose`.
- **Class-init rejections**: two facets declaring the same option or meta member, and both `ClientFile` forms on one command.
- **Programming errors** throw `IllegalStateException`, which becomes `internal-error`: an undeclared option, meta member or warning, or `run.client()` called from a facet without `needsClient`.
- **Facets**:
  - `DateRange.facet(fromDesc, toDesc)`, `Paging.FACET`, `Sorting.Spec`, and `Filters.reference(option, metaKey, metaDesc, predicate)`.
  - `PerformancePeriod`, and `PerformanceScope`, which absorbs `--by`.
  - `ClientFile.READ` and `ClientFile.write(dryRunDescription)`, whose value is `ClientFile.Writer`.
  - Apply's local `BATCH` facet and restore's local `BACKUP` facet.
  - Factories take the per-command option descriptions as parameters, so help wording stays the same.
- **Cross-option rules** stay inside the facet that owns all the options involved. There is no `Constraint` type.
- **`ErrorCode` enum** (replaces `ErrorSpec`):
  - Scope is `GLOBAL | EVERY_COMMAND | BY_USE`.
  - `CliException.usage`/`failure` take an `ErrorCode`.
  - A command's codes are the union of three sets: `EVERY_COMMAND`, the codes implied by option types, and the codes its facets declare. The result may be a superset of what the command raises, never a subset.
- **`OptionSpec.choices(...)`**: a multi-choice option (comma-separated, case-insensitive, deduplicated). It replaces `TransactionsCommand.parseTypes`.
- `Response` becomes immutable. `CommandSpec` loses its pass-through constructors and `usesClientFile`. `TableColumn` stays per command.
- When one command line has two usage errors, which one is reported now follows facet order. That isn't contract, and no test may depend on it.

### Valuation ([ticket 05](issues/05-valuation-facet.md))

- `ReportingCurrency` stays the module, in `cli.currency`. `ExchangeRates`, `StrictConverter` and `EcbCache` become its private implementation.
- **Facets**:
  - `facet(desc, Counting)` defaults to the base currency (holdings, allocation, performance).
  - `ifGiven(desc)` has no default (transactions).
  - `Counting { ITEMS, DAYS }` replaces `WARNINGS`/`WARNINGS_COUNTING_DAYS`.
  - The facet owns `--currency`, the `currency` and `exchangeRates` meta, and the three rate warnings.
  - `--currency` is a typed `OptionSpec.currency(...)`, checked at parse time.
- **`StalePrices.FACET`** owns `stale-price`, with `note(security, latest, asOf)`. Its warnings are sorted by name, then UUID. It is listed before `ReportingCurrency`.
- **Trust call**: `coreFigures()` gives `converter()`, `trusted(Supplier)`, `trusted(items, Supplier)` and `unreported()`.
  - Suppliers return finished data.
  - `unreported()` is the single place where core's 1.0 fallback is tolerated (ADR 0002).
  - Performance and `PerformanceByInstrument` use `trusted`.
  - `quietInstrument` and Holdings' `ClientSnapshot` use `unreported()`.
  - Holdings and Transactions keep the CLI's own `figure`/`total` path.
  - Allocation uses `trusted(items, …)`, and `allRated` goes.
- **Seam**: `ReportingCurrency(code, client, RateCache, today)`, where `RateCache { Loaded load() throws Unavailable; }`. Its two adapters are `RateCache.desktop(env, osName)` (ADR 0005 order) and the in-memory `RateCache.of(series…)`.
- **Rounding**: the two hand-copied inversions call core's `ExchangeRate.inverse`. The output stays the same (ticket 04).

### Transaction records ([ticket 01](issues/01-transaction-record-module.md))

- **Module**: one module in `cli.transaction`. It absorbs `query/TransactionRecord`, `BuiltRecord`, `TransactionShape` and the render half of `TransactionsCommand`, plus `apply/TransactionInput`, `TransactionOperations`, `TradeOperations`, `TransferOperations`, `TransactionUpdates`, `OversellGuard` and `EntityKind.TRANSACTION_RECORD`.
- **Interface**:
  - `TransactionRecords.open(client, existedBeforeBatch)` gives `all()`, `find(sideUuid)` and `render(record)`.
  - `add(input)`, `update(input, Claims)` and `delete(input, Claims)` each return `Outcome = Applied | Unchanged | Refused(List<FieldFailure>)`.
  - It publishes the static output, add and patch `Schema`s.
- **Invariants**:
  - `Refused` leaves the model untouched.
  - Every rule outcome is returned. Only programming errors throw.
  - Claims are taken before validation.
  - The side join runs once per call and is never cached.
  - The reported record is derived from the sides just built.
- **Rule table**: an internal `(type, transfer kind) × member → REQUIRED | OPTIONAL | REFUSED` table. Each refusal code is declared once, on its member. Messages follow `"<TYPE> must not have <member>."`, and aren't contract. Cross-field invariants stay as named code beside the table. Failure order follows member order; tests assert on sets of `(field, code)` pairs.
- **Help prose** stays hand-written. A drift test checks that every REQUIRED or REFUSED cell is named in its member's description.
- **Update** takes a direct path: Draft from the existing sides, then the patch, then the same checks as add, then a copy back. `type-immutable` and `kind-immutable` are Draft checks. Diffs compare rendered members with typed equality and drop `updatedAt`.
- **`OversellGuard`** is private to the module.
- The three transaction `OperationSpec` handlers live in `cli.transaction` (ticket 02).

### Master data kinds ([ticket 03](issues/03-master-data-kind.md))

- **Package**: `cli.masterdata` holds `MasterDataKind<T>` and one declaration per kind, published as `INSTRUMENTS`, `CASH_ACCOUNTS` and `INVESTMENT_ACCOUNTS`. A declaration holds:
  - the entity id and noun;
  - the entities, UUID and name;
  - an ordered member list;
  - the lookup keys and candidate details;
  - create, add and initialize;
  - a named validator;
  - the list command's summary and columns.
- **Derived from the declaration**: the reference lookup, the paged list command, the `EntityKind`, the item `Schema` and `render`, and the add and update `OperationSpec`s.
- **Removed**: `References.Kind`, `MasterDataList.Source`, `EntityOperations.Definition`/`Field`, the master data `EntityKind` constants, and the three `query` list commands.
- `AttributeCodec`, the resolver and the list builder move into `cli.masterdata`.
- **Members**:
  - A member has a name and a read schema. A writable member also has a writer (description, required/optional/flag, setter, "needs" phrase, single-member checks). Every write field is therefore a read member by construction.
  - Members render *always* or *if set*.
  - `attributes` and `updatedAt` are read-only.
  - Read and write descriptions stay separate strings.
- **Checks**: single-member checks sit on the writer. `duplicate-isin`, `currency-immutable` and the existing reference cash account are the kind's validator.
- **Writes**: `kind.add(JsonObject)` and `kind.update(JsonObject, Claims)` return ticket 01's `Outcome`. Claiming stays private.
- **Taxonomy** is not a kind. It shares only a narrow `Reference` interface in `cli.command`, which the reference facet uses. `EntityKind.of(reference)` serves all four kinds.
- **`Change.entity`** becomes an enum: `instrument`, `cash-account`, `investment-account`, `transaction`, `taxonomy-assignment`.
- **Dependency direction**: `OperationSpec` and `ApplyBatch.OPERATION_MEMBER` move to `cli.command`. The operation registry moves from `Commands.OPERATIONS` into `apply`.

### Declared failure codes ([ticket 06](issues/06-write-failure-codes.md))

- **One enum**: field codes join `ErrorCode`, and `FieldFailure(field, ErrorCode, message, check)` takes it. A facet's codes appear as the problem document's `code`; an operation's or `BATCH`'s codes appear in `failures[].code`.
- **`OperationSpec.failureCodes()`** is derived. It is the union of:
  - the codes implied by the input `Schema`: `required`, `invalid-type`, `not-positive`, `invalid-amount`, `reference-not-found`, `unknown-field`, and `duplicate-target` when the operation claims;
  - the codes declared by its owning module (`TransactionRecords` per add/update/delete, `MasterDataKind` per kind, `taxonomy.assign`).
- **`BATCH`** declares `invalid-operation` and `unknown-operation`.
- **Help**:
  - `apply --help <op>` gains a "Failure codes:" section.
  - `apply --help` lists the batch codes and adds one pointer line.
  - The global help lists no field-only codes.
- `check` stays a free string. Only `import-check-failed` is declared.
- **Glossary**: **Failure** is already added (ticket 06).

### Ruled out: an amounts & rates module ([ticket 04](issues/04-amounts-and-rates.md), [ADR 0007](../../docs/adr/0007-cli-arithmetic-mirrors-core-rounding-per-site.md))

No module. The only follow-up is two one-line comments, each naming the site it mirrors:

- the scale-10 HALF_UP transfer rate in `TransferOperations` mirrors the desktop `AccountTransferModel.setAmount`;
- the double `share` in `AllocationCommand` mirrors core's `getShare()`.

Both comments are in [ticket 08](issues/08-guard-rails.md).

### Migration order

The order follows four rules:

- **Guard first.** A byte-for-byte golden test of every command's `--help` lands before any refactor. Every later ticket that changes help on purpose updates the golden file in the same commit, and says so in its commit message.
- **The framework before its users.** `ErrorCode` comes first, then the facet framework, then the commands, one group per ticket.
- **Moves before deepening.** The shared write framework moves to `cli.command` before the transaction and master data modules are built on it.
- **Transitional coexistence is allowed inside the migration, never at its end.** The old `CommandSpec` constructors and `OperationContext.replacing()` live on until the tickets that remove them ([15](issues/15-finish-facets.md) and [19](issues/19-transaction-update-delete.md)).

| # | Ticket | Blocked by |
|---|---|---|
| 08 | [Golden help test and ADR 0007 comments](issues/08-guard-rails.md) | — |
| 09 | [`ErrorCode` enum](issues/09-error-code-enum.md) | 08 |
| 10 | [Facet framework and the plain read commands](issues/10-facet-framework.md) | 09 |
| 11 | [`ClientFile.write` facet: apply and restore](issues/11-write-facet.md) | 10 |
| 12 | [ReportingCurrency seam and trust call](issues/12-reporting-currency-seam.md) | 08 |
| 13 | [Converting commands on facets: holdings, allocation, transactions](issues/13-converting-commands-on-facets.md) | 10, 12 |
| 14 | [Performance on facets](issues/14-performance-on-facets.md) | 10, 12 |
| 15 | [Finish facets: derived scope, immutable Response, derived samples](issues/15-finish-facets.md) | 11, 13, 14 |
| 16 | [Shared write framework in `cli.command`](issues/16-shared-write-framework.md) | 11 |
| 17 | [TransactionRecords read side](issues/17-transaction-records-read.md) | 13, 16 |
| 18 | [TransactionRecords add and the rule table](issues/18-transaction-records-add.md) | 17 |
| 19 | [TransactionRecords update and delete](issues/19-transaction-update-delete.md) | 18 |
| 20 | [MasterDataKind read side](issues/20-master-data-kind-read.md) | 10, 16 |
| 21 | [MasterDataKind writes](issues/21-master-data-kind-writes.md) | 20 |
| 22 | [Declared failure codes](issues/22-declared-failure-codes.md) | 15, 19, 21 |

Three tracks can run in parallel once their blockers are resolved:

- **Facets**: 10, 11, 13, 14, 15.
- **Valuation seam**: 12, which is free from the start.
- **Write modules**: 16, then 17–19 and 20–21.

Some edges exist only to avoid merge conflicts, not for a green suite:

- 16 waits for 11, because both rewrite `ApplyCommand`.
- 17 waits for 13, because both rewrite `TransactionsCommand`.

### Contract-change ripple

Every contract change lands in the ticket that causes it, together with its golden-help, `SKILL.md` and SkillDriftTest updates. There is no separate ripple ticket.

| Change | Ticket | Help (golden file) | Skill |
|---|---|---|---|
| Meta-member order in a `Response meta:` block, if deriving it differs | the ticket converting that command (10, 11, 13, 14) | updated, called out in the commit | — |
| `--type` "one or more of …" line (`OptionSpec.choices`) | 13 | updated | — |
| `--currency` line, if the typed option renders differently | 13 (and 14 for performance) | updated if changed | — |
| Allocation's count-1 `exchange-rate-missing` when only core lacks a rate | 13 | — | none: SKILL.md already says to pass on every warning |
| `Change.entity` enum including `taxonomy-assignment` | 16 | `apply --help` results schema | `write-protocol.md` step 4 mentions taxonomy assignments as an entity in `results` |
| "Failure codes:" in `apply --help <op>`, batch codes and pointer line in `apply --help` | 22 | updated | SKILL.md `validation-failed` row: "Explain each entry of `failures`; `pp-cli apply --help <operation>` describes its code." |

SkillDriftTest itself changes only where a ticket makes it: the `ErrorCode.values()` source in ticket 09.

## Testing Decisions

- **Replace, don't layer** (map Notes). When a module gets interface-level tests, the end-to-end `CliFixture` tests they duplicate are deleted in the same ticket. Each ticket names the tests it deletes. Each command keeps one or two smoke runs. `CliRun` keeps its whole-response contract check on every run.
- **New interface seams**:
  - `FacetHarness.run(facet, args, client)` checks the meta fragment, warnings and thrown codes against the declaration.
  - `RateCache.of(...)` gives in-memory rates.
  - `TransactionRecords` and `MasterDataKind` are tested on an in-memory `Client` built with the junit `AccountBuilder`, `PortfolioBuilder` and `SecurityBuilder`.
- **Contract guards**:
  - the golden `--help` test (ticket 08);
  - `CliRun`'s error code in the command's `errorCodes()` (ticket 15);
  - `CliRun`'s `failures[].code` in the operation's set (ticket 22);
  - a static check that every `BY_USE` code is declared by a facet, operation or `BATCH` (tickets 15 and 22);
  - the transaction help-prose drift test (ticket 18);
  - SkillDriftTest.
- **Unordered sets**: tests assert sets wherever the order is not contract: failures inside `Refused`, warning order across codes, and usage-error precedence.
- **Commands**: every ticket ends with the CLI suite green:

  ```
  mvn -f portfolio-app/pom.xml verify -Plocal-dev -o -pl :portfolio-target-definition,:name.abuchen.portfolio.pdfbox1,:name.abuchen.portfolio.pdfbox3,:name.abuchen.portfolio,:name.abuchen.portfolio.junit,:name.abuchen.portfolio.cli,:name.abuchen.portfolio.cli.tests -am -amd
  ```

## Out of Scope

- **Core changes.** ADR 0002 keeps core untouched.
- **An amounts & rates module.** Ruled out by ADR 0007.
- **Generating transaction help prose from the rule table.** That would be a later contract change.
- **A per-(operation, code) reachability probe.**
- **Declaring the `check` names.** They belong to core.
- **ADR numbering housekeeping.** Done outside this map.

## Further Notes

- No new ADRs come from tickets 01, 02, 03, 05 or 06. ADR 0007 records ticket 04.
- Ticket messages ("`<TYPE>` must not have …") aren't contract. Codes and fields are.
