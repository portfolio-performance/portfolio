package name.abuchen.portfolio.datatransfer.actions;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.junit.Test;

import name.abuchen.portfolio.datatransfer.Extractor;
import name.abuchen.portfolio.datatransfer.Extractor.AccountTransferItem;
import name.abuchen.portfolio.datatransfer.Extractor.BuySellEntryItem;
import name.abuchen.portfolio.datatransfer.Extractor.PortfolioTransferItem;
import name.abuchen.portfolio.datatransfer.Extractor.TransactionItem;
import name.abuchen.portfolio.datatransfer.ImportAction;
import name.abuchen.portfolio.datatransfer.ImportAction.Status.Code;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.AccountTransferEntry;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.PortfolioTransferEntry;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction;

public class DetectDuplicatesActionTest
{
    @SuppressWarnings("nls")
    @Test
    public void testDuplicateDetection4AccountTransaction() throws IntrospectionException, ReflectiveOperationException
    {
        var action = new DetectDuplicatesAction(new Client());

        new PropertyChecker<AccountTransaction>(
                        AccountTransaction.class, "note", "source", "forex", "monetaryAmount", "exDate", "updatedAt")
                                        .before((name, o, c) -> assertThat(name,
                                                        action.process(o, account(c)).getCode(), is(Code.WARNING)))
                                        .after((name, o, c) -> assertThat(name, action.process(o, account(c)).getCode(),
                                                        is(Code.OK)))
                                        .run();
    }

    @SuppressWarnings("nls")
    @Test
    public void testDuplicateDetection4PortfolioTransaction()
                    throws IntrospectionException, ReflectiveOperationException
    {
        var action = new DetectDuplicatesAction(new Client());

        new PropertyChecker<PortfolioTransaction>(PortfolioTransaction.class, "fees", "taxes", "note", "source",
                        "forex", "monetaryAmount", "updatedAt") //
                                        .before((name, o, c) -> assertThat(name,
                                                        action.process(o, portfolio(c)).getCode(), is(Code.WARNING)))
                                        .after((name, o, c) -> assertThat(name,
                                                        action.process(o, portfolio(c)).getCode(), is(Code.OK)))
                                        .run();
    }

    @SuppressWarnings("nls")
    @Test
    public void testDuplicateDetectionWithPurchaseAndDeliveryPairs()
                    throws IntrospectionException, ReflectiveOperationException
    {
        var action = new DetectDuplicatesAction(new Client());

        new PropertyChecker<PortfolioTransaction>(PortfolioTransaction.class, "type", "fees", "taxes", "note", "source",
                        "forex", "monetaryAmount", "updatedAt") //
                                        .before((name, o, c) -> {
                                            o.setType(PortfolioTransaction.Type.BUY);
                                            c.setType(PortfolioTransaction.Type.DELIVERY_INBOUND);
                                            assertThat(name, action.process(o, portfolio(c)).getCode(),
                                                            is(Code.WARNING));
                                        }) //
                                        .after((name, o, c) -> assertThat(name,
                                                        action.process(o, portfolio(c)).getCode(), is(Code.OK)))
                                        .run();

        new PropertyChecker<PortfolioTransaction>(PortfolioTransaction.class, "type", "fees", "taxes", "note", "source",
                        "forex", "monetaryAmount", "updatedAt") //
                                        .before((name, o, c) -> {
                                            o.setType(PortfolioTransaction.Type.SELL);
                                            c.setType(PortfolioTransaction.Type.DELIVERY_OUTBOUND);
                                            assertThat(name, action.process(o, portfolio(c)).getCode(),
                                                            is(Code.WARNING));
                                        }) //
                                        .after((name, o, c) -> assertThat(name,
                                                        action.process(o, portfolio(c)).getCode(), is(Code.OK)))
                                        .run();

    }

    @Test
    public void testDuplicateDetectionWithTransferInAndDeposit()
    {
        var transferIn = getTestEntry(AccountTransaction.Type.TRANSFER_IN);
        var deposit = getTestEntry(AccountTransaction.Type.DEPOSIT);

        var action = new DetectDuplicatesAction(new Client());

        var status = action.process(deposit, account(transferIn));

        assertThat(status.getCode(), is(Code.WARNING));

        // check vice versa
        status = action.process(transferIn, account(deposit));

        assertThat(status.getCode(), is(Code.WARNING));
    }

