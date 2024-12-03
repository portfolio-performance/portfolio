package name.abuchen.portfolio.ui.handlers;

import java.io.File;
import java.io.IOException;
import java.util.Random;

import jakarta.inject.Named;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.dialogs.DisplayTextDialog;

public class CreateTextFromPDFHandler
{
    @SuppressWarnings("nls")
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
            textBuilder.append("```").append("\n");
            textBuilder.append("PDFBox Version: ")
                            .append(inputFile.getPDFBoxVersion().toString()) //
                            .append("\n");
            textBuilder.append("Portfolio Performance Version: ")
                            .append(PortfolioPlugin.getDefault().getBundle().getVersion().toString()) //
                            .append("\n");

            textBuilder.append("System: ") //
                            .append(System.getProperty("osgi.os", "unknown")).append(" | ")
                            .append(System.getProperty("osgi.arch", "unknown")).append(" | ")
                            .append(System.getProperty("java.vm.version", "unknown")).append(" | ")
                            .append(System.getProperty("java.vm.vendor", "unknown")).append("\n");

            textBuilder.append("-----------------------------------------\n");
            textBuilder.append(inputFile.getText().replace("\r", "")).append("\n");
            textBuilder.append("```");

            String text = textBuilder.toString();

            DisplayTextDialog dialog = new DisplayPDFTextDialog(shell, file, text);
            dialog.setDialogTitle(Messages.PDFImportDebugTextExtraction);
            dialog.setAdditionalText(Messages.PDFImportDebugInformation);
            dialog.open();
        }
        catch (IOException e)
        {
            PortfolioPlugin.log(e);
            MessageDialog.openError(shell, Messages.LabelError, e.getMessage());
        }
    }

    private static class DisplayPDFTextDialog extends DisplayTextDialog
    {

        public DisplayPDFTextDialog(Shell parentShell, File source, String text)
        {
            super(parentShell, source, text);
        }

        @Override
        protected Text createTextArea(Composite container)
        {
            Text textArea = super.createTextArea(container);

            textArea.addMouseListener(new PDFDebugMouseListener(textArea));

            return textArea;
        }
    }

    private static class PDFDebugMouseListener extends MouseAdapter
    {
        private final Random random = new Random();
        private final Text widget;

        public PDFDebugMouseListener(Text widget)
        {
            this.widget = widget;
        }

        @Override
        public void mouseDoubleClick(MouseEvent e)
        {
            String selectedText = widget.getSelectionText();

            // Check if selectedText is not empty and does not contain forbidden
            // characters or currency codes
            if (!selectedText.isEmpty() && !containsForbiddenCharacters(selectedText)
                            && !CurrencyUnit.containsCurrencyCode(selectedText.trim()))
            {
                // Generate a new string of random characters to replace the
                // selected text
                StringBuilder replacementTextBuilder = new StringBuilder();
                for (int i = 0; i < selectedText.length(); i++)
                {
                    char c = selectedText.charAt(i);
                    if (Character.isLetter(c))
                    {
                        replacementTextBuilder.append(generateRandomLetter());
                    }
                    else if (Character.isDigit(c))
                    {
                        replacementTextBuilder.append(generateRandomNumber());
                    }
                    else
                    {
                        replacementTextBuilder.append(c);
                    }
                }
                String replacementText = replacementTextBuilder.toString();

                // Replace selectedText with replacementText
                int startIndex = widget.getSelection().x;
                widget.insert(replacementText);
                widget.setSelection(startIndex, startIndex + replacementText.length());
            }
        }

        /** Check if the selected string contains any forbidden characters */
        private boolean containsForbiddenCharacters(String string)
        {
            return string.matches(".*[\\-\\.,':\\/].*") || string.matches(".*[A-Z]{2}[A-Z0-9]{9}\\d.*"); //$NON-NLS-1$ //$NON-NLS-2$
        }

        /** Generates a random letter */
        private char generateRandomLetter()
        {
            boolean isUpperCase = random.nextBoolean();
            int offset = isUpperCase ? 'A' : 'a';
            return (char) (random.nextInt(26) + offset);
        }

        /** Generates a random digit between 0 and 9 */
        private char generateRandomNumber()
        {
            return (char) (random.nextInt(10) + '0');
        }
    }

}
