package name.abuchen.portfolio.ui.views;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;

import org.hamcrest.core.IsNull;
import org.junit.Before;
import org.junit.Test;

import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.views.SecuritiesChart.ChartInterval;

@SuppressWarnings("nls")
public class SimpleMovingAverageTest
{
    private Security securityOnePrice;
    private Security securityTenPrices;
    private Security securitySevenPricesWithGaps; // no prices for 06.01.2017,
                                                  // 07.01.2017 and 08.01.2017

    @Before
    public void prepareSecurity()
    {

        securityOnePrice = new Security();
        securityTenPrices = new Security();
        securitySevenPricesWithGaps = new Security();
        SecurityPrice price = new SecurityPrice();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        formatter = formatter.withLocale(Locale.GERMANY);
        LocalDate date = LocalDate.parse("01.01.2017", formatter);
        price.setDate(date);
        price.setValue(0);
        securityOnePrice.addPrice(price);

        int i = 1;
        while (i <= 10)
        {
            if (i < 10)
                date = LocalDate.parse("0" + i + ".01.2017", formatter);
            else
                date = LocalDate.parse(i + ".01.2017", formatter);

            price = new SecurityPrice();
            price.setDate(date);
            price.setValue(i);
            securityTenPrices.addPrice(price);
            if (i != 8 && i != 7 && i != 6) // no prices for 06.01.2017,
                                            // 07.01.2017 and 08.01.2017
                securitySevenPricesWithGaps.addPrice(price);
            i++;
        }

    }

    @Test
    public void testSecurityHasOnlyOnePrice()
    {
        ChartInterval interval = new ChartInterval(securityOnePrice.getPrices().get(0).getDate(), LocalDate.now());
        ChartLineSeriesAxes sma = new SimpleMovingAverage(200, this.securityOnePrice, interval).getSMA();
        assertThat(sma.getDates(), is(IsNull.nullValue()));
    }

    @Test
    public void testSecurityIsNull()
    {
        ChartInterval interval = new ChartInterval(securityOnePrice.getPrices().get(0).getDate(), LocalDate.now());
        ChartLineSeriesAxes sma = new SimpleMovingAverage(200, null, interval).getSMA();
        assertThat(sma.getDates(), is(IsNull.nullValue()));
    }

    @Test
    public void testCorrectSMAEntries()
    {
        ChartInterval interval = new ChartInterval(securityTenPrices.getPrices().get(0).getDate(), LocalDate.now());
        ChartLineSeriesAxes sma = new SimpleMovingAverage(10, this.securityTenPrices, interval).getSMA();
        assertThat(sma, is(IsNull.notNullValue()));
        assertThat(sma.getValues().length, is(1));
        assertThat(sma.getValues()[0], is((1 + 2 + 3 + 4 + 5 + 6 + 7 + 8 + 9 + 10) / Values.Quote.divider() / 10));
    }

    @Test
    public void testCorrectSMAEntries_CorrectMultipleSMA_PricesWithDateGaps()
    {
        ChartInterval interval = new ChartInterval(this.securitySevenPricesWithGaps.getPrices().get(4).getDate(),
                        LocalDate.now());
        ChartLineSeriesAxes sma = new SimpleMovingAverage(5, this.securitySevenPricesWithGaps, interval).getSMA();
        assertThat(sma, is(IsNull.notNullValue()));
        assertThat(sma.getValues(), is(IsNull.notNullValue()));
        assertThat(sma.getValues().length, is(3));
        assertThat(sma.getValues()[0], is((1 + 2 + 3 + 4 + 5) / Values.Quote.divider() / 5));
        assertThat(sma.getValues()[1], is((2 + 3 + 4 + 5 + 9) / Values.Quote.divider() / 5));
        assertThat(sma.getValues()[2], is((3 + 4 + 5 + 9 + 10) / Values.Quote.divider() / 5));
    }

    @Test
    public void testSufficientPriceDataPass()
    {
        Security security = new Security();

        LocalDate date = LocalDate.parse("2016-01-01");
        for (int ii = 0; ii < 300; ii++)
        {
            security.addPrice(new SecurityPrice(date, Values.Quote.factorize(10)));
            date = date.plusDays(1);
        }

        ChartInterval interval = new ChartInterval(security.getPrices().get(0).getDate(), LocalDate.now());

        ChartLineSeriesAxes sma = new SimpleMovingAverage(10, security, interval).getSMA();
        assertThat(sma, is(IsNull.notNullValue()));
        assertThat(sma.getValues().length, is(security.getPrices().size() - 10 + 1));
    }

    @Test
    public void testSufficientPriceDataStartDate()
    {
        Security security = new Security();

        LocalDate date = LocalDate.parse("2016-01-01");
        for (int ii = 0; ii < 300; ii++)
        {
            security.addPrice(new SecurityPrice(date, Values.Quote.factorize(10)));
            date = date.plusDays(1);
        }
        LocalDate startDate = LocalDate.parse("2016-06-01");
        Date isStartDate = java.sql.Date.valueOf(startDate);
        ChartLineSeriesAxes sma = new SimpleMovingAverage(10, security, new ChartInterval(startDate, LocalDate.now()))
                        .getSMA();
        assertThat(sma, is(IsNull.notNullValue()));
        assertThat(sma.getDates()[0], is(isStartDate));
    }

}
