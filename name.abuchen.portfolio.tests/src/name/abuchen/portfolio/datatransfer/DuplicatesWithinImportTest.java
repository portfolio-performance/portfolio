package name.abuchen.portfolio.datatransfer;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import name.abuchen.portfolio.datatransfer.Extractor.AccountTransferItem;
import name.abuchen.portfolio.datatransfer.Extractor.BuySellEntryItem;
import name.abuchen.portfolio.datatransfer.Extractor.PortfolioTransferItem;
import name.abuchen.portfolio.datatransfer.Extractor.SecurityItem;
import name.abuchen.portfolio.datatransfer.Extractor.TransactionItem;
import name.abuchen.portfolio.datatransfer.ImportAction.Status.Code;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.AccountTransferEntry;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.PortfolioTransferEntry;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class DuplicatesWithinImportTest
{
    @Test
    public void testNoDuplicatesForNone()
    {
        var item = new TransactionItem(getTestEntry(AccountTransaction.Type.DIVIDENDS, "Dividende01.pdf"));

        assertThat(DuplicatesWithinImport.none().isDuplicate(item), is(false));
        assertThat(DuplicatesWithinImport.none().keptCopyOf(item).isPresent(), is(false));
    }

    @Test
    public void testDuplicate4AccountTransactionFromDifferentSources()
    {
        var items = List.<Extractor.Item>of( //
                        new TransactionItem(getTestEntry(AccountTransaction.Type.DIVIDENDS, "Dividende01.pdf")),
                        new TransactionItem(getTestEntry(AccountTransaction.Type.DIVIDENDS,
                                        "Dividende01 (Kopie).pdf")));

        // the transaction to keep is determined by the sorted file names, not
        // by the order of the items: "Dividende01 (Kopie).pdf" comes first
        assertThat(statusOf(items), is(List.of(Code.WARNING, Code.OK)));
    }

    @Test
    public void testDuplicate4RemovalAndAccountTransferFromDifferentSources()
    {
        var source = new Account();
        var target = new Account();

        var items = List.<Extractor.Item>of( //
                        new TransactionItem(getTestEntry(AccountTransaction.Type.REMOVAL, "Kontoauszug01.pdf")),
                        new AccountTransferItem(getTestTransfer(source, target, "Kontoauszug02.pdf"), true));

        assertThat(statusOf(items), is(List.of(Code.OK, Code.WARNING)));
    }

    @Test
    public void testNoDuplicateFromSameSource()
    {
        // a single document can contain identical transactions (e.g. two
        // identical fees on the same day)
        var items = List.<Extractor.Item>of( //
                        new TransactionItem(getTestEntry(AccountTransaction.Type.FEES, "Kontoauszug01.pdf")),
                        new TransactionItem(getTestEntry(AccountTransaction.Type.FEES, "Kontoauszug01.pdf")));

        assertThat(statusOf(items), is(List.of(Code.OK, Code.OK)));
    }

    @Test
    public void testNoDuplicateWithoutSource()
    {
        var items = List.<Extractor.Item>of( //
                        new TransactionItem(getTestEntry(AccountTransaction.Type.FEES, null)),
                        new TransactionItem(getTestEntry(AccountTransaction.Type.FEES, "Kontoauszug01.pdf")),
                        new TransactionItem(getTestEntry(AccountTransaction.Type.FEES, null)));

        assertThat(statusOf(items), is(List.of(Code.OK, Code.OK, Code.OK)));
    }

    @Test
    public void testNoDuplicateWithDifferentValues()
    {
        var otherAmount = getTestEntry(AccountTransaction.Type.DIVIDENDS, "Dividende02.pdf");
        otherAmount.setAmount(1001);

        var otherDate = getTestEntry(AccountTransaction.Type.DIVIDENDS, "Dividende03.pdf");
        otherDate.setDateTime(LocalDateTime.of(2025, 12, 16, 0, 0));

        var otherType = getTestEntry(AccountTransaction.Type.INTEREST, "Zinsen01.pdf");

        var items = List.<Extractor.Item>of( //
                        new TransactionItem(getTestEntry(AccountTransaction.Type.DIVIDENDS, "Dividende01.pdf")),
                        new TransactionItem(otherAmount), //
                        new TransactionItem(otherDate), //
                        new TransactionItem(otherType));

        assertThat(statusOf(items), is(List.of(Code.OK, Code.OK, Code.OK, Code.OK)));
    }

    @Test
    public void testNoDuplicate4ItemsWithoutTransaction()
    {
        var security = new Security("Example", "EUR");

        var items = List.<Extractor.Item>of( //
                        new SecurityItem(security), //
                        new SecurityItem(security));

        assertThat(statusOf(items), is(List.of(Code.OK, Code.OK)));
    }

    @Test
    public void testTransactionWithoutDateIsIgnored()
    {
        // the missing date is reported by the import actions, the detection
        // must not fail
        var withoutDate = getTestEntry(AccountTransaction.Type.FEES, "Gebuehr01.pdf");
        withoutDate.setDateTime(null);

        var items = List.<Extractor.Item>of( //
                        new TransactionItem(withoutDate),
                        new TransactionItem(getTestEntry(AccountTransaction.Type.FEES, "Gebuehr02.pdf")),
                        new TransactionItem(getTestEntry(AccountTransaction.Type.FEES, "Gebuehr03.pdf")));

        assertThat(statusOf(items), is(List.of(Code.OK, Code.OK, Code.WARNING)));
    }

    @Test
    public void testDuplicate4PortfolioTransactionFromDifferentSources()
    {
        var security = new Security();

        var items = List.<Extractor.Item>of( //
                        new TransactionItem(getTestEntry(PortfolioTransaction.Type.DELIVERY_INBOUND, security,
                                        "Eingang01.pdf")),
                        new TransactionItem(getTestEntry(PortfolioTransaction.Type.DELIVERY_INBOUND, security,
                                        "Eingang02.pdf")));

        assertThat(statusOf(items), is(List.of(Code.OK, Code.WARNING)));
    }

    @Test
    public void testNoDuplicate4PortfolioTransactionWithDifferentSecurities()
    {
        var items = List.<Extractor.Item>of( //
                        new TransactionItem(getTestEntry(PortfolioTransaction.Type.DELIVERY_INBOUND, new Security(),
                                        "Eingang01.pdf")),
                        new TransactionItem(getTestEntry(PortfolioTransaction.Type.DELIVERY_INBOUND, new Security(),
                                        "Eingang02.pdf")));

        assertThat(statusOf(items), is(List.of(Code.OK, Code.OK)));
    }

    @Test
    public void testDuplicate4BuySellEntryFromDifferentSources()
    {
        var security = new Security();

        // same security and shares, but different amount
        var otherAmount = getTestEntry(PortfolioTransaction.Type.BUY, security, 100L, "Kauf02.pdf");
        otherAmount.setAmount(2000);

        var items = List.<Extractor.Item>of( //
                        new BuySellEntryItem(getTestEntry(PortfolioTransaction.Type.BUY, security, 100L, "Kauf01.pdf")),
                        new BuySellEntryItem(getTestEntry(PortfolioTransaction.Type.BUY, security, 100L,
                                        "Kauf01 (1).pdf")),
                        new BuySellEntryItem(otherAmount));

        // "Kauf01 (1).pdf" is sorted before "Kauf01.pdf" and is kept
        assertThat(statusOf(items), is(List.of(Code.WARNING, Code.OK, Code.OK)));
    }

    @Test
    public void testDuplicate4PurchaseAndDelivery()
    {
        var security = new Security();

        var delivery = getTestEntry(PortfolioTransaction.Type.DELIVERY_INBOUND, security, "Eingang01.pdf");
        delivery.setShares(100L);

        var items = List.<Extractor.Item>of( //
                        new BuySellEntryItem(getTestEntry(PortfolioTransaction.Type.BUY, security, 100L, "Kauf01.pdf")),
                        new TransactionItem(delivery));

        assertThat(statusOf(items), is(List.of(Code.WARNING, Code.OK)));
    }

    @Test
    public void testDuplicate4AccountTransferEntryFromDifferentSources()
    {
        var source = new Account();
        var target = new Account();

        var items = List.<Extractor.Item>of( //
                        new AccountTransferItem(getTestTransfer(source, target, "Umbuchung01.pdf"), true),
                        new AccountTransferItem(getTestTransfer(source, target, "Umbuchung02.pdf"), true));

        assertThat(statusOf(items), is(List.of(Code.OK, Code.WARNING)));
    }

    @Test
    public void testDuplicate4PortfolioTransferEntryFromDifferentSources()
    {
        var security = new Security();
        var source = new Portfolio();
        var target = new Portfolio();

        var items = List.<Extractor.Item>of( //
                        withSourceKey(new PortfolioTransferItem(
                                        getTestTransfer(source, target, security, "Depotuebertrag01.pdf")),
                                        "/tmp/import/Ordner1/Depotuebertrag01.pdf"),
                        withSourceKey(new PortfolioTransferItem(
                                        getTestTransfer(source, target, security, "Depotuebertrag01.pdf")),
                                        "/tmp/import/Ordner2/Depotuebertrag01.pdf"));

        assertThat(statusOf(items), is(List.of(Code.OK, Code.WARNING)));
    }

    @Test
    public void testNoDuplicate4PortfolioTransferEntryFromSameSource()
    {
        var security = new Security();
        var source = new Portfolio();
        var target = new Portfolio();

        var items = List.<Extractor.Item>of( //
                        withSourceKey(new PortfolioTransferItem(
                                        getTestTransfer(source, target, security, "Depotuebertrag01.pdf")),
                                        "/tmp/import/Ordner1/Depotuebertrag01.pdf"),
                        withSourceKey(new PortfolioTransferItem(
                                        getTestTransfer(source, target, security, "Depotuebertrag01.pdf")),
                                        "/tmp/import/Ordner1/Depotuebertrag01.pdf"));

        assertThat(statusOf(items), is(List.of(Code.OK, Code.OK)));
    }

    @Test
    public void testDuplicateViaExtractedItems()
    {
        // simulates the same document imported twice with different file names
        // plus a document with two identical transactions
        var security = new Security();

        var items = List.<Extractor.Item>of( //
                        new BuySellEntryItem(getTestEntry(PortfolioTransaction.Type.BUY, security, 100L, "Kauf01.pdf")),
                        new TransactionItem(getTestEntry(AccountTransaction.Type.FEES, "Kauf01.pdf")),
                        new TransactionItem(getTestEntry(AccountTransaction.Type.FEES, "Kauf01.pdf")),
                        new BuySellEntryItem(getTestEntry(PortfolioTransaction.Type.BUY, security, 100L,
                                        "Kauf01_2.pdf")),
                        new TransactionItem(getTestEntry(AccountTransaction.Type.FEES, "Kauf01_2.pdf")),
                        new TransactionItem(getTestEntry(AccountTransaction.Type.FEES, "Kauf01_2.pdf")));

        assertThat(statusOf(items),
                        is(List.of(Code.OK, Code.OK, Code.OK, Code.WARNING, Code.WARNING, Code.WARNING)));
    }

    @Test
    public void testDuplicateFromFilesWithSameNameInDifferentFolders()
    {
        // e.g. a ZIP archive containing Ordner1/Kauf01.pdf and
        // Ordner2/Kauf01.pdf: the source (file name) is identical, the input
        // files are not
        var security = new Security();

        var items = List.<Extractor.Item>of( //
                        withSourceKey(new BuySellEntryItem(getTestEntry(PortfolioTransaction.Type.BUY, security, 100L,
                                        "Kauf01.pdf")), "/tmp/import/Ordner1/Kauf01.pdf"),
                        withSourceKey(new TransactionItem(getTestEntry(AccountTransaction.Type.DIVIDENDS,
                                        "Dividende01.pdf")), "/tmp/import/Ordner1/Dividende01.pdf"),
                        withSourceKey(new BuySellEntryItem(getTestEntry(PortfolioTransaction.Type.BUY, security, 100L,
                                        "Kauf01.pdf")), "/tmp/import/Ordner2/Kauf01.pdf"),
                        withSourceKey(new TransactionItem(getTestEntry(AccountTransaction.Type.DIVIDENDS,
                                        "Dividende01.pdf")), "/tmp/import/Ordner2/Dividende01.pdf"));

        assertThat(statusOf(items), is(List.of(Code.OK, Code.OK, Code.WARNING, Code.WARNING)));
    }

    @Test
    public void testNoDuplicateFromSameInputFile()
    {
        // identical transactions of the same input file are no duplicates,
        // even if the input file is identified by its path
        var items = List.<Extractor.Item>of( //
                        withSourceKey(new TransactionItem(getTestEntry(AccountTransaction.Type.FEES,
                                        "Kontoauszug01.pdf")), "/tmp/import/Ordner1/Kontoauszug01.pdf"),
                        withSourceKey(new TransactionItem(getTestEntry(AccountTransaction.Type.FEES,
                                        "Kontoauszug01.pdf")), "/tmp/import/Ordner1/Kontoauszug01.pdf"));

        assertThat(statusOf(items), is(List.of(Code.OK, Code.OK)));
    }

    @Test
    public void testIdenticalFeesInStatementAndDetailDocumentsStatementFirst()
    {
        // the account statement lists two identical fees, each fee is also
        // documented in a separate document: economically there are two fees
        var items = fees("AccountStatement01.pdf", "AccountStatement01.pdf", "FeeStatement01.pdf",
                        "FeeStatement02.pdf");

        assertThat(kept(items), is(2L));
    }

    @Test
    public void testIdenticalFeesInStatementAndDetailDocumentsDetailDocumentsFirst()
    {
        // same documents as above, but the detail documents are processed
        // first: the result must not depend on the order of the files
        var items = fees("FeeStatement01.pdf", "FeeStatement02.pdf", "AccountStatement01.pdf",
                        "AccountStatement01.pdf");

        assertThat(kept(items), is(2L));
    }

    @Test
    public void testNumberOfKeptTransactionsDoesNotDependOnFileNames()
    {
        // the account statement (Z.pdf) is sorted after the detail documents
        // (A.pdf, B.pdf): still two fees must be kept
        assertThat(kept(fees("A.pdf", "B.pdf", "Z.pdf", "Z.pdf")), is(2L));
        assertThat(kept(fees("Z.pdf", "Z.pdf", "A.pdf", "B.pdf")), is(2L));
    }

    @Test
    public void testFileWithMostOccurrencesDeterminesOnlyNumberOfKeptTransactions()
    {
        // A.pdf contains the fee once, Z.pdf twice: two fees are kept. Which
        // ones is determined by the fixed order (file, position): the fee of
        // A.pdf and the first fee of Z.pdf. The second fee of Z.pdf is marked
        // as duplicate although it stems from the file with the most
        // occurrences.
        var items = fees("A.pdf", "Z.pdf", "Z.pdf");
        assertThat(statusOf(items), is(List.of(Code.OK, Code.OK, Code.WARNING)));

        // same result if the files are read in a different order
        items = fees("Z.pdf", "Z.pdf", "A.pdf");
        assertThat(statusOf(items), is(List.of(Code.OK, Code.WARNING, Code.OK)));
    }

    @Test
    public void testDividendWithoutShareCountIsNoDuplicateWithinImport()
    {
        // compared with existing transactions, a dividend without number of
        // shares matches the same dividend with number of shares. Within the
        // import, amount and shares are compared exactly.
        var security = new Security("Example", "EUR");

        var withShares = getTestEntry(AccountTransaction.Type.DIVIDENDS, "Dividende01.pdf");
        withShares.setSecurity(security);
        withShares.setShares(100L * Values.Share.factor());

        var withoutShares = getTestEntry(AccountTransaction.Type.DIVIDENDS, "Kontoauszug01.pdf");
        withoutShares.setSecurity(security);

        var items = List.<Extractor.Item>of(new TransactionItem(withShares), new TransactionItem(withoutShares));

        assertThat(statusOf(items), is(List.of(Code.OK, Code.OK)));
    }

    @Test
    public void testDetailedPurchaseDocumentIsKeptRatherThanStatement()
    {
        // the account statement only lists the total, the purchase document
        // also the fee. By path, the statement would be kept
        // ("Kontoauszug01.pdf" < "Wertpapierabrechnung01.pdf").
        var security = new Security();

        var statement = getTestEntry(PortfolioTransaction.Type.BUY, security, 100L, "Kontoauszug01.pdf");
        var purchase = getTestEntry(PortfolioTransaction.Type.BUY, security, 100L, "Wertpapierabrechnung01.pdf");
        purchase.getPortfolioTransaction().addUnit(new Unit(Unit.Type.FEE, Money.of("EUR", 500)));

        var statementItem = new BuySellEntryItem(statement);
        var purchaseItem = new BuySellEntryItem(purchase);

        assertThat(statusOf(List.of(statementItem, purchaseItem)), is(List.of(Code.WARNING, Code.OK)));
        assertThat(statusOf(List.of(purchaseItem, statementItem)), is(List.of(Code.OK, Code.WARNING)));
    }

    @Test
    public void testDetailedDividendDocumentIsKeptRatherThanStatement()
    {
        // the dividend document lists withholding tax and solidarity
        // surcharge, the account statement only the net amount
        var statement = getTestEntry(AccountTransaction.Type.DIVIDENDS, "Kontoauszug01.pdf");
        var dividend = getTestEntry(AccountTransaction.Type.DIVIDENDS, "Dividende01.pdf");
        dividend.addUnit(new Unit(Unit.Type.TAX, Money.of("EUR", 300)));
        dividend.addUnit(new Unit(Unit.Type.TAX, Money.of("EUR", 16)));

        var withOneTax = getTestEntry(AccountTransaction.Type.DIVIDENDS, "Dividende01 (Kopie).pdf");
        withOneTax.addUnit(new Unit(Unit.Type.TAX, Money.of("EUR", 316)));

        // more fee and tax units win, regardless of the file names
        var items = List.<Extractor.Item>of(new TransactionItem(statement), new TransactionItem(withOneTax),
                        new TransactionItem(dividend));

        assertThat(statusOf(items), is(List.of(Code.WARNING, Code.WARNING, Code.OK)));
    }

    @Test
    public void testGrossValueUnitIsPreferredIfFeesAndTaxesAreEqual()
    {
        var security = new Security();

        var withoutForex = getTestEntry(PortfolioTransaction.Type.BUY, security, 100L, "Kauf01.pdf");
        var withForex = getTestEntry(PortfolioTransaction.Type.BUY, security, 100L, "Kauf02.pdf");
        withForex.getPortfolioTransaction().addUnit(new Unit(Unit.Type.GROSS_VALUE, Money.of("EUR", 1000),
                        Money.of("USD", 2000), BigDecimal.valueOf(0.5)));

        var items = List.<Extractor.Item>of(new BuySellEntryItem(withoutForex), new BuySellEntryItem(withForex));

        assertThat(statusOf(items), is(List.of(Code.WARNING, Code.OK)));
    }

    @Test
    public void testKeptCopyStemsFromAnotherInputFile()
    {
        // the account statement lists two fees, each fee is also documented
        // in a separate (more detailed) document: both fees of the statement
        // are duplicates, each one refers to another fee document
        var statement1 = new TransactionItem(getTestEntry(AccountTransaction.Type.FEES, "Kontoauszug01.pdf"));
        var statement2 = new TransactionItem(getTestEntry(AccountTransaction.Type.FEES, "Kontoauszug01.pdf"));

        var fee1 = getTestEntry(AccountTransaction.Type.FEES, "Gebuehr01.pdf");
        fee1.addUnit(new Unit(Unit.Type.TAX, Money.of("EUR", 10)));
        var fee2 = getTestEntry(AccountTransaction.Type.FEES, "Gebuehr02.pdf");
        fee2.addUnit(new Unit(Unit.Type.TAX, Money.of("EUR", 10)));
        var feeItem1 = new TransactionItem(fee1);
        var feeItem2 = new TransactionItem(fee2);

        var duplicates = DuplicatesWithinImport.of(List.of(statement1, statement2, feeItem1, feeItem2));

        assertThat(duplicates.isDuplicate(feeItem1), is(false));
        assertThat(duplicates.isDuplicate(feeItem2), is(false));
        assertThat(duplicates.keptCopyOf(statement1).orElseThrow().getSource(), is("Gebuehr01.pdf"));
        assertThat(duplicates.keptCopyOf(statement2).orElseThrow().getSource(), is("Gebuehr02.pdf"));
    }

    @Test
    public void testResultDoesNotDependOnOrderOfFiles()
    {
        // four files: an account statement with two identical fees and one
        // dividend, a copy of the account statement, and two fee documents
        var security = new Security();

        List<List<Extractor.Item>> files = List.of( //
                        List.of(new TransactionItem(getTestEntry(AccountTransaction.Type.FEES, "Kontoauszug01.pdf")),
                                        new TransactionItem(getTestEntry(AccountTransaction.Type.FEES,
                                                        "Kontoauszug01.pdf")),
                                        new TransactionItem(dividend(security, "Kontoauszug01.pdf"))),
                        List.of(new TransactionItem(getTestEntry(AccountTransaction.Type.FEES,
                                        "Kontoauszug01 (Kopie).pdf")),
                                        new TransactionItem(getTestEntry(AccountTransaction.Type.FEES,
                                                        "Kontoauszug01 (Kopie).pdf")),
                                        new TransactionItem(dividend(security, "Kontoauszug01 (Kopie).pdf"))),
                        List.of(new TransactionItem(getTestEntry(AccountTransaction.Type.FEES, "Gebuehr01.pdf"))),
                        List.of(new TransactionItem(getTestEntry(AccountTransaction.Type.FEES, "Gebuehr02.pdf"))));

        Set<Extractor.Item> expected = null;

        for (var order : permutations(List.of(0, 1, 2, 3)))
        {
            List<Extractor.Item> items = new ArrayList<>();
            order.forEach(index -> items.addAll(files.get(index)));

            var codes = statusOf(items);

            Set<Extractor.Item> keptItems = Collections.newSetFromMap(new IdentityHashMap<>());
            for (int ii = 0; ii < items.size(); ii++)
            {
                if (codes.get(ii) == Code.OK)
                    keptItems.add(items.get(ii));
            }

            // two fees and one dividend survive
            assertThat(order.toString(), keptItems.size(), is(3));

            if (expected == null)
                expected = keptItems;
            else
                assertThat(order.toString(), keptItems, is(expected));
        }
    }

    /**
     * Returns WARNING for duplicates and OK for kept items (as shown in the
     * review of the import).
     */
    private List<Code> statusOf(List<Extractor.Item> items)
    {
        var duplicates = DuplicatesWithinImport.of(items);
        return items.stream().map(item -> duplicates.isDuplicate(item) ? Code.WARNING : Code.OK).toList();
    }

    private long kept(List<Extractor.Item> items)
    {
        return statusOf(items).stream().filter(code -> code == Code.OK).count();
    }

    private List<Extractor.Item> fees(String... sources)
    {
        return Arrays.stream(sources) //
                        .<Extractor.Item>map(source -> new TransactionItem(
                                        getTestEntry(AccountTransaction.Type.FEES, source)))
                        .toList();
    }

    private AccountTransaction dividend(Security security, String source)
    {
        var transaction = getTestEntry(AccountTransaction.Type.DIVIDENDS, source);
        transaction.setSecurity(security);
        transaction.setAmount(500);
        return transaction;
    }

    /**
     * Returns all permutations of the list.
     */
    private static <T> List<List<T>> permutations(List<T> list)
    {
        if (list.isEmpty())
            return List.of(List.of());

        List<List<T>> result = new ArrayList<>();
        for (int ii = 0; ii < list.size(); ii++)
        {
            var head = list.get(ii);
            List<T> rest = new ArrayList<>(list);
            rest.remove(ii);
            for (var permutation : permutations(rest))
            {
                List<T> p = new ArrayList<>();
                p.add(head);
                p.addAll(permutation);
                result.add(p);
            }
        }
        return result;
    }

    private Extractor.Item withSourceKey(Extractor.Item item, String sourceKey)
    {
        item.setData(DuplicatesWithinImport.SOURCE_KEY, sourceKey);
        return item;
    }

    private AccountTransaction getTestEntry(AccountTransaction.Type type, String source)
    {
        var transaction = new AccountTransaction();
        transaction.setType(type);
        transaction.setAmount(1000);
        transaction.setCurrencyCode("EUR");
        transaction.setDateTime(LocalDateTime.of(2025, 12, 15, 0, 0));
        transaction.setSource(source);
        return transaction;
    }

    private PortfolioTransaction getTestEntry(PortfolioTransaction.Type type, Security security, String source)
    {
        var transaction = new PortfolioTransaction();
        transaction.setType(type);
        transaction.setSecurity(security);
        transaction.setAmount(1000);
        transaction.setShares(1000L);
        transaction.setCurrencyCode("EUR");
        transaction.setDateTime(LocalDateTime.of(2025, 12, 15, 0, 0));
        transaction.setSource(source);
        return transaction;
    }

    private BuySellEntry getTestEntry(PortfolioTransaction.Type type, Security security, long shares, String source)
    {
        var entry = new BuySellEntry(type);
        entry.setSecurity(security);
        entry.setShares(shares);
        entry.setAmount(1000);
        entry.setCurrencyCode("EUR");
        entry.setDate(LocalDateTime.of(2025, 12, 15, 0, 0));
        entry.setSource(source);
        return entry;
    }

    private AccountTransferEntry getTestTransfer(Account source, Account target, String filename)
    {
        var entry = new AccountTransferEntry(source, target);
        entry.setAmount(1000);
        entry.setCurrencyCode("EUR");
        entry.setDate(LocalDateTime.of(2025, 12, 15, 0, 0));
        entry.setSource(filename);
        return entry;
    }

    private PortfolioTransferEntry getTestTransfer(Portfolio source, Portfolio target, Security security,
                    String filename)
    {
        var entry = new PortfolioTransferEntry(source, target);
        entry.setSecurity(security);
        entry.setShares(1000L);
        entry.setAmount(1000);
        entry.setCurrencyCode("EUR");
        entry.setDate(LocalDateTime.of(2025, 12, 15, 0, 0));
        entry.setSource(filename);
        return entry;
    }
}
