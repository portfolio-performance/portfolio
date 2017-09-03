package name.abuchen.portfolio.ui.wizards.datatransfer;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.util.PDFTextStripper;

import name.abuchen.portfolio.datatransfer.Extractor;
import name.abuchen.portfolio.datatransfer.pdf.AbstractPDFExtractor;

public class PDFImportAssistant
{
    public static Extractor detectBankIdentifier(File PDFfilename, List<Extractor> extractors) throws IOException
    {
        String author = null;
        String text = null;

        // Loading an existing document
        try (PDDocument document = PDDocument.load(PDFfilename))
        {

            // Getting the PDDocumentInformation object
            PDDocumentInformation pdd = document.getDocumentInformation();

            // Getting the PDFTextStripper object
            PDFTextStripper textStripper = new PDFTextStripper();
            textStripper.setSortByPosition(true);
            text = textStripper.getText(document);

            author = pdd.getAuthor() == null ? "" : pdd.getAuthor(); //$NON-NLS-1$
        }

        // PDF import assistant - Level 1 - Allocate by PDF Author

        for (Extractor extractor : extractors)
        {
            if (!(extractor instanceof AbstractPDFExtractor))
                continue;

            String a = ((AbstractPDFExtractor) extractor).getPDFAuthor();
            if (a != null && a.equals(author))
                return extractor;
        }

        // PDF import assistant - Level 2 - Precheck if specific securities
        // could cause faulty detection
        // ISIN DE0005088108 = Baader Bank Aktie detect the bank
        // identifier "Baader Bank"
        Matcher matcherISIN = Pattern.compile(
                        "DE0005088108|DE0005428007|DE000CBK1001|FR0000131104|INE007B01023|DE000FTG1111|CH0001351862|CH0001350328|CH0001354296|CH0001352720|CH0001350112|CH0318681860|CH0001343885|FR0000131104|INE007B01023") //$NON-NLS-1$
                        .matcher(text);

        if (matcherISIN.find())
            return null;

        // PDF import assistent - Level 3 - use bank identifier

        for (Extractor extractor : extractors)
        {
            if (!(extractor instanceof AbstractPDFExtractor))
                continue;

            List<String> bankIdentifier = ((AbstractPDFExtractor) extractor).getBankIdentifier();
            
            if (bankIdentifier.isEmpty() && text.contains(extractor.getLabel()))
                return extractor;

            for (String identifier : bankIdentifier)
            {
                if (identifier.isEmpty())
                    continue;

                if (text.contains(identifier))
                    return extractor;
            }

        }

        return null;
    }
}
