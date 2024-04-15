package name.abuchen.portfolio.ui.views.dashboard;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import name.abuchen.portfolio.model.Dashboard;
import name.abuchen.portfolio.model.Dashboard.Widget;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityEvent;
import name.abuchen.portfolio.model.SecurityEvent.DividendEvent;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.FormDataFactory;
import name.abuchen.portfolio.ui.util.LogoManager;

public class DividendListWidget extends AbstractSecurityListWidget<DividendListWidget.DividendItem>
{
    public static class DividendItem extends AbstractSecurityListWidget.Item
    {
        boolean hasNewDate;
        Deque<DateType> types = new LinkedList<>();
        DividendEvent div;

        public DividendItem(DateType type, Security security, DividendEvent div)
        {
            super(security);
            this.div = div;
            this.types.add(type);
        }

        DateType getFirstType()
        {
            return types.peekFirst();
        }
    }

    public enum DateStartRange
    {
        FROM_TODAY(Messages.LabelToday, d -> d), //
        FROM_ONE_WEEK(Messages.LabelReportingDialogWeek, d -> d.minus(1, ChronoUnit.WEEKS)), //
        FROM_ONE_MONTH(Messages.LabelReportingDialogMonth, d -> d.minus(1, ChronoUnit.MONTHS)), //
        FROM_YTD(Messages.LabelReportingDialogYearYTD, d -> LocalDate.of(d.getYear(), 1, 1)), //
        ;

        private DateStartRange(String label, UnaryOperator<LocalDate> dateStartProvider)
        {
            this.label = label;
            this.dateStartProvider = dateStartProvider;
        }

        private String label;
        private UnaryOperator<LocalDate> dateStartProvider;

        LocalDate getDate(LocalDate now)
        {
            return dateStartProvider.apply(now);
        }

        @Override
        public String toString()
        {
            return label;
        }
    }

    static class DateStartRangeConfig extends EnumBasedConfig<DateStartRange>
    {
        public DateStartRangeConfig(WidgetDelegate<?> delegate)
        {
            super(delegate, Messages.LabelEarningsDividendPeriodFrom, DateStartRange.class,
                            Dashboard.Config.START_YEAR, Policy.EXACTLY_ONE);
        }
    }

    public enum DateEndRange
    {
        UNTIL_EOY(Messages.LabelReportingDialogYear, d -> LocalDate.of(d.getYear(), 12, 31)), //
        UNTIL_ONE_MONTH(Messages.LabelReportingDialogMonth, d -> d.plus(1, ChronoUnit.MONTHS)), //
        UNTIL_ONE_WEEK(Messages.LabelReportingDialogWeek, d -> d.plus(1, ChronoUnit.WEEKS)), //
        UNTIL_TODAY(Messages.LabelToday, d -> d), //
        ;

        private DateEndRange(String label, UnaryOperator<LocalDate> dateStartProvider)
        {
            this.label = label;
            this.dateEndProvider = dateStartProvider;
        }

        private String label;
        private UnaryOperator<LocalDate> dateEndProvider;

        LocalDate getDate(LocalDate now)
        {
            return dateEndProvider.apply(now);
        }

        @Override
        public String toString()
        {
            return label;
        }
    }

    static class DateEndRangeConfig extends EnumBasedConfig<DateEndRange>
    {
        public DateEndRangeConfig(WidgetDelegate<?> delegate)
        {
            super(delegate, Messages.LabelEarningsDividendPeriodUntil, DateEndRange.class,
                            Dashboard.Config.REPORTING_PERIOD, Policy.EXACTLY_ONE);
        }
    }

    public enum DateType
    {
        ALL_DATES(Messages.LabelAllAttributes, d -> null), //
        EX_DIVIDEND_DATE(Messages.ColumnExDate, DividendEvent::getDate), //
        PAYMENT_DATE(Messages.ColumnPaymentDate, DividendEvent::getPaymentDate), //
        ;

        private DateType(String label, Function<DividendEvent, LocalDate> dateProvider)
        {
            this.label = label;
            this.dateProvider = dateProvider;
        }

        private String label;
        private Function<DividendEvent, LocalDate> dateProvider;

        public LocalDate getDate(DividendEvent evt)
        {
            return dateProvider.apply(evt);
        }

        @Override
        public String toString()
        {
            return label;
        }
    }

    static class DateTypeConfig extends EnumBasedConfig<DateType>
    {
        public DateTypeConfig(WidgetDelegate<?> delegate)
        {
            super(delegate, Messages.LabelEarningsDividendDateType, DateType.class, Dashboard.Config.EARNING_TYPE,
                            Policy.EXACTLY_ONE);
        }
    }

    /**
     * @deprecated This constructor is only used for testing, don't use it for
     *             anything else
     */
    @Deprecated
    DividendListWidget() // needed for testing
    {
        super(null, null);
    }

