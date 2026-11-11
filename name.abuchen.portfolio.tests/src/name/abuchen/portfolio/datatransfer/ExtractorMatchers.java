package name.abuchen.portfolio.datatransfer;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;

import name.abuchen.portfolio.datatransfer.Extractor.AccountTransferItem;
import name.abuchen.portfolio.datatransfer.Extractor.BuySellEntryItem;
import name.abuchen.portfolio.datatransfer.Extractor.Item;
import name.abuchen.portfolio.datatransfer.Extractor.PortfolioTransferItem;
import name.abuchen.portfolio.datatransfer.Extractor.SecurityItem;
import name.abuchen.portfolio.datatransfer.Extractor.SkippedItem;
import name.abuchen.portfolio.datatransfer.Extractor.TransactionItem;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.AccountTransferEntry;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.PortfolioTransferEntry;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityProperty;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.model.Transaction.Unit.Type;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

public class ExtractorMatchers
{
    private static class PropertyMatcher<T, V> extends TypeSafeDiagnosingMatcher<T>
    {
        private String label;
        private V expectedValue;
        private Function<T, V> value;

        public PropertyMatcher(String label, V expectedValue, Function<T, V> value)
        {
            this.label = label;
            this.expectedValue = expectedValue;
            this.value = value;
        }

        @Override
        protected boolean matchesSafely(T transaction, Description mismatchDescription)
        {
            Object actualValue;
            try
            {
                actualValue = value.apply(transaction);
            }
            catch (AssertionError e)
            {
                mismatchDescription.appendText(label).appendText(" = " + e.getMessage()); //$NON-NLS-1$
                return false;
            }

            if (!Objects.equals(expectedValue, actualValue))
            {
                mismatchDescription.appendText(label).appendText(" = ").appendText(String.valueOf(actualValue)); //$NON-NLS-1$
                return false;
            }

            return true;
        }

        @Override
        public void describeTo(Description description)
        {
            description.appendText(label).appendText(" = ").appendText(String.valueOf(expectedValue)); //$NON-NLS-1$
        }
    }

    private static class ExtractorItemMatcher<V> extends TypeSafeDiagnosingMatcher<Extractor.Item>
    {
        private String label;
        private Function<Extractor.Item, V> transaction;
        private Matcher<V>[] properties;

        public ExtractorItemMatcher(String label, Function<Extractor.Item, V> transaction, Matcher<V>[] properties)
        {
            this.label = label;
            this.transaction = transaction;
            this.properties = properties;
        }

        @Override
        protected boolean matchesSafely(Extractor.Item item, Description mismatchDescription)
        {
            var tx = transaction.apply(item);
            if (tx == null)
            {
                mismatchDescription.appendText("\n* not a '").appendText(label).appendText("' item"); //$NON-NLS-1$ //$NON-NLS-2$
                return false;
            }

            for (Matcher<V> property : properties)
            {
                if (!property.matches(tx))
                {
                    mismatchDescription.appendText("\n* ").appendText(label).appendText(" with "); //$NON-NLS-1$ //$NON-NLS-2$
                    property.describeMismatch(tx, mismatchDescription);
                    return false;
                }
            }

            return true;
        }

        @Override
        public void describeTo(Description description)
        {
            description.appendText(label).appendText(" with:"); //$NON-NLS-1$
            for (Matcher<V> p : properties)
            {
                description.appendText("\n - "); //$NON-NLS-1$
                p.describeTo(description);
            }
        }
    }

    private static class AccountTransactionMatcher extends ExtractorItemMatcher<Transaction>
    {
        public AccountTransactionMatcher(String label, AccountTransaction.Type type, Matcher<Transaction>[] properties)
        {
            super(label, item -> item instanceof TransactionItem tx //
                            && tx.getSubject() instanceof AccountTransaction atx && atx.getType() == type ? atx : null,
                            properties);
        }
    }

    private static class PairedTransactionMatcher extends ExtractorItemMatcher<Transaction>
    {
        private final String firstLabel;
        private final String secondLabel;

        public PairedTransactionMatcher(String label, Function<Extractor.Item, Transaction> transaction,
                        Matcher<Transaction>[] properties, String firstLabel, String secondLabel)
        {
            super(label, transaction, properties);
            this.firstLabel = firstLabel;
            this.secondLabel = secondLabel;
        }

