package name.abuchen.portfolio.cli;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import name.abuchen.portfolio.cli.apply.ApplyCommand;
import name.abuchen.portfolio.cli.apply.Operations;
import name.abuchen.portfolio.cli.command.CliException;
import name.abuchen.portfolio.cli.command.CommandContext;
import name.abuchen.portfolio.cli.command.CommandSpec;
import name.abuchen.portfolio.cli.command.ErrorCode;
import name.abuchen.portfolio.cli.command.MinorUnits;
import name.abuchen.portfolio.cli.command.OperationSpec;
import name.abuchen.portfolio.cli.command.OptionSpec;
import name.abuchen.portfolio.cli.command.OutputFormat;
import name.abuchen.portfolio.cli.currency.RateCache;

/**
 * Plain-text help for <code>--help</code>, generated from everything declared
 * in {@link Commands}: commands, options, response schemas, write operations
 * and error codes. Nothing here is written by hand per command, so the help
 * can't drift from the behavior.
 */
final class HelpText
{
    private static final OptionSpec HELP = OptionSpec.flag("help",
                    "Show this help, or the help of the command before it. Needs no file.");

    static final String OPTION = "--" + HELP.name();

    private static final int ONE_LINE_LIMIT = 100;

    private static final String FAILURE_CODES = "Failure codes (failures[].code of validation-failed)";

    /** What the tool can't do. Agents must not promise these to users. */
    static final List<String> KNOWN_LIMITS = List.of(
                    "Unsaved edits inside a running Portfolio Performance app can't be detected; a file open in the app may be out of date on disk.",
                    "PDF import is not available.",
                    "Prices can't be written: there are no price writes, and quotes are neither fetched nor edited.");

    record EnvironmentVariable(String name, String description)
    {
    }

    static final List<EnvironmentVariable> ENVIRONMENT = List.of(
                    new EnvironmentVariable("PP_FILE", "Client file to read, used when --file is not given."),
                    new EnvironmentVariable(RateCache.VARIABLE,
                                    "Path of the desktop app's ECB exchange-rate cache (ecb_exchange_rates.pb), read-only,"
                                                    + " used by commands that convert currencies. If unset, the app's default workspace is"
                                                    + " searched (%LOCALAPPDATA%\\PortfolioPerformance\\workspace on Windows,"
                                                    + " ~/Library/Application Support/name.abuchen.portfolio.product/workspace on macOS,"
                                                    + " ~/.PortfolioPerformance/workspace on Linux); a custom workspace needs this variable."
                                                    + " Core's built-in rates are never used; without a cache, rates still come from"
                                                    + " transactions and exchange-rate instruments."),
                    new EnvironmentVariable(CommandContext.PASSWORD_VARIABLE,
                                    "Password of an encrypted client file. If unset, it is read as one line from stdin"
                                                    + " (the prompt goes to stderr). There is no password option;"
                                                    + " a missing or wrong one gives password-required or incorrect-password."));

    private HelpText()
    {
    }

    /**
     * The help the arguments ask for, or empty if they don't ask for any.
     * <code>--help</code> wins over every other argument; so does an empty
     * command line.
     *
     * @throws CliException
     *             a usage error if the help is for a command or operation that
     *             does not exist
     */
    static Optional<String> render(List<String> args) throws CliException
    {
        if (args.isEmpty())
            return Optional.of(global());

        var helpIndex = args.indexOf(OPTION);
        if (helpIndex < 0)
            return Optional.empty();

        if (args.get(0).startsWith("--"))
            return Optional.of(global());

        var command = Commands.require(args.get(0));
        if (command.name().equals(ApplyCommand.SPEC.name()) && helpIndex + 1 < args.size()
                        && !args.get(helpIndex + 1).startsWith("--"))
            return Optional.of(operation(args.get(helpIndex + 1)));

        return Optional.of(command(command));
    }

