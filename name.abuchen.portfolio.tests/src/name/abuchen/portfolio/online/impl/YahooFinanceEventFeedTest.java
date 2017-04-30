package name.abuchen.portfolio.online.impl;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.model.SecurityEvent;
import name.abuchen.portfolio.model.Security;

@SuppressWarnings("nls")
public class YahooFinanceEventFeedTest
{
    @Test
    public void testParsingMultipleEvents4Security() throws IOException
    {
        YahooFinanceEventFeed feed = new YahooFinanceEventFeed()
        {
            @Override
            protected InputStream openStream(String url) throws IOException
            {
                return getClass().getResourceAsStream("response_yahoo_events.txt");
            }
        };

        Security security = new Security("Commerzbank AG", "DE000CBK1001", "CBK.DE", YahooFinanceEventFeed.ID); 

        List<Exception> errors = new ArrayList<Exception>();
        feed.update(security, errors);
        //if (!errors.isEmpty())
        //    System.err.println("YahooFinanceEventTest.testParsingMultipleEvents4Security - errors: " + errors.toString());

        assertThat(errors.size(), is(0));

        assertThat(security.getEvents().size(), is(10));

        assertThat(security.getEvents().get(0).getType(), //
                        equalTo(SecurityEvent.Type.STOCK_DIVIDEND));

        assertThat(security.getEvents().get(1), //
                        is(new SecurityEvent(LocalDate.of(2013, Month.APRIL, 24), SecurityEvent.Type.STOCK_SPLIT).setRatio((double) 1.0, (double) 10.0)));

        assertThat(security.getEvents().get(3), //
                        is(new SecurityEvent(LocalDate.of(2007, Month.MAY, 17), SecurityEvent.Type.STOCK_DIVIDEND).setAmount("EUR", BigDecimal.valueOf((double) 1.018680))));

        assertThat(security.getEvents().get(security.getEvents().size() - 1), // 
                        is(new SecurityEvent(LocalDate.of(2000, Month.MAY, 29), SecurityEvent.Type.STOCK_DIVIDEND).setAmount("EUR", (double) 1.086590)));
    }

    @Test
    public void testForInvalidCurrencyFromYahoo() throws IOException
    {
        YahooFinanceEventFeed feed = new YahooFinanceEventFeed()
        {
            @Override
            protected InputStream openStream(String url) throws IOException
            {
                return new ByteArrayInputStream("\"DAI.DE\",3.47,\"3/30/2017\",N/A,4.94,\"-0.40 - -0.60%\"" 
                                .getBytes(StandardCharsets.UTF_8));
            }
        };


        List<Security> securities = new ArrayList<Security>();

        Security adidas = new Security("Adidas", "DE000A1EWWW0", "ADS.DE", YahooFinanceQuoteFeed.ID);
        securities.add(adidas);
        securities.add(new Security("Daimler AG", "DE0007100000", "DAI.DE", YahooFinanceQuoteFeed.ID));

        List<Exception> errors = new ArrayList<Exception>();
        feed.update(securities, errors);

        assertThat(errors.size(), is(1));
        assertThat(errors.get(0).getMessage(), containsString(adidas.getTickerSymbol()));

        // not first, but second security must have value
        assertThat(securities.get(0).getEvents().size(), is(0));
        assertThat(securities.get(1).getEvents().size(), is(1));
        assertThat(securities.get(1).getEvents().get(0), // 
                        is(new SecurityEvent(LocalDate.of(2017, Month.MARCH, 30), SecurityEvent.Type.STOCK_DIVIDEND).setAmount(Messages.LabelNoCurrencyCode, (double) 3.47)));

    }

    @Test
    public void testForPayANDExDateFromYahoo() throws IOException
    {
        YahooFinanceEventFeed feed = new YahooFinanceEventFeed()
        {
            @Override
            protected InputStream openStream(String url) throws IOException
            {
                return new ByteArrayInputStream("\"IBM\",5.60,\"2/8/2017\",\"3/10/2017\",3.30,\"USD\"" 
                                .getBytes(StandardCharsets.UTF_8));
            }
        };

        List<Security> securities = new ArrayList<Security>();
        securities.add(new Security("International Business Machines Corporation", "US4592001014", "IBM", YahooFinanceQuoteFeed.ID));
        Security commerzbank = new Security("Commerzbank AG", "DE000CBK1001", "CBK.DE", YahooFinanceEventFeed.ID);
        securities.add(commerzbank);

        List<Exception> errors = new ArrayList<Exception>();
        feed.update(securities, errors);
//        if (!errors.isEmpty())
//            System.err.println("YahooFinanceEventTest.testForMissingEventsFromYahoo - errors: " + errors.toString());
//        System.err.println("YahooFinanceEventTest.testForMissingEventsFromYahoo - securities: " + securities.toString());
//        System.err.println("YahooFinanceEventTest.testForMissingEventsFromYahoo - events(0): " + securities.get(0).getEvents().toString());
//        System.err.println("YahooFinanceEventTest.testForMissingEventsFromYahoo - events(1): " + securities.get(1).getEvents().toString());

        assertThat(errors.size(), is(1));
        assertThat(errors.get(0).getMessage(), containsString(commerzbank.getTickerSymbol()));

        // not first, but second security must have value
        assertThat(securities.get(0).getEvents().size(), is(1));
        assertThat(securities.get(0).getEvents().get(0), //
                        is(new SecurityEvent(LocalDate.of(2017, Month.MARCH, 10), SecurityEvent.Type.STOCK_DIVIDEND).setAmount("USD", (double) 5.60).setExDate(LocalDate.of(2017, Month.FEBRUARY, 8))));
        assertThat(securities.get(1).getEvents().size(), is(0));

    }
    
}
