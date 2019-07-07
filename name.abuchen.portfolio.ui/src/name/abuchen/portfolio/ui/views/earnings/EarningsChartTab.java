package name.abuchen.portfolio.ui.views.earnings;

import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.swtchart.Chart;
import org.swtchart.IBarSeries;
import org.swtchart.ISeries.SeriesType;

import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.ui.util.chart.TimelineChartToolTip;
import name.abuchen.portfolio.ui.views.earnings.EarningsViewModel.Line;
import name.abuchen.portfolio.util.TextUtil;

public class EarningsChartTab extends AbstractChartTab
{
    private class DividendChartToolTip extends TimelineChartToolTip
    {
        private EarningsViewModel model;

        public DividendChartToolTip(Chart chart, EarningsViewModel model)
        {
            super(chart);

            this.model = model;
        }

        @Override
        protected void createComposite(Composite parent)
        {
            int month = (Integer) getFocusedObject();
            int totalNoOfMonths = model.getNoOfMonths();

            List<Line> lines = model.getLines().stream() //
                            .filter(line -> {
                                for (int index = month; index < totalNoOfMonths; index += 12)
                                    if (line.getValue(index) != 0L)
                                        return true;
                                return false;
                            })
                            .sorted((l1, l2) -> l1.getVehicle().getName()
                                            .compareToIgnoreCase(l2.getVehicle().getName()))
                            .collect(Collectors.toList());

            int noOfYears = (totalNoOfMonths / 12) + (totalNoOfMonths % 12 > month ? 1 : 0);

            final Composite container = new Composite(parent, SWT.NONE);
            container.setBackgroundMode(SWT.INHERIT_FORCE);
            GridLayoutFactory.swtDefaults().numColumns(1 + noOfYears).applyTo(container);

            Color foregroundColor = Display.getDefault().getSystemColor(SWT.COLOR_BLACK);
            container.setForeground(foregroundColor);
            container.setBackground(Colors.INFO_TOOLTIP_BACKGROUND);

            Label topLeft = new Label(container, SWT.NONE);
            topLeft.setForeground(foregroundColor);
            topLeft.setText(Messages.ColumnSecurity);

            for (int year = 0; year < noOfYears; year++)
            {
                Label label = new Label(container, SWT.CENTER);

                Color color = ((IBarSeries) getChart().getSeriesSet().getSeries()[year]).getBarColor();
                label.setBackground(color);
                label.setForeground(Colors.getTextColor(color));
                label.setText(String.valueOf(model.getStartYear() + year));
                GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).applyTo(label);
            }

            lines.forEach(line -> {
                Label l = new Label(container, SWT.NONE);
                l.setForeground(foregroundColor);
                l.setText(TextUtil.tooltip(line.getVehicle().getName()));

                for (int m = month; m < totalNoOfMonths; m += 12)
                {
                    l = new Label(container, SWT.RIGHT);
                    l.setForeground(foregroundColor);
                    l.setText(Values.Amount.format(line.getValue(m)));
                    GridDataFactory.fillDefaults().align(SWT.END, SWT.FILL).applyTo(l);
                }
            });

            Label l = new Label(container, SWT.NONE);
            l.setForeground(foregroundColor);
            l.setText(Messages.ColumnSum);

            for (int m = month; m < totalNoOfMonths; m += 12)
            {
                l = new Label(container, SWT.RIGHT);
                Color color = ((IBarSeries) getChart().getSeriesSet().getSeries()[m / 12]).getBarColor();
                l.setBackground(color);
                l.setForeground(Colors.getTextColor(color));
                l.setText(Values.Amount.format(model.getSum().getValue(m)));
                GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).applyTo(l);
            }

        }
    }

    @Override
    public String getLabel()
    {
        return Messages.LabelEarningsPerMonth;
    }

    @Override
    protected void attachTooltipTo(Chart chart)
    {
        DividendChartToolTip toolTip = new DividendChartToolTip(chart, model);
        toolTip.enableCategory(true);
    }

    @Override
    protected void createSeries()
    {
        for (int index = 0; index < model.getNoOfMonths(); index += 12)
        {
            int year = model.getStartYear() + (index / 12);

            IBarSeries barSeries = (IBarSeries) getChart().getSeriesSet().createSeries(SeriesType.BAR,
                            String.valueOf(year));

            double[] series = new double[Math.min(12, model.getNoOfMonths() - index)];
            for (int ii = 0; ii < series.length; ii++)
                series[ii] = model.getSum().getValue(index + ii) / Values.Amount.divider();
            barSeries.setYSeries(series);

            barSeries.setBarColor(getColor(year));
            barSeries.setBarPadding(25);
        }
    }
}
