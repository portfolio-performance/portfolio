package name.abuchen.portfolio.online.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Scanner;

import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;

import name.abuchen.portfolio.model.LatestSecurityPrice;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.money.Values;


public class TASEQuoteFeedMockTest
{


    // Stock Example - NICE
    // https://market.tase.co.il/en/market_data/security/273011
    private String getSharesDetails()
    {
        return getHistoricalTaseQuotes("response_tase_security_details02.txt");
    }

    private String getHistoricalTaseQuotes(String filename)
    {
        String responseBody = null;
        Scanner scanner = new Scanner(getClass().getResourceAsStream(filename), "UTF-8");
        responseBody = scanner.useDelimiter("\\A").next();
        scanner.close();

        return responseBody;
    }



    @Ignore
    @Test
    public void mocked_share_should_return_latest_quotes()
    {
        Security security = new Security();
        security.setTickerSymbol("NICE");
        security.setCurrencyCode("ILS");
        security.setWkn("273011");

        String mockedresponse = getSharesDetails();
        assertThat(mockedresponse.length(), greaterThan(0));

        Optional<LatestSecurityPrice> mockedprice = null;

        try
        {
            TASEQuoteFeed feed = Mockito.spy(new TASEQuoteFeed());
            Mockito.doReturn(mockedprice).when(feed).getLatestQuote(security);

            // PRice in ILS, Type = Mutual Fund
            Optional<LatestSecurityPrice> price = feed.getLatestQuote(security);

            assertThat(price, not(Optional.empty()));

            LatestSecurityPrice p = price.get();

            assertThat(p.getDate(), is(LocalDate.of(2025, 8, 31)));
            assertThat(p.getHigh(), is(Values.Quote.factorize(471.90)));
            assertThat(p.getLow(), is(Values.Quote.factorize(460.50)));
            assertThat(p.getValue(), is(Values.Quote.factorize(460.50)));

            verify(feed).getLatestQuote(security);
        }
        catch (Exception e)
        {
            System.out.println(e.getMessage());
            assertTrue(false);
        }
    }



    @Test
    public void CalculateDate()
    {

        TASEQuoteFeed feed = new TASEQuoteFeed();

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
