package name.abuchen.portfolio.ui.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.apache.commons.csv.CSVPrinter;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.util.viewers.DateLabelProvider;
import name.abuchen.portfolio.ui.util.viewers.DateTimeLabelProvider;
import name.abuchen.portfolio.ui.util.viewers.SharesLabelProvider;

public class TableViewerCSVExporter extends AbstractCSVExporter
{
    private static final DateTimeFormatter DATETIME_FORMAT_PATTERN = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"); //$NON-NLS-1$
    private static final DateTimeFormatter DATE_FORMAT_PATTERN = DateTimeFormatter.ofPattern("yyyy-MM-dd"); //$NON-NLS-1$

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
            LabelProvider[] labelProvider = new LabelProvider[columnCount];
            for (int ii = 0; ii < columnCount; ii++)
            {
                CellLabelProvider p = viewer.getLabelProvider(ii);
                if (p instanceof SharesLabelProvider lp)
                {
                    labelProvider[ii] = LabelProvider.createTextProvider(e -> {
                        Long value = lp.getValue(e);
                        return value != null ? Values.Share.format(value) : ""; //$NON-NLS-1$
                    });
                }
                else if (p instanceof DateTimeLabelProvider lp)
                {
                    labelProvider[ii] = LabelProvider.createTextProvider(e -> {
                        LocalDateTime dateTime = lp.getValue(e);
                        return dateTime != null ? dateTime.format(DATETIME_FORMAT_PATTERN) : ""; //$NON-NLS-1$
                    });
                }
                else if (p instanceof DateLabelProvider lp)
                {
                    labelProvider[ii] = LabelProvider.createTextProvider(e -> {
                        LocalDate dateTime = lp.getValue(e);
                        return dateTime != null ? dateTime.format(DATE_FORMAT_PATTERN) : ""; //$NON-NLS-1$
                    });
                }
            }

            // write body
            TableItem[] items = selectionOnly ? viewer.getTable().getSelection() : viewer.getTable().getItems();
            for (TableItem item : items)
            {
                // if the table is tagged SWT.VIRTUAL, then the item is only
                // resolved if the text (or other properties) are requested
                // Also for invisible items the getText sometimes returns "" so
                // in case of blank string, additional call of getText required
                item.getText();

                for (int ii = 0; ii < columnCount; ii++)
                {
                    if (labelProvider[ii] != null)
                    {
                        printer.print(labelProvider[ii].getText(item.getData()));
                    }
                    else
                    {
                        printer.print(item.getText(ii));
                    }
                }
                printer.println();
            }
        }
    }
}
