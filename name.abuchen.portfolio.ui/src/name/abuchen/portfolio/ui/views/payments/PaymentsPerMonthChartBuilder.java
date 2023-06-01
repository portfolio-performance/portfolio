package name.abuchen.portfolio.ui.views.payments;

import java.text.DateFormatSymbols;
import java.util.Arrays;
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

public class PaymentsPerMonthChartBuilder implements PaymentsChartBuilder
{
    private static class DividendPerMonthChartToolTip extends TimelineChartToolTip
    {
        public DividendPerMonthChartToolTip(Chart chart)
        {
            super(chart);

            enableCategory(true);
        }

        @Override
        protected void createComposite(Composite parent)
        {
            PaymentsViewModel model = (PaymentsViewModel) getChart().getData(PaymentsViewModel.class.getSimpleName());

            int month = (Integer) getFocusedObject();
            int totalNoOfMonths = model.getNoOfMonths();

            List<Line> lines = model.getLines().stream() //
                            .filter(line -> {
                                for (int index = month; index < totalNoOfMonths; index += 12)
                                    if (line.getValue(index) != 0L)
                                        return true;
                                return false;
                            })
                            .sorted((l1, l2) -> TextUtil.compare(l1.getVehicle().getName(), l2.getVehicle().getName()))
                            .toList();

            int noOfYears = (totalNoOfMonths / 12) + (totalNoOfMonths % 12 > month ? 1 : 0);

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

                for (int m = month; m < totalNoOfMonths; m += 12)
                {
                    l = new Label(container, SWT.RIGHT);
                    l.setText(Values.Amount.format(line.getValue(m)));
                    GridDataFactory.fillDefaults().align(SWT.END, SWT.FILL).applyTo(l);
                }
            });

            if (model.usesConsolidateRetired())
            {
                Label lSumRetired = new Label(container, SWT.NONE);
                lSumRetired.setText(Messages.LabelPaymentsConsolidateRetired);

                for (int m = month; m < totalNoOfMonths; m += 12)
                {
                    ColoredLabel cl = new ColoredLabel(container, SWT.RIGHT);
                    cl.setText(Values.Amount.format(model.getSumRetired().getValue(m)));
                    GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).applyTo(cl);
                }
            }

            Label lSum = new Label(container, SWT.NONE);
            lSum.setText(Messages.ColumnSum);

            for (int m = month; m < totalNoOfMonths; m += 12)
            {
                ColoredLabel cl = new ColoredLabel(container, SWT.RIGHT);
                cl.setBackdropColor(((IBarSeries) getChart().getSeriesSet().getSeries()[m / 12]).getBarColor());
                cl.setText(Values.Amount.format(model.getSum().getValue(m)));
                GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).applyTo(cl);
            }

        }
    }

    @Override
    public String getLabel()
    {
        return Messages.LabelPaymentsPerMonth;
    }

    @Override
    public int getTabIndex()
    {
        return 3;
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

        // format symbols returns 13 values as some calendars have 13 months
        xAxis.setCategorySeries(Arrays.copyOfRange(new DateFormatSymbols().getShortMonths(), 0, 12));

        IAxis yAxis = chart.getAxisSet().getYAxis(0);
        yAxis.getTitle().setVisible(false);
        yAxis.getTick().setVisible(true);
        yAxis.setPosition(Position.Secondary);
        yAxis.getTick().setFormat(new ThousandsNumberFormat());

        new DividendPerMonthChartToolTip(chart);
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
                series[ii] = model.getSum().getValue(index + ii) / Values.Amount.divider();
            barSeries.setYSeries(series);

            barSeries.setBarColor(PaymentsColors.getColor(year));
            barSeries.setBarPadding(25);
        }

    }
}
