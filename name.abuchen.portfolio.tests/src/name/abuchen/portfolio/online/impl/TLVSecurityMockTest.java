package name.abuchen.portfolio.online.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
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
import name.abuchen.portfolio.online.QuoteFeedData;
import name.abuchen.portfolio.online.impl.TLVMarket.TLVSecurity;
import name.abuchen.portfolio.online.impl.TLVMarket.jsondata.SecurityHistory;

public class TLVSecurityMockTest
{

    // Government Bond Example - 01135912
    // https://market.tase.co.il/en/market_data/security/1135912/major_data
    private String getBondDetails()
    {
        return getHistoricalTaseQuotes("response_tase_security_details01.txt");
    }

    // Stock Example - NICE
    // https://market.tase.co.il/en/market_data/security/273011
    private String getSharesDetails()
    {
        return getHistoricalTaseQuotes("response_tase_security_details02.txt");
    }

    private String getSecurityDetails()
    {
        return "";
    }

    // Mutual Fund Example - 5127121
    // https://maya.tase.co.il/en/funds/mutual-funds/5127121
    // returns in ILS
    private String getFundDetails()
    {
        return getHistoricalTaseQuotes("response_tase_fund_details01.txt");
    }

    private String getSecurityHistory()
    {
        return getHistoricalTaseQuotes("response_tase_security_history01.txt");
    }

    private String getSecurityHistory2()
    {
        return getHistoricalTaseQuotes("response_tase_security_history02.txt");
    }

    private String getHistoricalTaseQuotes(String filename)
    {
        String responseBody = null;
        Scanner scanner = new Scanner(getClass().getResourceAsStream(filename), "UTF-8");
        responseBody = scanner.useDelimiter("\\A").next();
        scanner.close();

        return responseBody;
    }

    @Ignore("Live Test")
    @Test
    public void mocked_security_returns_latest_quotes()
    {
        Security security = new Security();
        security.setCurrencyCode("ILS");
        security.setWkn("1135912");

        String mockedresponse = getSecurityDetails();
        assertTrue(mockedresponse.length() > 0);

        try
        {
            TLVSecurity securityfeed = spy(new TLVSecurity());
            doReturn(mockedresponse).when(securityfeed).rpcLatestQuoteSecurity(security);


            Optional<LatestSecurityPrice> optionalPrice = securityfeed.getLatestQuote(security);

            // Assert
            assertTrue("Expected price to be present", optionalPrice.isPresent());

            LatestSecurityPrice price = optionalPrice.get();
            assertFalse("Date should not be null", price.getDate() == null);
            // assertEquals("High price mismatch", 118720000L, price.getHigh());
            assertThat(price.getHigh(), is(Values.Quote.factorize(118.72)));
            assertThat(price.getLow(), is(Values.Quote.factorize(118.50)));
            assertThat(price.getDate(), is(LocalDate.of(2025, 8, 25)));

            // assertEquals("Low price mismatch", 11850000L, price.getLow());
            // assertEquals("Value mismatch", 11875000L, price.getValue());
            // assertEquals("Date mismatch", LocalDate.of(2025, 8, 25),
            // price.getDate());

            // Verify interaction
            verify(securityfeed).rpcLatestQuoteSecurity(security);

        }
        catch (Exception e)
        {
            assert (false);
        }

    }

