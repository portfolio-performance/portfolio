package name.abuchen.portfolio.ui.views.securitychart;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swtchart.IAxis;
import org.eclipse.swtchart.IAxis.Position;
import org.eclipse.swtchart.LineStyle;
import org.eclipse.swtchart.Range;

import name.abuchen.portfolio.ui.util.chart.ChartUtil;
import name.abuchen.portfolio.ui.util.chart.TimelineChart;
import name.abuchen.portfolio.ui.util.format.AxisTickPercentNumberFormat;

public class PriceTimelineChart extends TimelineChart
{
    private Double firstQuote;

    public PriceTimelineChart(Composite parent)
    {
        super(parent);

        // 2nd y axis
        int axisId = getAxisSet().createYAxis();
        IAxis y2Axis = getAxisSet().getYAxis(axisId);
        y2Axis.getTitle().setVisible(false);
        y2Axis.getTick().setVisible(false);
        y2Axis.getGrid().setStyle(LineStyle.NONE);
        y2Axis.setPosition(Position.Primary);

        // 3rd y axis (percentage)
        int axisId3rd = getAxisSet().createYAxis();
        IAxis y3Axis = getAxisSet().getYAxis(axisId3rd);
        y3Axis.getTitle().setVisible(false);
        y3Axis.getTick().setVisible(false);
        y3Axis.getTick().setFormat(new AxisTickPercentNumberFormat("+#.##%;-#.##%")); //$NON-NLS-1$
        y3Axis.getGrid().setStyle(LineStyle.NONE);
        y3Axis.setPosition(Position.Primary);
    }

    @Override
    public void adjustRange()
    {
        // custom implementation because the 2nd and 3rd y axis must follow
        // exactly the first one
        try
        {
            setRedraw(false);

            getAxisSet().adjustRange();

            var yAxis1st = getAxisSet().getYAxis(0);
            var yAxis2nd = getAxisSet().getYAxis(1);
            var yAxis3rd = getAxisSet().getYAxis(2);

            if (firstQuote != null)
            {
                yAxis2nd.setRange(new Range(yAxis1st.getRange().lower - firstQuote,
                                yAxis1st.getRange().upper - firstQuote));

                if (firstQuote != 0)
                {
                    yAxis3rd.setRange(new Range(yAxis1st.getRange().lower / firstQuote - 1,
                                    yAxis1st.getRange().upper / firstQuote - 1));
                }
            }

            ChartUtil.addYMargins(this, 0.08);

        }
        finally
        {
            setRedraw(true);
        }
    }

    public void setFirstQuote(Double firstQuote)
    {
        this.firstQuote = firstQuote;
    }

}