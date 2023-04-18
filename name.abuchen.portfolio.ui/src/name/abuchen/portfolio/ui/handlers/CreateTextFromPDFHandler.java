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
        fileDialog.setFilterExtensions(new String[] { "*.pdf;*.PDF" }); //$NON-NLS-1$
        fileDialog.open();

        String fileName = fileDialog.getFileName();
        if (fileName == null || fileName.isEmpty())
            return;

        try
        {
            File file = new File(fileDialog.getFilterPath(), fileName);
            PDFInputFile inputFile = new PDFInputFile(file);
            inputFile.convertPDFtoText();

            StringBuilder textBuilder = new StringBuilder();
            textBuilder.append("```") //$NON-NLS-1$
                            .append("\n"); //$NON-NLS-1$
            textBuilder.append("PDFBox Version: ") //$NON-NLS-1$
                            .append(inputFile.getPDFBoxVersion().toString()) //
                            .append("\n"); //$NON-NLS-1$
            textBuilder.append("Portfolio Performance Version: ") //$NON-NLS-1$
                            .append(PortfolioPlugin.getDefault().getBundle().getVersion().toString()) //
                            .append("\n"); //$NON-NLS-1$
            textBuilder.append("-----------------------------------------\n"); //$NON-NLS-1$
            textBuilder.append(inputFile.getText().replace("\r", "")) //$NON-NLS-1$ //$NON-NLS-2$
                            .append("\n"); //$NON-NLS-1$
            textBuilder.append("```"); //$NON-NLS-1$

            String text = textBuilder.toString();

            DisplayTextDialog dialog = new DisplayTextDialog(shell, file, text);
            dialog.setDialogTitle(Messages.PDFImportDebugTextExtraction);
            dialog.setAdditionalText(Messages.PDFImportDebugInformation);
            dialog.createPDFDebug(true);
            dialog.open();
        }
        catch (IOException e)
        {
            PortfolioPlugin.log(e);
            MessageDialog.openError(shell, Messages.LabelError, e.getMessage());
        }
    }
}
