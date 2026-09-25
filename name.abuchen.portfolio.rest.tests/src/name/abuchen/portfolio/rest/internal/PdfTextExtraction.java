package name.abuchen.portfolio.rest.internal;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import name.abuchen.portfolio.datatransfer.Extractor;
import name.abuchen.portfolio.datatransfer.SecurityCache;
import name.abuchen.portfolio.datatransfer.pdf.ComdirectPDFExtractor;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.model.Client;

/**
 * Stands in for the PDF conversion in tests: every file "contains" the
 * comdirect purchase document {@code comdirect-Kauf02.txt} (the text the
 * comdirect extractor test works on - a purchase of 160 Boeing shares), and
 * the extraction runs as {@code PDFImportAssistant} runs it, with the
 * document's text instead of the PDF's.
 */
@SuppressWarnings("nls")
public final class PdfTextExtraction implements PdfImportHandler.Extraction
{
    public static final String FIXTURE = "comdirect-Kauf02.txt";

    private PdfImportHandler.Extraction previous;

    /** replaces the handler's extraction; {@link #uninstall()} restores it */
    public static PdfTextExtraction install()
    {
        var extraction = new PdfTextExtraction();
        extraction.previous = PdfImportHandler.extraction;
        PdfImportHandler.extraction = extraction;
        return extraction;
    }

    public void uninstall()
    {
        PdfImportHandler.extraction = previous;
    }

    @Override
    public List<Extractor.Item> extract(Client client, List<File> files, Map<File, List<Exception>> errors)
    {
        var text = fixture();
        var extractor = new ComdirectPDFExtractor(client);
        var securityCache = new SecurityCache(client);

        var items = new ArrayList<Extractor.Item>();
        for (var file : files)
        {
            var input = PDFInputFile.createTestCase(file.getName(), text).get(0);
            var warnings = new ArrayList<Exception>();
            items.addAll(extractor.extract(securityCache, input, warnings));
            if (!warnings.isEmpty())
                errors.put(file, warnings);
        }

        extractor.postProcessing(items);
        var byExtractor = new HashMap<Extractor, List<Extractor.Item>>();
        byExtractor.put(extractor, items);
        securityCache.addMissingSecurityItems(byExtractor);
        return items;
    }

    private static String fixture()
    {
        try (InputStream in = PdfTextExtraction.class.getResourceAsStream(FIXTURE))
        {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        catch (IOException e)
        {
            throw new IllegalStateException(e);
        }
    }
}
