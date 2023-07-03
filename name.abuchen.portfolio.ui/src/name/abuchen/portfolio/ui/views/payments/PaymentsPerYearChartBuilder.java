package name.abuchen.portfolio.ui.views.payments;

import java.time.LocalDate;
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
import name.abuchen.portfolio.ui.util.Colors;
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

            TabularDataSource source = new TabularDataSource(Messages.LabelPaymentsPerYear,
                            builder -> buildTabularData(model, builder));

            source.createPlainComposite(parent);

            selectionListener.accept(source);
        }

        private void buildTabularData(PaymentsViewModel model, TabularDataSource.Builder builder)
        {
            int totalNoOfMonths = model.getNoOfMonths();
            int totalNoOfYears = LocalDate.now().getYear() - model.getStartYear() + 1;

            builder.addColumns(new Column(Messages.ColumnSecurity, SWT.LEFT, 220).withLogo());
            for (int year = 0; year < totalNoOfYears; year++)
            {
                builder.addColumns(new Column(String.valueOf(model.getStartYear() + year))
                                .withBackgroundColor(PaymentsColors.getColor(model.getStartYear() + year))
                                .withFormatter(cell -> Values.Amount.format((long) cell)));
            }

            model.getLines().stream()
                            .sorted((l1, l2) -> TextUtil.compare(l1.getVehicle().getName(), l2.getVehicle().getName()))
                            .forEach(line -> {
                                Object[] row = new Object[totalNoOfYears + 1];
                                row[0] = line.getVehicle();
                                fillInRow(totalNoOfYears, totalNoOfMonths, line, row);
                                builder.addRow(row);

                            });

            if (model.usesConsolidateRetired())
            {
                Object[] row = new Object[totalNoOfYears + 1];
                row[0] = Messages.LabelPaymentsConsolidateRetired;
                fillInRow(totalNoOfYears, totalNoOfMonths, model.getSumRetired(), row);
                builder.addRow(row);
            }

            Object[] sum = new Object[totalNoOfYears + 1];
            sum[0] = Messages.ColumnSum;
            fillInRow(totalNoOfYears, totalNoOfMonths, model.getSum(), sum);
            builder.setFooter(sum);
        }

        private void fillInRow(int totalNoOfYears, int totalNoOfMonths, Line line, Object[] row)
        {
            for (int year = 0; year < totalNoOfYears; year++)
            {
                long value = 0;
                for (int month = year * 12; month < (year + 1) * 12 && month < totalNoOfMonths; month += 1)
                    value += line.getValue(month);

                row[year + 1] = value;
            }
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
