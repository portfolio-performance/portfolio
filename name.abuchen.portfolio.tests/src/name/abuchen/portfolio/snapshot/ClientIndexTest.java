package name.abuchen.portfolio.snapshot;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.hamcrest.number.IsCloseTo;
import org.junit.Test;

import name.abuchen.portfolio.AccountBuilder;
import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.PortfolioBuilder;
import name.abuchen.portfolio.SecurityBuilder;
import name.abuchen.portfolio.TestCurrencyConverter;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.util.Dates;

@SuppressWarnings("nls")
public class ClientIndexTest
{
    private static final double PRECISION = 0.000001d;

    private Client createClient()
    {
        Client client = new Client();

        new AccountBuilder() //
                        .deposit_("2011-12-31", 10000 * Values.Amount.factor()) //
                        .interest("2012-01-01", 230 * Values.Amount.factor()) //
                        .deposit_("2012-01-02", 200 * Values.Amount.factor()) //
                        .interest("2012-01-02", 200 * Values.Amount.factor()) //
                        .withdraw("2012-01-03", 400 * Values.Amount.factor()) //
                        .fees____("2012-01-03", 23441) //
                        .interest("2012-01-04", 29399) //
                        .interest("2012-01-05", 29399) //
                        .deposit_("2012-01-06", 5400 * Values.Amount.factor()) //
                        .interest("2012-01-06", 19599) //
                        .withdraw("2012-01-07", 369704) //
                        .fees____("2012-01-07", 88252) //
                        .fees____("2012-01-08", 100385) //
                        .addTo(client);

        return client;
    }

    @Test
    public void testExcelSample()
    {
        Client client = createClient();

        ReportingPeriod.FromXtoY period = new ReportingPeriod.FromXtoY(LocalDate.of(2011, Month.DECEMBER, 31), //
                        LocalDate.of(2012, Month.JANUARY, 8));
        CurrencyConverter converter = new TestCurrencyConverter();
        PerformanceIndex index = PerformanceIndex.forClient(client, converter, period, new ArrayList<Exception>());

        assertNotNull(index);

        assertThat(period.toInterval(), is(index.getReportInterval().toInterval()));
        assertThat(client, is(index.getClient()));

        LocalDate[] dates = index.getDates();
        assertThat(dates.length, is(Dates.daysBetween(period.getStartDate(), period.getEndDate()) + 1));

        double[] delta = index.getDeltaPercentage();
        assertThat(delta[0], is(0d));
        assertThat(delta[1], IsCloseTo.closeTo(0.023d, PRECISION));
        assertThat(delta[2], IsCloseTo.closeTo(0.019175455d, PRECISION));
        assertThat(delta[3], IsCloseTo.closeTo(-0.0220517d, PRECISION));
        assertThat(delta[4], IsCloseTo.closeTo(0.0294117647d, PRECISION));
        assertThat(delta[5], IsCloseTo.closeTo(0.0285714286d, PRECISION));
        assertThat(delta[6], IsCloseTo.closeTo(0.012261967d, PRECISION));
        assertThat(delta[7], IsCloseTo.closeTo(-0.0545454545d, PRECISION));
        assertThat(delta[8], IsCloseTo.closeTo(-0.0865384615d, PRECISION));

        double[] accumulated = index.getAccumulatedPercentage();
        assertThat(accumulated[0], is(0d));
        assertThat(accumulated[1], IsCloseTo.closeTo(0.023d, PRECISION));
        assertThat(accumulated[2], IsCloseTo.closeTo(0.042616491d, PRECISION));
        assertThat(accumulated[3], IsCloseTo.closeTo(0.019624983d, PRECISION));
        assertThat(accumulated[4], IsCloseTo.closeTo(0.049614163d, PRECISION));
        assertThat(accumulated[5], IsCloseTo.closeTo(0.079603343d, PRECISION));
        assertThat(accumulated[6], IsCloseTo.closeTo(0.092841403d, PRECISION));
        assertThat(accumulated[7], IsCloseTo.closeTo(0.03323197d, PRECISION));
        assertThat(accumulated[8], IsCloseTo.closeTo(-0.056182678d, PRECISION));
    }

