package name.abuchen.portfolio.datatransfer;

import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.deposit;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.dividend;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasAmount;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasCurrencyCode;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasExDate;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasFees;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasForexGrossValue;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasGrossValue;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasIsin;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasName;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasNote;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasShares;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasTaxes;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasTicker;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasWkn;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.inboundCash;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.outboundCash;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.purchase;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.sale;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.security;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.securityTransfer;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.skippedItem;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import org.hamcrest.Matcher;
import org.hamcrest.StringDescription;
import org.junit.BeforeClass;
import org.junit.Test;

import name.abuchen.portfolio.datatransfer.Extractor.AccountTransferItem;
import name.abuchen.portfolio.datatransfer.Extractor.BuySellEntryItem;
import name.abuchen.portfolio.datatransfer.Extractor.PortfolioTransferItem;
import name.abuchen.portfolio.datatransfer.Extractor.SecurityItem;
import name.abuchen.portfolio.datatransfer.Extractor.SkippedItem;
import name.abuchen.portfolio.datatransfer.Extractor.TransactionItem;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.AccountTransferEntry;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.PortfolioTransferEntry;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class ExtractorMatchersTest
{
    private static SecurityItem someSecurity;
    private static TransactionItem someTransactionItem;
    private static SkippedItem someSkippedItem;

    @BeforeClass
    public static void setup()
    {
        var s = new Security();
        s.setName("test name");
        s.setIsin("DE01");
        s.setWkn("WKN");
        s.setTickerSymbol("TICKER");
        s.setCurrencyCode("USD");

        someSecurity = new SecurityItem(s);

        var tx = new AccountTransaction();
        tx.setType(AccountTransaction.Type.DIVIDENDS);
        tx.setCurrencyCode(CurrencyUnit.EUR);
        tx.setAmount(Values.Amount.factorize(100));
        tx.addUnit(new Unit(Unit.Type.FEE, Money.of(CurrencyUnit.EUR, Values.Amount.factorize(10))));
        tx.addUnit(new Unit(Unit.Type.TAX, Money.of(CurrencyUnit.EUR, Values.Amount.factorize(20))));
        tx.addUnit(new Unit(Unit.Type.GROSS_VALUE, Money.of(CurrencyUnit.EUR, Values.Amount.factorize(130)),
                        Money.of(CurrencyUnit.USD, Values.Amount.factorize(260)), BigDecimal.valueOf(0.5)));
        tx.setNote("test");
        tx.setShares(Values.Share.factorize(123));
        tx.setDateTime(LocalDateTime.parse("2023-04-30T12:45"));
        tx.setExDate(LocalDateTime.parse("2023-04-28T00:00"));
        tx.setSecurity(s);

        someTransactionItem = new TransactionItem(tx);

        someSkippedItem = new SkippedItem(someTransactionItem, "skip reason");
    }

    @Test(expected = AssertionError.class)
    public void testEmptyList()
    {
        List<Extractor.Item> items = Collections.emptyList();
        assertThat(items, hasItem(dividend(hasAmount("EUR", 0))));
    }

    @Test
    public void testBuySellTypes()
    {
        for (var type : List.of(PortfolioTransaction.Type.BUY, PortfolioTransaction.Type.SELL))
        {
            var entry = new BuySellEntry(type);
            var item = new BuySellEntryItem(entry);
            assertThat(item, buySell(type));

            // a deviating portfolio type selects a different kind of item
            entry.getPortfolioTransaction().setType(opposite(type));
            var label = type == PortfolioTransaction.Type.BUY ? "purchase" : "sale";
            assertMismatch(item, buySell(type), "not a '" + label + "' item");
        }
    }

    @Test
    public void testBuySellConsistency()
    {
        for (var type : List.of(PortfolioTransaction.Type.BUY, PortfolioTransaction.Type.SELL))
        {
            var oppositeAccountType = AccountTransaction.Type.valueOf(opposite(type).name());

            assertInconsistentBuySell(type, "type", e -> e.getAccountTransaction().setType(oppositeAccountType));
            assertInconsistentBuySell(type, "date",
                            e -> e.getAccountTransaction().setDateTime(LocalDateTime.parse("2024-01-01T12:00")));
            assertInconsistentBuySell(type, "security", e -> e.getAccountTransaction().setSecurity(new Security()));
            assertInconsistentBuySell(type, "amount", e -> e.getAccountTransaction().setAmount(123));
            assertInconsistentBuySell(type, "currency", e -> e.getAccountTransaction().setCurrencyCode("USD"));
            assertInconsistentBuySell(type, "note", e -> e.getAccountTransaction().setNote("different note"));
            assertInconsistentBuySell(type, "source", e -> e.getAccountTransaction().setSource("different source"));

            // the check must not depend on which side deviates
            assertInconsistentBuySell(type, "amount", e -> e.getPortfolioTransaction().setAmount(123));
            assertInconsistentBuySell(type, "note", e -> e.getPortfolioTransaction().setNote("different note"));
        }
    }

    private static PortfolioTransaction.Type opposite(PortfolioTransaction.Type type)
    {
        return type == PortfolioTransaction.Type.BUY ? PortfolioTransaction.Type.SELL : PortfolioTransaction.Type.BUY;
    }

    private static Matcher<Extractor.Item> buySell(PortfolioTransaction.Type type)
    {
        return type == PortfolioTransaction.Type.BUY ? purchase() : sale();
    }

    private void assertInconsistentBuySell(PortfolioTransaction.Type type, String field, Consumer<BuySellEntry> change)
    {
        var entry = new BuySellEntry(type);
        entry.setDate(LocalDateTime.parse("2023-04-30T12:45"));
        entry.setSecurity(new Security());
        entry.setAmount(1000);
        entry.setCurrencyCode("EUR");
        entry.setNote("note");
        entry.setSource("source");

        var description = assertInconsistentAfter(new BuySellEntryItem(entry), buySell(type), field,
                        () -> change.accept(entry));
        assertThat(description, containsString("portfolio transaction ="));
        assertThat(description, containsString("account transaction ="));
    }

    @Test
    public void testBuySellIgnoresPortfolioOnlySharesAndUnits()
    {
        for (var type : List.of(PortfolioTransaction.Type.BUY, PortfolioTransaction.Type.SELL))
        {
            var entry = new BuySellEntry(type);
            entry.setCurrencyCode("EUR");
            entry.setAmount(10000);
            entry.setShares(Values.Share.factorize(5));
            entry.getPortfolioTransaction().addUnit(new Unit(Unit.Type.TAX, Money.of("EUR", 100)));
            entry.getPortfolioTransaction().addUnit(new Unit(Unit.Type.FEE, Money.of("EUR", 200)));
            entry.getPortfolioTransaction().addUnit(new Unit(Unit.Type.GROSS_VALUE, Money.of("EUR", 9700),
                            Money.of("USD", 19400), BigDecimal.valueOf(0.5)));

            assertThat(List.of(new BuySellEntryItem(entry)), hasItem(buySell(type)));
        }
    }

    @Test
    public void testAccountTransferConsistency()
    {
        for (var inbound : List.of(true, false))
        {
            assertInconsistentAccountTransfer(inbound, "source type",
                            entry -> entry.getSourceTransaction().setType(AccountTransaction.Type.TRANSFER_IN));
            assertInconsistentAccountTransfer(inbound, "target type",
                            entry -> entry.getTargetTransaction().setType(AccountTransaction.Type.TRANSFER_OUT));
            assertInconsistentAccountTransfer(inbound, "date",
                            entry -> entry.getTargetTransaction().setDateTime(LocalDateTime.parse("2024-01-01T12:00")));
            assertInconsistentAccountTransfer(inbound, "note", entry -> entry.getTargetTransaction().setNote("other"));
            assertInconsistentAccountTransfer(inbound, "source",
                            entry -> entry.getTargetTransaction().setSource("other"));
            assertInconsistentAccountTransfer(inbound, "amount", entry -> entry.getTargetTransaction().setAmount(2000));

            var entry = newAccountTransfer();
            entry.getTargetTransaction().setCurrencyCode("USD");
            assertMismatch(new AccountTransferItem(entry, !inbound), cashTransfer(inbound), "missing forex unit");
        }
    }

    private static Matcher<Extractor.Item> cashTransfer(boolean inbound)
    {
        return inbound ? inboundCash() : outboundCash();
    }

    private AccountTransferEntry newAccountTransfer()
    {
        var entry = new AccountTransferEntry();
        entry.setDate(LocalDateTime.parse("2023-04-30T12:45"));
        entry.setCurrencyCode("EUR");
        entry.setAmount(1000);
        entry.setNote("note");
        entry.setSource("source");
        return entry;
    }

    private void assertInconsistentAccountTransfer(boolean inbound, String field, Consumer<AccountTransferEntry> change)
    {
        var entry = newAccountTransfer();
        assertInconsistentAfter(new AccountTransferItem(entry, !inbound), cashTransfer(inbound), field,
                        () -> change.accept(entry));
    }

    @Test
    public void testAccountTransferForex()
    {
        for (var inbound : List.of(true, false))
        {
            var entry = newAccountTransfer();
            entry.getTargetTransaction().setMonetaryAmount(Money.of("USD", 2000));
            entry.getSourceTransaction().addUnit(new Unit(Unit.Type.GROSS_VALUE, Money.of("EUR", 1000),
                            Money.of("USD", 2000), BigDecimal.valueOf(0.5)));
            var item = new AccountTransferItem(entry, !inbound);

            assertThat(item, cashTransfer(inbound));
            assertThat(item, inbound ? inboundCash(hasAmount("USD", 20)) : outboundCash(hasAmount("EUR", 10)));

            assertInconsistentAfter(item, cashTransfer(inbound), "forex amount",
                            () -> entry.getSourceTransaction().setAmount(1100));
            entry.getSourceTransaction().setAmount(1000);

            assertInconsistentAfter(item, cashTransfer(inbound), "forex target amount",
                            () -> entry.getTargetTransaction().setAmount(2100));
            entry.getTargetTransaction().setAmount(2000);

            assertInconsistentAfter(item, cashTransfer(inbound), "forex target amount",
                            () -> entry.getTargetTransaction().setCurrencyCode("GBP"));
        }
    }

    @Test
    public void testPortfolioTransferConsistency()
    {
        assertInconsistentPortfolioTransfer("source type",
                        entry -> entry.getSourceTransaction().setType(PortfolioTransaction.Type.TRANSFER_IN));
        assertInconsistentPortfolioTransfer("target type",
                        entry -> entry.getTargetTransaction().setType(PortfolioTransaction.Type.TRANSFER_OUT));
        assertInconsistentPortfolioTransfer("date",
                        entry -> entry.getTargetTransaction().setDateTime(LocalDateTime.parse("2024-01-01T12:00")));
        assertInconsistentPortfolioTransfer("security",
                        entry -> entry.getTargetTransaction().setSecurity(new Security()));
        assertInconsistentPortfolioTransfer("shares", entry -> entry.getTargetTransaction().setShares(200));
        assertInconsistentPortfolioTransfer("amount", entry -> entry.getTargetTransaction().setAmount(2000));
        assertInconsistentPortfolioTransfer("currency", entry -> entry.getTargetTransaction().setCurrencyCode("USD"));
        assertInconsistentPortfolioTransfer("note", entry -> entry.getTargetTransaction().setNote("other"));
        assertInconsistentPortfolioTransfer("source", entry -> entry.getTargetTransaction().setSource("other"));
    }

    private void assertInconsistentPortfolioTransfer(String field, Consumer<PortfolioTransferEntry> change)
    {
        var entry = new PortfolioTransferEntry();
        entry.setDate(LocalDateTime.parse("2023-04-30T12:45"));
        entry.setSecurity(new Security());
        entry.setShares(Values.Share.factorize(5));
        entry.setCurrencyCode("EUR");
        entry.setAmount(1000);
        entry.setNote("note");
        entry.setSource("source");
        assertInconsistentAfter(new PortfolioTransferItem(entry), securityTransfer(hasShares(5), hasAmount("EUR", 10)),
                        field, () -> change.accept(entry));
    }

    /**
     * Asserts that the item matches, applies the change, and asserts that the
     * item then fails with an inconsistency on exactly the given field. The
     * trailing colon keeps "source" from matching "source type".
     */
    private String assertInconsistentAfter(Extractor.Item item, Matcher<Extractor.Item> matcher, String field,
                    Runnable change)
    {
        assertThat(item, matcher);
        change.run();
        return assertMismatch(item, matcher, "inconsistent " + field + ":");
    }

    private String assertMismatch(Extractor.Item item, Matcher<Extractor.Item> matcher, String expected)
    {
        assertThat(matcher.matches(item), is(false));
        var description = new StringDescription();
        matcher.describeMismatch(item, description);
        assertThat(description.toString(), containsString(expected));
        return description.toString();
    }

    @Test(expected = AssertionError.class)
    public void testList()
    {
        List<Extractor.Item> items = new ArrayList<>();

        items.add(new Extractor.TransactionItem(new AccountTransaction()));

        assertThat(items, hasItem(dividend(hasAmount("EUR", 0))));
    }

    @Test(expected = AssertionError.class)
    public void testDividend()
    {
        List<Extractor.Item> items = new ArrayList<>();

        items.add(someTransactionItem);

        var tx = new AccountTransaction();
        tx.setType(AccountTransaction.Type.DIVIDENDS);
        tx.setCurrencyCode(CurrencyUnit.EUR);
        tx.setAmount(1000);
        items.add(new Extractor.TransactionItem(tx));

        assertThat(items, hasItem(dividend(hasAmount("EUR", 20))));
    }

    @Test(expected = AssertionError.class)
    public void testTypeFailure()
    {
        assertThat(List.of(someTransactionItem), hasItem(deposit()));
    }

    @Test
    public void testTypeSuccess()
    {
        assertThat(List.of(someTransactionItem), hasItem(dividend()));
    }

    @Test(expected = AssertionError.class)
    public void testAmountFailure()
    {
        assertThat(List.of(someTransactionItem), hasItem(dividend(hasAmount("EUR", 200))));
    }

    @Test
    public void testAmountSuccess()
    {
        assertThat(List.of(someTransactionItem), hasItem(dividend(hasAmount("EUR", 100))));
    }

    @Test(expected = AssertionError.class)
    public void testNoteFailure()
    {
        assertThat(List.of(someTransactionItem), hasItem(dividend(hasNote("foo"))));
    }

    @Test
    public void testNoteSuccess()
    {
        assertThat(List.of(someTransactionItem), hasItem(dividend(hasNote("test"))));
    }

    @Test(expected = AssertionError.class)
    public void testFeesFailure()
    {
        assertThat(List.of(someTransactionItem), hasItem(dividend(hasFees("EUR", 0))));
    }

    @Test
    public void testFeesSuccess()
    {
        assertThat(List.of(someTransactionItem), hasItem(dividend(hasFees("EUR", 10))));
    }

    @Test(expected = AssertionError.class)
    public void testTaxesFailure()
    {
        assertThat(List.of(someTransactionItem), hasItem(dividend(hasTaxes("EUR", 0))));
    }

    @Test
    public void testTaxesSuccess()
    {
        assertThat(List.of(someTransactionItem), hasItem(dividend(hasTaxes("EUR", 20))));
    }

    @Test(expected = AssertionError.class)
    public void testSharesFailure()
    {
        assertThat(List.of(someTransactionItem), hasItem(dividend(hasShares(0))));
    }

    @Test
    public void testSharesSuccess()
    {
        assertThat(List.of(someTransactionItem), hasItem(dividend(hasShares(123))));
    }

    @Test(expected = AssertionError.class)
    public void testGrossValueFailure()
    {
        assertThat(List.of(someTransactionItem), hasItem(dividend(hasGrossValue("EUR", 0))));
    }

    @Test
    public void testGrossValueSuccess()
    {
        assertThat(List.of(someTransactionItem), hasItem(dividend(hasGrossValue("EUR", 130))));
    }

    @Test(expected = AssertionError.class)
    public void testForexGrossValueFailure()
    {
        assertThat(List.of(someTransactionItem), hasItem(dividend(hasForexGrossValue("EUR", 0))));
    }

    @Test
    public void testForexGrossValueSuccess()
    {
        assertThat(List.of(someTransactionItem), hasItem(dividend(hasForexGrossValue("USD", 260))));
    }

    @Test(expected = AssertionError.class)
    public void testNameFailure()
    {
        assertThat(List.of(someSecurity), hasItem(security(hasName("test"))));
    }

    @Test
    public void testNameSuccess()
    {
        assertThat(List.of(someSecurity), hasItem(security(hasName("test name"))));
    }

    @Test(expected = AssertionError.class)
    public void testIsinFailure()
    {
        assertThat(List.of(someSecurity), hasItem(security(hasIsin("test"))));
    }

    @Test
    public void testIsinSuccess()
    {
        assertThat(List.of(someSecurity), hasItem(security(hasIsin("DE01"))));
    }

    @Test(expected = AssertionError.class)
    public void testWknFailure()
    {
        assertThat(List.of(someSecurity), hasItem(security(hasWkn("test"))));
    }

    @Test
    public void testWknSuccess()
    {
        assertThat(List.of(someSecurity), hasItem(security(hasWkn("WKN"))));
    }

    @Test(expected = AssertionError.class)
    public void testTickerFailure()
    {
        assertThat(List.of(someSecurity), hasItem(security(hasTicker("test"))));
    }

    @Test
    public void testTickerSuccess()
    {
        assertThat(List.of(someSecurity), hasItem(security(hasTicker("TICKER"))));
    }

    @Test(expected = AssertionError.class)
    public void testCurrencyCodeFailure()
    {
        assertThat(List.of(someSecurity), hasItem(security(hasCurrencyCode("CHF"))));
    }

    @Test
    public void testCurrencyCodeSuccess()
    {
        assertThat(List.of(someSecurity), hasItem(security(hasCurrencyCode("USD"))));
    }

    @Test(expected = AssertionError.class)
    public void testExDateFailure()
    {
        assertThat(List.of(someTransactionItem), hasItem(dividend(hasExDate("2023-01-01"))));
    }

    @Test
    public void testExDateSuccess()
    {
        assertThat(List.of(someTransactionItem), hasItem(dividend(hasExDate("2023-04-28"))));
    }

    @Test(expected = AssertionError.class)
    public void testExDateNullFailure()
    {
        assertThat(List.of(someTransactionItem), hasItem(dividend(hasExDate(null))));
    }

    @Test
    public void testExDateNullSuccess()
    {
        var tx = new AccountTransaction();
        tx.setType(AccountTransaction.Type.DIVIDENDS);
        tx.setCurrencyCode(CurrencyUnit.EUR);
        tx.setAmount(Values.Amount.factorize(100));
        tx.setDateTime(LocalDateTime.parse("2023-04-30T12:45"));

        assertThat(List.of(new TransactionItem(tx)), hasItem(dividend(hasExDate(null))));
    }

    @Test(expected = AssertionError.class)
    public void testSkippedItemNotSkipped()
    {
        assertThat(List.of(someTransactionItem), hasItem(skippedItem("skip reason")));
    }

    @Test(expected = AssertionError.class)
    public void testSkippedItemWrongReasonFailure()
    {
        assertThat(List.of(someSkippedItem), hasItem(skippedItem("wrong reason")));
    }

    @Test
    public void testSkippedItemReasonSuccess()
    {
        assertThat(List.of(someSkippedItem), hasItem(skippedItem("skip reason")));
    }

    @Test(expected = AssertionError.class)
    public void testSkippedItemWithInnerMatcherFailure()
    {
        assertThat(List.of(someSkippedItem), hasItem(skippedItem("skip reason", dividend(hasAmount("EUR", 999)))));
    }

    @Test
    public void testSkippedItemWithInnerMatcherSuccess()
    {
        assertThat(List.of(someSkippedItem), hasItem(skippedItem("skip reason", dividend(hasAmount("EUR", 100)))));
    }

}
