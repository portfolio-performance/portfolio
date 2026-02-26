package name.abuchen.portfolio.ui.views.payments;

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

public class PaymentsPerQuarterChartBuilder implements PaymentsChartBuilder
{
    private static class DividendPerQuarterChartToolTip extends TimelineChartToolTip
    {
        private Consumer<TabularDataSource> selectionListener;

        public DividendPerQuarterChartToolTip(Chart chart, Consumer<TabularDataSource> selectionListener)
        {
            super(chart);
            this.selectionListener = selectionListener;

            enableCategory(true);
        }

        @Override
        protected void createComposite(Composite parent)
        {
            PaymentsViewModel model = (PaymentsViewModel) getChart().getData(PaymentsViewModel.class.getSimpleName());

            int quarter = (Integer) getFocusedObject();

            IAxis xAxis = getChart().getAxisSet().getXAxes()[0];
            TabularDataSource source = new TabularDataSource(
                            Messages.LabelPaymentsPerQuarter + " - " + xAxis.getCategorySeries()[quarter], //$NON-NLS-1$
                            builder -> buildTabularData(model, quarter, builder));

            source.createPlainComposite(parent);

            selectionListener.accept(source);
        }

        private void buildTabularData(PaymentsViewModel model, int quarter, TabularDataSource.Builder builder)
        {
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
                for (int m = quarter * 3; m < totalNoOfMonths; m += 12)
                {
                    int mLimit = m + 3;
                    long value = 0;
                    for (int mQuarter = m; mQuarter < mLimit && mQuarter < totalNoOfMonths; mQuarter += 1)
                        value += line.getValue(mQuarter);
                    row[index++] = value;
                }
                builder.addRow(row);
            });

            if (model.usesConsolidateRetired())
            {
                Object[] row = new Object[noOfYears + 1];
                row[0] = Messages.LabelPaymentsConsolidateRetired;

                int index = 1;
                for (int m = quarter * 3; m < totalNoOfMonths; m += 12)
                {
                    int mLimit = m + 3;
                    long value = 0;
                    for (int mQuarter = m; mQuarter < mLimit && mQuarter < totalNoOfMonths; mQuarter += 1)
                        value += model.getSumRetired().getValue(mQuarter);
                    row[index++] = value;
                }
                builder.addRow(row);
            }

            Object[] row = new Object[noOfYears + 1];
            row[0] = Messages.ColumnSum;

            int index = 1;
            for (int m = quarter * 3; m < totalNoOfMonths; m += 12)
            {
                int mLimit = m + 3;
                long value = 0;
                for (int mQuarter = m; mQuarter < mLimit && mQuarter < totalNoOfMonths; mQuarter += 1)
                    value += model.getSum().getValue(mQuarter);
                row[index++] = value;
            }
            builder.setFooter(row);
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
    public void configure(Chart chart, Consumer<TabularDataSource> selectionListener)
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

        new DividendPerQuarterChartToolTip(chart, selectionListener);

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

            var barSeries = (IBarSeries<?>) chart.getSeriesSet().createSeries(SeriesType.BAR, String.valueOf(year));

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
