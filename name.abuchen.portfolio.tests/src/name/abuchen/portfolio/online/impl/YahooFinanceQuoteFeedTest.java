package name.abuchen.portfolio.online.impl;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Stream;

import org.junit.Test;

import name.abuchen.portfolio.model.Exchange;
import name.abuchen.portfolio.model.LatestSecurityPrice;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.online.QuoteFeedData;

@SuppressWarnings("nls")
public class YahooFinanceQuoteFeedTest
{
    @Test
    public void testCalculateDate() throws IOException
    {
        YahooFinanceQuoteFeed feed = new YahooFinanceQuoteFeed();

        Security security = new Security();
        security.setName("Daimler AG");
        security.setIsin("DE0007100000");
        security.setTickerSymbol("DAI.DE");

        LocalDate nineteenHundred = LocalDate.of(1900, 1, 1);

        LocalDate date = feed.caculateStart(security);
        assertThat(date, equalTo(nineteenHundred));

        security.addPrice(new SecurityPrice(LocalDate.now(), 100));
        date = feed.caculateStart(security);
        assertThat(date, equalTo(LocalDate.now()));
    }
    
    @Test
    public void testParsingHistoricalQuotes()
    {
        String responseBody = null;
        try (Scanner scanner = new Scanner(getClass().getResourceAsStream("response_yahoo_historical.txt"), "UTF-8"))
        {
            responseBody = scanner.useDelimiter("\\A").next();
        }

        Security security = new Security();
        security.setTickerSymbol("DAI.DE");

        YahooFinanceQuoteFeed feed = new YahooFinanceQuoteFeed();
        QuoteFeedData data = feed.extractQuotes(responseBody);
        List<LatestSecurityPrice> prices = data.getLatestPrices();
        Collections.sort(prices, new SecurityPrice.ByDate());

        assertThat(prices.size(), is(123));

        LatestSecurityPrice price = new LatestSecurityPrice(LocalDate.of(2017, Month.NOVEMBER, 27), //
                        Values.Quote.factorize(188.55), //
                        0, //
                        0, //
                        0);
        assertThat(prices.get(0), equalTo(price));

        price = new LatestSecurityPrice(LocalDate.of(2018, Month.MAY, 25), //
                        Values.Quote.factorize(188.3), //
                        0, //
                        0, //
                        0);
        assertThat(prices.get(prices.size() - 1), equalTo(price));
    }

    @Test
    public void testParsingHistoricalAdjustedCloseQuotes() throws IOException
    {
        String responseBody = null;
        try (Scanner scanner = new Scanner(getClass().getResourceAsStream("response_yahoo_historical.txt"), "UTF-8"))
        {
            responseBody = scanner.useDelimiter("\\A").next();
        }

        Security security = new Security();
        security.setTickerSymbol("DAI.DE");

        YahooFinanceAdjustedCloseQuoteFeed feed = new YahooFinanceAdjustedCloseQuoteFeed();
        QuoteFeedData data = feed.extractQuotes(responseBody);
        List<LatestSecurityPrice> prices = data.getLatestPrices();
        Collections.sort(prices, new SecurityPrice.ByDate());

        assertThat(prices.size(), is(123));

        LatestSecurityPrice price = new LatestSecurityPrice(LocalDate.of(2017, Month.NOVEMBER, 27), //
                        Values.Quote.factorize(180.344), //
                        0, //
                        0, //
                        0);
        assertThat(prices.get(0), equalTo(price));

        price = new LatestSecurityPrice(LocalDate.of(2018, Month.MAY, 25), //
                        Values.Quote.factorize(188.3), //
                        0, //
                        0, //
                        0);
        assertThat(prices.get(prices.size() - 1), equalTo(price));
    }

    @Test
    public void testThatAtLeastTheGivenExchangeIsReturned() throws IOException
    {
        YahooFinanceQuoteFeed feed = new YahooFinanceQuoteFeed()
        {
            @Override
            protected Stream<YahooSymbolSearch.Result> searchSymbols(String query) throws IOException
            {
                throw new IOException();
            }
        };

        Security s = new Security();
        s.setTickerSymbol("BAS.DE");

        ArrayList<Exception> errors = new ArrayList<Exception>();
        List<Exchange> exchanges = feed.getExchanges(s, errors);

        assertThat(exchanges.size(), is(1));
        assertThat(exchanges.get(0).getId(), is("BAS.DE"));

        assertThat(errors.size(), is(1));
    }

}
