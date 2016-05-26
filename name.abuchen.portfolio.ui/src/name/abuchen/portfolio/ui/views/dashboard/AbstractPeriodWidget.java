package name.abuchen.portfolio.ui.views.dashboard;

import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import name.abuchen.portfolio.model.Dashboard.Widget;
import name.abuchen.portfolio.snapshot.ReportingPeriod;
import name.abuchen.portfolio.ui.util.ContextMenu;

public abstract class AbstractPeriodWidget extends WidgetDelegate
{
    private ReportingPeriodConfig reportingPeriod;

    protected Label title;
    protected Label indicator;

    public AbstractPeriodWidget(Widget widget, DashboardData dashboardData)
    {
        super(widget, dashboardData);

        reportingPeriod = new ReportingPeriodConfig(widget);
    }

    protected ReportingPeriod getReportingPeriod()
    {
        return reportingPeriod.getReportingPeriod();
    }

    @Override
    public Composite createControl(Composite parent, DashboardResources resources)
    {
        Composite container = new Composite(parent, SWT.NONE);
        container.setBackground(parent.getBackground());
        GridLayoutFactory.fillDefaults().numColumns(1).margins(5, 5).applyTo(container);
    
        title = new Label(container, SWT.NONE);
        title.setText(getWidget().getLabel());
        GridDataFactory.fillDefaults().grab(true, false).applyTo(title);
    
        indicator = new Label(container, SWT.NONE);
        indicator.setFont(resources.getKpiFont());
        indicator.setText(""); //$NON-NLS-1$
        GridDataFactory.fillDefaults().grab(true, false).applyTo(indicator);
    
        return container;
    }

    @Override
    public void attachContextMenu(IMenuListener listener)
    {
        new ContextMenu(title, listener).hook();
    }
}
