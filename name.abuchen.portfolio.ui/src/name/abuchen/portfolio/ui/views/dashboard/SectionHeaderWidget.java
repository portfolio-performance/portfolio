package name.abuchen.portfolio.ui.views.dashboard;

import java.util.function.Supplier;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.events.IHyperlinkListener;
import org.eclipse.ui.forms.widgets.Hyperlink;

import name.abuchen.portfolio.model.Dashboard;
import name.abuchen.portfolio.model.Dashboard.Widget;
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.util.TextUtil;

/**
 * A dashboard widget that acts as a collapsible section header. All widgets
 * below this header (within the same column) are hidden when the section is
 * collapsed. The collapsed state is persisted in the widget configuration map.
 */
public class SectionHeaderWidget extends WidgetDelegate<Object>
{
    private Label title;
    private Hyperlink toggleButton;
    private Composite container;

    public SectionHeaderWidget(Widget widget, DashboardData data)
    {
        super(widget, data);
    }

    public boolean isCollapsed()
    {
        return Boolean.parseBoolean(getWidget().getConfiguration().get(Dashboard.Config.COLLAPSED.name()));
    }

    private void setCollapsed(boolean collapsed)
    {
        getWidget().getConfiguration().put(Dashboard.Config.COLLAPSED.name(), String.valueOf(collapsed));
    }

    @Override
    public Composite createControl(Composite parent, DashboardResources resources)
    {
        container = new Composite(parent, SWT.NONE);
        container.setBackground(Colors.theme().defaultBackground());
        container.setData(UIConstants.CSS.CLASS_NAME, this.getContainerCssClassNames());
        GridLayoutFactory.fillDefaults().numColumns(2).margins(5, 8).spacing(4, 0).applyTo(container);

        toggleButton = new Hyperlink(container, SWT.NONE);
        toggleButton.setText(isCollapsed() ? "\u25B6" : "\u25BC"); //$NON-NLS-1$ //$NON-NLS-2$
        toggleButton.setUnderlined(false);
        toggleButton.setForeground(Colors.HEADINGS);
        toggleButton.setBackground(container.getBackground());
        GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).applyTo(toggleButton);

        title = new Label(container, SWT.NONE);
        title.setData(UIConstants.CSS.CLASS_NAME, UIConstants.CSS.HEADING1);
        title.setForeground(Colors.HEADINGS);
        title.setBackground(container.getBackground());
        title.setText(TextUtil.tooltip(getWidget().getLabel()));
        GridDataFactory.fillDefaults().grab(true, false).applyTo(title);

        Label separator = new Label(container, SWT.SEPARATOR | SWT.HORIZONTAL);
        GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(separator);

        toggleButton.addHyperlinkListener(IHyperlinkListener.linkActivatedAdapter(e -> onToggle()));

        return container;
    }

    private void onToggle()
    {
        Composite dashboardContainer = findDashboardContainer(container);
        if (dashboardContainer == null)
            return;

        boolean newCollapsed = !isCollapsed();
        String thisLabel = getWidget().getLabel();

        // Sync all section headers with the same label across all columns
        for (Control column : dashboardContainer.getChildren())
        {
            if (!(column instanceof Composite columnComposite))
                continue;
            for (Control child : columnComposite.getChildren())
            {
                if (!(child instanceof Composite))
                    continue;
                Object delegate = child.getData(DashboardView.DELEGATE_KEY);
                if (delegate instanceof SectionHeaderWidget other && thisLabel.equals(other.getWidget().getLabel()))
                {
                    other.setCollapsed(newCollapsed);
                    other.update(null);
                    other.getClient().touch();
                    applySectionVisibility(columnComposite);
                }
            }
        }

        // Update the scrolled composite so the full content is reachable
        Object view = dashboardContainer.getData(DashboardView.VIEW_KEY);
        if (view instanceof DashboardView dashboardView)
            dashboardView.updateScrolledCompositeMinSize();
    }

    @Override
    public Control getTitleControl()
    {
        return title;
    }

    @Override
    public Supplier<Object> getUpdateTask()
    {
        return () -> null;
    }

    @Override
    public void update(Object data)
    {
        if (title != null && !title.isDisposed())
            title.setText(TextUtil.tooltip(getWidget().getLabel()));
        if (toggleButton != null && !toggleButton.isDisposed())
            toggleButton.setText(isCollapsed() ? "\u25B6" : "\u25BC"); //$NON-NLS-1$ //$NON-NLS-2$
    }

    /**
     * Walks all widget composites in the column and shows/hides them according
     * to the collapsed state of any preceding SectionHeaderWidget. Widgets
     * before the first section header are always visible.
     */
    static void applySectionVisibility(Composite columnControl)
    {
        boolean collapsed = false;
        var filler = columnControl.getData(DashboardView.FILLER_KEY);

        for (Control child : columnControl.getChildren())
        {
            if (!(child instanceof Composite))
                continue;

            // The filler composite at the column bottom must always stay
            // visible – it is the drop target for the column context menu
            if (child == filler)
            {
                setVisible(child, true);
                continue;
            }

            var delegate = child.getData(DashboardView.DELEGATE_KEY);

            if (delegate instanceof SectionHeaderWidget sectionWidget)
            {
                collapsed = sectionWidget.isCollapsed();
                setVisible(child, true); // header itself is always visible
            }
            else
            {
                setVisible(child, !collapsed);
            }
        }

        columnControl.layout(true);
        if (columnControl.getParent() != null)
            columnControl.getParent().layout(true);
    }

    private static void setVisible(Control control, boolean visible)
    {
        if (control.getVisible() == visible)
            return;

        Object layoutData = control.getLayoutData();
        if (layoutData instanceof GridData gd)
            gd.exclude = !visible;

        control.setVisible(visible);
    }

    private static Composite findDashboardContainer(Control control)
    {
        Composite c = control.getParent();
        while (c != null && c.getData(DashboardView.VIEW_KEY) == null)
            c = c.getParent();
        return c;
    }
}
