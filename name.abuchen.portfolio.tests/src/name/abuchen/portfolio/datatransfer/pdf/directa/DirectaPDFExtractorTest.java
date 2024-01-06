package name.abuchen.portfolio.datatransfer.pdf.directa;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import name.abuchen.portfolio.datatransfer.Extractor.Item;
import name.abuchen.portfolio.datatransfer.pdf.DirectaPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.model.Client;

@SuppressWarnings("nls")
public class DirectaPDFExtractorTest
{
    DirectaPDFExtractor extractor = new DirectaPDFExtractor(new Client());

    @Test
    public void testInfoReport01()
    {
        List<Exception> errors = new ArrayList<>();

        List<Item> results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Buy01.txt"), errors);

        if (!errors.isEmpty())
            errors.get(0).printStackTrace();

        assertThat(errors, empty());
        // 1 transaction
    }
}
