package name.abuchen.portfolio.ui.views.dashboard;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import javax.inject.Inject;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

import name.abuchen.portfolio.model.Dashboard.Widget;
import name.abuchen.portfolio.model.LimitPrice;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.editor.AbstractFinanceView;
import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.ui.util.FormDataFactory;
import name.abuchen.portfolio.ui.util.swt.ColoredLabel;
import name.abuchen.portfolio.ui.util.swt.StyledLabel;
import name.abuchen.portfolio.ui.views.SecurityListView;
import name.abuchen.portfolio.ui.views.dashboard.LimitExceededWidget.Item;
import name.abuchen.portfolio.util.TextUtil;

public class LimitExceededWidget extends WidgetDelegate<List<Item>>
{
    static class Item
    {
        private Security security;
        private LimitPrice limit;
        private SecurityPrice price;

        public Item(Security security, LimitPrice limit, SecurityPrice price)
        {
            this.security = security;
            this.limit = limit;
            this.price = price;
        }
    }

    @Inject
    private AbstractFinanceView view;

    private StyledLabel title;
    private Composite list;

    public LimitExceededWidget(Widget widget, DashboardData data)
    {
        super(widget, data);

        addConfig(new ChartHeightConfig(this));
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
        title.setOpenLinkHandler(data -> view.getPart().activateView(SecurityListView.class,
                        SecurityListView.LIMIT_PRICE_FILTER_ID));
        GridDataFactory.fillDefaults().grab(true, false).applyTo(title);

        list = new Composite(container, SWT.NONE);
        RowLayout layout = new RowLayout(SWT.VERTICAL);
        layout.marginLeft = 0;
        layout.marginRight = 0;
        layout.marginTop = 0;
        layout.marginBottom = 0;
        layout.spacing = 10;
        layout.wrap = false;
        list.setLayout(layout);
        int yHint = get(ChartHeightConfig.class).getPixel();
        GridDataFactory.fillDefaults().hint(SWT.DEFAULT, yHint).grab(true, false).applyTo(list);

        return container;
    }

    @Override
    public Supplier<List<Item>> getUpdateTask()
    {
        return () -> {

            List<Item> items = new ArrayList<>();

            for (Security security : getClient().getSecurities())
            {
                for (Object attribute : security.getAttributes().getMap().values())
                {
                    if (!(attribute instanceof LimitPrice))
                        continue;

                    LimitPrice limit = (LimitPrice) attribute;

                    SecurityPrice latest = security.getSecurityPrice(LocalDate.now());
                    if (latest != null && limit.isExceeded(latest))
                    {
                        items.add(new Item(security, limit, latest));
                    }
                }
            }

            return items;
        };
    }

    @Override
    public void update(List<Item> items)
    {
        GridData data = (GridData) list.getLayoutData();

        int oldHeight = data.heightHint;
        int newHeight = get(ChartHeightConfig.class).getPixel();

        if (oldHeight != newHeight)
        {
            data.heightHint = newHeight;
            title.getParent().layout(true);
            title.getParent().getParent().layout(true);
        }

        this.title.setText(String.format("%s (<a href=\"open\">%d</a>)", getWidget().getLabel(), items.size())); //$NON-NLS-1$

        Control[] children = list.getChildren();
        for (Control child : children)
            if (!child.isDisposed())
                child.dispose();

        for (Item item : items)
        {
            Composite composite = new Composite(list, SWT.NONE);
            composite.setLayout(new FormLayout());

            Label name = new Label(composite, SWT.NONE);
            name.setText(item.security.getName());

            ColoredLabel price = new ColoredLabel(composite, SWT.RIGHT);
            price.setBackdropColor(item.limit.getRelationalOperator().isGreater() ? Colors.theme().greenBackground()
                            : Colors.theme().redBackground());
            price.setText(Values.Quote.format(item.security.getCurrencyCode(), item.price.getValue()));

            Label limit = new Label(composite, SWT.NONE);
            limit.setText(item.limit.toString());

            FormDataFactory.startingWith(name).left(new FormAttachment(0)).right(new FormAttachment(100))
                            .thenBelow(price).thenRight(limit);

            MouseListener mouseUpAdapter = MouseListener
                            .mouseUpAdapter(e -> view.setInformationPaneInput(item.security));
            composite.addMouseListener(mouseUpAdapter);
            name.addMouseListener(mouseUpAdapter);
            limit.addMouseListener(mouseUpAdapter);
            price.addMouseListener(mouseUpAdapter);
        }

        list.layout(true);
    }

    @Override
    public Control getTitleControl()
    {
        return title;
    }

}
