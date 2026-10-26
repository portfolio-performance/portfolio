package name.abuchen.portfolio.cli;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.time.LocalDate;
import java.util.List;

import org.junit.Test;

import name.abuchen.portfolio.junit.PortfolioBuilder;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.online.QuoteFeed;

/**
 * The stale-price warnings of a command, in process: one per instrument, in
 * name order, and none for prices that aren't stale.
 */
public class StalePricesTest
{
    private static final LocalDate DAY = LocalDate.parse("2026-03-20");

    /** 19 days before {@link #DAY}. */
    private static final SecurityPrice OLD = new SecurityPrice(LocalDate.parse("2026-03-01"),
                    PortfolioBuilder.quoteOf(10));

    private static Security security(String name)
    {
        var security = new Security(name, "EUR");
        security.addPrice(OLD);
        return security;
    }

    private static List<Object> instruments(StalePrices stalePrices)
    {
        return stalePrices.warnings().stream().map(warning -> warning.toJson().get("instrument")).toList();
    }

    @Test
    public void anInstrumentMetTwiceWarnsOnce()
    {
        var alpha = security("Alpha");
        var stalePrices = new StalePrices(DAY);

        stalePrices.note(alpha, OLD);
        stalePrices.note(alpha, OLD);

        assertThat(instruments(stalePrices), is(List.of(alpha.getUUID())));
    }

    @Test
    public void warningsComeInNameThenUuidOrder()
    {
        var charlie = security("Charlie");
        var alpha = security("Alpha");
        var bravo1 = security("Bravo");
        var bravo2 = security("Bravo");
        var bravos = bravo1.getUUID().compareTo(bravo2.getUUID()) < 0 ? List.of(bravo1, bravo2)
                        : List.of(bravo2, bravo1);

        var stalePrices = new StalePrices(DAY);
        for (var security : List.of(charlie, bravos.get(1), alpha, bravos.get(0)))
            stalePrices.note(security, OLD);

        assertThat(instruments(stalePrices), is(List.of(alpha.getUUID(), bravos.get(0).getUUID(),
                        bravos.get(1).getUUID(), charlie.getUUID())));
    }

    @Test
    public void freshRetiredAndManualPricesDoNotWarn()
    {
        var fresh = new Security("Fresh", "EUR");
        var current = new SecurityPrice(LocalDate.parse("2026-03-15"), PortfolioBuilder.quoteOf(10));
        fresh.addPrice(current);

        var retired = security("Retired");
        retired.setRetired(true);

        var manual = security("Manual");
        manual.setFeed(QuoteFeed.MANUAL);

        var stalePrices = new StalePrices(DAY);
        stalePrices.note(fresh, current);
        stalePrices.note(retired, OLD);
        stalePrices.note(manual, OLD);

        assertThat(stalePrices.warnings().size(), is(0));
    }
}
