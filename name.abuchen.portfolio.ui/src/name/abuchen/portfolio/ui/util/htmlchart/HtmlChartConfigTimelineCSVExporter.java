package name.abuchen.portfolio.ui.util.htmlchart;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.AbstractCSVExporter;

import org.apache.commons.csv.CSVPrinter;
import org.eclipse.swt.widgets.Control;

public class HtmlChartConfigTimelineCSVExporter extends AbstractCSVExporter
{
    private final HtmlChartConfigTimeline config;

    private Set<String> discontinousSeries = new HashSet<String>();

    private DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd"); //$NON-NLS-1$
    private NumberFormat valueFormat = new DecimalFormat("#,##0.00"); //$NON-NLS-1$

    public HtmlChartConfigTimelineCSVExporter(HtmlChartConfigTimeline config)
    {
        this.config = config;
    }

    public void setDateFormat(DateFormat dateFormat)
    {
        this.dateFormat = dateFormat;
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
    protected Control getControl()
    {
        return null;
    }

    @Override
    protected void writeToFile(File file) throws IOException
    {
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))
        {
            CSVPrinter printer = new CSVPrinter(writer);
            printer.setStrategy(STRATEGY);
            
            List<HtmlChartConfigTimelineSeries> series = config.series();

            // write header
            printer.print(Messages.ColumnDate);
            for (HtmlChartConfigTimelineSeries s : series)
                printer.print(s.getName());
            printer.println();

            // write body
            Date[] dateSeries = config.series().get(0).dates;

            SeriesAdapter[] adapters = new SeriesAdapter[series.size()];
            for (int ii = 0; ii < series.size(); ii++)
            {
                if (discontinousSeries.contains(series.get(ii).getName()))
                    adapters[ii] = new DiscontinousAdapter(series.get(ii));
                else
                    adapters[ii] = new DefaultAdapter(series.get(ii));
            }

            for (int line = 0; line < dateSeries.length; line++)
            {
                printer.print(dateFormat.format(dateSeries[line]));

                for (int col = 0; col < adapters.length; col++)
                    printer.print(adapters[col].format(dateSeries[line], line));

                printer.println();
            }
        }
    }

    private interface SeriesAdapter
    {
        String format(Date date, int line);
    }

    private class DefaultAdapter implements SeriesAdapter
    {
        private double[] values;

        public DefaultAdapter(HtmlChartConfigTimelineSeries series)
        {
            this.values = series.getValues();
        }

        @Override
        public String format(Date date, int line)
        {
            // benchmark data series might not have all values
            if (line >= values.length)
                return ""; //$NON-NLS-1$

            return valueFormat.format(values[line]);
        }
    }

    private class DiscontinousAdapter implements SeriesAdapter
    {
        private Date[] dates;
        private double[] values;

        private int next = 0;

        public DiscontinousAdapter(HtmlChartConfigTimelineSeries series)
        {
            this.dates = series.getDates();
            this.values = series.getValues();
        }

        @Override
        public String format(Date date, int line)
        {
            if (next >= dates.length)
                return ""; //$NON-NLS-1$

            if (date.getTime() != dates[next].getTime())
                return ""; //$NON-NLS-1$

            return valueFormat.format(values[next++]);
        }
    }
}
