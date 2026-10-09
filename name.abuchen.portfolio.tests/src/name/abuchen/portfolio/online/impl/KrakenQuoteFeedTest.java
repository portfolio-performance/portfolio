package name.abuchen.portfolio.online.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;

import java.time.LocalDate;
import java.time.ZoneOffset;

import org.junit.Test;

import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;

@SuppressWarnings("nls")
public class KrakenQuoteFeedTest
{
    @Test
    public void testStartsAtTheEpochWithoutPrices()
    {
        var security = new Security("BABY", "EUR");

        // Kraken answers "EGeneral:Invalid arguments" for a negative "since"
        // (LocalDate.MIN before), so a security without prices got none
        var start = KrakenQuoteFeed.startDate(security);
        assertThat(start, is(LocalDate.EPOCH));
        assertThat(start.atStartOfDay(ZoneOffset.UTC).toEpochSecond(), greaterThanOrEqualTo(0L));
    }

    @Test
    public void testStartsAtTheLastStoredPrice()
    {
        var security = new Security("BABY", "EUR");
        security.addPrice(new SecurityPrice(LocalDate.of(2026, 10, 1), 100));
        security.addPrice(new SecurityPrice(LocalDate.of(2026, 10, 8), 100));

        assertThat(KrakenQuoteFeed.startDate(security), is(LocalDate.of(2026, 10, 8)));
    }
}
