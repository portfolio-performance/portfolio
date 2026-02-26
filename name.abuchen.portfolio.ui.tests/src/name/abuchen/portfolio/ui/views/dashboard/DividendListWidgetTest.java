package name.abuchen.portfolio.ui.views.dashboard;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import name.abuchen.portfolio.junit.PortfolioBuilder;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Dashboard;
import name.abuchen.portfolio.model.Dashboard.Widget;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityEvent;
import name.abuchen.portfolio.model.SecurityEvent.DividendEvent;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.ui.views.dashboard.DividendListWidget.DateType;
import name.abuchen.portfolio.ui.views.dashboard.DividendListWidget.DividendItem;
import name.abuchen.portfolio.ui.views.dashboard.DividendListWidget.ShowEventsStarting;

@SuppressWarnings("nls")
public class DividendListWidgetTest
{
    private static Locale defaultLocale;

    private Client testClient;

    private Security security1;
    private Security security2;

    @BeforeClass
    public static void setupLocale()
    {
        defaultLocale = Locale.getDefault();
        Locale.setDefault(Locale.GERMANY);
    }

    @AfterClass
    public static void resetLocale()
    {
        Locale.setDefault(defaultLocale);
    }

    @Before
    public void setupTestData()
    {
        security1 = new Security("Security 1", "EUR");
        security1.addEvent(new DividendEvent( //
                        LocalDate.of(2024, 4, 9), LocalDate.of(2024, 4, 12), Money.of("EUR", 55), "source"));
        security1.addEvent(new DividendEvent( //
                        LocalDate.of(2024, 7, 14), LocalDate.of(2024, 8, 11), Money.of("EUR", 22), "source"));
        security1.addEvent(new SecurityEvent( //
                        LocalDate.of(2024, 2, 3), SecurityEvent.Type.NOTE, "some note"));
        security1.addEvent(new DividendEvent( //
                        LocalDate.of(2024, 2, 4), LocalDate.of(2024, 3, 5), Money.of("EUR", 33), "source"));

        security2 = new Security("Security 2", "EUR");
        security2.addEvent(new DividendEvent( //
                        LocalDate.of(2024, 4, 5), LocalDate.of(2024, 4, 9), Money.of("USD", 55), "source"));
        security2.addEvent(new DividendEvent( //
                        LocalDate.of(2024, 7, 14), LocalDate.of(2024, 7, 15), Money.of("USD", 122), "source"));
        security2.addEvent(new SecurityEvent( //
                        LocalDate.of(2024, 8, 25), SecurityEvent.Type.STOCK_SPLIT, "some stock split"));
        // ex-date and payment date on same day
        security2.addEvent(new DividendEvent( //
                        LocalDate.of(2024, 2, 5), LocalDate.of(2024, 2, 5), Money.of("USD", 1333), "source"));
        security2.setRetired(true);

        testClient = new Client();
        testClient.addSecurity(security1);
        testClient.addSecurity(security2);

        new PortfolioBuilder().buy(security1, "2024-01-01", 1, 1).addTo(testClient);
    }

    @Test
    public void testDividendItemInstantiation()
    {
        var event = (DividendEvent) security1.getEvents().get(0);

        DividendItem dividendItem = new DividendItem(security1, event.getDate(), event);
        dividendItem.getTypes().add(DateType.EX_DIVIDEND_DATE);
        assertThat(dividendItem.getSecurity(), is(security1));
        assertThat(dividendItem.getTypes(), equalTo(EnumSet.of(DateType.EX_DIVIDEND_DATE)));
        assertThat(dividendItem.getDividendEvent(), is(event));
    }

    @Test
    public void testDateStartRange()
    {
        assertEquals("TODAY,ONE_WEEK_AGO,ONE_MONTH_AGO,THREE_MONTHS_AGO", //
                        Arrays.stream(ShowEventsStarting.values()).map(ShowEventsStarting::name)
                                        .collect(Collectors.joining(",")));

        var now = LocalDate.of(2024, 4, 8);
        assertThat(ShowEventsStarting.TODAY.getDate(now), is(now));
        assertThat(ShowEventsStarting.ONE_WEEK_AGO.getDate(now), is(LocalDate.of(2024, 4, 1)));
        assertThat(ShowEventsStarting.ONE_MONTH_AGO.getDate(now), is(LocalDate.of(2024, 3, 8)));
        assertThat(ShowEventsStarting.THREE_MONTHS_AGO.getDate(now), is(LocalDate.of(2024, 1, 8)));
    }

