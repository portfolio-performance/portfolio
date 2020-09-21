package name.abuchen.portfolio.ui.views.earnings;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.swtchart.Chart;
import org.swtchart.IBarSeries;
import org.swtchart.ISeries.SeriesType;

import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.ui.util.chart.TimelineChartToolTip;
import name.abuchen.portfolio.ui.util.swt.ColoredLabel;
import name.abuchen.portfolio.ui.views.earnings.EarningsViewModel.Line;
import name.abuchen.portfolio.util.TextUtil;

public class EarningsPerYearChartTab extends AbstractChartTab
{
    private class DividendPerYearToolTip extends TimelineChartToolTip
    {
        private EarningsViewModel model;

        public DividendPerYearToolTip(Chart chart, EarningsViewModel model)
        {
            super(chart);

            this.model = model;
        }

        @Override
        protected void createComposite(Composite parent)
        {
            final int year = (Integer) getFocusedObject();
            int totalNoOfMonths = model.getNoOfMonths();

            IBarSeries barSeries = (IBarSeries) getChart().getSeriesSet().getSeries()[0];

            List<Line> lines = model.getLines().stream() //
                            .filter(line -> {
                                for (int index = year * 12; index < (year + 1) * 12
                                                && index < totalNoOfMonths; index += 1)
                                    if (line.getValue(index) != 0L)
                                        return true;
                                return false;
                            })
                            .sorted((l1, l2) -> l1.getVehicle().getName()
                                            .compareToIgnoreCase(l2.getVehicle().getName()))
                            .collect(Collectors.toList());

            final Composite container = new Composite(parent, SWT.NONE);
            container.setBackgroundMode(SWT.INHERIT_FORCE);
            GridLayoutFactory.swtDefaults().numColumns(2).applyTo(container);

            Label topLeft = new Label(container, SWT.NONE);
            topLeft.setText(Messages.ColumnSecurity);

            ColoredLabel label = new ColoredLabel(container, SWT.CENTER);
            label.setHightlightColor(barSeries.getBarColor());
            label.setText(String.valueOf(model.getStartYear() + year));
            GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).applyTo(label);

            lines.forEach(line -> {
                Label l = new Label(container, SWT.NONE);
                l.setText(TextUtil.tooltip(line.getVehicle().getName()));

                long value = 0;
                for (int m = year * 12; m < (year + 1) * 12 && m < totalNoOfMonths; m += 1)
                    value += line.getValue(m);

                l = new Label(container, SWT.RIGHT);
                l.setText(Values.Amount.format(value));
                GridDataFactory.fillDefaults().align(SWT.END, SWT.FILL).applyTo(l);
            });

            Label l = new Label(container, SWT.NONE);
            l.setText(Messages.ColumnSum);

            long value = 0;
            for (int m = year * 12; m < (year + 1) * 12 && m < totalNoOfMonths; m += 1)
                value += model.getSum().getValue(m);

            ColoredLabel cl = new ColoredLabel(container, SWT.RIGHT);
            cl.setHightlightColor(barSeries.getBarColor());
            cl.setText(Values.Amount.format(value));
            GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).applyTo(cl);
        }
    }

    @Override
    public String getLabel()
    {
        return Messages.LabelEarningsPerYear;
    }

    @Override
    protected void attachTooltipTo(Chart chart)
    {
        DividendPerYearToolTip toolTip = new DividendPerYearToolTip(chart, model);
        toolTip.enableCategory(true);
    }

    private void updateCategorySeries()
    {
        int startYear = model.getStartYear();
        String[] labels = new String[LocalDate.now().getYear() - startYear + 1];
        for (int ii = 0; ii < labels.length; ii++)
            labels[ii] = String.valueOf(startYear + ii);
        getChart().getAxisSet().getXAxis(0).setCategorySeries(labels);
    }

    @Override
    protected void createSeries()
    {
        updateCategorySeries();

        for (int index = 0; index < model.getNoOfMonths(); index += 12)
        {
            int year = model.getStartYear() + (index / 12);
            IBarSeries barSeries = (IBarSeries) getChart().getSeriesSet().createSeries(SeriesType.BAR, String.valueOf(year));
            double[] series = new double[Math.min(12, model.getNoOfMonths() - index)];

            long total = 0;

//            int months = Math.min(12, model.getNoOfMonths() - index);
//            for (int ii = 0; ii < months; ii++)
                for (int ii = 0; ii < series.length; ii++)
                {
                    series[(int) ii / 12 + index / 12] = series[ii / 12 + index / 12] + model.getSum().getValue(index + ii) / Values.Amount.divider();
                }
//                total += model.getSum().getValue(index + ii);

//            series[index/12] = total / Values.Amount.divider();

            barSeries.setYSeries(series);
            
            barSeries.setBarColor(getColor(year));
            barSeries.setBarPadding(25);
        }
    }
}
