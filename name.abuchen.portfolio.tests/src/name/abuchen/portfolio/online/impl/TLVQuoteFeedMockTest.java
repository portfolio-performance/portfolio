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
import java.util.Map;
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
import name.abuchen.portfolio.online.QuoteFeedException;
import name.abuchen.portfolio.online.impl.TLVMarket.TLVEntities;
import name.abuchen.portfolio.online.impl.TLVMarket.TLVFund;
import name.abuchen.portfolio.online.impl.TLVMarket.TLVSecurity;
import name.abuchen.portfolio.online.impl.TLVMarket.jsondata.SecurityListing;
import name.abuchen.portfolio.online.impl.TLVMarket.utils.TLVHelper;
import name.abuchen.portfolio.online.impl.TLVMarket.utils.TLVHelper.Language;


public class TLVQuoteFeedMockTest
{


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

    /* Mock Tests using saved Query Files - no testing against the API */

    @Test
    public void testTLVGetAllSecuritiesResponse()
    {
        String response = getIndicesList();

        TLVEntities indices = Mockito.spy(new TLVEntities());
        
        try
        {
            Mockito.doReturn((response)).when(indices).rpcAllIndices(Language.ENGLISH);

            String allIncides = indices.rpcAllIndices(Language.ENGLISH);

            assertTrue(allIncides.contains("ABRA"));
            

            List<SecurityListing> incidesList = indices.getAllEntities(Language.ENGLISH);
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





    @Test
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