    public DividendListWidget(Widget widget, DashboardData data)
    {
        super(widget, data);

        addConfig(new DateTypeConfig(this));
        addConfig(new DateStartRangeConfig(this));
        addConfig(new DateEndRangeConfig(this));
        addConfig(new ChartHeightConfig(this));
    }

    @Override
    public Supplier<List<DividendItem>> getUpdateTask()
    {
        return getUpdateTask(LocalDate.now());
    }

    Supplier<List<DividendItem>> getUpdateTask(LocalDate now)
    {
        return () -> {

            DateType dateType = getDateTypeValue();

            List<DividendItem> items = new ArrayList<>();
            LocalDate fromDate = getStartRangeValue().getDate(now);
            LocalDate untilDate = getEndRangeValue().getDate(now);
            for (Security security : getSecurities())
            {
                for (SecurityEvent se : security.getEvents())
                {
                    if (!(se instanceof DividendEvent de))
                    {
                        continue;
                    }
                    DividendItem added = checkAndAdd(items, security, de, dateType, DateType.EX_DIVIDEND_DATE, fromDate,
                                    untilDate, null);
                    checkAndAdd(items, security, de, dateType, DateType.PAYMENT_DATE, fromDate, untilDate, added);
                }
            }

            Collections.sort(items, new DividendItemComparator());
            LocalDate prevDate = null;
            DividendItem prevItem = null;
            for (DividendItem item : items)
            {
                prevItem = item;
                LocalDate currDate = item.getFirstType().getDate(item.div);
                if (prevDate == null || !currDate.isEqual(prevDate))
                {
                    item.hasNewDate = true;
                    prevDate = currDate;
                    continue;
                }
            }

            return items;
        };
    }

    static class DividendItemComparator implements Comparator<DividendItem>
    {

        @Override
        public int compare(DividendItem di1, DividendItem di2)
        {
            DateType type1 = di1.getFirstType();
            DateType type2 = di2.getFirstType();
            int ret = type1.getDate(di1.div).compareTo(type2.getDate(di2.div));
            if (ret != 0)
            {
                return ret; //
            }
            ret = String.CASE_INSENSITIVE_ORDER.compare(di1.getSecurity().getName(), di2.getSecurity().getName());
            if (ret != 0)
            {
                return ret; //
            }
            return type1.compareTo(type2);
        }
    }

    DateStartRange getStartRangeValue()
    {
        return get(DateStartRangeConfig.class).getValue();
    }

    DateEndRange getEndRangeValue()
    {
        return get(DateEndRangeConfig.class).getValue();
    }

    DateType getDateTypeValue()
    {
        return get(DateTypeConfig.class).getValue();
    }

    List<Security> getSecurities()
    {
        return getClient().getSecurities();
    }

    DividendItem checkAndAdd(List<DividendItem> items, Security sec, DividendEvent de, DateType configuredType,
                    DateType typeToAdd, LocalDate startRange, LocalDate endRange, DividendItem prevAdded)
    {
        if (configuredType != typeToAdd && configuredType != DateType.ALL_DATES)
        {
            return null;
        }

        LocalDate checkDate = typeToAdd.getDate(de);
        if (checkDate.isBefore(startRange))
        {
            return null; //
        }

        if (checkDate.isAfter(endRange))
        {
            return null; //
        }

        DividendItem ret = new DividendItem(typeToAdd, sec, de);
        if (prevAdded != null && checkDate.equals(prevAdded.types.peekLast().getDate(de)))
        {
            prevAdded.types.add(typeToAdd);
        }
        else
        {
            items.add(ret);
        }
        return ret;
    }

    @Override
    protected Composite createItemControl(Composite parent, DividendItem item)
    {
        Security sec = item.getSecurity();
        Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayout(new FormLayout());

        Image image = LogoManager.instance().getDefaultColumnImage(sec, getClient().getSettings());
        Label logo = createLabel(composite, image);
        Label name = createLabel(composite, sec.getName());
        String typeStr = item.types.stream() //
                        .map(dt -> dt.label) //
                        .collect(Collectors.joining(", "));
        Label amtAndType = createLabel(composite,
                        "   " + Values.Money.format(item.div.getAmount()) + " " + typeStr); //$NON-NLS-1$ //$NON-NLS-2$


        Label date = null;

        if (item.hasNewDate)
        {
            date = createLabel(composite, Values.Date.format(item.getFirstType().getDate(item.div)));
        }

        addListener(mouseUpAdapter, composite, name, date, amtAndType);

        FormDataFactory start;
        if (date != null)
        {
            start = FormDataFactory.startingWith(date).thenBelow(logo);
        }
        else
        {
            start = FormDataFactory.startingWith(logo);
        }
        start.thenRight(name).right(new FormAttachment(100)).thenBelow(amtAndType);

        return composite;
    }

    @Override
    protected void createEmptyControl(Composite parent)
    {
        // nothing to do
    }

}
