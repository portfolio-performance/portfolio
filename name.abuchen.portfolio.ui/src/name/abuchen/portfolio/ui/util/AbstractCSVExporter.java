package name.abuchen.portfolio.ui.util;

import java.io.File;
import java.io.IOException;

import org.apache.commons.csv.CSVStrategy;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;

import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.util.TextUtil;

public abstract class AbstractCSVExporter
{
    protected static final CSVStrategy STRATEGY = new CSVStrategy(';', '"', CSVStrategy.COMMENTS_DISABLED,
                    CSVStrategy.ESCAPE_DISABLED, false, false, false, false);

    protected abstract Shell getShell();

    protected abstract void writeToFile(File file) throws IOException;

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
