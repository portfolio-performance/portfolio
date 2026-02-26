package name.abuchen.portfolio.ui.util.chart;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.NumberFormat;

import org.apache.commons.csv.CSVPrinter;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swtchart.ISeries;

import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.AbstractCSVExporter;

public class StackedChartCSVExporter extends AbstractCSVExporter
{
    private final StackedTimelineChart stackedchart;

    private NumberFormat valueFormat = new DecimalFormat("#,##0.00"); //$NON-NLS-1$

    public StackedChartCSVExporter(StackedTimelineChart stackedchart)
    {
        this.stackedchart = stackedchart;
    }

    public void setValueFormat(NumberFormat valueFormat)
    {
        this.valueFormat = valueFormat;
    }

    @Override
    protected Shell getShell()
    {
            return stackedchart.getShell();
    }

    @Override
    protected void writeToFile(File file) throws IOException
    {
        try (CSVPrinter printer = new CSVPrinter(
                        new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8), STRATEGY))
        {
            var series = stackedchart.getSeriesSet().getSeries();

            // write header
            printer.print(Messages.ColumnDate);
            for (var s : series)
                printer.print(s.getDescription() != null ? s.getDescription() : s.getId());
            printer.println();

            // write body
            SeriesAdapter[] adapters = new SeriesAdapter[series.length];
            for (int ii = 0; ii < series.length; ii++)
            {
                adapters[ii] = new DefaultAdapter(series[ii]);
            }

            String[] dateSeriesStacked = stackedchart.getAxisSet().getXAxis(0).getCategorySeries();
            for (int line = 0; line < dateSeriesStacked.length; line++)
            {
                printer.print(dateSeriesStacked[line]);

                for (int col = 0; col < adapters.length; col++)
                    printer.print(adapters[col].format(line));

                printer.println();
            }
        }
    }

    private interface SeriesAdapter
    {
        String format(int line);
    }

    private class DefaultAdapter implements SeriesAdapter
    {
        private double[] values;

        public DefaultAdapter(ISeries<?> series)
        {
            this.values = series.getYSeries();
        }

        @Override
        public String format(int line)
        {
            return valueFormat.format(values[line]);
        }
    }
}