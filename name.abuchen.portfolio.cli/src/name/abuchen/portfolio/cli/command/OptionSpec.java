package name.abuchen.portfolio.cli.command;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

import name.abuchen.portfolio.money.CurrencyUnit;

/**
 * Declares one option that takes a value, e.g. <code>--file &lt;path&gt;</code>.
 * An integer option also declares its default and range, a date option its
 * ISO format; <code>--help</code> publishes them and {@link #parseInteger}
 * and {@link #parseDate} enforce them, so the two can't drift. A required
 * option is checked by the argument parser.
 */
public record OptionSpec(String name, String valueName, String description, Type type, boolean required)
{
    /** How the value of an option is read. */
    sealed interface Type permits Text, IsoDate, IntegerDomain, Flag, Choice, Choices, Currency, Reference
    {
    }

    /** A reference to one entity of a kind; declared through {@link ReferenceOption}. */
    public record Reference(String label) implements Type
    {
    }

    /** One of a fixed set of values; <code>--help</code> publishes them. */
    public record Choice(List<String> values, String defaultValue) implements Type
    {
    }

    /**
     * One or more of a fixed set of values, comma-separated and
     * case-insensitive; <code>--help</code> publishes them.
     */
    public record Choices(List<String> values) implements Type
    {
    }

    /** The ISO code of a currency known to core. */
    public record Currency() implements Type
    {
    }

    /** A switch that takes no value: present or absent. */
    public record Flag() implements Type
    {
    }

    /** A free-form value. */
    public record Text() implements Type
    {
    }

    /** An ISO <code>yyyy-MM-dd</code> date. */
    public record IsoDate() implements Type
    {
    }

    /** The default and inclusive range of an integer option. The maximum may be null (unbounded). */
    public record IntegerDomain(long defaultValue, long minimum, Long maximum) implements Type
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

    public static final OptionSpec FILE = new OptionSpec("file", "path",
                    "Client file to read. Falls back to the PP_FILE environment variable.");

    public static final OptionSpec VERBOSE = flag("verbose",
                    "Show core's INFO and OK status messages on stderr, such as the line logged when a file is loaded; hidden by default. Warnings and errors always show; stdout is unaffected.");

    // Long.parseLong alone would accept a leading '+' and non-ASCII digits
    private static final Pattern INTEGER = Pattern.compile("-?[0-9]+");

    private static final Pattern ISO_DATE = Pattern.compile("[0-9]{4}-[0-9]{2}-[0-9]{2}");

    public OptionSpec(String name, String valueName, String description)
    {
        this(name, valueName, description, new Text(), false);
    }

    public static OptionSpec integer(String name, String valueName, String description, long defaultValue, long minimum,
                    Long maximum)
    {
        return new OptionSpec(name, valueName, description, new IntegerDomain(defaultValue, minimum, maximum), false);
    }

    public static OptionSpec date(String name, String description)
    {
        return new OptionSpec(name, "yyyy-MM-dd", description, new IsoDate(), false);
    }

    static OptionSpec choice(String name, String valueName, String description, List<String> values,
                    String defaultValue)
    {
        return new OptionSpec(name, valueName, description, new Choice(values, defaultValue), false);
    }

    /**
     * A multi-choice option: one or more of the values, comma-separated,
     * matched case-insensitively. Its value is the values given, without
     * duplicates, in the order of <code>values</code>.
     */
    public static OptionSpec choices(String name, String valueName, String description, List<String> values)
    {
        return new OptionSpec(name, valueName, description, new Choices(List.copyOf(values)), false);
    }

    /** An option that takes the ISO code of a currency known to core. */
    public static OptionSpec currency(String name, String description)
    {
        return new OptionSpec(name, "code", description, new Currency(), false);
    }

    /** A switch such as <code>--include-zero</code>, given without a value. */
    public static OptionSpec flag(String name, String description)
    {
        return new OptionSpec(name, "", description, new Flag(), false);
    }

    public boolean isFlag()
    {
        return type instanceof Flag;
    }

    /** The same option, but the parser rejects a command line without it. */
    public OptionSpec asRequired()
    {
        return new OptionSpec(name, valueName, description, type, true);
    }

    /** The usage error for a value of this option that is not <code>expected</code>. */
    public CliException invalid(String text, String expected)
    {
        return CliException.usage(ErrorCode.INVALID_OPTION_VALUE, "Invalid option value",
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
     * The values of this multi-choice option, without duplicates and in the
     * published order; empty when the option is absent.
     *
     * @throws CliException
     *             a usage error if an item is not one of the choices
     */
    List<String> parseChoices(Optional<String> raw) throws CliException
    {
        var choices = (Choices) type;
        if (raw.isEmpty())
            return List.of();

        var given = new HashSet<String>();
        for (var item : raw.get().split(",", -1))
        {
            var value = item.trim().toUpperCase(Locale.ROOT);
            if (choices.values().stream().noneMatch(v -> v.toUpperCase(Locale.ROOT).equals(value)))
                throw invalid(raw.get(), "a comma-separated list of: " + String.join(", ", choices.values()));
            given.add(value);
        }
        return choices.values().stream().filter(v -> given.contains(v.toUpperCase(Locale.ROOT))).toList();
    }

    /**
     * The value of this currency option as given, or empty if it is absent.
     *
     * @throws CliException
     *             a usage error if core doesn't know the currency
     */
    Optional<String> parseCurrency(Optional<String> raw) throws CliException
    {
        if (raw.isPresent() && CurrencyUnit.getInstance(raw.get()) == null)
            throw invalid(raw.get(), "an ISO currency code known to core");
        return raw;
    }

    /**
     * Checks a value against the option's type, as the command line is
     * parsed. A reference can only be checked once the client is loaded.
     *
     * @throws CliException
     *             a usage error if the value does not fit the type
     */
    void check(Optional<String> raw) throws CliException
    {
        switch (type)
        {
            case IsoDate date -> parseDate(raw);
            case IntegerDomain domain -> parseInteger(raw);
            case Choice choice -> parseChoice(raw);
            case Choices choices -> parseChoices(raw);
            case Currency currency -> parseCurrency(raw);
            case Flag flag ->
            {
            }
            case Text text ->
            {
            }
            case Reference reference ->
            {
            }
        }
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
