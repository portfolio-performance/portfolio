package name.abuchen.portfolio.ui.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;

import name.abuchen.portfolio.model.Values;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;

import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVStrategy;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

public class TableViewerCSVExporter
{
    private static final CSVStrategy STRATEGY = new CSVStrategy(';', '"', CSVStrategy.COMMENTS_DISABLED,
                    CSVStrategy.ESCAPE_DISABLED, false, false, false, false);

    private final TableViewer viewer;

    public TableViewerCSVExporter(TableViewer viewer)
    {
        this.viewer = viewer;
    }

    public void writeToFile(File file) throws IOException
    {
        Writer writer = new OutputStreamWriter(new FileOutputStream(file), Charset.forName("UTF-8")); //$NON-NLS-1$

        try
        {
            CSVPrinter printer = new CSVPrinter(writer);
            printer.setStrategy(STRATEGY);

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
        finally
        {
            writer.close();

        }
    }

    public void export(String fileName)
    {
        FileDialog dialog = new FileDialog(viewer.getTable().getShell(), SWT.SAVE);
        dialog.setFileName(fileName);
        String name = dialog.open();
        if (name == null)
            return;

        File file = new File(name);

        try
        {
            writeToFile(file);
        }
        catch (IOException e)
        {
            PortfolioPlugin.log(e);
            MessageDialog.openError(viewer.getTable().getShell(), Messages.ExportWizardErrorExporting, e.getMessage());
        }
    }

}
