package name.abuchen.portfolio.ui.views;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;

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
public class MovingAverageConvergenceDivergenceTest
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
            price.setValue(i * Values.Quote.factor());
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
        MovingAverageConvergenceDivergence macd = new MovingAverageConvergenceDivergence(this.securityOnePrice,
                        interval);
        ChartLineSeriesAxes macdLine = macd.getMacdLine();
        ChartLineSeriesAxes signalLine = macd.getSignalLine();
        assertThat(macdLine.getDates().length, is(1));
        assertThat(macdLine.getDates()[0], is(java.sql.Date.valueOf(LocalDate.parse("2017-01-01"))));
        assertThat(signalLine.getDates().length, is(1));
        assertThat(signalLine.getDates()[0], is(java.sql.Date.valueOf(LocalDate.parse("2017-01-01"))));
    }

    @Test
    public void testSecurityIsNull()
    {
        ChartInterval interval = new ChartInterval(securityOnePrice.getPrices().get(0).getDate(), LocalDate.now());
        MovingAverageConvergenceDivergence macd = new MovingAverageConvergenceDivergence(null, interval);
        ChartLineSeriesAxes macdLine = macd.getMacdLine();
        ChartLineSeriesAxes signalLine = macd.getSignalLine();
        assertThat(macdLine.getDates(), is(IsNull.nullValue()));
        assertThat(signalLine.getDates(), is(IsNull.nullValue()));
    }

    @Test
    public void testMacdEntries()
    {
        double error = 0.006d;
        ChartInterval interval = new ChartInterval(securityTenPrices.getPrices().get(0).getDate(), LocalDate.now());
        MovingAverageConvergenceDivergence macd = new MovingAverageConvergenceDivergence(this.securityTenPrices,
                        interval);
        ChartLineSeriesAxes macdLine = macd.getMacdLine();
        ChartLineSeriesAxes signalLine = macd.getSignalLine();
        assertThat(macdLine, is(IsNull.notNullValue()));
        assertThat(macdLine.getValues().length, is(10));
        assertThat(macdLine.getValues()[0], is(0.0));
        assertThat(macdLine.getValues()[1], closeTo(0.07977d, error));
        assertThat(macdLine.getValues()[2], closeTo(0.22113d, error));
        assertThat(macdLine.getValues()[3], closeTo(0.40914d, error));
        assertThat(macdLine.getValues()[4], closeTo(0.63154d, error));
        assertThat(macdLine.getValues()[5], closeTo(0.87837d, error));
        assertThat(macdLine.getValues()[6], closeTo(1.14151d, error));
        assertThat(macdLine.getValues()[7], closeTo(1.41444d, error));
        assertThat(macdLine.getValues()[8], closeTo(1.69193d, error));
        assertThat(macdLine.getValues()[9], closeTo(1.96983d, error));

        assertThat(signalLine, is(IsNull.notNullValue()));
        assertThat(signalLine.getValues().length, is(10));
        assertThat(signalLine.getValues()[0], is(0.0d));
        assertThat(signalLine.getValues()[1], closeTo(0.01595d, error));
        assertThat(signalLine.getValues()[2], closeTo(0.05699d, error));
        assertThat(signalLine.getValues()[3], closeTo(0.12742d, error));
        assertThat(signalLine.getValues()[4], closeTo(0.22824d, error));
        assertThat(signalLine.getValues()[5], closeTo(0.35827d, error));
        assertThat(signalLine.getValues()[6], closeTo(0.51492d, error));
        assertThat(signalLine.getValues()[7], closeTo(0.69482d, error));
        assertThat(signalLine.getValues()[8], closeTo(0.89424d, error));
        assertThat(signalLine.getValues()[9], closeTo(1.10936d, error));
    }

    @Test
    public void testMacdEntries_PricesWithDateGaps()
    {
        double error = 0.006d;
        ChartInterval interval = new ChartInterval(this.securitySevenPricesWithGaps.getPrices().get(4).getDate(),
                        LocalDate.now());
        MovingAverageConvergenceDivergence macd = new MovingAverageConvergenceDivergence(
                        this.securitySevenPricesWithGaps, interval);
        ChartLineSeriesAxes macdLine = macd.getMacdLine();
        ChartLineSeriesAxes signalLine = macd.getSignalLine();
        assertThat(macdLine, is(IsNull.notNullValue()));
        assertThat(macdLine.getValues(), is(IsNull.notNullValue()));
        assertThat(macdLine.getValues().length, is(3));
        assertThat(macdLine.getValues()[0], closeTo(-0.355755d, error));
        assertThat(macdLine.getValues()[1], closeTo(0.0651133d, error));
        assertThat(macdLine.getValues()[2], closeTo(0.4738841d, error));
        assertThat(signalLine, is(IsNull.notNullValue()));
        assertThat(signalLine.getValues(), is(IsNull.notNullValue()));
        assertThat(signalLine.getValues().length, is(3));
        assertThat(signalLine.getValues()[0], closeTo(-0.291856d, error));
        assertThat(signalLine.getValues()[1], closeTo(-0.220462d, error));
        assertThat(signalLine.getValues()[2], closeTo(-0.081593d, error));
    }

    @Test
    public void testSufficientPriceDataPass()
    {
        Security security = new Security();

        LocalDate date = LocalDate.parse("2016-01-01");
        for (int i = 0; i < 300; i++)
        {
            security.addPrice(new SecurityPrice(date, Values.Quote.factorize(10)));
            date = date.plusDays(1);
        }

        ChartInterval interval = new ChartInterval(security.getPrices().get(0).getDate(), LocalDate.now());

        MovingAverageConvergenceDivergence macd = new MovingAverageConvergenceDivergence(security, interval);
        ChartLineSeriesAxes macdLine = macd.getMacdLine();
        ChartLineSeriesAxes signalLine = macd.getSignalLine();
        assertThat(macdLine, is(IsNull.notNullValue()));
        assertThat(macdLine.getValues().length, is(security.getPrices().size()));
        assertThat(signalLine, is(IsNull.notNullValue()));
        assertThat(signalLine.getValues().length, is(security.getPrices().size()));
    }

    @Test
    public void testSufficientPriceDataStartDate()
    {
        Security security = new Security();

        LocalDate date = LocalDate.parse("2016-01-01");
        for (int i = 0; i < 300; i++)
        {
            security.addPrice(new SecurityPrice(date, Values.Quote.factorize(10)));
            date = date.plusDays(1);
        }
        LocalDate startDate = LocalDate.parse("2016-06-01");
        Date isStartDate = java.sql.Date.valueOf(startDate);
        MovingAverageConvergenceDivergence macd = new MovingAverageConvergenceDivergence(security,
                        new ChartInterval(startDate, LocalDate.now()));
        ChartLineSeriesAxes macdLine = macd.getMacdLine();
        ChartLineSeriesAxes signalLine = macd.getSignalLine();
        assertThat(macdLine, is(IsNull.notNullValue()));
        assertThat(macdLine.getDates()[0], is(isStartDate));
        assertThat(signalLine, is(IsNull.notNullValue()));
        assertThat(signalLine.getDates()[0], is(isStartDate));
    }

}
