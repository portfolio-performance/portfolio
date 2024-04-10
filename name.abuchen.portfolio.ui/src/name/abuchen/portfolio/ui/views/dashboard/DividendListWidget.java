package name.abuchen.portfolio.ui.views.dashboard;

import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;

import com.google.common.annotations.VisibleForTesting;

import name.abuchen.portfolio.model.Dashboard;
import name.abuchen.portfolio.model.Dashboard.Widget;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityEvent;
import name.abuchen.portfolio.model.SecurityEvent.DividendEvent;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.ClientSnapshot;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.util.FormDataFactory;
import name.abuchen.portfolio.ui.util.LogoManager;
import name.abuchen.portfolio.ui.views.dashboard.lists.AbstractSecurityListWidget;
import name.abuchen.portfolio.util.TextUtil;

public class DividendListWidget extends AbstractSecurityListWidget<DividendListWidget.DividendItem>
{
    public static class DividendItem extends AbstractSecurityListWidget.Item
    {
        private boolean hasNewDate;
        private final LocalDate date;
        private final Set<DateType> types = EnumSet.noneOf(DateType.class);
        private final DividendEvent dividendEvent;

        public DividendItem(Security security, LocalDate date, DividendEvent dividendEvent)
        {
            super(security);
            this.date = date;
            this.dividendEvent = dividendEvent;
        }

        LocalDate getDate()
        {
            return date;
        }

        DividendEvent getDividendEvent()
        {
            return dividendEvent;
        }

        boolean hasNewDate()
        {
            return hasNewDate;
        }

        Set<DateType> getTypes()
        {
            return types;
        }
    }

    public enum DateType
    {
        ALL_DATES(INDICATOR_EX_DATE + INDICATOR_PAY_DATE + " " + Messages.LabelAllAttributes, d -> null), //$NON-NLS-1$
        EX_DIVIDEND_DATE(INDICATOR_EX_DATE + " " + Messages.ColumnExDate, DividendEvent::getDate), //$NON-NLS-1$
        PAYMENT_DATE(INDICATOR_PAY_DATE + " " + Messages.ColumnPaymentDate, DividendEvent::getPaymentDate); //$NON-NLS-1$

        private String label;
        private Function<DividendEvent, LocalDate> dateProvider;

        private DateType(String label, Function<DividendEvent, LocalDate> dateProvider)
        {
            this.label = label;
            this.dateProvider = dateProvider;
        }

        public LocalDate getDate(DividendEvent event)
        {
            return dateProvider.apply(event);
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
            super(delegate, Messages.LabelEarningsDividendDateType, DateType.class, Dashboard.Config.DATE_TYPE_FILTER,
                            Policy.EXACTLY_ONE);
        }
    }

    enum SecurityFilter
    {
        ALL(Messages.LabelAllSecurities), //
        ACTIVE(Messages.LabelActiveSecurities), //
        OWNED(Messages.LabelHeldSecurities);

        private String label;

        private SecurityFilter(String label)
        {
            this.label = label;
        }

        @Override
        public String toString()
        {
            return label;
        }
    }

    static class SecuritySelectionConfig extends EnumBasedConfig<SecurityFilter>
    {
        public SecuritySelectionConfig(WidgetDelegate<?> delegate)
        {
            super(delegate, Messages.LabelSecurities, SecurityFilter.class, Dashboard.Config.SECURITY_FILTER,
                            Policy.EXACTLY_ONE);
        }
    }

    public enum ShowEventsStarting
    {
        TODAY(Messages.LabelToday, d -> d), //
        ONE_WEEK_AGO(MessageFormat.format(Messages.LabelWeeksAgo, 1), d -> d.minus(1, ChronoUnit.WEEKS)), //
        ONE_MONTH_AGO(MessageFormat.format(Messages.LabelMonthsAgo, 1), d -> d.minus(1, ChronoUnit.MONTHS)), //
        THREE_MONTHS_AGO(MessageFormat.format(Messages.LabelMonthsAgo, 3), d -> d.minus(3, ChronoUnit.MONTHS));

        private ShowEventsStarting(String label, UnaryOperator<LocalDate> dateStartProvider)
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

    static class ShowEventsStartingConfig extends EnumBasedConfig<ShowEventsStarting>
    {
        public ShowEventsStartingConfig(WidgetDelegate<?> delegate)
        {
            super(delegate, Messages.LabelShowEventsStarting, ShowEventsStarting.class, Dashboard.Config.START_YEAR,
                            Policy.EXACTLY_ONE);
        }
    }

    static class DividendItemComparator implements Comparator<DividendItem>
    {
        @Override
        public int compare(DividendItem left, DividendItem right)
        {
            var compare = left.getDate().compareTo(right.getDate());
            if (compare != 0)
                return compare;

            return TextUtil.compare(left.getSecurity().getName(), right.getSecurity().getName());
        }
    }

    private static final String INDICATOR_EX_DATE = "◇"; //$NON-NLS-1$
    private static final String INDICATOR_PAY_DATE = "◆"; //$NON-NLS-1$

    public DividendListWidget(Widget widget, DashboardData data)
    {
        super(widget, data);

        addConfig(new DateTypeConfig(this));
        addConfig(new SecuritySelectionConfig(this));
        addConfig(new ClientFilterConfig(this));
        addConfig(new ShowEventsStartingConfig(this));
    }

    @Override
    public Supplier<List<DividendItem>> getUpdateTask()
    {
        return () -> getUpdateTask(LocalDate.now());
    }

