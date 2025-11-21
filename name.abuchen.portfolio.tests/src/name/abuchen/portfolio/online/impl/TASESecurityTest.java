package name.abuchen.portfolio.online.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Scanner;

import org.junit.Test;
import org.mockito.Mockito;

import name.abuchen.portfolio.model.LatestSecurityPrice;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.online.QuoteFeedData;
import name.abuchen.portfolio.online.impl.TASE.TASESecurity;
import name.abuchen.portfolio.online.impl.TASE.jsondata.SecurityHistory;

public class TASESecurityTest
{

    // Government Bond Example - 01135912
    // https://market.tase.co.il/en/market_data/security/1135912/major_data

    private String getBondDetails()
    {
        return getHistoricalTaseQuotes("response_tase_security_bond_details01.txt");
    }

    // Stock Example - NICE
    // https://market.tase.co.il/en/market_data/security/273011
    private String getSharesDetails()
    {
        return getHistoricalTaseQuotes("response_tase_security_share_details01.txt");
            
    }


    // Mutual Fund Example - 5127121
    // https://maya.tase.co.il/en/funds/mutual-funds/5127121
    // returns in ILS
    private String getFundDetails()
    {
        return getHistoricalTaseQuotes("response_tase_fund_details01.txt");
    }

    private String getShareHistory()
    {
        return getHistoricalTaseQuotes("response_tase_security_share_history01.txt");
    }

    private String getSecurityBondHistory()
    {
        return getHistoricalTaseQuotes("response_tase_security_bond_history01.txt");
    }

    private String getHistoricalTaseQuotes(String filename)
    {
        String responseBody = "";
        Scanner scanner = new Scanner(getClass().getResourceAsStream(filename), "UTF-8");
        responseBody = scanner.useDelimiter("\\A").next();
        scanner.close();

        return responseBody;
    }



    @Test
    public void mocked_bond_returns_latest_quotes()
    {
        Security security = new Security();
        security.setCurrencyCode("ILA");
        security.setWkn("01135912"); // Government bond - "GALIL" - CPI1025 -
                                     // Reporting in ILA

        String mockedresponse = getBondDetails();
        assertThat(mockedresponse.length(), greaterThan(0));

        try
        {
            TASESecurity securityfeed = spy(new TASESecurity());
            doReturn(mockedresponse).when(securityfeed).rpcLatestQuoteSecurity(security);

            Optional<LatestSecurityPrice> optionalPrice = securityfeed.getLatestQuote(security);


            assertThat(optionalPrice.isPresent(), is(true));

            LatestSecurityPrice price = optionalPrice.get();
            assertFalse("Date should not be null", price.getDate() == null);
            assertThat(price.getDate(), is(LocalDate.of(2025, 8, 26)));
            assertThat(price.getHigh(), is(Values.Quote.factorize(118.75)));
            assertThat(price.getLow(), is(Values.Quote.factorize(118.50)));
            assertThat(price.getValue(), is(Values.Quote.factorize(118.75)));
            assertThat(price.getDate(), is(LocalDate.of(2025, 8, 26)));
            assertThat(price.getVolume(), is(2919229L));

            verify(securityfeed).rpcLatestQuoteSecurity(security);

        }
        catch (Exception e)
        {
            fail("Exception not expected" + e.getMessage());
        }

    }

    @Test
    public void mocked_fund_does_not_return_latest_quote()
    {
        Security security = new Security();
        security.setCurrencyCode("ILS");
        security.setWkn("5127121"); // Government bond - "GALIL" - CPI1025 -
                                    // Reporting in ILA

        String mockedresponse = getFundDetails();
        assertTrue(mockedresponse.length() > 0);

        try
        {
            TASESecurity securityfeed = spy(new TASESecurity());
            doReturn(mockedresponse).when(securityfeed).rpcLatestQuoteSecurity(security);

            Optional<LatestSecurityPrice> optionalPrice = securityfeed.getLatestQuote(security);

            assertTrue("Expected price to be present", optionalPrice.isEmpty());

            verify(securityfeed).rpcLatestQuoteSecurity(security);

        }
        catch (Exception e)
        {
            fail("Exception not expected" + e.getMessage());
        }

    }

