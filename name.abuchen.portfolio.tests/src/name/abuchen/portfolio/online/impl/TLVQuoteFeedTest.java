package name.abuchen.portfolio.online.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.time.LocalDate;
import java.time.Month;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
// import java.time.LocalDate;
// import java.util.NoSuchElementException;
// import java.util.Optional;
import java.util.Scanner;

import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;

import name.abuchen.portfolio.model.LatestSecurityPrice;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.online.QuoteFeedData;
import name.abuchen.portfolio.online.QuoteFeedException;
import name.abuchen.portfolio.online.impl.TLVMarket.TLVFund;
import name.abuchen.portfolio.online.impl.TLVMarket.TLVIndices;
import name.abuchen.portfolio.online.impl.TLVMarket.TLVSecurity;
import name.abuchen.portfolio.online.impl.TLVMarket.jsondata.SecurityListing;
import name.abuchen.portfolio.online.impl.TLVMarket.utils.TLVHelper;
import name.abuchen.portfolio.online.impl.TLVMarket.utils.TLVHelper.Language;


public class TLVQuoteFeedTest
{

    public interface SlowTests
    {
    }

    private String getIndicesList()

    {
        return getHistoricalTaseQuotes("response_tase_list_indices.txt");
    }

    private String getFundDetails()
    {
        return getHistoricalTaseQuotes("response_tase_fund_details01.txt");
    }