    @Test
    public void testDuplicateDetectionWithTransferOutAndWithdrawal()
    {
        var transferOut = getTestEntry(AccountTransaction.Type.TRANSFER_OUT);
        var withdrawl = getTestEntry(AccountTransaction.Type.REMOVAL);

        var action = new DetectDuplicatesAction(new Client());

        var status = action.process(withdrawl, account(transferOut));

        assertThat(status.getCode(), is(Code.WARNING));

        // check vice versa
        status = action.process(transferOut, account(withdrawl));

        assertThat(status.getCode(), is(Code.WARNING));
    }

    @SuppressWarnings("nls")
    @Test
    public void testNoDuplicateDetectionWithinImportByDefault()
    {
        var account = new Account();
        var action = new DetectDuplicatesAction(new Client());

        assertThat(action.process(getTestEntry(AccountTransaction.Type.DIVIDENDS, "Dividende01.pdf"), account)
                        .getCode(), is(Code.OK));
        assertThat(action.process(getTestEntry(AccountTransaction.Type.DIVIDENDS, "Dividende01 (Kopie).pdf"),
                        account).getCode(), is(Code.OK));
    }

    @SuppressWarnings("nls")
    @Test
    public void testDuplicateWithinImport4AccountTransactionFromDifferentSources()
    {
        var items = List.<Extractor.Item>of( //
                        new TransactionItem(getTestEntry(AccountTransaction.Type.DIVIDENDS, "Dividende01.pdf")),
                        new TransactionItem(getTestEntry(AccountTransaction.Type.DIVIDENDS,
                                        "Dividende01 (Kopie).pdf")));

        // the transaction to keep is determined by the sorted file names, not
        // by the order of the items: "Dividende01 (Kopie).pdf" comes first
        assertThat(statusOf(items), is(List.of(Code.WARNING, Code.OK)));
    }

    @SuppressWarnings("nls")
    @Test
    public void testDuplicateWithinImport4RemovalAndAccountTransferFromDifferentSources()
    {
        var source = new Account();
        var target = new Account();

        var removal = getTestEntry(AccountTransaction.Type.REMOVAL, "Kontoauszug01.pdf");
        var transfer = getTestTransfer(source, target, "Kontoauszug02.pdf");

        var action = new DetectDuplicatesAction(new Client(),
                        List.of(new TransactionItem(removal), new AccountTransferItem(transfer, true)));

        assertThat(action.process(removal, source).getCode(), is(Code.OK));
        assertThat(action.process(transfer, source, target).getCode(), is(Code.WARNING));
    }

    @SuppressWarnings("nls")
    @Test
    public void testNoDuplicateWithinImportFromSameSource()
    {
        // a single document can contain identical transactions (e.g. two
        // identical fees on the same day)
        var items = List.<Extractor.Item>of( //
                        new TransactionItem(getTestEntry(AccountTransaction.Type.FEES, "Kontoauszug01.pdf")),
                        new TransactionItem(getTestEntry(AccountTransaction.Type.FEES, "Kontoauszug01.pdf")));

        assertThat(statusOf(items), is(List.of(Code.OK, Code.OK)));
    }

    @SuppressWarnings("nls")
    @Test
    public void testNoDuplicateWithinImportWithoutSource()
    {
        var items = List.<Extractor.Item>of( //
                        new TransactionItem(getTestEntry(AccountTransaction.Type.FEES, null)),
                        new TransactionItem(getTestEntry(AccountTransaction.Type.FEES, "Kontoauszug01.pdf")),
                        new TransactionItem(getTestEntry(AccountTransaction.Type.FEES, null)));

        assertThat(statusOf(items), is(List.of(Code.OK, Code.OK, Code.OK)));
    }

    @SuppressWarnings("nls")
    @Test
    public void testNoDuplicateWithinImportWithDifferentValues()
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

    @SuppressWarnings("nls")
    @Test
    public void testDuplicateWithExistingTransactionIsStillDetectedWithinImport()
    {
        var subject = getTestEntry(AccountTransaction.Type.DIVIDENDS, "Dividende01.pdf");
        var action = new DetectDuplicatesAction(new Client(), List.of(new TransactionItem(subject)));

        var existing = getTestEntry(AccountTransaction.Type.DIVIDENDS, null);

        assertThat(action.process(subject, account(existing)).getCode(), is(Code.WARNING));
    }

