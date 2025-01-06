package name.abuchen.portfolio.ui.views.dashboard.lists;

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

import name.abuchen.portfolio.model.Dashboard;
import name.abuchen.portfolio.model.Dashboard.Widget;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityEvent;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.FormDataFactory;
import name.abuchen.portfolio.ui.util.LogoManager;
import name.abuchen.portfolio.ui.util.swt.StyledLabel;
import name.abuchen.portfolio.ui.views.dashboard.ChartHeightConfig;
import name.abuchen.portfolio.ui.views.dashboard.DashboardData;
import name.abuchen.portfolio.ui.views.dashboard.EnumBasedConfig;
import name.abuchen.portfolio.ui.views.dashboard.WidgetDelegate;

public class EventListWidget extends AbstractSecurityListWidget<EventListWidget.EventItem>
{
    public static class EventItem extends AbstractSecurityListWidget.Item
    {
        private SecurityEvent event;

        public EventItem(Security security, SecurityEvent event)
        {
            super(security);
            this.event = event;
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

    public EventListWidget(Widget widget, DashboardData data)
    {
        super(widget, data);

        addConfig(new DateDateConfig(this));
        addConfig(new SortingConfig(this));
        addConfig(new ChartHeightConfig(this));
    }

    @Override
    public Supplier<List<EventItem>> getUpdateTask()
    {
        return () -> {

            DateCheck dateType = get(DateDateConfig.class).getValue();

            List<EventItem> items = new ArrayList<>();
            for (Security security : getClient().getSecurities())
            {
                for (var event : security.getEvents())
                {
                    if (dateType.include(event.getDate()))
                    {
                        items.add(new EventItem(security, event));
                    }
                }
            }

            var comparator = get(SortingConfig.class).getValue().getComparator();
            Collections.sort(items, (r, l) -> comparator.compare(r.event.getDate(), l.event.getDate()));

            return items;
        };
    }

    @Override
    protected Composite createItemControl(Composite parent, EventItem item)
    {
        Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayout(new FormLayout());

        Label logo = createLabel(composite,
                        LogoManager.instance().getDefaultColumnImage(item.getSecurity(), getClient().getSettings()));

        Label name = createLabel(composite, item.getSecurity().getName());

        Label date = createLabel(composite, Values.Date.format(item.event.getDate()) + ": " + item.event.getDetails()); //$NON-NLS-1$

        composite.addMouseListener(mouseUpAdapter);
        name.addMouseListener(mouseUpAdapter);
        date.addMouseListener(mouseUpAdapter);

        FormDataFactory.startingWith(logo).thenRight(name).right(new FormAttachment(100)).thenBelow(date);

        return composite;
    }

    @Override
    protected void createEmptyControl(Composite parent)
    {
        title = new StyledLabel(parent, SWT.WRAP);
        title.setText(Messages.MsgHintNoEvents);
    }

}
