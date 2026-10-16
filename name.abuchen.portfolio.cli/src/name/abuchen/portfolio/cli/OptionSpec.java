package name.abuchen.portfolio.cli;

/**
 * Declares one option that takes a value, e.g. <code>--file &lt;path&gt;</code>.
 */
record OptionSpec(String name, String valueName, String description)
{
    static final OptionSpec FILE = new OptionSpec("file", "path",
                    "Client file to read. Falls back to the PP_FILE environment variable.");
}
