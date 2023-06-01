package name.abuchen.portfolio.ui.views.payments;

import java.time.LocalDate;
import java.util.List;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
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
import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.ui.util.chart.TimelineChartToolTip;
import name.abuchen.portfolio.ui.util.format.ThousandsNumberFormat;
import name.abuchen.portfolio.ui.util.swt.ColoredLabel;
import name.abuchen.portfolio.ui.views.payments.PaymentsViewModel.Line;
import name.abuchen.portfolio.util.TextUtil;

public class PaymentsPerYearChartBuilder implements PaymentsChartBuilder
{
    private static class DividendPerYearToolTip extends TimelineChartToolTip
    {
        public DividendPerYearToolTip(Chart chart)
        {
            super(chart);

            enableCategory(true);
        }

        @Override
        protected void createComposite(Composite parent)
        {
            PaymentsViewModel model = (PaymentsViewModel) getChart().getData(PaymentsViewModel.class.getSimpleName());

            final int year = (Integer) getFocusedObject();
            int totalNoOfMonths = model.getNoOfMonths();

            Color barColor = Colors.DARK_BLUE;

            IBarSeries barSeries = (IBarSeries) getChart().getSeriesSet().getSeries()[0];
            if (barSeries.isStackEnabled())
            {
                barSeries = (IBarSeries) getChart().getSeriesSet().getSeries()[year];
                barColor = barSeries.getBarColor();
            }

            List<Line> lines = model.getLines().stream() //
                            .filter(line -> {
                                for (int index = year * 12; index < (year + 1) * 12
                                                && index < totalNoOfMonths; index += 1)
                                    if (line.getValue(index) != 0L)
                                        return true;
                                return false;
                            })
                            .sorted((l1, l2) -> TextUtil.compare(l1.getVehicle().getName(), l2.getVehicle().getName()))
                            .toList();

            final Composite container = new Composite(parent, SWT.NONE);
            container.setBackgroundMode(SWT.INHERIT_FORCE);
            GridLayoutFactory.swtDefaults().numColumns(2).applyTo(container);

            Label topLeft = new Label(container, SWT.NONE);
            topLeft.setText(Messages.ColumnSecurity);

            ColoredLabel label = new ColoredLabel(container, SWT.CENTER);
            label.setBackdropColor(barColor);
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

            if (model.usesConsolidateRetired())
            {
                Label lSumRetired = new Label(container, SWT.NONE);
                lSumRetired.setText(Messages.LabelPaymentsConsolidateRetired);

                long value = 0;
                for (int m = year * 12; m < (year + 1) * 12 && m < totalNoOfMonths; m += 1)
                    value += model.getSumRetired().getValue(m);

                ColoredLabel cl = new ColoredLabel(container, SWT.RIGHT);
                cl.setText(Values.Amount.format(value));
                GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).applyTo(cl);
            }

            Label lSum = new Label(container, SWT.NONE);
            lSum.setText(Messages.ColumnSum);

            long value = 0;
            for (int m = year * 12; m < (year + 1) * 12 && m < totalNoOfMonths; m += 1)
                value += model.getSum().getValue(m);

            ColoredLabel cl = new ColoredLabel(container, SWT.RIGHT);
            cl.setBackdropColor(barColor);
            cl.setText(Values.Amount.format(value));
            GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).applyTo(cl);
        }
    }

    @Override
    public String getLabel()
    {
        return Messages.LabelPaymentsPerYear;
    }

    @Override
    public int getTabIndex()
    {
        return 5;
    }

    @Override
    public void configure(Chart chart)
    {
        IAxis xAxis = chart.getAxisSet().getXAxis(0);
        xAxis.getTick().setVisible(true);
        xAxis.getTitle().setVisible(false);
        xAxis.getTitle().setText(Messages.ColumnYear);
        xAxis.getGrid().setStyle(LineStyle.NONE);
        xAxis.enableCategory(true);

        IAxis yAxis = chart.getAxisSet().getYAxis(0);
        yAxis.getTitle().setVisible(false);
        yAxis.getTick().setVisible(true);
        yAxis.setPosition(Position.Secondary);
        yAxis.getTick().setFormat(new ThousandsNumberFormat());

        new DividendPerYearToolTip(chart);
    }

    @Override
    public void createSeries(Chart chart, PaymentsViewModel model)
    {
        chart.setData(PaymentsViewModel.class.getSimpleName(), model);

        updateCategorySeries(chart, model);

        int startYear = model.getStartYear();

        double[] series = new double[LocalDate.now().getYear() - startYear + 1];

        boolean hasNegativeNumber = false;

        for (int index = 0; index < model.getNoOfMonths(); index += 12)
        {
            int year = (index / 12);

            long total = 0;

            int months = Math.min(12, model.getNoOfMonths() - index);
            for (int ii = 0; ii < months; ii++)
                total += model.getSum().getValue(index + ii);

            series[year] = total / Values.Amount.divider();

            if (total < 0L)
                hasNegativeNumber = true;
        }

        if (hasNegativeNumber)
        {
            IBarSeries barSeries = (IBarSeries) chart.getSeriesSet().createSeries(SeriesType.BAR,
                            Messages.LabelPaymentsPerYear);
            barSeries.setYSeries(series);
            barSeries.setBarColor(Colors.DARK_BLUE);
        }
        else
        {
            for (int i = 0; i < series.length; i++)
            {
                int year = model.getStartYear() + i;
                IBarSeries barSeries = (IBarSeries) chart.getSeriesSet().createSeries(SeriesType.BAR,
                                String.valueOf(year));

                double[] seriesX = new double[LocalDate.now().getYear() - startYear + 1];
                seriesX[i] = series[i];

                barSeries.setYSeries(seriesX);

                barSeries.setBarColor(PaymentsColors.getColor(year));
                barSeries.setBarPadding(25);
                barSeries.enableStack(true);
            }
        }
    }

    private void updateCategorySeries(Chart chart, PaymentsViewModel model)
    {
        int startYear = model.getStartYear();
        String[] labels = new String[LocalDate.now().getYear() - startYear + 1];
        for (int ii = 0; ii < labels.length; ii++)
            labels[ii] = String.format("%02d", (startYear + ii) % 100); //$NON-NLS-1$
        chart.getAxisSet().getXAxis(0).setCategorySeries(labels);
    }
}