    @SuppressWarnings("nls")
    @Test
    public void testDuplicateWithinImport4PortfolioTransactionFromDifferentSources()
    {
        var security = new Security();

        var items = List.<Extractor.Item>of( //
                        new TransactionItem(getTestEntry(PortfolioTransaction.Type.DELIVERY_INBOUND, security,
                                        "Eingang01.pdf")),
                        new TransactionItem(getTestEntry(PortfolioTransaction.Type.DELIVERY_INBOUND, security,
                                        "Eingang02.pdf")));

        assertThat(statusOf(items), is(List.of(Code.OK, Code.WARNING)));
    }

    @SuppressWarnings("nls")
    @Test
    public void testNoDuplicateWithinImport4PortfolioTransactionWithDifferentSecurities()
    {
        var items = List.<Extractor.Item>of( //
                        new TransactionItem(getTestEntry(PortfolioTransaction.Type.DELIVERY_INBOUND, new Security(),
                                        "Eingang01.pdf")),
                        new TransactionItem(getTestEntry(PortfolioTransaction.Type.DELIVERY_INBOUND, new Security(),
                                        "Eingang02.pdf")));

        assertThat(statusOf(items), is(List.of(Code.OK, Code.OK)));
    }

    @SuppressWarnings("nls")
    @Test
    public void testDuplicateWithinImport4BuySellEntryFromDifferentSources()
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

    @SuppressWarnings("nls")
    @Test
    public void testDuplicateWithinImport4PurchaseAndDelivery()
    {
        var security = new Security();

        var delivery = getTestEntry(PortfolioTransaction.Type.DELIVERY_INBOUND, security, "Eingang01.pdf");
        delivery.setShares(100L);

        var items = List.<Extractor.Item>of( //
                        new BuySellEntryItem(getTestEntry(PortfolioTransaction.Type.BUY, security, 100L, "Kauf01.pdf")),
                        new TransactionItem(delivery));

        assertThat(statusOf(items), is(List.of(Code.WARNING, Code.OK)));
    }

    @SuppressWarnings("nls")
    @Test
    public void testDuplicateWithinImport4AccountTransferEntryFromDifferentSources()
    {
        var source = new Account();
        var target = new Account();

        var transfer1 = getTestTransfer(source, target, "Umbuchung01.pdf");
        var transfer2 = getTestTransfer(source, target, "Umbuchung02.pdf");

        var action = new DetectDuplicatesAction(new Client(),
                        List.of(new AccountTransferItem(transfer1, true), new AccountTransferItem(transfer2, true)));

        assertThat(action.process(transfer1, source, target).getCode(), is(Code.OK));
        assertThat(action.process(transfer2, source, target).getCode(), is(Code.WARNING));
    }

    @SuppressWarnings("nls")
    @Test
    public void testDuplicateWithinImport4PortfolioTransferEntryFromDifferentSources()
    {
        var security = new Security();
        var source = new Portfolio();
        var target = new Portfolio();

        var transfer1 = getTestTransfer(source, target, security, "Depotuebertrag01.pdf");
        var transfer2 = getTestTransfer(source, target, security, "Depotuebertrag01.pdf");

        var action = new DetectDuplicatesAction(new Client(), List.of( //
                        withSourceKey(new PortfolioTransferItem(transfer1), "/tmp/import/Ordner1/Depotuebertrag01.pdf"),
                        withSourceKey(new PortfolioTransferItem(transfer2),
                                        "/tmp/import/Ordner2/Depotuebertrag01.pdf")));

        assertThat(action.process(transfer1, source, target).getCode(), is(Code.OK));
        assertThat(action.process(transfer2, source, target).getCode(), is(Code.WARNING));
    }

    @SuppressWarnings("nls")
    @Test
    public void testNoDuplicateWithinImport4PortfolioTransferEntryFromSameSource()
    {
        var security = new Security();
        var source = new Portfolio();
        var target = new Portfolio();

        var transfer1 = getTestTransfer(source, target, security, "Depotuebertrag01.pdf");
        var transfer2 = getTestTransfer(source, target, security, "Depotuebertrag01.pdf");

        var action = new DetectDuplicatesAction(new Client(), List.of( //
                        withSourceKey(new PortfolioTransferItem(transfer1), "/tmp/import/Ordner1/Depotuebertrag01.pdf"),
                        withSourceKey(new PortfolioTransferItem(transfer2),
                                        "/tmp/import/Ordner1/Depotuebertrag01.pdf")));

        assertThat(action.process(transfer1, source, target).getCode(), is(Code.OK));
        assertThat(action.process(transfer2, source, target).getCode(), is(Code.OK));
    }

