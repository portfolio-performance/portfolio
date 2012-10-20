package name.abuchen.portfolio.ui.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import name.abuchen.portfolio.ui.Messages;

import org.apache.commons.csv.CSVPrinter;
import org.eclipse.swt.widgets.Control;
import org.swtchart.ISeries;

public class TimelineChartCSVExporter extends AbstractCSVExporter
{
    private final TimelineChart chart;

    private DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd"); //$NON-NLS-1$
    private NumberFormat valueFormat = new DecimalFormat("#,##0.00"); //$NON-NLS-1$

    public TimelineChartCSVExporter(TimelineChart viewer)
    {
        this.chart = viewer;
    }

    public void setDateFormat(DateFormat dateFormat)
    {
        this.dateFormat = dateFormat;
    }

    public void setValueFormat(NumberFormat valueFormat)
    {
        this.valueFormat = valueFormat;
    }

    @Override
    protected Control getControl()
    {
        return chart;
    }

    @Override
    protected void writeToFile(File file) throws IOException
    {
        Writer writer = new OutputStreamWriter(new FileOutputStream(file), Charset.forName("UTF-8")); //$NON-NLS-1$

        try
        {
            CSVPrinter printer = new CSVPrinter(writer);
            printer.setStrategy(STRATEGY);

            ISeries[] series = chart.getSeriesSet().getSeries();

            // write header
            printer.print(Messages.ColumnDate);
            for (ISeries s : series)
                printer.print(s.getId());
            printer.println();

            // write body
            Date[] dateSeries = series[0].getXDateSeries();

            double[][] valueSeries = new double[series.length][];
            for (int ii = 0; ii < series.length; ii++)
                valueSeries[ii] = series[ii].getYSeries();

            for (int line = 0; line < dateSeries.length; line++)
            {
                printer.print(dateFormat.format(dateSeries[line]));

                for (int col = 0; col < valueSeries.length; col++)
                    printer.print(valueFormat.format(valueSeries[col][line]));

                printer.println();
            }
        }
        finally
        {
            writer.close();
        }
    }
}
