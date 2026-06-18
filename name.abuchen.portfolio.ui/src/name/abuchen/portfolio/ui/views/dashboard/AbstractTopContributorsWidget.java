package name.abuchen.portfolio.ui.views.dashboard;

import java.util.ArrayList;
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
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

import name.abuchen.portfolio.model.Dashboard.Widget;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.editor.AbstractFinanceView;
import name.abuchen.portfolio.ui.util.FormDataFactory;
import name.abuchen.portfolio.ui.util.LogoManager;
import name.abuchen.portfolio.ui.util.ValueColorScheme;
import name.abuchen.portfolio.ui.util.swt.StyledLabel;
import name.abuchen.portfolio.ui.views.SecurityListView;
import name.abuchen.portfolio.util.TextUtil;

public abstract class AbstractTopContributorsWidget<D> extends WidgetDelegate<D>
{
    record DisplayRow(Security security, String label, String valueText, boolean isPositive)
    {
    }

    @Inject
    protected AbstractFinanceView view;

    private StyledLabel title;
    private Composite list;

    protected MouseListener mouseUpAdapter = MouseListener.mouseUpAdapter(e -> {
        if (!(e.widget instanceof Control))
            return;

        Object item = e.widget.getData();
        if (item == null)
            item = ((Control) e.widget).getParent().getData();

        if (!(item instanceof DisplayRow row))
            return;

        view.setInformationPaneInput(row.security());
        view.showPane();
    });

    protected AbstractTopContributorsWidget(Widget widget, DashboardData dashboardData)
    {
        super(widget, dashboardData);
    }

    @Override
    public Composite createControl(Composite parent, DashboardResources resources)
    {
        var container = new Composite(parent, SWT.NONE);
        container.setData(UIConstants.CSS.CLASS_NAME, this.getContainerCssClassNames());
        container.setBackground(parent.getBackground());
        GridLayoutFactory.fillDefaults().numColumns(1).margins(5, 5).applyTo(container);

        createTitleArea(container);

        list = new Composite(container, SWT.NONE);
        GridLayoutFactory.fillDefaults().numColumns(1).spacing(0, 2).applyTo(list);
        GridDataFactory.fillDefaults().grab(true, false).applyTo(list);

        return container;
    }

    private void createTitleArea(Composite container)
    {
        title = new StyledLabel(container, SWT.NONE);
        title.setBackground(container.getBackground());
        title.setText(TextUtil.tooltip(getWidget().getLabel()));
        title.setData(UIConstants.CSS.CLASS_NAME, UIConstants.CSS.TITLE);

        title.setOpenLinkHandler(d -> {
            @SuppressWarnings("unchecked")
            var data = (List<DisplayRow>) list.getData();
            if (data == null)
                return;

            Set<Security> selected = data.stream().map(DisplayRow::security).filter(Objects::nonNull)
                            .collect(Collectors.toSet());

            if (!selected.isEmpty())
                view.getPart().activateView(SecurityListView.class, (Predicate<Security>) selected::contains);
        });

        GridDataFactory.fillDefaults().grab(true, false).applyTo(title);
    }

    @Override
    public Control getTitleControl()
    {
        return title;
    }

    /**
     * Returns the rows to display, sorted in descending order (best first).
     * Implementations must sort their domain records before mapping to
     * {@link DisplayRow}.
     */
    protected abstract List<DisplayRow> buildDisplayRows(D data);

    @Override
    public void update(D data)
    {
        int count = get(CountConfig.class).getCount();

        var rows = buildDisplayRows(data);

        var top = rows.subList(0, Math.min(rows.size(), count));
        var bottom = rows.subList(Math.max(0, Math.max(rows.size() - count, top.size())), rows.size());
        boolean showDivider = rows.size() > 2 * count;

        var visibleRows = new ArrayList<DisplayRow>();
        visibleRows.addAll(top);
        visibleRows.addAll(bottom);

        long securityCount = visibleRows.stream().map(DisplayRow::security).filter(Objects::nonNull).distinct().count();

        if (securityCount == 0)
            title.setText(getWidget().getLabel() + " (0)"); //$NON-NLS-1$
        else
            title.setText(String.format("%s (<a href=\"open\">%d</a>)", getWidget().getLabel(), securityCount)); //$NON-NLS-1$

        Control[] children = list.getChildren();
        for (Control child : children)
            if (!child.isDisposed())
                child.dispose();

        for (var row : top)
        {
            var child = createRow(list, row);
            child.setData(row);
        }

        if (showDivider)
            GridDataFactory.fillDefaults().grab(true, false).applyTo(new Label(list, SWT.SEPARATOR | SWT.HORIZONTAL));

        for (var row : bottom)
        {
            var child = createRow(list, row);
            child.setData(row);
        }

        list.setData(visibleRows);
        list.layout(true);
        list.getParent().layout(true);
        list.getParent().getParent().layout();
    }

    private Composite createRow(Composite parent, DisplayRow row)
    {
        var security = row.security();

        var composite = new Composite(parent, SWT.NONE);
        composite.setLayout(new FormLayout());

        Label logo = null;
        if (security != null)
        {
            logo = new Label(composite, SWT.NONE);
            logo.setImage(LogoManager.instance().getDefaultColumnImage(security, getClient().getSettings()));
        }

        var name = new StyledLabel(composite, SWT.NONE);
        name.setText(row.label());

        var value = new Label(composite, SWT.RIGHT);
        value.setText(row.valueText());
        value.setForeground(row.isPositive() ? ValueColorScheme.current().positiveForeground()
                        : ValueColorScheme.current().negativeForeground());

        addListener(mouseUpAdapter, composite, name, value);

        if (logo != null)
        {
            FormDataFactory.startingWith(logo).thenRight(name).right(new FormAttachment(value, -5, SWT.LEFT));
            FormDataFactory.startingWith(value).right(new FormAttachment(100));
        }
        else
        {
            FormDataFactory.startingWith(name)
                            .left(new FormAttachment(0,
                                            16 /* logo */ + 5 /* space */))
                            .right(new FormAttachment(value, -5, SWT.LEFT));
            FormDataFactory.startingWith(value).right(new FormAttachment(100));
        }
        GridDataFactory.fillDefaults().grab(true, false).applyTo(composite);
        return composite;
    }

    protected void addListener(MouseListener listener, Control... controls)
    {
        for (Control ctr : controls)
        {
            if (ctr != null)
            {
                ctr.addMouseListener(listener);
            }
        }
    }
}
