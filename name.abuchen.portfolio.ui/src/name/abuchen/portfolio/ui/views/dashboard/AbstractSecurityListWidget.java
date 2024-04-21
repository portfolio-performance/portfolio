package name.abuchen.portfolio.ui.views.dashboard;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

import name.abuchen.portfolio.model.Dashboard.Widget;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.editor.AbstractFinanceView;
import name.abuchen.portfolio.ui.util.swt.StyledLabel;
import name.abuchen.portfolio.ui.views.SecurityListView;
import name.abuchen.portfolio.util.TextUtil;

public abstract class AbstractSecurityListWidget<T extends AbstractSecurityListWidget.Item>
                extends WidgetDelegate<List<T>>
{
    public static class Item
    {
        private Security security;

        public Item(Security security)
        {
            this.security = security;
        }

        public Security getSecurity()
        {
            return security;
        }

    }

    @Inject
    protected AbstractFinanceView view;

    protected StyledLabel title;
    protected Composite list;

    protected MouseListener mouseUpAdapter = MouseListener.mouseUpAdapter(e -> {
        if (!(e.widget instanceof Control))
            return;

        Object item = e.widget.getData();
        if (item == null)
            item = ((Control) e.widget).getParent().getData();

        if (!(item instanceof Item))
            return;

        view.setInformationPaneInput(((Item) item).getSecurity());
    });

    public AbstractSecurityListWidget(Widget widget, DashboardData data)
    {
        super(widget, data);
    }

    @Override
    public Composite createControl(Composite parent, DashboardResources resources)
    {
        Composite container = new Composite(parent, SWT.NONE);
        container.setData(UIConstants.CSS.CLASS_NAME, this.getContainerCssClassNames());
        container.setBackground(parent.getBackground());
        GridLayoutFactory.fillDefaults().numColumns(1).margins(5, 5).applyTo(container);

        title = new StyledLabel(container, SWT.NONE);
        title.setBackground(container.getBackground());
        title.setText(TextUtil.tooltip(getWidget().getLabel()));
        title.setData(UIConstants.CSS.CLASS_NAME, UIConstants.CSS.TITLE);

        title.setOpenLinkHandler(d -> {
            @SuppressWarnings("unchecked")
            List<Item> data = (List<Item>) list.getData();
            if (data == null)
                return;

            Set<Security> selected = data.stream().map(Item::getSecurity).collect(Collectors.toSet());
            view.getPart().activateView(SecurityListView.class, (Predicate<Security>) selected::contains);
        });

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
        int yHint = get(ChartHeightConfig.class).getPixel();
        GridDataFactory.fillDefaults().hint(SWT.DEFAULT, yHint).grab(true, false).applyTo(list);

        return container;
    }

    @Override
    public void update(List<T> items)
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

        if (items.isEmpty())
            this.title.setText(getWidget().getLabel() + " (0)"); //$NON-NLS-1$
        else
            this.title.setText(String.format("%s (<a href=\"open\">%d</a>)", getWidget().getLabel(), items.size())); //$NON-NLS-1$

        Control[] children = list.getChildren();
        for (Control child : children)
            if (!child.isDisposed())
                child.dispose();

        if (items.isEmpty())
        {
            createEmptyControl(list);
        }
        else
        {
            int count = 0;
            for (T item : items)
            {
                // limit the number of securities listed on the dashboard to 25
                if (count >= 25)
                    break;

                Composite child = createItemControl(list, item);
                child.setData(item);
                count++;
            }
        }

        list.setData(items);
        list.layout(true);
    }

    protected abstract void createEmptyControl(Composite parent);

    protected abstract Composite createItemControl(Composite parent, T item);

    @Override
    public Control getTitleControl()
    {
        return title;
    }

    protected Label createLabel(Composite composite, String text)
    {
        Label ret = new Label(composite, SWT.NONE);
        ret.setText(TextUtil.tooltip(Objects.toString(text, ""))); //$NON-NLS-1$
        return ret;
    }

    protected Label createLabel(Composite composite, Image image)
    {
        Label ret = new Label(composite, SWT.NONE);
        ret.setImage(image);
        return ret;
    }

}
