package name.abuchen.portfolio.ui.views.dividends;

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
import name.abuchen.portfolio.ui.views.dividends.DividendsViewModel.Line;
import name.abuchen.portfolio.util.TextUtil;

public class DividendsPerQuarterChartTab extends AbstractChartTab
{
    
    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy"); //$NON-NLS-1$

    private class DividendPerQuarterToolTip extends TimelineChartToolTip
    {
        private DividendsViewModel model;

        public DividendPerQuarterToolTip(Chart chart, DividendsViewModel model)
        {
            super(chart);

            this.model = model;
        }

        @Override
        protected void createComposite(Composite parent)
        {
            final int year = (Integer) getFocusedObject();
            int totalNoOfMonths = model.getNoOfMonths();

            IBarSeries barSeries = (IBarSeries) getChart().getSeriesSet().getSeries()[0];

            List<Line> lines = model.getLines().stream() //
                            .filter(line -> {
                                for (int index = year * 12; index < (year + 1) * 12
                                                && index < totalNoOfMonths; index += 1)
                                    if (line.getValue(index) != 0L)
                                        return true;
                                return false;
                            })
                            .sorted((l1, l2) -> l1.getVehicle().getName()
                                            .compareToIgnoreCase(l2.getVehicle().getName()))
                            .collect(Collectors.toList());

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
            label.setText(String.valueOf(model.getStartYear() + year));
            GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).applyTo(label);

            lines.forEach(line -> {
                Label l = new Label(container, SWT.NONE);
                l.setForeground(foregroundColor);
                l.setText(TextUtil.tooltip(line.getVehicle().getName()));

                long value = 0;
                for (int m = year * 12; m < (year + 1) * 12 && m < totalNoOfMonths; m += 1)
                    value += line.getValue(m);

                l = new Label(container, SWT.RIGHT);
                l.setForeground(foregroundColor);
                l.setText(Values.Amount.format(value));
                GridDataFactory.fillDefaults().align(SWT.END, SWT.FILL).applyTo(l);
            });

            Label l = new Label(container, SWT.NONE);
            l.setForeground(foregroundColor);
            l.setText(Messages.ColumnSum);

            long value = 0;
            for (int m = year * 12; m < (year + 1) * 12 && m < totalNoOfMonths; m += 1)
                value += model.getSum().getValue(m);

            l = new Label(container, SWT.RIGHT);
            l.setBackground(barSeries.getBarColor());
            l.setForeground(Colors.getTextColor(barSeries.getBarColor()));
            l.setText(Values.Amount.format(value));
        }
    }

    @Override
    public String getLabel()
    {
        return Messages.LabelDividendsPerQuarter;
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
         * The number of month in a quarter.
         * 
         * While most people will know this, I prefer named variables over the occurrence of magic
         * numbers in the code.
         */
        int monthInQuarter = 3;
        
        // How many quarters we are about to display. We show every started quarter, hence the Math.ceil
        int nQuarters = (int) Math.ceil((double)nMonths / (double)monthInQuarter);

        String[] labels = new String[nQuarters];
        
        for (int quarter = 0; quarter < nQuarters; quarter++) 
        {
            // the fifth total quarter is the first quarter in the corresponding year
            int quarterWithinYear = (quarter % 4) + 1;
            
            // The caption looks like "Q<quarter within the year> <year>"
            String label = String.format("Q%d %s", quarterWithinYear, formatter.format(date));
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
         * The number of month in a quarter.
         * 
         * While most people will know this, I prefer named variables over the occurrence of magic
         * numbers in the code.
         */
        int monthInQuarter = 3;
        
        // How many quarters we are about to display. We show every started quarter, hence the Math.ceil
        int nQuarters = (int) Math.ceil((double)nMonths / (double)monthInQuarter);
        
        double[] series = new double[nQuarters];
        
        int quarterBeginIndex = 0;
        int quarterEndIndex = Math.min(monthInQuarter, nMonths);
        
        for (int quarter = 0; quarter < nQuarters; quarter++) 
        {

            
            double quarterDividends = 0;
            
            for (int i=quarterBeginIndex; i < quarterEndIndex; i++) 
            {
                 
                quarterDividends += model.getSum().getValue(i);
            }
            
            series[quarter] = quarterDividends / Values.Amount.divider();
            
            System.out.println(Values.Amount.divider());
            
            // Starting from here, we make sure to step into the next quarter
            quarterBeginIndex = Math.min(quarterBeginIndex+monthInQuarter,nMonths);
            quarterEndIndex = Math.min(quarterEndIndex+monthInQuarter, nMonths);
                           
        }

//        int startYear = model.getStartYear();
//        int nYears = LocalDate.now().getYear() - startYear + 1;
//        int nQuarters = nYears * 4;
//        
//        double[] series = new double[nQuarters];
//        
//
//        for (int index = 0; index < model.getNoOfMonths(); index += 12)
//        {
//            int year = (index / 12);
//
//            long total = 0;
//
//            int months = Math.min(12, model.getNoOfMonths() - index);
//            for (int ii = 0; ii < months; ii++)
//                total += model.getSum().getValue(index + ii);
//
//            series[year] = total / Values.Amount.divider();
//        }

        IBarSeries barSeries = (IBarSeries) getChart().getSeriesSet().createSeries(SeriesType.BAR, getLabel());
        barSeries.setYSeries(series);
        barSeries.setBarColor(Colors.DARK_BLUE);
    }
}
