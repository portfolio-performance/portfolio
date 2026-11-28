package name.abuchen.portfolio.online.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

import org.junit.Test;

import name.abuchen.portfolio.model.LatestSecurityPrice;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.online.QuoteFeedData;

@SuppressWarnings("nls")
public class SwissFundDataQuoteFeedTest
{
    private static final String ISIN = "CH1529090479";

    private Security createSecurity(String currency)
    {
        var security = new Security("Swisscanto Index Equity Fund Large Caps Switzerland NMT", currency);
        security.setIsin(ISIN);
        security.setFeed(SwissFundDataQuoteFeed.ID);
        return security;
    }

    private String getResponse(String name) throws IOException
    {
        try (var scanner = new Scanner(getClass().getResourceAsStream(name), StandardCharsets.UTF_8.name()))
        {
            return scanner.useDelimiter("\\A").next();
        }
    }

    private LatestSecurityPrice priceAt(List<LatestSecurityPrice> prices, LocalDate date)
    {
        return prices.stream().filter(p -> p.getDate().equals(date)).findFirst().orElseThrow();
    }

    @Test
    public void testFindShareClassId() throws IOException
    {
        var response = getResponse("SwissFundDataSuggestions.json");

        assertThat(SwissFundDataQuoteFeed.findShareClassId(response, ISIN),
                        is(Optional.of("79afc3c9-fcf9-4b5d-b308-71c4caf529ca")));
        assertThat(SwissFundDataQuoteFeed.findShareClassId(response, ISIN.toLowerCase()),
                        is(Optional.of("79afc3c9-fcf9-4b5d-b308-71c4caf529ca")));
        assertThat(SwissFundDataQuoteFeed.findShareClassId(response, "CH0117044971"), is(Optional.empty()));
    }

    @Test
    public void testParsePrices() throws IOException
    {
        var data = new QuoteFeedData();
        SwissFundDataQuoteFeed.parsePrices(createSecurity("CHF"), getResponse("SwissFundDataPerformance.json"),
                        data);

        assertThat(data.getErrors(), empty());

        var prices = data.getLatestPrices();
        assertThat(prices, hasSize(186));

        // one price per day in ascending order
        for (int ii = 1; ii < prices.size(); ii++)
            assertThat(prices.get(ii - 1).getDate().isBefore(prices.get(ii).getDate()), is(true));

        assertThat(prices.get(0).getDate(), is(LocalDate.parse("2026-03-05")));
        assertThat(prices.get(0).getValue(), is(Values.Quote.factorize(100)));

        assertThat(prices.get(prices.size() - 1).getDate(), is(LocalDate.parse("2026-11-26")));
        assertThat(prices.get(prices.size() - 1).getValue(), is(Values.Quote.factorize(114.844)));

        // days with several values: rounded duplicates and the EUR price
        assertThat(priceAt(prices, LocalDate.parse("2026-11-18")).getValue(),
                        is(Values.Quote.factorize(115.3738)));
        assertThat(priceAt(prices, LocalDate.parse("2026-11-19")).getValue(),
                        is(Values.Quote.factorize(114.1021)));
        assertThat(priceAt(prices, LocalDate.parse("2026-11-17")).getValue(),
                        is(Values.Quote.factorize(116.0699)));

        assertThat(priceAt(prices, LocalDate.parse("2026-11-19")).getHigh(), is(LatestSecurityPrice.NOT_AVAILABLE));
        assertThat(priceAt(prices, LocalDate.parse("2026-11-19")).getLow(), is(LatestSecurityPrice.NOT_AVAILABLE));
        assertThat(priceAt(prices, LocalDate.parse("2026-11-19")).getVolume(),
                        is(LatestSecurityPrice.NOT_AVAILABLE));
    }

    @Test
    public void testCurrencyMismatch() throws IOException
    {
        var data = new QuoteFeedData();
        SwissFundDataQuoteFeed.parsePrices(createSecurity("EUR"), getResponse("SwissFundDataPerformance.json"),
                        data);

        assertThat(data.getLatestPrices(), empty());
        assertThat(data.getErrors(), hasSize(1));
    }

    @Test
    public void testAccessRestricted()
    {
        // response for this share class if the investor category is 'private'
        var response = "{\"isAccessRestricted\":true,\"availableCurrencies\":[],\"historicalData\":[],"
                        + "\"historicalDataDetails\":[],\"corporateActions\":[]}";

        var data = new QuoteFeedData();
        SwissFundDataQuoteFeed.parsePrices(createSecurity("CHF"), response, data);

        assertThat(data.getLatestPrices(), empty());
        assertThat(data.getErrors(), hasSize(1));
    }

    @Test
    public void testMissingIsin()
    {
        var security = createSecurity("CHF");
        security.setIsin(null);

        var data = new SwissFundDataQuoteFeed().getHistoricalQuotes(security, false);

        assertThat(data.getLatestPrices(), empty());
        assertThat(data.getErrors(), hasSize(1));
    }

    @Test
    public void testSelectValue()
    {
        // values of 2026-11-19 in the CHF response: the CHF price, the same
        // price rounded, and the EUR price
        var candidates = List.of(new BigDecimal("114.1021"), new BigDecimal("114.102"), new BigDecimal("123.2565"));

        assertThat(SwissFundDataQuoteFeed.selectValue(candidates, new BigDecimal("115.3738")),
                        is(new BigDecimal("114.1021")));

        // without a previous price the majority wins
        assertThat(SwissFundDataQuoteFeed.selectValue(candidates, null), is(new BigDecimal("114.1021")));

        // the order of the values must not matter
        var reordered = List.of(new BigDecimal("123.2565"), new BigDecimal("114.102"), new BigDecimal("114.1021"));

        assertThat(SwissFundDataQuoteFeed.selectValue(reordered, new BigDecimal("115.3738")),
                        is(new BigDecimal("114.1021")));
        assertThat(SwissFundDataQuoteFeed.selectValue(reordered, null), is(new BigDecimal("114.1021")));

        // the more precise value wins even if the rounded value is closer
        assertThat(SwissFundDataQuoteFeed.selectValue(List.of(new BigDecimal("114.102"), new BigDecimal("114.1021")),
                        new BigDecimal("114.1000")), is(new BigDecimal("114.1021")));
    }
}
