package name.abuchen.portfolio.ui.util.chart;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.csv.CSVPrinter;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swtchart.ISeries;

import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.AbstractCSVExporter;

public class TimelineChartCSVExporter extends AbstractCSVExporter
{
    private final TimelineChart chart;

    private Set<String> discontinousSeries = new HashSet<>();

    private NumberFormat valueFormat = new DecimalFormat("#,##0.00"); //$NON-NLS-1$

    public TimelineChartCSVExporter(TimelineChart viewer)
    {
        this.chart = viewer;
    }

    public void setValueFormat(NumberFormat valueFormat)
    {
        this.valueFormat = valueFormat;
    }

    public void addDiscontinousSeries(String seriesId)
    {
        discontinousSeries.add(seriesId);
    }

    @Override
    protected Shell getShell()
    {
        return chart.getShell();
    }

    @Override
    protected void writeToFile(File file) throws IOException
    {
        try (CSVPrinter printer = new CSVPrinter(
                        new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8), STRATEGY))
        {
            ISeries<?>[] series = chart.getSeriesSet().getSeries();
            int seriescounter = 0;

            // write header
            printer.print(Messages.ColumnDate);
            for (ISeries<?> s : series)
            {
                if (s.isVisibleInLegend())
                {
                    printer.print(s.getDescription() != null ? s.getDescription() : s.getId());
                    seriescounter++;
                }
            }
            printer.println();

            int j = 0;
            ISeries<?>[] seriesToExport = new ISeries[seriescounter];
            for (int i = 0; i < series.length; i++)
            {
                // the goal is to NOT export virtual series such as the positive
                // and negative colored area in Securities Account chart
                if (series[i].isVisibleInLegend())
                {
                    seriesToExport[j] = series[i];
                    j++;
                }
            }

            // write body
            var dataModel = (TimelineSeriesModel) seriesToExport[0].getDataModel();
            var dateSeries = dataModel.getXDateSeries();

            SeriesAdapter[] adapters = new SeriesAdapter[seriesToExport.length];
            for (int ii = 0; ii < seriesToExport.length; ii++)
            {
                if (discontinousSeries.contains(seriesToExport[ii].getId()))
                    adapters[ii] = new DiscontinousAdapter(seriesToExport[ii]);
                else
                    adapters[ii] = new DefaultAdapter(seriesToExport[ii]);
            }

            for (int line = 0; line < dateSeries.length; line++)
            {
                printer.print(dateSeries[line].toString()); // ISO format

                for (int col = 0; col < adapters.length; col++)
                    printer.print(adapters[col].format(dateSeries[line], line));

                printer.println();
            }
        }
    }

    private interface SeriesAdapter
    {
        String format(LocalDate date, int line);
    }

    private class DefaultAdapter implements SeriesAdapter
    {
        private double[] values;

        public DefaultAdapter(ISeries<?> series)
        {
            this.values = series.getYSeries();
        }

        @Override
        public String format(LocalDate date, int line)
        {
            // benchmark data series might not have all values
            if (line >= values.length)
                return ""; //$NON-NLS-1$

            return valueFormat.format(values[line]);
        }
    }

    private class DiscontinousAdapter implements SeriesAdapter
    {
        private LocalDate[] dates;
        private double[] values;

        private int next = 0;

        public DiscontinousAdapter(ISeries<?> series)
        {
            var dataModel = (TimelineSeriesModel) series.getDataModel();
            this.dates = dataModel.getXDateSeries();
            this.values = series.getYSeries();
        }

        @Override
        public String format(LocalDate date, int line)
        {
            if (next >= dates.length)
                return ""; //$NON-NLS-1$

            if (!date.equals(dates[next]))
                return ""; //$NON-NLS-1$

            return valueFormat.format(values[next++]);
        }
    }
}
