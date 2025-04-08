package name.abuchen.portfolio.ui.views.dashboard.lists;

import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
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
import name.abuchen.portfolio.ui.views.dashboard.AttributesConfig;
import name.abuchen.portfolio.ui.views.dashboard.ChartHeightConfig;
import name.abuchen.portfolio.ui.views.dashboard.DashboardData;
import name.abuchen.portfolio.ui.views.dashboard.EnumBasedConfig;
import name.abuchen.portfolio.ui.views.dashboard.WidgetDelegate;
import name.abuchen.portfolio.ui.views.settings.AttributeFieldType;
import name.abuchen.portfolio.ui.views.settings.SettingsView;

public class FollowUpWidget extends AbstractSecurityListWidget<FollowUpWidget.FollowUpItem>
{
    public static class FollowUpItem extends AbstractSecurityListWidget.Item
    {
        private AttributeType type;
        LocalDate date;

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

            var comparator = get(SortingConfig.class).getValue().getComparator();
            Collections.sort(items, (r, l) -> {
                int res = comparator.compare(r.date, l.date);
                // If date is the same, we want to group items by attribute type
                // (by simply sorting them by it).
                if (res == 0)
                    res = r.type.getName().compareTo(l.type.getName());
                return res;
            });

            return items;
        };
    }

    @Override
    protected Composite createItemControl(Composite parent, FollowUpItem item, FollowUpItem previous)
    {
        Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayout(new FormLayout());

        Label logo = createLabel(composite,
                        LogoManager.instance().getDefaultColumnImage(item.getSecurity(), getClient().getSettings()));

        Label name = createLabel(composite, item.getSecurity().getName(getClient().getSecurityNameConfig()));

        composite.addMouseListener(mouseUpAdapter);
        name.addMouseListener(mouseUpAdapter);

        if (previous == null || !item.date.equals(previous.date) || !item.type.equals(previous.type))
        {
            Label date = createLabel(composite, item.type.getName() + ": " + Values.Date.format(item.date)); //$NON-NLS-1$
            FormDataFactory.startingWith(date).thenBelow(logo).thenRight(name).right(new FormAttachment(100));
        }
        else
        {
            FormDataFactory.startingWith(logo).thenRight(name).right(new FormAttachment(100));
        }

        return composite;
    }

    @Override
    protected void createEmptyControl(Composite parent)
    {
        if (get(AttributesConfig.class).hasTypes())
            return;

        var label = new StyledLabel(parent, SWT.WRAP);
        label.setText(MessageFormat.format(Messages.MsgHintNoAttributesConfigured, AttributeFieldType.DATE.toString()));
        label.setOpenLinkHandler(d -> view.getPart().activateView(SettingsView.class, 1));
    }

}
