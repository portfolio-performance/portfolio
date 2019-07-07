package name.abuchen.portfolio.ui.views.earnings;

import java.time.LocalDate;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.swtchart.Chart;
import org.swtchart.IBarSeries;
import org.swtchart.ISeries.SeriesType;

import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.ui.util.chart.TimelineChartToolTip;
import name.abuchen.portfolio.ui.views.earnings.EarningsViewModel.Line;
import name.abuchen.portfolio.util.TextUtil;

public class EarningsPerQuarterChartTab extends AbstractChartTab
{

    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy"); //$NON-NLS-1$

    private class DividendPerQuarterToolTip extends TimelineChartToolTip
    {
        private EarningsViewModel model;

        public DividendPerQuarterToolTip(Chart chart, EarningsViewModel model)
        {
            super(chart);

            this.model = model;
        }

        @Override
        protected void createComposite(Composite parent)
        {
            final int hoveredQuarterIndex = (Integer) getFocusedObject();
            LocalDate date = LocalDate.of(model.getStartYear(), Month.JANUARY, 1);
            date = date.plusYears(hoveredQuarterIndex / 4);
            int quarterWithinYear = (hoveredQuarterIndex % 4) + 1;

            int totalNoOfMonths = model.getNoOfMonths();

            IBarSeries barSeries = (IBarSeries) getChart().getSeriesSet().getSeries()[0];

            final Composite container = new Composite(parent, SWT.NONE);
            container.setBackgroundMode(SWT.INHERIT_FORCE);
            GridLayoutFactory.swtDefaults().numColumns(2).applyTo(container);

            Color foregroundColor = Display.getDefault().getSystemColor(SWT.COLOR_BLACK);
            container.setForeground(foregroundColor);
            container.setBackground(Colors.INFO_TOOLTIP_BACKGROUND);

            Label topLeft = new Label(container, SWT.NONE);
            topLeft.setForeground(foregroundColor);
            topLeft.setText(Messages.ColumnSecurity);

            Label label = new Label(container, SWT.CENTER);
            label.setBackground(barSeries.getBarColor());
            label.setForeground(Colors.getTextColor(barSeries.getBarColor()));
            String labelString = String.format("Q%d %s", quarterWithinYear, formatter.format(date)); //$NON-NLS-1$
            label.setText(labelString);
            GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).applyTo(label);

            final int quarterBeginIndex = 3 * hoveredQuarterIndex;
            final int quarterEndIndex = Math.min(3 * (hoveredQuarterIndex + 1), totalNoOfMonths);

            // first: filter out
            List<Line> lines = model.getLines().stream() //
                            .filter(line -> {
                                for (int index = quarterBeginIndex; index < quarterEndIndex; index += 1)
                                {
                                    if (line.getValue(index) != 0L)
                                        return true;
                                }
                                return false;
                            }).sorted((l1, l2) -> l1.getVehicle().getName() // sort
                                                                            // alphabetically
                                            .compareToIgnoreCase(l2.getVehicle().getName()))
                            .collect(Collectors.toList());

            lines.forEach(line -> {
                Label l = new Label(container, SWT.NONE);
                l.setForeground(foregroundColor);
                l.setText(TextUtil.tooltip(line.getVehicle().getName()));

                long value = 0;
                for (int index = quarterBeginIndex; index < quarterEndIndex; index += 1)
                {
                    value += line.getValue(index);
                }

                l = new Label(container, SWT.RIGHT);
                l.setForeground(foregroundColor);
                l.setText(Values.Amount.format(value));
                GridDataFactory.fillDefaults().align(SWT.END, SWT.FILL).applyTo(l);
            });

            Label l = new Label(container, SWT.NONE);
            l.setForeground(foregroundColor);
            l.setText(Messages.ColumnSum);

            // compute total sum of dividends in quarter
            long value = 0;
            for (int index = quarterBeginIndex; index < quarterEndIndex; index += 1)
            {
                value += model.getSum().getValue(index);
            }
            l = new Label(container, SWT.RIGHT);
            l.setBackground(barSeries.getBarColor());
            l.setForeground(Colors.getTextColor(barSeries.getBarColor()));
            l.setText(Values.Amount.format(value));
            GridDataFactory.fillDefaults().align(SWT.END, SWT.FILL).applyTo(l);
        }
    }

    @Override
    public String getLabel()
    {
        return Messages.LabelEarningsPerQuarter;
    }

    @Override
    protected void attachTooltipTo(Chart chart)
    {
        DividendPerQuarterToolTip toolTip = new DividendPerQuarterToolTip(chart, model);
        toolTip.enableCategory(true);
    }

    private void setCategorySeriesLabels()
    {
        LocalDate date = LocalDate.of(model.getStartYear(), Month.JANUARY, 1);

        int nMonths = model.getNoOfMonths();

        /*
         * The number of month in a quarter. While most people will know this, I
         * prefer named variables over the occurrence of magic numbers in the
         * code.
         */
        int monthInQuarter = 3;

        // How many quarters we are about to display. We show every started
        // quarter, hence the Math.ceil
        int nQuarters = (int) Math.ceil((double) nMonths / (double) monthInQuarter);

        String[] labels = new String[nQuarters];

        for (int quarter = 0; quarter < nQuarters; quarter++)
        {
            // the fifth total quarter is the first quarter in the corresponding
            // year
            int quarterWithinYear = (quarter % 4) + 1;

            // The caption looks like "Q<quarter within the year> <year>"
            String label = String.format("Q%d %s", quarterWithinYear, formatter.format(date)); //$NON-NLS-1$
            labels[quarter] = label;

            // every four quarters we need to switch to the next year
            if (quarterWithinYear == 4)
            {
                date = date.plusYears(1);
            }

        }

        getChart().getAxisSet().getXAxis(0).setCategorySeries(labels);
    }

    @Override
    protected void createSeries()
    {
        setCategorySeriesLabels();

        int nMonths = model.getNoOfMonths();

        /*
         * The number of month in a quarter. While most people will know this, I
         * prefer named variables over the occurrence of magic numbers in the
         * code.
         */
        int monthInQuarter = 3;

        // How many quarters we are about to display. We show every started
        // quarter, hence the Math.ceil
        int nQuarters = (int) Math.ceil((double) nMonths / (double) monthInQuarter);

        double[] series = new double[nQuarters];

        int quarterBeginIndex = 0;
        int quarterEndIndex = Math.min(monthInQuarter, nMonths);

        for (int quarter = 0; quarter < nQuarters; quarter++)
        {

            double quarterDividends = 0;

            for (int i = quarterBeginIndex; i < quarterEndIndex; i++)
            {

                quarterDividends += model.getSum().getValue(i);
            }

            series[quarter] = quarterDividends / Values.Amount.divider();

            // Starting from here, we make sure to step into the next quarter
            quarterBeginIndex = Math.min(quarterBeginIndex + monthInQuarter, nMonths);
            quarterEndIndex = Math.min(quarterEndIndex + monthInQuarter, nMonths);

        }

        IBarSeries barSeries = (IBarSeries) getChart().getSeriesSet().createSeries(SeriesType.BAR, getLabel());
        barSeries.setYSeries(series);
        barSeries.setBarColor(Colors.DARK_BLUE);
    }
}
