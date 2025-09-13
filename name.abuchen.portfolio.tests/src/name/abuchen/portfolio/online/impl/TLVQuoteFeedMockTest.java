package name.abuchen.portfolio.online.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;

import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;

import com.google.gson.Gson;

import name.abuchen.portfolio.model.LatestSecurityPrice;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.online.QuoteFeedData;
import name.abuchen.portfolio.online.impl.TLVMarket.TLVFund;
import name.abuchen.portfolio.online.impl.TLVMarket.TLVSecurity;
import name.abuchen.portfolio.online.impl.TLVMarket.jsondata.FundHistory;
import name.abuchen.portfolio.online.impl.TLVMarket.jsondata.FundHistoryEntry;
import name.abuchen.portfolio.online.impl.TLVMarket.jsondata.SecurityHistory;
import name.abuchen.portfolio.online.impl.TLVMarket.jsondata.SecurityHistoryEntry;
import name.abuchen.portfolio.online.impl.TLVMarket.jsondata.SecurityListing;
import name.abuchen.portfolio.online.impl.TLVMarket.utils.GSONUtil;
import name.abuchen.portfolio.online.impl.TLVMarket.utils.TLVHelper.Language;


public class TLVQuoteFeedMockTest
{


    private String getIndicesList()

    {
        return getHistoricalTaseQuotes("response_tase_list_indices.txt");
    }

    // private String getFundDetails()
    // {
    // return getHistoricalTaseQuotes("response_tase_fund_details01.txt");
    // }

    private String getSecurityDetails()
    {
        return getHistoricalTaseQuotes("response_tase_security_details01.txt");
    }

    private String getSecurityDetailsEnglish()
    {
        return getHistoricalTaseQuotes("response_tase_security_details01.txt");
    }

    private String getSecurityDetailsHebrew()
    {
        return getHistoricalTaseQuotes("response_tase_security_details03.txt");
    }

    private String getSharesDetails()
    {
        return getHistoricalTaseQuotes("response_tase_security_details02.txt");
    }

    private String getFundHistory()
    {
        return getHistoricalTaseQuotes("response_tase_fund_history01.txt");
    }

    private String getShareHistory()
    {
        return "";
    }

    private String getSecurityHistory()
    {
        return getHistoricalTaseQuotes("response_tase_security_history01.txt");
    }

    private String getHistoricalTaseQuotes(String filename)
    {
        String responseBody = null;
        Scanner scanner = new Scanner(getClass().getResourceAsStream(filename), "UTF-8");
        responseBody = scanner.useDelimiter("\\A").next();
        scanner.close();

        return responseBody;
    }




    @Ignore("Mock Test Test needs to be refactored")
    @Test
    public void testGetLatestQuoteForShare() throws IOException
    {
        // TODO add support for Subid.
        Security security = new Security();
        security.setTickerSymbol("AAPL");
        security.setCurrencyCode("ILS");
        security.setWkn("273");
        String response = getSharesDetails();
        assertTrue(response.length() > 0);

        TLVQuoteFeed feed = Mockito.spy(new TLVQuoteFeed());
        Mockito.doReturn(response).when(feed).rpcLatestQuoteFund(security);

        // PRice in ILS, Type = Mutual Fund
        Optional<LatestSecurityPrice> price = feed.getLatestQuote(security);
        assertFalse(price.equals(Optional.empty()));
        assertTrue(price.isPresent());

        LatestSecurityPrice p = price.get();
        // System.out.println(p + "High " + p.getValue() + " " +
        // LocalDate.of(2025, 8, 26));
        assertTrue(p.getHigh() == 47190000000l);
        assertTrue(p.getLow() == 46050000000l);
        assertTrue(p.getValue() == 46050000000l);
        assertTrue(p.getDate().equals(LocalDate.of(2025, 8, 31))); // 05/11/2024

    }

    @Ignore()
    @Test
    public void get_latest_quote_for_Bonds()
    {
    }

    @Ignore()
    @Test
    public void get_latest_quote_for_Security()
    {
    }


