package name.abuchen.portfolio.online.impl;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
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
import name.abuchen.portfolio.online.impl.TLVMarket.jsondata.FundHistoryEntry;
import name.abuchen.portfolio.online.impl.TLVMarket.utils.GSONUtil;
import name.abuchen.portfolio.online.impl.TLVMarket.utils.TLVHelper.Language;

public class TLVFundTest
{

    // Mutual Fund Example - 5127121
    // https://maya.tase.co.il/en/funds/mutual-funds/5127121
    //@formatter:off
    /*
     * "FundId": 5127121,
     * "PurchasePrice": 155.97,
        "SellPrice": 155.97,
        "CreationPrice": 155.97,
        "UnitValuePrice": 155.97,
     */
    //@formatter:on
    private String getFundDetails()
    {
        return getHistoricalTaseQuotes("response_tase_fund_details01.txt");
    }

    private String getFundHistory()
    {
    //@formatter:off
    /*
     *  "Total": 206,
        "StartDate": "2024-11-03T00:00:00",
        "EndDate": "2024-11-04T00:00:00"
        "Table":
        "FundId": null,
        "TradeDate": "2025-08-25T00:00:00",
        "LastUpdateDate": "0001-01-01T00:00:00",
        "PurchasePrice": 146.88,
        "SellPrice": 146.88,
        "CreationPrice": null,
        "DayYield": 0.04,
        "ShowDayYield": true,
        "Rate": 0,
        "ManagmentFee": 0.25,
        "TrusteeFee": 0.025,
        "SuccessFee": null,
        "AssetValue": 130.3
     */
    //@formatter:on

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
        assertThat(mockedresponse.length(), not(0));

        try
        {
            TLVFund fundfeed = Mockito.spy(new TLVFund());
            Mockito.doReturn(mockedresponse).when(fundfeed).rpcLatestQuoteFund(security);

            Optional<LatestSecurityPrice> oprice = fundfeed.getLatestQuote(security);

            assert (oprice.isPresent());

            LatestSecurityPrice p = oprice.get();
            assertThat(p.getHigh(), is(-1L));
            assertThat(p.getLow(), is(-1L));
            assertThat(p.getValue(), is(Values.Quote.factorize(155.97))); // 155.97
            assertThat(p.getDate(), is(LocalDate.of(2025, 8, 21)));

            verify(fundfeed, times(1)).rpcLatestQuoteFund(security);

        }
        catch (IOException e)
        {
            assert (false);
        }

    }



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
    public void mocked_fund_return_Historical_Quotes_though_getPriceHistory()
    {
        Security security = new Security();
        security.setWkn("5127121");
        security.setCurrencyCode("ILA");

        LocalDate from = LocalDate.of(2025, 7, 14);
        LocalDate to = LocalDate.of(2025, 8, 25);

        String response = getFundHistory();
        assertThat(response.length(), not(0));
        assertTrue(response.contains("Table"));
        assertTrue(response.contains("StartDate"));

        Gson gson = GSONUtil.createGson();
        FundHistory mockedFundHistory = gson.fromJson(response, FundHistory.class);

        try
        {
            TLVFund tlvFund = Mockito.spy(new TLVFund());
            Mockito.doReturn(Optional.of(mockedFundHistory)).when(tlvFund).getPriceHistory(security, from, to, 1,
                            Language.ENGLISH);

            Optional<FundHistory> fundHistoryOptional = tlvFund.getPriceHistory(security, from, to, 1,
                            Language.ENGLISH);
            assertThat(fundHistoryOptional.isPresent(), is(true));
            FundHistory fundHistory = fundHistoryOptional.get();

            assertTrue(fundHistory.getItems() != null);
            assertThat(fundHistory.getItems().length, is(30));

            FundHistoryEntry[] entries = mockedFundHistory.getItems();

            FundHistoryEntry firstprice = entries[0];
            FundHistoryEntry lastprice = entries[entries.length - 1];

            assertTrue(firstprice != null);
            assertThat(firstprice.getTradeDate(), is(to));

            assertThat(firstprice.getSellPrice(), is("146.88"));

            assertThat(lastprice.getTradeDate(), is(from));
            verify(tlvFund, times(1)).getPriceHistory(security, from, to, 1, Language.ENGLISH);

        }
        catch (Exception e)
        {
            e.printStackTrace();
            assertTrue(false);
        }


    }
    
    @Test
    public void mocked_fund_return_Historical_Quotes_though_getHistoricalQuotes()
    {
        Security security = new Security();
        security.setWkn("5127121");
        security.setCurrencyCode("ILA");

        LocalDate to = LocalDate.of(2025, 8, 25);

        String response = getFundHistory();
        assertThat(response.length(), not(0));
        assertTrue(response.contains("Table"));
        assertTrue(response.contains("StartDate"));

        FundHistory mockedhistory = FundHistory.fromJson(response);
        Optional<FundHistory> mockedhistoryopt = Optional.of(mockedhistory);


        try
        {
            TLVFund tlvFund = Mockito.spy(new TLVFund());
            Optional<QuoteFeedData> mockedFeed = tlvFund.convertFundHistoryToQuoteFeedData(mockedhistoryopt, security);

            doReturn(mockedFeed).when(tlvFund).getHistoricalQuotes(security, false);

            Optional<QuoteFeedData> fundQuoteFeedDataOptional = tlvFund.getHistoricalQuotes(security, false);
            verify(tlvFund, times(1)).getHistoricalQuotes(security, false);
            assertThat(fundQuoteFeedDataOptional.isPresent(), is(true));

            QuoteFeedData fundQuoteFeedData  = fundQuoteFeedDataOptional.get();

            assertTrue(fundQuoteFeedData.getPrices() != null);
            assertThat(fundQuoteFeedData.getPrices().size(), is(30));

            List<SecurityPrice> entries = fundQuoteFeedData.getPrices();

            SecurityPrice firstprice = entries.get(0);
            SecurityPrice lastprice = entries.get(entries.size() - 1);

            assertTrue(firstprice != null);
            assertThat(firstprice.getDate(), is(to));

            assertThat(firstprice.getValue(), is(Values.Quote.factorize(146.88)));
            verify(tlvFund, times(1)).getHistoricalQuotes(security, false);



        }
        catch (Exception e)
        {
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
        assertThat(response.length(), not(0));
        assertTrue(response.contains("Table"));
        assertTrue(response.contains("StartDate"));

        Gson gson = GSONUtil.createGson();
        FundHistory historyListing = gson.fromJson(response, FundHistory.class);

        try
        {
            TLVFund tlvFund = Mockito.spy(new TLVFund());

            // No need to mock as we are testing the conditions for WKN

            Optional<FundHistory> fundFeedDataOptional = tlvFund.getPriceHistory(security, from, to, 1,
                            Language.ENGLISH);
            assertThat(fundFeedDataOptional.isEmpty(), is(true));


        }
        catch (Exception e)
        {
            e.printStackTrace();
            assertTrue(false);
        }

        security.setWkn("");
        try
        {
            TLVFund tlvFund = Mockito.spy(new TLVFund());

            // No need to mock as we are testing the conditions for WKN

            Optional<FundHistory> fundFeedDataOptional = tlvFund.getPriceHistory(security, from, to, 1,
                            Language.ENGLISH);
            assertThat(fundFeedDataOptional.isEmpty(), is(true));

        }
        catch (Exception e)
        {
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
