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
import name.abuchen.portfolio.online.impl.TLVMarket.TLVSecurity;
import name.abuchen.portfolio.online.impl.TLVMarket.jsondata.SecurityHistory;

public class TLVSecurityMockTest
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
            TLVSecurity securityfeed = spy(new TLVSecurity());
            doReturn(mockedresponse).when(securityfeed).rpcLatestQuoteSecurity(security);

            Optional<LatestSecurityPrice> optionalPrice = securityfeed.getLatestQuote(security);

            //@formatter:off
            /*
             * 
                "BaseRate": 118.72,
                "HighRate": 118.75,
                "LowRate": 118.5,
                "OpenRate": 118.5,
                "InDay": 1,
                "ShareType": "0406",
                "TradeDataLink": null,
                "EODTradeDate": "25/08/2025",
                "OverallTurnOverUnits": 2919229,
                "MarketValue": 13124707,
                "LastRate": 118.75,
                "TradeDate": "26/08/2025",
             */
            //@formatter:on
            assertThat(optionalPrice.isPresent(), is(true));

            LatestSecurityPrice price = optionalPrice.get();
            assertFalse("Date should not be null", price.getDate() == null);
            assertThat(price.getDate(), is(LocalDate.of(2025, 8, 26)));
            assertThat(price.getHigh(), is(Values.Quote.factorize(118.75)));
            assertThat(price.getLow(), is(Values.Quote.factorize(118.50)));
            assertThat(price.getValue(), is(Values.Quote.factorize(118.75)));
            assertThat(price.getDate(), is(LocalDate.of(2025, 8, 26)));
            assertThat(price.getVolume(), is(2919229L));

            // Verify interaction
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
            TLVSecurity feed = Mockito.spy(new TLVSecurity());
            doReturn(mockedresponse).when(feed).rpcLatestQuoteSecurity(security);

            Optional<LatestSecurityPrice> optionalPrice = feed.getLatestQuote(security);

            // Assert
            //@formatter:off
            /*
             *  BaseRate": 47440.00,
                "HighRate": 47190.00,
                "LowRate": 46050.00,
                "OpenRate": 47100.00,
                "InDay": 0,
                "ShareType": "0101",
                "TradeDataLink": null,
                "EODTradeDate": "31/08/2025",
                "TurnOverValueShekel": 23829007.00,
                "TurnOverValue": null,
                "MarketValue": 28432054,
                "LastRate": 46050.00,
                "OverallTurnOverUnits": 50975,
             */
            //@formatter:on
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
            assertFalse("FeedData shoould contain latestprices", feedData.getLatestPrices().isEmpty());

            //@formatter:off
            /*
             * "TradeDate": "01/09/2025",
            "Change": 0.01,
            "BaseRate": 118.9300,
            "OpenRate": 118.6000,
            "CloseRate": 118.9400,
            "HighRate": 118.9600,
            "LowRate": 118.6000,
            "MarketValue": 13149028,
            "RegisteredCapital": 11055177366,
            "TurnOverValueShekel": 372343043.0,
            "OverallTurnOverUnits": 313006515,
            "DealsNo": 90,
            "Exe": "",
            "AdjustmentCoefficient": null,
            "ExeDesc": "",
            "IANS": 11055177366.0,
            "IndexAdjustedFreeFloat": 100.00,
            "LastIANSUpdate": "31/08/2025",
            "TradeDateEOD": null,
            "AdjustmentRate": 118.9400,
            "BrutoYield": 0.00,
            "IfTraded": true,
            "ShareTradingStatus": null,
            "IsOfferingPrice": false,
            "AdjustmentRateDesc": "Closing Price"
             */
            //@formatter:on

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

            //@formatter:off
            /*
             * "TradeDate": "28/08/2025",
            "Change": 0.09,
            "BaseRate": 118.7800,
            "OpenRate": 118.7000,
            "CloseRate": 118.8900,
            "HighRate": 118.9000,
            "LowRate": 118.7000,
            "MarketValue": 13143500,
            "RegisteredCapital": 11055177366,
            "TurnOverValueShekel": 417530707.0,
            "OverallTurnOverUnits": 351193156,
            "DealsNo": 135,
            "Exe": "",
            "AdjustmentCoefficient": null,
            "ExeDesc": "",
            "IANS": 12682986802.0,
            "IndexAdjustedFreeFloat": 100.00,
            "LastIANSUpdate": "31/07/2025",
            "TradeDateEOD": null,
            "AdjustmentRate": 118.8900,
            "BrutoYield": 0.00,
            "IfTraded": true,
            "ShareTradingStatus": null,
            "IsOfferingPrice": false,
            "AdjustmentRateDesc": "Closing Price"
             */
                            
            //@formatter:on
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

            //@formatter:off
            /*
             * 
            "TradeDate": "10/11/2024",
            "Change": 0,
            "BaseRate": 114.99,
            "OpenRate": 114.99,
            "CloseRate": 114.99,
            "HighRate": 115.1,
            "LowRate": 114.83,
            "MarketValue": 24955967,
            "RegisteredCapital": 21702727840,
            "TurnOverValueShekel": 14761943,
            "OverallTurnOverUnits": 12842819,
            "DealsNo": 153,
            "Exe": "",
            "AdjustmentCoefficient": null,
            "ExeDesc": "",
            "IANS": 21702727840,
            "IndexAdjustedFreeFloat": 100,
            "LastIANSUpdate": "31/10/2024",
            "TradeDateEOD": null,
            "AdjustmentRate": 114.99,
            "BrutoYield": 1.4,
            "IfTraded": true,
            "ShareTradingStatus": null,
            "IsOfferingPrice": false,
            "AdjustmentRateDesc": "Closing Price"
             */
            //@formatter:on
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

            // Verify interaction
            verify(feed).getHistoricalQuotes(security, false);
        }
        catch (Exception e)
        {
            fail("Exception not expected" + e.getMessage());
        }
    }

    @Test
    public void live_testing_security_1119478()

    {
        Security security = new Security();
        security.setTickerSymbol("DDDD");
        security.setCurrencyCode("ILA");
        security.setWkn("1119478");


        TLVSecurity feed = new TLVSecurity();

        try
        {

            Optional<QuoteFeedData> feedDataOpt = feed.getHistoricalQuotes(security, false);

            assertFalse("GetHistoricalQoutes feedData should not be empty", feedDataOpt.isEmpty());

            QuoteFeedData feedData = feedDataOpt.get();

            assertFalse("FeeData shoould contain prices", feedData.getPrices().isEmpty());

            SecurityPrice firstprice = feedData.getPrices().get(0);
            SecurityPrice lastprice = feedData.getPrices().get(feedData.getPrices().size() - 1);

            LatestSecurityPrice latestprice = feedData.getLatestPrices().get(0);


            assertThat(firstprice.getDate(), greaterThan(LocalDate.now().minusDays(5)));
            assertThat(lastprice.getDate(), greaterThan(LocalDate.now().minusMonths(2)));
            assertThat(firstprice.getValue(), greaterThan(Values.Quote.factorize(10000.00)));
            assertThat(lastprice.getValue(), greaterThan(Values.Quote.factorize(10000.00)));

            assertThat(latestprice.getDate(), greaterThan(LocalDate.now().minusDays(5)));
            assertThat(latestprice.getValue(), greaterThan(100L));
            assertThat(latestprice.getHigh(), greaterThan(100L));
            assertThat(latestprice.getLow(), greaterThan(100L));

        }
        catch (Exception e)
        {
            fail("Exception not expected" + e.getMessage());
        }
    }

}
