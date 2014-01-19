package name.abuchen.portfolio.online.impl;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.containsString;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import name.abuchen.portfolio.model.LatestSecurityPrice;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.util.Dates;

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

        Calendar fiveYearsAgo = Calendar.getInstance();
        fiveYearsAgo.setTime(Dates.today()); // no milliseconds
        fiveYearsAgo.add(Calendar.YEAR, -5);

        Calendar cal = feed.caculateStart(security);
        assertThat(cal, equalTo(fiveYearsAgo));

        security.addPrice(new SecurityPrice(Dates.today(), 100));
        cal = feed.caculateStart(security);
        assertThat(cal.getTime(), equalTo(Dates.today()));
    }

    @Test
    public void testParsingLatestQuotes() throws IOException
    {
        YahooFinanceQuoteFeed feed = new YahooFinanceQuoteFeed()
        {
            @Override
            protected InputStream openStream(String url) throws MalformedURLException, IOException
            {
                return getClass().getResourceAsStream("response_yahoo_quotes.txt");
            }
        };

        ArrayList<Security> securities = new ArrayList<Security>();
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
        assertThat(latest.getTime(), equalTo(Dates.date(2011, Calendar.SEPTEMBER, 29)));
        assertThat(latest.getHigh(), is(1375L));
        assertThat(latest.getLow(), is(1370L));
        assertThat(latest.getVolume(), is(10037));
        assertThat(latest.getPreviousClose(), is(1271L));

        latest = securities.get(1).getLatest();
        assertThat(latest.getHigh(), is(-1L));
        assertThat(latest.getLow(), is(-1L));
        assertThat(latest.getVolume(), is(-1));

        latest = securities.get(3).getLatest();
        assertThat(latest.getTime(), equalTo(Dates.today()));
    }

    @Test
    public void testForMissingQuotesFromYahoo() throws IOException
    {
        YahooFinanceQuoteFeed feed = new YahooFinanceQuoteFeed()
        {
            @Override
            protected InputStream openStream(String url) throws MalformedURLException, IOException
            {
                return new ByteArrayInputStream("\"ADS.DE\",49.20,\"9/1/2011\",N/A,N/A,48.66,N/A" //
                                .getBytes(Charset.forName("UTF-8")));
            }
        };

        Security daimler = new Security("Daimler AG", "DE0007100000", "DAI.DE", YahooFinanceQuoteFeed.ID);
        Security adidas = new Security("Adidas", "DE000A1EWWW0", "ADS.DE", YahooFinanceQuoteFeed.ID);

        ArrayList<Security> securities = new ArrayList<Security>();
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
                        equalTo(new SecurityPrice(Dates.date(2003, Calendar.JANUARY, 1), 2935)));

        assertThat(security.getPrices().get(security.getPrices().size() - 1),
                        equalTo(new SecurityPrice(Dates.date(2011, Calendar.SEPTEMBER, 22), 3274)));
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
                        equalTo(new SecurityPrice(Dates.date(2003, Calendar.JANUARY, 1), 2255)));

        assertThat(security.getPrices().get(security.getPrices().size() - 1),
                        equalTo(new SecurityPrice(Dates.date(2011, Calendar.SEPTEMBER, 22), 3274)));
    }

}