    @SuppressWarnings("nls")
    @Test
    public void testDuplicateWithinImportViaExtractedItems()
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

    @SuppressWarnings("nls")
    @Test
    public void testDuplicateWithinImportFromFilesWithSameNameInDifferentFolders()
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

    @SuppressWarnings("nls")
    @Test
    public void testNoDuplicateWithinImportFromSameInputFile()
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

    @SuppressWarnings("nls")
    @Test
    public void testIdenticalFeesInStatementAndDetailDocumentsStatementFirst()
    {
        // the account statement lists two identical fees, each fee is also
        // documented in a separate document: economically there are two fees
        var items = fees("AccountStatement01.pdf", "AccountStatement01.pdf", "FeeStatement01.pdf",
                        "FeeStatement02.pdf");

        assertThat(kept(items), is(2L));
    }

    @SuppressWarnings("nls")
    @Test
    public void testIdenticalFeesInStatementAndDetailDocumentsDetailDocumentsFirst()
    {
        // same documents as above, but the detail documents are processed
        // first: the result must not depend on the order of the files
        var items = fees("FeeStatement01.pdf", "FeeStatement02.pdf", "AccountStatement01.pdf",
                        "AccountStatement01.pdf");

        assertThat(kept(items), is(2L));
    }

    @SuppressWarnings("nls")
    @Test
    public void testNumberOfKeptTransactionsDoesNotDependOnFileNames()
    {
        // the account statement (Z.pdf) is sorted after the detail documents
        // (A.pdf, B.pdf): still two fees must be kept
        assertThat(kept(fees("A.pdf", "B.pdf", "Z.pdf", "Z.pdf")), is(2L));
        assertThat(kept(fees("Z.pdf", "Z.pdf", "A.pdf", "B.pdf")), is(2L));
    }

    @SuppressWarnings("nls")
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

    @SuppressWarnings("nls")
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

    private List<Code> statusOf(List<Extractor.Item> items)
    {
        var account = new Account();
        account.setCurrencyCode("EUR"); //$NON-NLS-1$
        var context = context(account, new Portfolio());

        var action = new DetectDuplicatesAction(new Client(), items);

        return items.stream().map(item -> item.apply(action, context).getCode()).toList();
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
        item.setData(DetectDuplicatesAction.SOURCE_KEY, sourceKey);
        return item;
    }

    private AccountTransaction getTestEntry(AccountTransaction.Type type, String source)
    {
        var transaction = getTestEntry(type);
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
        transaction.setCurrencyCode("EUR"); //$NON-NLS-1$
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
        entry.setCurrencyCode("EUR"); //$NON-NLS-1$
        entry.setDate(LocalDateTime.of(2025, 12, 15, 0, 0));
        entry.setSource(source);
        return entry;
    }

    private AccountTransferEntry getTestTransfer(Account source, Account target, String filename)
    {
        var entry = new AccountTransferEntry(source, target);
        entry.setAmount(1000);
        entry.setCurrencyCode("EUR"); //$NON-NLS-1$
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
        entry.setCurrencyCode("EUR"); //$NON-NLS-1$
        entry.setDate(LocalDateTime.of(2025, 12, 15, 0, 0));
        entry.setSource(filename);
        return entry;
    }

    private ImportAction.Context context(Account account, Portfolio portfolio)
    {
        return new ImportAction.Context()
        {
            @Override
            public Account getAccount(String currencyCode)
            {
                return account;
            }

            @Override
            public Portfolio getPortfolio()
            {
                return portfolio;
            }

            @Override
            public Account getSecondaryAccount(String currencyCode)
            {
                return null;
            }

            @Override
            public Portfolio getSecondaryPortfolio()
            {
                return null;
            }
        };
    }

    private AccountTransaction getTestEntry(AccountTransaction.Type type)
    {
        var transaction = new AccountTransaction();
        transaction.setType(type);
        transaction.setAmount(1000);
        transaction.setCurrencyCode("EUR"); //$NON-NLS-1$
        transaction.setDateTime(LocalDateTime.of(2025, 12, 15, 0, 0));

        return transaction;
    }

    private Account account(AccountTransaction t)
    {
        Account a = new Account();
        a.setCurrencyCode(t.getCurrencyCode());
        a.addTransaction(t);
        return a;
    }

