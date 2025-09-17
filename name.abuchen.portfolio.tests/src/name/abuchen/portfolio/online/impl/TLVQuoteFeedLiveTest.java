package name.abuchen.portfolio.online.impl;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import org.junit.Ignore;
import org.junit.Test;

import name.abuchen.portfolio.model.LatestSecurityPrice;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.online.QuoteFeedData;

public class TLVQuoteFeedLiveTest
{

    /* Real Tests against API */
    //@formatter:off
    /*
     * Tests should include: 
     * Test that we get a valid list of Entities
     * Test that we get a valid quote for security, fund and share
     * Test that we get valid historical quotes for security, fund and share
     * Test getNames on security, fund and share
     */
    //@formatter:on


    @Test
    public void live_fund_returns_latest_quote() throws IOException
    {
        Security security = new Security();
        security.setWkn("5113428");
        security.setCurrencyCode("ILS"); // KSM KTF TEL GOV - Mutual Fund,
                                         // reported in ILS

        TLVQuoteFeed feed = new TLVQuoteFeed();
        try
        {
            Optional<LatestSecurityPrice> response = feed.getLatestQuote(security);

            assertFalse(response.isEmpty());

            LatestSecurityPrice price = response.get();
            assertTrue(price.getDate() != null);
            LocalDate date = price.getDate();
            Long daysdiff = ChronoUnit.DAYS.between(date, LocalDate.now());
            assertThat(daysdiff, lessThanOrEqualTo(3l));

            assertTrue(price.getValue() != 0l);
            assertThat(price.getHigh(), is(-1l));
            assertThat(price.getLow(), is(-1l));
            assertThat(price.getValue(), greaterThan(Values.Quote.factorize(100.00)));
            assertThat(price.getVolume(), is(-1l));

        }
        catch (Exception e)
        {
            System.out.println(e.getMessage());
            assertTrue(false);
        }

    }

    @Test
    public void live_bond_returns_latest_quote() throws IOException
    {
        Security security = new Security();
        security.setWkn("1410307");
        security.setCurrencyCode("ILA"); // Bond - reported in ILA - SHLD.B18

        TLVQuoteFeed feed = new TLVQuoteFeed();
        try
        {
            Optional<LatestSecurityPrice> response = feed.getLatestQuote(security);

            if (response.isEmpty())
                assertTrue(false);
            LatestSecurityPrice price = response.get();
            assertTrue(price.getDate() != null);

            LocalDate date = price.getDate();
            Long daysdiff = ChronoUnit.DAYS.between(date, LocalDate.now());
            assertTrue(daysdiff < 3l);
            assertTrue(price.getValue() != 0l);

            assertThat(price.getHigh(), greaterThan(Values.Quote.factorize(100.00)));
            assertThat(price.getLow(), greaterThan(Values.Quote.factorize(100.00)));
            assertThat(price.getValue(), greaterThan(Values.Quote.factorize(100.00)));
            assertThat(price.getVolume(), greaterThan(0l));

        }
        catch (Exception e)
        {
            System.out.println(e.getMessage());
            assertTrue(false);
        }

    }

    @Test
    public void live_bond_should_not_return_latest_quotes_without_wks()
    {
        Security security = new Security();
        security.setWkn("");
        security.setCurrencyCode("ILA"); // Bond - reported in ILA - SHLD.B18

        TLVQuoteFeed feed = new TLVQuoteFeed();
        try
        {
            Optional<LatestSecurityPrice> response = feed.getLatestQuote(security);

            if (response.isEmpty())
                assertTrue(true);

        }
        catch (Exception e)
        {
            System.out.println(e.getMessage());
            assertTrue(false);
        }

        security.setWkn("0000");
        try
        {
            Optional<LatestSecurityPrice> response = feed.getLatestQuote(security);

            if (response.isEmpty())
                assertTrue(true);

        }
        catch (Exception e)
        {
            System.out.println(e.getMessage());
            assertTrue(false);
        }

    }

    @Test
    public void live_stock_should_return_latest_quote()
    {

        Security security = new Security();
        security.setWkn("273011");
        security.setCurrencyCode("ILA"); // NICE Stock - reported in ILA

        TLVQuoteFeed feed = new TLVQuoteFeed();
        try
        {
            Optional<LatestSecurityPrice> response = feed.getLatestQuote(security);

            if (response.isEmpty())
                assertTrue(false);
            LatestSecurityPrice price = response.get();
            assertTrue(price.getDate() != null);
            LocalDate date = price.getDate();
            Long daysdiff = ChronoUnit.DAYS.between(date, LocalDate.now());
            assertTrue(daysdiff < 3l);
            assertTrue(price.getValue() != 0l);
            assertTrue(price.getVolume() != 0l);
            assertThat(price.getHigh(), greaterThan(Values.Quote.factorize(40000.00)));
            assertThat(price.getLow(), greaterThan(Values.Quote.factorize(40000.00)));
            assertThat(price.getValue(), greaterThan(Values.Quote.factorize(40000.00)));
            assertThat(price.getVolume(), greaterThan(0l));

        }
        catch (Exception e)
        {
            System.out.println(e.getMessage());
            assertTrue(false);
        }

    }





    @Test
    public void live_fund_should_return_historical_prices()
    {
        Security security = new Security();
        security.setWkn("5113428");
        security.setCurrencyCode("ILS"); // KSM KTF TEL GOV - Mutual Fund,
                                         // reported in ILS
        TLVQuoteFeed feed = new TLVQuoteFeed();

        QuoteFeedData prices = feed.getHistoricalQuotes(security, false);

        assertThat(prices.getPrices().size(), is(0));
    }

    @Test
    public void live_shares_should_return_historical_prices()
    {
        Security security = new Security();
        security.setWkn("273011");
        security.setCurrencyCode("ILA"); // NICE Stock - reported in ILA

        TLVQuoteFeed feed = new TLVQuoteFeed();

        QuoteFeedData prices = feed.getHistoricalQuotes(security, false);

        assertThat(prices.getPrices().size(), is(30));
    }

    @Test
    public void live_index_should_not_return_last_quotes()
    {
        Security security = new Security();
        security.setWkn("187");
        security.setCurrencyCode("ILS"); // Bond - reported in ILA - SHLD.B18


        TLVQuoteFeed feed = new TLVQuoteFeed();
        try
        {
            Optional<LatestSecurityPrice> response = feed.getLatestQuote(security);

            if (response.isEmpty())
                assertTrue(true);

        }
        catch (Exception e)
        {
            System.out.println(e.getMessage());
            assertTrue(false);
        }

        
    }

    @Ignore("Test not ready")
    @Test
    public void live_index_should_not_return_historical_quotes()
    {
        
    }

    @Test
    public void live_bond_should_return_historical_prices()
    {
        // LocalDate from = LocalDate.now().minusDays(30);
        // LocalDate to = LocalDate.now().minusDays(10);
        // DateTimeFormatter formatter =
        // DateTimeFormatter.ofPattern("dd/MM/yyyy");

        Security security = new Security();
        security.setWkn("1410307"); // Corporate Bond - reporting in ILA
        security.setCurrencyCode("ILA");

        // security.setWkn("5113428");
        // security.setCurrencyCode("ILS");

        TLVQuoteFeed feed = new TLVQuoteFeed();
        
        QuoteFeedData prices = feed.getHistoricalQuotes(security, false);

        assertThat(prices.getPrices().size(), is(0));
    }


    @Ignore
    @Test
    public void live_security_should_not_return_Historical_Prices_without_WKS()
    {
        //
    }
}
