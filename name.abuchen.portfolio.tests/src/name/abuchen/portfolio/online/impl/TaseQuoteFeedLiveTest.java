package name.abuchen.portfolio.online.impl;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.not;
// import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import org.junit.Test;

import name.abuchen.portfolio.model.LatestSecurityPrice;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.online.QuoteFeedData;
import name.abuchen.portfolio.online.impl.TLVMarket.jsondata.IndiceListing;
import name.abuchen.portfolio.online.impl.TLVMarket.utils.TLVHelper.SecuritySubType;
import name.abuchen.portfolio.online.impl.TLVMarket.utils.TLVHelper.SecurityType;

public class TLVQuoteFeedLiveTest
{

    @Test
    public void random_fund_returns_latest_quote_and_history()
    {
        Security security = new Security();
        security.setCurrencyCode("ILS"); // KSM KTF TEL GOV - Mutual Fund,
                                         // reported in ILS

        TLVQuoteFeed tlvFeed = new TLVQuoteFeed();
        List<IndiceListing> mappedEntities = tlvFeed.getTLVEntities();

        Optional<IndiceListing> randomFund = mappedEntities.stream()
                        .filter(e -> e.getType() == SecurityType.MUTUAL_FUND.getValue() && e.getSubType() == null)
                        .findAny();

        if (randomFund.isPresent())
        {
            System.out.println("Test random TVL Fund: " + randomFund.get().getId());
            security.setWkn(randomFund.get().getId());
            try
            {
                Optional<LatestSecurityPrice> response = tlvFeed.getLatestQuote(security);

                if (response.isEmpty())
                    fail("Expected a response");
                LatestSecurityPrice price = response.get();
                assertTrue(price.getDate() != null);

                LocalDate date = price.getDate();
                Long daysdiff = ChronoUnit.DAYS.between(date, LocalDate.now());
                assertThat(daysdiff, lessThan(4L));
                assertTrue(price.getValue() != 0l);

                assertThat(price.getHigh(), not(0L)); // (Values.Quote.factorize(0.00)));
                assertThat(price.getLow(), is(-1L));
                assertThat(price.getVolume(), is(-1L));
                System.out.println("Test random TVL Fund: " + randomFund.get().getId()
                                + " getLatestQuote for TVL Fund passed");
                

            }
            catch (Exception e)
            {
                System.out.println(e.getMessage());
                assertTrue(false);
            }

            QuoteFeedData prices = tlvFeed.getHistoricalQuotes(security, false);

            assertThat(prices.getPrices().size(), greaterThan(1));
            assertThat(prices.getLatestPrices().size(), greaterThan(1));

            SecurityPrice secprice = prices.getPrices().get(0);
            assertTrue(secprice.getDate() instanceof LocalDate);

            assertThat(secprice.getValue(), greaterThan(0L));

            // Funds have Purchase and Sell, not high and low
            LatestSecurityPrice latestsecprice = prices.getLatestPrices().get(0);
            assertThat(latestsecprice.getHigh(), is(0L));
            assertThat(latestsecprice.getLow(), is(0L));
            assertThat(latestsecprice.getValue(), greaterThan(0L));
            assertThat(latestsecprice.getVolume(), is(0L));
            System.out.println("Test random TVL Fund: " + randomFund.get().getId()
                            + " getHistoricalQuotes for TVL Fund passed");

        }
        else
        {
            assertTrue(false);
        }

    }



    @Test
    public void random_bond_returns_latest_quote_and_history() throws IOException
    {
        Security security = new Security();
        // security.setWkn("1410307");
        security.setCurrencyCode("ILA"); // Bond - reported in ILA - SHLD.B18

        TLVQuoteFeed tlvFeed = new TLVQuoteFeed();
        List<IndiceListing> mappedEntities = tlvFeed.getTLVEntities();

        Optional<IndiceListing> randomBond = mappedEntities.stream()
                        .filter(e -> e.getType() == SecurityType.SECURITY.getValue()
                                        && Integer.valueOf(e.getSubType()) == SecuritySubType.CORPORATE_BONDS
                                                        .getValue())
                        .findAny();

        if (randomBond.isPresent())
        {
            System.out.println("Test random TVL Bond: " + randomBond.get().getId());
            security.setWkn(randomBond.get().getId());

            try
            {
                Optional<LatestSecurityPrice> response = tlvFeed.getLatestQuote(security);

                if (response.isEmpty())
                    assertTrue(false);
                LatestSecurityPrice price = response.get();
                assertTrue(price.getDate() != null);

                LocalDate date = price.getDate();
                Long daysdiff = ChronoUnit.DAYS.between(date, LocalDate.now());
                assertThat(daysdiff, lessThanOrEqualTo(3L));
                assertThat(price.getValue(), not(0L));

                assertThat(price.getHigh(), greaterThan(0L));
                assertThat(price.getLow(), greaterThan(0L));
                assertThat(price.getValue(), greaterThan(100L));
                assertThat(price.getVolume(), greaterThan(0l));
                System.out.println("Test random TVL Bond: " + randomBond.get().getId()
                                + " getLatestQuote for TVL Bond passed");

            }
            catch (Exception e)
            {
                System.out.println(e.getMessage());
                assertTrue(false);
            }

            QuoteFeedData prices = tlvFeed.getHistoricalQuotes(security, false);

            assertThat(prices.getPrices().size(), greaterThan(1));
            assertThat(prices.getLatestPrices().size(), greaterThan(1));

            SecurityPrice secprice = prices.getPrices().get(0);
            assertTrue(secprice.getDate() instanceof LocalDate);

            assertThat(secprice.getValue(), greaterThan(0L));

            LatestSecurityPrice latestsecprice = prices.getLatestPrices().get(0);
            assertThat(latestsecprice.getHigh(), greaterThan(0L));
            assertThat(latestsecprice.getLow(), greaterThan(0L));
            assertThat(latestsecprice.getValue(), greaterThan(0L));
            assertThat(latestsecprice.getVolume(), greaterThan(100L));
            System.out.println("Test random TVL Bond: " + randomBond.get().getId()
                            + " getHistoricalQuotes for TVL Bond passed");

        }
    }



