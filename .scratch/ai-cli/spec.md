# pp-cli: headless AI-agent access to client files

Status: ready-for-agent

Source: the wayfinding map in [map.md](map.md) and its resolved tickets in [issues/](issues/). Architecture: [ADR 0002](../../docs/adr/0002-headless-java-cli-module-in-fork.md). Packaging: [ADR 0003](../../docs/adr/0003-package-cli-as-self-contained-per-platform-bundle.md). Vocabulary: [GLOSSARY.md](../../GLOSSARY.md).

## Problem Statement

I keep my complete investment data in a Portfolio Performance **client file**. I want AI agents (GitHub Copilot in VS Code, Claude Code in a shell) to answer questions about it and make routine changes for me. Today an agent has no reliable way to do either:

- Reading the raw XML gives the agent entities but none of Portfolio Performance's **calculated figures** (valuation, IRR, TTWROR, gains, currency conversion). The agent then either guesses or re-implements the engine badly.
- Editing the XML by hand, or with any of the existing third-party tools, skips Portfolio Performance's validation. It can corrupt the file, lose data on a newer file version, or silently break buy/sell pairs.
- The upstream in-app REST API and MCP endpoint (PR #5870) only work while the desktop app is running with the file open. Its writes cover instruments only, they stay in memory and a human has to save them, and its read set has no transactions, prices, taxonomies or allocation.

I need a command an agent can run from its shell against a closed client file. It must return the engine's own figures as JSON and apply validated domain changes safely, with backups, dry-run, and clean refusals instead of corruption.

## Solution

`pp-cli` is a command-line tool built as a new, additive Java 21 module in this fork. It runs Portfolio Performance's own core engine headlessly on a plain classpath, one JVM per command, with no daemon and no desktop app.

- **Reads** list instruments, investment accounts, cash accounts, **transaction records**, **holdings**, prices, performance, taxonomies and allocation. Each command returns one JSON envelope `{data, meta, warnings}` with the engine's calculated figures.
- **Writes** go through one command, `pp-cli apply`. It takes a JSON batch of domain operations: add, update and delete transaction records; add and update instruments and accounts; replace an instrument's taxonomy assignment. The batch is atomic and reuses core's validation plus a few extra rules. Every write follows one safe-write sequence: lock, consistency check, timestamped backup, temp file plus atomic rename, and a check that the file did not change on disk. `--dry-run` shows the entity-level changes without writing anything. `pp-cli restore` rolls back to a backup.
- **Formats and versions**: every client file format core supports is read, and written back in its existing format. A file newer than the CLI's core is refused cleanly.
- **Discovery**: `pp-cli describe` publishes every command, option, operation and response schema, so an agent can learn the tool without outside documentation.
- **Distribution**: a self-contained bundle per platform (macOS arm64, Windows x64) with a bundled runtime, built locally. An idempotent install script puts `pp-cli` on `PATH` for both agent shells. Per-platform build, install and launch docs are included.

## User Stories

### Discovery and setup

1. As the owner, I want to build `pp-cli` locally from my fork on my macOS arm64 machine, so that I can use it without a CI pipeline or signing.
2. As the owner, I want to build `pp-cli` locally from my fork on my Windows x64 machine, so that I can use it on both machines I own.
3. As the owner, I want one idempotent install script per platform, so that installing and updating are the same step.
4. As the owner, I want the macOS installer to put `pp-cli` where non-interactive agent shells find it (a Homebrew bin symlink, or `~/.zprofile` and `~/.zshenv`), so that Claude Code and Copilot can run it without me editing shell config.
5. As the owner, I want the Windows installer to add the install directory to my user `PATH`, so that new VS Code terminals, PowerShell, cmd and Git Bash all resolve `pp-cli`.
6. As the owner, I want the Windows launcher to be a `.cmd` wrapper, so that PowerShell execution policy never blocks it.
7. As the owner, I want the bundle to include its own Java runtime, so that I don't have to install or manage a JDK.
8. As the owner, I want an AppCDS archive generated at install time, so that each one-JVM-per-call command starts faster.
9. As the owner, I want written per-platform instructions for building, installing, confirming `PATH` in both agent shells, and updating, so that I can repeat the setup months from now.
10. As the owner, I want to update the CLI by bumping the pinned upstream tag, rebuilding and re-running the installer, so that the CLI's core tracks my desktop release.
11. As an AI agent, I want `pp-cli describe` to return machine-readable commands, options, write operation schemas and response schemas, so that I can use the tool correctly without prior training.
12. As an AI agent, I want `describe` to be generated from the same definitions that implement the commands, so that the documentation can never drift from the behavior.
13. As an AI agent, I want `describe` to state the known limits (unsaved edits inside the running app can't be detected; no PDF import; no price writes), so that I don't promise the user things the tool can't do.
14. As the owner, I want `describe` and `info` to report the pinned upstream core tag and the maximum supported file version, so that I can check that the CLI matches my installed desktop app.

### Selecting the client file

15. As an AI agent, I want to pass the client file with `--file`, so that each command is explicit and reproducible.
16. As an AI agent, I want `PP_FILE` used when `--file` is absent, so that I don't repeat the path on every call in a session.
17. As the owner, I want encrypted-file passwords to come only from `PP_PASSWORD` or an interactive stdin prompt, never a flag, so that passwords don't leak into shell history or process listings.
18. As an AI agent, I want a `password-required` error when an encrypted file has no password, so that I can ask the user for one.
19. As an AI agent, I want an `incorrect-password` error when decryption fails, so that I can tell the user the password was wrong without parsing a localized message.
20. As the owner, I want the password never echoed, logged or shown in a stack trace, so that it stays secret.

### Formats and file versions

21. As the owner, I want `pp-cli` to read all seven client file formats (XML, XML with ID references, XML in ZIP, binary, XML AES-256, XML AES-128, binary AES-256), so that it works whichever format I chose in the app.
22. As the owner, I want writes to keep the file's existing format, never converting it, so that the desktop app opens it exactly as before.
23. As the owner, I want encrypted files written back with the key from load, so that I type the password only once per command.
24. As the owner, I want any file newer than the CLI supports refused on every command with `file-version-unsupported`, `fileVersion` and `maxSupportedVersion`, so that an outdated CLI never loses data. This applies to binary files too, which core itself doesn't guard.
25. As the owner, I want a version refusal on a write to happen before any lock, backup or temp file is created, so that a refused command leaves no trace.
26. As the owner, I want an older file migrated on write and reported through `fileVersionBefore`, `fileVersionAfter` and a warning, so that I know an older desktop app can no longer open it.
27. As an AI agent, I want `info` to report the file version, the detected format, the base currency and entity counts, so that I can orient myself before deeper queries.

### Reading master data

28. As an AI agent, I want to list instruments with UUID, name, currency, ISIN, ticker, WKN, retired flag and user-defined attributes, so that I can identify what the user holds.
29. As an AI agent, I want to list investment accounts with their reference cash account and retired flag, so that I understand where instruments are held.
30. As an AI agent, I want to list cash accounts with their currency and retired flag, so that I understand where cash moves.
31. As an AI agent, I want UUIDs to be canonical in all input and output, so that references are stable across calls.
32. As an AI agent, I want to address an entity by ISIN, ticker or exact name as a convenience, so that I can act on what the user said without a lookup round-trip.
33. As an AI agent, I want an ambiguous ISIN, ticker or name to fail and list the candidates rather than guess, so that I never act on the wrong entity.

### Reading transactions

34. As an AI agent, I want each business event returned as one **transaction record**, so that a buy shows both its investment-account side and its cash-account side together.
35. As an AI agent, I want transfers returned as one record with source, target and a `kind`, so that I don't double-count them.
36. As an AI agent, I want to filter transactions by account, instrument, date range and type, so that I fetch only what I need.
37. As an AI agent, I want `type` to use the core enum names and to be documented as an open enum, so that a new upstream type doesn't break my parsing.
38. As an AI agent, I want each record to carry its `id` and the UUIDs of all its sides, so that I can address it in later writes using either side.
39. As an AI agent, I want amounts in each record's native currency with an ISO code, and converted fields added with `--currency`, so that I can compare across currencies without losing the original values.
40. As an AI agent, I want a converted transaction amount to use the transaction's own forex rate or its date's rate, and to carry the rate and rate date, so that I can explain the conversion.
41. As an AI agent, I want fees, taxes and forex units in the same shape I can write back, so that I can read, edit and re-submit a record.

### Holdings, prices, performance, allocation

42. As an AI agent, I want `holdings --date` aggregated across investment accounts, so that I can answer "what do I own and what is it worth".
43. As an AI agent, I want `--investment-account` to filter holdings and `--by-account` to split them into one block per account, so that I can answer account-specific questions.
44. As an AI agent, I want zero-share holdings hidden unless I pass `--include-zero`, so that output stays focused.
45. As an AI agent, I want cash balances in a separate `cash` section of `holdings`, so that I can report total wealth.
46. As an AI agent, I want holding totals in the base currency (or `--currency`), with each row also giving its native amount, so that I can report both.
47. As an AI agent, I want `prices --instrument` with an optional date range, including the latest price and its date, so that I can discuss price history and stale prices.
48. As an AI agent, I want `performance` for the client, one investment account (optionally with its reference cash account), one cash account or one instrument, so that I can report IRR, TTWROR and gains at the scope the user asks about.
49. As an AI agent, I want `performance --by instrument` to return one record per instrument active in the period, with IRR, TTWROR, absolute gain, dividends, fees, taxes, opening value and closing value, so that I can rank performers.
50. As an AI agent, I want an instrument with no activity in the period to produce a `no-activity-in-period` warning rather than an error, so that one quiet instrument doesn't fail the whole query.
51. As an AI agent, I want periods given as `--from`/`--to` (with `--to` defaulting to today) or as a `--period` reporting-period code such as `L1Y0`, `YTD` or `Y2025`, with the resolved dates echoed, so that I always know which window I got.
52. As an AI agent, I want passing both `--period` and `--from`/`--to` to be a usage error, so that ambiguous requests fail fast.
53. As an AI agent, I want `--cost-method fifo|moving-average`, default FIFO and always echoed, so that gains are reproducible.
54. As an AI agent, I want `taxonomies` to list taxonomies and their classifications, so that I can find the taxonomy the user means.
55. As an AI agent, I want `allocation --taxonomy --date` as flat rows (classification id, name, parent path, weight, value, share) that add up to 100% including an "unassigned" row, so that I can present an allocation without rebuilding the tree.

### Output conventions

56. As an AI agent, I want every response in the `{data, meta, warnings}` envelope, so that I parse one shape everywhere.
57. As an AI agent, I want `meta` to carry the file version, as-of date, reporting currency, resolved period and cost method, so that I can state my assumptions to the user.
58. As an AI agent, I want warnings for stale prices and for exchange rates that fell back to the built-in ECB data, so that I can qualify my figures.
59. As an AI agent, I want amounts as decimals with an ISO currency, dates in ISO 8601, and rates as unrounded fractions, so that I never decode internal scaled integers.
60. As an AI agent, I want JSON only on stdout, with logs and warnings on stderr, so that core logging never corrupts my parse.
61. As an AI agent, I want structured (problem+json style) errors with a stable `code` and a non-zero exit code, so that I can branch on failures.
62. As an AI agent, I want list commands paginated with `--limit` (default 200) and `--offset`, returning `total` and `nextOffset`, ordered by date then UUID (name then UUID for master data), so that large files don't flood my context.
63. As an AI agent, I want camelCase field names that match #5870 where an equivalent exists, so that what I know from the upstream API carries over.
64. As the owner, I want an optional `--format table` for human reading, so that I can use the tool myself (nice-to-have).

### Writing: the apply batch

65. As an AI agent, I want one `pp-cli apply --input <file|->` command that takes `{"operations": [...]}`, so that every change goes through one validated path.
66. As an AI agent, I want to add a transaction record of any supported type, with date, amount, currency, shares, instrument, accounts, units, note, source, `exDate` and `targetAmount` as applicable, so that I can record what the user tells me.
67. As an AI agent, I want to update a transaction record with a patch that is overlaid on the full record and then validated like an add, so that partial edits can never bypass validation.
68. As an AI agent, I want a patch that leaves fees, taxes or forex units inconsistent with a changed `shares` or `amount` refused with a field-level error, so that nothing is silently recomputed.
69. As an AI agent, I want changing a record's `type` on update to be refused, so that I delete and re-add instead of morphing a record.
70. As an AI agent, I want to delete a whole transaction record, both sides, by either side's UUID, so that I never leave a half-transaction behind.
71. As an AI agent, I want to add and update instruments (name, currency, ISIN, ticker, WKN, note, retired), so that I can register a new holding before recording a buy.
72. As an AI agent, I want to add and update cash accounts (name, currency, note, retired), so that I can model a new bank account.
73. As an AI agent, I want to add and update investment accounts (name, reference cash account, note, retired), so that I can model a new depot.
74. As an AI agent, I want to replace one instrument's complete assignment within one existing taxonomy, so that I can classify instruments.
75. As an AI agent, I want an empty taxonomy assignment list to remove the instrument from that taxonomy, so that I can unclassify it.
76. As an AI agent, I want the batch to be atomic, so that a failure in operation 7 doesn't leave operations 1 to 6 applied.
77. As an AI agent, I want operations applied in order against the evolving model, so that the duplicate checks see earlier adds in the same batch.
78. As an AI agent, I want two operations on the same existing entity in one batch refused, so that the outcome never depends on ordering subtleties.
79. As an AI agent, I want every reference in a write to be a UUID that existed before the batch, so that writes are unambiguous. I accept that a new entity's UUID comes back in the response for use in my next call.
80. As an AI agent, I want every validation failure in the batch reported at once, each with `operationIndex`, a `field` path, a `code`, a message and the core check that raised it, so that I can fix everything in one retry.
81. As an AI agent, I want a successful batch to return one result per operation, each with `created`, `updated` and `deleted` change records (entity type, UUIDs, before and after for changed fields), so that I can confirm the change to the user.

### Writing: validation rules

82. As the owner, I want core's own import checks (transaction date, security-related values, currencies, forex gross value, valid types) applied to every write, so that the CLI enforces the same rules as the app's importers.
83. As the owner, I want amounts, shares and exchange rates required to be positive, so that sign errors are caught.
84. As the owner, I want an instrument required, refused or optional exactly according to the transaction type, and `shares` accepted only on share-carrying types, so that records stay well formed.
85. As the owner, I want the cash side to use the cash account's currency, and an instrument in another currency to require a forex gross-value unit with a rate, so that multi-currency records stay consistent.
86. As the owner, I want cross-currency cash transfers supported with `amount` in the source currency and `targetAmount` in the target currency, with the rate derived and echoed and any supplied rate checked within core's rounding tolerance, so that FX transfers are recorded correctly.
87. As the owner, I want forex buys and sells to follow the same pattern, so that foreign-currency purchases are recorded correctly.
88. As the owner, I want `exDate` accepted only on dividends with an instrument, and never after the payment date, so that dividend data stays sane.
89. As the owner, I want any write refused if it makes an investment account's holding of an instrument negative at the end of any date, including edits and deletes that remove an earlier buy, so that overselling is impossible.
90. As the owner, I want an invalid ISIN or a duplicate ISIN refused, so that instrument identity stays clean.
91. As the owner, I want an instrument's currency, and a cash account's currency, to be immutable once there are transactions, so that historical amounts keep their meaning.
92. As the owner, I want accounts to require a name and currency, and investment accounts to require an existing reference cash account, so that the model stays complete.
93. As the owner, I want taxonomy weights to be positive, not repeated, at most 100% in total, and on leaf classifications only, so that classifications stay meaningful.
94. As the owner, I want `grossAmount` treated as a cross-check only and refused if it doesn't reconcile, so that `amount` stays authoritative.

### Writing: safety

95. As the owner, I want writes applied by default and previewed only with `--dry-run`, so that an agent that forgets the flag still does what I asked.
96. As an AI agent, I want `--dry-run` to return the same entity-level change records and say whether and where a backup would be written, without creating any file, backup or lock, so that I can show the user exactly what will happen.
97. As the owner, I want a timestamped backup in a `backups/` folder next to the client file before every real write, so that every agent change can be undone.
98. As the owner, I want `--backup-dir` to override the backup location, so that I can keep backups elsewhere.
99. As the owner, I want only the newest 20 CLI backups kept by default, pruned only after a successful write, with `--keep` to change this (0 means unlimited), so that backups don't pile up.
100. As the owner, I want the CLI never to touch the desktop app's own `.backup` file or to read its backup preferences, so that the CLI and the app don't interfere.
101. As the owner, I want the file written to a temp file and then atomically renamed, falling back to a plain replace only when the filesystem refuses an atomic move, so that a crash never leaves a half-written client file.
102. As the owner, I want core's consistency checker run before and after the change on scratch copies, with the write refused if it introduces a new issue, so that the CLI never makes my file less consistent. Issues that were already there are reported as warnings.
103. As the owner, I want the file's size, mtime and content hash re-checked just before the rename, with the write aborted on any change, so that a save from the desktop app in the meantime isn't overwritten.
104. As the owner, I want a best-effort open-file detection that warns, or refuses unless `--force` is given, so that I'm reminded to close the file in the app.
105. As the owner, I want concurrent CLI writers prevented by an exclusive lock file holding the PID, with a second writer failing fast with a structured error, so that two agents can't clobber each other.
106. As the owner, I want reads never to take the lock, so that queries work while a write is in progress or the app has the file open.
107. As an AI agent, I want the backup path and the before and after file versions in `meta` after a write, so that I can tell the user how to undo it.
108. As the owner, I want `pp-cli restore --backup <path>` to restore a backup through the same safe-write sequence, including a backup of the current file, so that undoing an agent change is itself undoable.

## Implementation Decisions

### Module and architecture

- A new, additive Java 21 module in this fork, built by the existing Maven/Tycho reactor. It never modifies existing core files, so merges from `upstream` stay conflict-free (ADR 0002).
- It depends on the core bundle `name.abuchen.portfolio` and runs it on a **plain classpath**: the built core bundle jar (which carries the `ServiceLoader` service files) plus the target-platform jars. There is no OSGi launch, no `IApplication`, no workbench and no SWT display, though the SWT, equinox.common, core.runtime and osgi jars must be on the classpath. The pdfbox bundles are excluded, so there is no PDF import.
- One JVM per command, no daemon. No code dependency on PR #5870. The external vocabulary (*instrument*, *investment account*, *cash account*, *holding*, *transaction record*) and the camelCase JSON shapes follow #5870 where cheap, with no compatibility promise. Internal code keeps the core terms (Security, Portfolio, Account).
- The CLI module handles the core caveats itself:
  - It redirects `PortfolioLog`'s stdout output to stderr.
  - It uses the offline ECB rates; a fallback to the built-in 2015 data is reported as a warning.
  - It doesn't use online feeds, the login token store or persisted ECB caches.

### Command layer: a single in-process entry point

- All behavior goes through one in-process entry point that takes arguments, environment, stdin, stdout, stderr and a clock, and returns an exit code. The launcher's `main` is a thin adapter over it. This entry point is also the primary test seam (see Testing Decisions).
- Commands and their options, write operations and response schemas are declared once. Those declarations drive argument parsing, dispatch and `describe` output, so `describe` can't drift.
- Invocation shape: `pp-cli <resource> <verb-or-options> --file <path>`, with `PP_FILE` as the fallback. Passwords come from `PP_PASSWORD` or a stdin prompt, never a flag.

### Read surface

- Commands:
  - `info`: file version, `maxSupportedVersion`, detected `format`, base currency, counts, pinned upstream tag.
  - `instruments`, `investment-accounts`, `cash-accounts`.
  - `transactions`, with filters for account, instrument, date range and type.
  - `holdings --date`, with `--investment-account`, `--by-account`, `--include-zero` and a `cash` section.
  - `prices --instrument`, with an optional date range.
  - `performance`, at client, investment account (optionally with its reference cash account), cash account or instrument scope, with `--by instrument`.
  - `taxonomies`.
  - `allocation --taxonomy --date`.
  - `describe`.
- Core building blocks: the client snapshot (holdings and grouping by taxonomy), the performance index (TTWROR, IRR), the lazy per-security performance snapshot (per-instrument records), reporting-period parsing from a code string, and the exchange-rate provider factory with the currency converter.
- Transaction records: a buy or sell joins its investment-account and cash-account sides. A transfer carries its source, target and a `kind`. The record `id` is the investment-account side for buys and sells, the outgoing side for transfers, and the transaction itself otherwise. Every side's UUID is listed.
- Reference resolution: the module adds its own resolver, because core has none. It accepts a UUID (canonical) or an ISIN, ticker or exact name. It never guesses: an ambiguous match is an error that lists the candidates. Name, ISIN and ticker lookups are allowed only on reads.
- Currency rules:
  - Records are in their native currency, and `--currency` adds converted fields.
  - Aggregates are in the base currency or `--currency`, and holding rows also carry the native amount.
  - Rate date: the transaction date (or the transaction's own forex rate) for transactions, the as-of date for holdings and allocation, and the engine's own behavior for performance.
  - Every converted amount carries the rate and rate date used.
- Periods: `--from`/`--to` (`--to` defaults to today from the injected clock) or `--period <code>`. Passing both is a usage error, and the resolved dates are always echoed.
- Cost method: `--cost-method fifo|moving-average`, default FIFO, always echoed in `meta`.
- Pagination (shared by every list command):
  - `--limit` (default 200, integer 1–1000) and `--offset` (integer ≥ 0). Any other value is a usage error `invalid-option-value` (exit 2) with `option` and `value` extensions.
  - Response: `data = {items, total, nextOffset}`. `nextOffset` is omitted on the last page. An offset past the end returns empty `items` with the correct `total`. `meta` echoes the resolved `limit` and `offset`.
  - Ordering: date then UUID for dated records (transactions); name (case-insensitive, ordinal, not locale-collated) then UUID for master data.
- Master-data fields: unset optional fields are omitted, never `null`. A null or blank string counts as unset; values that are present are emitted untrimmed. `name` is the exception: it is always present, and a null name becomes `""`. `retired` is always present as a boolean. Instrument attributes are an always-present array of `{id, name, type, value}`, with values encoded as upstream #5870's `AttributeCodec` does (details in ticket 15). Attributes on cash and investment accounts are deferred. The two deviations from upstream (attribute array, paging fields in `data`) are recorded in ADR 0004.
- Write inputs use the same field names as the reads (`currencyCode`, `tickerSymbol`, `referenceCashAccount`, ...), so an agent can send back what it read.
- The module owns all JSON mapping, because core has no model serializer:
  - Amounts are decimals with an ISO currency.
  - Dates are ISO 8601.
  - Rates are unrounded fractions.
  - Fields are camelCase.
- Deferred, not in v1: classification-level performance, `income`, matched trades.

### Write surface

- One command: `pp-cli apply --input <file|->`, JSON only, shaped `{"operations": [...]}`. There are no per-resource write commands.
- Operations:
  - `transaction.add`, `transaction.update`, `transaction.delete`.
  - `instrument.add`, `instrument.update`.
  - `cash-account.add`, `cash-account.update`.
  - `investment-account.add`, `investment-account.update`.
  - `taxonomy.assign`.
- Batch semantics:
  - The batch is atomic.
  - Operations are applied in order against the evolving model.
  - There are no caller-supplied IDs and no in-batch references.
  - Two operations on the same existing entity in one batch are refused.
  - All validation errors are collected and reported together.
- Record construction goes through core's entry classes, so both sides of a buy, sell or transfer are created, updated and deleted together.
- Update is read, merge, validate, write: the patch is overlaid on the full record, then the full `add` validation runs. Nothing is recomputed silently.
- Validation layers:
  1. Core's non-mutating import checks: transaction date, security-related values, currencies, forex gross value, valid types.
  2. CLI rules:
     - Amount, shares and exchange rate must be positive.
     - The instrument is required, refused or optional by transaction type, and shares are allowed only on share-carrying types.
     - Currency and forex rules, including cross-currency transfers and forex buys and sells within core's rounding tolerance.
     - `exDate` rules.
     - The `grossAmount` cross-check.
     - No negative holding at the end of any date, per investment account and instrument.
     - ISIN format and uniqueness.
     - Currency immutability once an instrument or cash account has transactions.
     - Accounts need a name and currency; an investment account needs an existing reference cash account.
     - Taxonomy weights: positive, unique classifications, at most 100% in total, leaf classifications only.
  3. Core's consistency checker, using the no-new-issues rule (see Safe-write sequence).
- Error contract: nothing is written, the exit code is non-zero, and the response lists every failure. Each failure carries `operationIndex`, a `field` path, a `code`, a message and, where relevant, the core check that raised it.
- Success contract: `data.results[]` has one entry per operation, with `created`, `updated` and `deleted` change records (entity type, UUIDs, before and after for changed fields). `meta` carries the backup path, `fileVersionBefore` and `fileVersionAfter`.
- Excluded from v1:
  - Price writes.
  - Deleting instruments or accounts (core's removal cascades into transactions).
  - Creating taxonomies or classifications.
  - Watchlists, investment plans, attributes, events and feed settings.
  - Assigning cash accounts to taxonomies.

### Safe-write sequence

`apply` and `restore` share one sequence:

1. Version guard.
2. Take the lock.
3. Load the file and record its size, mtime and hash.
4. Apply the operations.
5. Run the consistency checks.
6. Back up the original file.
7. Write the temp file.
8. Re-check the size, mtime and hash.
9. Atomically rename.
10. Prune old backups.

`--dry-run` stops before any side effect. Details:

- **Lock**: an exclusive lock file named `.<name>.pp-cli.lock`, created atomically and holding the PID. A second writer fails fast. Reads never lock.
- **Consistency check**: core's consistency checker runs before and after the change, each time on a scratch copy, because eight of its checks repair the model as they run. The before pass uses a fresh load of the original file; the after pass uses a reload of the temp file. The write is refused if the change introduces any issue (compared by check and entity) that wasn't already there. Pre-existing issues become warnings.
- **Backup**: saved as `backups/<name>.<yyyyMMdd-HHmmss>.pp-cli<ext>` next to the client file (`--backup-dir` overrides). The newest 20 are kept (`--keep n`, where 0 means unlimited), pruned only after a successful write. The app's `.backup` file and backup preferences are never touched.
- **Atomic write**: a temp file named `.<name>.pp-cli-tmp` in the same directory, saved with the file's existing format. Core's save ignores the target file name, so the format is preserved. The temp file is renamed with an atomic move, falling back to a plain replace only when the filesystem refuses an atomic move. The temp file is deleted on failure. Core's in-place write is bypassed without modifying core.
- **Open-in-app guard**: the size, mtime and hash re-check aborts the write on any mismatch. Best-effort `lsof` detection on macOS warns, or refuses unless `--force` is given. Unsaved edits inside the app can't be detected; this limit is documented and published in `describe`.
- **Restore**: `pp-cli restore --backup <path>` uses the same sequence, including a backup of the current file. There is no backup listing command in v1.

### Formats, versions and passwords

- Reads cover all seven formats through core's loader. Writes preserve the existing format; there is no conversion and no write-as option.
- **Version guard on every load, for every command**: if the file version after read is greater than the core's current version (70 at the pinned tag), the command is refused with `file-version-unsupported`, `fileVersion` and `maxSupportedVersion`. On writes this happens before any lock, backup or temp file. Both values are public in core, so no core patch is needed. This closes core's gap for unencrypted binary files.
- Older files are migrated and saved at the current version, with a migration warning and before and after versions in `meta`.
- The CLI detects the format itself from the file extension and magic bytes, because core's save-flags accessor is package-private. The normalized `format` values are `xml`, `xml-zip`, `binary`, `xml-aes256` and so on.
- Encrypted files: the password is consumed once at load, and the key is reused on save. Failures map to `password-required` (no password given) or `incorrect-password` (a password was given and decryption failed). Neither depends on core's localized message, and the password is never echoed, logged or shown in a trace.
- Upstream tracking: the core is pinned to a named upstream release tag, reported by `info` and `describe`. The supported contract is that the CLI's core matches the installed desktop release. There are no backward-compatible writes to older file versions.

### Packaging and distribution (ADR 0003)

- The build produces a self-contained bundle per platform: the fork's core jar plus the target-platform jars, collected by Tycho `list-dependencies` and placed on a flat classpath; a bundled jlink runtime; and a thin launcher. SWT's per-platform fragment makes each OS and architecture a separate artifact.
- Targets: macOS arm64 and Windows x64, each built locally and natively on its own machine. There is no CI build, signing or notarization.
- Launchers: a POSIX `sh` script `pp-cli` on macOS and a `pp-cli.cmd` batch wrapper on Windows. Each runs the bundled runtime with the fixed classpath and the AppCDS archive.
- One idempotent install and update script per platform:
  - macOS: installs to `~/.pp-cli/` and symlinks into `/opt/homebrew/bin` if present, otherwise appends to `~/.zprofile` and `~/.zshenv`.
  - Windows: installs to `%LOCALAPPDATA%\pp-cli\` and adds that directory to the user `PATH`.
  - Both generate a dynamic AppCDS archive at install time.
  - Updating means re-running the script after bumping the pinned tag.
- Per-platform build, install and launch documentation ships with the module as a required v1 deliverable.

## Testing Decisions

### What makes a good test here

- Tests exercise external behavior only, in the same way an agent does: command arguments, environment and stdin in; stdout JSON, stderr, exit code and files on disk out. They don't assert on internal classes, private helpers or the shape of intermediate objects.
- Expected calculated figures (holdings values, IRR, TTWROR, gains, converted amounts) are cross-checked against core's own snapshot and performance APIs run on the same fixture, not hand-computed. The CLI's job is to expose the engine faithfully, not to recompute it.
- Each test builds or copies its own fixture client file into a temporary directory, so tests are independent and never touch shared sample files in place.
- Time-dependent behavior (the `--to` default, backup timestamps, pruning order) uses the injected clock, so results are deterministic.

### Seam 1 (primary): the in-process CLI entry point

The single seam for all behavior. Tests call the entry point with arguments, an environment map, stdin, captured stdout and stderr, and a fixed clock, then assert on:

- the envelope (`data`, `meta`, `warnings`), problem+json errors and exit codes;
- the client file after the command (reloaded through core and compared at the domain level, format preserved, version as expected);
- the `backups/` folder (naming, retention, pruning only after success);
- the absence of lock and temp files after success, failure and dry-run.

Coverage through this seam:

- Every read command, with its filters, pagination, period handling, currency conversion, cost method and warnings.
- Reference resolution, including ambiguity errors that list the candidates.
- Every `apply` operation, its validation rules and the all-errors-at-once reporting.
- Batch atomicity.
- Dry-run behavior.
- The consistency check's no-new-issues rule.
- Overselling by end-of-date holdings.
- Backup and restore.
- Lock contention: a pre-created lock file makes the next writer fail fast.
- The version guard on every command, including a newer-than-supported binary file, with no side effects on writes.
- Old-file migration warnings.
- All seven formats read, and written back in the same format.
- `password-required` and `incorrect-password`, with the password never in any output.
- `describe` consistency: every declared command and operation appears, and its schema matches real responses.

The few failure modes that can't be reached through arguments and files:

- a filesystem that refuses an atomic move;
- the file changing between load and rename;
- `lsof` reporting the file as open.

These are covered with a narrow injectable file-operations and process-probe hook on the same entry point, not with a new lower-level seam.

### Seam 2 (smoke only): the installed launcher

After a local build and install on each platform, a scripted smoke check runs the installed `pp-cli` from a fresh shell (`info` and `describe` against a sample file). It confirms the bundled runtime, the flat classpath (no pdfbox, the correct SWT fragment), the AppCDS archive and `PATH` wiring. This is a release-time check documented in the per-platform instructions, not part of the Maven test run.

### Prior art in the codebase

- The core test bundle already loads XML, binary and encrypted client files headlessly through the client factory, and computes snapshots and performance outside the workbench. The scenario tests, the historic file-version tests and the protobuf writer test are the closest models, and the historic sample files can serve as format and migration fixtures.
- The shared JUnit bundle's builders for securities, portfolios, accounts and taxonomies, plus its test currency converter, are the way to build small, purpose-specific fixtures, which are then saved through the client factory in the format under test.
- The trade-collector and issue-regression tests show how core figures are asserted, and can serve as reference values when cross-checking CLI output.

## Out of Scope

- **MCP server** over the CLI. Ruled out of v1 (not deferred with a recorded shape); a future effort would start the design from scratch.
- **Live sync with, or live reads from, the running desktop app**. A possible later add-on if only #5870 merges upstream.
- **AI skills or agent prompt packs** that teach agents to use `pp-cli`. To be written later, possibly in another repository.
- **PDF statement import** (pdfbox is excluded from the flat classpath).
- **Price writes**: historical or latest prices, and online feed configuration.
- **Deleting instruments or accounts**; creating taxonomies or classifications; watchlists, investment plans, attributes, events; assigning cash accounts to taxonomies.
- **Format conversion** or writing older file versions for compatibility.
- **Read extras deferred from v1**: classification-level performance, `income`, matched trades, a backup listing command.
- **CI builds, code signing, notarization**, and platforms other than macOS arm64 and Windows x64.
- **An XML round-trip integrity check** before commit (a possible follow-up enhancement).
- **Upstream contribution**. Allowed but not a goal; upstream acceptance doesn't drive any decision here.

## Further Notes

- Audience is personal use only. If upstream ships a full REST API with writes to the file, this project is archived. If only #5870 merges (it did on 2026-10-10, with instrument-only, in-memory writes), work continues as specified.
- The main operational risk is a version mismatch between the CLI's pinned core and the installed desktop app. An older CLI core is caught by the version guard; a newer one migrates files and locks out an older desktop app, which the migration warning surfaces. The per-platform docs must state that the CLI and the desktop release need to match.
- Measured startup is about 0.5 s per call, or about 0.37 s with AppCDS. Agent workflows should prefer batching writes into one `apply` and paginated reads over many small calls.
- Unsaved edits inside a running desktop app can't be detected. The hash and mtime re-check only protects against a save made between the CLI's load and its rename. This limit has to be visible in `describe` and the docs.
- Decision history and rejected alternatives for every point above are in the resolved tickets under [issues/](issues/).
