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
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.junit.Test;

import name.abuchen.portfolio.datatransfer.Extractor;
import name.abuchen.portfolio.datatransfer.Extractor.BuySellEntryItem;
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
        var account = new Account();
        var action = new DetectDuplicatesAction(new Client(), true);

        assertThat(action.process(getTestEntry(AccountTransaction.Type.DIVIDENDS, "Dividende01.pdf"), account)
                        .getCode(), is(Code.OK));
        assertThat(action.process(getTestEntry(AccountTransaction.Type.DIVIDENDS, "Dividende01 (Kopie).pdf"),
                        account).getCode(), is(Code.WARNING));
    }

    @SuppressWarnings("nls")
    @Test
    public void testDuplicateWithinImport4DepositAndTransferInFromDifferentSources()
    {
        var account = new Account();
        var action = new DetectDuplicatesAction(new Client(), true);

        assertThat(action.process(getTestEntry(AccountTransaction.Type.DEPOSIT, "Kontoauszug01.pdf"), account)
                        .getCode(), is(Code.OK));
        assertThat(action.process(getTestEntry(AccountTransaction.Type.TRANSFER_IN, "Kontoauszug02.pdf"), account)
                        .getCode(), is(Code.WARNING));
    }

    @SuppressWarnings("nls")
    @Test
    public void testNoDuplicateWithinImportFromSameSource()
    {
        // a single document can contain identical transactions (e.g. two
        // identical fees on the same day)
        var account = new Account();
        var action = new DetectDuplicatesAction(new Client(), true);

        assertThat(action.process(getTestEntry(AccountTransaction.Type.FEES, "Kontoauszug01.pdf"), account)
                        .getCode(), is(Code.OK));
        assertThat(action.process(getTestEntry(AccountTransaction.Type.FEES, "Kontoauszug01.pdf"), account)
                        .getCode(), is(Code.OK));
    }

    @SuppressWarnings("nls")
    @Test
    public void testNoDuplicateWithinImportWithoutSource()
    {
        var account = new Account();
        var action = new DetectDuplicatesAction(new Client(), true);

        assertThat(action.process(getTestEntry(AccountTransaction.Type.FEES, null), account).getCode(),
                        is(Code.OK));
        assertThat(action.process(getTestEntry(AccountTransaction.Type.FEES, "Kontoauszug01.pdf"), account)
                        .getCode(), is(Code.OK));
        assertThat(action.process(getTestEntry(AccountTransaction.Type.FEES, null), account).getCode(),
                        is(Code.OK));
    }

    @SuppressWarnings("nls")
    @Test
    public void testNoDuplicateWithinImportWithDifferentValues()
    {
        var account = new Account();
        var action = new DetectDuplicatesAction(new Client(), true);

        assertThat(action.process(getTestEntry(AccountTransaction.Type.DIVIDENDS, "Dividende01.pdf"), account)
                        .getCode(), is(Code.OK));

        var otherAmount = getTestEntry(AccountTransaction.Type.DIVIDENDS, "Dividende02.pdf");
        otherAmount.setAmount(1001);
        assertThat(action.process(otherAmount, account).getCode(), is(Code.OK));

        var otherDate = getTestEntry(AccountTransaction.Type.DIVIDENDS, "Dividende03.pdf");
        otherDate.setDateTime(LocalDateTime.of(2025, 12, 16, 0, 0));
        assertThat(action.process(otherDate, account).getCode(), is(Code.OK));

        var otherType = getTestEntry(AccountTransaction.Type.INTEREST, "Zinsen01.pdf");
        assertThat(action.process(otherType, account).getCode(), is(Code.OK));
    }

    @SuppressWarnings("nls")
    @Test
    public void testNoDuplicateWithinImportForDifferentAccounts()
    {
        var action = new DetectDuplicatesAction(new Client(), true);

        assertThat(action.process(getTestEntry(AccountTransaction.Type.DIVIDENDS, "Dividende01.pdf"), new Account())
                        .getCode(), is(Code.OK));
        assertThat(action.process(getTestEntry(AccountTransaction.Type.DIVIDENDS, "Dividende01 (Kopie).pdf"),
                        new Account()).getCode(), is(Code.OK));
    }

    @SuppressWarnings("nls")
    @Test
    public void testDuplicateWithExistingTransactionIsStillDetectedWithinImport()
    {
        var action = new DetectDuplicatesAction(new Client(), true);

        var existing = getTestEntry(AccountTransaction.Type.DIVIDENDS, null);
        var status = action.process(getTestEntry(AccountTransaction.Type.DIVIDENDS, "Dividende01.pdf"),
                        account(existing));

        assertThat(status.getCode(), is(Code.WARNING));
    }

    @SuppressWarnings("nls")
    @Test
    public void testDuplicateWithinImport4PortfolioTransactionFromDifferentSources()
    {
        var security = new Security();
        var portfolio = new Portfolio();
        var action = new DetectDuplicatesAction(new Client(), true);

        assertThat(action.process(getTestEntry(PortfolioTransaction.Type.DELIVERY_INBOUND, security, "Eingang01.pdf"),
                        portfolio).getCode(), is(Code.OK));
        assertThat(action.process(getTestEntry(PortfolioTransaction.Type.DELIVERY_INBOUND, security, "Eingang02.pdf"),
                        portfolio).getCode(), is(Code.WARNING));
    }

    @SuppressWarnings("nls")
    @Test
    public void testNoDuplicateWithinImport4PortfolioTransactionWithDifferentSecurities()
    {
        var portfolio = new Portfolio();
        var action = new DetectDuplicatesAction(new Client(), true);

        assertThat(action.process(getTestEntry(PortfolioTransaction.Type.DELIVERY_INBOUND, new Security(),
                        "Eingang01.pdf"), portfolio).getCode(), is(Code.OK));
        assertThat(action.process(getTestEntry(PortfolioTransaction.Type.DELIVERY_INBOUND, new Security(),
                        "Eingang02.pdf"), portfolio).getCode(), is(Code.OK));
    }

    @SuppressWarnings("nls")
    @Test
    public void testDuplicateWithinImport4BuySellEntryFromDifferentSources()
    {
        var security = new Security();
        var account = new Account();
        var portfolio = new Portfolio();
        var action = new DetectDuplicatesAction(new Client(), true);

        assertThat(action.process(getTestEntry(PortfolioTransaction.Type.BUY, security, 100L, "Kauf01.pdf"), account,
                        portfolio).getCode(), is(Code.OK));
        assertThat(action.process(getTestEntry(PortfolioTransaction.Type.BUY, security, 100L, "Kauf01 (1).pdf"),
                        account, portfolio).getCode(), is(Code.WARNING));

        // same security and shares, but different amount
        var otherAmount = getTestEntry(PortfolioTransaction.Type.BUY, security, 100L, "Kauf02.pdf");
        otherAmount.setAmount(2000);
        assertThat(action.process(otherAmount, account, portfolio).getCode(), is(Code.OK));
    }

    @SuppressWarnings("nls")
    @Test
    public void testDuplicateWithinImport4PurchaseAndDelivery()
    {
        var security = new Security();
        var portfolio = new Portfolio();
        var action = new DetectDuplicatesAction(new Client(), true);

        assertThat(action.process(getTestEntry(PortfolioTransaction.Type.BUY, security, 100L, "Kauf01.pdf"),
                        new Account(), portfolio).getCode(), is(Code.OK));

        var delivery = getTestEntry(PortfolioTransaction.Type.DELIVERY_INBOUND, security, "Eingang01.pdf");
        delivery.setShares(100L);
        assertThat(action.process(delivery, portfolio).getCode(), is(Code.WARNING));
    }

    @SuppressWarnings("nls")
    @Test
    public void testDuplicateWithinImport4AccountTransferEntryFromDifferentSources()
    {
        var source = new Account();
        var target = new Account();
        var action = new DetectDuplicatesAction(new Client(), true);

        assertThat(action.process(getTestTransfer(source, target, "Umbuchung01.pdf"), source, target).getCode(),
                        is(Code.OK));
        assertThat(action.process(getTestTransfer(source, target, "Umbuchung02.pdf"), source, target).getCode(),
                        is(Code.WARNING));
    }

    @SuppressWarnings("nls")
    @Test
    public void testDuplicateWithinImportViaExtractedItems()
    {
        // simulates the same document imported twice with different file names
        // plus a document with two identical transactions
        var security = new Security();
        var account = new Account();
        account.setCurrencyCode("EUR");
        var portfolio = new Portfolio();
        var context = context(account, portfolio);

        var action = new DetectDuplicatesAction(new Client(), true);

        List<Extractor.Item> items = new ArrayList<>();
        items.add(new BuySellEntryItem(getTestEntry(PortfolioTransaction.Type.BUY, security, 100L, "Kauf01.pdf")));
        items.add(new TransactionItem(getTestEntry(AccountTransaction.Type.FEES, "Kauf01.pdf")));
        items.add(new TransactionItem(getTestEntry(AccountTransaction.Type.FEES, "Kauf01.pdf")));
        items.add(new BuySellEntryItem(getTestEntry(PortfolioTransaction.Type.BUY, security, 100L, "Kauf01_2.pdf")));
        items.add(new TransactionItem(getTestEntry(AccountTransaction.Type.FEES, "Kauf01_2.pdf")));
        items.add(new TransactionItem(getTestEntry(AccountTransaction.Type.FEES, "Kauf01_2.pdf")));

        var codes = items.stream().map(item -> item.apply(action, context).getCode()).toList();

        assertThat(codes, is(List.of(Code.OK, Code.OK, Code.OK, Code.WARNING, Code.WARNING, Code.WARNING)));
    }

    @SuppressWarnings("nls")
    @Test
    public void testDuplicateWithinImportFromFilesWithSameNameInDifferentFolders()
    {
        // e.g. a ZIP archive containing Ordner1/Kauf01.pdf and
        // Ordner2/Kauf01.pdf: the source (file name) is identical, the input
        // files are not
        var security = new Security();
        var account = new Account();
        account.setCurrencyCode("EUR");
        var portfolio = new Portfolio();
        var context = context(account, portfolio);

        List<Extractor.Item> items = new ArrayList<>();
        items.add(withSourceKey(new BuySellEntryItem(getTestEntry(PortfolioTransaction.Type.BUY, security, 100L,
                        "Kauf01.pdf")), "/tmp/import/Ordner1/Kauf01.pdf"));
        items.add(withSourceKey(new TransactionItem(getTestEntry(AccountTransaction.Type.DIVIDENDS, "Dividende01.pdf")),
                        "/tmp/import/Ordner1/Dividende01.pdf"));
        items.add(withSourceKey(new BuySellEntryItem(getTestEntry(PortfolioTransaction.Type.BUY, security, 100L,
                        "Kauf01.pdf")), "/tmp/import/Ordner2/Kauf01.pdf"));
        items.add(withSourceKey(new TransactionItem(getTestEntry(AccountTransaction.Type.DIVIDENDS, "Dividende01.pdf")),
                        "/tmp/import/Ordner2/Dividende01.pdf"));

        var action = new DetectDuplicatesAction(new Client(), true, DetectDuplicatesAction.sourceKeysOf(items));

        var codes = items.stream().map(item -> item.apply(action, context).getCode()).toList();

        assertThat(codes, is(List.of(Code.OK, Code.OK, Code.WARNING, Code.WARNING)));
    }

    @SuppressWarnings("nls")
    @Test
    public void testNoDuplicateWithinImportFromSameInputFile()
    {
        // identical transactions of the same input file are no duplicates,
        // even if the input file is identified by its path
        var account = new Account();
        account.setCurrencyCode("EUR");
        var context = context(account, new Portfolio());

        List<Extractor.Item> items = new ArrayList<>();
        items.add(withSourceKey(new TransactionItem(getTestEntry(AccountTransaction.Type.FEES, "Kontoauszug01.pdf")),
                        "/tmp/import/Ordner1/Kontoauszug01.pdf"));
        items.add(withSourceKey(new TransactionItem(getTestEntry(AccountTransaction.Type.FEES, "Kontoauszug01.pdf")),
                        "/tmp/import/Ordner1/Kontoauszug01.pdf"));

        var action = new DetectDuplicatesAction(new Client(), true, DetectDuplicatesAction.sourceKeysOf(items));

        var codes = items.stream().map(item -> item.apply(action, context).getCode()).toList();

        assertThat(codes, is(List.of(Code.OK, Code.OK)));
    }

    @SuppressWarnings("nls")
    @Test
    public void testSourceKeysOfUsesSourceKeyOfItem()
    {
        var security = new Security();
        var buySell = getTestEntry(PortfolioTransaction.Type.BUY, security, 100L, "Kauf01.pdf");
        var transfer = getTestTransfer(new Account(), new Account(), "Umbuchung01.pdf");
        var dividend = getTestEntry(AccountTransaction.Type.DIVIDENDS, "Dividende01.pdf");

        List<Extractor.Item> items = new ArrayList<>();
        items.add(withSourceKey(new BuySellEntryItem(buySell), "/tmp/import/Ordner1/Kauf01.pdf"));
        items.add(withSourceKey(new Extractor.AccountTransferItem(transfer, true), "/tmp/import/Umbuchung01.pdf"));
        items.add(new TransactionItem(dividend));

        var sourceKeyOf = DetectDuplicatesAction.sourceKeysOf(items);

        assertThat(sourceKeyOf.apply(buySell.getPortfolioTransaction()), is("/tmp/import/Ordner1/Kauf01.pdf"));
        assertThat(sourceKeyOf.apply(buySell.getAccountTransaction()), is("/tmp/import/Ordner1/Kauf01.pdf"));
        assertThat(sourceKeyOf.apply(transfer.getSourceTransaction()), is("/tmp/import/Umbuchung01.pdf"));
        assertThat(sourceKeyOf.apply(transfer.getTargetTransaction()), is("/tmp/import/Umbuchung01.pdf"));

        // no source key --> fall back to the source (file name)
        assertThat(sourceKeyOf.apply(dividend), is("Dividende01.pdf"));
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
