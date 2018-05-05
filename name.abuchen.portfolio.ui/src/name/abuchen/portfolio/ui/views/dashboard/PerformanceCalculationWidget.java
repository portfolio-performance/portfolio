package name.abuchen.portfolio.ui.views.dashboard;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.FontDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

import name.abuchen.portfolio.model.Dashboard.Widget;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.ClientPerformanceSnapshot;
import name.abuchen.portfolio.snapshot.PerformanceIndex;

public class PerformanceCalculationWidget extends WidgetDelegate
{
    private Composite container;
    private Label title;
    private Label[] signs;
    private Label[] labels;
    private Label[] values;

    public PerformanceCalculationWidget(Widget widget, DashboardData dashboardData)
    {
        super(widget, dashboardData);

        addConfig(new ReportingPeriodConfig(this));
        addConfig(new DataSeriesConfig(this, false));
    }

    @Override
    public Composite createControl(Composite parent, DashboardResources resources)
    {
        container = new Composite(parent, SWT.NONE);
        GridLayoutFactory.fillDefaults().numColumns(3).margins(5, 5).spacing(3, 3).applyTo(container);
        container.setBackground(parent.getBackground());

        title = new Label(container, SWT.NONE);
        title.setText(getWidget().getLabel());
        GridDataFactory.fillDefaults().span(3, 1).hint(SWT.DEFAULT, title.getFont().getFontData()[0].getHeight() + 10).grab(true, false).applyTo(title);

        Font boldFont = resources.getResourceManager().createFont(FontDescriptor.createFrom(container.getFont()).setStyle(SWT.BOLD));

        labels = new Label[ClientPerformanceSnapshot.CategoryType.values().length];
        signs = new Label[labels.length];
        values = new Label[labels.length];

        for (int ii = 0; ii < labels.length; ii++)
        {
            signs[ii] = new Label(container, SWT.NONE);
            labels[ii] = new Label(container, SWT.NONE);
            values[ii] = new Label(container, SWT.RIGHT);
            GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).applyTo(values[ii]);
            
            if (ii == 0 || ii == labels.length - 1)
            {
                signs[ii].setFont(boldFont);
                labels[ii].setFont(boldFont);
                values[ii].setFont(boldFont);
            }
            
            if (ii == 0 || ii == labels.length - 2)
            {
                Label separator = new Label(container, SWT.SEPARATOR | SWT.HORIZONTAL);
                GridDataFactory.fillDefaults().span(3, 1).grab(true, false).align(SWT.FILL, SWT.BEGINNING).applyTo(separator);
            }
        }

        return container;
    }

    @Override
    public Control getTitleControl()
    {
        return title;
    }

    @Override
    public void update()
    {
        title.setText(getWidget().getLabel());

        PerformanceIndex index = getDashboardData().calculate(get(DataSeriesConfig.class).getDataSeries(),
                        get(ReportingPeriodConfig.class).getReportingPeriod());
        ClientPerformanceSnapshot snapshot = index.getClientPerformanceSnapshot();

        int ii = 0;
        for (ClientPerformanceSnapshot.Category category : snapshot.getCategories())
        {
            signs[ii].setText(category.getSign());
            labels[ii].setText(category.getLabel());
            values[ii].setText(Values.Money.format(category.getValuation(), getClient().getBaseCurrency()));

            if (++ii >= labels.length)
                break;
        }

        container.layout();
    }
}
