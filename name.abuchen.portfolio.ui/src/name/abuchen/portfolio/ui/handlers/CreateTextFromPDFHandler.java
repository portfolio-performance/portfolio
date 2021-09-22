package name.abuchen.portfolio.ui.handlers;

import java.io.File;
import java.io.IOException;

import javax.inject.Named;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;

import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.dialogs.DisplayTextDialog;

public class CreateTextFromPDFHandler
{
    @Execute
    public void execute(@Named(IServiceConstants.ACTIVE_PART) MPart part,
                    @Named(IServiceConstants.ACTIVE_SHELL) Shell shell) throws IOException
    {
        FileDialog fileDialog = new FileDialog(shell, SWT.OPEN | SWT.SINGLE);
        fileDialog.setText(Messages.PDFImportDebugTextExtraction);
        fileDialog.setFilterNames(new String[] { Messages.PDFImportFilterName });
        fileDialog.setFilterExtensions(new String[] { "*.pdf" }); //$NON-NLS-1$
        fileDialog.open();

        String fileName = fileDialog.getFileName();
        if (fileName == null || fileName.isEmpty())
            return;

        try
        {
            File file = new File(fileDialog.getFilterPath(), fileName);
            PDFInputFile inputFile = new PDFInputFile(file);
            inputFile.convertPDFtoText();

            String text = "PDFBox Version: " + inputFile.getPDFBoxVersion().toString(); //$NON-NLS-1$
            text += "\n-----------------------------------------\n"; //$NON-NLS-1$
            text += inputFile.getText().replace("\r","");   // CRLF to spac; //$NON-NLS-1$ //$NON-NLS-2$

            new DisplayTextDialog(shell, file, text).open();
        }
        catch (IOException e)
        {
            PortfolioPlugin.log(e);
            MessageDialog.openError(shell, Messages.LabelError, e.getMessage());
        }
    }
}