    @Test
    public void testDateType()
    {
        var event = (DividendEvent) security1.getEvents().get(0);

        assertEquals("ALL_DATES,EX_DIVIDEND_DATE,PAYMENT_DATE", //
                        Arrays.stream(DateType.values()).map(DateType::name).collect(Collectors.joining(",")));

        assertThat(DateType.ALL_DATES.getDate(event), nullValue());
        assertThat(DateType.EX_DIVIDEND_DATE.getDate(event), is(event.getDate()));
        assertThat(DateType.PAYMENT_DATE.getDate(event), is(event.getPaymentDate()));
    }

    @Test
    public void testAllInstruments()
    {
        var widget = new Widget();
        widget.getConfiguration().put(Dashboard.Config.SECURITY_FILTER.name(),
                        DividendListWidget.SecurityFilter.ALL.name());
        widget.getConfiguration().put(Dashboard.Config.DATE_TYPE_FILTER.name(),
                        DividendListWidget.DateType.ALL_DATES.name());
        widget.getConfiguration().put(Dashboard.Config.START_YEAR.name(),
                        DividendListWidget.ShowEventsStarting.TODAY.name());
        var dashboardData = new DashboardData(testClient);

        var dividendList = new DividendListWidget(widget, dashboardData);

        var now = LocalDate.of(2024, 4, 8);
        var items = dividendList.getUpdateTask(now);

        assertEquals("2024-04-09 Security 1 EUR 0,55 [" + DateType.EX_DIVIDEND_DATE.name() + "]\n" //
                        + "Security 2 USD 0,55 [" + DateType.PAYMENT_DATE.name() + "]\n" //
                        + "2024-04-12 Security 1 EUR 0,55 [" + DateType.PAYMENT_DATE.name() + "]\n" //
                        + "2024-07-14 Security 1 EUR 0,22 [" + DateType.EX_DIVIDEND_DATE.name() + "]\n" //
                        + "Security 2 USD 1,22 [" + DateType.EX_DIVIDEND_DATE.name() + "]\n" //
                        + "2024-07-15 Security 2 USD 1,22 [" + DateType.PAYMENT_DATE.name() + "]\n" //
                        + "2024-08-11 Security 1 EUR 0,22 [" + DateType.PAYMENT_DATE.name() + "]",
                        getListAsString(items));
    }

    @Test
    public void testAllInstrumentsIncludingLastQuarter()
    {
        var widget = new Widget();
        widget.getConfiguration().put(Dashboard.Config.SECURITY_FILTER.name(),
                        DividendListWidget.SecurityFilter.ALL.name());
        widget.getConfiguration().put(Dashboard.Config.DATE_TYPE_FILTER.name(),
                        DividendListWidget.DateType.ALL_DATES.name());
        widget.getConfiguration().put(Dashboard.Config.START_YEAR.name(),
                        DividendListWidget.ShowEventsStarting.THREE_MONTHS_AGO.name());
        var dashboardData = new DashboardData(testClient);

        var dividendList = new DividendListWidget(widget, dashboardData);

        var now = LocalDate.of(2024, 4, 8);
        var items = dividendList.getUpdateTask(now);

        assertEquals("2024-02-04 Security 1 EUR 0,33 [" + DateType.EX_DIVIDEND_DATE.name() + "]\n" //
                        + "2024-02-05 Security 2 USD 13,33 [" + DateType.EX_DIVIDEND_DATE.name() + ","
                        + DateType.PAYMENT_DATE.name() + "]\n" //
                        + "2024-03-05 Security 1 EUR 0,33 [" + DateType.PAYMENT_DATE.name() + "]\n" //
                        + "2024-04-05 Security 2 USD 0,55 [" + DateType.EX_DIVIDEND_DATE.name() + "]\n" //
                        + "2024-04-09 Security 1 EUR 0,55 [" + DateType.EX_DIVIDEND_DATE.name() + "]\n" //
                        + "Security 2 USD 0,55 [" + DateType.PAYMENT_DATE.name() + "]\n" //
                        + "2024-04-12 Security 1 EUR 0,55 [" + DateType.PAYMENT_DATE.name() + "]\n" //
                        + "2024-07-14 Security 1 EUR 0,22 [" + DateType.EX_DIVIDEND_DATE.name() + "]\n" //
                        + "Security 2 USD 1,22 [" + DateType.EX_DIVIDEND_DATE.name() + "]\n" //
                        + "2024-07-15 Security 2 USD 1,22 [" + DateType.PAYMENT_DATE.name() + "]\n" //
                        + "2024-08-11 Security 1 EUR 0,22 [" + DateType.PAYMENT_DATE.name() + "]",
                        getListAsString(items));
    }

