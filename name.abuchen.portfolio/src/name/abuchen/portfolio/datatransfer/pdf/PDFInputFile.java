package name.abuchen.portfolio.datatransfer.pdf;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import name.abuchen.portfolio.datatransfer.Extractor;
import name.abuchen.portfolio.pdfbox1.PDFBox1Adapter;
import name.abuchen.portfolio.pdfbox3.PDFBox3Adapter;
import name.abuchen.portfolio.pdfbox3.PDFBox3Renderer;

public class PDFInputFile extends Extractor.InputFile
{
    private String text;
    private String version;

    public PDFInputFile(File file)
    {
        super(file);
    }

    /* protected */ PDFInputFile(File file, String extractedText)
    {
        this(file);
        this.text = sanitize(extractedText);
    }

    public static List<Extractor.InputFile> loadTestCase(Class<?> testCase, String... filenames)
    {
        List<Extractor.InputFile> answer = new ArrayList<>();

        for (String filename : filenames)
            answer.add(loadSingleTestCase(testCase, filename));

        return answer;
    }

    public static PDFInputFile loadSingleTestCase(Class<?> testCase, String filename)
    {
        try (Scanner scanner = new Scanner(testCase.getResourceAsStream(filename), StandardCharsets.UTF_8.name()))
        {
            String extractedText = scanner.useDelimiter("\\A").next(); //$NON-NLS-1$
            return new PDFInputFile(new File(filename), extractedText);
        }
    }

    public static List<Extractor.InputFile> createTestCase(String filename, String text)
    {
        List<Extractor.InputFile> answer = new ArrayList<>();
        answer.add(new PDFInputFile(new File(filename), text));
        return answer;
    }

    public String getText()
    {
        return text;
    }

    public String getPDFBoxVersion()
    {
        return version;
    }

    public void convertPDFtoText() throws IOException
    {
        var adapter = new PDFBox3Adapter();

        text = sanitize(adapter.convertToText(getFile()));
        version = adapter.getPDFBoxVersion();
    }

    /**
     * Opens a reusable renderer that keeps the underlying PDF document open, so
     * that paging through the document does not re-parse the file on every page.
     * The renderer is not thread-safe and must be closed by the caller.
     */
    public PageRenderer openRenderer() throws IOException
    {
        return new PageRenderer(new PDFBox3Renderer(getFile()));
    }

    /**
     * Core-side wrapper around the pdfbox3 renderer so that callers (e.g. the
     * UI) depend only on this module.
     */
    public static final class PageRenderer implements Closeable
    {
        private final PDFBox3Renderer delegate;

        private PageRenderer(PDFBox3Renderer delegate)
        {
            this.delegate = delegate;
        }

        public int getPageCount()
        {
            return delegate.getPageCount();
        }

        public byte[] renderPage(int pageIndex, float dpi) throws IOException
        {
            return delegate.renderPage(pageIndex, dpi);
        }

        @Override
        public void close() throws IOException
        {
            delegate.close();
        }
    }

    public void convertLegacyPDFtoText() throws IOException
    {
        var adapter = new PDFBox1Adapter();

        text = sanitize(adapter.convertToText(getFile()));
        version = adapter.getPDFBoxVersion();
    }

    @SuppressWarnings("nls")
    private String sanitize(String s)
    {
        // replace horizontal whitespace characters by normal whitespace
        // without carriage returns
        return s.replaceAll("\\h", " ").replace("\r", "");
    }
}