        protected boolean checkMetadata(Transaction first, Transaction second, Description description)
        {
            return checkConsistency("date", first.getDateTime(), second.getDateTime(), description) //$NON-NLS-1$
                            && checkConsistency("note", first.getNote(), second.getNote(), description) //$NON-NLS-1$
                            && checkConsistency("source", first.getSource(), second.getSource(), description); //$NON-NLS-1$
        }

        protected boolean checkConsistency(String field, Object firstValue, Object secondValue, Description description)
        {
            if (Objects.equals(firstValue, secondValue))
                return true;

            description.appendText("\n* inconsistent ").appendText(field) //$NON-NLS-1$
                            .appendText(": ").appendText(firstLabel).appendText(" = ").appendValue(firstValue) //$NON-NLS-1$ //$NON-NLS-2$
                            .appendText(", ").appendText(secondLabel).appendText(" = ").appendValue(secondValue); //$NON-NLS-1$ //$NON-NLS-2$
            return false;
        }
    }

    private static class BuySellEntryMatcher extends PairedTransactionMatcher
    {
        public BuySellEntryMatcher(String label, PortfolioTransaction.Type type, Matcher<Transaction>[] properties)
        {
            super(label, item -> item instanceof BuySellEntryItem buysell
                            && buysell.getSubject() instanceof BuySellEntry entry
                            && entry.getPortfolioTransaction().getType() == type ? entry.getPortfolioTransaction()
                                            : null,
                            properties, "portfolio transaction", "account transaction"); //$NON-NLS-1$ //$NON-NLS-2$
        }

        @Override
        protected boolean matchesSafely(Extractor.Item item, Description mismatchDescription)
        {
            if (!super.matchesSafely(item, mismatchDescription))
                return false;

            var entry = (BuySellEntry) item.getSubject();
            var portfolio = entry.getPortfolioTransaction();
            var account = entry.getAccountTransaction();

            return checkConsistency("type", AccountTransaction.Type.valueOf(portfolio.getType().name()), //$NON-NLS-1$
                            account.getType(), mismatchDescription)
                            && checkConsistency("security", portfolio.getSecurity(), account.getSecurity(), //$NON-NLS-1$
                                            mismatchDescription)
                            && checkConsistency("amount", portfolio.getAmount(), account.getAmount(), //$NON-NLS-1$
                                            mismatchDescription)
                            && checkConsistency("currency", portfolio.getCurrencyCode(), account.getCurrencyCode(), //$NON-NLS-1$
                                            mismatchDescription)
                            && checkMetadata(portfolio, account, mismatchDescription);
        }
    }

    /**
     * Matches a cash transfer item. {@code inbound} only selects which side of
     * the transfer the property matchers see (target or source). It does not
     * check the direction of the item: extractors usually create outbound
     * items, and tests assert both sides of the same item.
     */
    private static class AccountTransferMatcher extends PairedTransactionMatcher
    {
        public AccountTransferMatcher(boolean inbound, Matcher<Transaction>[] properties)
        {
            super("cash transfer", item -> item instanceof AccountTransferItem transfer //$NON-NLS-1$
                            && transfer.getSubject() instanceof AccountTransferEntry entry
                                            ? inbound ? entry.getTargetTransaction() : entry.getSourceTransaction()
                                            : null,
                            properties, "source transaction", "target transaction"); //$NON-NLS-1$ //$NON-NLS-2$
        }

        @Override
        protected boolean matchesSafely(Extractor.Item item, Description description)
        {
            if (!super.matchesSafely(item, description))
                return false;

            var entry = (AccountTransferEntry) item.getSubject();
            var source = entry.getSourceTransaction();
            var target = entry.getTargetTransaction();

            if (!checkConsistency("source type", AccountTransaction.Type.TRANSFER_OUT, source.getType(), description) //$NON-NLS-1$
                            || !checkConsistency("target type", AccountTransaction.Type.TRANSFER_IN, //$NON-NLS-1$
                                            target.getType(), description)
                            || !checkMetadata(source, target, description))
                return false;

            if (Objects.equals(source.getCurrencyCode(), target.getCurrencyCode()))
                return checkConsistency("amount", source.getAmount(), target.getAmount(), description); //$NON-NLS-1$

            var forex = source.getUnit(Unit.Type.GROSS_VALUE);
            if (forex.isEmpty())
            {
                description.appendText("\n* missing forex unit on source transaction"); //$NON-NLS-1$
                return false;
            }

            return checkConsistency("forex amount", source.getMonetaryAmount(), forex.get().getAmount(), description) //$NON-NLS-1$
                            && checkConsistency("forex target amount", target.getMonetaryAmount(), //$NON-NLS-1$
                                            forex.get().getForex(), description);
        }
    }