    private String getSecurityDetails()
    {
        return getHistoricalTaseQuotes("response_tase_security_details01.txt");
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



    /* Mock Tests using saved Queries */

    @Test
    public void testTLVGetAllSecuritiesResponse()
    {
        String response = getIndicesList();

        TLVIndices indices = Mockito.spy(new TLVIndices());
        
        try
        {
            Mockito.doReturn((response)).when(indices).rpcAllIndices(Language.ENGLISH);

            String allIncides = indices.rpcAllIndices(Language.ENGLISH);

            assertTrue(allIncides.contains("ABRA"));
            

            List<SecurityListing> incidesList = indices.getAllSecurities(Language.ENGLISH);
            SecurityListing listing = incidesList.getFirst();
            assertTrue(listing.getId().equals("2442"));
            assertTrue(listing.getType().equals("5"));

        }
        catch (Exception e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
            assertTrue(false);
        }

    }

    @Test
    public void testGetLatestQuoteOnSecurity() throws IOException
    {

        Security security = new Security();
        security.setTickerSymbol("AAPL");
        security.setCurrencyCode("ILS");
        String response = getSecurityDetails();


        TLVSecurity securityfeed = Mockito.spy(new TLVSecurity());
        Mockito.doReturn(response).when(securityfeed).rpcLatestQuoteSecurity(security);

        // System.out.println(feed.getLatestQuote(security));
        try
        {
            String json = securityfeed.getLatestQuote(security);
            Optional<String> reldate = TLVHelper.extract(json, 0, "\"RelevantDate\":", ","); //$NON-NLS-1$ //$NON-NLS-2$
            assertTrue(reldate.equals(Optional.empty()));
            Optional<String> tradeDate = TLVHelper.extract(json, 0, "\"TradeDate\":", ","); //$NON-NLS-1$ //$NON-NLS-2$
            assertFalse(tradeDate.equals(Optional.empty()));


        }
        catch (Exception e)
        {
            System.out.println(e.getMessage());
            assertTrue(false);
        }
    }

    @Test
    public void testGetLatestQuoteOnFund() throws IOException
    {

        Security security = new Security();
        security.setTickerSymbol("AAPL");
        security.setCurrencyCode("ILS");
        String response = getFundDetails();

        TLVFund fundfeed = Mockito.spy(new TLVFund());
        Mockito.doReturn(response).when(fundfeed).rpcLatestQuoteFund(security);

        try
        {
            String json = fundfeed.getLatestQuote(security);
            Optional<String> reldate = TLVHelper.extract(json, 0, "\"RelevantDate\":", ","); //$NON-NLS-1$ //$NON-NLS-2$
            assertFalse(reldate.equals(Optional.empty()));
            Optional<String> tradeDate = TLVHelper.extract(json, 0, "\"TradeDate\":", ","); //$NON-NLS-1$ //$NON-NLS-2$
            assertTrue(tradeDate.equals(Optional.empty()));


        }
        catch (Exception e)
        {
            System.out.println(e.getMessage());
            assertTrue(false);
        }
    }

    @Test
    public void testGetLatestQuoteForFund() throws IOException
    {

        Security security = new Security();
        security.setTickerSymbol("AAPL");
        security.setCurrencyCode("ILS");
        security.setWkn("5127121");
        String response = getFundDetails();
        assertTrue(response.length() > 0);

        TLVQuoteFeed feed = Mockito.spy(new TLVQuoteFeed());
        Mockito.doReturn(response).when(feed).rpcLatestQuoteFund(security);
        
        // PRice in ILS, Type = Mutual Fund
        try
        {
            Optional<LatestSecurityPrice> price = feed.getLatestQuote(security);
            assertFalse(price.equals(Optional.empty()));
            assertTrue(price.isPresent());
            
            LatestSecurityPrice p = price.get();
            // System.out.println(p + "High " + p.getValue());
            assertTrue(p.getHigh() == -1);
            assertTrue(p.getLow() == -1);
            assertTrue(p.getValue() == 15597000000l);
            assertTrue(p.getDate().equals(LocalDate.of(2025, 8, 21)));
            
            
        }
        catch (QuoteFeedException e)
        {
            System.out.println(e.getMessage());
            assertTrue(false);
        }
    }

    @Test
    public void testGetLatestQuoteForSecurity() throws IOException
    {

        Security security = new Security();
        security.setTickerSymbol("AAPL");
        security.setCurrencyCode("ILS");
        security.setWkn("1410307");
        String response = getSecurityDetails();
        assertTrue(response.length() > 0);

        TLVQuoteFeed feed = Mockito.spy(new TLVQuoteFeed());
        Mockito.doReturn(response).when(feed).rpcLatestQuoteFund(security);

        // PRice in ILS, Type = Mutual Fund
        try
        {
            Optional<LatestSecurityPrice> price = feed.getLatestQuote(security);
            assertFalse(price.equals(Optional.empty()));
            assertTrue(price.isPresent());

            LatestSecurityPrice p = price.get();
            // System.out.println(p + "High " + p.getDate() + " " +
            // LocalDate.of(2025, 8, 26));
            assertTrue(p.getHigh() == 118750000l);
            assertTrue(p.getLow() == 118500000l);
            assertTrue(p.getValue() == 118750000l);
            assertTrue(p.getDate().equals(LocalDate.of(2025, 8, 26))); // 05/11/2024

        }
        catch (QuoteFeedException e)
        {
            System.out.println(e.getMessage());
            assertTrue(false);
        }
    }

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
        try
        {
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
        catch (QuoteFeedException e)
        {
            System.out.println(e.getMessage());
            assertTrue(false);
        }
    }

    @Test
    public void testgetHistoricalQuotesOnFund() throws IOException
    {
        // TODO add support for Subid.
        Security security = new Security();
        security.setTickerSymbol("AAPL");
        security.setCurrencyCode("ILS");
        security.setWkn("5127121");
        String response = getFundHistory();
        assertTrue(response.length() > 0);

        TLVQuoteFeed feed = Mockito.spy(new TLVQuoteFeed());
        
        LocalDate from=LocalDate.of(1900, 1,1);
        if (!security.getPrices().isEmpty())
        {
            SecurityPrice lastHistoricalQuote = security.getPrices().get(security.getPrices().size() - 1);
            from = lastHistoricalQuote.getDate();
        }
        
        LocalDate to = LocalDate.now();
        

        // Price in ILS, Type = Mutual Fund
        try
        {
            Mockito.doReturn(response).when(feed).getPriceHistoryChunk(security.getWkn(), from, to, 1,
                            Language.ENGLISH);

            QuoteFeedData feedData = feed.getHistoricalQuotes(security, false);
            assertFalse(feedData.getPrices().isEmpty());

            SecurityPrice firstprice = feedData.getPrices().get(0);
            LocalDate firstdate = feedData.getPrices().get(0).getDate();

            assert (firstprice.getDate().equals(LocalDate.of(2025, 8, 25)));
            assert (firstprice.getValue() == 14688000000l);

        }
        catch (Exception e)
        {
            System.out.println(e.getMessage());
            assertTrue(false);
        }
    }

    @Test
    public void testgetHistoricalQuotesOnSecurity() throws IOException
    {
        // TODO add support for Subid.
        Security security = new Security();
        security.setTickerSymbol("AAPL");
        security.setCurrencyCode("ILS");
        security.setWkn("5127121");
        String response = getSecurityHistory();
        assertTrue(response.length() > 0);

        TLVQuoteFeed feed = Mockito.spy(new TLVQuoteFeed());
        LocalDate from = LocalDate.of(1900, 1, 1);
        if (!security.getPrices().isEmpty())
        {
            SecurityPrice lastHistoricalQuote = security.getPrices().get(security.getPrices().size() - 1);
            from = lastHistoricalQuote.getDate();
        }

        LocalDate to = LocalDate.now();



        // PRice in ILS, Type = Mutual Fund
        try
        {
            Mockito.doReturn(response).when(feed).getPriceHistoryChunk(security.getWkn(), from, to, 1,
                            Language.ENGLISH);

            QuoteFeedData feedData = feed.getHistoricalQuotes(security, false);
            assertFalse(feedData.getPrices().isEmpty());

            SecurityPrice firstprice = feedData.getPrices().get(0);
            // LocalDate firstdate = feedData.getPrices().get(0).getDate();
            // System.out.println("FeedData " + firstprice.getValue());

            assertTrue(firstprice.getDate().equals(LocalDate.of(2024, 11, 10)));
            assertTrue(firstprice.getValue() == 11499000000l);

        }
        catch (Exception e)
        {
            System.out.println(e.getMessage());
            assertTrue(false);
        }
    }

    @Test
    @Ignore
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
            assert (firstprice.getValue() == 14688000000l);

        }
        catch (Exception e)
        {
            System.out.println(e.getMessage());
            assertTrue(false);
        }
    }

    /* Real Tests against API */
    private String getHistoricalTaseQuotes(String filename)
    {
        String responseBody = null;
        Scanner scanner = new Scanner(getClass().getResourceAsStream(filename), "UTF-8");
        responseBody = scanner.useDelimiter("\\A").next();
        scanner.close();

        return responseBody;
    }

    @Test
    @Ignore
    public void testTLVFundDetailsAPI() throws IOException
    {
        Security security = new Security();
        security.setWkn("5113428");
        security.setCurrencyCode("ILS");

        // TLVQuoteFeed feed = new TLVQuoteFeed();
        TLVFund fund = new TLVFund();
        try
        {
            // Test TASE API
            String response = fund.rpcLatestQuoteFund(security);
            // System.out.println(response);
            Optional<String> mngrId = fund.extract(response, 0, "\"ManagerId\":", ",");
            assertTrue(mngrId.get().trim().contentEquals("10047"));

            Optional<String> type = fund.extract(response, 0, "\"Type\":", ","); // $NON-NLS-1$
            assertTrue(type.equals(Optional.empty()));
        }
        catch (Exception e)
        {
            assertTrue(false);
        }

    }

    @Test
    @Ignore
    public void testTLVSecurityDetailsAPI() throws IOException
    {
        Security security = new Security();
        security.setWkn("1410307");
        security.setCurrencyCode("ILS");

        // TLVQuoteFeed feed = new TLVQuoteFeed();
        TLVSecurity securityfeed = new TLVSecurity();
        try
        {
            // String response = feed.rpcLatestQuoteSecurity(security);
            String response = securityfeed.rpcLatestQuoteSecurity(security);
            // String response = feed.rpcLatestQuote(security);
            // System.out.println(response);
            Optional<String> mngrId = securityfeed.extract(response, 0, "\"IsTASEUP\":", ","); //$NON-NLS-1$ //$NON-NLS-2$
            boolean isTase = Boolean.parseBoolean(mngrId.get().trim());
            assertFalse(isTase);

            Optional<String> type = securityfeed.extract(response, 0, "\"Type\":", ","); // $NON-NLS-1$
            // assertTrue(type.get().trim().replace("\"", "").equals("Bonds"));
            assertTrue(type.get().trim().replace("\"", "").equals("איגרות חוב"));
        }
        catch (Exception e)
        {
            System.out.println(e.getMessage());
            assertTrue(false);
        }

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

    // @Test
    // public void testParsingHistoricalQuotes()
    // {
    // String rawQuotes = getHistoricalFundDetails();
    //
    // TLVQuoteFeed feed = new TLVQuoteFeed();
    // QuoteFeedData data = feed.extractQuotes(rawQuotes);
    // List<LatestSecurityPrice> prices = data.getLatestPrices();
    // Collections.sort(prices, new SecurityPrice.ByDate());
    //
    // assertThat(prices.size(), is(123));
    //
    // LatestSecurityPrice price = new LatestSecurityPrice(LocalDate.of(2017,
    // Month.NOVEMBER, 27), //
    // Values.Quote.factorize(188.55), //
    // LatestSecurityPrice.NOT_AVAILABLE, //
    // LatestSecurityPrice.NOT_AVAILABLE, //
    // LatestSecurityPrice.NOT_AVAILABLE);
    // assertThat(prices.get(0), equalTo(price));
    //
    // price = new LatestSecurityPrice(LocalDate.of(2018, Month.MAY, 25), //
    // Values.Quote.factorize(188.3), //
    // LatestSecurityPrice.NOT_AVAILABLE, //
    // LatestSecurityPrice.NOT_AVAILABLE, //
    // LatestSecurityPrice.NOT_AVAILABLE);
    // assertThat(prices.get(prices.size() - 1), equalTo(price));
    // }

    @Test
    @Ignore
    public void testSecurityHistoryPrices()
    {
        LocalDate from = LocalDate.of(2024, 9, 30);
        LocalDate to = LocalDate.of(2024, 10, 30);
        // DateTimeFormatter formatter =
        // DateTimeFormatter.ofPattern("dd/MM/yyyy"); //$NON-NLS-1$


        Security security = new Security();
        security.setWkn("1410307");
        security.setCurrencyCode("ILS");

        TLVQuoteFeed feed = new TLVQuoteFeed();
        // <String, Object> pricehistory;
        String pricehistory;
        try
        {
            pricehistory = feed.getPriceHistoryChunk(security.getWkn(), from, to, 1, Language.ENGLISH);
            // assertTrue(pricehistory.get("DateFrom").equals(from.format(formatter)));
            // assertTrue(pricehistory.get("DateTo").equals(to.format(formatter)));
            // assertTrue((int) pricehistory.get("TotalRec") == 17);

        }
        catch (Exception e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
            assertTrue(false);
        }


    }

    @Test
    @Ignore
    public void testSecurityHistoricalPrices()
    {
        // LocalDate from = LocalDate.of(2024, 9, 30);
        // LocalDate to = LocalDate.of(2024, 10, 30);
        // DateTimeFormatter formatter =
        // DateTimeFormatter.ofPattern("dd/MM/yyyy"); //$NON-NLS-1$

        Security security = new Security();
        security.setWkn("1410307");
        security.setCurrencyCode("ILS");
        LocalDate start = LocalDate.now().minusDays(3);
        SecurityPrice price = new SecurityPrice(start, 10l);
        security.addPrice(price);


        TLVQuoteFeed feed = new TLVQuoteFeed();
        // Map<String, Object> pricehistory;
        QuoteFeedData pricehistory;
        try
        {
            pricehistory = feed.getHistoricalQuotes(security, false);
            // System.out.println(pricehistory.getPrices());

            assertTrue(pricehistory.getPrices().size() == 2);
            assertFalse(pricehistory.getPrices().isEmpty());
            LocalDate firstdate = pricehistory.getPrices().get(0).getDate();
            // System.out.println("Start " + start);
            assertTrue(firstdate.isEqual(LocalDate.now()));

            LocalDate lastdate = pricehistory.getPrices().get(pricehistory.getPrices().size() - 1).getDate();
            assertTrue(lastdate.isEqual(start));
            // System.out.println(pricehistory.getPrices().get(0).getValue());

            // assertTrue(pricehistory.get("DateFrom").equals(from.format(formatter)));
            // assertTrue(pricehistory.get("DateTo").equals(to.format(formatter)));
            // assertTrue((int) pricehistory.get("TotalRec") == 17);

        }
        catch (Exception e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
            assertTrue(false);
        }
    }

    @Test
    @Ignore
    public void testFundHistoryPrices()
    {
        LocalDate from = LocalDate.of(2024, 9, 30);
        LocalDate to = LocalDate.of(2024, 10, 30);
        // DateTimeFormatter formatter =
        // DateTimeFormatter.ofPattern("dd/MM/yyyy"); //$NON-NLS-1$

        Security security = new Security();
        security.setWkn("5113428");
        security.setCurrencyCode("ILS");

        TLVQuoteFeed feed = new TLVQuoteFeed();
        //Map<String, Object> pricehistory;
        String pricehistory;
        try
        {
            pricehistory = feed.getPriceHistoryChunk(security.getWkn(), from, to, 1, Language.ENGLISH);
            // System.out.println(pricehistory);
            // assertTrue((int) pricehistory.get("Total") == 226);
            // assertTrue(pricehistory.get("StartDate").equals(from.format(formatter)));
            // assertTrue(pricehistory.get("EndDate").equals(to.format(formatter)));

        }
        catch (Exception e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
            assertTrue(false);
        }

    }

    public void testParsingHistoricalQuotes()
    {
        String rawQuotes = getFundHistory();

        TLVQuoteFeed feed = new TLVQuoteFeed();
        QuoteFeedData data = feed.extractQuotes(rawQuotes);
        List<LatestSecurityPrice> prices = data.getLatestPrices();
        Collections.sort(prices, new SecurityPrice.ByDate());

        assertThat(prices.size(), is(123));

        LatestSecurityPrice price = new LatestSecurityPrice(LocalDate.of(2017, Month.NOVEMBER, 27), //
                        Values.Quote.factorize(188.55), //
                        LatestSecurityPrice.NOT_AVAILABLE, //
                        LatestSecurityPrice.NOT_AVAILABLE, //
                        LatestSecurityPrice.NOT_AVAILABLE);
        assertThat(prices.get(0), equalTo(price));

        price = new LatestSecurityPrice(LocalDate.of(2018, Month.MAY, 25), //
                        Values.Quote.factorize(188.3), //
                        LatestSecurityPrice.NOT_AVAILABLE, //
                        LatestSecurityPrice.NOT_AVAILABLE, //
                        LatestSecurityPrice.NOT_AVAILABLE);
        assertThat(prices.get(prices.size() - 1), equalTo(price));
    }

}
