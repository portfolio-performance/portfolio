package name.abuchen.portfolio.ui.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;

import name.abuchen.portfolio.model.Exporter;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.util.TextUtil;

public class JSONExporterDialog
{
    private final Shell shell;
    private final Exporter exporter;

    public JSONExporterDialog(Shell shell, Exporter exporter)
    {
        this.shell = shell;
        this.exporter = exporter;
    }

    public void export()
    {
        var dialog = new FileDialog(shell, SWT.SAVE);
        dialog.setFilterNames(new String[] { Messages.CSVConfigCSVImportLabelFileJSON });
        dialog.setFilterExtensions(new String[] { "*.json" }); //$NON-NLS-1$

        var defaultFileName = exporter.getName();
        if (defaultFileName != null)
        {
            defaultFileName = TextUtil.sanitizeFilename(defaultFileName);
            if (!defaultFileName.toLowerCase().endsWith(".json")) //$NON-NLS-1$
                defaultFileName += ".json"; //$NON-NLS-1$
            dialog.setFileName(defaultFileName);
        }

        String fileName = dialog.open();
        if (fileName == null)
            return;

        try
        {
            File file = new File(fileName);
            try (FileOutputStream out = new FileOutputStream(file))
            {
                exporter.export(out);
            }
        }
        catch (IOException e)
        {
            PortfolioPlugin.log(e);

            MessageDialog.openError(shell, Messages.ExportWizardErrorExporting, e.getMessage());
        }
    }
}
