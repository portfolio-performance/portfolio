package name.abuchen.portfolio.cli;

import java.util.ArrayList;
import java.util.List;

/**
 * Declares a command once: its parsing, dispatch and its help all
 * derive from this record. The data and meta schemas describe the two
 * variable parts of the response envelope; the warnings are every code the
 * command can emit. A command that declares table columns also accepts
 * <code>--format table</code>; its data must then be a page of items.
 */
record CommandSpec(String name, String summary, boolean usesClientFile, List<OptionSpec> declaredOptions,
                Schema dataSchema, Schema metaSchema, List<WarningSpec> warnings, List<TableColumn> tableColumns,
                CommandHandler handler)
{
    @FunctionalInterface
    interface CommandHandler
    {
        Response execute(CommandContext context) throws CliException;
    }

    /** A command with warnings that can only answer in JSON. */
    CommandSpec(String name, String summary, boolean usesClientFile, List<OptionSpec> declaredOptions,
                    Schema dataSchema, Schema metaSchema, List<WarningSpec> warnings, CommandHandler handler)
    {
        this(name, summary, usesClientFile, declaredOptions, dataSchema, metaSchema, warnings, List.of(), handler);
    }

    /** A command that emits no warnings and can only answer in JSON. */
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
        options.add(OutputFormat.option(!tableColumns.isEmpty()));
        options.add(OptionSpec.VERBOSE);
        return options;
    }

    boolean accepts(String optionName)
    {
        return options().stream().anyMatch(o -> o.name().equals(optionName));
    }

    /** The accepted option of that name. */
    OptionSpec option(String optionName)
    {
        return options().stream().filter(o -> o.name().equals(optionName)).findFirst().orElseThrow();
    }
}