    private Client createClient(double[] delta, long[] transferals)
    {
        Client client = new Client();

        AccountBuilder account = new AccountBuilder();

        LocalDateTime time = LocalDateTime.of(2012, Month.JANUARY, 1, 0, 0);

        long valuation = 0;
        double quote = 1;
        for (int ii = 0; ii < delta.length; ii++)
        {
            // the idea: inbound transferals are added at the beginning of the
            // day and hence contribute to the performance. Outbound transferals
            // are deducted at the end of the day.

            long inbound = transferals[ii] > 0 ? transferals[ii] : 0;

            long v = (long) Math.round((double) (valuation + inbound) * (delta[ii] + 1) / quote) - inbound;

            long d = v - valuation;

            if (transferals[ii] > 0)
                account.deposit_(time, transferals[ii]);
            else if (transferals[ii] < 0)
                account.withdraw(time, Math.abs(transferals[ii]));

            if (v > 0)
                account.interest(time, d);
            else if (v < 0)
                account.fees____(time, Math.abs(d));

            valuation = v + transferals[ii];

            quote = 1 + delta[ii];

            time = time.plusDays(1);
        }

        account.addTo(client);
        return client;
    }

    @Test
    public void testThatTransferalsDoNotChangePerformance()
    {
        double[] delta = { 0d, 0.023d, 0.043d, 0.02d, 0.05d, 0.08d, 0.1d, 0.04d, -0.05d };
        long[] transferals = { 1000000, 0, 20000, -40000, 0, 0, 540000, -369704, 0 };
        long[] transferals2 = { 1000000, 0, 0, 0, 0, 0, 0, 0, 0 };

        Client client = createClient(delta, transferals);

        ReportingPeriod.FromXtoY period = new ReportingPeriod.FromXtoY(LocalDate.of(2012, Month.JANUARY, 1), //
                        LocalDate.of(2012, Month.JANUARY, 9));
        CurrencyConverter converter = new TestCurrencyConverter();
        PerformanceIndex index = PerformanceIndex.forClient(client, converter, period, new ArrayList<Exception>());

        double[] accumulated = index.getAccumulatedPercentage();
        for (int ii = 0; ii < accumulated.length; ii++)
            assertThat(accumulated[ii], IsCloseTo.closeTo(delta[ii], PRECISION));

        Client anotherClient = createClient(delta, transferals2);
        index = PerformanceIndex.forClient(anotherClient, converter, period, new ArrayList<Exception>());

        accumulated = index.getAccumulatedPercentage();
        for (int ii = 0; ii < accumulated.length; ii++)
            assertThat(accumulated[ii], IsCloseTo.closeTo(delta[ii], PRECISION));
    }

    @Test
    public void testThatNoValuationResultsInZeroPerformance()
    {
        Client client = new Client();

        ReportingPeriod.FromXtoY period = new ReportingPeriod.FromXtoY(LocalDate.of(2012, Month.JANUARY, 1), //
                        LocalDate.of(2012, Month.JANUARY, 9));
        CurrencyConverter converter = new TestCurrencyConverter();
        PerformanceIndex index = PerformanceIndex.forClient(client, converter, period, new ArrayList<Exception>());

        double[] accumulated = index.getAccumulatedPercentage();
        for (int ii = 0; ii < accumulated.length; ii++)
            assertThat(accumulated[ii], IsCloseTo.closeTo(0d, PRECISION));
    }

    @Test
    public void testThatInterstWithoutInvestmentDoesNotCorruptResultAndIsReported()
    {
        Client client = new Client();
        new AccountBuilder() //
                        .interest(LocalDateTime.of(2012, 01, 02, 0, 0), 100) //
                        .addTo(client);

        ReportingPeriod.FromXtoY period = new ReportingPeriod.FromXtoY(LocalDate.of(2012, Month.JANUARY, 1), //
                        LocalDate.of(2012, Month.JANUARY, 9));

        List<Exception> errors = new ArrayList<Exception>();
        CurrencyConverter converter = new TestCurrencyConverter();
        PerformanceIndex index = PerformanceIndex.forClient(client, converter, period, errors);

        double[] accumulated = index.getAccumulatedPercentage();
        for (int ii = 0; ii < accumulated.length; ii++)
            assertThat(accumulated[ii], IsCloseTo.closeTo(0d, PRECISION));

        assertThat(errors.size(), is(1));
        assertThat(errors.get(0).getMessage(), startsWith(
                        Messages.MsgDeltaWithoutAssets.substring(0, Messages.MsgDeltaWithoutAssets.indexOf('{'))));
    }

