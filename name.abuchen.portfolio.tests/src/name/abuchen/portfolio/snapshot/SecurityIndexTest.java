package name.abuchen.portfolio.snapshot;

import static name.abuchen.portfolio.snapshot.ModelUtilities.addT;
import static name.abuchen.portfolio.snapshot.ModelUtilities.generatePrices;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction.Type;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Values;

import org.hamcrest.number.IsCloseTo;
import org.joda.time.DateMidnight;
import org.joda.time.Days;
import org.junit.Test;

public class SecurityIndexTest
{

    @Test
    public void testThatSecurityIndexIsCalculated()
    {
        DateMidnight startDate = new DateMidnight(2012, 12, 31);
        DateMidnight endDate = new DateMidnight(2013, 4, 1);
        long startPrice = 100 * Values.Amount.factor();

        // create model

        Client client = new Client();

        Account account = new Account();
        client.addAccount(account);
        addT(account, startDate.toCalendar(Locale.getDefault()), Type.DEPOSIT, startPrice);

        Security security = new Security();
        client.addSecurity(security);

        generatePrices(security, startPrice, startDate, endDate);

        // calculate performance indices

        List<Exception> warnings = new ArrayList<Exception>();

        ReportingPeriod reportInterval = new ReportingPeriod.FromXtoY(startDate.toDate(), endDate.toDate());
        ClientIndex clientIndex = ClientIndex.forPeriod(client, reportInterval, warnings);
        SecurityIndex securityIndex = SecurityIndex.forClient(clientIndex, security, warnings);

        // asserts

        assertTrue(warnings.isEmpty());

        Date[] dates = securityIndex.getDates();
        assertThat(dates[0], is(startDate.toDate()));
        assertThat(dates[dates.length - 1], is(endDate.toDate()));

        long lastPrice = security.getSecurityPrice(endDate.toDate()).getValue();
        double performance = (double) (lastPrice - startPrice) / (double) startPrice;

        double[] accumulated = securityIndex.getAccumulatedPercentage();
        assertThat(accumulated[0], is(0d));
        assertThat(accumulated[accumulated.length - 1], IsCloseTo.closeTo(performance, 0.000001d));
    }

    @Test
    public void testWhenQuotesAreOnlyAvailableFromTheMiddleOfTheReportInterval()
    {
        DateMidnight startDate = new DateMidnight(2012, 12, 31);
        DateMidnight middleDate = new DateMidnight(2013, 2, 18);
        DateMidnight endDate = new DateMidnight(2013, 4, 1);

        // create model

        Client client = new Client();

        Account account = new Account();
        client.addAccount(account);
        addT(account, startDate.toCalendar(Locale.getDefault()), Type.DEPOSIT, 100 * Values.Amount.factor());
        addT(account, startDate.plusDays(10).toCalendar(Locale.getDefault()), Type.INTEREST,
                        10 * Values.Amount.factor());

        Security security = new Security();
        client.addSecurity(security);

        generatePrices(security, 50 * Values.Amount.factor(), middleDate, endDate);

        // calculate performance indices

        List<Exception> warnings = new ArrayList<Exception>();

        ReportingPeriod reportInterval = new ReportingPeriod.FromXtoY(startDate.toDate(), endDate.toDate());
        ClientIndex clientIndex = ClientIndex.forPeriod(client, reportInterval, warnings);
        SecurityIndex securityIndex = SecurityIndex.forClient(clientIndex, security, warnings);

        // asserts

        assertTrue(warnings.isEmpty());

        Date[] clientDates = clientIndex.getDates();
        Date[] securityDates = securityIndex.getDates();

        assertThat(securityDates[0], is(middleDate.toDate()));
        assertThat(securityDates[securityDates.length - 1], is(endDate.toDate()));
        assertThat(new DateMidnight(clientDates[clientDates.length - 1]), is(new DateMidnight(
                        securityDates[securityDates.length - 1])));

        double[] clientAccumulated = clientIndex.getAccumulatedPercentage();
        double[] securityAccumulated = securityIndex.getAccumulatedPercentage();

        int index = Days.daysBetween(startDate, middleDate).getDays();
        assertThat(new DateMidnight(clientDates[index]), is(middleDate));
        assertThat(securityAccumulated[0], IsCloseTo.closeTo(clientAccumulated[index], 0.000001d));

        long middlePrice = security.getSecurityPrice(middleDate.toDate()).getValue();
        long lastPrice = security.getSecurityPrice(endDate.toDate()).getValue();

        // 10% is interest of the deposit
        double performance = (double) (lastPrice - middlePrice) / (double) middlePrice + 0.1d;
        assertThat(securityAccumulated[securityAccumulated.length - 1], IsCloseTo.closeTo(performance, 0.000001d));
    }

