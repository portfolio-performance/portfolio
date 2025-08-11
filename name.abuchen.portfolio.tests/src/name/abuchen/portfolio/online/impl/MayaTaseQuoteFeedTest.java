package name.abuchen.portfolio.online.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Scanner;

import org.junit.Test;
import org.mockito.Mockito;

import name.abuchen.portfolio.model.LatestSecurityPrice;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class MayaTaseQuoteFeedTest
{
    private String getHistoricalMayaQuotes()
    {
        return getHistoricalMayaQuotes("response_maya_historical01.txt");
    }

    private String getHistoricalMayaQuotes(String filename)
    {
        String responseBody = null;
        Scanner scanner = new Scanner(getClass().getResourceAsStream(filename), "UTF-8");
        responseBody = scanner.useDelimiter("\\A").next();
        scanner.close();

        return responseBody;
    }

    @Test
    public void testGetLatestQuoteValid() throws IOException
    {
        Security security = new Security();
        security.setTickerSymbol("");
        String response = "{}";

        MayaTaseQuoteFeed feed = Mockito.spy(new MayaTaseQuoteFeed());
        Mockito.doReturn(response).when(feed).rpcLatestQuote(security);

        LatestSecurityPrice price = feed.getLatestQuote(security).get();

        assertThat(price.getDate(), is(LocalDate.of(2023, 7, 13)));
        assertThat(price.getHigh(), is(LatestSecurityPrice.NOT_AVAILABLE));
        assertThat(price.getLow(), is(LatestSecurityPrice.NOT_AVAILABLE));
        assertThat(price.getValue(), is(Values.Quote.factorize(190.54)));
        assertThat(price.getVolume(), is(LatestSecurityPrice.NOT_AVAILABLE));

    }
}
