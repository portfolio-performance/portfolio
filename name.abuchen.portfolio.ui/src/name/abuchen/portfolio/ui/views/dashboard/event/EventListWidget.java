package name.abuchen.portfolio.ui.views.dashboard.event;

import java.time.LocalDate;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

import javax.inject.Inject;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Dashboard.Widget;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.ReportingPeriod;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.editor.AbstractFinanceView;
import name.abuchen.portfolio.ui.util.FormDataFactory;
import name.abuchen.portfolio.ui.util.swt.StyledLabel;
import name.abuchen.portfolio.ui.views.AccountListView;
import name.abuchen.portfolio.ui.views.SecurityListView;
import name.abuchen.portfolio.ui.views.dashboard.DashboardData;
import name.abuchen.portfolio.ui.views.dashboard.DashboardResources;
import name.abuchen.portfolio.ui.views.dashboard.ReportingPeriodConfig;
import name.abuchen.portfolio.ui.views.dashboard.WidgetDelegate;
import name.abuchen.portfolio.ui.views.dashboard.event.item.AccountEventItem;
import name.abuchen.portfolio.ui.views.dashboard.event.item.EventItem;
import name.abuchen.portfolio.ui.views.dashboard.event.item.EventType;
import name.abuchen.portfolio.ui.views.dashboard.event.item.HolidayEventItem;
import name.abuchen.portfolio.ui.views.dashboard.event.item.SecurityEventItem;
import name.abuchen.portfolio.util.Interval;
import name.abuchen.portfolio.util.TextUtil;

public final class EventListWidget extends WidgetDelegate<List<EventItem>>
{

    @Inject
    private AbstractFinanceView view;
    private StyledLabel title;
    private Composite list;

    public EventListWidget(Widget widget, DashboardData data)
    {
        super(widget, data);
        
        addConfig(new ReportingPeriodConfig(this));
    }

    @Override
    public Control getTitleControl()
    {
        return title;
    }
    
    @Override
    public Composite createControl(Composite parent, DashboardResources resources)
    {
        Composite container = new Composite(parent, SWT.NONE);
        container.setBackground(parent.getBackground());
        GridLayoutFactory.fillDefaults().numColumns(1).margins(5, 5).applyTo(container);

        title = new StyledLabel(container, SWT.NONE);
        title.setBackground(container.getBackground());
        title.setText(TextUtil.tooltip(getWidget().getLabel()));
        GridDataFactory.fillDefaults().grab(true, false).applyTo(title);

        list = new Composite(container, SWT.NONE);
        RowLayout layout = new RowLayout(SWT.VERTICAL);
        layout.marginLeft = 0;
        layout.marginRight = 0;
        layout.marginTop = 0;
        layout.marginBottom = 0;
        layout.spacing = 10;
        layout.wrap = false;
        layout.fill = true;
        list.setLayout(layout);
        
        return container;
    }
    
    @Override
    public Supplier<List<EventItem>> getUpdateTask()
    {
        ReportingPeriod reportingPeriod = get(ReportingPeriodConfig.class).getReportingPeriod();
        Interval interval = reportingPeriod.toInterval(LocalDate.now());
        return new EventItemSupplier(getClient(), new EventItemFactory(interval));
    }
    
    @Override
    public void update(List<EventItem> items)
    {
        Control[] children = list.getChildren();
        for (Control child : children)
            if (!child.isDisposed())
                child.dispose();

        String linkedTitle = String.format("%s (%d)", getWidget().getLabel(), items.size()); //$NON-NLS-1$
        title.setText(linkedTitle);
        
        if (items.isEmpty())
            addEmptyListing(list);
        else
            addListing(items);

        list.setData(items);
        list.requestLayout();
    }

    private void addEmptyListing(Composite parent)
    {
        StyledLabel noContent = new StyledLabel(parent, SWT.WRAP);
        noContent.setText(Messages.MsgHintNoEvents);
    }

    private void addListing(List<EventItem> items)
    {
        for (EventItem item : items)
        {
            Composite child = createItemControl(list, item);
            child.setData(item);
        }
    }
    
    private Composite createItemControl(Composite parent, EventItem eventItem)
    {
        Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayout(new FormLayout());

        Label logo = new Label(composite, SWT.NONE);
        logo.setImage(eventItem.getType().getIcon());
        
        Label type = new Label(composite, SWT.NONE);
        type.setText(eventItem.getType().toString());

        Label date = new Label(composite, SWT.NONE);
        date.setText(Values.Date.format(eventItem.getDate()));
        
        Composite content = createContent(eventItem, composite);

        FormDataFactory.startingWith(logo).thenRight(type).right(new FormAttachment(100))
                .thenBelow(date)
                .thenBelow(content);

        return composite;
    }

    private Composite createContent(EventItem eventItem, Composite parent)
    {
        RowLayout layout = new RowLayout(SWT.VERTICAL);
        layout.marginLeft = 0;
        layout.marginRight = 0;
        layout.marginTop = 0;
        layout.marginBottom = 0;
        layout.spacing = 2;
        layout.wrap = false;
        layout.fill = true;
        
        Composite container = new Composite(parent, SWT.NONE);
        container.setLayout(layout);
        
        StyledLabel content = new StyledLabel(container, SWT.NONE);
        if (eventItem instanceof SecurityEventItem) 
        {
            SecurityEventItem securityEventItem = (SecurityEventItem) eventItem;
            Security security = securityEventItem.getSecurity();
            String linkedName = String.format("<a href=\"open\">%s</a>", security.getName()); //$NON-NLS-1$
            content.setText(linkedName);
            content.setOpenLinkHandler(
                    d -> view.getPart().activateView(SecurityListView.class, (Predicate<?>) security::equals));
            
            if (eventItem.getType() == EventType.NOTE || eventItem.getType() == EventType.PAYMENT)
            {
                addMessage(container, securityEventItem.getMessage());
            }
        } 
        else if (eventItem instanceof AccountEventItem)
        {
            AccountEventItem accountEventItem = (AccountEventItem) eventItem;
            Account account = accountEventItem.getAccount();
            String linkedName = String.format("<a href=\"open\">%s</a>", account.getName()); //$NON-NLS-1$
            content.setText(linkedName);
            content.setOpenLinkHandler(
                    d -> view.getPart().activateView(AccountListView.class, (Predicate<?>) account::equals));
            addMessage(container, accountEventItem.getMessage());
        }
        else if (eventItem instanceof HolidayEventItem)
        {
            HolidayEventItem holidayEventItem = (HolidayEventItem) eventItem;
            content.setText(holidayEventItem.getHoliday().getLabel());
        }
        
        return container;
    }

    private void addMessage(Composite container, String message)
    {
        if (message != null) 
        {
            StyledLabel messageLabel = new StyledLabel(container, SWT.NONE);
            String styledMessage = String.format("<em>\"%s\"</em>", message); //$NON-NLS-1$
            messageLabel.setText(styledMessage);
        }
    }

}
