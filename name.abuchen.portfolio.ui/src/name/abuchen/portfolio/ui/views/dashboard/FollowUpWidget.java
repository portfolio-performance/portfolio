package name.abuchen.portfolio.ui.views.dashboard;

import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import name.abuchen.portfolio.model.AttributeType;
import name.abuchen.portfolio.model.Dashboard;
import name.abuchen.portfolio.model.Dashboard.Widget;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.FormDataFactory;
import name.abuchen.portfolio.ui.util.LogoManager;
import name.abuchen.portfolio.ui.util.swt.StyledLabel;
import name.abuchen.portfolio.ui.views.settings.AttributeFieldType;
import name.abuchen.portfolio.ui.views.settings.SettingsView;

public class FollowUpWidget extends AbstractSecurityListWidget<FollowUpWidget.FollowUpItem>
{
    public static class FollowUpItem extends AbstractSecurityListWidget.Item
    {
        private AttributeType type;
        private LocalDate date;

        public FollowUpItem(Security security, AttributeType type, LocalDate date)
        {
            super(security);
            this.type = type;
            this.date = date;
        }
    }

    public enum DateCheck
    {
        PAST(Messages.OptionDateIsInThePast, date -> !LocalDate.now().isBefore(date)), //
        FUTURE(Messages.OptionDateIsInTheFuture, date -> !date.isBefore(LocalDate.now()));

        private String label;
        private Predicate<LocalDate> predicate;

        private DateCheck(String label, Predicate<LocalDate> predicate)
        {
            this.label = label;
            this.predicate = predicate;
        }

        public boolean include(LocalDate date)
        {
            return predicate.test(date);
        }

        @Override
        public String toString()
        {
            return label;
        }
    }

    static class DateDateConfig extends EnumBasedConfig<DateCheck>
    {
        public DateDateConfig(WidgetDelegate<?> delegate)
        {
            super(delegate, Messages.ColumnDate, DateCheck.class, Dashboard.Config.REPORTING_PERIOD,
                            Policy.EXACTLY_ONE);
        }
    }

    public enum SortDirection
    {
        ASCENDING(Messages.FollowUpWidget_Option_SortingByDateAscending, (r, l) -> r.date.compareTo(l.date)), //
        DESCENDING(Messages.FollowUpWidget_Option_SortingByDateDescending, (r, l) -> l.date.compareTo(r.date));

        private Comparator<FollowUpItem> comparator;
        private String label;

        private SortDirection(String label, Comparator<FollowUpItem> comparator)
        {
            this.label = label;
            this.comparator = comparator;
        }

        Comparator<FollowUpItem> getComparator()
        {
            return comparator;
        }

        @Override
        public String toString()
        {
            return label;
        }
    }

    static class SortingConfig extends EnumBasedConfig<SortDirection>
    {
        public SortingConfig(WidgetDelegate<?> delegate)
        {
            super(delegate, Messages.FollowUpWidget_Option_Sorting, SortDirection.class,
                            Dashboard.Config.SORT_DIRECTION, Policy.EXACTLY_ONE);
        }
    }

    public FollowUpWidget(Widget widget, DashboardData data)
    {
        super(widget, data);

        addConfig(new AttributesConfig(this, t -> t.getTarget() == Security.class && t.getType() == LocalDate.class));
        addConfig(new DateDateConfig(this));
        addConfig(new SortingConfig(this));
        addConfig(new ChartHeightConfig(this));
    }

    @Override
    public Supplier<List<FollowUpItem>> getUpdateTask()
    {
        return () -> {

            DateCheck dateType = get(DateDateConfig.class).getValue();
            List<AttributeType> types = get(AttributesConfig.class).getTypes();

            List<FollowUpItem> items = new ArrayList<>();
            for (Security security : getClient().getSecurities())
            {
                for (AttributeType t : types)
                {
                    Object attribute = security.getAttributes().get(t);
                    if (!(attribute instanceof LocalDate))
                        continue;

                    if (dateType.include((LocalDate) attribute))
                    {
                        items.add(new FollowUpItem(security, t, (LocalDate) attribute));
                    }
                }
            }

            Collections.sort(items, get(SortingConfig.class).getValue().getComparator());

            return items;
        };
    }

    @Override
    protected Composite createItemControl(Composite parent, FollowUpItem item)
    {
        Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayout(new FormLayout());

        Label logo = createLabel(composite,
                        LogoManager.instance().getDefaultColumnImage(item.getSecurity(), getClient().getSettings()));

        Label name = createLabel(composite, item.getSecurity().getName());

        Label date = createLabel(composite, item.type.getName() + ": " + Values.Date.format(item.date)); //$NON-NLS-1$

        composite.addMouseListener(mouseUpAdapter);
        name.addMouseListener(mouseUpAdapter);
        date.addMouseListener(mouseUpAdapter);

        FormDataFactory.startingWith(logo).thenRight(name).right(new FormAttachment(100)).thenBelow(date);

        return composite;
    }

    @Override
    protected void createEmptyControl(Composite parent)
    {
        if (get(AttributesConfig.class).hasTypes())
            return;

        title = new StyledLabel(parent, SWT.WRAP);
        title.setText(MessageFormat.format(Messages.MsgHintNoAttributesConfigured, AttributeFieldType.DATE.toString()));
        title.setOpenLinkHandler(d -> view.getPart().activateView(SettingsView.class, 1));
    }

}
