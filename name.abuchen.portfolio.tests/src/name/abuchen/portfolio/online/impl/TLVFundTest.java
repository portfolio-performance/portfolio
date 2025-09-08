package name.abuchen.portfolio.online.impl;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Scanner;

import org.junit.Test;
import org.mockito.Mockito;

import name.abuchen.portfolio.model.LatestSecurityPrice;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.online.impl.TLVMarket.TLVFund;

public class TLVFundTest
{

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
    public void testGetFundLatestQuote()
    {
        Security security = new Security();
        security.setCurrencyCode("ILS");
        security.setWkn("01135912");
        String response = getFundDetails();

        try
        {
            TLVFund fundfeed = Mockito.spy(new TLVFund());
            Mockito.doReturn(response).when(fundfeed).rpcLatestQuoteFund(security);

            // System.out.println(feed.getLatestQuote(security));
            Optional<LatestSecurityPrice> oprice = fundfeed.getLatestQuote(security);

            assert (oprice.isPresent());

            LatestSecurityPrice p = oprice.get();
            assertTrue(p.getHigh() == -1);
            assertTrue(p.getLow() == -1);
            System.out.println(p.getValue());
            assertTrue(p.getValue() == 15597000000l); // 155.97
            assertTrue(p.getDate().equals(LocalDate.of(2025, 8, 21)));

        }
        catch (IOException e)
        {
            assert (false);
        }

    }

}