    @Test
    public void testFirstDataPointWhenInsideReportingInterval()
    {
        Client client = createClient();

        ReportingPeriod.FromXtoY period = new ReportingPeriod.FromXtoY(LocalDate.of(2011, Month.DECEMBER, 20), //
                        LocalDate.of(2012, Month.JANUARY, 8));
        CurrencyConverter converter = new TestCurrencyConverter();
        PerformanceIndex index = PerformanceIndex.forClient(client, converter, period, new ArrayList<Exception>());

        assertThat(index.getFirstDataPoint().get(), is(LocalDate.of(2011, 12, 31)));
        assertThat(index.getFirstDataPoint().get(), not(period.toInterval().getStart()));
    }

    @Test
    public void testThatPerformanceOfAnInvestmentIntoAnIndexIsIdenticalToIndex()
    {
        LocalDate startDate = LocalDate.of(2012, 1, 1);
        LocalDate endDate = LocalDate.of(2012, 4, 29); // a weekend
        long startPrice = Values.Quote.factorize(100);

        Client client = new Client();

        Security security = new SecurityBuilder() //
                        .generatePrices(startPrice, startDate, endDate) //
                        .addTo(client);

        PortfolioBuilder portfolio = new PortfolioBuilder(new Account());

        // add some buy transactions
        LocalDate date = startDate;
        while (date.isBefore(endDate))
        {
            // performance is only identical if the capital invested
            // additionally has the identical performance on its first day -->
            // use the closing quote of the previous day

            long p = security.getSecurityPrice(date.minusDays(1)).getValue();
            portfolio.inbound_delivery(security, date.atStartOfDay(), Values.Share.factorize(100), p);
            date = date.plusDays(20);
        }

        portfolio.addTo(client);

        ReportingPeriod.FromXtoY period = new ReportingPeriod.FromXtoY(startDate, endDate);

        List<Exception> warnings = new ArrayList<Exception>();
        CurrencyConverter converter = new TestCurrencyConverter();
        PerformanceIndex index = PerformanceIndex.forClient(client, converter, period, warnings);
        assertTrue(warnings.isEmpty());

        double[] accumulated = index.getAccumulatedPercentage();
        long lastPrice = security.getSecurityPrice(endDate).getValue();

        assertThat((double) (lastPrice - startPrice) / (double) startPrice,
                        IsCloseTo.closeTo(accumulated[accumulated.length - 1], PRECISION));

        PerformanceIndex benchmark = PerformanceIndex.forSecurity(index, security);
        assertThat(benchmark.getFinalAccumulatedPercentage(),
                        IsCloseTo.closeTo(index.getFinalAccumulatedPercentage(), PRECISION));
    }

    @Test
    public void testThatPerformanceOfInvestmentAndIndexIsIdendicalWhenInForeignCurrency()
    {
        LocalDate startDate = LocalDate.of(2015, 1, 1);
        LocalDate endDate = LocalDate.of(2015, 8, 1);
        long startPrice = Values.Quote.factorize(100);

        Client client = new Client();

        Security security = new SecurityBuilder("USD") //
                        .generatePrices(startPrice, startDate, endDate) //
                        .addTo(client);

        Account account = new AccountBuilder().addTo(client);

        new PortfolioBuilder(account) //
                        .inbound_delivery(security, LocalDateTime.of(2014, 01, 01, 0, 0), Values.Share.factorize(100), 100) //
                        .addTo(client);

        ReportingPeriod.FromXtoY period = new ReportingPeriod.FromXtoY(startDate, endDate);

        List<Exception> warnings = new ArrayList<Exception>();
        CurrencyConverter converter = new TestCurrencyConverter().with(CurrencyUnit.EUR);

        PerformanceIndex index = PerformanceIndex.forClient(client, converter, period, warnings);
        assertTrue(warnings.isEmpty());

        PerformanceIndex benchmark = PerformanceIndex.forSecurity(index, security);
        assertTrue(warnings.isEmpty());
        assertThat(benchmark.getFinalAccumulatedPercentage(),
                        IsCloseTo.closeTo(index.getFinalAccumulatedPercentage(), PRECISION));

        PerformanceIndex investment = PerformanceIndex.forInvestment(client, converter, security, period, warnings);
        assertTrue(warnings.isEmpty());
        assertThat(investment.getFinalAccumulatedPercentage(), is(index.getFinalAccumulatedPercentage()));
    }

