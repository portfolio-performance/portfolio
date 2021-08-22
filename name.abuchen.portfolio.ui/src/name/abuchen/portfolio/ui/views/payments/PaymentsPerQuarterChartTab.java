package name.abuchen.portfolio.ui.views.payments;

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
import name.abuchen.portfolio.ui.util.chart.TimelineChartToolTip;
import name.abuchen.portfolio.ui.util.swt.ColoredLabel;
import name.abuchen.portfolio.ui.views.payments.PaymentsViewModel.Line;
import name.abuchen.portfolio.util.TextUtil;

public class PaymentsPerQuarterChartTab extends AbstractChartTab
{
    private class DividendPerQuarterChartToolTip extends TimelineChartToolTip
    {
        private PaymentsViewModel model;

        public DividendPerQuarterChartToolTip(Chart chart, PaymentsViewModel model)
        {
            super(chart);

            this.model = model;
        }

        @Override
        protected void createComposite(Composite parent)
        {
            int quarter = (Integer) getFocusedObject();
            int totalNoOfMonths = model.getNoOfMonths();

            List<Line> lines = model.getLines().stream() //
                            .filter(line -> {
                                for (int index = 0; index < totalNoOfMonths; index += 1)
                                {
                                    if ((line.getValue(index) != 0L) && (((index % 12) / 3) == quarter))
                                        return true;
                                }
                                return false;
                            }).sorted((l1, l2) -> l1.getVehicle().getName() // sort
                                                                            // alphabetically
                                            .compareToIgnoreCase(l2.getVehicle().getName()))
                            .collect(Collectors.toList());

            int noOfYears = (totalNoOfMonths / 12) + (totalNoOfMonths % 12 > quarter * 3 ? 1 : 0);

            final Composite container = new Composite(parent, SWT.NONE);
            container.setBackgroundMode(SWT.INHERIT_FORCE);
            GridLayoutFactory.swtDefaults().numColumns(1 + noOfYears).applyTo(container);

            Label topLeft = new Label(container, SWT.NONE);
            topLeft.setText(Messages.ColumnSecurity);

            for (int year = 0; year < noOfYears; year++)
            {
                ColoredLabel label = new ColoredLabel(container, SWT.CENTER);
                label.setBackdropColor(((IBarSeries) getChart().getSeriesSet().getSeries()[year]).getBarColor());
                label.setText(String.valueOf(model.getStartYear() + year));
                GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).applyTo(label);
            }

            lines.forEach(line -> {
                Label l = new Label(container, SWT.NONE);
                l.setText(TextUtil.tooltip(line.getVehicle().getName()));

                for (int m = quarter * 3; m < totalNoOfMonths; m += 12)
                {
                    int mLimit = m + 3;
                    long value = 0;
                    for (int mQuarter = m; mQuarter < mLimit && mQuarter < totalNoOfMonths; mQuarter += 1)
                        value += line.getValue(mQuarter);
                    l = new Label(container, SWT.RIGHT);
                    l.setText(Values.Amount.format(value));
                    GridDataFactory.fillDefaults().align(SWT.END, SWT.FILL).applyTo(l);
                }
            });

            if (model.usesConsolidateRetired())
            {
                Label lSumRetired = new Label(container, SWT.NONE);
                lSumRetired.setText(Messages.LabelPaymentsConsolidateRetired);

                for (int m = quarter * 3; m < totalNoOfMonths; m += 12)
                {
                    int mLimit = m + 3;
                    long value = 0;
                    for (int mQuarter = m; mQuarter < mLimit && mQuarter < totalNoOfMonths; mQuarter += 1)
                        value += model.getSumRetired().getValue(mQuarter);
                    ColoredLabel cl = new ColoredLabel(container, SWT.RIGHT);
                    cl.setText(Values.Amount.format(value));
                    GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).applyTo(cl);
                }
            }

            Label lSum = new Label(container, SWT.NONE);
            lSum.setText(Messages.ColumnSum);

            for (int m = quarter * 3; m < totalNoOfMonths; m += 12)
            {
                int mLimit = m + 3;
                long value = 0;
                for (int mQuarter = m; mQuarter < mLimit && mQuarter < totalNoOfMonths; mQuarter += 1)
                    value += model.getSum().getValue(mQuarter);
                ColoredLabel cl = new ColoredLabel(container, SWT.RIGHT);
                cl.setBackdropColor(((IBarSeries) getChart().getSeriesSet().getSeries()[m / 12]).getBarColor());
                cl.setText(Values.Amount.format(value));
                GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).applyTo(cl);
            }
        }
    }

    @Override
    public String getLabel()
    {
        return Messages.LabelPaymentsPerQuarter;
    }

    @Override
    protected void attachTooltipTo(Chart chart)
    {
        DividendPerQuarterChartToolTip toolTip = new DividendPerQuarterChartToolTip(chart, model);
        toolTip.enableCategory(true);
    }

    private void updateCategorySeries()
    {
        String[] labels = new String[4];
        for (int ii = 0; ii < 4; ii++)
        {
            String label = String.format("Q%d", ii + 1); //$NON-NLS-1$
            labels[ii] = label;
        }
        getChart().getAxisSet().getXAxis(0).setCategorySeries(labels);
    }

    @Override
    protected void createSeries()
    {
        updateCategorySeries();
        for (int index = 0; index < model.getNoOfMonths(); index += 12)
        {
            int year = model.getStartYear() + (index / 12);

            IBarSeries barSeries = (IBarSeries) getChart().getSeriesSet().createSeries(SeriesType.BAR,
                            String.valueOf(year));

            double[] series = new double[Math.min(12, model.getNoOfMonths() - index)];
            for (int ii = 0; ii < series.length; ii++)
            {
                series[(int) ii / 3] = series[ii / 3] + model.getSum().getValue(index + ii) / Values.Amount.divider();
            }
            barSeries.setYSeries(series);

            barSeries.setBarColor(getColor(year));
            barSeries.setBarPadding(25);
        }
    }
}
