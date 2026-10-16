# 24: Write back in every format, and migrate older files

Type: task
Status: ready-for-agent
Blocked by: 14, 22

Spec: [spec.md](../spec.md), user stories 22–23 and 26, and "Formats, versions and passwords".

## What to build

A write keeps the client file in its existing format, for all seven formats, with no conversion and no write-as option. An encrypted file is written back with the key from load, so the password is supplied once per command. The backup is a copy of the original file, in the original format.

An older file is migrated and saved at core's current version. The result carries a migration warning saying that an older desktop app can no longer open it, and `meta` shows `fileVersionBefore` and `fileVersionAfter`.

## Acceptance criteria

- [ ] An `apply` against each of the seven formats leaves the file in the same format, verified by reloading through core and by format detection
- [ ] An encrypted file is written with the load-time password and reopens with it; the password is prompted at most once
- [ ] An older historic sample file is migrated on write, with a migration warning and correct `fileVersionBefore`/`fileVersionAfter`
- [ ] `--dry-run` on an older file reports the pending migration without writing anything