    // @Test
    // public void testgetHistoricalQuotesOnSecurity() throws IOException
    // {
    // // TODO add support for Subid.
    // Security security = new Security();
    // security.setTickerSymbol("AAPL");
    // security.setCurrencyCode("ILS");
    // security.setWkn("5127121");
    // String response = getSecurityHistory();
    // assertTrue(response.length() > 0);
    //
    // TLVQuoteFeed feed = Mockito.spy(new TLVQuoteFeed());
    // LocalDate from = LocalDate.of(1900, 1, 1);
    // if (!security.getPrices().isEmpty())
    // {
    // SecurityPrice lastHistoricalQuote =
    // security.getPrices().get(security.getPrices().size() - 1);
    // from = lastHistoricalQuote.getDate();
    // }
    //
    // LocalDate to = LocalDate.now();
    //
    //
    //
    // // PRice in ILS, Type = Mutual Fund
    // try
    // {
    // Mockito.doReturn(response).when(feed).getPriceHistoryChunk(security.getWkn(),
    // from, to, 1,
    // Language.ENGLISH);
    //
    // QuoteFeedData feedData = feed.getHistoricalQuotes(security, false);
    // assertFalse(feedData.getPrices().isEmpty());
    //
    // SecurityPrice firstprice = feedData.getPrices().get(0);
    // // LocalDate firstdate = feedData.getPrices().get(0).getDate();
    // // System.out.println("FeedData " + firstprice.getValue());
    //
    // assertTrue(firstprice.getDate().equals(LocalDate.of(2024, 11, 10)));
    // assertTrue(firstprice.getValue() == 11499000000l);
    //
    // }
    // catch (Exception e)
    // {
    // System.out.println(e.getMessage());
    // assertTrue(false);
    // }
    // }

    @Test
    @Ignore("Mock Test")
    public void get_Historical_quotes_for_Bonds() throws IOException
    {

    }

    @Test
    @Ignore("Mock Test")
    public void get_Historical_quotes_for_Security() throws IOException
    {

    }

    @Test
    @Ignore("Mock Test")
    public void testgetHistoricalQuotesOnShares() throws IOException
    {
        // TODO add support for Subid.
        Security security = new Security();
        security.setTickerSymbol("AAPL");
        security.setCurrencyCode("ILS");
        security.setWkn("273");
        String response = getShareHistory();
        assertTrue(response.length() > 0);

        TLVQuoteFeed feed = Mockito.spy(new TLVQuoteFeed());
        Mockito.doReturn(response).when(feed).rpcLatestQuoteFund(security);

        // PRice in ILS, Type = Mutual Fund
        try
        {
            QuoteFeedData feedData = feed.getHistoricalQuotes(security, false);
            assertFalse(feedData.getPrices().isEmpty());

            SecurityPrice firstprice = feedData.getPrices().get(0);
            // LocalDate firstdate = feedData.getPrices().get(0).getDate();
            // System.out.println("FeedData " + firstdate);

            assert (firstprice.getDate().equals(LocalDate.of(2025, 7, 28)));
            assertThat(firstprice.getDate(), is(LocalDate.of(2025, 7, 28)));
            assert (firstprice.getValue() == 14688000000l);

        }
        catch (Exception e)
        {
            System.out.println(e.getMessage());
            assertTrue(false);
        }
    }

    @Test
    public void testSecurityGetNames()
    {
        Security security = new Security();
        security.setTickerSymbol("AAPL");
        security.setCurrencyCode("ILS");
        security.setWkn("5127121");
        String responseEng = getSecurityDetailsEnglish();
        String responseHeb = getSecurityDetailsHebrew();
        assertTrue(responseEng.length() > 0);
        assertTrue(responseHeb.length() > 0);

        TLVSecurity feed = Mockito.spy(new TLVSecurity());
        try
        {
            Mockito.doReturn(responseEng).when(feed).rpcLatestQuoteSecuritywithLanguage(security, Language.ENGLISH);
            Mockito.doReturn(responseHeb).when(feed).rpcLatestQuoteSecuritywithLanguage(security, Language.HEBREW);

            SecurityListing englishListing = feed.getDetails(security, Language.ENGLISH);
            SecurityListing hebrewListing = feed.getDetails(security, Language.HEBREW);

            Map<String, String> names = (feed.getNames(englishListing, hebrewListing));
            assertTrue(names.getOrDefault("Id", null) == null);
            assertTrue(names.getOrDefault("english_short_name", null).equals("ILCPI % 1025"));
            assertTrue(names.getOrDefault("hebrew_short_name", null).equals("ממשל צמודה 1025"));

        }
        catch (Exception e)
        {
            System.out.println(e.getMessage());
            assertTrue(false);
        }



    }

