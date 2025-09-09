package name.abuchen.portfolio.online.impl;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
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
import name.abuchen.portfolio.online.impl.TLVMarket.jsondata.FundHistory;
import name.abuchen.portfolio.online.impl.TLVMarket.jsondata.FundHistoryEntry;
import name.abuchen.portfolio.online.impl.TLVMarket.utils.GSONUtil;
import name.abuchen.portfolio.online.impl.TLVMarket.utils.TLVHelper.Language;

public class TLVFundTest
{
    //@formatter:off
    /*
     * Tests should include: 
     * Test that we get a valid quote
     * Test that we get valid historical quotes
     * Test getNames
     * Test all work when it is a non-existing wkn
     * Test all work when without a wkn
     * Test conversions of ILS/ILA and back - could be moved to Listing
     */
    //@formatter:on

    private String getFundDetails()
    {
        return getHistoricalTaseQuotes("response_tase_fund_details01.txt");
    }

    private String getFundHistory()
    {
        return getHistoricalTaseQuotes("response_tase_fund_history01.txt");
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
    public void shouldReturnCorrectLatestQuotesForFundMock()
    {
        Security security = new Security();
        security.setCurrencyCode("ILS");
        security.setWkn("5127121");

        String mockedresponse = getFundDetails();
        assertTrue(mockedresponse.length() > 0);

        try
        {
            TLVFund fundfeed = Mockito.spy(new TLVFund());
            Mockito.doReturn(mockedresponse).when(fundfeed).rpcLatestQuoteFund(security);

            Optional<LatestSecurityPrice> oprice = fundfeed.getLatestQuote(security);

            assert (oprice.isPresent());

            LatestSecurityPrice p = oprice.get();
            assertThat(p.getHigh(), is(-1l));
            assertThat(p.getLow(), is(-1l));
            // System.out.println(p.getValue());
            assertThat(p.getValue(), is(15597000000l)); // 155.97
            assertTrue(p.getDate().equals(LocalDate.of(2025, 8, 21)));

        }
        catch (IOException e)
        {
            assert (false);
        }

    }

    @Test
    public void latestQuoteonBlankWKSFundReturnsCorrectValues()
    {
        Security security = new Security();
        security.setCurrencyCode("ILS");
        security.setWkn("");
        String response = getFundDetails();

        try
        {
            TLVFund fundfeed = Mockito.spy(new TLVFund());
            Mockito.doReturn(response).when(fundfeed).rpcLatestQuoteFund(security);

            // System.out.println(feed.getLatestQuote(security));
            Optional<LatestSecurityPrice> oprice = fundfeed.getLatestQuote(security);

            assert (oprice.isEmpty());


        }
        catch (IOException e)
        {
            assert (false);
        }

    }

    @Test
    public void latestQuoteonNoWKSFundReturnsCorrectValues()
    {
        Security security = new Security();
        security.setCurrencyCode("ILS");
        String response = getFundDetails();

        try
        {
            TLVFund fundfeed = Mockito.spy(new TLVFund());
            Mockito.doReturn(response).when(fundfeed).rpcLatestQuoteFund(security);

            // System.out.println(feed.getLatestQuote(security));
            Optional<LatestSecurityPrice> oprice = fundfeed.getLatestQuote(security);

            assert (oprice.isEmpty());

        }
        catch (IOException e)
        {
            assert (false);
        }

    }

    
    @Ignore("Test is not ready")
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
    
    @Ignore("Test is not ready")
    @Test
    public void quoteHistoryonValidFundReturnsCorrectValues()
    {
        // TODO add support for Subid.
        Security security = new Security();
        security.setTickerSymbol("AAPL");
        security.setCurrencyCode("ILS");
        security.setWkn("5127121");

        String mockedresponse = getFundHistory();
        assertTrue(mockedresponse.length() > 0);

        // Need to convert response to Optional<FundHistory>
        Optional<FundHistory> fundHistoryOpt = Optional.of(new FundHistory());
        
        // TLVQuoteFeed feed = Mockito.spy(new TLVQuoteFeed());
        TLVFund feed = Mockito.spy(new TLVFund());

        // Set a start date
        LocalDate from = LocalDate.of(1900, 1, 1);
        if (!security.getPrices().isEmpty())
        {
            SecurityPrice lastHistoricalQuote = security.getPrices().get(security.getPrices().size() - 1);
            from = lastHistoricalQuote.getDate();
        }

        LocalDate to = LocalDate.now();

        // Price in ILS, Type = Mutual Fund
        try
        {

            Mockito.doReturn(fundHistoryOpt).when((feed).getPriceHistoryChunk(security, from, to, 1, Language.ENGLISH));

            Optional<QuoteFeedData> feedDataOpt = feed.getHistoricalQuotes(security, false);

            assertTrue(feedDataOpt.isPresent());

            QuoteFeedData feedData = feedDataOpt.get();
            assertFalse(feedData.getPrices().isEmpty());

            SecurityPrice firstprice = feedData.getPrices().get(0);
            // LocalDate firstdate = feedData.getPrices().get(0).getDate();

            assert (firstprice.getDate().equals(LocalDate.of(2025, 8, 25)));
            assert (firstprice.getValue() == 14688000000l);

        }
        catch (Exception e)
        {
            System.out.println(e.getMessage());
            assertTrue(false);
        }
    }

    @Ignore("This class is under development and not ready for testing")
    @Test
    public void quoteHistoryonBlankInvalidWKSFundReturnsCorrectValues()
    {
        //
    }

    @Ignore("This class is under development and not ready for testing")
    @Test
    public void DetailsonValidFundReturnsCorrectValues()
    {
        //
    }

    @Ignore("This class is under development and not ready for testing")
    @Test
    public void DetailsonInValidFundReturnsCorrectValues()
    {
        //
    }
}
