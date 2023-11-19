package name.abuchen.portfolio.ui.views.payments;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.swtchart.Chart;
import org.swtchart.IAxis;
import org.swtchart.IAxis.Position;
import org.swtchart.IBarSeries;
import org.swtchart.ISeries.SeriesType;
import org.swtchart.LineStyle;

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

    /**
     * Creates bar series in the provided chart to visualize monthly payment
     * data for each year. The years are automatically sorted in ascending order
     * using a TreeMap
     */
    @Override
    public void createSeries(Chart chart, PaymentsViewModel model)
    {
        chart.setData(PaymentsViewModel.class.getSimpleName(), model);

        updateCategorySeries(chart, model);

        int startYear = model.getStartYear();

        // Use a TreeMap to automatically sort the years
        Map<Integer, IBarSeries> yearToSeries = new TreeMap<>();

        // Iterate over the months in the model
        for (int index = 0; index < model.getNoOfMonths(); index += 12)
        {
            int year = startYear + (index / 12);

            long total = 0;

            // Calculate the total for the current year
            int months = Math.min(12, model.getNoOfMonths() - index);
            for (int ii = 0; ii < months; ii++)
                total += model.getSum().getValue(index + ii);

            // Create or retrieve the data series for the current year
            IBarSeries barSeries = yearToSeries.computeIfAbsent(year, //
                            k -> {
                                IBarSeries series = (IBarSeries) chart.getSeriesSet() //
                                                .createSeries(SeriesType.BAR, String.valueOf(k));
                                series.setBarColor(PaymentsColors.getColor(k));
                                series.setBarPadding(25);
                                series.enableStack(true);
                                return series;
                            });

            double[] seriesX = new double[LocalDate.now().getYear() - startYear + 1];
            seriesX[year - startYear] = total / Values.Amount.divider();

            barSeries.setYSeries(seriesX);
        }
    }

    /**
     * Updates the category series on the X-axis with labels for each year.
     */
    private void updateCategorySeries(Chart chart, PaymentsViewModel model)
    {
        int startYear = model.getStartYear();
        int numYears = LocalDate.now().getYear() - startYear + 1;

        String[] labels = new String[numYears];

        for (int i = 0; i < numYears; i++)
            labels[i] = String.format("%02d", (startYear + i) % 100); //$NON-NLS-1$

        chart.getAxisSet().getXAxis(0).setCategorySeries(labels);
    }
}