    @Test
    public void mocked_shares_returns_latest_quotes()
    {
        Security security = new Security();
        security.setTickerSymbol("NICE");
        security.setCurrencyCode("ILS");
        security.setWkn("273011"); // NICE Shares - Reported in ILA ???

        String mockedresponse = getSharesDetails();
        assertThat(mockedresponse.length(), greaterThan(0));

        try
        {
            TASESecurity feed = Mockito.spy(new TASESecurity());
            doReturn(mockedresponse).when(feed).rpcLatestQuoteSecurity(security);

            Optional<LatestSecurityPrice> optionalPrice = feed.getLatestQuote(security);


            assertTrue("Expected price to be present", optionalPrice.isPresent());

            LatestSecurityPrice price = optionalPrice.get();
            assertTrue(price.getDate() != null);
            assertThat(price.getDate(), is(LocalDate.of(2025, 8, 31)));
            assertThat(price.getLow(), is(Values.Quote.factorize(460.50)));
            assertThat(price.getHigh(), is(Values.Quote.factorize(471.90)));
            assertThat(price.getValue(), is(Values.Quote.factorize(460.50)));
            assertThat(price.getVolume(), is(50975L));

           
            // Verify interaction
            verify(feed).rpcLatestQuoteSecurity(security);


        }
        catch (Exception e)
        {
            System.out.println(e.getMessage());
            assertTrue(false);

        }
    }




    @Test
    public void mocked_bond_return_Historical_Quotes_though_getHistoricalQuotes()
    {
        Security security = new Security();
        security.setTickerSymbol("AAPL");
        security.setCurrencyCode("ILA");
        security.setWkn("01135912"); // Government bond - "GALIL" - CPI1025 -
        // Reporting in ILA

        String mockedResponse = getSecurityBondHistory();
        assertTrue(mockedResponse.length() > 0);

        TASESecurity feed = Mockito.spy(new TASESecurity());

        // Convert mockedResponse to the QuoteFeedData format expected by
        // getHistoricalQuotes
        SecurityHistory history = SecurityHistory.fromJson(mockedResponse);
        Optional<SecurityHistory> historyopt = Optional.of(history);
        Optional<QuoteFeedData> mockedFeed = feed.convertSecurityHistoryToQuoteFeedData(historyopt, security);

        try
        {

            // Return mockedFeed instead of real feed
            doReturn(mockedFeed).when(feed).getHistoricalQuotes(security, false);


            Optional<QuoteFeedData> feedDataOpt = feed.getHistoricalQuotes(security, false);
            assertFalse("GetHistoricalQoutes feedData should not be empty", feedDataOpt.isEmpty());

            QuoteFeedData feedData = feedDataOpt.get();
            assertFalse("FeeData shoould contain prices", feedData.getPrices().isEmpty());
            assertFalse("FeedData shoould contain latestprices", feedData.getLatestPrices().isEmpty());



            SecurityPrice firstprice = feedData.getPrices().get(0);
            assertThat(firstprice.getDate(), is(LocalDate.of(2025, 9, 1)));
            assertThat(firstprice.getValue(), is(Values.Quote.factorize(118.94)));
            // No Low and High on Price

            LatestSecurityPrice firstlatestprice = feedData.getLatestPrices().get(0);
            assertThat(firstlatestprice.getDate(), is(LocalDate.of(2025, 9, 1)));
            assertThat(firstlatestprice.getValue(), is(Values.Quote.factorize(118.94)));
            assertThat(firstlatestprice.getVolume(), is(313006515L));
            assertThat(firstlatestprice.getHigh(), is(Values.Quote.factorize(118.96)));
            assertThat(firstlatestprice.getLow(), is(Values.Quote.factorize(118.60)));


            SecurityPrice lastprice = feedData.getPrices().get(feedData.getPrices().size() - 1);
            assertThat(lastprice.getDate(), is(LocalDate.of(2025, 8, 28)));
            assertThat(lastprice.getValue(), is(Values.Quote.factorize(118.89)));

            // Verify interaction
            verify(feed).getHistoricalQuotes(security, false);

        }
        catch (Exception e)
        {
            fail("Exception not expected" + e.getMessage());
        }
    }

