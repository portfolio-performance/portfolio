# Deepen the pp-cli modules

Labels: wayfinder:map

## Destination

A hand-off-ready refactoring spec for `name.abuchen.portfolio.cli` and its tests: for each candidate of the [architecture review](architecture-review.html), the deepened module's interface decided (or the candidate ruled out), the work sequenced, and implementation tickets ready for agents. No refactoring code changes inside this map.

## Notes

- Scope: `name.abuchen.portfolio.cli` and `name.abuchen.portfolio.cli.tests` only. Core is out of scope (ADR 0002 keeps it untouched).
- Read `GLOSSARY.md` for the domain (the "AI/CLI vocabulary" section names the external terms: **Transaction record**, **Side**, **Master data**, **Reporting currency**, **Existing entity**, ...).
- Use the `codebase-design` vocabulary exactly: module, interface, implementation, depth, seam, adapter, leverage, locality.
- The external contract stays fixed (JSON shapes, error and warning codes, help text), except where a finding is itself a contract fix: the `Change.entity` value set (must include `taxonomy-assignment`) and write failure codes listed in `apply --help <op>`. ADR 0006 and the skill drift tests stay as the backstop.
- Package layout may change where a deepened module calls for it; packages follow modules, not the read/write direction (the recent `query`/`apply` split is not binding).
- Tests: when a deepened module gets interface-level tests, delete the end-to-end CliFixture tests they duplicate (replace, don't layer). Each command keeps one or two smoke runs; CliRun keeps its contract check on every run.
- Skills: `grilling` + `domain-modeling` for every grilling ticket; additionally the `codebase-design` design-it-twice pattern (parallel sub-agents, radically different interfaces) for the Transaction record and Command facets tickets.
- A candidate rejected with a load-bearing reason goes to **Out of scope**, with an ADR offered. New ADRs take the next number not already in use in `docs/adr/`.
- Build and test commands: see `CLAUDE.md`; CLI tests need `:name.abuchen.portfolio.junit,:name.abuchen.portfolio.cli,:name.abuchen.portfolio.cli.tests` in `-pl`.

## Decisions so far

<!-- one line per closed ticket: [title](issues/NN-slug.md): gist -->

- Fixed before charting (commit c84342165, "Stop a transaction update whose type or kind change is refused"): `transaction.update` now stops when the overlay refuses a type or kind change; previously the replacing context built the replacement anyway.
- [What is the interface of a deepened Transaction record module?](issues/01-transaction-record-module.md): one `cli.transaction` module, `TransactionRecords.open(client, existedBeforeBatch)` with `all/find/render` and `add/update/delete` returning `Applied | Unchanged | Refused(FieldFailure…)`. Internal per-type rule table, a direct typed-Draft update path, OversellGuard inside, one join per call. Help stays hand-written with a drift test. `OperationInput` and `EntityKind` move to `cli.command` behind a failure sink.
- [What is the facet interface that a command is composed of?](issues/02-command-facets.md): `Facet<T>` (Declaration + resolve + report) composed by `CommandSpec.of(..., facets…)`; handlers return data via `run.get(facet)`. `ClientFile.READ`/`write(...)` are facets (no write kind), cross-option rules stay in their owning facet, `ErrorSpec` → `ErrorCode` enum with scope derived from facets, immutable `Response`, multi-choice `OptionSpec`. Regex scan → CliRun code-in-scope check; `FacetHarness` tests; derived samples; golden `--help` test first. Transaction `OperationSpec`s move to `cli.transaction`; `TableColumn` stays per command.
- [Should one module per Master data kind replace the four registries?](issues/03-master-data-kind.md): yes. A `MasterDataKind<T>` per kind in `cli.masterdata`, whose member list *is* the read item, so schema, render, writes, references, list and existing-entity check all derive from it. Writes return ticket 01's `Outcome`. Taxonomy shares only a narrow Reference interface in `cli.command`. `Change.entity` becomes a five-value enum, including `taxonomy-assignment`. `OperationSpec` moves to `cli.command` and the operation registry into `apply`, which breaks both cycles. The three `query` list commands are deleted.

## Not yet specified

- **Contract-change ripple.** Help text, skill (`SKILL.md`) and SkillDriftTest updates for the `Change.entity` enum, the declared failure codes, the multi-choice `--type` help line, and any `Response meta:` line the golden help test shows changed (ticket 02). The `Change.entity` enum (ticket 03) is now fixed. Hangs on ticket 06.
- **Migration order.** How to stage the refactors so the suite stays green and each step is one agent session; which end-to-end tests go when. Settled inside ticket 07 once the interfaces are known.

## Out of scope

- ADR numbering housekeeping (CLI ADRs share numbers 0002–0005 with the REST API ADRs; `docs/adr/README.md` indexes only the REST ones): documentation housekeeping with no decision in it; done directly, outside this map.
