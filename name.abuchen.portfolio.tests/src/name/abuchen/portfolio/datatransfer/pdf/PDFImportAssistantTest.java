package name.abuchen.portfolio.datatransfer.pdf;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.Test;

import name.abuchen.portfolio.datatransfer.Extractor.BuySellEntryItem;
import name.abuchen.portfolio.datatransfer.Extractor.Item;
import name.abuchen.portfolio.datatransfer.Extractor.TransactionItem;
import name.abuchen.portfolio.datatransfer.actions.DetectDuplicatesAction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;

@SuppressWarnings("nls")
public class PDFImportAssistantTest
{
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
