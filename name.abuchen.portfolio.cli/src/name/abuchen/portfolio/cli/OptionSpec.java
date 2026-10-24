package name.abuchen.portfolio.cli;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Declares one option that takes a value, e.g. <code>--file &lt;path&gt;</code>.
 * An integer option also declares its default and range, a date option its
 * ISO format; <code>--help</code> publishes them and {@link #parseInteger}
 * and {@link #parseDate} enforce them, so the two can't drift. A required
 * option is checked by the argument parser.
 */
record OptionSpec(String name, String valueName, String description, Type type, boolean required)
{
    /** How the value of an option is read. */
    sealed interface Type permits Text, IsoDate, IntegerDomain, Flag, Choice
    {
    }

    /** One of a fixed set of values; <code>--help</code> publishes them. */
    record Choice(List<String> values, String defaultValue) implements Type
    {
    }

    /** A switch that takes no value: present or absent. */
    record Flag() implements Type
    {
    }

    /** A free-form value. */
    record Text() implements Type
    {
    }

    /** An ISO <code>yyyy-MM-dd</code> date. */
    record IsoDate() implements Type
    {
    }

    /** The default and inclusive range of an integer option. The maximum may be null (unbounded). */
    record IntegerDomain(long defaultValue, long minimum, Long maximum) implements Type
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

    static final OptionSpec VERBOSE = flag("verbose",
                    "Show core's INFO and OK status messages on stderr, such as the line logged when a file is loaded; hidden by default. Warnings and errors always show; stdout is unaffected.");

    // Long.parseLong alone would accept a leading '+' and non-ASCII digits
    private static final Pattern INTEGER = Pattern.compile("-?[0-9]+");

    private static final Pattern ISO_DATE = Pattern.compile("[0-9]{4}-[0-9]{2}-[0-9]{2}");

    OptionSpec(String name, String valueName, String description)
    {
        this(name, valueName, description, new Text(), false);
    }

    static OptionSpec integer(String name, String valueName, String description, long defaultValue, long minimum,
                    Long maximum)
    {
        return new OptionSpec(name, valueName, description, new IntegerDomain(defaultValue, minimum, maximum), false);
    }

    static OptionSpec date(String name, String description)
    {
        return new OptionSpec(name, "yyyy-MM-dd", description, new IsoDate(), false);
    }

    static OptionSpec choice(String name, String valueName, String description, List<String> values,
                    String defaultValue)
    {
        return new OptionSpec(name, valueName, description, new Choice(values, defaultValue), false);
    }

    /** A switch such as <code>--include-zero</code>, given without a value. */
    static OptionSpec flag(String name, String description)
    {
        return new OptionSpec(name, "", description, new Flag(), false);
    }

    boolean isFlag()
    {
        return type instanceof Flag;
    }

    /** The same option, but the parser rejects a command line without it. */
    OptionSpec asRequired()
    {
        return new OptionSpec(name, valueName, description, type, true);
    }

    /** The usage error for a value of this option that is not <code>expected</code>. */
    CliException invalid(String text, String expected)
    {
        return CliException.usage("invalid-option-value", "Invalid option value",
                        "Option '--" + name + "' must be " + expected + ".").with("option", name).with("value",
                                        text);
    }

    /**
     * The value of this date option, or empty if the option is absent.
     *
     * @throws CliException
     *             a usage error if the value is not a valid ISO date
     */
    Optional<LocalDate> parseDate(Optional<String> raw) throws CliException
    {
        if (raw.isEmpty())
            return Optional.empty();

        var text = raw.get();
        if (ISO_DATE.matcher(text).matches())
        {
            try
            {
                return Optional.of(LocalDate.parse(text));
            }
            catch (DateTimeException e)
            {
                // a well-formed but impossible date, e.g. 2026-02-30: reported below
            }
        }

        throw invalid(text, "a date in the form yyyy-MM-dd");
    }

    /**
     * The value of this choice option: the default when the option is absent.
     *
     * @throws CliException
     *             a usage error if the value is not one of the choices
     */
    String parseChoice(Optional<String> raw) throws CliException
    {
        var choice = (Choice) type;
        if (raw.isEmpty())
            return choice.defaultValue();

        var text = raw.get();
        if (choice.values().contains(text))
            return text;

        throw invalid(text, "one of " + String.join(", ", choice.values()));
    }

    /**
     * The value of this integer option: the default when the option is absent.
     *
     * @throws CliException
     *             a usage error if the value is not an integer in the range
     */
    long parseInteger(Optional<String> raw) throws CliException
    {
        var integerDomain = (IntegerDomain) type;
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

        throw invalid(text, "an integer " + integerDomain.rangeDescription());
    }
}
