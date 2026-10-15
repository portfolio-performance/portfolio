# Which client file formats can core read and write headlessly, and what does tracking upstream format changes cost?

Type: research
Status: resolved

## Question

For each client file format core supports (plain XML, compressed XML, binary/protobuf, encrypted in its AES variants), can `ClientFactory` load and save it headlessly on the plain classpath from ticket 01, with the format preserved on save? How does core detect the format and file version, where is the supported-version ceiling defined, and how does it react to a newer-than-supported file? How are encrypted-file passwords supplied to core, and what happens on a wrong password? Finally, how often has upstream changed the file format or version over the last few years (from git history of `ClientFactory`, the protobuf schema and the version constant), as input for the cost of keeping this fork current?

## Comments

Research findings: branch `research/format-support-facts`, file `.scratch/ai-cli/research/format-support-facts.md`.

## Answer

- Seven file types (XML, XML with ID references, XML in ZIP, binary protobuf in ZIP, XML+AES-256, XML+AES-128, binary+AES-256). `ClientFactory.load(File, char[], IProgressMonitor)` reads all; `ClientFactory.save(Client, File)` writes each back in its own format. All headless; only `Platform.getOS()` touches Eclipse.
- Format is detected from extension plus magic bytes and kept as save flags on `Client`. `save` ignores the target file name, so temp file plus rename preserves the format. Encrypted files re-save with the key cached at load, so saving needs no password.
- `getSaveFlags()` and `getVersion()` are package-private; the CLI cannot read format or version through the public API.
- Ceiling: `Client.CURRENT_VERSION = 70`. Newer XML and encrypted files are refused with an `IOException`. **Gap:** unencrypted binary files are never checked; a newer one loads silently and may lose data on save. The CLI needs its own `getFileVersionAfterRead() > CURRENT_VERSION` guard.
- Older files are migrated in memory and saved at version 70, after which an older desktop app can no longer open them.
- Passwords go in as `char[]`; wrong or missing password is a plain `IOException` with a localized message (`Messages.MsgIncorrectPassword`), no dedicated type. Encrypted writes work with JDK crypto only.
- Upstream churn: 17 version bumps in 5 years (53 to 70, about 3.4 a year, in bursts); `client.proto` changed in 9 commits since 2021-10, some without a bump. The ZIP and encryption container has not changed since 2014.
- Implication: merging upstream carries format changes without conflict. The real constraint is matching the CLI's core to the user's installed desktop release: older core risks data loss on binary files, newer core locks the desktop app out of the saved file.
