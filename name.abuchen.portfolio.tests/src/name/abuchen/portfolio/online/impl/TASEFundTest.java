package name.abuchen.portfolio.online.impl;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
// import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

import org.junit.Test;
import org.mockito.Mockito;

import name.abuchen.portfolio.model.LatestSecurityPrice;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.online.QuoteFeedData;
import name.abuchen.portfolio.online.impl.TASE.TASEFund;
import name.abuchen.portfolio.online.impl.TASE.jsondata.FundHistory;
import name.abuchen.portfolio.online.impl.TASE.jsondata.FundHistoryEntry;
import name.abuchen.portfolio.online.impl.TASE.utils.TASEHelper.Language;

public class TASEFundTest
{

    /**
     * Load JSON response of a fund details request
     */
    private String getFundDetails()
    {
        return getHistoricalTaseQuotes("response_tase_fund_details01.txt");
    }

    /**
     * Load JSON response of a fund historical request
     */
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
        assertThat(mockedresponse.length(), not(0));

        try
        {
            TASEFund fundfeed = Mockito.spy(new TASEFund());
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
            fail("Exception not expected" + e.getMessage());
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
            TASEFund fundfeed = Mockito.spy(new TASEFund());
            Mockito.doReturn(response).when(fundfeed).rpcLatestQuoteFund(security);

            // System.out.println(feed.getLatestQuote(security));
            Optional<LatestSecurityPrice> oprice = fundfeed.getLatestQuote(security);

            assert (oprice.isEmpty());
            verify(fundfeed, times(0)).rpcLatestQuoteFund(security);


        }
        catch (IOException e)
        {
            fail("Exception not expected" + e.getMessage());
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
            TASEFund fundfeed = Mockito.spy(new TASEFund());
            Mockito.doReturn(response).when(fundfeed).rpcLatestQuoteFund(security);

            // System.out.println(feed.getLatestQuote(security));
            Optional<LatestSecurityPrice> oprice = fundfeed.getLatestQuote(security);

            assert (oprice.isEmpty());

            // Verify interaction
            verify(fundfeed, times(0)).rpcLatestQuoteFund(security);

        }
        catch (IOException e)
        {
            fail("Exception not expected" + e.getMessage());
        }

    }

    
    @Test
    public void mocked_fund_return_Historical_Quotes_though_getPriceHistory()
    {
        Security security = new Security();
        security.setWkn("5127121");
        security.setCurrencyCode("ILA");

        LocalDate from = LocalDate.of(2025, 8, 17);
        LocalDate to = LocalDate.of(2025, 8, 25);

        String response = getFundHistory();
        assertThat(response.length(), not(0));
        assertTrue(response.contains("Table"));
        assertTrue(response.contains("StartDate"));

        new FundHistory();

        FundHistory mockedFundHistory = FundHistory.fromJson(response);

        try
        {
            TASEFund tlvFund = Mockito.spy(new TASEFund());
            Mockito.doReturn(Optional.of(mockedFundHistory)).when(tlvFund).getPriceHistory(security, from, to, 1,
                            Language.ENGLISH);

            Optional<FundHistory> fundHistoryOptional = tlvFund.getPriceHistory(security, from, to, 1,
                            Language.ENGLISH);
            assertThat(fundHistoryOptional.isPresent(), is(true));
            FundHistory fundHistory = fundHistoryOptional.get();

            assertTrue(fundHistory.getItems() != null);
            assertThat(fundHistory.getItems().length, is(7));

            FundHistoryEntry[] entries = mockedFundHistory.getItems();


            FundHistoryEntry firstprice = entries[0];

            assertTrue(firstprice != null);
            assertThat(firstprice.getTradeDate(), is(to));

            assertThat(firstprice.getSellPrice(), is("146.88"));

            FundHistoryEntry lastprice = entries[entries.length - 1];
            assertThat(lastprice.getTradeDate(), is(from));

            verify(tlvFund, times(1)).getPriceHistory(security, from, to, 1, Language.ENGLISH);

        }
        catch (Exception e)
        {
            fail("Exception not expected" + e.getMessage());
        }


    }
    
    @Test
    public void mocked_fund_return_Historical_Quotes_though_getHistoricalQuotes()
    {
        Security security = new Security();
        security.setWkn("5127121");
        security.setCurrencyCode("ILS");

        LocalDate to = LocalDate.of(2025, 8, 25);

        String response = getFundHistory();
        assertThat(response.length(), not(0));
        assertTrue(response.contains("Table"));
        assertTrue(response.contains("StartDate"));

        FundHistory mockedhistory = FundHistory.fromJson(response);
        Optional<FundHistory> mockedhistoryopt = Optional.of(mockedhistory);


        try
        {
            TASEFund tlvFund = Mockito.spy(new TASEFund());
            Optional<QuoteFeedData> mockedFeed = tlvFund.convertFundHistoryToQuoteFeedData(mockedhistoryopt, security);

            doReturn(mockedFeed).when(tlvFund).getHistoricalQuotes(security, false);

            Optional<QuoteFeedData> fundQuoteFeedDataOptional = tlvFund.getHistoricalQuotes(security, false);
            verify(tlvFund, times(1)).getHistoricalQuotes(security, false);
            assertThat(fundQuoteFeedDataOptional.isPresent(), is(true));

            QuoteFeedData fundQuoteFeedData  = fundQuoteFeedDataOptional.get();


            assertTrue(fundQuoteFeedData.getPrices() != null);
            assertThat(fundQuoteFeedData.getPrices().size(), is(7));

            List<SecurityPrice> entries = fundQuoteFeedData.getPrices();

            SecurityPrice firstprice = entries.get(0);

            assertTrue(firstprice != null);
            assertThat(firstprice.getDate(), is(to));
            assertThat(firstprice.getValue(), is(Values.Quote.factorize(146.88)));

            verify(tlvFund, times(1)).getHistoricalQuotes(security, false);



        }
        catch (Exception e)
        {
            fail("Exception not expected" + e.getMessage());
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


        try
        {
            TASEFund tlvFund = Mockito.spy(new TASEFund());

            // No need to mock as we are testing the conditions for WKN

            Optional<FundHistory> fundFeedDataOptional = tlvFund.getPriceHistory(security, from, to, 1,
                            Language.ENGLISH);
            assertThat(fundFeedDataOptional.isEmpty(), is(true));


        }
        catch (Exception e)
        {
            fail("Exception not expected" + e.getMessage());
        }

        security.setWkn("");
        try
        {
            TASEFund tlvFund = Mockito.spy(new TASEFund());

            // No need to mock as we are testing the conditions for WKN in the
            // function and will reject if needed

            Optional<FundHistory> fundFeedDataOptional = tlvFund.getPriceHistory(security, from, to, 1,
                            Language.ENGLISH);
            assertThat(fundFeedDataOptional.isEmpty(), is(true));

        }
        catch (Exception e)
        {
            fail("Exception not expected" + e.getMessage());
        }

    }



}
