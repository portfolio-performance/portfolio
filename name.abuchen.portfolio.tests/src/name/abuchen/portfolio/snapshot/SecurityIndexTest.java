package name.abuchen.portfolio.snapshot;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.hamcrest.number.IsCloseTo;
import org.junit.Test;

import name.abuchen.portfolio.AccountBuilder;
import name.abuchen.portfolio.SecurityBuilder;
import name.abuchen.portfolio.TestCurrencyConverter;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.util.Dates;

public class SecurityIndexTest
{

    @Test
    public void testThatSecurityIndexIsCalculated()
    {
        LocalDate startDate = LocalDate.of(2012, 12, 31);
        LocalDate endDate = LocalDate.of(2013, 4, 1);
        long startPrice = 100 * Values.Amount.factor();

        // create model

        Client client = new Client();

        new AccountBuilder() //
                        .deposit_(startDate.atStartOfDay(), startPrice) //
                        .addTo(client);

        Security security = new SecurityBuilder() //
                        .generatePrices(startPrice, startDate, endDate) //
                        .addTo(client);

        // calculate performance indices

        List<Exception> warnings = new ArrayList<Exception>();

        ReportingPeriod reportInterval = new ReportingPeriod.FromXtoY(startDate, endDate);
        CurrencyConverter converter = new TestCurrencyConverter();
        PerformanceIndex clientIndex = PerformanceIndex.forClient(client, converter, reportInterval, warnings);
        PerformanceIndex securityIndex = PerformanceIndex.forSecurity(clientIndex, security);

        // asserts

        assertTrue(warnings.isEmpty());

        LocalDate[] dates = securityIndex.getDates();
        assertThat(dates[0], is(startDate));
        assertThat(dates[dates.length - 1], is(endDate));

        long lastPrice = security.getSecurityPrice(endDate).getValue();
        double performance = (double) (lastPrice - startPrice) / (double) startPrice;

        double[] accumulated = securityIndex.getAccumulatedPercentage();
        assertThat(accumulated[0], is(0d));
        assertThat(accumulated[accumulated.length - 1], IsCloseTo.closeTo(performance, 0.000001d));
    }

    @Test
    public void testWhenQuotesAreOnlyAvailableFromTheMiddleOfTheReportInterval()
    {
        LocalDate startDate = LocalDate.of(2012, 12, 31);
        LocalDate middleDate = LocalDate.of(2013, 2, 18);
        LocalDate endDate = LocalDate.of(2013, 4, 1);

        // create model

        Client client = new Client();

        new AccountBuilder() //
                        .deposit_(startDate.atStartOfDay(), 100 * Values.Amount.factor()) //
                        .interest(startDate.atStartOfDay().plusDays(10), 10 * Values.Amount.factor()) //
                        .addTo(client);

        Security security = new SecurityBuilder() //
                        .generatePrices(50 * Values.Amount.factor(), middleDate, endDate) //
                        .addTo(client);

        // calculate performance indices

        List<Exception> warnings = new ArrayList<Exception>();

        ReportingPeriod reportInterval = new ReportingPeriod.FromXtoY(startDate, endDate);
        CurrencyConverter converter = new TestCurrencyConverter();
        PerformanceIndex clientIndex = PerformanceIndex.forClient(client, converter, reportInterval, warnings);
        PerformanceIndex securityIndex = PerformanceIndex.forSecurity(clientIndex, security);

        // asserts

        assertTrue(warnings.isEmpty());

        LocalDate[] clientDates = clientIndex.getDates();
        LocalDate[] securityDates = securityIndex.getDates();

        assertThat(securityDates[0], is(middleDate));
        assertThat(securityDates[securityDates.length - 1], is(endDate));
        assertThat(clientDates[clientDates.length - 1], is(securityDates[securityDates.length - 1]));

        double[] clientAccumulated = clientIndex.getAccumulatedPercentage();
        double[] securityAccumulated = securityIndex.getAccumulatedPercentage();

        int index = Dates.daysBetween(startDate, middleDate);
        assertThat(clientDates[index], is(middleDate));
        assertThat(securityAccumulated[0], IsCloseTo.closeTo(clientAccumulated[index], 0.000001d));

        long middlePrice = security.getSecurityPrice(middleDate).getValue();
        long lastPrice = security.getSecurityPrice(endDate).getValue();

        // 10% is interest of the deposit
        double performance = (double) (lastPrice - middlePrice) / (double) middlePrice + 0.1d;
        assertThat(securityAccumulated[securityAccumulated.length - 1], IsCloseTo.closeTo(performance, 0.000001d));
    }

