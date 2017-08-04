package name.abuchen.portfolio.ui.handlers;

import java.io.File;
import java.io.IOException;

import java.text.MessageFormat;

import javax.inject.Named;

import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.dialogs.DisplayTextDialog;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.util.PDFTextStripper;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;

public class CreateTextFromPDFHandler
{
    @Execute
    public void execute(@Named(IServiceConstants.ACTIVE_PART) MPart part,
                    @Named(IServiceConstants.ACTIVE_SHELL) Shell shell) throws IOException
    {
        // open file dialog to pick pdf files
        FileDialog fileDialog = new FileDialog(shell, SWT.OPEN | SWT.SINGLE);
        fileDialog.setText(Messages.PDFImportDebugTextExtraction);
        fileDialog.setFilterNames(new String[] { Messages.PDFImportFilterName });
        fileDialog.setFilterExtensions(new String[] { "*.pdf" }); //$NON-NLS-1$
        fileDialog.open();

        String fileName = fileDialog.getFileName();
        if (fileName == null || fileName.isEmpty())
            return;
        File file = new File(fileDialog.getFilterPath(), fileName);

        try (PDDocument doc = PDDocument.load(file))
        {
            String text;
            PDDocumentInformation info = doc.getDocumentInformation();
            String PDFauthor = info.getAuthor();
            if (PDFauthor == null || PDFauthor.equals("")){
                text = MessageFormat.format(Messages.PDFImportDebugAuthor, "")+"\n";
                text+= Messages.PDFImportDebugAutodetectionImpossible;               
            } else  {
                text = MessageFormat.format(Messages.PDFImportDebugAuthor, PDFauthor)+"\n";
                text+= Messages.PDFImportDebugAutodetectionPossible;
            }
            text+= "\n-----------------------------------------\n";
            PDFTextStripper textStripper = new PDFTextStripper();
            textStripper.setSortByPosition(true);
            text+= textStripper.getText(doc);

            new DisplayTextDialog(shell, text).open();
        }
        catch (IOException e)
        {
            PortfolioPlugin.log(e);
            MessageDialog.openError(shell, Messages.LabelError, e.getMessage());
        }
    }
}