    private static class PortfolioTransferMatcher extends PairedTransactionMatcher
    {
        public PortfolioTransferMatcher(Matcher<Transaction>[] properties)
        {
            super("securityTransfer", item -> item instanceof PortfolioTransferItem transfer //$NON-NLS-1$
                            && transfer.getSubject() instanceof PortfolioTransferEntry entry
                                            ? entry.getSourceTransaction()
                                            : null,
                            properties, "source transaction", "target transaction"); //$NON-NLS-1$ //$NON-NLS-2$
        }

        @Override
        protected boolean matchesSafely(Extractor.Item item, Description description)
        {
            if (!super.matchesSafely(item, description))
                return false;

            var entry = (PortfolioTransferEntry) item.getSubject();
            var source = entry.getSourceTransaction();
            var target = entry.getTargetTransaction();

            return checkConsistency("source type", PortfolioTransaction.Type.TRANSFER_OUT, source.getType(), //$NON-NLS-1$
                            description)
                            && checkConsistency("target type", PortfolioTransaction.Type.TRANSFER_IN, //$NON-NLS-1$
                                            target.getType(), description)
                            && checkMetadata(source, target, description)
                            && checkConsistency("security", source.getSecurity(), target.getSecurity(), description) //$NON-NLS-1$
                            && checkConsistency("shares", source.getShares(), target.getShares(), description) //$NON-NLS-1$
                            && checkConsistency("amount", source.getAmount(), target.getAmount(), description) //$NON-NLS-1$
                            && checkConsistency("currency", source.getCurrencyCode(), target.getCurrencyCode(), //$NON-NLS-1$
                                            description);
        }
    }

    private static class TransactionSecurityMatcher extends TypeSafeDiagnosingMatcher<Transaction>
    {
        private Matcher<Security>[] properties;

        public TransactionSecurityMatcher(Matcher<Security>[] properties)
        {
            this.properties = properties;
        }

        @Override
        protected boolean matchesSafely(Transaction transaction, Description mismatchDescription)
        {
            var security = transaction.getSecurity();
            if (security == null)
            {
                mismatchDescription.appendText("\n* has no 'security'"); //$NON-NLS-1$
                return false;
            }

            for (var property : properties)
            {
                if (!property.matches(security))
                {
                    mismatchDescription.appendText("\n* a security with "); //$NON-NLS-1$
                    property.describeMismatch(security, mismatchDescription);
                    return false;
                }
            }

            return true;
        }

        @Override
        public void describeTo(Description description)
        {
            description.appendText("security with:"); //$NON-NLS-1$
            for (var p : properties)
            {
                description.appendText("\n   - "); //$NON-NLS-1$
                p.describeTo(description);
            }
        }
    }

    private static class FailureMessageItemMatcher extends TypeSafeDiagnosingMatcher<Extractor.Item>
    {
        private String message;
        private Matcher<Extractor.Item> matcher;

        public FailureMessageItemMatcher(String message, Matcher<Extractor.Item> matcher)
        {
            this.message = message;
            this.matcher = matcher;
        }

        @Override
        protected boolean matchesSafely(Extractor.Item item, Description mismatchDescription)
        {
            if (!Objects.equals(message, item.getFailureMessage()))
            {
                if (item.getFailureMessage() == null)
                    mismatchDescription.appendText("\n* item w/o failure message"); //$NON-NLS-1$
                else

                    mismatchDescription.appendText(
                                    MessageFormat.format("\n* with failure message ''{0}''", item.getFailureMessage())); //$NON-NLS-1$
                return false;
            }

            var result = matcher.matches(item);
            if (!result)
            {
                matcher.describeMismatch(item, mismatchDescription);
                return false;
            }

            return true;
        }

        @Override
        public void describeTo(Description description)
        {
            description.appendText(MessageFormat.format("a failure message ''{0}'' and type ", message)); //$NON-NLS-1$
            matcher.describeTo(description);
        }
    }