    private static String global()
    {
        var out = new Text();
        out.line("pp-cli reads and writes a Portfolio Performance client file. Output is one JSON document on stdout.");
        out.line("Built from upstream tag " + CoreVersion.PINNED_UPSTREAM_TAG + "; reads client files up to version "
                        + CoreVersion.MAX_SUPPORTED_FILE_VERSION + " (pp-cli info reports both).");
        out.blank();
        out.line("Usage:");
        out.line("  pp-cli <command> [options]");
        out.line("  pp-cli <command> --help           Options, response fields and error codes of one command.");
        out.line("  pp-cli apply --help <operation>   Input schema and failure codes of one write operation.");
        out.line("References (the <ref> option values): a UUID. Reads also accept an exact name, and for instruments an ISIN or ticker; writes take UUIDs only.");
        out.blank();

        out.line("Commands:");
        var width = Commands.ALL.stream().mapToInt(c -> c.name().length()).max().orElse(0);
        for (var command : Commands.ALL)
            out.line("  " + pad(command.name(), width) + "  " + oneLine(command.summary()));
        out.blank();

        out.line("Global options:");
        out.line("  " + optionLine(OptionSpec.FILE) + " Not for commands that read no file.");
        out.line("  " + optionLine(OutputFormat.option(true)) + " Only the commands whose help lists it offer table.");
        out.line("  " + optionLine(OptionSpec.VERBOSE));
        out.line("  " + syntax(HELP) + "  " + HELP.description());
        out.blank();

        out.line("Environment variables:");
        for (var variable : ENVIRONMENT)
            out.line("  " + variable.name() + "  " + variable.description());
        out.blank();

        out.line("Exit codes: 0 success; 2 usage error; 1 any other failure. A failure is a problem+json document"
                        + " on stdout whose code is one of:");
        errors(out, problemCodes());
        out.blank();

        out.line("Limits (do not promise these):");
        for (var limit : KNOWN_LIMITS)
            out.line("  - " + limit);
        return out.toString();
    }

    private static String command(CommandSpec command)
    {
        var out = new Text();
        out.line("pp-cli " + command.name() + ": " + command.summary());
        out.blank();
        out.line("Usage: " + usage(command));
        out.blank();

        out.line("Options:");
        command.declaredOptions().forEach(option -> out.line("  " + optionLine(option)));
        out.line("  Global options (pp-cli --help): "
                        + String.join(", ", command.sharedOptions().stream().map(HelpText::sharedSyntax).toList()));

        if (command.name().equals(ApplyCommand.SPEC.name()))
        {
            out.blank();
            out.line("Operations (pp-cli apply --help <operation> shows the input schema and failure codes of one):");
            var width = Operations.ALL.stream().mapToInt(o -> o.name().length()).max().orElse(0);
            for (var operation : Operations.ALL)
                out.line("  " + pad(operation.name(), width) + "  " + oneLine(operation.summary()));
        }

        out.blank();
        out.line("Response data: " + orEmpty(command.dataSchema().description()));
        var shown = new HashSet<String>();
        command.dataSchema().helpLines(2).forEach(line -> out.line(shortenRepeatedNotes(line, shown)));
        out.blank();
        var meta = command.metaSchema();
        var optionalMeta = meta.memberNames(false);
        out.line("Response meta: " + String.join(", ", meta.memberNames(true))
                        + (optionalMeta.isEmpty() ? "" : "; optional: " + String.join(", ", optionalMeta)));

        if (!command.warnings().isEmpty())
        {
            out.blank();
            out.line("Warnings:");
            for (var warning : command.warnings())
                out.line("  " + warning.code() + "  " + warning.description());
        }

        out.blank();
        out.line("Error codes (meanings: pp-cli --help): " + String.join(", ",
                        command.errorCodes().stream().map(ErrorCode::code).toList()));

        if (command.name().equals(ApplyCommand.SPEC.name()))
        {
            out.blank();
            out.line(FAILURE_CODES + " of the batch:");
            errors(out, List.copyOf(ApplyCommand.BATCH_FAILURE_CODES));
            out.line("  Those of each operation: pp-cli apply --help <operation>.");
        }
        return out.toString();
    }

    private static String operation(String name) throws CliException
    {
        var operation = Operations.ALL.stream().filter(o -> o.name().equals(name)).findFirst()
                        .orElseThrow(() -> HELP.invalid(name, "one of the operations " + String.join(", ",
                                        Operations.ALL.stream().map(OperationSpec::name).toList())));

        var out = new Text();
        out.line("pp-cli apply operation " + operation.name() + ": " + operation.summary());
        out.blank();
        out.line("Input: one element of \"operations\" in the batch given with --input. "
                        + orEmpty(operation.input().description()));
        operation.input().helpLines(2).forEach(out::line);
        out.blank();
        out.line(FAILURE_CODES + ":");
        errors(out, List.copyOf(operation.failureCodes()));
        return out.toString();
    }

