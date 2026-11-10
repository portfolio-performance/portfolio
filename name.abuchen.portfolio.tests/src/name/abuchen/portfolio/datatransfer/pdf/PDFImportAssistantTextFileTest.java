package name.abuchen.portfolio.datatransfer.pdf;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import name.abuchen.portfolio.datatransfer.Extractor;
import name.abuchen.portfolio.datatransfer.pdf.degiro.DegiroPDFExtractorTest;
import name.abuchen.portfolio.model.Client;

@SuppressWarnings("nls")
public class PDFImportAssistantTextFileTest
{
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private String degiroText()
    {
        return PDFInputFile.loadSingleTestCase(DegiroPDFExtractorTest.class, "AccountStatement01.txt").getText();
    }

    private String wrapAsDebugText(String text)
    {
        // same layout as created by the "Create text from PDF" wizard
        return "```\n" //
                        + "PDFBox Version: 3.0.5\n" //
                        + "Portfolio Performance Version: 0.0.0\n" //
                        + "System: macosx | aarch64 | 21 | Eclipse Adoptium\n" //
                        + "-----------------------------------------\n" //
                        + text + "\n" //
                        + "```";
    }

    private File write(String name, String content) throws IOException
    {
        var file = new File(folder.getRoot(), name);
        Files.writeString(file.toPath(), content, StandardCharsets.UTF_8);
        return file;
    }

    private int countItems(Map<Extractor, List<Extractor.Item>> result)
    {
        return result.values().stream().mapToInt(List::size).sum();
    }

    @Test
    public void testRawTextFile() throws IOException
    {
        var file = PDFInputFile.fromTextFile(write("raw.txt", degiroText()));

        assertThat(file.isTextFile(), is(true));
        assertThat(file.getText(), is(degiroText()));
        assertThat(file.getPDFBoxVersion(), is("text file"));
    }

    @Test
    public void testDebugTextFileIsUnwrapped() throws IOException
    {
        var file = PDFInputFile.fromTextFile(write("wrapped.txt", wrapAsDebugText(degiroText())));

        assertThat(file.isTextFile(), is(true));
        assertThat(file.getText(), is(degiroText()));
        assertThat(file.getPDFBoxVersion(), is("3.0.5"));
    }

    @Test
    public void testTextFilesAreImportedLikePDFDocuments() throws IOException
    {
        var raw = write("raw.txt", degiroText());
        var wrapped = write("wrapped.txt", wrapAsDebugText(degiroText()));

        Map<File, List<Exception>> errors = new HashMap<>();
        var rawResult = new PDFImportAssistant(new Client(), new ArrayList<>(List.of(raw)))
                        .run(new NullProgressMonitor(), errors);
        var wrappedResult = new PDFImportAssistant(new Client(), new ArrayList<>(List.of(wrapped)))
                        .run(new NullProgressMonitor(), errors);

        assertThat(errors.isEmpty(), is(true));
        assertThat(rawResult.size(), is(1));
        assertThat(rawResult.keySet().iterator().next(), instanceOf(DegiroPDFExtractor.class));
        assertThat(countItems(rawResult) > 0, is(true));
        assertThat(countItems(wrappedResult), is(countItems(rawResult)));
    }

    @Test
    public void testUnrecognizedTextFileIsOfferedForManualEntry() throws IOException
    {
        var unknown = write("unknown.txt", "This is not a bank document\nat all\n");

        Map<File, List<Exception>> errors = new HashMap<>();
        var assistant = new PDFImportAssistant(new Client(), new ArrayList<>(List.of(unknown)));
        var result = assistant.run(new NullProgressMonitor(), errors);

        assertThat(result.isEmpty(), is(true));
        assertThat(errors, hasKey(unknown));
        assertThat(assistant.getFailedInputFiles(), hasKey(unknown));

        var failed = assistant.getFailedInputFiles().get(unknown);
        assertThat(failed.isTextFile(), is(true));
        assertThat(failed.getText(), is("This is not a bank document\nat all\n"));
    }
}
