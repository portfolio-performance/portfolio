package name.abuchen.portfolio.ui.util;

import java.time.format.DateTimeFormatterBuilder;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

import org.apache.commons.csv.CSVPrinter;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.util.viewers.SharesLabelProvider;
import name.abuchen.portfolio.ui.Messages;

public class TableViewerCSVExporter extends AbstractCSVExporter
{
    private final TableViewer viewer;
    private final boolean selectionOnly;

    public TableViewerCSVExporter(TableViewer viewer)
    {
        this(viewer, false);
    }

    public TableViewerCSVExporter(TableViewer viewer, boolean selectionOnly)
    {
        this.viewer = viewer;
        this.selectionOnly = selectionOnly;
    }

    @Override
    protected Shell getShell()
    {
        return viewer.getTable().getShell();
    }

    @Override
    protected void writeToFile(File file) throws IOException
    {
        try (CSVPrinter printer = new CSVPrinter(
                        new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8), STRATEGY))
        {
            int columnCount = viewer.getTable().getColumnCount();

            // write header
            for (TableColumn column : viewer.getTable().getColumns())
                printer.print(column.getText());
            printer.println();

            // check for "special" label provider
            SharesLabelProvider[] labelProvider = new SharesLabelProvider[columnCount];
            for (int ii = 0; ii < columnCount; ii++)
            {
                CellLabelProvider p = viewer.getLabelProvider(ii);
                if (p instanceof SharesLabelProvider)
                    labelProvider[ii] = (SharesLabelProvider) p;
            }

            // write body
            TableItem[] items = selectionOnly ? viewer.getTable().getSelection() : viewer.getTable().getItems();
            for (TableItem item : items)
            {
                for (int ii = 0; ii < columnCount; ii++)
                {
                    if (labelProvider[ii] != null)
                    {
                        Long value = labelProvider[ii].getValue(item.getData());
                        printer.print(value != null ? Values.Share.format(value) : ""); //$NON-NLS-1$
                    }
                    else
                    {
                        // For columns that contain date values, format them consistently, so they'll sort chronologically
                        // when opened in a spreadsheet (whether or not the value is a date only, or a date w/ a time)
                        String columnHeader = viewer.getTable().getColumns()[ii].getText();
                        String columnValue = item.getText(ii);
                        boolean isDateColumn = columnHeader.equals(Messages.ColumnStartDate) || columnHeader.equals(Messages.ColumnEndDate);
                        if(isDateColumn)
                        {
                            try {
                                DateTimeFormatter formatter =  
                                                new DateTimeFormatterBuilder().appendPattern("MMM d, uuuu[, h:m a]")
                                                .parseDefaulting(ChronoField.CLOCK_HOUR_OF_AMPM, 12)
                                                .parseDefaulting(ChronoField.MINUTE_OF_HOUR, 0)
                                                .parseDefaulting(ChronoField.SECOND_OF_MINUTE, 0)
                                                .parseDefaulting(ChronoField.AMPM_OF_DAY, 0)
                                                .toFormatter();
                                
                                LocalDateTime dateTime = LocalDateTime.parse(columnValue, formatter);
                                columnValue = dateTime.toString();
                            }
                            catch(Exception e)
                            {
                                //Do nothing
                            }
                        }
                        printer.print(columnValue);
                    }
                }
                printer.println();
            }
        }
    }
}