    /**
     * The codes a problem document can carry: those of every command's help.
     * A code that only a write operation or the batch declares is listed by
     * the help of apply.
     */
    private static List<ErrorCode> problemCodes()
    {
        var declared = Commands.ALL.stream().flatMap(c -> c.errorCodes().stream()).collect(Collectors.toSet());
        return Arrays.stream(ErrorCode.values())
                        .filter(code -> code.scope() != ErrorCode.Scope.BY_USE || declared.contains(code)).toList();
    }

    private static void errors(Text out, List<ErrorCode> errors)
    {
        for (var error : errors)
            out.line("  " + error.code() + "  " + error.description());
    }

    private static String usage(CommandSpec command)
    {
        var usage = new StringBuilder("pp-cli " + command.name());
        for (var option : command.options())
        {
            if (option.required())
                usage.append(' ').append(syntax(option));
        }
        return usage.append(" [options]").toString();
    }

    private static String optionLine(OptionSpec option)
    {
        return syntax(option) + "  " + option.description() + facts(option);
    }

    /** A global option as one phrase: its syntax, and its values if it has a choice. */
    private static String sharedSyntax(OptionSpec option)
    {
        return option.type() instanceof OptionSpec.Choice choice
                        ? syntax(option) + " (" + String.join(", ", choice.values()) + ")"
                        : syntax(option);
    }

    private static String syntax(OptionSpec option)
    {
        return "--" + option.name() + (option.isFlag() ? "" : " <" + option.valueName() + ">");
    }

    /** What the option's declaration adds to its description: required, type, allowed values, default. */
    private static String facts(OptionSpec option)
    {
        var facts = new ArrayList<String>();
        if (option.required())
            facts.add("required");

        switch (option.type())
        {
            case OptionSpec.IntegerDomain domain ->
            {
                facts.add("integer " + (domain.maximum() == null ? "at least " + domain.minimum()
                                : domain.minimum() + " to " + domain.maximum()));
                facts.add("default " + domain.defaultValue());
            }
            case OptionSpec.IsoDate date ->
            {
                // the value placeholder already reads yyyy-MM-dd
            }
            case OptionSpec.Flag flag -> facts.add("switch, default off");
            case OptionSpec.Choice choice ->
            {
                facts.add("one of " + String.join(", ", choice.values()));
                facts.add("default " + choice.defaultValue());
            }
            case OptionSpec.Choices choices -> facts.add("one or more of " + String.join(", ", choices.values()));
            case OptionSpec.Currency currency ->
            {
            }
            case OptionSpec.Text text ->
            {
            }
            case OptionSpec.Reference reference ->
            {
            }
        }
        return facts.isEmpty() ? "" : " (" + String.join(", ", facts) + ")";
    }

    /** The summary up to its first full stop, cut at a word if it is still long; the rest is for the command's own help. */
    private static String oneLine(String summary)
    {
        var end = summary.indexOf(". ");
        var sentence = end < 0 ? summary : summary.substring(0, end + 1);
        if (sentence.length() <= ONE_LINE_LIMIT)
            return sentence;

        return sentence.substring(0, sentence.lastIndexOf(' ', ONE_LINE_LIMIT)) + " ...";
    }

    /** The minor-unit notes repeat on many fields; the second time a note appears it is replaced by a pointer. */
    private static String shortenRepeatedNotes(String line, Set<String> shown)
    {
        for (var note : MinorUnits.NOTES)
        {
            if (line.contains(note) && !shown.add(note))
                return line.replace(note, MinorUnits.AS_ABOVE);
        }
        return line;
    }

    private static String pad(String text, int width)
    {
        return text + " ".repeat(Math.max(0, width - text.length()));
    }

    private static String orEmpty(String text)
    {
        return text == null ? "" : text;
    }

    private static final class Text
    {
        private final StringBuilder text = new StringBuilder();

        void line(String line)
        {
            text.append(line).append('\n');
        }

        void blank()
        {
            text.append('\n');
        }

        @Override
        public String toString()
        {
            return text.toString();
        }
    }
}
