package name.abuchen.portfolio.online.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;

import org.junit.Test;

import name.abuchen.portfolio.model.LatestSecurityPrice;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.online.impl.TLVMarket.jsondata.FundHistoryEntry;
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
        security.setCurrencyCode("ILS");

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
        security.setCurrencyCode("ILS");

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
        security.setCurrencyCode("ILS");

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

    @Test
    public void testFundHistoryPrices()
    {
        LocalDate from = LocalDate.now().minusDays(30);
        LocalDate to = LocalDate.now().minusDays(10);
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

        Security security = new Security();
        security.setWkn("5113428");
        security.setCurrencyCode("ILS");

        TLVQuoteFeed feed = new TLVQuoteFeed();
        Map<String, Object> pricehistory;
        try
        {
            pricehistory = feed.getPriceHistoryChunk2(security, from, to, 1, Language.ENGLISH);
            

            LocalDate start = null;
            LocalDate end = null;

            
            if (pricehistory.get("StartDate") != null)
            {
                start = LocalDate.parse((String) pricehistory.get("StartDate"), formatter);
                Long daysdiff = ChronoUnit.DAYS.between(start, from);
                assertTrue(daysdiff < 3l);
            }
            else
            {
                assertTrue(false);    
            }
            if (pricehistory.get("EndDate") != null)
            {
                end = LocalDate.parse((String) pricehistory.get("EndDate"), formatter);
                Long daysdiff = ChronoUnit.DAYS.between(end, from);
                assertTrue(daysdiff < 3l);
            }
            else
            {
                assertTrue(false);
            }
            if (pricehistory.get("Table") != null)
            {
                ArrayList<FundHistoryEntry> items = (ArrayList<FundHistoryEntry>) pricehistory.get("Table");
                // FundHistoryEntry entry = (FundHistoryEntry) items.get(0);
                @SuppressWarnings("unchecked")
                Map<String, Object> entry = (Map<String, Object>) items.get(0);
                FundHistoryEntry e = FundHistoryEntry.fromMap(entry);


                assertTrue(e.getRate() >= 0);
                assertTrue(e.getPurchasePrice() > 0);
                assertTrue(e.getSellPrice() > 0);
            }

        }
        catch (Exception e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
            assertTrue(false);
        }

    }

    @Test
    public void testSecurityHistoricalPrices()
    {
        LocalDate from = LocalDate.now().minusDays(30);
        LocalDate to = LocalDate.now().minusDays(10);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        Security security = new Security();
        security.setWkn("1410307");
        security.setCurrencyCode("ILS");

        TLVQuoteFeed feed = new TLVQuoteFeed();
        Map<String, Object> pricehistory;
        try
        {
            pricehistory = feed.getPriceHistoryChunk2(security, from, to, 1, Language.ENGLISH);
            
            LocalDate start = null;
            LocalDate end = null;

            
            if (pricehistory.get("DateFrom") != null)
            {
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
}
