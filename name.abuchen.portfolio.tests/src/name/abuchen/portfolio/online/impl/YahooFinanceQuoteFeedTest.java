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

import name.abuchen.portfolio.model.Exchange;
import name.abuchen.portfolio.model.LatestSecurityPrice;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import org.junit.Test;

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

        LocalDate fiveYearsAgo = LocalDate.now().minusYears(5);

        LocalDate date = feed.caculateStart(security);
        assertThat(date, equalTo(fiveYearsAgo));

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
        assertThat(latest.getValue(), is(1371L));
        assertThat(latest.getTime(), equalTo(LocalDate.of(2011, Month.SEPTEMBER, 29)));
        assertThat(latest.getHigh(), is(1375L));
        assertThat(latest.getLow(), is(1370L));
        assertThat(latest.getVolume(), is(10037));
        assertThat(latest.getPreviousClose(), is(1271L));

        latest = securities.get(1).getLatest();
        assertThat(latest.getHigh(), is(-1L));
        assertThat(latest.getLow(), is(-1L));
        assertThat(latest.getVolume(), is(-1));

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
        assertThat(latest.getValue(), is(4920L));

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
                        equalTo(new SecurityPrice(LocalDate.of(2003, Month.JANUARY, 1), 2935)));

        assertThat(security.getPrices().get(security.getPrices().size() - 1),
                        equalTo(new SecurityPrice(LocalDate.of(2011, Month.SEPTEMBER, 22), 3274)));
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
                        equalTo(new SecurityPrice(LocalDate.of(2003, Month.JANUARY, 1), 2255)));

        assertThat(security.getPrices().get(security.getPrices().size() - 1),
                        equalTo(new SecurityPrice(LocalDate.of(2011, Month.SEPTEMBER, 22), 3274)));
    }

    @Test
    public void testParsingExchanges() throws IOException
    {
        YahooFinanceQuoteFeed feed = new YahooFinanceQuoteFeed()
        {
            @Override
            protected InputStream openStream(String url) throws IOException
            {
                return new ByteArrayInputStream(
                                ("YAHOO.Finance.SymbolSuggest.ssCallback({\"ResultSet\":{"
                                                + "\"Query\":\"bas.\","
                                                + "\"Result\":[{\"symbol\":\"BAS.DE\",\"name\": \"BASF SE\",\"exch\": \"GER\",\"type\": \"S\",\"exchDisp\":\"XETRA\",\"typeDisp\":\"Equity\"},"
                                                + "{\"symbol\":\"BAS.F\",\"name\": \"BASF N\",\"exch\": \"FRA\",\"type\": \"S\",\"exchDisp\":\"Frankfurt\",\"typeDisp\":\"Equity\"},"
                                                + "{\"symbol\":\"BAS.AX\",\"name\": \"Bass Strait Oil Company Ltd\",\"exch\": \"ASX\",\"type\": \"S\",\"exchDisp\":\"Australian\",\"typeDisp\":\"Equity\"},"
                                                + "{\"symbol\":\"BAS.HM\",\"name\": \"BASF N\",\"exch\": \"HAM\",\"type\": \"S\",\"exchDisp\":\"Hamburg\",\"typeDisp\":\"Equity\"},"
                                                + "{\"symbol\":\"BAS.SG\",\"name\": \"BASF N\",\"exch\": \"STU\",\"type\": \"S\",\"exchDisp\":\"Stuttgart\",\"typeDisp\":\"Equity\"},"
                                                + "{\"symbol\":\"BAS.SW\",\"name\": \"BASF N\",\"exch\": \"EBS\",\"type\": \"S\",\"exchDisp\":\"Swiss\",\"typeDisp\":\"Equity\"},"
                                                + "{\"symbol\":\"BAS.HA\",\"name\": \"BASF N\",\"exch\": \"HAN\",\"type\": \"S\",\"exchDisp\":\"Hanover\",\"typeDisp\":\"Equity\"},"
                                                + "{\"symbol\":\"BAS.MU\",\"name\": \"BASF N\",\"exch\": \"MUN\",\"type\": \"S\",\"exchDisp\":\"Munich\",\"typeDisp\":\"Equity\"},"
                                                + "{\"symbol\":\"BAS.BR\",\"name\": \"BASILIX\",\"exch\": \"BRU\",\"type\": \"S\",\"exchDisp\":\"Brussels Stock Exchange \",\"typeDisp\":\"Equity\"},"
                                                + "{\"symbol\":\"BAS.BE\",\"name\": \"BASF N\",\"exch\": \"BER\",\"type\": \"S\",\"exchDisp\":\"Berlin\",\"typeDisp\":\"Equity\"}]}})")
                                                .getBytes(StandardCharsets.UTF_8));
            }
        };

        Security s = new Security();
        s.setTickerSymbol("BAS.DE");
        List<Exchange> exchanges = feed.getExchanges(s, new ArrayList<Exception>());

        assertThat(exchanges.size(), is(10));
        assertThat(exchanges.get(0).getId(), is("BAS.DE"));
    }

    @Test
    public void testThatAtLeastTheGivenExchangeIsReturned() throws IOException
    {
        YahooFinanceQuoteFeed feed = new YahooFinanceQuoteFeed()
        {
            @Override
            protected InputStream openStream(String url) throws IOException
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