    @Test
    public void testWhenQuotesAreOnlyAvailableUntilTheMiddleOfTheReportInterval()
    {
        DateMidnight startDate = new DateMidnight(2012, 12, 31);
        DateMidnight middleDate = new DateMidnight(2013, 2, 18);
        DateMidnight endDate = new DateMidnight(2013, 3, 31);

        // create model

        Client client = new Client();

        Account account = new Account();
        client.addAccount(account);
        addT(account, startDate.toCalendar(Locale.getDefault()), Type.DEPOSIT, 100 * Values.Amount.factor());
        addT(account, startDate.plusDays(10).toCalendar(Locale.getDefault()), Type.INTEREST,
                        10 * Values.Amount.factor());

        Security security = new Security();
        client.addSecurity(security);

        int startPrice = 50 * Values.Amount.factor();
        generatePrices(security, startPrice, startDate, middleDate);

        // calculate performance indices

        List<Exception> warnings = new ArrayList<Exception>();

        ReportingPeriod reportInterval = new ReportingPeriod.FromXtoY(startDate.toDate(), endDate.toDate());
        ClientIndex clientIndex = ClientIndex.forPeriod(client, reportInterval, warnings);
        SecurityIndex securityIndex = SecurityIndex.forClient(clientIndex, security, warnings);

        // asserts

        assertTrue(warnings.isEmpty());

        Date[] clientDates = clientIndex.getDates();
        Date[] securityDates = securityIndex.getDates();

        assertThat(securityDates[0], is(startDate.toDate()));
        assertThat(securityDates[securityDates.length - 1], is(middleDate.toDate()));
        assertThat(new DateMidnight(clientDates[0]), is(new DateMidnight(securityDates[0])));

        double[] securityAccumulated = securityIndex.getAccumulatedPercentage();

        int index = Days.daysBetween(startDate, middleDate).getDays();
        assertThat(new DateMidnight(clientDates[index]), is(middleDate));
        assertThat(securityAccumulated[0], IsCloseTo.closeTo(0d, 0.000001d));

        long middlePrice = security.getSecurityPrice(middleDate.toDate()).getValue();
        double performance = (double) (middlePrice - startPrice) / (double) startPrice;
        assertThat(securityAccumulated[securityAccumulated.length - 1], IsCloseTo.closeTo(performance, 0.000001d));
    }

    @Test
    public void testIndexWhenNoQuotesExist()
    {
        DateMidnight startDate = new DateMidnight(2012, 12, 31);
        DateMidnight endDate = new DateMidnight(2013, 3, 31);

        // create model

        Client client = new Client();

        Account account = new Account();
        client.addAccount(account);
        addT(account, startDate.toCalendar(Locale.getDefault()), Type.DEPOSIT, 100 * Values.Amount.factor());

        Security security = new Security();
        client.addSecurity(security);

        // calculate performance indices

        List<Exception> warnings = new ArrayList<Exception>();

        ReportingPeriod reportInterval = new ReportingPeriod.FromXtoY(startDate.toDate(), endDate.toDate());
        ClientIndex clientIndex = ClientIndex.forPeriod(client, reportInterval, warnings);
        SecurityIndex securityIndex = SecurityIndex.forClient(clientIndex, security, warnings);

        // asserts

        assertTrue(warnings.isEmpty());
        assertThat(securityIndex.getDates().length, is(1));
        assertThat(securityIndex.getDates()[0], is(clientIndex.getFirstDataPoint().toDate()));
    }
}
