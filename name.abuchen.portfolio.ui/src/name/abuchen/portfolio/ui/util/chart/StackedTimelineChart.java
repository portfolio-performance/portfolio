package name.abuchen.portfolio.ui.util.chart;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swtchart.Chart;
import org.eclipse.swtchart.IAxis;
import org.eclipse.swtchart.IAxis.Position;
import org.eclipse.swtchart.ICustomPaintListener;
import org.eclipse.swtchart.ILineSeries;
import org.eclipse.swtchart.ILineSeries.PlotSymbolType;
import org.eclipse.swtchart.ISeries.SeriesType;
import org.eclipse.swtchart.internal.PlotArea;
import org.eclipse.swtchart.LineStyle;
import org.eclipse.swtchart.Range;

import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.util.Dates;

public class StackedTimelineChart extends Chart // NOSONAR
{
    private TimelineChartToolTip toolTip;

    private List<LocalDate> dates;

    private ChartContextMenu contextMenu;

    @SuppressWarnings("restriction")
    public StackedTimelineChart(Composite parent, List<LocalDate> dates)
    {
        super(parent, SWT.NONE, null);

        // we must use the secondary constructor that is not creating the
        // PlotArea because the default constructor adds a mouse move listener
        // that is redrawing the chart on every mouse move. That leads to janky
        // UI when the tooltip is shown.
        new PlotArea(this, SWT.NONE);

        setData(UIConstants.CSS.CLASS_NAME, "chart"); //$NON-NLS-1$

        getLegend().setVisible(false);

        // x axis
        IAxis xAxis = getAxisSet().getXAxis(0);
        xAxis.getTitle().setVisible(false);
        xAxis.getTitle().setText(Messages.ColumnDate);
        xAxis.getTick().setVisible(false);
        xAxis.getGrid().setStyle(LineStyle.NONE);
        setDates(dates);

        // y axis
        IAxis yAxis = getAxisSet().getYAxis(0);
        yAxis.getTitle().setVisible(false);
        yAxis.setPosition(Position.Secondary);

        getPlotArea().addCustomPaintListener(new ICustomPaintListener()
        {
            @Override
            public void paintControl(PaintEvent e)
            {
                paintTimeGrid(e);
            }

            @Override
            public boolean drawBehindSeries()
            {
                return true;
            }
        });

        toolTip = new TimelineChartToolTip(this);
        toolTip.enableCategory(true);
        toolTip.reverseLabels(true);
        toolTip.setXAxisFormat(obj -> {
            Integer index = (Integer) obj;
            DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE;
            return Values.Date.format(LocalDate.parse(getAxisSet().getXAxis(0).getCategorySeries()[index], formatter));
        });

        this.contextMenu = new ChartContextMenu(this);
    }

    public void setDates(List<LocalDate> dates)
    {
        this.dates = dates;
        IAxis xAxis = getAxisSet().getXAxis(0);
        String[] categories = new String[dates.size()];
        for (int ii = 0; ii < categories.length; ii++)
            categories[ii] = dates.get(ii).toString();
        xAxis.setCategorySeries(categories);
        xAxis.enableCategory(true);
    }

    public ILineSeries<?> addSeries(String id, String label, double[] values, Color color)
    {
        var series = (ILineSeries<?>) getSeriesSet().createSeries(SeriesType.LINE, id);
        series.setDescription(label);
        series.setYSeries(values);

        series.setLineWidth(2);
        series.setSymbolType(PlotSymbolType.NONE);
        series.setAntialias(SWT.ON);

        series.enableStack(true);
        series.enableArea(true);
        series.setLineColor(color);

        return series;
    }

    public TimelineChartToolTip getToolTip()
    {
        return toolTip;
    }

    private void paintTimeGrid(PaintEvent e)
    {
        IAxis xAxis = getAxisSet().getXAxis(0);
        Range range = xAxis.getRange();

        LocalDate start = dates.get(0);
        LocalDate end = dates.get(dates.size() - 1);
        int days = Dates.daysBetween(start, end) + 1;

        TimeGridHelper.paintTimeGrid(this, e, start, end, cursor -> {
            int d = Dates.daysBetween(start, cursor);
            return xAxis.getPixelCoordinate(d * range.upper / days);
        });
    }

    @Override
    public void save(String filename, int format)
    {
        ChartUtil.save(this, filename, format);
    }

    @Override
    public boolean setFocus()
    {
        return getPlotArea().getControl().setFocus();
    }

    public void exportMenuAboutToShow(IMenuManager manager, String label)
    {
        this.contextMenu.exportMenuAboutToShow(manager, label);
    }
}
