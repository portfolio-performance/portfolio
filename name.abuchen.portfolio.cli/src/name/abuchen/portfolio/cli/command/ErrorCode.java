package name.abuchen.portfolio.cli.command;

import java.util.List;

/**
 * An error code the tool can fail with; <code>--help</code> publishes it.
 * {@link CliException} takes only these constants, so the set of codes is
 * closed at compile time. The scope says which commands can return it, so that
 * a command's help lists only its own codes. Where a code follows from an
 * option, the scope derives from the declared options, so a new option can't
 * leave its codes out.
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
    // performance: --with-reference-cash-account needs --investment-account
    OPTION_REQUIRED("option-required", "A required option is missing, or an option needs another option.",
                    Scope.REQUIRED_OPTION, "performance"),
    INVALID_OPTION_VALUE("invalid-option-value",
                    "An option value is not accepted, including an unknown operation after apply --help; the detail states the expected value.",
                    Scope.EVERY_COMMAND),
    CONFLICTING_OPTIONS("conflicting-options", "Options were combined that can't be used together.", Scope.COMMANDS,
                    "performance"),
    INVALID_DATE_RANGE("invalid-date-range", "The start of the period is after its end.", Scope.COMMANDS, "prices",
                    "transactions", "performance"),
    PERIOD_REQUIRED("period-required", "performance needs --from or --period.", Scope.COMMANDS, "performance"),

    // client file
    FILE_REQUIRED("file-required", "No client file was given: pass --file or set PP_FILE.", Scope.CLIENT_FILE),
    FILE_NOT_FOUND("file-not-found", "There is no client file at the given path.", Scope.CLIENT_FILE),
    FILE_UNREADABLE("file-unreadable", "The client file exists but could not be read or parsed.", Scope.CLIENT_FILE),
    FILE_VERSION_UNSUPPORTED("file-version-unsupported",
                    "The client file is newer than this CLI supports (fileVersion > maxSupportedVersion),"
                                    + " usually because the desktop app is newer than the CLI's core."
                                    + " Rebuild the CLI from the matching upstream tag.",
                    Scope.CLIENT_FILE),
    PASSWORD_REQUIRED("password-required",
                    "The client file is encrypted and no password was given: set PP_PASSWORD.", Scope.CLIENT_FILE),
    INCORRECT_PASSWORD("incorrect-password", "The client file could not be decrypted with the given password.",
                    Scope.CLIENT_FILE),

    // references
    REFERENCE_NOT_FOUND("reference-not-found", "No entity of the expected kind matches the reference.",
                    Scope.REFERENCE_OPTION),
    REFERENCE_AMBIGUOUS("reference-ambiguous",
                    "The reference matches several entities; candidates lists their UUIDs.", Scope.REFERENCE_OPTION),
    NO_REFERENCE_CASH_ACCOUNT("no-reference-cash-account",
                    "The investment account has no reference cash account to include.", Scope.COMMANDS,
                    "performance"),

    // writes
    INPUT_UNREADABLE("input-unreadable", "The --input file could not be read.", Scope.COMMANDS, "apply"),
    INVALID_INPUT("invalid-input", "The --input batch is not valid JSON or not shaped as a batch.", Scope.COMMANDS,
                    "apply"),
    VALIDATION_FAILED("validation-failed",
                    "One or more operations of the batch failed validation; failures lists every one"
                                    + " with its operationIndex, field and code. Nothing was written.",
                    Scope.COMMANDS, "apply"),
    CONSISTENCY_CHECK_FAILED("consistency-check-failed",
                    "The change would introduce inconsistencies into the client file. Nothing was written.",
                    Scope.COMMANDS, "apply", "restore"),
    FILE_OPEN_IN_APP("file-open-in-app",
                    "Another process, probably the Portfolio Performance app, has the client file open."
                                    + " Nothing was written; close the file there first.",
                    Scope.COMMANDS, "apply", "restore"),
    FILE_LOCKED("file-locked",
                    "Another pp-cli is writing the client file, or crashed while doing so; the detail names the lock file.",
                    Scope.COMMANDS, "apply", "restore"),
    FILE_CHANGED("file-changed",
                    "The client file changed on disk after it was read. Nothing was written; run the command again.",
                    Scope.COMMANDS, "apply", "restore"),
    WRITE_FAILED("write-failed", "The client file or its lock file could not be written. The file was not changed.",
                    Scope.COMMANDS, "apply", "restore"),
    BACKUP_NOT_FOUND("backup-not-found", "There is no backup file at the given path.", Scope.COMMANDS, "restore"),

    INTERNAL_ERROR("internal-error", "An unexpected failure inside the tool.", Scope.EVERY_COMMAND);

    /**
     * Where an error code can occur. Transitional: ticket 15 replaces these
     * with scopes derived from the commands' facets.
     */
    public enum Scope
    {
        /** Only before a command is chosen; listed in the global help. */
        GLOBAL,
        /** Any command. */
        EVERY_COMMAND,
        /** Any command that reads a client file. */
        CLIENT_FILE,
        /** Any command that declares a reference option. */
        REFERENCE_OPTION,
        /**
         * Any command that declares a required option, and the named commands
         * (those that raise the code for an option that is required only
         * together with another one, which no declaration states).
         */
        REQUIRED_OPTION,
        /** Only the named commands. */
        COMMANDS
    }

    private final String code;
    private final String description;
    private final Scope scope;
    private final List<String> commands;

    ErrorCode(String code, String description, Scope scope, String... commands)
    {
        this.code = code;
        this.description = description;
        this.scope = scope;
        this.commands = List.of(commands);
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

    /** The commands the scope names, if it names any. */
    public List<String> commands()
    {
        return commands;
    }

    /** Whether the command's help lists this code. */
    public boolean appliesTo(CommandSpec command)
    {
        return switch (scope)
        {
            case GLOBAL -> false;
            case EVERY_COMMAND -> true;
            case CLIENT_FILE -> command.usesClientFile();
            case REFERENCE_OPTION -> command.options().stream()
                            .anyMatch(option -> option.type() instanceof OptionSpec.Reference);
            case REQUIRED_OPTION -> command.options().stream().anyMatch(OptionSpec::required)
                            || commands.contains(command.name());
            case COMMANDS -> commands.contains(command.name());
        };
    }
}
