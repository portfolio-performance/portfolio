package name.abuchen.portfolio.ui.views.dashboard;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityEvent;
import name.abuchen.portfolio.model.SecurityEvent.DividendEvent;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.views.dashboard.DividendListWidget.DateEndRange;
import name.abuchen.portfolio.ui.views.dashboard.DividendListWidget.DateStartRange;
import name.abuchen.portfolio.ui.views.dashboard.DividendListWidget.DateType;
import name.abuchen.portfolio.ui.views.dashboard.DividendListWidget.DividendItem;

public class DividendListWidgetTest
{
    private static Locale defaultLocale;

    private Security sec1;
    private Security sec2;
    private DividendEvent de1_1;
    private DividendEvent de1_2;
    private SecurityEvent de1_3;
    private DividendEvent de1_4;
    private DividendEvent de1_5;
    private DividendEvent de2_1;
    private DividendEvent de2_2;
    private SecurityEvent de2_3;
    private DividendEvent de2_4;

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
        sec1 = new Security("Security 1", "EUR");
        de1_1 = new DividendEvent(LocalDate.of(2024, 4, 9), LocalDate.of(2024, 4, 12), Money.of("EUR", 55), "source");
        de1_2 = new DividendEvent(LocalDate.of(2024, 7, 14), LocalDate.of(2024, 8, 11), Money.of("EUR", 22), "source");
        de1_3 = new SecurityEvent(LocalDate.of(2024, 2, 3), SecurityEvent.Type.NOTE, "some note");
        de1_4 = new DividendEvent(LocalDate.of(2024, 2, 4), LocalDate.of(2024, 3, 5), Money.of("EUR", 33), "source");
        de1_5 = new DividendEvent(LocalDate.of(2024, 8, 1), LocalDate.of(2024, 8, 1), Money.of("EUR", 33), "source");
        sec1.addEvent(de1_1);
        sec1.addEvent(de1_2);
        sec1.addEvent(de1_3);
        sec1.addEvent(de1_4);

