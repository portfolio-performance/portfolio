package name.abuchen.portfolio.cli.command;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * How the response of a command is written: as the JSON envelope, or, for a
 * command that declares table columns, as a text table.
 */
public enum OutputFormat
{
    JSON, TABLE;

    public static final String OPTION_NAME = "format";

    public String id()
    {
        return name().toLowerCase();
    }

    /** The <code>--format</code> option, accepting <code>table</code> only if the command can render one. */
    public static OptionSpec option(boolean tableSupported)
    {
        var values = tableSupported ? List.of(JSON.id(), TABLE.id()) : List.of(JSON.id());
        return OptionSpec.choice(OPTION_NAME, "format",
                        "How to write the result: json (the default; the only format for agents)"
                                        + (tableSupported ? " or table (an aligned text table for reading in a terminal)."
                                                        : "."),
                        values, JSON.id());
    }

    /**
     * @throws CliException
     *             <code>invalid-option-value</code> if the command does not
     *             support the requested format
     */
    public static OutputFormat parse(CommandSpec command, Map<String, String> options) throws CliException
    {
        var id = command.option(OPTION_NAME).parseChoice(Optional.ofNullable(options.get(OPTION_NAME)));
        return id.equals(TABLE.id()) ? TABLE : JSON;
    }
}
