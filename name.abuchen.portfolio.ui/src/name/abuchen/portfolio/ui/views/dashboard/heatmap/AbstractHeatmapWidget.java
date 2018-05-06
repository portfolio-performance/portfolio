package name.abuchen.portfolio.ui.views.dashboard.heatmap;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

import name.abuchen.portfolio.model.Dashboard.Widget;
import name.abuchen.portfolio.ui.views.dashboard.DashboardData;
import name.abuchen.portfolio.ui.views.dashboard.DashboardResources;
import name.abuchen.portfolio.ui.views.dashboard.ReportingPeriodConfig;
import name.abuchen.portfolio.ui.views.dashboard.WidgetDelegate;

public abstract class AbstractHeatmapWidget extends WidgetDelegate
{
    private Composite table;
    private Label title;
    private DashboardResources resources;

    public AbstractHeatmapWidget(Widget widget, DashboardData data)
    {
        super(widget, data);

        addConfig(new ReportingPeriodConfig(this));
        addConfig(new ColorSchemaConfig(this));
    }

    @Override
    public Composite createControl(Composite parent, DashboardResources resources)
    {
        this.resources = resources;

        Composite container = new Composite(parent, SWT.NONE);
        GridLayoutFactory.fillDefaults().numColumns(1).margins(5, 5).applyTo(container);
        container.setBackground(parent.getBackground());

        title = new Label(container, SWT.NONE);
        title.setText(getWidget().getLabel() != null ? getWidget().getLabel() : ""); //$NON-NLS-1$
        GridDataFactory.fillDefaults().grab(true, false).applyTo(title);

        table = new Composite(container, SWT.NONE);
        // 13 columns, one for the legend and 12 for the months
        GridDataFactory.fillDefaults().grab(true, false).applyTo(table);
        table.setBackground(container.getBackground());

        fillTable(table, resources);

        return container;
    }

    protected abstract void fillTable(Composite table, DashboardResources resources);

    @Override
    public void update()
    {
        title.setText(getWidget().getLabel() != null ? getWidget().getLabel() : ""); //$NON-NLS-1$

        for (Control child : table.getChildren())
            child.dispose();

        fillTable(table, resources);

        table.getParent().layout(true);
        table.getParent().getParent().layout(true);
    }

    @Override
    public Control getTitleControl()
    {
        return title;
    }
}
