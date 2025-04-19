package name.abuchen.portfolio.ui.util.chart;

// import java.time.LocalDate;
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
import org.eclipse.swtchart.ISeries.SeriesType;
import org.eclipse.swtchart.internal.PlotArea;
import org.eclipse.swtchart.LineStyle;
import org.eclipse.swtchart.IBarSeries;
// import name.abuchen.portfolio.PortfolioLog;
import java.util.ArrayList;

import name.abuchen.portfolio.ui.UIConstants;

public class StackedChart extends Chart // NOSONAR
{
    private TimelineChartToolTip toolTip;

    private List<String> categories;

    private ChartContextMenu contextMenu;

    @SuppressWarnings("restriction")
    public StackedChart(Composite parent, String xAxisTitle)
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
        xAxis.getTitle().setText(xAxisTitle);
        xAxis.getTick().setVisible(false);
        xAxis.getGrid().setStyle(LineStyle.NONE);

        setCategories(new ArrayList<>());

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

        this.contextMenu = new ChartContextMenu(this);
    }

    public void setCategories(List<String> categories)
    {
        this.categories = categories;
        IAxis xAxis = getAxisSet().getXAxis(0);
        xAxis.setCategorySeries(categories.toArray(new String[categories.size()]));
        xAxis.enableCategory(true);
    }

    public IBarSeries<?> addSeries(String id, String label, double[] values, Color color, boolean visible)
    {
        var series = (IBarSeries<?>) getSeriesSet().createSeries(SeriesType.BAR, id);
        series.setDescription(label);
        series.setYSeries(values);
        series.setBarColor(color);
        series.setVisible(visible);
        series.setBarPadding(25);

        return series;
    }

    public TimelineChartToolTip getToolTip()
    {
        return toolTip;
    }

    private void paintTimeGrid(PaintEvent e)
    {
        // IAxis xAxis = getAxisSet().getXAxis(0);

        // LocalDate start = LocalDate.ofEpochDay(0);
        // LocalDate end = LocalDate.ofEpochDay(categories.size() - 1);

        // PortfolioLog.error(String.format("start: %s, end: %s", start, end));

        // TimeGridHelper.paintTimeGrid(this, e, start, end, cursor -> xAxis.getPixelCoordinate(cursor.toEpochDay()));
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

    public void adjustRange()
    {
        try
        {
            setRedraw(false);

            getAxisSet().adjustRange();
            ChartUtil.addYMargins(this, 0.08);
        }
        finally
        {
            setRedraw(true);
        }
    }
}
