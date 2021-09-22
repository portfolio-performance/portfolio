package name.abuchen.portfolio.datatransfer.pdf;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.apache.pdfbox.exceptions.CryptographyException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.util.PDFTextStripper;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.Version;

import name.abuchen.portfolio.datatransfer.Extractor;

public class PDFInputFile extends Extractor.InputFile
{
    private String text;

    public PDFInputFile(File file)
    {
        super(file);
    }

    /* protected */ PDFInputFile(File file, String extractedText)
    {
        this(file);
        this.text = extractedText;
        this.text = withoutHorizontalWhitespace(extractedText);
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

    public Version getPDFBoxVersion()
    {
        return FrameworkUtil.getBundle(PDDocument.class).getVersion();
    }

    public void convertPDFtoText() throws IOException
    {
        try (PDDocument document = PDDocument.load(getFile()))
        {
            boolean isProtected = document.isEncrypted();
            if (isProtected)
            {
                document.decrypt(""); //$NON-NLS-1$
                document.setAllSecurityToBeRemoved(true);
            }

            PDFTextStripper textStripper = new PDFTextStripper();
            textStripper.setSortByPosition(true);
            text = textStripper.getText(document);

            text = withoutHorizontalWhitespace(text);
        }
        catch (CryptographyException e)
        {
            throw new IOException(e);
        }
    }

    private String withoutHorizontalWhitespace(String s)
    {
        // replace horizontal whitespace characters by normal whitespace
        return s.replaceAll("\\h", " "); //$NON-NLS-1$ //$NON-NLS-2$
    }
}
