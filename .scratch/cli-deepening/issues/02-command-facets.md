# 02: What is the facet interface that a command is composed of?

Type: grilling
Status: open
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
