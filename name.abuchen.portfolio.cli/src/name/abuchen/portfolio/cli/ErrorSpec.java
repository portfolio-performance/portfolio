package name.abuchen.portfolio.cli;

import java.util.List;

/**
 * Declares an error code the tool can fail with; <code>describe</code>
 * publishes it. Every code passed to {@link CliException} must be listed in
 * {@link #ALL}.
 */
record ErrorSpec(String code, String description)
{
    static final List<ErrorSpec> ALL = List.of(
                    // invocation
                    new ErrorSpec("command-required", "No command was given."),
                    new ErrorSpec("unknown-command", "The command does not exist; the detail lists the known commands."),
                    new ErrorSpec("unexpected-argument", "An argument is not an option; options look like --name value."),
                    new ErrorSpec("unknown-option", "The command has no such option."),
                    new ErrorSpec("option-value-required", "An option that takes a value was given without one."),
                    new ErrorSpec("option-required", "A required option is missing, or an option needs another option."),
                    new ErrorSpec("invalid-option-value",
                                    "An option value is not accepted; the detail states the expected value."),
                    new ErrorSpec("conflicting-options", "Options were combined that can't be used together."),
                    new ErrorSpec("invalid-date-range", "The start of the period is after its end."),
                    new ErrorSpec("period-required", "performance needs --from or --period."),

                    // client file
                    new ErrorSpec("file-required", "No client file was given: pass --file or set PP_FILE."),
                    new ErrorSpec("file-not-found", "There is no client file at the given path."),
                    new ErrorSpec("file-unreadable", "The client file exists but could not be read or parsed."),
                    new ErrorSpec("file-version-unsupported",
                                    "The client file is newer than this CLI supports (fileVersion > maxSupportedVersion),"
                                                    + " usually because the desktop app is newer than the CLI's core."
                                                    + " Rebuild the CLI from the matching upstream tag; nothing was read or written."),
                    new ErrorSpec("password-required",
                                    "The client file is encrypted and no password was given: set PP_PASSWORD."),
                    new ErrorSpec("incorrect-password", "The client file could not be decrypted with the given password."),

                    // references
                    new ErrorSpec("reference-not-found", "No entity of the expected kind matches the reference."),
                    new ErrorSpec("reference-ambiguous",
                                    "The reference matches several entities; candidates lists their UUIDs."),
                    new ErrorSpec("no-reference-cash-account",
                                    "The investment account has no reference cash account to include."),

                    // writes
                    new ErrorSpec("input-unreadable", "The --input file could not be read."),
                    new ErrorSpec("invalid-input", "The --input batch is not valid JSON or not shaped as a batch."),
                    new ErrorSpec("validation-failed",
                                    "One or more operations of the batch failed validation; failures lists every one"
                                                    + " with its operationIndex, field and code. Nothing was written."),
                    new ErrorSpec("consistency-check-failed",
                                    "The change would introduce inconsistencies into the client file. Nothing was written."),
                    new ErrorSpec("file-open-in-app",
                                    "Another process, probably the Portfolio Performance app, has the client file open."
                                                    + " Nothing was written; close the file there first."),
                    new ErrorSpec("file-locked",
                                    "Another pp-cli is writing the client file, or crashed while doing so; the detail names the lock file."),
                    new ErrorSpec("file-changed",
                                    "The client file changed on disk after it was read. Nothing was written; run the command again."),
                    new ErrorSpec("write-failed", "The client file or its lock file could not be written. The file was not changed."),
                    new ErrorSpec("backup-not-found", "There is no backup file at the given path."),

                    new ErrorSpec("internal-error", "An unexpected failure inside the tool."));
}
