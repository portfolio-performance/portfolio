package name.abuchen.portfolio.ui.views.dashboard;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import com.google.common.annotations.VisibleForTesting;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Dashboard;
import name.abuchen.portfolio.model.Dashboard.Widget;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityEvent;
import name.abuchen.portfolio.model.SecurityEvent.DividendEvent;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.util.Colors;
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
        FROM_QUARTER(Messages.LabelReportingDialogQuarter, d -> d.minus(3, ChronoUnit.MONTHS)), //
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
        UNTIL_ALL(Messages.LabelAllAttributes, d -> null), //
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
     * This constructor is only used for testing, don't use it for
     *             anything else
     */
    @VisibleForTesting
    DividendListWidget() // needed for testing
    {
        super(null, null);
    }

    public DividendListWidget(Widget widget, DashboardData data)
    {
        super(widget, data);

        addConfig(new ClientFilterConfig(this));
        addConfig(new DateTypeConfig(this));
        addConfig(new DateStartRangeConfig(this));
    }

    @Override
    public Supplier<List<DividendItem>> getUpdateTask()
    {
        return () -> getUpdateTask(LocalDate.now());
    }

    List<DividendItem> getUpdateTask(LocalDate now)
    {
        DateType dateType = getDateTypeValue();
        ClientFilterConfig clientFilter = getClientFilterConfig();
        LocalDate fromDate = getStartRangeValue().getDate(now);
        LocalDate untilDate = getEndRangeValue().getDate(now);

        // get all securities to show and put them into a map to benefit
        // from O(1)
        Set<Security> secSet = getSecuritiesToShow(clientFilter);

        List<DividendItem> items = new ArrayList<>();
        getSecurities().forEach(security -> handleSecurity(security, items, dateType, fromDate, untilDate, secSet));
        groupListEntries(items);

        return items;
    }

    void groupListEntries(List<DividendItem> items)
    {
        Collections.sort(items, new DividendItemComparator());
        LocalDate prevDate = null;
        for (DividendItem item : items)
        {
            LocalDate currDate = item.getFirstType().getDate(item.div);
            if (prevDate == null || !currDate.isEqual(prevDate))
            {
                item.hasNewDate = true;
                prevDate = currDate;
            }
        }
    }

    void handleSecurity(Security security, List<DividendItem> items, DateType dateType, LocalDate fromDate,
                    LocalDate untilDate, Set<Security> activeSecurities)
    {
        if (!activeSecurities.contains(security))
        {
            return;
        }

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

    Set<Security> getSecuritiesToShow(ClientFilterConfig clientFilter)
    {
        Client filteredClient = clientFilter.getSelectedFilter().filter(getClient());
        HashSet<Security> secSet = new HashSet<>();
        secSet.addAll(filteredClient.getActiveSecurities());
        return secSet;
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

    ClientFilterConfig getClientFilterConfig()
    {
        return get(ClientFilterConfig.class);
    }

    DateStartRange getStartRangeValue()
    {
        return get(DateStartRangeConfig.class).getValue();
    }

    DateEndRange getEndRangeValue()
    {
        return DateEndRange.UNTIL_ALL;
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

        if (endRange != null && checkDate.isAfter(endRange))
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
    public Composite createControl(Composite parent, DashboardResources resources)
    {
        Composite ret = super.createControl(parent, resources);
        GridLayoutFactory.fillDefaults().numColumns(1).margins(5, 5).applyTo(ret);
        GridLayoutFactory.fillDefaults().numColumns(1).spacing(0, 2).applyTo(list);
        GridDataFactory.fillDefaults().grab(true, true).applyTo(list);

        return ret;
    }

    @Override
    protected void createTitleArea(Composite container)
    {
        super.createTitleArea(container);

        Composite cmp = new Composite(container, SWT.NONE);
        cmp.setLayout(new FormLayout());
        Label exDate = createLabel(cmp, DateType.EX_DIVIDEND_DATE.label);
        exDate.setForeground(Colors.DARK_RED);
        Label paymDate = createLabel(cmp, DateType.PAYMENT_DATE.label);
        paymDate.setForeground(Colors.DARK_GREEN);
        Label exPaymDate = createLabel(cmp, DateType.EX_DIVIDEND_DATE.label + "," + DateType.PAYMENT_DATE.label); //$NON-NLS-1$
        exPaymDate.setForeground(container.getDisplay().getSystemColor(SWT.COLOR_DARK_YELLOW));

        FormDataFactory.startingWith(exDate).thenRight(paymDate, 10).thenRight(exPaymDate, 10);

    }

    @Override
    protected Composite createItemControl(Composite parent, DividendItem item)
    {
        Security sec = item.getSecurity();

        if (item.hasNewDate)
        {
            Composite dtCmp = new Composite(parent, SWT.NONE);
            dtCmp.setLayout(new FormLayout());
            Label date = createLabel(dtCmp, Values.Date.format(item.getFirstType().getDate(item.div)));
            date.setData(UIConstants.CSS.CLASS_NAME, UIConstants.CSS.HEADING2);
        }

        Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayout(new FormLayout());

        Image image = LogoManager.instance().getDefaultColumnImage(sec, getClient().getSettings());
        Label logo = createLabel(composite, image);
        Label name = createLabel(composite, sec.getName());
        Label amt = createLabel(composite,
                        Values.Money.format(item.div.getAmount(), getClient().getBaseCurrency(), false), SWT.RIGHT);
        setForegroundColor(item, amt);

        addListener(mouseUpAdapter, composite, name, amt);

        FormDataFactory.startingWith(logo).thenRight(name).right(new FormAttachment(amt, -5, SWT.LEFT));
        FormDataFactory.startingWith(amt).right(new FormAttachment(100));
        GridDataFactory.fillDefaults().grab(true, false).applyTo(composite);
        return composite;
    }

    void setForegroundColor(DividendItem item, Label label)
    {
        if (item.types.contains(DateType.EX_DIVIDEND_DATE))
        {
            if (item.types.contains(DateType.PAYMENT_DATE))
            {
                label.setForeground(label.getDisplay().getSystemColor(SWT.COLOR_DARK_YELLOW));
            }
            else
            {
                label.setForeground(Colors.DARK_RED);
            }
        }
        else if (item.types.contains(DateType.PAYMENT_DATE))
        {
            label.setForeground(Colors.DARK_GREEN);
        }
    }

    @Override
    protected void createEmptyControl(Composite parent)
    {
        // nothing to do
    }

    @Override
    protected boolean hasHeightSetting()
    {
        return false;
    }
}