    public static Matcher<Extractor.Item> withFailureMessage(String message, Matcher<Extractor.Item> matcher)
    {
        return new FailureMessageItemMatcher(message, matcher);
    }

    private static class SkippedItemMatcher extends TypeSafeDiagnosingMatcher<Extractor.Item>
    {
        private String skipReason;
        private Matcher<Extractor.Item> matcher;

        public SkippedItemMatcher(String skipReason, Matcher<Extractor.Item> matcher)
        {
            this.skipReason = skipReason;
            this.matcher = matcher;
        }

        @Override
        protected boolean matchesSafely(Extractor.Item item, Description mismatchDescription)
        {
            if (!item.isSkipped())
            {
                mismatchDescription.appendText("\n* item is not skipped"); //$NON-NLS-1$
                return false;
            }

            var skipped = (SkippedItem) item;
            if (!Objects.equals(skipReason, skipped.getSkipReason()))
            {
                mismatchDescription.appendText(
                                MessageFormat.format("\n* skip reason is ''{0}''", skipped.getSkipReason())); //$NON-NLS-1$
                return false;
            }

            if (matcher != null)
            {
                var original = skipped.getOriginalItem();
                if (!matcher.matches(original))
                {
                    matcher.describeMismatch(original, mismatchDescription);
                    return false;
                }
            }

            return true;
        }

        @Override
        public void describeTo(Description description)
        {
            description.appendText(MessageFormat.format("a skipped item with reason ''{0}''", skipReason)); //$NON-NLS-1$
            if (matcher != null)
            {
                description.appendText(" and "); //$NON-NLS-1$
                matcher.describeTo(description);
            }
        }
    }

    public static Matcher<Extractor.Item> skippedItem(String skipReason)
    {
        return new SkippedItemMatcher(skipReason, null);
    }

    public static Matcher<Extractor.Item> skippedItem(String skipReason, Matcher<Extractor.Item> matcher)
    {
        return new SkippedItemMatcher(skipReason, matcher);
    }

    public static Matcher<Extractor.Item> hasAccount(Account account)
    {
        return new PropertyMatcher<>("primary account", account, Item::getAccountPrimary); //$NON-NLS-1$
    }

    public static Matcher<Extractor.Item> hasSecondaryAccount(Account account)
    {
        return new PropertyMatcher<>("secondary account", account, Item::getAccountSecondary); //$NON-NLS-1$
    }

    public static Matcher<Extractor.Item> hasPortfolio(Portfolio portfolio)
    {
        return new PropertyMatcher<>("primary portfolio", portfolio, Item::getPortfolioPrimary); //$NON-NLS-1$
    }

    public static Matcher<Extractor.Item> hasSecondaryPortfolio(Portfolio portfolio)
    {
        return new PropertyMatcher<>("secondary portfolio", portfolio, Item::getPortfolioSecondary); //$NON-NLS-1$
    }

    @SafeVarargs
    public static Matcher<Extractor.Item> dividend(Matcher<Transaction>... properties)
    {
        return new AccountTransactionMatcher("dividend", AccountTransaction.Type.DIVIDENDS, properties); //$NON-NLS-1$
    }

    @SafeVarargs
    public static Matcher<Extractor.Item> interest(Matcher<Transaction>... properties)
    {
        return new AccountTransactionMatcher("interest", AccountTransaction.Type.INTEREST, properties); //$NON-NLS-1$
    }

    @SafeVarargs
    public static Matcher<Extractor.Item> interestCharge(Matcher<Transaction>... properties)
    {
        return new AccountTransactionMatcher("interest charge", AccountTransaction.Type.INTEREST_CHARGE, properties); //$NON-NLS-1$
    }

    @SafeVarargs
    public static Matcher<Extractor.Item> removal(Matcher<Transaction>... properties)
    {
        return new AccountTransactionMatcher("removal", AccountTransaction.Type.REMOVAL, properties); //$NON-NLS-1$
    }

    @SafeVarargs
    public static Matcher<Extractor.Item> deposit(Matcher<Transaction>... properties)
    {
        return new AccountTransactionMatcher("deposit", AccountTransaction.Type.DEPOSIT, properties); //$NON-NLS-1$
    }

