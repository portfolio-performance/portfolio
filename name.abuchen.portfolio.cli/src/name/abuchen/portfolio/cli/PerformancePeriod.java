package name.abuchen.portfolio.cli;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import name.abuchen.portfolio.snapshot.ReportingPeriod;
import name.abuchen.portfolio.util.Interval;

/**
 * The period a performance figure covers, from the first to the last day, both
 * counted: <code>--from</code> and <code>--to</code> mean the same here as in
 * the other commands. Core's interval is open at its start, so it begins on the
 * day before the first day, where the opening value is taken at the end of that
 * day.
 * <p>
 * A <code>--period</code> reporting-period code is parsed by core and resolved
 * to the same first and last day, so that the resolved dates echoed in
 * <code>meta</code> read the same however the period was given.
 *
 * @param code
 *            the reporting-period code as given, if the period came from one
 */
record PerformancePeriod(LocalDate firstDay, LocalDate lastDay, Optional<String> code)
{
    private static final OptionSpec FROM = OptionSpec.date("from",
                    "First day of the period: activity on it counts. With --to; not with --period.");

    private static final OptionSpec TO = OptionSpec.date("to",
                    "Last day of the period, inclusive. Defaults to today. Not with --period.");

    private static final OptionSpec PERIOD = new OptionSpec("period", "code",
                    "A reporting-period code of the desktop app, resolved by core relative to today: for example L1Y0 (last year), X or YTD (year to date), Y2025 (the year 2025), L0Y6 (last six months), S2025-01-15 (since a day). Instead of --from and --to.");

    static final List<OptionSpec> OPTIONS = List.of(FROM, TO, PERIOD);

    /** Core's code for "year to date"; agents also say <code>YTD</code>. */
    private static final String YEAR_TO_DATE_CODE = "X";
    private static final String YEAR_TO_DATE_ALIAS = "YTD";

    /**
     * @throws CliException
     *             a usage error if the options are missing, conflict or are
     *             invalid, or if the period would end before it starts
     */
    static PerformancePeriod resolve(CommandContext context) throws CliException
    {
        var code = context.text(PERIOD);
        if (code.isPresent())
            return fromCode(context, code.get());

        var range = DateRange.parse(context, FROM, TO);
        var from = range.from().orElseThrow(() -> CliException.usage("period-required", "Period required",
                        "Pass --" + FROM.name() + " <date> (with an optional --" + TO.name() + "), or --"
                                        + PERIOD.name() + " <code>."));
        return new PerformancePeriod(from, range.to(), Optional.empty());
    }

    private static PerformancePeriod fromCode(CommandContext context, String code) throws CliException
    {
        if (context.given(FROM) || context.given(TO))
            throw CliException.usage("conflicting-options", "Conflicting options",
                            "Option '--" + PERIOD.name() + "' cannot be combined with '--" + FROM.name() + "' or '--"
                                            + TO.name() + "'.")
                            .with("option", PERIOD.name());

        var today = LocalDate.now(context.clock());
        var interval = parseReportingPeriod(code, today).toInterval(today);
        var firstDay = interval.getStart().plusDays(1);

        // core answers a period that ends on its start day (e.g. "since" a future day) with an empty interval
        if (firstDay.isAfter(interval.getEnd()))
            throw CliException.usage("invalid-date-range", "Invalid date range",
                            "The period '" + code + "' starts on " + firstDay + ", after it ends on "
                                            + interval.getEnd() + ".")
                            .with("from", firstDay.toString()).with("to", interval.getEnd().toString());

        return new PerformancePeriod(firstDay, interval.getEnd(), Optional.of(code));
    }

    private static ReportingPeriod parseReportingPeriod(String code, LocalDate today) throws CliException
    {
        var normalized = YEAR_TO_DATE_ALIAS.equals(code) ? YEAR_TO_DATE_CODE : code;

        Optional<ReportingPeriod> parsed = Optional.empty();
        try
        {
            if (!normalized.isBlank())
                parsed = ReportingPeriod.tryFrom(normalized);
        }
        catch (RuntimeException e)
        {
            // core does not guard every malformed code: reported below like any other
        }

        return parsed.map(p -> independentOfTheWallClock(p, today))
                        .orElseThrow(() -> PERIOD.invalid(code, "a reporting-period code such as L1Y0, YTD or Y2025"));
    }

    /**
     * Core's year-to-date and current-month periods take their start from the
     * system clock, not from the day they are resolved for. The same periods
     * are expressed as "since" the day before the start, from the injected
     * clock, so that the period is deterministic.
     */
    private static ReportingPeriod independentOfTheWallClock(ReportingPeriod period, LocalDate today)
    {
        if (period instanceof ReportingPeriod.YearToDate)
            return new ReportingPeriod.SinceX(today.withDayOfMonth(1).withMonth(1).minusDays(1));
        if (period instanceof ReportingPeriod.CurrentMonth)
            return new ReportingPeriod.SinceX(today.withDayOfMonth(1).minusDays(1));
        return period;
    }

    /** The interval core's performance engine takes: it is open at its start, which is the day before the first day. */
    Interval interval()
    {
        return Interval.of(firstDay.minusDays(1), lastDay);
    }

    /** Adds the resolved first and last day and, if given, the code. */
    void putInto(Map<String, Object> meta)
    {
        meta.put("from", firstDay.toString());
        meta.put("to", lastDay.toString());
        code.ifPresent(c -> meta.put("period", c));
    }

    /** Declares the members {@link #putInto} adds. */
    static Schema declareIn(Schema meta)
    {
        return meta.property("from", Schema.string(
                        "First day of the period, as resolved: as given with --from, or the first day of the --period code."))
                        .property("to", Schema.string(
                                        "Last day of the period, as resolved: as given with --to, otherwise today, or the last day of the --period code. It can lie after today, e.g. for the code of a year not yet over."))
                        .optionalProperty("period",
                                        Schema.string("The reporting-period code, as given. Absent with --from."));
    }
}
