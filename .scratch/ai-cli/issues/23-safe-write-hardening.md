# 23: Safe-write hardening

Type: task
Status: ready-for-agent
Blocked by: 22

Spec: [spec.md](../spec.md), user stories 98–104, "Safe-write sequence" and seam 1's injectable hook under "Testing Decisions".

## What to build

The full safe-write sequence that `apply` and, later, `restore` share:

- **Consistency check**: core's consistency checker runs before and after the change, each time on a scratch copy, because some of its checks repair the model as they run. The before pass uses a fresh load of the original; the after pass reloads the temp file. A write that introduces any new issue (compared by check and entity) is refused. Issues that were already there are reported as warnings.
- **Open-in-app guard**: the file's size, mtime and content hash are recorded at load and re-checked just before the rename. Any change aborts the write and deletes the temp file. On macOS a best-effort `lsof` probe warns, or refuses unless `--force` is given.
- **Atomic move fallback**: when the filesystem refuses an atomic move, the write falls back to a plain replace. It never falls back for any other failure.
- **Backups**: `--backup-dir` overrides the location. Only the newest 20 CLI backups are kept by default; `--keep n` changes this and `0` means unlimited. Pruning happens only after a successful write. The app's own `.backup` file and its backup preferences are never touched.
- **Test hook**: a narrow, injectable file-operations and process-probe hook on the same entry point lets tests simulate a filesystem that refuses atomic moves, the file changing before the rename, and `lsof` reporting the file open.

## Acceptance criteria

- [ ] A change that introduces a new consistency issue is refused; pre-existing issues show up as warnings and don't block the write
- [ ] The original client file is never modified by the checker's repairs
- [ ] A file changed between load and rename (simulated through the hook) aborts the write, leaves the original intact and removes the temp file
- [ ] A refused atomic move (simulated) falls back to a plain replace and the write succeeds
- [ ] A simulated open-file probe warns, or refuses without `--force`, and `--force` overrides
- [ ] `--backup-dir` and `--keep` work; with the injected clock, pruning removes the oldest CLI backups only after success and never touches other files
- [ ] No lock or temp file remains after any success, failure or dry-run
- [ ] New options are declared, appear in `describe` and pass its consistency test
