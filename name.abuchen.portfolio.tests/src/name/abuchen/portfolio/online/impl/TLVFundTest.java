package name.abuchen.portfolio.online.impl;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Scanner;

import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;

import name.abuchen.portfolio.model.LatestSecurityPrice;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.online.impl.TLVMarket.TLVFund;

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

    @Ignore("This class is under development and not ready for testing")
    @Test
    public void quoteHistoryonValidFundReturnsCorrectValues()
    {
        //
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
