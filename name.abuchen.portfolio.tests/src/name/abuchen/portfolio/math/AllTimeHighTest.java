package name.abuchen.portfolio.math;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.time.LocalDate;

import org.junit.Before;
import org.junit.Test;

import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.util.Interval;

public class AllTimeHighTest
{
    private Security securityOnePrice;
    private Security securityTenPrices;
    private Security securitySevenPricesWithGaps;

    @Before
    public void setUp()
    {
        securityOnePrice = new Security();
        securityTenPrices = new Security();
        securitySevenPricesWithGaps = new Security();

        Long[] pricesSecurityOne = { 0L };
        this.buildPrices(this.securityOnePrice, pricesSecurityOne);

        Long[] pricesSecuritySeven = { 1323L, null, 2062L, null, 2000L, 1567L, null };
        this.buildPrices(this.securitySevenPricesWithGaps, pricesSecuritySeven);

        Long[] pricesSecurityTen = { 210L, 699L, 699L, 2133L, 2074L, 1854L, 1500L, 9L, 900L, 1121L };
        this.buildPrices(this.securityTenPrices, pricesSecurityTen);
    }

    private void buildPrices(Security security, Long[] prices)
    {
        int i = 1;
        for (Long price : prices)
        {
            i++;
            if (price == null)
            {
                continue;
            }

            // start on 2022-06-02
            LocalDate date = LocalDate.of(2022, 06, i);
            security.addPrice(new SecurityPrice(date, price));
        }
    }

    @Test
    public void testSecurityIsNull()
    {
        Interval interval = Interval.of(securityOnePrice.getPrices().get(0).getDate(), LocalDate.now());
        AllTimeHigh ath = new AllTimeHigh(null, interval);

        assertNull(ath.getDistance());
        assertNull(ath.getValue());
        assertNull(ath.getDate());
    }

    @Test
    public void testSecurityHasOnlyOnePrice()
    {
        Interval interval = Interval.of(securityOnePrice.getPrices().get(0).getDate(), LocalDate.now());
        AllTimeHigh ath = new AllTimeHigh(this.securityOnePrice, interval);

        assertNull(ath.getValue());
        assertNull(ath.getDistance());
        assertNull(ath.getDate());
    }

    @Test
    public void testAthValueFor10Prices()
    {
        Interval interval = Interval.of(securityTenPrices.getPrices().get(0).getDate(), LocalDate.now());
        AllTimeHigh ath = new AllTimeHigh(this.securityTenPrices, interval);

        assertEquals(Long.valueOf(2133L), ath.getValue());
        assertEquals(-0.47444, ath.getDistance(), 0.00001);
        assertEquals(LocalDate.of(2022, 6, 5), ath.getDate());
    }

    @Test
    public void testAthValueFor7PricesWithDateGaps()
    {
        Interval interval = Interval.of(this.securitySevenPricesWithGaps.getPrices().get(0).getDate(), LocalDate.now());
        AllTimeHigh ath = new AllTimeHigh(this.securitySevenPricesWithGaps, interval);

        assertEquals(Long.valueOf(2062L), ath.getValue());
        assertEquals(-0.24005, ath.getDistance(), 0.00001);
        assertEquals(LocalDate.of(2022, 6, 4), ath.getDate());
    }

    @Test
    public void testAthValueFor10PricesForLargerInterval()
    {
        Interval interval = Interval.of(LocalDate.of(2021, 1, 1), LocalDate.of(2023, 12, 31));
        AllTimeHigh ath = new AllTimeHigh(this.securityTenPrices, interval);

        assertEquals(Long.valueOf(2133L), ath.getValue());
        assertEquals(-0.47444, ath.getDistance(), 0.00001);
        assertEquals(LocalDate.of(2022, 6, 5), ath.getDate());
    }

    @Test
    public void testAthValueFor10PricesForLast2DaysInterval()
    {
        Interval interval = Interval.of(LocalDate.of(2022, 6, 10), LocalDate.of(2022, 6, 11));
        AllTimeHigh ath = new AllTimeHigh(this.securityTenPrices, interval);

        assertEquals(Long.valueOf(1121L), ath.getValue());
        assertEquals(0, ath.getDistance(), 0.00001);
        assertEquals(LocalDate.of(2022, 6, 11), ath.getDate());
    }

    @Test
    public void testAthValueFor10PricesFor5DaysInterval()
    {
        Interval interval = Interval.of(LocalDate.of(2022, 6, 7), LocalDate.of(2022, 6, 10));
        AllTimeHigh ath = new AllTimeHigh(this.securityTenPrices, interval);

        assertEquals(Long.valueOf(1500L), ath.getValue());
        assertEquals(-0.4, ath.getDistance(), 0.00001);
        assertEquals(LocalDate.of(2022, 6, 8), ath.getDate());
    }

    @Test
    public void testAthValueFor10PricesForFirst3DaysInterval()
    {
        Interval interval = Interval.of(LocalDate.of(2022, 1, 31), LocalDate.of(2022, 6, 4));
        AllTimeHigh ath = new AllTimeHigh(this.securityTenPrices, interval);

        assertEquals(Long.valueOf(699L), ath.getValue());
        assertEquals(0, ath.getDistance(), 0.00001);
        assertEquals(LocalDate.of(2022, 6, 3), ath.getDate());
    }
}
