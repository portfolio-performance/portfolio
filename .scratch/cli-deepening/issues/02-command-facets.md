# 02: What is the facet interface that a command is composed of?

Type: grilling
Status: resolved
Blocked by:

Map: [map.md](../map.md). Review card 3, "Command facets", in [architecture-review.html](../architecture-review.html#c3). Use design-it-twice (see map Notes). Must keep ADR 0006: CommandSpec, OptionSpec and Schema stay the single source help is generated from.

## Question

What interface lets one concern (a date range, paging, sorting, a reference filter, the write scaffolding) declare its options, meta schema, warnings and error codes **and** emit its meta in one place? How does `CommandSpec` compose those concerns, and how does error-code scope come from use instead of from command-name lists?

Evidence from the review:

- Each option appears in three or four places: the static field, the SPEC list, META, and the read plus `meta.put` in `execute`. For example TransactionsCommand ~48–66, ~83–96 and ~120–149, and HoldingsCommand ~118–122 vs ~197–201.
- The pattern already exists but is used unevenly: `declareIn`/`putInto` in DateRange (~37–51), Sorting, PerformanceScope and PerformancePeriod. Holdings, Allocation and Transactions don't use it for their date or filters.
- ErrorSpec scopes codes by hard-coded command names (`invalid-date-range` → "prices", "transactions", "performance", ~98–99; write errors → "apply", "restore"; `option-required` → "performance"). HelpConsistencyTest only compares code *sets*, found by a regex over the source (~248–266).
- `invalid-option-value` is hand-rolled in `TransactionsCommand.parseTypes` (~157–174) and `ReportingCurrency.requested` (~111–119). The first is a missing multi-choice OptionSpec type.
- ApplyCommand and RestoreCommand repeat the write scaffolding: DRY_RUN, `SafeWrite.OPTIONS`, `writeSchema`, `SafeWrite.WARNINGS`, forwarding warnings, and `ResponseMeta.written`.
- `Response` is a record around a mutable list; CommandSpec has three pass-through constructors.
- Adding a command means touching `Commands.ALL`, `HelpConsistencyTest.sampleArguments`, ErrorSpec name lists, and sometimes TableColumns.

Sub-questions to settle: the facet interface's members; whether a write command is a facet or a command kind; whether a test checks that each facet's `putMeta` matches its schema; what replaces the regex scan; whether the hand-picked sample arguments in HelpConsistencyTest can be derived.

## Answer

Resolved by grilling, using design-it-twice. Four parallel designs were compared:
- minimal: one `Facet<T>`, `CommandSpec.of`, `run.get`;
- flexible: a facet graph with `requires`, capabilities, declarative rules and contributed item members;
- easy common caller: a `ListCommand<T>` builder, with write as a command kind;
- declaration-driven: no facet object, a generic `OptionSpec<V>` with echoes, constraints and `Derived`.

The chosen design is the minimal one. It takes from the common-caller design a `Filters.reference(...)` helper, and from the declaration-driven design the finding that help prints meta members in schema order (`HelpText` ~164), which differs from option order.

**Interface.** In `cli.command`:

```java
interface Facet<T> {
    Declaration declaration();          // options, meta fragment (members only), warnings, ErrorCodes, sample, needsClient
    T resolve(CommandContext run) throws CliException;
    default void report(T value, Report out) {}       // meta members + warnings, after the handler
    static <T> Facet<T> of(Declaration d, Resolver<T> r);
    static <T> Facet<T> of(Declaration d, Resolver<T> r, Reporter<T> p);
    static Facet<...> option(OptionSpec spec);         // a lone option: value is the parsed option
}
interface Report { void meta(String member, Object value); void warn(Warning w); }
CommandSpec.of(name, summary, dataSchema, handler, Facet<?>... facets)   // the only construction path
interface Handler { Object execute(CommandContext run) throws CliException; }  // returns data only
// CommandContext gains: <T> T get(Facet<T>), Client client(), warn(Warning)
// derived on CommandSpec: options(), metaSchema(), warnings(), errorCodes(), tableColumns()
```

**Invariants and ordering.**

- Facets resolve in list order, split into two stages. First come the facets without `needsClient`. Then the client file loads, lazily on the first `run.client()`. Then come the `needsClient` facets; reference facets are `needsClient` automatically. Usage errors therefore still come before file errors.
- `report` runs after the handler, in facet order.
- The meta order is fixed by the framework: base envelope (from `ClientFile`), paging, sorting, then the other facets in list order.
- Help lists options in facet-list order, with `--file`, `--format` and `--verbose` always at the end.
- `CommandSpec.of` rejects at class-init: two facets declaring the same option or meta member, and both `ClientFile` forms on one command.

**Error modes.**

- A rule violation is a `CliException` from the facet's `resolve`.
- These programming errors throw `IllegalStateException`, which becomes `internal-error`:
  - a facet reading an option it didn't declare;
  - a facet emitting an undeclared meta member or warning code, or missing a required member it declares;
  - `run.warn` with a code that no facet of the command declares;
  - `run.client()` from a facet without `needsClient`.

**Facets.**

- `DateRange.facet(fromDesc, toDesc)` declares `invalid-date-range`.
- `Paging.FACET`.
- `Sorting.Spec` implements `Facet<Sorting<T>>`.
- `Filters.reference(option, metaKey, metaDesc, predicate)` for the reference filters.
- `ReportingCurrency`. The shape of its facet and its internals belong to ticket 05, which also decides where holdings' `stale-price` is declared.
- `PerformancePeriod`, and `PerformanceScope`, which absorbs `--by` because `unsupportedScope` couples the two.
- Facet factories take the per-command option descriptions as parameters, so help wording does not change.

**Write.** A facet, not a command kind.

- `ClientFile.READ` owns `--file`, the `fileVersion`/`asOf` meta and the client-file codes.
- `ClientFile.write(dryRunDescription)` additionally owns `--dry-run`, `SafeWrite.OPTIONS`, the write meta (`fileVersionBefore`/`fileVersionAfter`, `dryRun`, `backupPath`), `SafeWrite.WARNINGS` and the codes `consistency-check-failed`, `file-open-in-app`, `file-locked`, `file-changed` and `write-failed`.
- Its value, `ClientFile.Writer`, owns the open/run/replace/outcome cycle and forwards the outcome's warnings.
- A command has exactly one `ClientFile` form or none (`info`). `usesClientFile` goes.
- Apply gets a local `BATCH` facet (`input-unreadable`, `invalid-input`, `validation-failed`). Restore gets a local `BACKUP` facet (`backup-not-found`).
- Ticket 06's per-operation codes stay on `OperationSpec`; nothing here blocks them.

**Cross-option rules.** They stay inside the facet that owns every option involved: `conflicting-options`, `option-required` for `--with-reference-cash-account`, and `period-required` in `PerformancePeriod`/`PerformanceScope`. There is no declarative `Constraint` type: its only user would be performance, which makes it a hypothetical seam.

**Error codes.**

- `ErrorSpec` becomes the `ErrorCode` enum in `cli.command`, with the same codes and global descriptions. Scope becomes `GLOBAL | EVERY_COMMAND | BY_USE`.
- `CliException.usage`/`failure` take an `ErrorCode`, not a string.
- A command's codes are the union of:
  - the `EVERY_COMMAND` codes;
  - the codes implied by option types (a Reference brings `reference-not-found`/`reference-ambiguous`; any required option brings `option-required`);
  - its facets' declared codes.
- The scope may be a superset of what the command can actually raise, but never a subset.
- The command-name lists, `Scope.COMMANDS`/`REQUIRED_OPTION` and `alsoCommands` go.
- Help filters `ErrorCode.values()` by that set.

**Other decisions.**

- `OptionSpec.choices(...)`: a multi-choice type (comma-separated, case-insensitive, deduplicated in published order) replaces `TransactionsCommand.parseTypes`. Its "one or more of ..." help line is an accepted contract addition.
- `Response` becomes immutable (`List.copyOf`, unmodifiable meta), built only by the framework; `warn()` goes. Handlers return data only and warn through `run.warn`.
- `CommandSpec`'s three pass-through constructors go.
- Usage-error precedence may change: when one command line has two usage errors, the one reported follows facet order (e.g. `--type` before `--from`). It isn't contract, so no test may depend on it.
- `TableColumn` stays per command, as item rendering rather than a facet.
- Apply's three transaction `OperationSpec` handlers move to `cli.transaction`, beside `TransactionRecords`, whose add and patch schemas they publish. `apply` keeps `ApplyBatch`, `ApplyCommand`, `RestoreCommand` and the master data operations, which are ticket 03's to reshape.

**Help contract guard.** A golden test of every command's `--help` output, byte for byte, written before the migration starts. If deriving the meta order still leaves a difference, it is a one-line contract change called out in the spec. The `--type` line is the one expected addition.

**Tests (replace, don't layer).**

- The regex scan in `HelpConsistencyTest` goes, because the typed `ErrorCode` closes the code set at compile time. In its place:
  - CliRun asserts, on every failing run, that the code is in that command's `errorCodes()`;
  - a static test checks that every `BY_USE` code is declared by some facet in `Commands.ALL`.
- `FacetHarness.run(facet, args, client)` gives each facet an interface test. It checks the meta fragment against the declared schema, the warnings against the declared ones, and that what the facet throws is within its declared codes. The per-command copies are deleted (e.g. `invalid-date-range` in the Prices, Transactions and Performance tests).
- `sampleArguments` is derived from the facets' samples: a reference draws the first entity of its kind from the fixture. An override map keeps two entries, `apply --input` and `restore --backup`, and the switch and its "add sample arguments" failure go.
- CliRun keeps its whole-response contract check.

**Not changed.** No glossary terms ("facet" is an implementation name). No ADR: facets are how a command assembles the ADR 0006 declarations, which is not surprising.