    @SafeVarargs
    public static Matcher<Extractor.Item> taxes(Matcher<Transaction>... properties)
    {
        return new AccountTransactionMatcher("taxes", AccountTransaction.Type.TAXES, properties); //$NON-NLS-1$
    }

    @SafeVarargs
    public static Matcher<Extractor.Item> taxRefund(Matcher<Transaction>... properties)
    {
        return new AccountTransactionMatcher("tax refund", AccountTransaction.Type.TAX_REFUND, properties); //$NON-NLS-1$
    }

    @SafeVarargs
    public static Matcher<Extractor.Item> fee(Matcher<Transaction>... properties)
    {
        return new AccountTransactionMatcher("fee", AccountTransaction.Type.FEES, properties); //$NON-NLS-1$
    }

    @SafeVarargs
    public static Matcher<Extractor.Item> feeRefund(Matcher<Transaction>... properties)
    {
        return new AccountTransactionMatcher("fee refund", AccountTransaction.Type.FEES_REFUND, properties); //$NON-NLS-1$
    }

    @SafeVarargs
    public static Matcher<Extractor.Item> inboundCash(Matcher<Transaction>... properties)
    {
        return new AccountTransferMatcher(true, properties);
    }

    @SafeVarargs
    public static Matcher<Extractor.Item> outboundCash(Matcher<Transaction>... properties)
    {
        return new AccountTransferMatcher(false, properties);
    }

    @SafeVarargs
    public static Matcher<Extractor.Item> purchase(Matcher<Transaction>... properties)
    {
        return new BuySellEntryMatcher("purchase", PortfolioTransaction.Type.BUY, properties); //$NON-NLS-1$
    }

    @SafeVarargs
    public static Matcher<Extractor.Item> sale(Matcher<Transaction>... properties)
    {
        return new BuySellEntryMatcher("sale", PortfolioTransaction.Type.SELL, properties); //$NON-NLS-1$
    }

    @SafeVarargs
    public static Matcher<Extractor.Item> inboundDelivery(Matcher<Transaction>... properties)
    {
        return new ExtractorItemMatcher<>("inbound delivery", //$NON-NLS-1$
                        item -> item instanceof TransactionItem tItem //
                                        && tItem.getSubject() instanceof PortfolioTransaction tx
                                        && tx.getType() == PortfolioTransaction.Type.DELIVERY_INBOUND ? tx : null, //
                        properties);
    }

    @SafeVarargs
    public static Matcher<Extractor.Item> outboundDelivery(Matcher<Transaction>... properties)
    {
        return new ExtractorItemMatcher<>("outbound delivery", //$NON-NLS-1$
                        item -> item instanceof TransactionItem tItem //
                                        && tItem.getSubject() instanceof PortfolioTransaction tx
                                        && tx.getType() == PortfolioTransaction.Type.DELIVERY_OUTBOUND ? tx : null, //
                        properties);
    }

    @SafeVarargs
    public static Matcher<Extractor.Item> securityTransfer(Matcher<Transaction>... properties)
    {
        return new PortfolioTransferMatcher(properties);
    }

    @SafeVarargs
    public static Matcher<Transaction> hasSecurity(Matcher<Security>... properties)
    {
        return new TransactionSecurityMatcher(properties);
    }

    public static Matcher<Transaction> hasDate(String dateString)
    {
        var expectecd = dateString.contains("T") //$NON-NLS-1$
                        ? LocalDateTime.parse(dateString)
                        : LocalDate.parse(dateString).atStartOfDay();

        return new PropertyMatcher<>("date", //$NON-NLS-1$
                        expectecd, //
                        Transaction::getDateTime);
    }

    public static Matcher<Transaction> hasExDate(String dateString)
    {
        var expected = dateString == null ? null
                        : dateString.contains("T") //$NON-NLS-1$
                                        ? LocalDateTime.parse(dateString)
                                        : LocalDate.parse(dateString).atStartOfDay();

        return new PropertyMatcher<>("exDate", //$NON-NLS-1$
                        expected, //
                        t -> ((AccountTransaction) t).getExDate());
    }