    @Test
    public void testExcelSampleAggregatedWeekly()
    {
        // first day of week is locale dependent
        Locale locale = Locale.getDefault();
        Locale.setDefault(Locale.GERMAN);

        try
        {
            Client client = createClient();

            ReportingPeriod.FromXtoY reportInterval = new ReportingPeriod.FromXtoY( //
                            LocalDate.of(2011, Month.DECEMBER, 31), LocalDate.of(2012, Month.JANUARY, 8));
            CurrencyConverter converter = new TestCurrencyConverter();
            PerformanceIndex index = PerformanceIndex.forClient(client, converter, reportInterval,
                            new ArrayList<Exception>());

            index = Aggregation.aggregate(index, Aggregation.Period.WEEKLY);

            assertNotNull(index);

            double[] delta = index.getDeltaPercentage();
            assertThat(delta.length, is(2));
            assertThat(delta[0], IsCloseTo.closeTo(0.023d, PRECISION));
            assertThat(delta[1], IsCloseTo.closeTo(-0.077402422d, PRECISION));

            double[] accumulated = index.getAccumulatedPercentage();
            assertThat(accumulated[0], IsCloseTo.closeTo(0.023d, PRECISION));
            assertThat(accumulated[1], IsCloseTo.closeTo(-0.056182678d, PRECISION));
        }
        finally
        {
            Locale.setDefault(locale);
        }
    }

    @Test
    public void testThatDepositsOnTheLastDayArePerformanceNeutral()
    {
        Client client = new Client();

        new AccountBuilder() //
                        .deposit_("2012-01-01", 10000) //
                        .interest("2012-01-02", 1000) //
                        .deposit_("2012-01-10", 10000) //
                        .addTo(client);

        ReportingPeriod.FromXtoY reportInterval = new ReportingPeriod.FromXtoY(LocalDate.of(2012, Month.JANUARY, 1), //
                        LocalDate.of(2012, Month.JANUARY, 10));
        CurrencyConverter converter = new TestCurrencyConverter();
        PerformanceIndex index = PerformanceIndex.forClient(client, converter, reportInterval,
                        new ArrayList<Exception>());

        double[] accumulated = index.getAccumulatedPercentage();
        assertThat(accumulated[accumulated.length - 2], IsCloseTo.closeTo(0.1d, PRECISION));
        assertThat(accumulated[accumulated.length - 1], IsCloseTo.closeTo(0.1d, PRECISION));
    }

    @Test
    public void testChangesOnFirstDayOfInvestment()
    {
        Client client = new Client();

        new AccountBuilder() //
                        .deposit_("2012-01-01", 10000) //
                        .interest("2012-01-02", 1000) //
                        .addTo(client);

        ReportingPeriod.FromXtoY reportInterval = new ReportingPeriod.FromXtoY(LocalDate.of(2012, Month.JANUARY, 1), //
                        LocalDate.of(2012, Month.JANUARY, 10));
        CurrencyConverter converter = new TestCurrencyConverter();
        PerformanceIndex index = PerformanceIndex.forClient(client, converter, reportInterval,
                        new ArrayList<Exception>());

        double[] accumulated = index.getAccumulatedPercentage();
        assertThat(accumulated[accumulated.length - 1], IsCloseTo.closeTo(0.1d, PRECISION));
    }

}
