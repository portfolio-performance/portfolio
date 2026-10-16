package name.abuchen.portfolio.cli;

import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Declares one option that takes a value, e.g. <code>--file &lt;path&gt;</code>.
 * An integer option also declares its default and range; <code>describe</code>
 * publishes them and {@link #parseInteger} enforces them, so the two can't
 * drift.
 */
record OptionSpec(String name, String valueName, String description, IntegerDomain integerDomain)
{
    /** The default and inclusive range of an integer option. The maximum may be null (unbounded). */
    record IntegerDomain(long defaultValue, long minimum, Long maximum)
    {
        boolean contains(long value)
        {
            return value >= minimum && (maximum == null || value <= maximum);
        }

        String rangeDescription()
        {
            return maximum == null ? "at least " + minimum : "between " + minimum + " and " + maximum;
        }
    }

    static final OptionSpec FILE = new OptionSpec("file", "path",
                    "Client file to read. Falls back to the PP_FILE environment variable.");

    // Long.parseLong alone would accept a leading '+' and non-ASCII digits
    private static final Pattern INTEGER = Pattern.compile("-?[0-9]+");

    OptionSpec(String name, String valueName, String description)
    {
        this(name, valueName, description, null);
    }

    static OptionSpec integer(String name, String valueName, String description, long defaultValue, long minimum,
                    Long maximum)
    {
        return new OptionSpec(name, valueName, description, new IntegerDomain(defaultValue, minimum, maximum));
    }

    /**
     * The value of this integer option: the default when the option is absent.
     *
     * @throws CliException
     *             a usage error if the value is not an integer in the range
     */
    long parseInteger(Optional<String> raw) throws CliException
    {
        if (raw.isEmpty())
            return integerDomain.defaultValue();

        var text = raw.get();
        if (INTEGER.matcher(text).matches())
        {
            try
            {
                var value = Long.parseLong(text);
                if (integerDomain.contains(value))
                    return value;
            }
            catch (NumberFormatException e)
            {
                // beyond the long range: reported below like any other invalid value
            }
        }

        throw CliException.usage("invalid-option-value", "Invalid option value",
                        "Option '--" + name + "' must be an integer " + integerDomain.rangeDescription() + ".")
                        .with("option", name).with("value", text);
    }
}