    private Portfolio portfolio(PortfolioTransaction t)
    {
        Portfolio p = new Portfolio();
        p.addTransaction(t);
        return p;
    }

    @FunctionalInterface
    private interface Consumer<T>
    {
        void accept(String name, T original, T copy);
    }

    private static class PropertyChecker<T extends Transaction>
    {
        private Random random = new Random();

        private Class<T> type;
        private List<PropertyDescriptor> properties = new ArrayList<>();

        private Consumer<T> before;
        private Consumer<T> after;

        public PropertyChecker(Class<T> type, String... excludes) throws IntrospectionException
        {
            this.type = type;
            Set<String> excludedSet = new HashSet<>(Arrays.asList(excludes));

            BeanInfo info = Introspector.getBeanInfo(type);
            for (PropertyDescriptor p : info.getPropertyDescriptors()) // NOSONAR
            {
                if (excludedSet.contains(p.getName()))
                    continue;

                if (p.getWriteMethod() == null || p.getReadMethod() == null)
                    continue;

                properties.add(p);
            }
        }

        public PropertyChecker<T> before(Consumer<T> before)
        {
            this.before = before;
            return this;
        }

        public PropertyChecker<T> after(Consumer<T> after)
        {
            this.after = after;
            return this;
        }

        public void run() throws ReflectiveOperationException
        {
            for (PropertyDescriptor p : properties)
                check(p);
        }

        private void check(PropertyDescriptor change) throws ReflectiveOperationException
        {
            T instance = type.getDeclaredConstructor().newInstance();
            T other = type.getDeclaredConstructor().newInstance();

            for (PropertyDescriptor p : properties)
            {
                Object argument = arg(p.getPropertyType());
                p.getWriteMethod().invoke(instance, argument);
                p.getWriteMethod().invoke(other, argument);
            }

            before.accept(change.getName(), instance, other);

            change.getWriteMethod().invoke(other,
                            alternative(change.getPropertyType(), change.getReadMethod().invoke(other)));

            after.accept(change.getName(), instance, other);
        }

        private Object alternative(Class<?> propertyType, Object value)
        {
            if (propertyType == String.class)
            {
                return "x" + value; //$NON-NLS-1$
            }
            else if (propertyType == long.class)
            {
                return ((long) value) + 1;
            }
            else if (propertyType == LocalDate.class)
            {
                return LocalDate.of(1999, 1, 1);
            }
            else if (propertyType == LocalDateTime.class)
            {
                return LocalDateTime.of(1999, 1, 1, 0, 0, 0);
            }
            else if (propertyType == Security.class)
            {
                return new Security();
            }
            else if (propertyType == AccountTransaction.Type.class)
            {
                return AccountTransaction.Type.values()[(((AccountTransaction.Type) value).ordinal() + 1)
                                % AccountTransaction.Type.values().length];
            }
            else if (propertyType == PortfolioTransaction.Type.class)
            {
                return PortfolioTransaction.Type.values()[(((PortfolioTransaction.Type) value).ordinal() + 1)
                                % PortfolioTransaction.Type.values().length];
            }
            else
            {
                throw new UnsupportedOperationException(propertyType.getName());
            }
        }

        private Object arg(Class<?> propertyType)
        {
            if (propertyType == String.class)
            {
                char start = ' ';
                char end = 'z';
                int range = end - start;

                StringBuilder builder = new StringBuilder();
                for (int ii = 0; ii < 10; ii++)
                    builder.append((char) (random.nextInt(range) + start));

                return builder.toString();
            }
            else if (propertyType == long.class)
            {
                return random.nextLong();
            }
            else if (propertyType == LocalDate.class)
            {
                return LocalDate.of(2000 + random.nextInt(30), 1, 1);
            }
            else if (propertyType == LocalDateTime.class)
            {
                return LocalDateTime.of(2000 + random.nextInt(30), 1, 1, 0, 0, 0);
            }
            else if (propertyType == Security.class)
            {
                return new Security();
            }
            else if (propertyType == AccountTransaction.Type.class)
            {
                return AccountTransaction.Type.values()[random.nextInt(AccountTransaction.Type.values().length)];
            }
            else if (propertyType == PortfolioTransaction.Type.class)
            {
                return PortfolioTransaction.Type.values()[random.nextInt(PortfolioTransaction.Type.values().length)];
            }
            else
            {
                throw new UnsupportedOperationException(propertyType.getName());
            }
        }
    }
}
