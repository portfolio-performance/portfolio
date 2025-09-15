package name.abuchen.portfolio.online.impl;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.Ignore;
import org.junit.Test;

import name.abuchen.portfolio.model.LatestSecurityPrice;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.online.QuoteFeedData;
import name.abuchen.portfolio.online.impl.TLVMarket.TLVFund;
import name.abuchen.portfolio.online.impl.TLVMarket.jsondata.FundHistory;
import name.abuchen.portfolio.online.impl.TLVMarket.jsondata.SecurityHistory;
import name.abuchen.portfolio.online.impl.TLVMarket.jsondata.SecurityHistoryEntry;
import name.abuchen.portfolio.online.impl.TLVMarket.utils.TLVHelper.Language;

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
    public void testTLVFundDetailsAPI() throws IOException
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
    public void testTLVBondDetailsAPI() throws IOException
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
    public void TLVBondDetails_Should_not_return_value_without_wks()
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
    public void testTLVSecurityDetailsAPI() throws IOException
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


    @Ignore("Test not ready")
    @Test
    public void testFundHistoricalPrices()
    {
    }

    @Ignore("Test not ready")
    @Test
    public void testShareHistoricalPrices()
    {
    }

    @Test
    public void index_should_not_return_quotes()
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
    public void index_should_not_return_historical_quotes()
    {
        
    }

    @Test
    public void bond_should_Return_price_history()
    {
        LocalDate from = LocalDate.now().minusDays(30);
        LocalDate to = LocalDate.now().minusDays(10);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        Security security = new Security();
        security.setWkn("1410307"); // Corporate Bond - reporting in ILA
        security.setCurrencyCode("ILA");
        // security.setWkn("5113428");
        // security.setCurrencyCode("ILS");
        security.setWkn("273011");
        security.setCurrencyCode("ILA"); // NICE Stock - reported in ILA

        TLVQuoteFeed feed = new TLVQuoteFeed();
//        Map<String, Object> pricehistory;
        //Gson gson = GSONUtil.createGson();
        //FundHistory historyListing = gson.fromJson(response, FundHistory.class);
        
        try
        {
            LocalDate start = null;
            LocalDate end = null;

            Map<String, Object> pricehistory = feed.getPriceHistoryChunk2(security, from, to, 1, Language.ENGLISH);


            if (pricehistory.get("DateFrom") != null)
            {
                assertTrue(false);
                new SecurityHistory();
                SecurityHistory fundHistory = SecurityHistory.fromMap(pricehistory);

                // Optional<QuoteFeedData> fundFeedDataOptional =
                // tlvFund.convertFundHistoryToQuoteFeedData(fundHistory,
                // security);
                //
                // assertThat(fundFeedDataOptional.isPresent(), is(true));
                // QuoteFeedData fundFeedData = fundFeedDataOptional.get();
                //
                // assertTrue(fundFeedData.getPrices() != null);
                // assertThat(fundFeedData.getPrices().size(), is(30));
                //
                // List<SecurityPrice> listPrices = fundFeedData.getPrices();
                //
                // SecurityPrice firstprice = fundFeedData.getPrices().get(0);
                // SecurityPrice lastprice =
                // fundFeedData.getPrices().get(listPrices.size() - 1);
                // assertTrue(firstprice != null);
                //
                // assertThat(firstprice.getDate(), is(to));
                //
                // assertThat(firstprice.getValue(),
                // is(Values.Quote.factorize(130.30)));
                //
                // assertThat(lastprice.getDate(), is(from));
                // assertThat(firstprice.getValue(),
                // is(Values.Quote.factorize(130.30)));

                start = LocalDate.parse((String) pricehistory.get("DateFrom"), formatter);
                Long daysdiff = ChronoUnit.DAYS.between(start, from);
                assertTrue(daysdiff < 3l);
            }
            else
            {
                assertTrue(false);    
            }
            if (pricehistory.get("DateTo") != null)
            {
                new FundHistory();
                Optional<FundHistory> fundHistory = Optional.of(FundHistory.fromMap(pricehistory));
                TLVFund tlvFund = new TLVFund();

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
                end = LocalDate.parse((String) pricehistory.get("DateTo"), formatter);
                Long daysdiff = ChronoUnit.DAYS.between(end, from);
                assertTrue(daysdiff < 3l);
            }
            else
            {
                assertTrue(false);
            }
            if (pricehistory.get("Table") != null)
            {
                ArrayList<SecurityHistoryEntry> items = (ArrayList<SecurityHistoryEntry>) pricehistory.get("Table");

                @SuppressWarnings("unchecked")
                Map<String, Object> entry = (Map<String, Object>) items.get(0);
                SecurityHistoryEntry e = SecurityHistoryEntry.fromMap(entry);


                assertTrue(e.getHighRate() > 0);
                assertTrue(e.getLowRate() > 0);
                assertTrue(e.getBaseRate() > 0);
            }

        }
        catch (Exception e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
            assertTrue(false);
        }
    }

    @Ignore
    @Test
    public void bonds_should_return_Historical_Prices()
    {
        //
    }

    @Ignore
    @Test
    public void security_should_not_return_Historical_Prices_without_WKS()
    {
        //
    }
}
