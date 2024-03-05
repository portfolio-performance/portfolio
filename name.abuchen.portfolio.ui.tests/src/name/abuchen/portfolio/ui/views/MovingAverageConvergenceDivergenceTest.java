package name.abuchen.portfolio.ui.views;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.Test;

import com.google.common.base.Charsets;

import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.views.SecuritiesChart.ChartInterval;

@SuppressWarnings("nls")
public class MovingAverageConvergenceDivergenceTest
{
    private static final String DATE_HEADER = "Date";
    private static final String CLOSE_HEADER = "Close";
    private static final String MACD_HEADER = "MACD(12/26)";
    private static final String MACD_SIGNAL_HEADER = "MACD Signal(12/26/9)";

    @Test
    public void testSecurityPricesIsEmpty()
    {
        ChartInterval interval = new ChartInterval(LocalDate.of(2017, 1, 1), LocalDate.now());
        MovingAverageConvergenceDivergence macd = new MovingAverageConvergenceDivergence(new Security(), interval);
        var macdLine = macd.getMacdLine();
        var signalLine = macd.getSignalLine();
        assertThat(macdLine.isPresent(), is(false));
        assertThat(signalLine.isPresent(), is(false));
    }

    @Test
    public void testSecurityHasOnlyOnePrice()
    {
        Security security = new Security();
        security.addPrice(new SecurityPrice(LocalDate.of(2017, 1, 1), 0));
        ChartInterval interval = new ChartInterval(LocalDate.of(2017, 1, 1), LocalDate.now());
        MovingAverageConvergenceDivergence macd = new MovingAverageConvergenceDivergence(security, interval);
        var macdLine = macd.getMacdLine();
        var signalLine = macd.getSignalLine();
        assertThat(macdLine.isPresent(), is(false));
        assertThat(signalLine.isPresent(), is(false));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testIntervalInsideSecurityPrices()
    {
        Security security = new Security();
        LocalDate date = LocalDate.of(2016, 1, 1);
        for (int i = 0; i < 300; i++)
        {
            security.addPrice(new SecurityPrice(date, Values.Quote.factorize(10)));
            date = date.plusDays(1);
        }

        ChartInterval interval = new ChartInterval(LocalDate.of(2016, 3, 1), LocalDate.of(2016, 5, 31));
        MovingAverageConvergenceDivergence macd = new MovingAverageConvergenceDivergence(security, interval);
        ChartLineSeriesAxes macdLine = macd.getMacdLine().get();
        ChartLineSeriesAxes signalLine = macd.getSignalLine().get();
        assertThat(boxDoublesArray(macdLine.getValues()),
                        Matchers.arrayContaining(Collections.nCopies(92, is(0.0)).toArray(new Matcher[0])));
        assertThat(boxDoublesArray(signalLine.getValues()),
                        Matchers.arrayContaining(Collections.nCopies(92, is(0.0)).toArray(new Matcher[0])));
    }

    @Test
    public void testIntervalBeforeSecurityPrices()
    {
        Security security = new Security();
        LocalDate date = LocalDate.of(2016, 1, 1);
        for (int i = 0; i < 300; i++)
        {
            security.addPrice(new SecurityPrice(date, Values.Quote.factorize(10)));
            date = date.plusDays(1);
        }

        ChartInterval interval = new ChartInterval(LocalDate.of(2015, 3, 1), LocalDate.of(2015, 5, 31));
        MovingAverageConvergenceDivergence macd = new MovingAverageConvergenceDivergence(security, interval);
        var macdLine = macd.getMacdLine();
        assertThat(macdLine.isPresent(), is(false));
        var signalLine = macd.getSignalLine();
        assertThat(signalLine.isPresent(), is(false));
    }

    @Test
    public void testIntervalAfterSecurityPrices()
    {
        Security security = new Security();
        LocalDate date = LocalDate.of(2016, 1, 1);
        for (int i = 0; i < 300; i++)
        {
            security.addPrice(new SecurityPrice(date, Values.Quote.factorize(10)));
            date = date.plusDays(1);
        }

        ChartInterval interval = new ChartInterval(LocalDate.of(2017, 3, 1), LocalDate.of(2017, 5, 31));
        MovingAverageConvergenceDivergence macd = new MovingAverageConvergenceDivergence(security, interval);
        var macdLine = macd.getMacdLine();
        assertThat(macdLine.isPresent(), is(false));
        var signalLine = macd.getSignalLine();
        assertThat(signalLine.isPresent(), is(false));
    }

    @Test
    public void testSecurityPricesEqualsToExpected() throws Exception
    {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        NumberFormat numberFormat = NumberFormat.getInstance(Locale.US);
        List<CSVRecord> records = CSVParser
                        .parse(this.getClass().getResourceAsStream("Indicator.GOOG.csv"), Charsets.UTF_8,
                                        CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build())
                        .getRecords();

        Security prices = CsvTestDataLoader.loadSecurity(records, dateFormatter, DATE_HEADER, numberFormat,
                        CLOSE_HEADER);
        ChartLineSeriesAxes expectedMacd = CsvTestDataLoader.loadChartLineSeriesAxes(records, dateFormatter,
                        DATE_HEADER, numberFormat, MACD_HEADER);
        ChartLineSeriesAxes expectedMacdSignal = CsvTestDataLoader.loadChartLineSeriesAxes(records, dateFormatter,
                        DATE_HEADER, numberFormat, MACD_SIGNAL_HEADER);

        ChartInterval interval = new ChartInterval(prices.getPrices().get(0).getDate(), LocalDate.now());
        MovingAverageConvergenceDivergence macd = new MovingAverageConvergenceDivergence(prices, interval);
        ChartLineSeriesAxes macdLine = macd.getMacdLine().get();
        ChartLineSeriesAxes signalLine = macd.getSignalLine().get();

        assertThat(macdLine.getDates(), Matchers.arrayContaining(expectedMacd.getDates()));
        assertThat(boxDoublesArray(macdLine.getValues()),
                        Matchers.arrayContaining(createValueMatchers(expectedMacd.getValues())));

        assertThat(signalLine.getDates(), Matchers.arrayContaining(expectedMacdSignal.getDates()));
        assertThat(boxDoublesArray(signalLine.getValues()),
                        Matchers.arrayContaining(createValueMatchers(expectedMacdSignal.getValues())));
    }

    private Double[] boxDoublesArray(double[] values)
    {
        return Arrays.stream(values).boxed().toArray(Double[]::new);
    }

    @SuppressWarnings("unchecked")
    private Matcher<Double>[] createValueMatchers(double[] values)
    {
        return Arrays.stream(values).boxed().map(x -> closeTo(x, 0.000000005)).toArray(Matcher[]::new);
    }

}
