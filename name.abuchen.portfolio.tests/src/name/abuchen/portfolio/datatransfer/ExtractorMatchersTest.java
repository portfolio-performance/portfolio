package name.abuchen.portfolio.datatransfer;

import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.deposit;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.dividend;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasAmount;
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.hasCurrencyCode;
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
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.security;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;

import name.abuchen.portfolio.datatransfer.Extractor.SecurityItem;
import name.abuchen.portfolio.datatransfer.Extractor.TransactionItem;
import name.abuchen.portfolio.model.AccountTransaction;
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

    @BeforeClass
    public static void setup()
    {
        Security s = new Security();
        s.setName("test name");
        s.setIsin("DE01");
        s.setWkn("WKN");
        s.setTickerSymbol("TICKER");
        s.setCurrencyCode("USD");

        someSecurity = new SecurityItem(s);

        AccountTransaction tx = new AccountTransaction();
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
        tx.setSecurity(s);

        someTransactionItem = new TransactionItem(tx);
    }

    @Test(expected = AssertionError.class)
    public void testEmptyList()
    {
        List<Extractor.Item> items = Collections.emptyList();
        assertThat(items, hasItem(dividend(hasAmount("EUR", 0))));
    }

    @Test(expected = AssertionError.class)
    public void testList()
    {
        List<Extractor.Item> items = new ArrayList<Extractor.Item>();

        items.add(new Extractor.TransactionItem(new AccountTransaction()));

        assertThat(items, hasItem(dividend(hasAmount("EUR", 0))));
    }

    @Test(expected = AssertionError.class)
    public void testDividend()
    {
        List<Extractor.Item> items = new ArrayList<Extractor.Item>();

        items.add(someTransactionItem);

        AccountTransaction tx = new AccountTransaction();
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

}
