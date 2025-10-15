package name.abuchen.portfolio.online.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;
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


public class TASEQuoteFeedMockTest
{


    private String getIndicesList()

    {
        return getHistoricalTaseQuotes("response_tase_list_indices.txt");
    }

    // Mutual Fund Example - 5127121
    // https://maya.tase.co.il/en/funds/mutual-funds/5127121
    private String getFundDetails()
    {
        return getHistoricalTaseQuotes("response_tase_fund_details01.txt");
    }

    // Government Bond Example - 01135912
    // https://market.tase.co.il/en/market_data/security/1135912/major_data
    private String getSecurityDetails()
    {
        return getHistoricalTaseQuotes("response_tase_security_bond_details01.txt");
    }

    private String getSecurityDetailsEnglish()
    {
        return getSecurityDetails();
    }

    private String getSecurityDetailsHebrew()
    {
        return getHistoricalTaseQuotes("response_tase_security_details03.txt");
    }

    // Stock Example - NICE
    // https://market.tase.co.il/en/market_data/security/273011
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
        return "response_tase_share_history01.txt";
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
            // assertFalse(price.equals(Optional.empty()));
            // assertTrue(price.isPresent());

            LatestSecurityPrice p = price.get();
            // System.out.println(p + "High " + p.getValue() + " " +
            // LocalDate.of(2025, 8, 26));
            assertThat(p.getDate(), is(LocalDate.of(2025, 8, 31)));
            assertThat(p.getHigh(), is(Values.Quote.factorize(471.90)));
            assertThat(p.getLow(), is(Values.Quote.factorize(460.50)));
            assertThat(p.getValue(), is(Values.Quote.factorize(460.50)));
            // assertTrue(p.getHigh() == 47190000000l);
            // assertTrue(p.getLow() == 46050000000l);
            // assertTrue(p.getValue() == 46050000000l);
            // assertTrue(p.getDate().equals(LocalDate.of(2025, 8, 31))); //
            // 05/11/2024

