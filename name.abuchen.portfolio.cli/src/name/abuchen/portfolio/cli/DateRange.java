package name.abuchen.portfolio.cli;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

/**
 * The inclusive range of days of <code>--from</code> and <code>--to</code>. An
 * absent <code>--from</code> means no lower bound; <code>--to</code> defaults
 * to today from the injected clock.
 */
public record DateRange(Optional<LocalDate> from, LocalDate to)
{
    /**
     * @throws CliException
     *             <code>invalid-option-value</code> for a malformed date,
     *             <code>invalid-date-range</code> if the range is empty
     */
    public static DateRange parse(CommandContext context, OptionSpec fromOption, OptionSpec toOption)
                    throws CliException
    {
        var from = context.date(fromOption);
        var to = context.date(toOption).orElse(LocalDate.now(context.clock()));

        if (from.isPresent() && from.get().isAfter(to))
            throw CliException.usage("invalid-date-range", "Invalid date range",
                            "Option '--" + fromOption.name() + "' (" + from.get() + ") must not be after '--"
                                            + toOption.name() + "' (" + to + ").")
                            .with("from", from.get().toString()).with("to", to.toString());

        return new DateRange(from, to);
    }

    boolean contains(LocalDate day)
    {
        return (from.isEmpty() || !day.isBefore(from.get())) && !day.isAfter(to);
    }

    /** Adds <code>from</code>, if given, and <code>to</code>. */
    void putInto(Map<String, Object> meta)
    {
        from.ifPresent(date -> meta.put("from", date.toString()));
        meta.put("to", to.toString());
    }

    /** Declares the members {@link #putInto} adds. */
    static Schema declareIn(Schema meta)
    {
        return meta.optionalProperty("from", Schema.string("First day of the range, as given. Absent if not given."))
                        .property("to", Schema.string("Last day of the range: as given, otherwise today."));
    }
}
