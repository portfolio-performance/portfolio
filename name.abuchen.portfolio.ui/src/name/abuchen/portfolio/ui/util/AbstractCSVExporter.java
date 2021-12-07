package name.abuchen.portfolio.ui.util;

import java.io.File;
import java.io.IOException;

import org.apache.commons.csv.CSVFormat;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;

import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.util.TextUtil;

public abstract class AbstractCSVExporter
{
    protected static final CSVFormat STRATEGY = CSVFormat //
                    .newFormat(';').withQuote('"').withRecordSeparator("\r\n").withAllowDuplicateHeaderNames(); //$NON-NLS-1$

    protected abstract Shell getShell();

    protected abstract void writeToFile(File file) throws IOException;

    public void export(Object... labels)
    {
        StringBuilder fileName = new StringBuilder();

        for (Object label : labels)
        {
            if (label == null)
                continue;

            if (fileName.length() > 0)
                fileName.append(" "); //$NON-NLS-1$

            fileName.append(label);
        }

        fileName.append(".csv"); //$NON-NLS-1$

        this.export(fileName.toString());
    }

    public void export(String fileName)
    {
        FileDialog dialog = new FileDialog(getShell(), SWT.SAVE);
        dialog.setFileName(TextUtil.sanitizeFilename(fileName));
        dialog.setOverwrite(true);
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
            MessageDialog.openError(getShell(), Messages.ExportWizardErrorExporting, e.getMessage());
        }
    }
}
