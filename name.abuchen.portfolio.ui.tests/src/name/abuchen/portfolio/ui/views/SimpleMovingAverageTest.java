package name.abuchen.portfolio.ui.views;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

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
import name.abuchen.portfolio.ui.views.ChartLineSeriesAxes;
import name.abuchen.portfolio.ui.views.SimpleMovingAverage;

@SuppressWarnings("nls")
public class SimpleMovingAverageTest
{
    private Security securityOnePrice;
    private Security securityTenPrices;

    @Before
    public void prepareSecurity()
    {

        securityOnePrice = new Security();
        securityTenPrices = new Security();
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
            i++;
        }

    }

    @Test
    public void testSecurityHasOnlyOnePrice()
    {
        ChartLineSeriesAxes SMALines = new SimpleMovingAverage(200, this.securityOnePrice, null).getSMA();
        assertThat(SMALines.getDates(), is(IsNull.nullValue()));
    }

    @Test
    public void testSecurityIsNull()
    {
        ChartLineSeriesAxes SMALines = new SimpleMovingAverage(200, null, null).getSMA();
        assertThat(SMALines.getDates(), is(IsNull.nullValue()));
    }

    @Test
    public void testCorrectSMAEntries()
    {
        ChartLineSeriesAxes SMALines = new SimpleMovingAverage(10, this.securityTenPrices, null).getSMA();
        assertThat(SMALines, is(IsNull.notNullValue()));
        assertThat(SMALines.getValues().length, is(1));
        assertThat(SMALines.getValues()[0], is((1 + 2 + 3 + 4 + 5 + 6 + 7 + 8 + 9 + 10) / Values.Quote.divider() / 10));
    }

    @Test
    public void testSecurityHasSparsePrice()
    {
        Security security = new Security();

        LocalDate date = LocalDate.parse("2016-01-01");
        for (int ii = 0; ii < 100; ii++)
        {
            security.addPrice(new SecurityPrice(date, Values.Quote.factorize(10)));
            date = date.plusDays(1);
        }

        security.addPrice(new SecurityPrice(LocalDate.parse("2017-01-01"), Values.Quote.factorize(12)));
        LocalDate tmp = LocalDate.parse("2016-01-01");
        tmp = tmp.plusDays(99);
        Date lastSMADate = java.sql.Date.valueOf(tmp);

        ChartLineSeriesAxes SMALines = new SimpleMovingAverage(10, security, null).getSMA();
        assertThat(SMALines.getDates(), is(IsNull.notNullValue()));
        assertThat(SMALines.getDates()[SMALines.getDates().length - 1], is(lastSMADate));
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

        ChartLineSeriesAxes SMALines = new SimpleMovingAverage(10, security, null).getSMA();
        assertThat(SMALines, is(IsNull.notNullValue()));
        assertThat(SMALines.getValues().length, is(security.getPrices().size() - 10 + 1));
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
        ChartLineSeriesAxes SMALines = new SimpleMovingAverage(10, security, startDate).getSMA();
        assertThat(SMALines, is(IsNull.notNullValue()));
        assertThat(SMALines.getDates()[0], is(isStartDate));
    }

}
