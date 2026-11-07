# Which formats and file versions does v1 read and write, and how are encrypted files handled?

Type: grilling
Status: resolved
Blocked by: 07

## Question

Given the facts from ticket 07, which formats does v1 read and which does it write (plain XML is required; compressed, binary and encrypted are nice-to-have)? How does the CLI refuse a newer-than-supported file, and how does that refusal surface in the JSON error? For encrypted files, confirm or amend ticket 04's password input (`PP_PASSWORD` or stdin prompt, never a flag) for writes too, and decide the wrong-password error. What is the policy for keeping the fork in step with upstream format changes?

## Answer

The CLI reads and writes every format core supports, never converts, and preserves whatever the file already is. It adds one version-ceiling guard (core leaves a gap for binary files) and maps core's untyped failures to stable structured error codes.

### Formats

- **Read all seven** file types core loads headlessly: XML, XML with ID references, XML-in-ZIP, binary protobuf-in-ZIP, XML+AES-256, XML+AES-128, binary+AES-256. Reading comes for free from `ClientFactory.load`; refusing any would only reduce utility.
- **Write preservation-only.** Every write keeps the file's existing format and never converts (confirming ticket 06); there is no "write-as" option in v1. `save` writes each format back in place, so XML (the required case) plus compressed/binary/encrypted all work at no extra cost. Encrypted writes reuse the key cached at load, so the password is needed only once, at load.

### Version ceiling

- Core's `Client.CURRENT_VERSION` is `70`. Core refuses newer XML and encrypted files with an `IOException`, but **unencrypted binary protobuf has no ceiling check** (`ProtobufWriter.load` skips it) — a newer binary file loads silently and may lose data on save.
- The CLI closes this with **one guard on every load, for every command**: after load, if `Client.getFileVersionAfterRead() > Client.CURRENT_VERSION`, refuse. Both constants/getters are `public`, so no core patch is needed. `getVersion()` and `getSaveFlags()` are package-private and are not used.
- The refusal uses ticket 04's structured envelope: `code: "file-version-unsupported"`, detail fields `fileVersion` and `maxSupportedVersion` (70), a message explaining the desktop-version mismatch and pointing to upgrading the CLI, and a non-zero exit. On write commands it aborts **before** any lock, backup or temp file.
- Older files are still migrated in memory and saved at version 70, with `fileVersionBefore`/`fileVersionAfter` and a migration warning in `meta` (ticket 06).

### Passwords and encrypted files

- Writes use the **same input as reads**: `PP_PASSWORD` env var or an interactive stdin prompt, never a command-line flag. The password is consumed once at load; there is no separate write-side password handling.
- Core throws a generic `IOException` (localized `MsgIncorrectPassword`) with no dedicated type. The CLI detects "this file is encrypted" itself (extension + magic bytes) and maps the failure to two stable codes rather than parsing the localized string:
  - **`password-required`**: encrypted file, no `PP_PASSWORD` and no prompt answer.
  - **`incorrect-password`**: a password was supplied and decryption failed.
- Both exit non-zero, never echo or log the password, and emit no stack trace.

### Version/format metadata

- `info` reports the file version and additionally `maxSupportedVersion` (70) and a CLI-detected `format` (normalized value such as `xml`, `xml-zip`, `binary`, `xml-aes256`; detected from extension + magic bytes, since `getSaveFlags()` is unreadable).
- `describe` publishes the CLI's `maxSupportedVersion` and the **pinned upstream core version/tag**, so the match against the installed desktop app can be verified.
- `meta` keeps its existing `fileVersion`.

### Upstream-tracking policy

- Pin the fork's core to a **named upstream release tag**, recorded and reported via `info`/`describe`.
- The real risk is a version mismatch against the user's installed desktop app: older core risks data loss on binary files (closed by the guard above); newer core migrates files to 70 and locks an older desktop out. The CLI does **not** attempt backward-compatible writes to older versions.
- Rely on the version guard plus ticket 06's migration warning, and **document the version-match requirement** (keep the CLI's core aligned with your desktop release) as the supported contract.

### How it was resolved

Grilled 2026-10-16; the user accepted each recommendation. One fact was verified against the code: `Client.getFileVersionAfterRead()` and `Client.CURRENT_VERSION` are `public` (so the guard needs no core patch), while `getVersion()` and `getSaveFlags()` are package-private, and `ProtobufWriter.load` has no version ceiling check.

| Decision | Reasoning |
| --- | --- |
| Read all seven formats | `ClientFactory.load` reads them all headlessly; refusing any reduces utility for no gain. |
| Preservation-only writes, no conversion | Confirms ticket 06; `save` writes each format in place, so all formats work at no extra cost. |
| One version guard on every load | Core refuses newer XML/encrypted files but silently loads newer binary; the public getter closes the gap uniformly without a core patch. |
| `file-version-unsupported` with `fileVersion` + `maxSupportedVersion` | Reuses ticket 04's structured envelope; aborts writes before any side effect. |
| Same password input for writes as reads | The key is cached at load, so writes need nothing new; `PP_PASSWORD` or stdin prompt, never a flag. |
| Map to `password-required` vs `incorrect-password` | Core has no typed exception; the CLI knows the file is encrypted and whether a password was supplied, so it avoids scraping the localized message. |
| Expose version/format metadata in `info`/`describe` | Agents and users need to verify the file version, the CLI's ceiling, the detected format and the pinned upstream tag. |
| Pin to a named upstream tag; document version match | Matching the CLI's core to the installed desktop release is the real constraint; the CLI never writes older versions for compatibility. |

Rejected options:

- **Restricting reads or writes to a format subset:** core already handles all seven headlessly, so a subset only loses utility.
- **Format conversion / "write-as":** a separate concern from safe writes; preservation-only avoids surprising version or format changes.
- **Parsing core's localized `MsgIncorrectPassword` string:** fragile across locales; the CLI maps the failure from facts it already has.
- **Backward-compatible writes to older file versions:** core migrates forward only; attempting this would risk silent data loss.
