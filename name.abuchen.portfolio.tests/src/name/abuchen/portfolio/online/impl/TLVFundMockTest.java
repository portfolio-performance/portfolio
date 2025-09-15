package name.abuchen.portfolio.online.impl;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;

import com.google.gson.Gson;

import name.abuchen.portfolio.model.LatestSecurityPrice;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.online.QuoteFeedData;
import name.abuchen.portfolio.online.impl.TLVMarket.TLVFund;
import name.abuchen.portfolio.online.impl.TLVMarket.jsondata.FundHistory;
import name.abuchen.portfolio.online.impl.TLVMarket.utils.GSONUtil;
import name.abuchen.portfolio.online.impl.TLVMarket.utils.TLVHelper.Language;

public class TLVFundMockTest
{
    //@formatter:off
    /*
     * Tests should include: 
     * Test that we get a valid quote
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
    public void mocked_fund_resturns_latest_quote()
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
            assertThat(p.getValue(), is(Values.Quote.factorize(155.97))); // 155.97
            assertThat(p.getDate(), is(LocalDate.of(2025, 8, 21)));

            verify(fundfeed, times(1)).rpcLatestQuoteFund(security);

        }
        catch (IOException e)
        {
            assert (false);
        }

    }

    // @Test
    // public void shouldReturnHistoricalQuotesOnFund()
    // {
    // // TODO add support for Subid.
    // Security security = new Security();
    // security.setTickerSymbol("AAPL");
    // security.setCurrencyCode("ILS");
    // security.setWkn("5127121"); // Mutual Fund - T.F(2C) Tik 3 BenLeumi IL -
    // // Reported in ILS
    //
    // String mockedResponse = getSecurityHistory();
    // assertTrue(mockedResponse.length() > 0);
    //
    // // new SecurityHistory();
    // SecurityHistory history = SecurityHistory.fromJson(mockedResponse);
    // Optional<SecurityHistory> historyopt = Optional.of(history);
    //
    // TLVSecurity feed = Mockito.spy(new TLVSecurity());
    //
    // Optional<QuoteFeedData> mockedFeed =
    // feed.convertSecurityHistoryToQuoteFeedData(historyopt, security);
    //
    // // PRice in ILS, Type = Mutual Fund
    // try
    // {
    //
    // doReturn(mockedFeed).when(feed).getHistoricalQuotes(security, false);
    //
    // Optional<QuoteFeedData> feedDataOpt = feed.getHistoricalQuotes(security,
    // false);
    //
    // assertFalse("GetHistoricalQoutes feedData should not be empty",
    // feedDataOpt.isEmpty());
    //
    // QuoteFeedData feedData = feedDataOpt.get();
    //
    // assertFalse("FeeData shoould contain prices",
    // feedData.getPrices().isEmpty());
    //
    // SecurityPrice firstprice = feedData.getPrices().get(0);
    //
    // assertTrue("First price should be 27/7/2025",
    // firstprice.getDate().equals(LocalDate.of(2024, 11, 10)));
    // assertTrue("First price value should be 146.88", firstprice.getValue() ==
    // 114990000l);
    //
    // }
    // catch (Exception e)
    // {
    // System.out.println(e.getMessage());
    // assertTrue(false);
    // }
    // }

    @Test
    public void mocked_fund_with_blank_wks_does_not_return_latest_quote()
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
            verify(fundfeed, times(0)).rpcLatestQuoteFund(security);


        }
        catch (IOException e)
        {
            assert (false);
        }

    }

    @Test
    public void mocked_fund_withoutWKS_does_not_return_latest_quote()
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

            // Verify interaction
            verify(fundfeed, times(0)).rpcLatestQuoteFund(security);

        }
        catch (IOException e)
        {
            assert (false);
        }

    }

    
    @Test
    public void mocked_fund_return_Historical_Quotes()
    {
        Security security = new Security();
        security.setWkn("5127121");
        security.setCurrencyCode("ILS");

        LocalDate from = LocalDate.of(2025, 7, 14);
        LocalDate to = LocalDate.of(2025, 8, 25);

        String response = getFundHistory();
        assertTrue(response.length() > 0);
        assertTrue(response.contains("Table"));
        assertTrue(response.contains("StartDate"));

        Gson gson = GSONUtil.createGson();
        FundHistory historyListing = gson.fromJson(response, FundHistory.class);

        try
        {
            TLVFund tlvFund = Mockito.spy(new TLVFund());

            Mockito.doReturn(Optional.of(historyListing)).when(tlvFund).getPriceHistoryChunk(security, from, to, 1,
                            Language.ENGLISH);

            // Cannot use getHistoricalQuotes as this resets from and to dates
            // automatically.
            // Instead use the two internal functionsOptional<QuoteFeedData>
            // fundFeedDataOptional =
            // tlvFund.getHistoricalQuotes(security, false);
            Optional<FundHistory> fundHistory = tlvFund.getPriceHistoryChunk(security, from, to, 1, Language.ENGLISH);
            Optional<QuoteFeedData> fundFeedDataOptional = tlvFund.convertFundHistoryToQuoteFeedData(fundHistory,
                            security);


            assertThat(fundFeedDataOptional.isPresent(), is(true));
            QuoteFeedData fundFeedData = fundFeedDataOptional.get();

            assertTrue(fundFeedData.getPrices() != null);
            assertThat(fundFeedData.getPrices().size(), is(30));

            List<SecurityPrice> listPrices = fundFeedData.getPrices();

            SecurityPrice firstprice = fundFeedData.getPrices().get(0);
            SecurityPrice lastprice = fundFeedData.getPrices().get(listPrices.size() - 1);
            assertTrue(firstprice != null);

            assertThat(firstprice.getDate(), is(to));

            assertThat(firstprice.getValue(), is(Values.Quote.factorize(130.30)));

            assertThat(lastprice.getDate(), is(from));
            assertThat(firstprice.getValue(), is(Values.Quote.factorize(130.30)));
            verify(tlvFund, times(1)).getPriceHistoryChunk(security, from, to, 1, Language.ENGLISH);

        }
        catch (Exception e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
            assertTrue(false);
        }


    }
    
    @Test
    public void mocked_fund_without_wkn_does_notreturn_Historical_Quotes()
    {
        Security security = new Security();
        security.setCurrencyCode("ILS");

        LocalDate from = LocalDate.of(2025, 7, 14);
        LocalDate to = LocalDate.of(2025, 8, 25);

        String response = getFundHistory();
        assertTrue(response.length() > 0);
        assertTrue(response.contains("Table"));
        assertTrue(response.contains("StartDate"));

        Gson gson = GSONUtil.createGson();
        FundHistory historyListing = gson.fromJson(response, FundHistory.class);

        try
        {
            TLVFund tlvFund = Mockito.spy(new TLVFund());

            Mockito.doReturn(Optional.of(historyListing)).when(tlvFund).getPriceHistoryChunk(security, from, to, 1,
                            Language.ENGLISH);

            Optional<QuoteFeedData> fundFeedDataOptional = tlvFund.getHistoricalQuotes(security, false);

            assertThat(fundFeedDataOptional.isEmpty(), is(true));
            verify(tlvFund, times(0)).getPriceHistoryChunk(security, from, to, 1, Language.ENGLISH);


        }
        catch (Exception e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
            assertTrue(false);
        }

        security.setWkn("");
        try
        {
            TLVFund tlvFund = Mockito.spy(new TLVFund());

            Mockito.doReturn(Optional.of(historyListing)).when(tlvFund).getPriceHistoryChunk(security, from, to, 1,
                            Language.ENGLISH);

            Optional<QuoteFeedData> fundFeedDataOptional = tlvFund.getHistoricalQuotes(security, false);

            assertThat(fundFeedDataOptional.isEmpty(), is(true));
            verify(tlvFund, times(0)).getPriceHistoryChunk(security, from, to, 1, Language.ENGLISH);

        }
        catch (Exception e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
            assertTrue(false);
        }

    }



    @Ignore("This class is under development and not ready for testing")
    @Test
    public void mocked_Fund_returns_correct_details()
    {
        //
    }

    @Ignore("This class is under development and not ready for testing")
    @Test
    public void mocked_funds_without_Wks_does_not_return_details()
    {
        //
    }
}