    @Test
    public void testInstrumentsWithHoldings()
    {
        var widget = new Widget();
        widget.getConfiguration().put(Dashboard.Config.SECURITY_FILTER.name(),
                        DividendListWidget.SecurityFilter.OWNED.name());
        widget.getConfiguration().put(Dashboard.Config.DATE_TYPE_FILTER.name(),
                        DividendListWidget.DateType.ALL_DATES.name());
        widget.getConfiguration().put(Dashboard.Config.START_YEAR.name(),
                        DividendListWidget.ShowEventsStarting.THREE_MONTHS_AGO.name());
        var dashboardData = new DashboardData(testClient);

        var dividendList = new DividendListWidget(widget, dashboardData);

        var now = LocalDate.of(2024, 4, 8);
        var items = dividendList.getUpdateTask(now);

        assertEquals("2024-02-04 Security 1 EUR 0,33 [" + DateType.EX_DIVIDEND_DATE.name() + "]\n" //
                        + "2024-03-05 Security 1 EUR 0,33 [" + DateType.PAYMENT_DATE.name() + "]\n" //
                        + "2024-04-09 Security 1 EUR 0,55 [" + DateType.EX_DIVIDEND_DATE.name() + "]\n" //
                        + "2024-04-12 Security 1 EUR 0,55 [" + DateType.PAYMENT_DATE.name() + "]\n" //
                        + "2024-07-14 Security 1 EUR 0,22 [" + DateType.EX_DIVIDEND_DATE.name() + "]\n" //
                        + "2024-08-11 Security 1 EUR 0,22 [" + DateType.PAYMENT_DATE.name() + "]",
                        getListAsString(items));
    }

    @Test
    public void testAllInstrumentsWithOnlyExDate()
    {
        var widget = new Widget();
        widget.getConfiguration().put(Dashboard.Config.SECURITY_FILTER.name(),
                        DividendListWidget.SecurityFilter.ALL.name());
        widget.getConfiguration().put(Dashboard.Config.DATE_TYPE_FILTER.name(),
                        DividendListWidget.DateType.EX_DIVIDEND_DATE.name());
        widget.getConfiguration().put(Dashboard.Config.START_YEAR.name(),
                        DividendListWidget.ShowEventsStarting.THREE_MONTHS_AGO.name());
        var dashboardData = new DashboardData(testClient);

        var dividendList = new DividendListWidget(widget, dashboardData);

        var now = LocalDate.of(2024, 4, 8);
        var items = dividendList.getUpdateTask(now);

        assertEquals("2024-02-04 Security 1 EUR 0,33 [" + DateType.EX_DIVIDEND_DATE.name() + "]\n" //
                        + "2024-02-05 Security 2 USD 13,33 [" + DateType.EX_DIVIDEND_DATE.name() + "]\n" //
                        + "2024-04-05 Security 2 USD 0,55 [" + DateType.EX_DIVIDEND_DATE.name() + "]\n" //
                        + "2024-04-09 Security 1 EUR 0,55 [" + DateType.EX_DIVIDEND_DATE.name() + "]\n" //
                        + "2024-07-14 Security 1 EUR 0,22 [" + DateType.EX_DIVIDEND_DATE.name() + "]\n" //
                        + "Security 2 USD 1,22 [" + DateType.EX_DIVIDEND_DATE.name() + "]", //
                        getListAsString(items));
    }