        sec2 = new Security("Security 2", "EUR");
        de2_1 = new DividendEvent(LocalDate.of(2024, 4, 5), LocalDate.of(2024, 4, 9), Money.of("USD", 55), "source");
        de2_2 = new DividendEvent(LocalDate.of(2024, 7, 14), LocalDate.of(2024, 7, 15), Money.of("USD", 122), "source");
        de2_3 = new SecurityEvent(LocalDate.of(2024, 8, 25), SecurityEvent.Type.STOCK_SPLIT, "some stock split");
        de2_4 = new DividendEvent(LocalDate.of(2024, 2, 5), LocalDate.of(2024, 3, 5), Money.of("USD", 1333), "source");
        sec2.addEvent(de2_1);
        sec2.addEvent(de2_2);
        sec2.addEvent(de2_3);
        sec2.addEvent(de2_4);

    }

    @Test
    public void testDividendItemInstantiation()
    {
        DividendItem di;
        
        di = new DividendItem(DateType.EX_DIVIDEND_DATE, sec1, de1_1);
        assertThat(di.getSecurity(), is(sec1));
        assertThat(di.getFirstType(), is(DateType.EX_DIVIDEND_DATE));
        assertThat(di.div, is(de1_1));
    }

    @Test
    public void testDateStartRange()
    {
        assertEquals("FROM_TODAY,FROM_ONE_WEEK,FROM_ONE_MONTH,FROM_YTD", //
                        Arrays.stream(DateStartRange.values())
                        .map(DateStartRange::name).collect(Collectors.joining(",")));

        LocalDate now = LocalDate.of(2024, 4, 8);
        assertThat(DateStartRange.FROM_TODAY.getDate(now), is(now));
        assertThat(DateStartRange.FROM_ONE_WEEK.getDate(now), is(LocalDate.of(2024, 4, 1)));
        assertThat(DateStartRange.FROM_ONE_MONTH.getDate(now), is(LocalDate.of(2024, 3, 8)));
        assertThat(DateStartRange.FROM_YTD.getDate(now), is(LocalDate.of(2024, 1, 1)));
    }

    @Test
    public void testDateEndRange()
    {
        assertEquals("UNTIL_EOY,UNTIL_ONE_MONTH,UNTIL_ONE_WEEK,UNTIL_TODAY", //
                        Arrays.stream(DateEndRange.values()).map(DateEndRange::name).collect(Collectors.joining(",")));

        LocalDate now = LocalDate.of(2024, 4, 8);
        assertThat(DateEndRange.UNTIL_TODAY.getDate(now), is(now));
        assertThat(DateEndRange.UNTIL_ONE_WEEK.getDate(now), is(LocalDate.of(2024, 4, 15)));
        assertThat(DateEndRange.UNTIL_ONE_MONTH.getDate(now), is(LocalDate.of(2024, 5, 8)));
        assertThat(DateEndRange.UNTIL_EOY.getDate(now), is(LocalDate.of(2024, 12, 31)));
    }

    @Test
    public void testDateType()
    {
        assertEquals("ALL_DATES,EX_DIVIDEND_DATE,PAYMENT_DATE", //
                        Arrays.stream(DateType.values()).map(DateType::name).collect(Collectors.joining(",")));

        assertThat(DateType.ALL_DATES.getDate(de1_1), nullValue());
        assertThat(DateType.EX_DIVIDEND_DATE.getDate(de1_1), is(de1_1.getDate()));
        assertThat(DateType.PAYMENT_DATE.getDate(de1_1), is(de1_1.getPaymentDate()));
    }

    @Test
    public void testGetUpdateTask() throws Exception
    {
        LocalDate testnow = LocalDate.now();
        @SuppressWarnings("deprecation")
        AbstractSecurityListWidget<DividendItem> widget = new DividendListWidget()
        {
            @Override
            public Supplier<List<DividendItem>> getUpdateTask(LocalDate now)
            {
                assertThat(now, is(testnow));
                return null;
            }
        };
        assertThat(widget.getUpdateTask(), nullValue());
    }

    @Test
    public void testGetUpdateTaskNow()
    {
        List<DividendItem> list;
        AtomicReference<DateStartRange> dateStart = new AtomicReference<>(DateStartRange.FROM_TODAY);
        AtomicReference<DateEndRange> dateEnd = new AtomicReference<>(DateEndRange.UNTIL_TODAY);
        AtomicReference<DateType> dateType = new AtomicReference<>(DateType.ALL_DATES);

        @SuppressWarnings("deprecation")
        DividendListWidget widget = new DividendListWidget()
        {
            @Override
            DateStartRange getStartRangeValue()
            {
                return dateStart.get();
            }

            @Override
            DateEndRange getEndRangeValue()
            {
                return dateEnd.get();
            }

            @Override
            DateType getDateTypeValue()
            {
                return dateType.get();
            }

            @Override
            List<Security> getSecurities()
            {
                return Arrays.asList(sec1, sec2);
            }
        };

        LocalDate now = LocalDate.of(2024, 4, 9);
        list = widget.getUpdateTask(now).get();
        assertEquals("2024-04-09 Security 1 EUR 0,55 [" + Messages.ColumnExDate + "]\r\n" //
                        + "Security 2 USD 0,55 [" + Messages.ColumnPaymentDate + "]", getListAsString(list));

        now = LocalDate.of(2024, 4, 8);
        list = widget.getUpdateTask(now).get();
        assertEquals("", getListAsString(list));

        dateStart.set(DateStartRange.FROM_YTD);
        list = widget.getUpdateTask(now).get();
        assertEquals("2024-02-04 Security 1 EUR 0,33 [" + Messages.ColumnExDate + "]\r\n" //
                        + "2024-02-05 Security 2 USD 13,33 [" + Messages.ColumnExDate + "]\r\n" //
                        + "2024-03-05 Security 1 EUR 0,33 [" + Messages.ColumnPaymentDate + "]\r\n" //
                        + "Security 2 USD 13,33 [" + Messages.ColumnPaymentDate + "]\r\n" //
                        + "2024-04-05 Security 2 USD 0,55 [" + Messages.ColumnExDate + "]", getListAsString(list));

        dateEnd.set(DateEndRange.UNTIL_EOY);
        list = widget.getUpdateTask(now).get();
        assertEquals("2024-02-04 Security 1 EUR 0,33 [" + Messages.ColumnExDate + "]\r\n" //
                        + "2024-02-05 Security 2 USD 13,33 [" + Messages.ColumnExDate + "]\r\n" //
                        + "2024-03-05 Security 1 EUR 0,33 [" + Messages.ColumnPaymentDate + "]\r\n" //
                        + "Security 2 USD 13,33 [" + Messages.ColumnPaymentDate + "]\r\n" //
                        + "2024-04-05 Security 2 USD 0,55 [" + Messages.ColumnExDate + "]\r\n" //
                        + "2024-04-09 Security 1 EUR 0,55 [" + Messages.ColumnExDate + "]\r\n" //
                        + "Security 2 USD 0,55 [" + Messages.ColumnPaymentDate + "]\r\n" //
                        + "2024-04-12 Security 1 EUR 0,55 [" + Messages.ColumnPaymentDate + "]\r\n" //
                        + "2024-07-14 Security 1 EUR 0,22 [" + Messages.ColumnExDate + "]\r\n" //
                        + "Security 2 USD 1,22 [" + Messages.ColumnExDate + "]\r\n" //
                        + "2024-07-15 Security 2 USD 1,22 [" + Messages.ColumnPaymentDate + "]\r\n" //
                        + "2024-08-11 Security 1 EUR 0,22 [" + Messages.ColumnPaymentDate + "]", getListAsString(list));
        
        dateType.set(DateType.EX_DIVIDEND_DATE);
        list = widget.getUpdateTask(now).get();
        assertEquals("2024-02-04 Security 1 EUR 0,33 [" + Messages.ColumnExDate + "]\r\n" //
                        + "2024-02-05 Security 2 USD 13,33 [" + Messages.ColumnExDate + "]\r\n" //
                        + "2024-04-05 Security 2 USD 0,55 [" + Messages.ColumnExDate + "]\r\n" //
                        + "2024-04-09 Security 1 EUR 0,55 [" + Messages.ColumnExDate + "]\r\n" //
                        + "2024-07-14 Security 1 EUR 0,22 [" + Messages.ColumnExDate + "]\r\n" //
                        + "Security 2 USD 1,22 [" + Messages.ColumnExDate + "]", getListAsString(list));

        dateType.set(DateType.PAYMENT_DATE);
        list = widget.getUpdateTask(now).get();
        assertEquals("2024-03-05 Security 1 EUR 0,33 [" + Messages.ColumnPaymentDate + "]\r\n" //
                        + "Security 2 USD 13,33 [" + Messages.ColumnPaymentDate + "]\r\n" //
                        + "2024-04-09 Security 2 USD 0,55 [" + Messages.ColumnPaymentDate + "]\r\n" //
                        + "2024-04-12 Security 1 EUR 0,55 [" + Messages.ColumnPaymentDate + "]\r\n" //
                        + "2024-07-15 Security 2 USD 1,22 [" + Messages.ColumnPaymentDate + "]\r\n" //
                        + "2024-08-11 Security 1 EUR 0,22 [" + Messages.ColumnPaymentDate + "]", getListAsString(list));

        sec1.getEvents().clear();
        sec2.getEvents().clear();
        sec1.addEvent(de1_5);
        list = widget.getUpdateTask(now).get();
        assertEquals("2024-08-01 Security 1 EUR 0,33 [" + Messages.ColumnPaymentDate + "]", getListAsString(list));
        dateType.set(DateType.EX_DIVIDEND_DATE);
        list = widget.getUpdateTask(now).get();
        assertEquals("2024-08-01 Security 1 EUR 0,33 [" + Messages.ColumnExDate + "]", getListAsString(list));
        dateType.set(DateType.ALL_DATES);
        list = widget.getUpdateTask(now).get();
        assertEquals("2024-08-01 Security 1 EUR 0,33 [" + Messages.ColumnExDate + ", " + Messages.ColumnPaymentDate
                        + "]", getListAsString(list));
    }

    private String getListAsString(List<DividendItem> list)
    {
        return list.stream() //
                        .map(entry -> {
                            StringBuilder sb = new StringBuilder();
                            if (entry.hasNewDate)
                            {
                                sb.append(entry.getFirstType().getDate(entry.div).toString() + " ");
                            }
                            sb.append(entry.getSecurity().getName() + " ");
                            sb.append(entry.div.getAmount() + " ");
                            sb.append(entry.types);
                            return sb.toString();
                        }) //
                        .collect(Collectors.joining("\r\n"));
    }
}
