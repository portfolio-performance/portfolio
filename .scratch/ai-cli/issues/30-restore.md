# 30: `restore --backup`

Type: task
Status: ready-for-agent
Blocked by: 23

Spec: [spec.md](../spec.md), user story 108 and "Restore" under "Safe-write sequence".

## What to build

`pp-cli restore --backup <path>` rolls the client file back to a backup through the same safe-write sequence as `apply`: version guard, lock, consistency checks, a backup of the current file, temp file, re-check and atomic rename. Undoing an agent change can therefore itself be undone. `--dry-run`, `--backup-dir`, `--keep` and `--force` behave as they do for `apply`. There is no backup listing command in v1.

## Acceptance criteria

- [x] After `apply`, `restore` with the reported backup path brings the client file back to its original domain state
- [x] The restore itself creates a backup of the file it replaces, and `meta` reports that backup's path
- [x] A backup newer than supported, or a missing backup path, is refused with no side effects
- [x] A held lock, and a file changed before the rename, abort `restore` as they do `apply`
- [x] `--dry-run` reports what would happen and creates nothing
- [x] `restore` is declared, appears in `describe` and passes its consistency test
