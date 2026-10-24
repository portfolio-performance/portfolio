package name.abuchen.portfolio.cli;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Plain-text help for <code>--help</code>, generated from everything declared
 * in {@link Commands}: commands, options, response schemas, write operations
 * and error codes. Nothing here is written by hand per command, so the help
 * can't drift from the behavior.
 */
final class HelpText
{
    static final String OPTION = "--help";

    private static final int ONE_LINE_LIMIT = 100;

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
                    new EnvironmentVariable(EcbCache.VARIABLE,
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
        out.line("  pp-cli apply --help <operation>   Input schema of one write operation.");
        out.blank();

        out.line("Commands:");
        var width = Commands.ALL.stream().mapToInt(c -> c.name().length()).max().orElse(0);
        for (var command : Commands.ALL)
            out.line("  " + pad(command.name(), width) + "  " + oneLine(command.summary()));
        out.blank();

        out.line("Global options:");
        out.line("  --file <path>      " + OptionSpec.FILE.description() + " Not for commands that read no file.");
        out.line("  --format <format>  How to write the result: json (default) or, where the command's help lists it, table.");
        out.line("  --verbose          Show core's INFO and OK status messages on stderr; hidden by default.");
        out.line("  --help             Show this help, or the help of the command before it. Needs no file.");
        out.blank();

        out.line("Environment variables:");
        for (var variable : ENVIRONMENT)
            out.line("  " + variable.name() + "  " + variable.description());
        out.blank();

        out.line("Exit codes: 0 success; 2 usage error; 1 any other failure. A failure is a problem+json document"
                        + " on stdout whose code is one of:");
        errors(out, ErrorSpec.ALL);
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
        for (var option : command.options())
            out.line("  " + syntax(option) + "  " + option.description() + facts(option));

        if (command.name().equals(ApplyCommand.SPEC.name()))
        {
            out.blank();
            out.line("Operations (pp-cli apply --help <operation> shows the input schema of one):");
            var width = Commands.OPERATIONS.stream().mapToInt(o -> o.name().length()).max().orElse(0);
            for (var operation : Commands.OPERATIONS)
                out.line("  " + pad(operation.name(), width) + "  " + oneLine(operation.summary()));
        }

        out.blank();
        out.line("Response data: " + orEmpty(command.dataSchema().description()));
        command.dataSchema().helpLines(2).forEach(out::line);
        out.blank();
        var meta = command.metaSchema();
        var optionalMeta = meta.memberNames(false);
        out.line("Response meta: " + String.join(", ", meta.memberNames(true))
                        + (optionalMeta.isEmpty() ? "" : "; optional: " + String.join(", ", optionalMeta)));

        if (!command.warnings().isEmpty())
        {
            out.blank();
            out.line("Warnings (code and detail in the warnings of the response):");
            for (var warning : command.warnings())
                out.line("  " + warning.code() + "  " + warning.description());
        }

        out.blank();
        out.line("Errors (the code of the problem+json document; exit code 2 for usage errors, 1 otherwise;"
                        + " pp-cli --help says what each means): " + String.join(", ", ErrorSpec.ALL.stream()
                                        .filter(e -> e.appliesTo(command)).map(ErrorSpec::code).toList()));
        return out.toString();
    }

    private static String operation(String name) throws CliException
    {
        var operation = Commands.OPERATIONS.stream().filter(o -> o.name().equals(name)).findFirst()
                        .orElseThrow(() -> CliException.usage("unknown-operation", "Unknown operation",
                                        "Unknown operation '" + name + "'. Known operations: " + String.join(", ",
                                                        Commands.OPERATIONS.stream().map(OperationSpec::name)
                                                                        .toList())));

        var out = new Text();
        out.line("pp-cli apply operation " + operation.name() + ": " + operation.summary());
        out.blank();
        out.line("Input: one element of \"operations\" in the batch given with --input. "
                        + orEmpty(operation.input().description()));
        operation.input().helpLines(2).forEach(out::line);
        return out.toString();
    }

    private static void errors(Text out, List<ErrorSpec> errors)
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
            case OptionSpec.IsoDate date -> facts.add("date");
            case OptionSpec.Flag flag -> facts.add("switch, default off");
            case OptionSpec.Choice choice ->
            {
                facts.add("one of " + String.join(", ", choice.values()));
                facts.add("default " + choice.defaultValue());
            }
            case OptionSpec.Text text ->
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
