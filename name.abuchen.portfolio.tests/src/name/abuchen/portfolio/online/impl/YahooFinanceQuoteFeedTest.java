package name.abuchen.portfolio.online.impl;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import name.abuchen.portfolio.model.Exchange;
import name.abuchen.portfolio.model.LatestSecurityPrice;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.money.Values;

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
    public void testParsingLatestQuotes() throws IOException
    {
        YahooFinanceQuoteFeed feed = new YahooFinanceQuoteFeed()
        {
            @Override
            protected InputStream openStream(String url) throws IOException
            {
                return getClass().getResourceAsStream("response_yahoo_quotes.txt");
            }
        };

        List<Security> securities = new ArrayList<Security>();
        securities.add(new Security("Daimler AG", "DE0007100000", "DAI.DE", YahooFinanceQuoteFeed.ID));
        securities.add(new Security("Adidas", "DE000A1EWWW0", "ADS.DE", YahooFinanceQuoteFeed.ID));
        securities.add(new Security("Daimler AG", "DE0007100000", "BAYN.DE", YahooFinanceQuoteFeed.ID));
        securities.add(new Security("Daimler AG", "DE0007100000", "BMW.DE", YahooFinanceQuoteFeed.ID));
        securities.add(new Security("Daimler AG", "DE0007100000", "CBK.DE", YahooFinanceQuoteFeed.ID));

        List<Exception> errors = new ArrayList<Exception>();
        feed.updateLatestQuotes(securities, errors);
        assertThat(errors.size(), is(0));

        LatestSecurityPrice latest = securities.get(0).getLatest();
        assertThat(latest.getValue(), is(Values.Quote.factorize(13.71)));
        assertThat(latest.getTime(), equalTo(LocalDate.of(2011, Month.SEPTEMBER, 29)));
        assertThat(latest.getHigh(), is(Values.Quote.factorize(13.75)));
        assertThat(latest.getLow(), is(Values.Quote.factorize(13.70)));
        assertThat(latest.getVolume(), is(10037L));
        assertThat(latest.getPreviousClose(), is(Values.Quote.factorize(12.71)));

        latest = securities.get(1).getLatest();
        assertThat(latest.getHigh(), is(-1L));
        assertThat(latest.getLow(), is(-1L));
        assertThat(latest.getVolume(), is(-1L));

        latest = securities.get(3).getLatest();
        assertThat(latest.getTime(), equalTo(LocalDate.now()));
    }

    @Test
    public void testForMissingQuotesFromYahoo() throws IOException
    {
        YahooFinanceQuoteFeed feed = new YahooFinanceQuoteFeed()
        {
            @Override
            protected InputStream openStream(String url) throws IOException
            {
                return new ByteArrayInputStream("\"ADS.DE\",49.20,\"9/1/2011\",N/A,N/A,48.66,N/A" //
                                .getBytes(StandardCharsets.UTF_8));
            }
        };

        Security daimler = new Security("Daimler AG", "DE0007100000", "DAI.DE", YahooFinanceQuoteFeed.ID);
        Security adidas = new Security("Adidas", "DE000A1EWWW0", "ADS.DE", YahooFinanceQuoteFeed.ID);

        List<Security> securities = new ArrayList<Security>();
        securities.add(daimler);
        securities.add(adidas);

        List<Exception> errors = new ArrayList<Exception>();
        feed.updateLatestQuotes(securities, errors);

        // not first, but second security must have value
        LatestSecurityPrice latest = adidas.getLatest();
        assertThat(latest.getValue(), is(Values.Quote.factorize(49.20)));

        assertThat(errors.size(), is(1));

        assertThat(errors.get(0).getMessage(), containsString(daimler.getTickerSymbol()));
    }

    @Test
    public void testParsingHistoricalQuotes() throws IOException
    {
        YahooFinanceQuoteFeed feed = new YahooFinanceQuoteFeed()
        {
            @Override
            protected InputStream openStream(String wknUrl) throws IOException
            {
                return getClass().getResourceAsStream("response_yahoo_historical.txt");
            }
        };

        Security security = new Security();
        security.setTickerSymbol("DAI.DE");

        feed.updateHistoricalQuotes(security, new ArrayList<Exception>());

        assertThat(security.getPrices().size(), is(2257));

        assertThat(security.getPrices().get(0), //
                        equalTo(new SecurityPrice(LocalDate.of(2003, Month.JANUARY, 1),
                                        Values.Quote.factorize(29.35))));

        assertThat(security.getPrices().get(security.getPrices().size() - 1), equalTo(
                        new SecurityPrice(LocalDate.of(2011, Month.SEPTEMBER, 22), Values.Quote.factorize(32.74))));
    }

    @Test
    public void testParsingHistoricalAdjustedCloseQuotes() throws IOException
    {
        YahooFinanceAdjustedCloseQuoteFeed feed = new YahooFinanceAdjustedCloseQuoteFeed()
        {
            @Override
            protected InputStream openStream(String wknUrl) throws IOException
            {
                return getClass().getResourceAsStream("response_yahoo_historical.txt");
            }
        };

        Security security = new Security();
        security.setTickerSymbol("DAI.DE");

        feed.updateHistoricalQuotes(security, new ArrayList<Exception>());

        assertThat(security.getPrices().size(), is(2257));

        assertThat(security.getPrices().get(0), //
                        equalTo(new SecurityPrice(LocalDate.of(2003, Month.JANUARY, 1),
                                        Values.Quote.factorize(22.55))));

        assertThat(security.getPrices().get(security.getPrices().size() - 1), equalTo(
                        new SecurityPrice(LocalDate.of(2011, Month.SEPTEMBER, 22), Values.Quote.factorize(32.74))));
    }

    @Test
    public void testThatAtLeastTheGivenExchangeIsReturned() throws IOException
    {
        YahooFinanceQuoteFeed feed = new YahooFinanceQuoteFeed()
        {
            @Override
            protected void searchSymbols(List<Exchange> answer, String query) throws IOException
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
