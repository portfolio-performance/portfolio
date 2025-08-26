package name.abuchen.portfolio.online.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.time.LocalDate;
// import java.time.LocalDate;
// import java.util.NoSuchElementException;
// import java.util.Optional;
import java.util.Scanner;

import org.junit.Test;
import org.mockito.Mockito;

import name.abuchen.portfolio.model.LatestSecurityPrice;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;

public class TLVQuoteFeedTest
{

    private String getFundDetails()
    {
        return getHistoricalTaseQuotes("response_tase_fund_details01.txt");
    }

    private String getSecurityDetails()
    {
        return getHistoricalTaseQuotes("response_tase_security_details01.txt");
    }

    private String getHistoricalTaseQuotes()
    {
        return getHistoricalTaseQuotes("response_tase_historical01.txt");
    }

    private String getHistoricalTaseQuotesHighLowVolume()
    {
        return getHistoricalTaseQuotes("response_tase_historical02.txt");
    }

    private String getHistoricalTaseQuotesAX()
    {
        return getHistoricalTaseQuotes("tase_australian_quotes.txt");
    }

    private String getHistoricalTaseQuotes(String filename)
    {
        String responseBody = null;
        Scanner scanner = new Scanner(getClass().getResourceAsStream(filename), "UTF-8");
        responseBody = scanner.useDelimiter("\\A").next();
        scanner.close();

        return responseBody;
    }


    @Test
    public void testTLVGetEmptyQuoteValidSecurity() throws IOException
    {

        Security security = new Security();
        security.setTickerSymbol("AAPL");
        security.setCurrencyCode("ILS");
        String response = getSecurityDetails();

        // YahooFinanceQuoteFeed feed = Mockito.spy(new
        // YahooFinanceQuoteFeed());

        TLVQuoteFeed feed = Mockito.spy(new TLVQuoteFeed());
        Mockito.doReturn(response).when(feed).rpcLatestQuote(security);

        // System.out.println(feed.getLatestQuote(security));
        LatestSecurityPrice price = feed.getLatestQuote(security).get();

        // get Date
        // get Volume
        // FundID

        assertTrue(price.getDate().equals(LocalDate.of(2025, 8, 26)));

        assertTrue(price.getHigh() == 118750000);
        assertTrue(price.getLow() == 118500000);
        assertTrue(price.getValue() == 118750000);

    }

    @Test
    public void testTLVGetEmptyQuoteValidFund() throws IOException
    {

        Security security = new Security();
        security.setTickerSymbol("AAPL");
        security.setCurrencyCode("ILS");
        String response = getFundDetails();


        TLVQuoteFeed feed = Mockito.spy(new TLVQuoteFeed());
        Mockito.doReturn(response).when(feed).rpcLatestQuote(security);

        LatestSecurityPrice price = feed.getLatestQuote(security).get();

        // get Date
        // get Volume
        // FundID

        assertTrue(price.getDate().equals(LocalDate.of(2025, 8, 21)));
        assertTrue(price.getHigh() ==LatestSecurityPrice.NOT_AVAILABLE);
        assertTrue(price.getLow() ==LatestSecurityPrice.NOT_AVAILABLE);
        assertTrue(price.getValue() == 155970000);

    }

    @Test
    public void testCalculateDate()
    {

        TLVQuoteFeed feed = new TLVQuoteFeed();

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
}