    @Ignore("Test needs to be refactored")
    @Test
    public void testFundHistoryPrices()
    {
        Security security = new Security();
        security.setWkn("5127121");
        security.setCurrencyCode("ILS");

        LocalDate from = LocalDate.of(2024, 11, 3);
        LocalDate to = LocalDate.of(2024, 11, 4);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd"); //$NON-NLS-1$

        String response = getFundHistory();
        assertTrue(response.length() > 0);
        assertTrue(response.contains("Table"));
        assertTrue(response.contains("StartDate"));

        Gson gson = GSONUtil.createGson();
        FundHistory historyListing = gson.fromJson(response, FundHistory.class);
        System.out.println(historyListing.toString());
        TLVFund tlvFund = Mockito.spy(new TLVFund());
        TLVQuoteFeed feed = new TLVQuoteFeed();

        try
        {
            Mockito.doReturn(historyListing).when(tlvFund).getPriceHistoryChunk(security, from, to, 1,
                            Language.ENGLISH);

            // TODO - replace Chunk2
            Map<String, Object> pricehistorymap = feed.getPriceHistoryChunk2(security, from, to, 1, Language.ENGLISH);
            FundHistory historyResponse = FundHistory.fromMap(pricehistorymap);


            assertTrue(historyResponse.getDateFrom().equals(from));
            assertTrue(historyResponse.getDateTo().equals(to));
            assertTrue(historyResponse.getTotalRecs() == 30);

            FundHistoryEntry firstEntry = historyResponse.getItems()[0];
            System.out.println(firstEntry.getSellPrice());
            assertTrue(firstEntry.getSellPrice() == (float) 146.88);
            assertTrue(firstEntry.getRate() == (float) 0);
            assertTrue(firstEntry.getTradeDate().equals(to));

        }
        catch (Exception e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
            assertTrue(false);
        }


    }


    @Ignore("Test needs to be refactored")
    @Test
    public void testSecurityHistoryPrices()
    {
        Security security = new Security();
        security.setWkn("1410307");
        security.setCurrencyCode("ILS");

        LocalDate from = LocalDate.of(2024, 11, 5);
        LocalDate to = LocalDate.of(2024, 11, 10);
        // DateTimeFormatter formatter =
        // DateTimeFormatter.ofPattern("yyyy/MM/dd"); //$NON-NLS-1$

        String response = getSecurityHistory();
        assertTrue(response.length() > 0);
        assertTrue(response.contains("Items"));
        assertTrue(response.contains("DateTo"));

        Gson gson = GSONUtil.createGson();
        SecurityHistory historyListing = gson.fromJson(response, SecurityHistory.class);


        TLVSecurity tlvSecurity = Mockito.spy(new TLVSecurity());
        TLVQuoteFeed feed = new TLVQuoteFeed();


        try
        {
            Mockito.doReturn(historyListing).when(tlvSecurity).getPriceHistoryChunkSec(security, from, to, 1,
                            Language.ENGLISH);

            Map<String, Object> pricehistorymap = feed.getPriceHistoryChunk2(security, from, to, 1, Language.ENGLISH);
            SecurityHistory historyResponse = SecurityHistory.fromMap(pricehistorymap);

            assertTrue(historyResponse.getDateFrom().equals(from));
            assertTrue(historyResponse.getDateTo().equals(to));
            assertTrue(historyResponse.getTotalRec() == 4);

            SecurityHistoryEntry firstEntry = historyResponse.getItems()[0];
            assertTrue(firstEntry.getHighRate() == (float) 115.89);
            assertTrue(firstEntry.getLowRate() == (float) 0);
            assertTrue(firstEntry.getTradeDate().equals(to));

        }
        catch (Exception e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
            assertTrue(false);
        }


    }





}