            verify(feed).getLatestQuote(security);
        }
        catch (Exception e)
        {
            System.out.println(e.getMessage());
            assertTrue(false);
        }
    }

    @Ignore()
    @Test
    public void mocked_Bonds_should_return_latest_quotes()
    {
    }

    @Ignore()
    @Test
    public void mocked_Security_should_return_latest_quotes()
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
    public void mocked_Bonds_should_return_historical_Quotes() throws IOException
    {

    }

    @Test
    @Ignore("Mock Test")
    public void mocked_Security_should_return_historical_Quotes() throws IOException
    {
    }

    @Test
    @Ignore()
    public void mocked_Shares_should_return_historical_Quotes() throws IOException
    {
        // TODO add support for Subid.
        Security security = new Security();
        security.setTickerSymbol("AAPL");
        security.setCurrencyCode("ILS");
        security.setWkn("273");
        String response = getShareHistory();
        assertTrue(response.length() > 0);

        // return Optional<QuoteFeedData>

        TASEQuoteFeed feed = Mockito.spy(new TASEQuoteFeed());
        Mockito.doReturn(response).when(feed).getHistoricalQuotes(security, false);

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
            verify(feed, times(1)).getHistoricalQuotes(security, false);

        }
        catch (Exception e)
        {
            System.out.println(e.getMessage());
            assertTrue(false);
        }
    }

    // public void mocked_security_should_return_names()
    // {
    // Security security = new Security();
    // security.setTickerSymbol("AAPL");
    // security.setCurrencyCode("ILS");
    // security.setWkn("5127121");
    // String responseEng = getSecurityDetailsEnglish();
    // String responseHeb = getSecurityDetailsHebrew();
    // assertTrue(responseEng.length() > 0);
    // assertTrue(responseHeb.length() > 0);
    //
    // TLVSecurity feed = Mockito.spy(new TLVSecurity());
    // try
    // {
    // Mockito.doReturn(responseEng).when(feed).rpcLatestQuoteSecuritywithLanguage(security,
    // Language.ENGLISH);
    // Mockito.doReturn(responseHeb).when(feed).rpcLatestQuoteSecuritywithLanguage(security,
    // Language.HEBREW);
    //
    // SecurityListing englishListing = feed.getDetails(security,
    // Language.ENGLISH);
    // SecurityListing hebrewListing = feed.getDetails(security,
    // Language.HEBREW);
    //
    // Map<String, String> names = (feed.getNames(englishListing,
    // hebrewListing));
    // assertTrue(names.getOrDefault("Id", null) == null);
    // assertTrue(names.getOrDefault("english_short_name", null).equals("ILCPI %
    // 1025"));
    // assertTrue(names.getOrDefault("hebrew_short_name", null).equals("ממשל
    // צמודה 1025"));
    //
    // }
    // catch (Exception e)
    // {
    // System.out.println(e.getMessage());
    // assertTrue(false);
    // }
    //
    //
    // }



    @Ignore
    @Test
    public void mocked_shares_should_return_names()
    {
        //
    }

    @Ignore
    @Test
    public void mocked_bond_should_return_names()
    {
        //
    }

    // @Ignore("Test needs to be refactored")
    // @Test
    // public void mocked_fund_should_return_historical_prices()
    // {
    // Security security = new Security();
    // security.setWkn("5127121");
    // security.setCurrencyCode("ILS");
    //
    // LocalDate from = LocalDate.of(2024, 11, 3);
    // LocalDate to = LocalDate.of(2024, 11, 4);
    // DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    // //$NON-NLS-1$
    //
    // String response = getFundHistory();
    // assertTrue(response.length() > 0);
    // assertTrue(response.contains("Table"));
    // assertTrue(response.contains("StartDate"));
    //
    // Gson gson = GSONUtil.createGson();
    // FundHistory historyListing = gson.fromJson(response, FundHistory.class);
    // System.out.println(historyListing.toString());
    // TLVFund tlvFund = Mockito.spy(new TLVFund());
    // TLVQuoteFeed feed = new TLVQuoteFeed();
    //
    // try
    // {
    // Mockito.doReturn(historyListing).when(tlvFund).getPriceHistoryChunk(security,
    // from, to, 1,
    // Language.ENGLISH);
    //
    // // TODO - replace Chunk2
    // QuoteFeedData pricehistorymap = feed.getHistoricalQuotes(security,
    // false);
    // FundHistory historyResponse = FundHistory.fromMap(pricehistorymap);
    //
    //
    // assertTrue(historyResponse.getDateFrom().equals(from));
    // assertTrue(historyResponse.getDateTo().equals(to));
    // assertTrue(historyResponse.getTotalRecs() == 30);
    //
    // FundHistoryEntry firstEntry = historyResponse.getItems()[0];
    // System.out.println(firstEntry.getSellPrice());
    // assertThat(firstEntry.getSellPrice(),
    // is(Values.Quote.factorize(146.88)));
    // assertThat(firstEntry.getRate(), is(Values.Quote.factorize(0)));
    // assertTrue(firstEntry.getTradeDate().equals(to));
    //
    // }
    // catch (Exception e)
    // {
    // // TODO Auto-generated catch block
    // e.printStackTrace();
    // assertTrue(false);
    // }
    //
    //
    // }


    // @Ignore("Test needs to be refactored")
    // @Test
    // public void mocked_security_should_return_historical_prices()
    // {
    // Security security = new Security();
    // security.setWkn("1410307");
    // security.setCurrencyCode("ILS");
    //
    // LocalDate from = LocalDate.of(2024, 11, 5);
    // LocalDate to = LocalDate.of(2024, 11, 10);
    // // DateTimeFormatter formatter =
    // // DateTimeFormatter.ofPattern("yyyy/MM/dd"); //$NON-NLS-1$
    //
    // String response = getSecurityHistory();
    // assertTrue(response.length() > 0);
    // assertTrue(response.contains("Items"));
    // assertTrue(response.contains("DateTo"));
    //
    // Gson gson = GSONUtil.createGson();
    // SecurityHistory historyListing = gson.fromJson(response,
    // SecurityHistory.class);
    //
    //
    // TLVSecurity tlvSecurity = Mockito.spy(new TLVSecurity());
    // TLVQuoteFeed feed = new TLVQuoteFeed();
    //
    //
    // try
    // {
    // Mockito.doReturn(historyListing).when(tlvSecurity).getPriceHistoryChunkSec(security,
    // from, to, 1,
    // Language.ENGLISH);
    //
    // Map<String, Object> pricehistorymap =
    // feed.getPriceHistoryChunk2(security, from, to, 1, Language.ENGLISH);
    // SecurityHistory historyResponse =
    // SecurityHistory.fromMap(pricehistorymap);
    //
    // assertTrue(historyResponse.getDateFrom().equals(from));
    // assertTrue(historyResponse.getDateTo().equals(to));
    // assertTrue(historyResponse.getTotalRec() == 4);
    //
    // SecurityHistoryEntry firstEntry = historyResponse.getItems()[0];
    // assertTrue(firstEntry.getHighRate() == (float) 115.89);
    // assertTrue(firstEntry.getLowRate() == (float) 0);
    // assertTrue(firstEntry.getTradeDate().equals(to));
    //
    // }
    // catch (Exception e)
    // {
    // // TODO Auto-generated catch block
    // e.printStackTrace();
    // assertTrue(false);
    // }
    //
    //
    // }

    @Ignore
    @Test
    public void mocked_share_should_return_historical_prices()
    {
        //
    }

    @Ignore
    @Test
    public void mocked_index_should_not_return_historical_prices()
    {
        //
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
