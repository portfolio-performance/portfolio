package name.abuchen.portfolio.datatransfer.pdf;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Scanner;
import java.util.regex.Pattern;

import name.abuchen.portfolio.datatransfer.Extractor;
import name.abuchen.portfolio.pdfbox1.PDFBox1Adapter;
import name.abuchen.portfolio.pdfbox3.PDFBox3Adapter;
import name.abuchen.portfolio.pdfbox3.PDFBox3Renderer;

public class PDFInputFile extends Extractor.InputFile
{
    /**
     * Version label used for text files that do not carry the PDFBox version
     * in a debug header.
     */
    private static final String TEXT_FILE_VERSION = "text file"; //$NON-NLS-1$

    private static final Pattern PDFBOX_VERSION_HEADER = Pattern.compile("^PDFBox Version: (.*)$"); //$NON-NLS-1$
    private static final String DEBUG_HEADER_SEPARATOR = "-----------------------------------------"; //$NON-NLS-1$
    private static final String DEBUG_FENCE = "```"; //$NON-NLS-1$

    private String text;
    private String version;
    private boolean isTextFile;

    public PDFInputFile(File file)
    {
        super(file);
    }

    /* protected */ PDFInputFile(File file, String extractedText)
    {
        this(file);
        this.text = sanitize(extractedText);
    }

    /**
     * Returns true if the file holds text previously extracted from a PDF
     * document (as opposed to the PDF document itself).
     */
    public static boolean isTextFile(File file)
    {
        return file.getName().toLowerCase(Locale.ROOT).endsWith(".txt"); //$NON-NLS-1$
    }

    /**
     * Creates an input file from text previously extracted from a PDF
     * document. Accepts both the raw text (as used by the test cases) and the
     * text wrapped with the debug header as created by the "Create text from
     * PDF" wizard.
     */
    public static PDFInputFile fromTextFile(File file) throws IOException
    {
        var inputFile = new PDFInputFile(file);
        inputFile.isTextFile = true;
        inputFile.version = TEXT_FILE_VERSION;

        var content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
        inputFile.text = inputFile.sanitize(inputFile.stripDebugHeader(content));
        return inputFile;
    }

    private String stripDebugHeader(String content)
    {
        var lines = content.replace("\r", "").split("\n", -1); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

        if (lines.length == 0 || !DEBUG_FENCE.equals(lines[0].strip()))
            return content;

        var separator = -1;
        for (var ii = 1; ii < lines.length; ii++)
        {
            var matcher = PDFBOX_VERSION_HEADER.matcher(lines[ii]);
            if (matcher.matches())
                version = matcher.group(1).strip();

            if (DEBUG_HEADER_SEPARATOR.equals(lines[ii].strip()))
            {
                separator = ii;
                break;
            }
        }

        if (separator < 0)
            return content;

        var end = lines.length;
        while (end > separator + 1 && lines[end - 1].isBlank())
            end--;
        if (end > separator + 1 && DEBUG_FENCE.equals(lines[end - 1].strip()))
            end--;

        return String.join("\n", Arrays.copyOfRange(lines, separator + 1, end)); //$NON-NLS-1$
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
        try (Scanner scanner = new Scanner(Objects.requireNonNull(testCase.getResourceAsStream(filename), filename),
                        StandardCharsets.UTF_8.name()))
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

    /**
     * Returns true if this input was loaded from a text file, i.e. there is no
     * PDF document to convert or render.
     */
    public boolean isTextFile()
    {
        return isTextFile;
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