    @Test
    public void mocked_bond_returns_latest_quotes()
    {
        Security security = new Security();
        security.setCurrencyCode("ILA");
        security.setWkn("01135912"); // Government bond - "GALIL" - CPI1025 -
                                     // Reporting in ILA

        String mockedresponse = getBondDetails();
        assertTrue(mockedresponse.length() > 0);

        try
        {
            TLVSecurity securityfeed = spy(new TLVSecurity());
            doReturn(mockedresponse).when(securityfeed).rpcLatestQuoteSecurity(security);

            Optional<LatestSecurityPrice> optionalPrice = securityfeed.getLatestQuote(security);

            // Assert
            assertTrue("Expected price to be present", optionalPrice.isPresent());

            LatestSecurityPrice price = optionalPrice.get();
            assertFalse("Date should not be null", price.getDate() == null);
            assertThat(price.getHigh(), is(Values.Quote.factorize(118.75)));
            assertThat(price.getLow(), is(Values.Quote.factorize(118.50)));
            assertThat(price.getValue(), is(Values.Quote.factorize(118.75)));
            assertThat(price.getDate(), is(LocalDate.of(2025, 8, 26)));
            // assertEquals("Date mismatch", LocalDate.of(2025, 8, 26),
            // price.getDate());

            // Verify interaction
            verify(securityfeed).rpcLatestQuoteSecurity(security);

        }
        catch (Exception e)
        {
            assert (false);
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
            TLVSecurity securityfeed = spy(new TLVSecurity());
            doReturn(mockedresponse).when(securityfeed).rpcLatestQuoteSecurity(security);

            Optional<LatestSecurityPrice> optionalPrice = securityfeed.getLatestQuote(security);

            // Assert
            assertTrue("Expected price to be present", optionalPrice.isEmpty());



            // Verify interaction
            verify(securityfeed).rpcLatestQuoteSecurity(security);

        }
        catch (Exception e)
        {
            assert (false);
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
            TLVSecurity feed = Mockito.spy(new TLVSecurity());
            doReturn(mockedresponse).when(feed).rpcLatestQuoteSecurity(security);

            Optional<LatestSecurityPrice> optionaPrice = feed.getLatestQuote(security);

            // Assert
            assertTrue("Expected price to be present", optionaPrice.isPresent());

            LatestSecurityPrice price = optionaPrice.get();
            assertTrue(price.getDate() != null);
            assertThat(price.getLow(), is(Values.Quote.factorize(460.50)));
            assertThat(price.getHigh(), is(Values.Quote.factorize(471.90)));
            assertThat(price.getValue(), is(Values.Quote.factorize(460.50)));

            assertEquals("Date mismatch", LocalDate.of(2025, 8, 31), price.getDate());

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

        String mockedResponse = getSecurityHistory2();
        assertTrue(mockedResponse.length() > 0);

        SecurityHistory history = SecurityHistory.fromJson(mockedResponse);
        Optional<SecurityHistory> historyopt = Optional.of(history);

        TLVSecurity feed = Mockito.spy(new TLVSecurity());

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


        }
        catch (Exception e)
        {
            System.out.println(e.getMessage());
            assertTrue(false);
        }
    }

    @Ignore
    @Test
    public void mocked_security_return_Historical_Quotesthough_getHistoricalQuotes()
    {
        Security security = new Security();
        security.setTickerSymbol("AAPL");
        security.setCurrencyCode("ILA");
        security.setWkn(""); // Government bond - "GALIL" - CPI1025 -
        // Reporting in ILA

        String mockedResponse = getSecurityHistory2();
        assertTrue(mockedResponse.length() > 0);

        SecurityHistory history = SecurityHistory.fromJson(mockedResponse);
        Optional<SecurityHistory> historyopt = Optional.of(history);

        TLVSecurity feed = Mockito.spy(new TLVSecurity());

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

        }
        catch (Exception e)
        {
            System.out.println(e.getMessage());
            assertTrue(false);
        }
    }

    @Ignore
    @Test
    public void mocked_shares_return_Historical_Quotes_though_getHistoricalQuotes()
    {
        Security security = new Security();
        security.setTickerSymbol("NICE");
        security.setCurrencyCode("ILA");
        security.setWkn("273011");

        String mockedResponse = getSecurityHistory2();
        assertTrue(mockedResponse.length() > 0);

        SecurityHistory history = SecurityHistory.fromJson(mockedResponse);
        Optional<SecurityHistory> historyopt = Optional.of(history);

        TLVSecurity feed = Mockito.spy(new TLVSecurity());

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

        }
        catch (Exception e)
        {
            System.out.println(e.getMessage());
            assertTrue(false);
        }
    }
}
