package name.abuchen.portfolio.ui.util;

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

public class TableViewerCSVExporter extends AbstractCSVExporter
{
    private final TableViewer viewer;

    public TableViewerCSVExporter(TableViewer viewer)
    {
        this.viewer = viewer;
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
            for (TableItem item : viewer.getTable().getItems())
            {
                for (int ii = 0; ii < columnCount; ii++)
                {
                    if (labelProvider[ii] != null)
                    {
                        Long value = labelProvider[ii].getValue(item.getData());
                        printer.print(value != null ? Values.Share.format(value) : ""); //$NON-NLS-1$
                    }
                    else
                        printer.print(item.getText(ii));
                }
                printer.println();
            }
        }
    }
}
