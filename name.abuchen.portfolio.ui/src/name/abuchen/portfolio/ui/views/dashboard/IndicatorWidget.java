package name.abuchen.portfolio.ui.views.dashboard;

import java.util.function.BiFunction;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;

import name.abuchen.portfolio.model.Dashboard.Widget;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.ReportingPeriod;

public class IndicatorWidget<N extends Number> extends AbstractPeriodWidget
{
    private final Values<N> formatter;
    private final BiFunction<DashboardData, ReportingPeriod, N> provider;

    public IndicatorWidget(Widget widget, Values<N> formatter, BiFunction<DashboardData, ReportingPeriod, N> provider)
    {
        super(widget);

        this.formatter = formatter;
        this.provider = provider;
    }

    @Override
    public void update(DashboardData data)
    {
        N value = provider.apply(data, getReportingPeriod());
        indicator.setText(formatter.format(value));
        indicator.setForeground(Display.getDefault()
                        .getSystemColor(value.doubleValue() < 0 ? SWT.COLOR_DARK_RED : SWT.COLOR_DARK_GREEN));
    }

}
