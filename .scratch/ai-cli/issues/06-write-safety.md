# How do backup and dry-run behave on writes?

Type: grilling
Status: resolved
Blocked by: 05

## Question

What exactly happens around a write: where and how backups are named and retained, what a dry-run reports (structured diff of domain changes vs textual file diff), and whether writes are atomic (temp file + rename)?

## Answer

Every write command (and `restore`) follows one safe-write sequence: take the lock, load, apply the operation, run the consistency check, back up the original, write a temp file, re-check the file is unchanged, atomically rename. `--dry-run` stops before any side effect.

- **Backup**: before every real write, copy the original to `backups/<name>.<yyyyMMdd-HHmmss>.pp-cli<ext>` next to the client file; `--backup-dir` overrides. The CLI ignores the app's backup preferences and never touches the app's `.backup` files.
- **Retention**: keep the newest 20 CLI-created backups (matched by the `.pp-cli` name pattern), pruned only after a successful write; `--keep <n>` overrides, `0` means unlimited.
- **Atomic write**: write to a temp file in the same directory (`.<name>.pp-cli-tmp`) with the file's existing save flags, then rename with `ATOMIC_MOVE`; fall back to a plain replace-move only if the filesystem refuses an atomic move. Delete the temp file on failure.
- **Verification**: before committing, run core's `Checker` on the client before and after the operation; refuse the write if the operation introduces an issue that was not already there. Pre-existing issues are warnings. Ticket 05 references this check instead of redefining it.
- **Open-in-app guard**: record size, mtime and content hash at load and re-check immediately before the rename; a mismatch aborts the write. Best-effort `lsof` detection (macOS/Linux) warns, or refuses unless `--force`. Unsaved edits inside the app are undetectable; the docs and `describe` state this limit.
- **Concurrent CLI runs**: writes take an exclusive lock file `.<name>.pp-cli.lock` (created atomically, holds the PID so a stale lock can be cleared deliberately); a second writer fails fast with a structured error. Reads never lock.
- **Dry-run**: opt-in `--dry-run`; writes nothing (no file, backup or lock). Returns the usual JSON envelope with entity-level changes (`created`, `updated`, `deleted`, each with UUID, type and before/after for updated fields) and whether and where a backup would be written. No textual XML diff. Each operation emits its own change records; the exact shapes per operation are settled with ticket 05.
- **File version**: refuse files newer than the CLI supports. Older files are migrated and saved at the current version; `fileVersionBefore` and `fileVersionAfter` go in `meta`, with a warning when they differ.
- **Format**: writes keep the file's existing format and never convert. Unsupported formats (for example encrypted without a password) refuse the write with a clear error, never falling back to XML.
- **Restore**: `pp-cli restore --backup <path>` takes an explicit backup path and goes through the same safe-write sequence (including a backup of the current file). No `backups list` in v1.

### How it was resolved

Grilled 2026-10-09; the user accepted each recommendation except where noted.

| Decision | Reasoning |
| --- | --- |
| Dedicated `backups/` folder with timestamped names | The app's own backup is one `Files.copy` with `REPLACE_EXISTING` to a single `.backup` slot, so a second agent write would destroy the only pre-agent state. |
| Do not read the app's backup preferences | They live in the app's preference store, which a headless CLI does not load. |
| Temp file plus atomic rename | Core's `ClientFactory.writeFile` truncates and rewrites the target in place via `FileOutputStream`, so a crash mid-write corrupts the file; it also skips the file lock on macOS. The CLI routes around it without modifying core. |
| Hash/mtime check plus `lsof` as the open-in-app guard | The app writes no lock marker and does not watch the file's mtime, so there is no ready-made signal. |
| Writes applied by default, `--dry-run` opt-in (user's answer) | An agent that forgets the flag still does what the user asked. |
| Only the `Checker` no-new-issues verification (user's answer, replacing the recommendation to also check XML round-trip) | Core has no model differ and no guarantee that XML serialization is stable across load and save; the round-trip check is a possible follow-up enhancement. The `Checker` check reuses core validation and needs no serialization assumptions. |
| Keep existing format, never convert | Converting is a separate concern from safe writes; see the format and version support item on the map. |
| Minimal `restore` by explicit path | The write response already returns the backup path; a human can browse the folder. |

Rejected options:

- **Apply only with `--apply` (dry-run by default):** the user prefers writes applied by default.
- **Textual XML diff in dry-run:** XML noise helps neither the agent nor the user.
- **Round-trip integrity check before commit:** deferred as a follow-up (see above).

Left to other tickets:

- **Per-operation change record shapes and validation rules:** write operations (05).
- **Which formats the CLI can write, and password handling for encrypted files:** format and version support mechanics.
