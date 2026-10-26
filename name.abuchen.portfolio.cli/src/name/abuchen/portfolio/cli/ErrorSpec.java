package name.abuchen.portfolio.cli;

import java.util.List;

import name.abuchen.portfolio.cli.command.CliException;
import name.abuchen.portfolio.cli.command.CommandSpec;
import name.abuchen.portfolio.cli.command.OptionSpec;

/**
 * Declares an error code the tool can fail with; <code>--help</code>
 * publishes it. Every code passed to {@link CliException} must be listed in
 * {@link #ALL}. The scope says which commands can return it, so that a
 * command's help lists only its own codes. Where a code follows from an
 * option, the scope derives from the declared options, so a new option can't
 * leave its codes out.
 */
record ErrorSpec(String code, String description, Scope scope, List<String> commands)
{
    /** Where an error code can occur. */
    enum Scope
    {
        /** Only before a command is chosen; listed in the global help. */
        GLOBAL,
        /** Any command. */
        EVERY_COMMAND,
        /** Any command that reads a client file. */
        CLIENT_FILE,
        /** Any command that declares a reference option. */
        REFERENCE_OPTION,
        /** Any command that declares a required option, and the named commands. */
        REQUIRED_OPTION,
        /** Only the named commands. */
        COMMANDS
    }

    static ErrorSpec global(String code, String description)
    {
        return new ErrorSpec(code, description, Scope.GLOBAL, List.of());
    }

    static ErrorSpec everyCommand(String code, String description)
    {
        return new ErrorSpec(code, description, Scope.EVERY_COMMAND, List.of());
    }

    static ErrorSpec clientFile(String code, String description)
    {
        return new ErrorSpec(code, description, Scope.CLIENT_FILE, List.of());
    }

    static ErrorSpec referenceOption(String code, String description)
    {
        return new ErrorSpec(code, description, Scope.REFERENCE_OPTION, List.of());
    }

    /**
     * @param alsoCommands
     *            commands that raise the code for an option that is required
     *            only together with another one, which no declaration states
     */
    static ErrorSpec requiredOption(String code, String description, String... alsoCommands)
    {
        return new ErrorSpec(code, description, Scope.REQUIRED_OPTION, List.of(alsoCommands));
    }

    static ErrorSpec of(String code, String description, String... commands)
    {
        return new ErrorSpec(code, description, Scope.COMMANDS, List.of(commands));
    }

    boolean appliesTo(CommandSpec command)
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

    static final List<ErrorSpec> ALL = List.of(
                    // invocation
                    global("unknown-command", "The command does not exist; the detail lists the known commands."),
                    everyCommand("unexpected-argument", "An argument is not an option; options look like --name value."),
                    everyCommand("unknown-option", "The command has no such option."),
                    everyCommand("option-value-required", "An option that takes a value was given without one."),
                    // performance: --with-reference-cash-account needs --investment-account
                    requiredOption("option-required",
                                    "A required option is missing, or an option needs another option.", "performance"),
                    everyCommand("invalid-option-value",
                                    "An option value is not accepted, including an unknown operation after apply --help; the detail states the expected value."),
                    of("conflicting-options", "Options were combined that can't be used together.", "performance"),
                    of("invalid-date-range", "The start of the period is after its end.", "prices", "transactions",
                                    "performance"),
                    of("period-required", "performance needs --from or --period.", "performance"),

                    // client file
                    clientFile("file-required", "No client file was given: pass --file or set PP_FILE."),
                    clientFile("file-not-found", "There is no client file at the given path."),
                    clientFile("file-unreadable", "The client file exists but could not be read or parsed."),
                    clientFile("file-version-unsupported",
                                    "The client file is newer than this CLI supports (fileVersion > maxSupportedVersion),"
                                                    + " usually because the desktop app is newer than the CLI's core."
                                                    + " Rebuild the CLI from the matching upstream tag."),
                    clientFile("password-required",
                                    "The client file is encrypted and no password was given: set PP_PASSWORD."),
                    clientFile("incorrect-password", "The client file could not be decrypted with the given password."),

                    // references
                    referenceOption("reference-not-found", "No entity of the expected kind matches the reference."),
                    referenceOption("reference-ambiguous",
                                    "The reference matches several entities; candidates lists their UUIDs."),
                    of("no-reference-cash-account", "The investment account has no reference cash account to include.",
                                    "performance"),

                    // writes
                    of("input-unreadable", "The --input file could not be read.", "apply"),
                    of("invalid-input", "The --input batch is not valid JSON or not shaped as a batch.", "apply"),
                    of("validation-failed",
                                    "One or more operations of the batch failed validation; failures lists every one"
                                                    + " with its operationIndex, field and code. Nothing was written.",
                                    "apply"),
                    of("consistency-check-failed",
                                    "The change would introduce inconsistencies into the client file. Nothing was written.",
                                    "apply", "restore"),
                    of("file-open-in-app",
                                    "Another process, probably the Portfolio Performance app, has the client file open."
                                                    + " Nothing was written; close the file there first.",
                                    "apply", "restore"),
                    of("file-locked",
                                    "Another pp-cli is writing the client file, or crashed while doing so; the detail names the lock file.",
                                    "apply", "restore"),
                    of("file-changed",
                                    "The client file changed on disk after it was read. Nothing was written; run the command again.",
                                    "apply", "restore"),
                    of("write-failed", "The client file or its lock file could not be written. The file was not changed.",
                                    "apply", "restore"),
                    of("backup-not-found", "There is no backup file at the given path.", "restore"),

                    everyCommand("internal-error", "An unexpected failure inside the tool."));
}
