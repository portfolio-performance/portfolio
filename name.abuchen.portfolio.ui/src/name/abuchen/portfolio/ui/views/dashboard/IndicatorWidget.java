package name.abuchen.portfolio.ui.views.dashboard;

import java.util.function.BiFunction;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;

import name.abuchen.portfolio.model.Dashboard.Widget;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.ReportingPeriod;
import name.abuchen.portfolio.ui.views.dataseries.DataSeries;

public class IndicatorWidget<N extends Number> extends AbstractIndicatorWidget
{
    private final Values<N> formatter;
    private final BiFunction<DataSeries, ReportingPeriod, N> provider;

    public IndicatorWidget(Widget widget, DashboardData dashboardData, boolean supportsBenchmarks, Values<N> formatter,
                    BiFunction<DataSeries, ReportingPeriod, N> provider)
    {
        super(widget, dashboardData, supportsBenchmarks);

        this.formatter = formatter;
        this.provider = provider;
    }

    @Override
    public void update()
    {
        super.update();

        N value = provider.apply(getDataSeries(), getReportingPeriod());
        indicator.setText(formatter.format(value));
        indicator.setForeground(Display.getDefault()
                        .getSystemColor(value.doubleValue() < 0 ? SWT.COLOR_DARK_RED : SWT.COLOR_DARK_GREEN));
    }

}