    @Test
    public void testAllInstrumentsWithOnlyPaymentDate()
    {
        var widget = new Widget();
        widget.getConfiguration().put(Dashboard.Config.SECURITY_FILTER.name(),
                        DividendListWidget.SecurityFilter.ALL.name());
        widget.getConfiguration().put(Dashboard.Config.DATE_TYPE_FILTER.name(),
                        DividendListWidget.DateType.PAYMENT_DATE.name());
        widget.getConfiguration().put(Dashboard.Config.START_YEAR.name(),
                        DividendListWidget.ShowEventsStarting.THREE_MONTHS_AGO.name());
        var dashboardData = new DashboardData(testClient);

        var dividendList = new DividendListWidget(widget, dashboardData);

        var now = LocalDate.of(2024, 4, 8);
        var items = dividendList.getUpdateTask(now);

        assertEquals("2024-02-05 Security 2 USD 13,33 [" + DateType.PAYMENT_DATE.name() + "]\n" //
                        + "2024-03-05 Security 1 EUR 0,33 [" + DateType.PAYMENT_DATE.name() + "]\n" //
                        + "2024-04-09 Security 2 USD 0,55 [" + DateType.PAYMENT_DATE.name() + "]\n" //
                        + "2024-04-12 Security 1 EUR 0,55 [" + DateType.PAYMENT_DATE.name() + "]\n" //
                        + "2024-07-15 Security 2 USD 1,22 [" + DateType.PAYMENT_DATE.name() + "]\n" //
                        + "2024-08-11 Security 1 EUR 0,22 [" + DateType.PAYMENT_DATE.name() + "]",
                        getListAsString(items));
    }

    @Test
    public void testActiveInstruments()
    {
        var widget = new Widget();
        widget.getConfiguration().put(Dashboard.Config.SECURITY_FILTER.name(),
                        DividendListWidget.SecurityFilter.ACTIVE.name());
        widget.getConfiguration().put(Dashboard.Config.DATE_TYPE_FILTER.name(),
                        DividendListWidget.DateType.ALL_DATES.name());
        widget.getConfiguration().put(Dashboard.Config.START_YEAR.name(),
                        DividendListWidget.ShowEventsStarting.THREE_MONTHS_AGO.name());
        var dashboardData = new DashboardData(testClient);

        var dividendList = new DividendListWidget(widget, dashboardData);

        var now = LocalDate.of(2024, 4, 8);
        var items = dividendList.getUpdateTask(now);

        assertEquals("2024-02-04 Security 1 EUR 0,33 [" + DateType.EX_DIVIDEND_DATE.name() + "]\n" //
                        + "2024-03-05 Security 1 EUR 0,33 [" + DateType.PAYMENT_DATE.name() + "]\n" //
                        + "2024-04-09 Security 1 EUR 0,55 [" + DateType.EX_DIVIDEND_DATE.name() + "]\n" //
                        + "2024-04-12 Security 1 EUR 0,55 [" + DateType.PAYMENT_DATE.name() + "]\n" //
                        + "2024-07-14 Security 1 EUR 0,22 [" + DateType.EX_DIVIDEND_DATE.name() + "]\n" //
                        + "2024-08-11 Security 1 EUR 0,22 [" + DateType.PAYMENT_DATE.name() + "]",
                        getListAsString(items));
    }

    private String getListAsString(List<DividendItem> list)
    {
        return list.stream() //
                        .map(entry -> {
                            var sb = new StringBuilder();
                            if (entry.hasNewDate())
                            {
                                sb.append(entry.getDate().toString() + " ");
                            }
                            sb.append(entry.getSecurity().getName() + " ");
                            sb.append(entry.getDividendEvent().getAmount() + " ");
                            sb.append(entry.getTypes().stream().map(Enum::name)
                                            .collect(Collectors.joining(",", "[", "]")));
                            return sb.toString();
                        }) //
                        .collect(Collectors.joining("\n"));
    }
}
