package name.abuchen.portfolio.datatransfer.pdf;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import name.abuchen.portfolio.datatransfer.Extractor;
import name.abuchen.portfolio.datatransfer.pdf.baaderbank.BaaderBankPDFExtractorTest;
import name.abuchen.portfolio.datatransfer.pdf.degiro.DegiroPDFExtractorTest;
import name.abuchen.portfolio.model.Client;

@SuppressWarnings("nls")
public class PDFImportAssistantLabelTest
{
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private File copy(Class<?> testClass, String testCase, String name) throws IOException
    {
        var text = PDFInputFile.loadSingleTestCase(testClass, testCase).getText();
        var file = new File(folder.getRoot(), name);
        Files.writeString(file.toPath(), text, StandardCharsets.UTF_8);
        return file;
    }

    private Map<String, List<Extractor.Item>> run(List<File> files)
    {
        Map<File, List<Exception>> errors = new HashMap<>();

        var result = new PDFImportAssistant(new Client(), new ArrayList<>(files)) //
                        .run(new NullProgressMonitor(), errors);

        assertThat(errors.isEmpty(), is(true));

        return result.entrySet().stream().collect(Collectors.toMap(e -> e.getKey().getLabel(), Map.Entry::getValue));
    }

    private long countTransactions(List<Extractor.Item> items)
    {
        return items.stream().filter(i -> !(i instanceof Extractor.SecurityItem)).count();
    }

    @Test
    public void testBaaderBankDocumentsAreGroupedPerBroker() throws IOException
    {
        var files = List.of( //
                        copy(BaaderBankPDFExtractorTest.class, "Dividende01.txt", "zero.txt"), //
                        copy(BaaderBankPDFExtractorTest.class, "Dividende04.txt", "gratisbroker.txt"), //
                        copy(BaaderBankPDFExtractorTest.class, "Dividende17.txt", "smartbroker.txt"), //
                        copy(BaaderBankPDFExtractorTest.class, "Kauf25.txt", "oskar.txt"), //
                        copy(BaaderBankPDFExtractorTest.class, "Kauf01.txt", "scalable.txt"), //
                        copy(BaaderBankPDFExtractorTest.class, "Kauf30.txt", "tradersplace.txt"), //
                        copy(BaaderBankPDFExtractorTest.class, "Kauf05.txt", "baader.txt"));

        var result = run(files);

        assertThat(result.keySet(), containsInAnyOrder( //
                        "Baader Bank AG / finanzen.net zero", //
                        "Baader Bank AG / Smartbroker+", //
                        "Baader Bank AG / Oskar", //
                        "Baader Bank AG / Scalable Capital", //
                        "Baader Bank AG / Traders Place", //
                        "Baader Bank AG"));

        // GRATISBROKER and finanzen.net zero are intentionally grouped together
        assertThat(countTransactions(result.get("Baader Bank AG / finanzen.net zero")), is(2L));
        assertThat(countTransactions(result.get("Baader Bank AG / Smartbroker+")), is(1L));
        assertThat(countTransactions(result.get("Baader Bank AG / Oskar")), is(1L));
        assertThat(countTransactions(result.get("Baader Bank AG / Scalable Capital")), is(1L));
        assertThat(countTransactions(result.get("Baader Bank AG / Traders Place")), is(1L));
        assertThat(countTransactions(result.get("Baader Bank AG")), is(1L));
    }

    @Test
    public void testDocumentsWithoutBrokerUseOriginalExtractor() throws IOException
    {
        Map<File, List<Exception>> errors = new HashMap<>();

        var files = List.of( //
                        copy(BaaderBankPDFExtractorTest.class, "Kauf05.txt", "baader.txt"), //
                        copy(BaaderBankPDFExtractorTest.class, "Dividende17.txt", "smartbroker.txt"));

        var result = new PDFImportAssistant(new Client(), new ArrayList<>(files)) //
                        .run(new NullProgressMonitor(), errors);

        assertThat(errors.isEmpty(), is(true));
        assertThat(result.size(), is(2));

        for (var extractor : result.keySet())
        {
            if ("Baader Bank AG".equals(extractor.getLabel()))
                assertThat(extractor, instanceOf(BaaderBankPDFExtractor.class));
            else
                assertThat(extractor, not(instanceOf(BaaderBankPDFExtractor.class)));
        }
    }

    @Test
    public void testOtherExtractorsAreNotAffected() throws IOException
    {
        Map<File, List<Exception>> errors = new HashMap<>();

        var files = List.of(copy(DegiroPDFExtractorTest.class, "AccountStatement01.txt", "degiro.txt"));

        var result = new PDFImportAssistant(new Client(), new ArrayList<>(files)) //
                        .run(new NullProgressMonitor(), errors);

        assertThat(errors.isEmpty(), is(true));
        assertThat(result.size(), is(1));
        assertThat(result.keySet().iterator().next(), instanceOf(DegiroPDFExtractor.class));
    }
}
