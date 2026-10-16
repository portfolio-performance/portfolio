# 22: `apply` walking skeleton with `instrument.add` and `instrument.update`

Type: task
Status: ready-for-agent
Blocked by: 15

Spec: [spec.md](../spec.md), user stories 65, 71, 76–81, 90–91, 95–97, 101, 105–107, and "Write surface" and "Safe-write sequence".

## What to build

The first complete write path. `pp-cli apply --input <file|->` takes `{"operations": [...]}` and supports `instrument.add` and `instrument.update` (name, currency, ISIN, ticker, WKN, note, retired). Input field names match the `instruments` read output: `name`, `currencyCode`, `isin`, `tickerSymbol`, `wkn`, `note`, `retired`. The batch contract:

- The batch is atomic, and operations apply in order against the evolving model.
- References must be UUIDs that existed before the batch. There are no caller-supplied IDs.
- Two operations on the same existing entity in one batch are refused.
- All validation failures are reported together. Each carries `operationIndex`, a `field` path, a `code`, a message and, where relevant, the core check that raised it.
- On success, `data.results[]` has one entry per operation with `created`, `updated` and `deleted` change records (entity type, UUIDs, before and after for changed fields). New UUIDs are returned.
- Validation rules for this ticket: ISIN format, ISIN uniqueness (including earlier adds in the same batch), and currency immutability once an instrument has transactions.

Writes are applied by default, through a minimal safe-write sequence:

1. Version guard, before anything else.
2. Exclusive lock file `.<name>.pp-cli.lock` holding the PID; a second writer fails fast with a structured error. Reads never lock.
3. Load the file.
4. Apply the operations.
5. Timestamped backup in `backups/` next to the client file.
6. Temp file `.<name>.pp-cli-tmp` in the same directory.
7. Atomic rename.

`meta` carries the backup path, `fileVersionBefore` and `fileVersionAfter`. `--dry-run` returns the same change records and says whether and where a backup would be written, without creating any file, backup or lock.

Ticket 23 hardens this sequence; ticket 24 extends it to every format and adds migration.

## Acceptance criteria

- [ ] Adding and updating instruments works, and the reloaded client file reflects exactly the reported change records
- [ ] An invalid or duplicate ISIN (including one added earlier in the batch) and a currency change on an instrument with transactions are refused with field-level errors
- [ ] A batch with several invalid operations reports every failure at once and leaves the file byte-identical
- [ ] A batch where a later operation fails applies nothing
- [ ] Two operations on the same existing instrument are refused
- [ ] A successful write leaves a correctly named backup in `backups/` and no lock or temp file; `meta` has the backup path and before and after versions
- [ ] `--dry-run` returns the same change records and the would-be backup path, and creates no file, backup or lock
- [ ] A pre-created lock file makes `apply` fail fast with a structured error, while reads still succeed
- [ ] A newer-than-supported file is refused before any lock, backup or temp file exists
- [ ] `apply` and both operations are declared (with input schemas), appear in `describe` and pass its consistency test
