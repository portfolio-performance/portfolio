package name.abuchen.portfolio.ui.handlers;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Collectors;

import jakarta.inject.Named;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;

import com.github.difflib.DiffUtils;
import com.github.difflib.UnifiedDiffUtils;

import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.dialogs.DisplayTextDialog;

public class CreateDiffFromPDFHandler
{
    @SuppressWarnings("nls")
    @Execute
    public void execute(@Named(IServiceConstants.ACTIVE_PART) MPart part,
                    @Named(IServiceConstants.ACTIVE_SHELL) Shell shell) throws IOException
    {
        FileDialog fileDialog = new FileDialog(shell, SWT.OPEN | SWT.MULTI);
        fileDialog.setText(Messages.PDFImportDebugTextExtraction);
        fileDialog.setFilterNames(new String[] { Messages.PDFImportFilterName });
        fileDialog.setFilterExtensions(new String[] { "*.pdf;*.PDF" }); //$NON-NLS-1$
        fileDialog.open();

        String[] filenames = fileDialog.getFileNames();

        if (filenames.length == 0)
            return;

        var count = 0;
        var diff = new StringBuilder();

        for (String filename : filenames)
        {
            File file = new File(fileDialog.getFilterPath(), filename);
            PDFInputFile inputFile = new PDFInputFile(file);

            try
            {

                // create text from PDF
                inputFile.convertPDFtoText();
                var extractedText = inputFile.getText();
                var pdfBoxVersion = inputFile.getPDFBoxVersion();

                // check if the extracted text changed with the PDFBox version
                inputFile.convertLegacyPDFtoText();
                var legacyText = inputFile.getText();
                var legacyPdfBoxVersion = inputFile.getPDFBoxVersion();
                var isDifferent = !extractedText.equals(legacyText);

                if (isDifferent)
                {
                    count++;

                    var patch = DiffUtils.diff(legacyText, extractedText, null);
                    var unifiedDiff = UnifiedDiffUtils.generateUnifiedDiff(legacyPdfBoxVersion, pdfBoxVersion,
                                    Arrays.asList(legacyText.split("\n")), patch, 0);

                    diff.append(inputFile.getName()).append("\n\n")
                                    .append(unifiedDiff.stream().collect(Collectors.joining("\n"))).append("\n\n\n");
                }
            }
            catch (IOException e)
            {
                diff.append(inputFile.getName()).append("\n\n").append(e.getMessage()).append("\n\n\n");
                PortfolioPlugin.log(e);
            }
        }

        diff.append("\n\n").append(count).append(" of ").append(filenames.length)
                        .append(" documents show a difference.");

        DisplayTextDialog dialog = new DisplayTextDialog(shell, null, diff.toString());
        dialog.setDialogTitle(Messages.PDFImportDebugTextExtraction);
        dialog.open();
    }
}
