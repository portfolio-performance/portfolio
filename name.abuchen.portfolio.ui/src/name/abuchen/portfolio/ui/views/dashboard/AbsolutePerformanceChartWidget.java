package name.abuchen.portfolio.ui.views.dashboard;

import java.time.LocalDate;
import java.util.function.Supplier;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

import name.abuchen.portfolio.model.Dashboard.Widget;
import name.abuchen.portfolio.snapshot.ClientPerformanceSnapshot;
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.views.AbsolutePerformancePieChart;
import name.abuchen.portfolio.util.TextUtil;

public class AbsolutePerformanceChartWidget extends WidgetDelegate<ClientPerformanceSnapshot>
{
    private Label title;
    private AbsolutePerformancePieChart chart;
    private Composite container;

    public AbsolutePerformanceChartWidget(Widget widget, DashboardData data)
    {
        super(widget, data);
        addConfig(new ReportingPeriodConfig(this));
        addConfig(new DataSeriesConfig(this, false));
        addConfig(new ChartHeightConfig(this));
    }

    @Override
    public Composite createControl(Composite parent, DashboardResources resources)
    {
        container = new Composite(parent, SWT.NONE);
        GridLayoutFactory.fillDefaults().margins(5, 5).applyTo(container);
        container.setData(UIConstants.CSS.CLASS_NAME, getContainerCssClassNames());

        title = new Label(container, SWT.NONE);
        title.setText(TextUtil.tooltip(getWidget().getLabel()));
        title.setData(UIConstants.CSS.CLASS_NAME, UIConstants.CSS.TITLE);
        GridDataFactory.fillDefaults().grab(true, false).applyTo(title);

        chart = new AbsolutePerformancePieChart();
        var chartControl = chart.createControl(container, true);
        GridDataFactory.fillDefaults().grab(true, false).applyTo(chartControl);
        chart.setChartHeight(get(ChartHeightConfig.class).getPixel());
        getDashboardData().getStylingEngine().style(container);
        return container;
    }

    @Override
    public Supplier<ClientPerformanceSnapshot> getUpdateTask()
    {
        return () -> {
            var period = get(ReportingPeriodConfig.class).getReportingPeriod().toInterval(LocalDate.now());
            var index = getDashboardData().calculate(get(DataSeriesConfig.class).getDataSeries(), period);
            return index.getClientPerformanceSnapshot().orElseThrow(IllegalArgumentException::new);
        };
    }

    @Override
    public void update(ClientPerformanceSnapshot snapshot)
    {
        title.setText(TextUtil.tooltip(getWidget().getLabel()));
        chart.setChartHeight(get(ChartHeightConfig.class).getPixel());
        chart.refresh(snapshot, get(DataSeriesConfig.class).getDataSeries().getLabel());
        container.getParent().layout(true, true);
    }

    @Override
    public Control getTitleControl()
    {
        return title;
    }
}