    @VisibleForTesting
    protected List<DividendItem> getUpdateTask(LocalDate now)
    {
        var dateType = get(DateTypeConfig.class).getValue();
        var fromDate = get(ShowEventsStartingConfig.class).getValue().getDate(now);

        var securities = getSecurities(now);

        List<DividendItem> items = new ArrayList<>();
        for (var secur : securities)
            handleSecurity(secur, items, dateType, fromDate);

        groupListEntries(items);

        return items;
    }

    private void groupListEntries(List<DividendItem> items)
    {
        Collections.sort(items, new DividendItemComparator());
        LocalDate previousDate = null;
        for (var item : items)
        {
            var currentDate = item.getDate();
            if (previousDate == null || !currentDate.isEqual(previousDate))
            {
                item.hasNewDate = true;
                previousDate = currentDate;
            }
        }
    }

    private void handleSecurity(Security security, List<DividendItem> items, DateType dateType, LocalDate fromDate)
    {
        for (SecurityEvent event : security.getEvents())
        {
            if (event instanceof DividendEvent dividendEvent)
            {
                var exItem = createItem(security, dividendEvent, dateType, DateType.EX_DIVIDEND_DATE, fromDate);
                if (exItem != null)
                    items.add(exItem);

                var payItem = createItem(security, dividendEvent, dateType, DateType.PAYMENT_DATE, fromDate);
                if (payItem != null)
                {
                    if (exItem != null && exItem.getDate().equals(payItem.getDate()))
                    {
                        exItem.getTypes().add(DateType.PAYMENT_DATE);
                    }
                    else
                    {
                        items.add(payItem);
                    }
                }
            }
        }
    }

    private DividendItem createItem(Security security, DividendEvent dividendEvent, DateType configuredType,
                    DateType typeToAdd, LocalDate startRange)
    {
        if (configuredType != typeToAdd && configuredType != DateType.ALL_DATES)
            return null;

        var eventDate = typeToAdd.getDate(dividendEvent);
        if (eventDate.isBefore(startRange))
            return null;

        var item = new DividendItem(security, eventDate, dividendEvent);
        item.getTypes().add(typeToAdd);
        return item;
    }

    private List<Security> getSecurities(LocalDate now)
    {
        var security = get(SecuritySelectionConfig.class).getValue();
        var clientFilter = get(ClientFilterConfig.class);

        switch (security)
        {
            case ALL:
                return getClient().getSecurities();
            case ACTIVE:
                return getClient().getSecurities().stream().filter(s -> !s.isRetired()).toList();
            case OWNED:
                var filteredClient = clientFilter.getSelectedFilter().filter(getClient());
                var snapshot = ClientSnapshot.create(filteredClient, getDashboardData().getCurrencyConverter(), now);
                return snapshot.getJointPortfolio().getPositions().stream().map(p -> p.getSecurity()).toList();
            default:
                throw new IllegalArgumentException();
        }
    }

    @Override
    public Composite createControl(Composite parent, DashboardResources resources)
    {
        var composite = super.createControl(parent, resources);
        GridLayoutFactory.fillDefaults().numColumns(1).margins(5, 5).applyTo(composite);
        GridLayoutFactory.fillDefaults().numColumns(1).spacing(0, 2).applyTo(list);
        GridDataFactory.fillDefaults().grab(true, true).applyTo(list);

        return composite;
    }

    @Override
    protected void createTitleArea(Composite container)
    {
        super.createTitleArea(container);

        var composite = new Composite(container, SWT.NONE);
        composite.setLayout(new FormLayout());
        var exDate = createLabel(composite, INDICATOR_EX_DATE + " " + Messages.ColumnExDate); //$NON-NLS-1$
        var payDate = createLabel(composite, INDICATOR_PAY_DATE + " " + Messages.ColumnPaymentDate); //$NON-NLS-1$

        FormDataFactory.startingWith(exDate).thenRight(payDate, 10);
    }

    @Override
    protected Composite createItemControl(Composite parent, DividendItem item, DividendItem previous)
    {
        var security = item.getSecurity();

        if (item.hasNewDate())
        {
            var composite = new Composite(parent, SWT.NONE);
            composite.setLayout(new FormLayout());
            var date = createLabel(composite, Values.Date.format(item.getDate()));
            date.setData(UIConstants.CSS.CLASS_NAME, UIConstants.CSS.HEADING2);
        }

        var composite = new Composite(parent, SWT.NONE);
        composite.setLayout(new FormLayout());

        var image = LogoManager.instance().getDefaultColumnImage(security, getClient().getSettings());
        var logo = createLabel(composite, image);
        var name = createLabel(composite, security.getName());
        var amount = createLabel(composite, buildAmountLabel(item), SWT.RIGHT);

        addListener(mouseUpAdapter, composite, name, amount);

        FormDataFactory.startingWith(logo).thenRight(name).right(new FormAttachment(amount, -5, SWT.LEFT));
        FormDataFactory.startingWith(amount).right(new FormAttachment(100));
        GridDataFactory.fillDefaults().grab(true, false).applyTo(composite);
        return composite;
    }

    private String buildAmountLabel(DividendItem item)
    {
        var label = new StringBuilder();
        if (item.types.contains(DateType.EX_DIVIDEND_DATE))
            label.append(INDICATOR_EX_DATE);
        if (item.types.contains(DateType.PAYMENT_DATE))
            label.append(INDICATOR_PAY_DATE);
        label.append(" "); //$NON-NLS-1$
        label.append(Values.Money.formatAlwaysVisible(item.dividendEvent.getAmount(), getClient().getBaseCurrency()));
        return label.toString();
    }

    @Override
    protected void createEmptyControl(Composite parent)
    {
        // nothing to do
    }
}
