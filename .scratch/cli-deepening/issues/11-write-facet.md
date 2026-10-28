# 11: `ClientFile.write` facet: apply and restore

Type: task
Status: resolved
Blocked by: 10

Spec: [spec.md](../spec.md), "Command facets". Decision: [ticket 02](02-command-facets.md), "Write".

## What to build

Turn the write scaffolding into one facet, and move `apply` and `restore` onto `CommandSpec.of`.

- **`ClientFile.write(dryRunDescription)`** owns:
  - `--dry-run` and `SafeWrite.OPTIONS`;
  - the write meta (`fileVersionBefore`, `fileVersionAfter`, `dryRun`, `backupPath`);
  - `SafeWrite.WARNINGS`;
  - the codes `consistency-check-failed`, `file-open-in-app`, `file-locked`, `file-changed` and `write-failed`.

  Its value `ClientFile.Writer` owns the open → run → replace → outcome cycle and forwards the outcome's warnings.
- **Local facets**: apply gets `BATCH` (`input-unreadable`, `invalid-input`, `validation-failed`) and restore gets `BACKUP` (`backup-not-found`).
- **Removed** from both commands: the duplicated `DRY_RUN`, `writeSchema` and warning forwarding, and their `ResponseMeta.written` calls.
- `FacetHarness` tests for `ClientFile.write`, `BATCH` and `BACKUP`.

## Acceptance criteria

- [ ] `ApplyCommand` and `RestoreCommand` declare `ClientFile.write(...)` and their local facet, and nothing else of the write scaffolding
- [ ] Dry run, a real write, the backup path, and each write failure code still produce the same envelope (existing `SafeWriteTest` runs)
- [ ] Golden help unchanged, or each difference is a meta-order line called out in the commit
- [ ] CLI suite green

## Tests deleted

- Cases in `ApplyCommandTest` and `RestoreCommandTest` that only re-check the write meta members or the forwarded `SafeWrite` warnings, now covered by the `ClientFile.write` facet test. `SafeWriteTest` stays: it tests the safe-write sequence, not the facet.

## Answer

Implemented on integration branch `feat/cli-deepening`, merge commit f098c095d. ClientFile.write facet with Writer; apply and restore on facets.
