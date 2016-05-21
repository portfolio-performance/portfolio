package name.abuchen.portfolio.ui.views.dashboard;

import java.io.IOException;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import name.abuchen.portfolio.model.Dashboard.Widget;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.ClientPerformanceSnapshot;
import name.abuchen.portfolio.snapshot.ReportingPeriod;
import name.abuchen.portfolio.ui.PortfolioPlugin;

public class PerformanceCalculationWidget implements WidgetDelegate
{
    private static final String CONFIG_PERIOD = "period"; //$NON-NLS-1$

    private final Widget widget;
    private ReportingPeriod reportingPeriod;

    private Label[] signs;
    private Label[] labels;
    private Label[] values;

    public PerformanceCalculationWidget(Widget widget)
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

    @Override
    public Composite createControl(Composite parent, DashboardResources resources)
    {
        Composite container = new Composite(parent, SWT.NONE);
        GridLayoutFactory.fillDefaults().numColumns(3).margins(5, 5).applyTo(container);
        container.setBackground(parent.getBackground());

        labels = new Label[ClientPerformanceSnapshot.CategoryType.values().length];
        signs = new Label[labels.length];
        values = new Label[labels.length];

        for (int ii = 0; ii < labels.length; ii++)
        {
            signs[ii] = new Label(container, SWT.NONE);
            labels[ii] = new Label(container, SWT.NONE);
            values[ii] = new Label(container, SWT.RIGHT);
            GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).applyTo(values[ii]);
        }

        return container;
    }

    @Override
    public void update(DashboardData data)
    {
        ClientPerformanceSnapshot snapshot = data.calculate(ClientPerformanceSnapshot.class, reportingPeriod);

        int ii = 0;
        for (ClientPerformanceSnapshot.Category category : snapshot.getCategories())
        {
            signs[ii].setText(category.getSign());
            labels[ii].setText(category.getLabel());
            values[ii].setText(Values.Money.format(category.getValuation(), data.getClient().getBaseCurrency()));

            if (++ii >= labels.length)
                break;
        }
    }

}
