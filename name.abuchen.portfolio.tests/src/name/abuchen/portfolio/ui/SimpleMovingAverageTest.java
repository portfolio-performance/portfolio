package name.abuchen.portfolio.ui;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.hamcrest.core.IsNull;
import org.junit.Before;
import org.junit.Test;

import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.views.ChartLineSeriesAxes;
import name.abuchen.portfolio.ui.views.SimpleMovingAverage;

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
        price.setTime(date);
        price.setValue(0);
        securityOnePrice.addPrice(price);
        
        int i = 1;
        while (i <= 10)
        {
            formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
            formatter = formatter.withLocale(Locale.GERMANY);
            if (i < 10) {
                date = LocalDate.parse("0"+i + ".01.2017", formatter);
            } else {
                date = LocalDate.parse(i + ".01.2017", formatter);
            }
            price = new SecurityPrice();
            price.setTime(date);
            price.setValue(i);
            securityTenPrices.addPrice( price );
            i++;
        }

    }

    @Test
    public void testSecurityHasOnlyOnePrice()
    {
        ChartLineSeriesAxes SMALines = SimpleMovingAverage.getSMA(200, this.securityOnePrice, null);
        assertThat(SMALines, is(IsNull.nullValue()));
    }

    @Test
    public void testSecurityIsNull()
    {
        ChartLineSeriesAxes SMALines = SimpleMovingAverage.getSMA(200, null, null);
        assertThat(SMALines, is(IsNull.nullValue()));
    }

    @Test
    public void testCorrectSMAEntries()
    {
        ChartLineSeriesAxes SMALines = SimpleMovingAverage.getSMA(10, this.securityTenPrices, null);
        assertThat(SMALines, is(IsNull.notNullValue()));
        assertThat(SMALines.getValues().length, is(1));
        assertThat(SMALines.getValues()[0], is((1 + 2 + 3 + 4 + 5 + 6 + 7 + 8 + 9 + 10) / Values.Quote.divider() / 10));
    }

}
