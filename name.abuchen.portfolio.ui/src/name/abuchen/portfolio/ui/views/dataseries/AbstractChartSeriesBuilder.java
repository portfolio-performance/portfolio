package name.abuchen.portfolio.ui.views.dataseries;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swtchart.IBarSeries;
import org.eclipse.swtchart.ILineSeries;

import name.abuchen.portfolio.ui.util.chart.TimelineChart;

public abstract class AbstractChartSeriesBuilder
{
    private final TimelineChart chart;
    private final DataSeriesCache cache;

    private final LocalResourceManager resources;

    public AbstractChartSeriesBuilder(TimelineChart chart, DataSeriesCache cache)
    {
        this.chart = chart;
        this.cache = cache;

        this.resources = new LocalResourceManager(JFaceResources.getResources(), chart);
    }

    public DataSeriesCache getCache()
    {
        return cache;
    }

    public TimelineChart getChart()
    {
        return chart;
    }

    protected void configure(DataSeries series, ILineSeries<?> lineSeries)
    {
        Color color = resources.createColor(series.getColor());

        lineSeries.setLineColor(color);
        lineSeries.setSymbolColor(color);
        lineSeries.enableArea(series.isShowArea());
        lineSeries.setLineStyle(series.getLineStyle());
        lineSeries.setLineWidth(series.getLineWidth());
    }

    protected void configure(DataSeries series, IBarSeries<?> barSeries)
    {
        barSeries.setBarPadding(50);
        barSeries.setBarColor(resources.createColor(series.getColor()));
    }
}