package name.abuchen.portfolio.ui.wizards.datatransfer;

import java.util.List;

import name.abuchen.portfolio.datatransfer.Extractor;
import name.abuchen.portfolio.datatransfer.pdf.AbstractPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;

public class PDFImportAssistant
{
    private PDFImportAssistant()
    {}

    public static Extractor detectBankIdentifier(PDFInputFile inputFile, List<Extractor> extractors)
    {
        String author = inputFile.getAuthor();
        String text = inputFile.getText();

        // PDF import assistant - Level 1 - Allocate by PDF Author

        for (Extractor extractor : extractors)
        {
            if (!(extractor instanceof AbstractPDFExtractor))
                continue;

            String a = ((AbstractPDFExtractor) extractor).getPDFAuthor();
            if (a != null && a.equals(author))
                return extractor;
        }

        // PDF import assistent - Level 2 - use bank identifier
        int countIdentifier = 0;
        Extractor matchedExtractor = null;
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
                {
                    countIdentifier++;
                    matchedExtractor = extractor;
                }
            }
        }

        if (countIdentifier == 1)
            return matchedExtractor;

        return null;
    }
}
