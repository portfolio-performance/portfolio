package name.abuchen.portfolio.ui.views.dashboard;

import java.io.IOException;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import name.abuchen.portfolio.model.Dashboard.Widget;
import name.abuchen.portfolio.snapshot.ReportingPeriod;
import name.abuchen.portfolio.ui.PortfolioPlugin;

public abstract class AbstractPeriodWidget implements WidgetDelegate
{
    private static final String CONFIG_PERIOD = "period"; //$NON-NLS-1$

    private final Widget widget;
    private ReportingPeriod reportingPeriod;

    protected Label indicator;

    public AbstractPeriodWidget(Widget widget)
    {
        this.widget = widget;

        String config = widget.getConfiguration().get(CONFIG_PERIOD);
        if (config == null || config.isEmpty())
            config = "L1Y0"; //$NON-NLS-1$

        try
        {
            this.reportingPeriod = ReportingPeriod.from(config);
        }
        catch (IOException e)
        {
            PortfolioPlugin.log(e);
            this.reportingPeriod = new ReportingPeriod.LastX(1, 0);
        }
    }

    public ReportingPeriod getReportingPeriod()
    {
        return reportingPeriod;
    }

    @Override
    public Composite createControl(Composite parent, DashboardResources resources)
    {
        Composite container = new Composite(parent, SWT.NONE);
        container.setBackground(parent.getBackground());
        GridLayoutFactory.fillDefaults().numColumns(1).margins(5, 5).applyTo(container);
    
        Label lbl = new Label(container, SWT.NONE);
        lbl.setText(widget.getLabel());
        GridDataFactory.fillDefaults().grab(true, false).applyTo(lbl);
    
        indicator = new Label(container, SWT.NONE);
        indicator.setFont(resources.getKpiFont());
        indicator.setText(""); //$NON-NLS-1$
        GridDataFactory.fillDefaults().grab(true, false).applyTo(indicator);
    
        return container;
    }
}
