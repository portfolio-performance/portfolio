package name.abuchen.portfolio.online.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import name.abuchen.portfolio.model.LatestSecurityPrice;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class AlphavantageQuoteFeedTest
{
    Security security;

    @Before
    public void setup()
    {
        security = new Security();
        security.setTickerSymbol("AAPL");
    }

    @Test
    public void testNoApiKey()
    {
        AlphavantageQuoteFeed feed = new AlphavantageQuoteFeed();
        Optional<LatestSecurityPrice> result = feed.getLatestQuote(security);
        assertThat(result.isEmpty(), is(true));
    }

    @Test
    public void testGetLatestQuoteValid() throws IOException
    {

        // a sample response, aquired through the api
        String response = "timestamp,open,high,low,close,volume\n"
                        + "2020-04-20 16:00:00,277.3800,277.5300,276.8550,276.8550,389622\n"
                        + "2020-04-20 15:59:00,277.2800,277.6599,277.1100,277.3700,243746\n"
                        + "2020-04-20 15:58:00,277.5100,277.5600,277.0800,277.3000,185781";

        AlphavantageQuoteFeed feed = new AlphavantageQuoteFeed();
        AlphavantageQuoteFeed feed1 = Mockito.spy(feed);
        feed1.setApiKey("apiKey");

        // we mock the rpc call to return the above string
        Mockito.doReturn(response).when(feed1).rpcLatestQuote(security);

        LatestSecurityPrice price = feed1.getLatestQuote(security).orElseThrow(IllegalArgumentException::new);

        assertThat(price.getDate(), is(LocalDate.of(2020, 4, 20)));
        assertThat(price.getHigh(), is(Values.Quote.factorize(277.53)));
        assertThat(price.getLow(), is(Values.Quote.factorize(276.855)));
        assertThat(price.getValue(), is(Values.Quote.factorize(276.855)));
        assertThat(price.getVolume(), is(389622L));
    }
}
