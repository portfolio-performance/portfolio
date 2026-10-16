package name.abuchen.portfolio.cli;

import java.util.ArrayList;
import java.util.List;

/**
 * Declares a command once: its parsing, dispatch and its description all
 * derive from this record. The data and meta schemas describe the two
 * variable parts of the response envelope; the warnings are every code the
 * command can emit.
 */
record CommandSpec(String name, String summary, boolean usesClientFile, List<OptionSpec> declaredOptions,
                Schema dataSchema, Schema metaSchema, List<WarningSpec> warnings, CommandHandler handler)
{
    @FunctionalInterface
    interface CommandHandler
    {
        Response execute(CommandContext context) throws CliException;
    }

    /** A command that emits no warnings. */
    CommandSpec(String name, String summary, boolean usesClientFile, List<OptionSpec> declaredOptions,
                    Schema dataSchema, Schema metaSchema, CommandHandler handler)
    {
        this(name, summary, usesClientFile, declaredOptions, dataSchema, metaSchema, List.of(), handler);
    }

    /** All options the parser accepts, including the shared ones. */
    List<OptionSpec> options()
    {
        var options = new ArrayList<OptionSpec>();
        if (usesClientFile)
            options.add(OptionSpec.FILE);
        options.addAll(declaredOptions);
        return options;
    }

    boolean accepts(String optionName)
    {
        return options().stream().anyMatch(o -> o.name().equals(optionName));
    }
}
