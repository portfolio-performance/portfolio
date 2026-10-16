package name.abuchen.portfolio.cli;

import java.util.ArrayList;
import java.util.List;

/**
 * Declares a command once: its parsing, dispatch and (later) its description
 * all derive from this record.
 */
record CommandSpec(String name, String summary, boolean usesClientFile, List<OptionSpec> declaredOptions,
                CommandHandler handler)
{
    @FunctionalInterface
    interface CommandHandler
    {
        Response execute(CommandContext context) throws CliException;
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