    @Test
    public void mocked_security_return_Historical_Quotesthough_getHistoricalQuotes()
    {
        Security security = new Security();
        security.setTickerSymbol("AAPL");
        security.setCurrencyCode("ILA");
        security.setWkn(""); // Government bond - "GALIL" - CPI1025 -
        // Reporting in ILA

        String mockedResponse = getSecurityBondHistory();
        assertTrue(mockedResponse.length() > 0);

        TASESecurity feed = Mockito.spy(new TASESecurity());

        SecurityHistory history = SecurityHistory.fromJson(mockedResponse);
        Optional<SecurityHistory> historyopt = Optional.of(history);
        Optional<QuoteFeedData> mockedFeed = feed.convertSecurityHistoryToQuoteFeedData(historyopt, security);

        try
        {

            doReturn(mockedFeed).when(feed).getHistoricalQuotes(security, false);

            Optional<QuoteFeedData> feedDataOpt = feed.getHistoricalQuotes(security, false);

            assertFalse("GetHistoricalQoutes feedData should not be empty", feedDataOpt.isEmpty());

            QuoteFeedData feedData = feedDataOpt.get();

            assertFalse("FeeData shoould contain prices", feedData.getPrices().isEmpty());

            SecurityPrice firstprice = feedData.getPrices().get(0);
            SecurityPrice lastprice = feedData.getPrices().get(feedData.getPrices().size() - 1);

            assertThat(firstprice.getDate(), is(LocalDate.of(2025, 9, 1)));
            assertThat(lastprice.getDate(), is(LocalDate.of(2025, 8, 28)));
            assertThat(firstprice.getValue(), is(Values.Quote.factorize(118.94)));
            assertThat(lastprice.getValue(), is(Values.Quote.factorize(118.89)));

            // Verify interaction
            verify(feed).getHistoricalQuotes(security, false);

        }
        catch (Exception e)
        {
            fail("Exception not expected" + e.getMessage());
        }
    }

    @Test
    public void mocked_shares_return_Historical_Quotes_though_getHistoricalQuotes()
    {
        Security security = new Security();
        security.setTickerSymbol("NICE");
        security.setCurrencyCode("ILA");
        security.setWkn("273011");

        String mockedResponse = getShareHistory();
        assertTrue(mockedResponse.length() > 0);

        TASESecurity feed = Mockito.spy(new TASESecurity());
        SecurityHistory history = SecurityHistory.fromJson(mockedResponse);
        Optional<SecurityHistory> historyopt = Optional.of(history);


        Optional<QuoteFeedData> mockedFeed = feed.convertSecurityHistoryToQuoteFeedData(historyopt, security);

        try
        {

            doReturn(mockedFeed).when(feed).getHistoricalQuotes(security, false);

            Optional<QuoteFeedData> feedDataOpt = feed.getHistoricalQuotes(security, false);
            assertFalse("GetHistoricalQoutes feedData should not be empty", feedDataOpt.isEmpty());

            QuoteFeedData feedData = feedDataOpt.get();
            assertFalse("FeeData shoould contain prices", feedData.getPrices().isEmpty());


            SecurityPrice firstprice = feedData.getPrices().get(0);
            assertThat(firstprice.getDate(), is(LocalDate.of(2024, 11, 10)));
            assertThat(firstprice.getValue(), is(Values.Quote.factorize(114.99)));

            LatestSecurityPrice latestprice = feedData.getLatestPrices().get(0);
            assertThat(latestprice.getDate(), is(LocalDate.of(2024, 11, 10)));
            assertThat(latestprice.getValue(), is(Values.Quote.factorize(114.99)));
            assertThat(latestprice.getHigh(), is(Values.Quote.factorize(115.1)));
            assertThat(latestprice.getLow(), is(Values.Quote.factorize(114.83)));
            assertThat(latestprice.getVolume(), is(12842819L));


            SecurityPrice lastprice = feedData.getPrices().get(feedData.getPrices().size() - 1);
            assertThat(lastprice.getDate(), is(LocalDate.of(2024, 11, 5)));
            assertThat(lastprice.getValue(), is(Values.Quote.factorize(114.86)));

            verify(feed).getHistoricalQuotes(security, false);
        }
        catch (Exception e)
        {
            fail("Exception not expected" + e.getMessage());
        }
    }



}
