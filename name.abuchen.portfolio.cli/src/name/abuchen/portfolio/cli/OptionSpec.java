package name.abuchen.portfolio.cli;

/**
 * Declares one option that takes a value, e.g. <code>--file &lt;path&gt;</code>.
 * The type, default and range are optional; when present, <code>describe</code>
 * publishes them.
 */
record OptionSpec(String name, String valueName, String description, String type, Object defaultValue, Long minimum,
                Long maximum)
{
    static final OptionSpec FILE = new OptionSpec("file", "path",
                    "Client file to read. Falls back to the PP_FILE environment variable.");

    OptionSpec(String name, String valueName, String description)
    {
        this(name, valueName, description, null, null, null, null);
    }

    /** An integer option with a default and an inclusive range; the maximum may be null. */
    static OptionSpec integer(String name, String valueName, String description, long defaultValue, long minimum,
                    Long maximum)
    {
        return new OptionSpec(name, valueName, description, "integer", defaultValue, minimum, maximum);
    }
}
