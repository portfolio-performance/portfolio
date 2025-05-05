package name.abuchen.portfolio.ui.util.chart;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swtchart.IAxis;
import org.eclipse.swtchart.IAxis.Position;
import org.eclipse.swtchart.IBarSeries;
import org.eclipse.swtchart.ISeries.SeriesType;
import org.eclipse.swtchart.LineStyle;

import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.util.format.AmountNumberFormat;
import name.abuchen.portfolio.ui.util.format.ThousandsNumberFormat;

public class BarChart extends PlainChart // NOSONAR
{
    private TimelineChartToolTip toolTip;

    private ChartContextMenu contextMenu;

    public BarChart(Composite parent, String xAxisTitle)
    {
        super(parent, SWT.NONE);

        setData(UIConstants.CSS.CLASS_NAME, "chart"); //$NON-NLS-1$

        getLegend().setVisible(false);
        getTitle().setVisible(false);

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
        yAxis.getTick().setFormat(new ThousandsNumberFormat());

        toolTip = new TimelineChartToolTip(this);
        toolTip.setDefaultValueFormat(new AmountNumberFormat());
        toolTip.enableCategory(true);
        toolTip.reverseLabels(false);

        this.contextMenu = new ChartContextMenu(this);
    }

    public void setCategories(List<String> categories)
    {
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
