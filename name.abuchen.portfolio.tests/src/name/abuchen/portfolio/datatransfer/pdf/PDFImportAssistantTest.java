package name.abuchen.portfolio.datatransfer.pdf;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import name.abuchen.portfolio.datatransfer.Extractor.BuySellEntryItem;
import name.abuchen.portfolio.datatransfer.Extractor.Item;
import name.abuchen.portfolio.datatransfer.Extractor.SecurityItem;
import name.abuchen.portfolio.datatransfer.Extractor.TransactionItem;
import name.abuchen.portfolio.datatransfer.actions.DetectDuplicatesAction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;

@SuppressWarnings("nls")
public class PDFImportAssistantTest
{
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void testRunMarksItemsWithAbsolutePathOfInputFile() throws IOException
    {
        var file = copyOfTestDocument("baaderbank/Kauf01.txt", "Ordner1");

        var items = run(List.of(file));

        assertThat(items.isEmpty(), is(false));
        for (var item : items)
        {
            assertThat(item.getData(DetectDuplicatesAction.SOURCE_KEY), is(file.getAbsolutePath()));

            // the visible source of the transaction remains the file name
            assertThat(item.getSource(), is("Kauf01.txt"));
        }
    }

    @Test
    public void testRunDistinguishesFilesWithSameNameInDifferentFolders() throws IOException
    {
        var file1 = copyOfTestDocument("baaderbank/Kauf01.txt", "Ordner1");
        var file2 = copyOfTestDocument("baaderbank/Kauf01.txt", "Ordner2");

        var items1 = run(List.of(file1));
        var items = run(List.of(file1, file2));

        assertThat(items.size(), is(2 * items1.size()));
        assertThat(items.stream().map(item -> item.getData(DetectDuplicatesAction.SOURCE_KEY)).distinct().count(),
                        is(2L));
    }

    @Test
    public void testRunExtractsRepeatedInputFileOnlyOnce() throws IOException
    {
        // the same file selected twice must not create two copies with the
        // same source key (they would not be detected as duplicates)
        var file = copyOfTestDocument("baaderbank/Kauf01.txt", "Ordner1");
        var sameFile = new File(new File(file.getParentFile(), "."), file.getName());

        var items = run(List.of(file, sameFile));

        assertThat(items.size(), is(run(List.of(file)).size()));
    }

    @Test
    public void testDistinctFilesKeepsOrderOfFirstOccurrence()
    {
        var file1 = new File("Ordner1", "Kauf01.pdf");
        var file2 = new File("Ordner2", "Kauf01.pdf");
        var file1Again = new File(new File("Ordner1", "."), "Kauf01.pdf");

        assertThat(PDFImportAssistant.distinctFiles(List.of(file1, file2, file1Again)), is(List.of(file1, file2)));
    }

    @Test
    public void testMarkSourceKeyWithAbsolutePathOfInputFile()
    {
        var file = new File("Ordner1", "Kauf01.pdf");

        List<Item> items = List.of(new BuySellEntryItem(buySell("Kauf01.pdf")),
                        new TransactionItem(transaction("Kauf01.pdf")));

        PDFImportAssistant.markSourceKey(items, new PDFInputFile(file));

        for (var item : items)
        {
            assertThat(item.getData(DetectDuplicatesAction.SOURCE_KEY), is(file.getAbsolutePath()));

            // the visible source of the transaction remains the file name
            assertThat(item.getSource(), is("Kauf01.pdf"));
        }
    }

    @Test
    public void testMarkSourceKeyDistinguishesFilesWithSameNameInDifferentFolders()
    {
        // e.g. a ZIP archive containing Ordner1/Kauf01.pdf and
        // Ordner2/Kauf01.pdf
        var file1 = new File("Ordner1", "Kauf01.pdf");
        var file2 = new File("Ordner2", "Kauf01.pdf");

        var item1 = new BuySellEntryItem(buySell("Kauf01.pdf"));
        var item2 = new BuySellEntryItem(buySell("Kauf01.pdf"));

        PDFImportAssistant.markSourceKey(List.of(item1), new PDFInputFile(file1));
        PDFImportAssistant.markSourceKey(List.of(item2), new PDFInputFile(file2));

        assertThat(item1.getSource(), is(item2.getSource()));
        assertThat(item1.getData(DetectDuplicatesAction.SOURCE_KEY),
                        is(not(item2.getData(DetectDuplicatesAction.SOURCE_KEY))));
    }

    /**
     * Returns the transactions (without the securities created by the import)
     * extracted from the files.
     */
    private List<Item> run(List<File> files)
    {
        Map<File, List<Exception>> errors = new HashMap<>();
        var result = new PDFImportAssistant(new Client(), files).run(new NullProgressMonitor(), errors);

        assertThat(errors.toString(), errors.isEmpty(), is(true));

        return result.values().stream() //
                        .flatMap(List::stream) //
                        .filter(item -> !(item instanceof SecurityItem)) //
                        .toList();
    }

    /**
     * Copies an existing test document into the given sub folder of a
     * temporary folder.
     */
    private File copyOfTestDocument(String resource, String subFolder) throws IOException
    {
        var target = folder.getRoot().toPath().resolve(subFolder).resolve(Path.of(resource).getFileName());
        Files.createDirectories(target.getParent());

        try (InputStream is = getClass().getResourceAsStream(resource))
        {
            if (is == null)
                throw new IOException("Test resource not found: " + resource);
            Files.copy(is, target);
        }

        return target.toFile();
    }

    private BuySellEntry buySell(String source)
    {
        var entry = new BuySellEntry(PortfolioTransaction.Type.BUY);
        entry.setSecurity(new Security());
        entry.setShares(100L);
        entry.setAmount(1000);
        entry.setCurrencyCode("EUR");
        entry.setDate(LocalDateTime.of(2025, 12, 15, 0, 0));
        entry.setSource(source);
        return entry;
    }

    private AccountTransaction transaction(String source)
    {
        var transaction = new AccountTransaction(AccountTransaction.Type.FEES);
        transaction.setAmount(100);
        transaction.setCurrencyCode("EUR");
        transaction.setDateTime(LocalDateTime.of(2025, 12, 15, 0, 0));
        transaction.setSource(source);
        return transaction;
    }
}
