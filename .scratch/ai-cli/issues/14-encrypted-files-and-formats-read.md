# 14: Encrypted client files and all seven formats on read

Type: task
Status: ready-for-agent
Blocked by: 12

Spec: [spec.md](../spec.md), user stories 17–21 and "Formats, versions and passwords".

## What to build

Every command can read every client file format core supports: XML, XML with ID references, XML in ZIP, binary, XML AES-256, XML AES-128 and binary AES-256. For encrypted files the password comes only from `PP_PASSWORD` or, if that isn't set, an interactive prompt on stdin. There is no password flag. A missing password gives `password-required` and a wrong one gives `incorrect-password`. The password never appears on stdout or stderr, in logs or in stack traces.

`info` reports the normalized `format` for every format. The version guard also applies to encrypted files.

## Acceptance criteria

- [x] `info` succeeds on fixtures in all seven formats and reports the correct normalized `format` for each
- [x] An encrypted file opens with `PP_PASSWORD` and with a password supplied on stdin
- [x] An encrypted file with no password gives `password-required`; a wrong password gives `incorrect-password`; both have non-zero exit codes
- [x] Tests assert that the password string never appears in stdout or stderr on success or failure
- [x] No command declares a password option, and `describe` documents `PP_PASSWORD`
- [x] An encrypted file newer than supported is refused with `file-version-unsupported`

_Notes for later tickets: the normalized formats are `xml`, `xml-id-references`, `xml-zip`, `binary`, `xml-aes256`, `xml-aes128`, `binary-aes256` (plus `binary-aes128`, which core can also write). The CLI decrypts the header itself (`EncryptedHeader`) to report `incorrect-password`, `file-version-unsupported` and the format without matching core's localized messages. `ClientLoader.load` zeroes the password after loading; core keeps only the derived key for saving. Core treats an encrypted header more than 20 versions ahead as a wrong password, so that is what the CLI reports too. `describe` now lists `environment` (PP_FILE, PP_PASSWORD). `CommandContext` carries stderr for the password prompt. Known limits from review: the stdin prompt does not disable terminal echo (stdin is injected, so there is no console handle); an empty password is treated as not given._
