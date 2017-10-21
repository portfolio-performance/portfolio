package name.abuchen.portfolio.ui.wizards.datatransfer;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
