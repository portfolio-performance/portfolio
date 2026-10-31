package name.abuchen.portfolio.cli.query;

import java.time.LocalDate;

import name.abuchen.portfolio.cli.command.Declaration;
import name.abuchen.portfolio.cli.command.Facet;
import name.abuchen.portfolio.cli.command.OptionSpec;
import name.abuchen.portfolio.cli.command.Schema;

/** <code>--date</code> of a command that values at one day: as given, otherwise today. */
final class ValuationDay
{
    private ValuationDay()
    {
    }

    /** The day; <code>meta</code> gets it as <code>date</code>. */
    static Facet<LocalDate> facet(String description, String metaDescription)
    {
        var option = OptionSpec.date("date", description);
        return Facet.of(Declaration.empty().options(option).meta("date", Schema.string(metaDescription))
                                        .sample("--" + option.name(), "2026-10-01"),
                        run -> run.date(option).orElse(LocalDate.now(run.clock())),
                        (day, out) -> out.meta("date", day.toString()));
    }
}
