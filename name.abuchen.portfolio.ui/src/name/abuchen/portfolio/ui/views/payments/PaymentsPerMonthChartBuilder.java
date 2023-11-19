package name.abuchen.portfolio.ui.views.payments;

import java.text.DateFormatSymbols;
import java.util.Arrays;
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
import name.abuchen.portfolio.ui.util.TabularDataSource.Builder;
import name.abuchen.portfolio.ui.util.TabularDataSource.Column;
import name.abuchen.portfolio.ui.util.chart.TimelineChartToolTip;
import name.abuchen.portfolio.ui.util.format.ThousandsNumberFormat;
import name.abuchen.portfolio.ui.views.payments.PaymentsViewModel.Line;
import name.abuchen.portfolio.util.TextUtil;

public class PaymentsPerMonthChartBuilder implements PaymentsChartBuilder
{
    private static class DividendPerMonthChartToolTip extends TimelineChartToolTip
    {
        private Consumer<TabularDataSource> selectionListener;

        public DividendPerMonthChartToolTip(Chart chart, Consumer<TabularDataSource> selectionListener)
        {
            super(chart);
            this.selectionListener = selectionListener;

            enableCategory(true);
        }

        @Override
        protected void createComposite(Composite parent)
        {
            PaymentsViewModel model = (PaymentsViewModel) getChart().getData(PaymentsViewModel.class.getSimpleName());

            int month = (Integer) getFocusedObject();

            IAxis xAxis = getChart().getAxisSet().getXAxes()[0];
            TabularDataSource source = new TabularDataSource(
                            Messages.LabelPaymentsPerMonth + " - " + xAxis.getCategorySeries()[month], //$NON-NLS-1$
                            builder -> buildTabularData(model, month, builder));

            source.createPlainComposite(parent);

            selectionListener.accept(source);
        }

        private void buildTabularData(PaymentsViewModel model, int month, Builder builder)
        {
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

            builder.addColumns(new Column(Messages.ColumnSecurity, SWT.LEFT, 220).withLogo());
            for (int year = 0; year < noOfYears; year++)
            {
                builder.addColumns(new Column(String.valueOf(model.getStartYear() + year))
                                .withBackgroundColor(PaymentsColors.getColor(model.getStartYear() + year))
                                .withFormatter(cell -> Values.Amount.format((long) cell)));
            }

            lines.forEach(line -> {
                Object[] row = new Object[noOfYears + 1];
                row[0] = line.getVehicle();

                int index = 1;
                for (int m = month; m < totalNoOfMonths; m += 12)
                    row[index++] = line.getValue(m);

                builder.addRow(row);
            });

            if (model.usesConsolidateRetired())
            {
                Object[] row = new Object[noOfYears + 1];
                row[0] = Messages.LabelPaymentsConsolidateRetired;

                int index = 1;
                for (int m = month; m < totalNoOfMonths; m += 12)
                    row[index++] = model.getSumRetired().getValue(m);
                builder.addRow(row);
            }

            Object[] row = new Object[noOfYears + 1];
            row[0] = Messages.ColumnSum;
            int index = 1;
            for (int m = month; m < totalNoOfMonths; m += 12)
                row[index++] = model.getSum().getValue(m);
            builder.setFooter(row);
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
    public void configure(Chart chart, Consumer<TabularDataSource> selectionListener)
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

        chart.setData(DividendPerMonthChartToolTip.class.getSimpleName(),
                        new DividendPerMonthChartToolTip(chart, selectionListener));
    }

    /**
     * Creates bar series for each year in the provided chart, with bars
     * representing monthly payment data. Uses a TreeMap to automatically sort
     * the years in ascending order.
     */
    @Override
    public void createSeries(Chart chart, PaymentsViewModel model)
    {
        chart.setData(PaymentsViewModel.class.getSimpleName(), model);

        // Use a TreeMap to automatically sort the years
        Map<Integer, IBarSeries> yearToSeries = new TreeMap<>();

        // Iterate over the months in the model
        for (int index = 0; index < model.getNoOfMonths(); index += 12)
        {
            int year = model.getStartYear() + (index / 12);

            // Create or retrieve the data series for the current year
            IBarSeries barSeries = yearToSeries.computeIfAbsent(year, //
                            k -> {
                                IBarSeries series = (IBarSeries) chart.getSeriesSet() //
                                                .createSeries(SeriesType.BAR, String.valueOf(k));
                                series.setBarColor(PaymentsColors.getColor(k));
                                series.setBarPadding(25);
                                return series;
                            });

            int monthsToPopulate = Math.min(12, model.getNoOfMonths() - index);
            double[] series = new double[monthsToPopulate];

            // Populate the series with monthly payment values
            for (int ii = 0; ii < monthsToPopulate; ii++)
            {
                int monthIndex = index + ii;
                series[ii] = model.getSum().getValue(monthIndex) / Values.Amount.divider();
            }

            barSeries.setYSeries(series);
        }
    }
}