    @Test
    public void testWhenQuotesAreOnlyAvailableUntilTheMiddleOfTheReportInterval()
    {
        LocalDate startDate = LocalDate.of(2012, 12, 31);
        LocalDate middleDate = LocalDate.of(2013, 2, 18);
        LocalDate endDate = LocalDate.of(2013, 3, 31);

        // create model

        Client client = new Client();

        new AccountBuilder() //
                        .deposit_(startDate.atStartOfDay(), 100 * Values.Amount.factor()) //
                        .interest(startDate.atStartOfDay().plusDays(10), 10 * Values.Amount.factor()) //
                        .addTo(client);

        int startPrice = 50 * Values.Amount.factor();

        Security security = new SecurityBuilder() //
                        .generatePrices(startPrice, startDate, middleDate) //
                        .addTo(client);

        // calculate performance indices

        List<Exception> warnings = new ArrayList<Exception>();

        ReportingPeriod reportInterval = new ReportingPeriod.FromXtoY(startDate, endDate);
        CurrencyConverter converter = new TestCurrencyConverter();
        PerformanceIndex clientIndex = PerformanceIndex.forClient(client, converter, reportInterval, warnings);
        PerformanceIndex securityIndex = PerformanceIndex.forSecurity(clientIndex, security);

        // asserts

        assertTrue(warnings.isEmpty());

        LocalDate[] clientDates = clientIndex.getDates();
        LocalDate[] securityDates = securityIndex.getDates();

        assertThat(securityDates[0], is(startDate));
        assertThat(securityDates[securityDates.length - 1], is(middleDate));
        assertThat(clientDates[0], is(securityDates[0]));

        double[] securityAccumulated = securityIndex.getAccumulatedPercentage();

        int index = Dates.daysBetween(startDate, middleDate);
        assertThat(clientDates[index], is(middleDate));
        assertThat(securityAccumulated[0], IsCloseTo.closeTo(0d, 0.000001d));

        long middlePrice = security.getSecurityPrice(middleDate).getValue();
        double performance = (double) (middlePrice - startPrice) / (double) startPrice;
        assertThat(securityAccumulated[securityAccumulated.length - 1], IsCloseTo.closeTo(performance, 0.000001d));
    }

    @Test
    public void testIndexWhenNoQuotesExist()
    {
        LocalDate startDate = LocalDate.of(2012, 12, 31);
        LocalDate endDate = LocalDate.of(2013, 3, 31);

        // create model

        Client client = new Client();

        new AccountBuilder() //
                        .deposit_(startDate.atStartOfDay(), 100 * Values.Amount.factor()) //
                        .addTo(client);

        Security security = new Security();
        client.addSecurity(security);

        // calculate performance indices

        List<Exception> warnings = new ArrayList<Exception>();

        ReportingPeriod reportInterval = new ReportingPeriod.FromXtoY(startDate, endDate);
        CurrencyConverter converter = new TestCurrencyConverter();
        PerformanceIndex clientIndex = PerformanceIndex.forClient(client, converter, reportInterval, warnings);
        PerformanceIndex securityIndex = PerformanceIndex.forSecurity(clientIndex, security);

        // asserts

        assertTrue(warnings.isEmpty());
        assertThat(securityIndex.getDates().length, is(1));
        assertThat(securityIndex.getDates()[0], is(clientIndex.getFirstDataPoint().get()));
    }
}
