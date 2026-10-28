package name.abuchen.portfolio.cli.command;

/**
 * An error code the tool can fail with; <code>--help</code> publishes it.
 * {@link CliException} takes only these constants, so the set of codes is
 * closed at compile time. The scope says which commands can return it, so that
 * a command's help lists only its own codes. No command is named here: a
 * {@link Scope#BY_USE} code belongs to the commands whose facets declare it or
 * whose option types imply it.
 */
public enum ErrorCode
{
    // invocation
    UNKNOWN_COMMAND("unknown-command", "The command does not exist; the detail lists the known commands.",
                    Scope.GLOBAL),
    UNEXPECTED_ARGUMENT("unexpected-argument", "An argument is not an option; options look like --name value.",
                    Scope.EVERY_COMMAND),
    UNKNOWN_OPTION("unknown-option", "The command has no such option.", Scope.EVERY_COMMAND),
    OPTION_VALUE_REQUIRED("option-value-required", "An option that takes a value was given without one.",
                    Scope.EVERY_COMMAND),
    OPTION_REQUIRED("option-required", "A required option is missing, or an option needs another option.",
                    Scope.BY_USE),
    INVALID_OPTION_VALUE("invalid-option-value",
                    "An option value is not accepted, including an unknown operation after apply --help; the detail states the expected value.",
                    Scope.EVERY_COMMAND),
    CONFLICTING_OPTIONS("conflicting-options", "Options were combined that can't be used together.", Scope.BY_USE),
    INVALID_DATE_RANGE("invalid-date-range", "The start of the period is after its end.", Scope.BY_USE),
    PERIOD_REQUIRED("period-required", "performance needs --from or --period.", Scope.BY_USE),

    // client file
    FILE_REQUIRED("file-required", "No client file was given: pass --file or set PP_FILE.", Scope.BY_USE),
    FILE_NOT_FOUND("file-not-found", "There is no client file at the given path.", Scope.BY_USE),
    FILE_UNREADABLE("file-unreadable", "The client file exists but could not be read or parsed.", Scope.BY_USE),
    FILE_VERSION_UNSUPPORTED("file-version-unsupported",
                    "The client file is newer than this CLI supports (fileVersion > maxSupportedVersion),"
                                    + " usually because the desktop app is newer than the CLI's core."
                                    + " Rebuild the CLI from the matching upstream tag.",
                    Scope.BY_USE),
    PASSWORD_REQUIRED("password-required",
                    "The client file is encrypted and no password was given: set PP_PASSWORD.", Scope.BY_USE),
    INCORRECT_PASSWORD("incorrect-password", "The client file could not be decrypted with the given password.",
                    Scope.BY_USE),

    // references
    REFERENCE_NOT_FOUND("reference-not-found", "No entity of the expected kind matches the reference.",
                    Scope.BY_USE),
    REFERENCE_AMBIGUOUS("reference-ambiguous",
                    "The reference matches several entities; candidates lists their UUIDs.", Scope.BY_USE),
    NO_REFERENCE_CASH_ACCOUNT("no-reference-cash-account",
                    "The investment account has no reference cash account to include.", Scope.BY_USE),

    // writes
    INPUT_UNREADABLE("input-unreadable", "The --input file could not be read.", Scope.BY_USE),
    INVALID_INPUT("invalid-input", "The --input batch is not valid JSON or not shaped as a batch.", Scope.BY_USE),
    VALIDATION_FAILED("validation-failed",
                    "One or more operations of the batch failed validation; failures lists every one"
                                    + " with its operationIndex, field and code. Nothing was written.",
                    Scope.BY_USE),
    CONSISTENCY_CHECK_FAILED("consistency-check-failed",
                    "The change would introduce inconsistencies into the client file. Nothing was written.",
                    Scope.BY_USE),
    FILE_OPEN_IN_APP("file-open-in-app",
                    "Another process, probably the Portfolio Performance app, has the client file open."
                                    + " Nothing was written; close the file there first.",
                    Scope.BY_USE),
    FILE_LOCKED("file-locked",
                    "Another pp-cli is writing the client file, or crashed while doing so; the detail names the lock file.",
                    Scope.BY_USE),
    FILE_CHANGED("file-changed",
                    "The client file changed on disk after it was read. Nothing was written; run the command again.",
                    Scope.BY_USE),
    WRITE_FAILED("write-failed", "The client file or its lock file could not be written. The file was not changed.",
                    Scope.BY_USE),
    BACKUP_NOT_FOUND("backup-not-found", "There is no backup file at the given path.", Scope.BY_USE),

    INTERNAL_ERROR("internal-error", "An unexpected failure inside the tool.", Scope.EVERY_COMMAND);

    /** Where an error code can occur. */
    public enum Scope
    {
        /** Only before a command is chosen; listed in the global help. */
        GLOBAL,
        /** Any command. */
        EVERY_COMMAND,
        /**
         * The commands that use what raises it: a facet that declares it, or
         * an option whose type implies it (see {@link CommandSpec#errorCodes()}).
         */
        BY_USE
    }

    private final String code;
    private final String description;
    private final Scope scope;

    ErrorCode(String code, String description, Scope scope)
    {
        this.code = code;
        this.description = description;
        this.scope = scope;
    }

    /** The published code, as it appears in a problem document. */
    public String code()
    {
        return code;
    }

    /** The meaning the global help lists. */
    public String description()
    {
        return description;
    }

    public Scope scope()
    {
        return scope;
    }
}