    public static Matcher<Transaction> hasShares(double value)
    {
        // work with BigDecimal to have better assertion failed messages
        return new PropertyMatcher<>("shares", //$NON-NLS-1$
                        BigDecimal.valueOf(value).setScale(Values.Share.precision(), Values.MC.getRoundingMode()), //
                        tx -> BigDecimal.valueOf(tx.getShares()).divide(Values.Share.getBigDecimalFactor(), Values.MC)
                                        .setScale(Values.Share.precision(), Values.MC.getRoundingMode()));
    }

    public static Matcher<Transaction> hasSource(String source)
    {
        return new PropertyMatcher<>("source", source, //$NON-NLS-1$
                        Transaction::getSource);
    }

    public static Matcher<Transaction> hasNote(String note)
    {
        return new PropertyMatcher<>("note", note, //$NON-NLS-1$
                        Transaction::getNote);
    }

    public static Matcher<Transaction> hasAmount(String currencyCode, double value)
    {
        return new PropertyMatcher<>("amount", //$NON-NLS-1$
                        Money.of(currencyCode, Values.Amount.factorize(value)), //
                        Transaction::getMonetaryAmount);
    }

    public static Matcher<Transaction> hasGrossValue(String currencyCode, double value)
    {
        return new PropertyMatcher<>("grossValue", //$NON-NLS-1$
                        Money.of(currencyCode, Values.Amount.factorize(value)), //
                        Transaction::getGrossValue);
    }

    public static Matcher<Transaction> hasForexGrossValue(String currencyCode, double value)
    {
        return new PropertyMatcher<>("forexGrossValue", //$NON-NLS-1$
                        Money.of(currencyCode, Values.Amount.factorize(value)), //
                        tx -> {
                            var grossValueUnit = tx.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(AssertionError::new);
                            return grossValueUnit.getForex();
                        });
    }

    public static Matcher<Transaction> hasTaxes(String currencyCode, double value)
    {
        return new PropertyMatcher<>("taxes", //$NON-NLS-1$
                        Money.of(currencyCode, Values.Amount.factorize(value)), //
                        tx -> tx.getUnitSum(Type.TAX));
    }

    public static Matcher<Transaction> hasFees(String currencyCode, double value)
    {
        return new PropertyMatcher<>("fees", //$NON-NLS-1$
                        Money.of(currencyCode, Values.Amount.factorize(value)), //
                        tx -> tx.getUnitSum(Type.FEE));
    }

    /**
     * Run a custom check within a transaction to do custom assertThat for the
     * given transaction
     */
    public static Matcher<Transaction> check(Consumer<Transaction> check)
    {
        return new PropertyMatcher<>("check", //$NON-NLS-1$
                        null, //
                        tx -> {
                            check.accept(tx);
                            return null;
                        });
    }

    @SafeVarargs
    public static Matcher<Extractor.Item> security(Matcher<Security>... properties)
    {
        return new ExtractorItemMatcher<>("security", //$NON-NLS-1$
                        item -> item instanceof SecurityItem securityItem ? securityItem.getSecurity() : null,
                        properties);
    }

    public static Matcher<Security> hasName(String name)
    {
        return new PropertyMatcher<>("Name", name, //$NON-NLS-1$
                        Security::getName);
    }

    public static Matcher<Security> hasIsin(String isin)
    {
        return new PropertyMatcher<>("isin", isin, //$NON-NLS-1$
                        Security::getIsin);
    }

    public static Matcher<Security> hasWkn(String wkn)
    {
        return new PropertyMatcher<>("wkn", wkn, //$NON-NLS-1$
                        Security::getWkn);
    }

    public static Matcher<Security> hasTicker(String ticker)
    {
        return new PropertyMatcher<>("ticker", ticker, //$NON-NLS-1$
                        Security::getTickerSymbol);
    }

    public static Matcher<Security> hasCurrencyCode(String currencyCode)
    {
        return new PropertyMatcher<>("currencyCode", currencyCode, //$NON-NLS-1$
                        Security::getCurrencyCode);
    }

    public static Matcher<Security> hasFeed(String quoteFeed)
    {
        return new PropertyMatcher<>("quoteFeed", quoteFeed, //$NON-NLS-1$
                        Security::getFeed);
    }

    public static Matcher<Security> hasFeedProperty(String name, String value)
    {
        return new PropertyMatcher<>("feedProperty " + name, value, //$NON-NLS-1$
                        s -> s.getPropertyValue(SecurityProperty.Type.FEED, name).orElse(null));
    }

}
