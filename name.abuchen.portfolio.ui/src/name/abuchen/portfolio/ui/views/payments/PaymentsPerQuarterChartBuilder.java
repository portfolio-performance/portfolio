package name.abuchen.portfolio.ui.views.payments;

import java.util.List;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.swtchart.Chart;
import org.swtchart.IAxis;
import org.swtchart.IAxis.Position;
import org.swtchart.IBarSeries;
import org.swtchart.ISeries.SeriesType;
import org.swtchart.LineStyle;

import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.chart.TimelineChartToolTip;
import name.abuchen.portfolio.ui.util.format.ThousandsNumberFormat;
import name.abuchen.portfolio.ui.util.swt.ColoredLabel;
import name.abuchen.portfolio.ui.views.payments.PaymentsViewModel.Line;
import name.abuchen.portfolio.util.TextUtil;

public class PaymentsPerQuarterChartBuilder implements PaymentsChartBuilder
{
    private static class DividendPerQuarterChartToolTip extends TimelineChartToolTip
    {
        public DividendPerQuarterChartToolTip(Chart chart)
        {
            super(chart);

            enableCategory(true);
        }

        @Override
        protected void createComposite(Composite parent)
        {
            PaymentsViewModel model = (PaymentsViewModel) getChart().getData(PaymentsViewModel.class.getSimpleName());

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
                            })
                            // sort alphabetically
                            .sorted((l1, l2) -> TextUtil.compare(l1.getVehicle().getName(), l2.getVehicle().getName()))
                            .toList();

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
    public int getTabIndex()
    {
        return 4;
    }

    @Override
    public void configure(Chart chart)
    {
        IAxis xAxis = chart.getAxisSet().getXAxis(0);
        xAxis.getTick().setVisible(true);
        xAxis.getTitle().setVisible(false);
        xAxis.getTitle().setText(Messages.ColumnMonth);
        xAxis.getGrid().setStyle(LineStyle.NONE);
        xAxis.enableCategory(true);

        setCategorySeries(chart);

        IAxis yAxis = chart.getAxisSet().getYAxis(0);
        yAxis.getTitle().setVisible(false);
        yAxis.getTick().setVisible(true);
        yAxis.setPosition(Position.Secondary);
        yAxis.getTick().setFormat(new ThousandsNumberFormat());

        new DividendPerQuarterChartToolTip(chart);

    }

    private void setCategorySeries(Chart chart)
    {
        String[] labels = new String[4];
        for (int ii = 0; ii < 4; ii++)
        {
            String label = String.format("Q%d", ii + 1); //$NON-NLS-1$
            labels[ii] = label;
        }
        chart.getAxisSet().getXAxis(0).setCategorySeries(labels);
    }

    @Override
    public void createSeries(Chart chart, PaymentsViewModel model)
    {
        chart.setData(PaymentsViewModel.class.getSimpleName(), model);

        for (int index = 0; index < model.getNoOfMonths(); index += 12)
        {
            int year = model.getStartYear() + (index / 12);

            IBarSeries barSeries = (IBarSeries) chart.getSeriesSet().createSeries(SeriesType.BAR, String.valueOf(year));

            double[] series = new double[Math.min(12, model.getNoOfMonths() - index)];
            for (int ii = 0; ii < series.length; ii++)
            {
                series[ii / 3] = series[ii / 3] + model.getSum().getValue(index + ii) / Values.Amount.divider();
            }
            barSeries.setYSeries(series);

            barSeries.setBarColor(PaymentsColors.getColor(year));
            barSeries.setBarPadding(25);
        }
    }
}
