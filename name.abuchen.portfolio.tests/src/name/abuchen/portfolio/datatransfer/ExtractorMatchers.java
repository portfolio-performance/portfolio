package name.abuchen.portfolio.datatransfer;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;

import name.abuchen.portfolio.datatransfer.Extractor.BuySellEntryItem;
import name.abuchen.portfolio.datatransfer.Extractor.TransactionItem;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.model.Transaction.Unit.Type;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

public class ExtractorMatchers
{
    private static class TransactionPropertyMatcher<V> extends TypeSafeDiagnosingMatcher<Transaction>
    {
        private String label;
        private V expectedValue;
        private Function<Transaction, V> value;

        public TransactionPropertyMatcher(String label, V expectedValue, Function<Transaction, V> value)
        {
            this.label = label;
            this.expectedValue = expectedValue;
            this.value = value;
        }

        @Override
        protected boolean matchesSafely(Transaction transaction, Description mismatchDescription)
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

    private static class TransactionMatcher extends TypeSafeDiagnosingMatcher<Extractor.Item>
    {
        private String label;
        private Function<Extractor.Item, Transaction> transaction;
        private Matcher<Transaction>[] properties;

        public TransactionMatcher(String label, Function<Extractor.Item, Transaction> transaction,
                        Matcher<Transaction>[] properties)
        {
            this.label = label;
            this.transaction = transaction;
            this.properties = properties;
        }

        @Override
        protected boolean matchesSafely(Extractor.Item item, Description mismatchDescription)
        {
            Transaction tx = transaction.apply(item);
            if (tx == null)
            {
                mismatchDescription.appendText("\n* not a '").appendText(label).appendText("' item"); //$NON-NLS-1$ //$NON-NLS-2$
                return false;
            }

            for (Matcher<Transaction> property : properties)
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
            for (Matcher<Transaction> p : properties)
            {
                description.appendText("\n - "); //$NON-NLS-1$
                p.describeTo(description);
            }
        }
    }

    private static class AccountTransactionMatcher extends TransactionMatcher
    {
        public AccountTransactionMatcher(String label, AccountTransaction.Type type, Matcher<Transaction>[] properties)
        {
            super(label, item -> item instanceof TransactionItem tx //
                            && tx.getSubject() instanceof AccountTransaction atx && atx.getType() == type ? atx : null,
                            properties);
        }
    }

    @SafeVarargs
    public static Matcher<Extractor.Item> dividend(Matcher<Transaction>... properties)
    {
        return new AccountTransactionMatcher("dividend", AccountTransaction.Type.DIVIDENDS, properties); //$NON-NLS-1$
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
    public static Matcher<Extractor.Item> purchase(Matcher<Transaction>... properties)
    {
        return new TransactionMatcher("purchase", //$NON-NLS-1$
                        item -> item instanceof BuySellEntryItem buysell //
                                        && buysell.getSubject() instanceof BuySellEntry entry
                                        && entry.getPortfolioTransaction().getType() == PortfolioTransaction.Type.BUY
                                                        ? entry.getPortfolioTransaction()
                                                        : null, //
                        properties);
    }

    @SafeVarargs
    public static Matcher<Extractor.Item> sale(Matcher<Transaction>... properties)
    {
        return new TransactionMatcher("sale", //$NON-NLS-1$
                        item -> item instanceof BuySellEntryItem buysell //
                                        && buysell.getSubject() instanceof BuySellEntry entry
                                        && entry.getPortfolioTransaction().getType() == PortfolioTransaction.Type.SELL
                                                        ? entry.getPortfolioTransaction()
                                                        : null, //
                        properties);
    }

    public static Matcher<Transaction> hasDate(String dateString)
    {
        LocalDateTime expectecd = dateString.contains("T") //$NON-NLS-1$
                        ? LocalDateTime.parse(dateString)
                        : LocalDate.parse(dateString).atStartOfDay();

        return new TransactionPropertyMatcher<>("date", //$NON-NLS-1$
                        expectecd, //
                        Transaction::getDateTime);
    }

    public static Matcher<Transaction> hasShares(double value)
    {
        // work with BigDecimal to have better assertion failed messages
        return new TransactionPropertyMatcher<BigDecimal>("shares", //$NON-NLS-1$
                        BigDecimal.valueOf(value).setScale(Values.Share.precision(), Values.MC.getRoundingMode()), //
                        tx -> BigDecimal.valueOf(tx.getShares()).divide(Values.Share.getBigDecimalFactor(), Values.MC)
                                        .setScale(Values.Share.precision(), Values.MC.getRoundingMode()));
    }

    public static Matcher<Transaction> hasSource(String source)
    {
        return new TransactionPropertyMatcher<>("source", source, //$NON-NLS-1$
                        Transaction::getSource);
    }

    public static Matcher<Transaction> hasNote(String note)
    {
        return new TransactionPropertyMatcher<>("note", note, //$NON-NLS-1$
                        Transaction::getNote);
    }

    public static Matcher<Transaction> hasAmount(String currencyCode, double value)
    {
        return new TransactionPropertyMatcher<>("amount", //$NON-NLS-1$
                        Money.of(currencyCode, Values.Amount.factorize(value)), //
                        Transaction::getMonetaryAmount);
    }

    public static Matcher<Transaction> hasGrossValue(String currencyCode, double value)
    {
        return new TransactionPropertyMatcher<>("grossValue", //$NON-NLS-1$
                        Money.of(currencyCode, Values.Amount.factorize(value)), //
                        Transaction::getGrossValue);
    }

    public static Matcher<Transaction> hasForexGrossValue(String currencyCode, double value)
    {
        return new TransactionPropertyMatcher<>("forexGrossValue", //$NON-NLS-1$
                        Money.of(currencyCode, Values.Amount.factorize(value)), //
                        tx -> {
                            Unit grossValueUnit = tx.getUnit(Unit.Type.GROSS_VALUE).orElseThrow(AssertionError::new);
                            return grossValueUnit.getForex();
                        });
    }

    public static Matcher<Transaction> hasTaxes(String currencyCode, double value)
    {
        return new TransactionPropertyMatcher<>("taxes", //$NON-NLS-1$
                        Money.of(currencyCode, Values.Amount.factorize(value)), //
                        tx -> tx.getUnitSum(Type.TAX));
    }

    public static Matcher<Transaction> hasFees(String currencyCode, double value)
    {
        return new TransactionPropertyMatcher<>("fees", //$NON-NLS-1$
                        Money.of(currencyCode, Values.Amount.factorize(value)), //
                        tx -> tx.getUnitSum(Type.FEE));
    }

    /**
     * Run a custom check within a transaction to do custom assertThat for the
     * given transaction
     */
    public static Matcher<Transaction> check(Consumer<Transaction> check)
    {
        return new TransactionPropertyMatcher<>("check", //$NON-NLS-1$
                        null, //
                        tx -> {
                            check.accept(tx);
                            return null;
                        });
    }

}
