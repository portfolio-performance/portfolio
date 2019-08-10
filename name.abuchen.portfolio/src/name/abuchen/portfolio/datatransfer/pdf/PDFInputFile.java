package name.abuchen.portfolio.datatransfer.pdf;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.util.PDFTextStripper;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.Version;

import name.abuchen.portfolio.datatransfer.Extractor;

public class PDFInputFile extends Extractor.InputFile
{
    private String text;
    private String author;

    public PDFInputFile(File file)
    {
        super(file);
    }

    public PDFInputFile(File file, String extractedText)
    {
        this(file);
        this.text = extractedText;
    }

    public static List<Extractor.InputFile> loadTestCase(Class<?> testCase, String... filenames)
    {
        List<Extractor.InputFile> answer = new ArrayList<>();

        for (String filename : filenames)
        {
            try (Scanner scanner = new Scanner(testCase.getResourceAsStream(filename), StandardCharsets.UTF_8.name()))
            {
                String extractedText = scanner.useDelimiter("\\A").next(); //$NON-NLS-1$
                answer.add(new PDFInputFile(new File(filename), extractedText));
            }
        }

        return answer;
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

    public String getAuthor()
    {
        return author;
    }

    public Version getPDFBoxVersion()
    {
        return FrameworkUtil.getBundle(PDDocument.class).getVersion();
    }

    public void convertPDFtoText() throws IOException
    {
        try (PDDocument document = PDDocument.load(getFile()))
        {
            PDDocumentInformation pdd = document.getDocumentInformation();
            author = pdd.getAuthor() == null ? "" : pdd.getAuthor(); //$NON-NLS-1$

            PDFTextStripper textStripper = new PDFTextStripper();
            textStripper.setSortByPosition(true);
            text = textStripper.getText(document);
        }
    }
}