    @Test
    public void bond_should_not_return_latest_quotes_without_wks()
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
    public void random_stock_should_return_latest_quote_and_history()
    {

        Security security = new Security();
        // security.setWkn("273011");
        security.setCurrencyCode("ILA"); // NICE Stock - reported in ILA

        TLVQuoteFeed tlvFeed = new TLVQuoteFeed();
        List<IndiceListing> mappedEntities = tlvFeed.getTLVEntities();

        Optional<IndiceListing> randomFund = mappedEntities.stream()
                        .filter(e -> e.getType() == SecurityType.SECURITY.getValue()
                                        && Integer.valueOf(e.getSubType()) == SecuritySubType.SHARES.getValue())
                        .findAny();

        if (randomFund.isPresent())
        {
            System.out.println("Test random TVL Stock: " + randomFund.get().getId());
            security.setWkn(randomFund.get().getId());
            try
            {
                Optional<LatestSecurityPrice> response = tlvFeed.getLatestQuote(security);

                if (response.isEmpty())
                    assertTrue(false);
                LatestSecurityPrice price = response.get();
                assertTrue(price.getDate() != null);
                LocalDate date = price.getDate();
                Long daysdiff = ChronoUnit.DAYS.between(date, LocalDate.now());
                assertThat(daysdiff, lessThanOrEqualTo(3l));

                assertThat(price.getValue(), greaterThan(0L));
                assertThat(price.getVolume(), is(0l));
                System.out.println("\tgetLatestQuote for TVL Stock passed");

            }
            catch (Exception e)
            {
                System.out.println(e.getMessage());
                assertTrue(false);
            }

            QuoteFeedData prices = tlvFeed.getHistoricalQuotes(security, false);

            assertThat(prices.getPrices().size(), greaterThan(1));
            assertThat(prices.getLatestPrices().size(), greaterThan(1));

            SecurityPrice secprice = prices.getPrices().get(0);
            assertTrue(secprice.getDate() instanceof LocalDate);

            assertThat(secprice.getValue(), greaterThan(0L));

            LatestSecurityPrice latestsecprice = prices.getLatestPrices().get(0);
            assertThat(latestsecprice.getHigh(), is(0L));
            assertThat(latestsecprice.getLow(), is(0L));
            assertThat(latestsecprice.getValue(), greaterThan(0L));
            assertThat(latestsecprice.getVolume(), is(0L));
            System.out.println("\tgetHistoricalQuotes for TVL Stock passed");

        }

    }









    @Test
    public void live_index_should_not_return_last_quotes_and_history()
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
        QuoteFeedData prices = feed.getHistoricalQuotes(security, false);

        assertThat(prices.getPrices().size(), is(0));
        assertThat(prices.getLatestPrices().size(), is(0));

        
    }

    @Test
    public void live_index_should_not_return_historical_quotes()
    {
        Security security = new Security();
        security.setCurrencyCode("ILS"); // KSM KTF TEL GOV - Mutual Fund,
                                         // reported in ILS

        TLVQuoteFeed tlvFeed = new TLVQuoteFeed();
        List<IndiceListing> mappedEntities = tlvFeed.getTLVEntities();

        Optional<IndiceListing> randomFund = mappedEntities.stream()
                        .filter(e -> e.getType() == SecurityType.INDEX.getValue() && e.getSubType() == null)
                        .findAny();

        if (randomFund.isPresent())
        {
            System.out.println("Test random Index: " + randomFund.get().getId());
            security.setWkn(randomFund.get().getId());
            try
            {
                Optional<LatestSecurityPrice> response = tlvFeed.getLatestQuote(security);

                assertTrue(response.isEmpty());

            }
            catch (Exception e)
            {
                System.out.println(e.getMessage());
                assertTrue(false);
            }

            QuoteFeedData prices = tlvFeed.getHistoricalQuotes(security, false);

            assertThat(prices.getPrices().size(), is(0));
            assertThat(prices.getLatestPrices().size(), is(0));


            System.out.println("Test random TVL Indx: " + randomFund.get().getId()
                            + " getHistoricalQuotes for TVL Index passed");

        }
        else
        {
            assertTrue(false);
        }
    }



    @Test
    public void live_security_should_not_return_Historical_Prices_without_WKS()
    {
        Security security = new Security();
        security.setCurrencyCode("ILA"); // NICE Stock - reported in ILA

        TLVQuoteFeed tlvFeed = new TLVQuoteFeed();

        try
        {
            Optional<LatestSecurityPrice> response = tlvFeed.getLatestQuote(security);

            if (!response.isEmpty())
                assertTrue(false);


        }
        catch (Exception e)
        {
            System.out.println(e.getMessage());
            assertTrue(false);
        }

        QuoteFeedData prices = tlvFeed.getHistoricalQuotes(security, false);

        assertThat(prices.getPrices().size(), is(0));
        assertThat(prices.getLatestPrices().size(), is(0));


    }
}
