package name.abuchen.portfolio.ui.views.payments;

import java.time.LocalDate;
import java.util.List;
import java.util.function.Consumer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swtchart.Chart;
import org.eclipse.swtchart.IAxis;
import org.eclipse.swtchart.IAxis.Position;
import org.eclipse.swtchart.IBarSeries;
import org.eclipse.swtchart.ISeries.SeriesType;
import org.eclipse.swtchart.LineStyle;

import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.TabularDataSource;
import name.abuchen.portfolio.ui.util.TabularDataSource.Column;
import name.abuchen.portfolio.ui.util.chart.TimelineChartToolTip;
import name.abuchen.portfolio.ui.util.format.ThousandsNumberFormat;
import name.abuchen.portfolio.ui.views.payments.PaymentsViewModel.Line;
import name.abuchen.portfolio.util.TextUtil;

public class PaymentsPerYearChartBuilder implements PaymentsChartBuilder
{
    private static class DividendPerYearToolTip extends TimelineChartToolTip
    {
        private Consumer<TabularDataSource> selectionListener;

        public DividendPerYearToolTip(Chart chart, Consumer<TabularDataSource> selectionListener)
        {
            super(chart);
            this.selectionListener = selectionListener;

            enableCategory(true);
        }

        @Override
        protected void createComposite(Composite parent)
        {
            PaymentsViewModel model = (PaymentsViewModel) getChart().getData(PaymentsViewModel.class.getSimpleName());

            int year = (Integer) getFocusedObject();

            TabularDataSource source = new TabularDataSource(
                            Messages.LabelPaymentsPerYear + " - " + (model.getStartYear() + year), //$NON-NLS-1$
                            builder -> buildTabularData(model, year, builder));

            source.createPlainComposite(parent);

            selectionListener.accept(source);
        }

        private void buildTabularData(PaymentsViewModel model, int year, TabularDataSource.Builder builder)
        {
            int totalNoOfMonths = model.getNoOfMonths();

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

            builder.addColumns( //
                            new Column(Messages.ColumnSecurity, SWT.LEFT, 220).withLogo(),
                            new Column(String.valueOf(model.getStartYear() + year))
                                            .withBackgroundColor(PaymentsColors.getColor(model.getStartYear() + year))
                                            .withFormatter(cell -> Values.Amount.format((long) cell)));

            lines.forEach(line -> {
                long value = 0;
                for (int m = year * 12; m < (year + 1) * 12 && m < totalNoOfMonths; m += 1)
                    value += line.getValue(m);

                builder.addRow(line.getVehicle(), value);
            });

            if (model.usesConsolidateRetired())
            {
                long value = 0;
                for (int m = year * 12; m < (year + 1) * 12 && m < totalNoOfMonths; m += 1)
                    value += model.getSumRetired().getValue(m);

                builder.addRow(Messages.LabelPaymentsConsolidateRetired, value);
            }

            long value = 0;
            for (int m = year * 12; m < (year + 1) * 12 && m < totalNoOfMonths; m += 1)
                value += model.getSum().getValue(m);
            builder.setFooter(Messages.ColumnSum, value);
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
    public void configure(Chart chart, Consumer<TabularDataSource> selectionListener)
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

        new DividendPerYearToolTip(chart, selectionListener);
    }

    @Override
    public void createSeries(Chart chart, PaymentsViewModel model)
    {
        chart.setData(PaymentsViewModel.class.getSimpleName(), model);

        updateCategorySeries(chart, model);

        int startYear = model.getStartYear();

        double[] series = new double[LocalDate.now().getYear() - startYear + 1];

        for (int index = 0; index < model.getNoOfMonths(); index += 12)
        {
            int year = (index / 12);

            long total = 0;

            int months = Math.min(12, model.getNoOfMonths() - index);
            for (int ii = 0; ii < months; ii++)
                total += model.getSum().getValue(index + ii);

            series[year] = total / Values.Amount.divider();
        }

        for (int i = 0; i <= series.length - 1; i++)
            {
                int year = model.getStartYear() + i;
                var barSeries = (IBarSeries<?>) chart.getSeriesSet().createSeries(SeriesType.BAR, String.valueOf(year));

                double[] seriesX = new double[LocalDate.now().getYear() - startYear + 1];
                seriesX[i] = series[i];

                barSeries.setYSeries(seriesX);

                barSeries.setBarColor(PaymentsColors.getColor(year));
                barSeries.setBarPadding(25);
                barSeries.setBarOverlay(true);
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
