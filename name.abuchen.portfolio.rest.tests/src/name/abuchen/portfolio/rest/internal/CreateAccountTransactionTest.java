package name.abuchen.portfolio.rest.internal;

import static name.abuchen.portfolio.rest.internal.TransactionFixture.assertError;
import static name.abuchen.portfolio.rest.internal.TransactionFixture.eur;
import static name.abuchen.portfolio.rest.internal.TransactionFixture.shares;
import static name.abuchen.portfolio.rest.internal.TransactionFixture.usd;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Transaction;

@SuppressWarnings("nls")
public class CreateAccountTransactionTest
{
    private TransactionFixture f;

    @Before
    public void setUp()
    {
        f = new TransactionFixture();
    }

    @After
    public void tearDown() throws Exception
    {
        f.dispose();
    }

    private AccountTransaction created(String body)
    {
        var json = f.create(body);
        var pair = f.find(json.get("uuid").getAsString());
        assertThat(json.has("linked"), is(false));
        return (AccountTransaction) pair.getTransaction();
    }

    private String dividend(String extra)
    {
        return "{'type':'dividends','date':'2026-07-20','cashAccount':'" + f.cash.getUUID() + "','instrument':'"
                        + f.eurSecurity.getUUID() + "'" + extra + "}";
    }

    @Test
    public void testDividendWithTaxesAndFees()
    {
        var tx = created(dividend(",'exDate':'2026-07-15','shares':'10','grossValue':'50','taxes':'7.50','fees':'1'"));

        assertThat(f.cash.getTransactions().contains(tx), is(true));
        assertThat(tx.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(tx.getSecurity(), is(f.eurSecurity));
        assertThat(tx.getShares(), is(shares("10")));
        assertThat(tx.getExDate(), is(LocalDateTime.of(2026, 7, 15, 0, 0)));
        assertThat(tx.getMonetaryAmount(), is(eur("41.50")));
        assertThat(tx.getGrossValue(), is(eur("50")));
        assertThat(tx.getUnitSum(Transaction.Unit.Type.TAX), is(eur("7.50")));
        assertThat(tx.getUnitSum(Transaction.Unit.Type.FEE), is(eur("1")));
        assertThat(tx.getUnit(Transaction.Unit.Type.GROSS_VALUE).isPresent(), is(false));
        assertThat(f.file.isDirty(), is(true));
    }

    @Test
    public void testDividendRequiresInstrumentButAllowsZeroShares()
    {
        var e = f.createFails(dividend(",'grossValue':'50'").replace(",'instrument':'" + f.eurSecurity.getUUID() + "'",
                        ""));
        assertError(e, "instrument", "required");

        var tx = created(dividend(",'grossValue':'50'"));
        assertThat(tx.getShares(), is(0L));
        assertThat(tx.getMonetaryAmount(), is(eur("50")));

        tx = created(dividend(",'shares':'0','amount':'40','taxes':'10'"));
        assertThat(tx.getGrossValue(), is(eur("50")));
    }

    @Test
    public void testDividendExDateRules()
    {
        var e = f.createFails(dividend(",'grossValue':'50','exDate':'2026-07-21'"));
        assertError(e, "exDate", "ex-date-after-date");

        var tx = created(dividend(",'grossValue':'50','exDate':'2026-07-20'"));
        assertThat(tx.getExDate(), is(LocalDateTime.of(2026, 7, 20, 0, 0)));

        // an ex-date is only for dividends
        e = f.createFails("{'type':'deposit','date':'2026-07-20','cashAccount':'" + f.cash.getUUID()
                        + "','amount':'5','exDate':'2026-07-01'}");
        assertError(e, "exDate", "not-allowed-for-type");
    }

    @Test
    public void testDividendAmountMustMatch()
    {
        var e = f.createFails(dividend(",'grossValue':'50','taxes':'10','amount':'41'"));
        assertError(e, "amount", "total-mismatch");
    }

    @Test
    public void testForeignCurrencyDividend()
    {
        var tx = created(dividend(",'grossValue':'45','taxes':'5','forexTaxes':'1','exchangeRate':'0.9'")
                        .replace(f.eurSecurity.getUUID(), f.usdSecurity.getUUID()));

        assertThat(tx.getCurrencyCode(), is("EUR"));
        var gross = tx.getUnit(Transaction.Unit.Type.GROSS_VALUE).orElseThrow();
        assertThat(gross.getAmount(), is(eur("45")));
        assertThat(gross.getForex(), is(usd("50")));
        assertThat(gross.getExchangeRate(), comparesEqualTo(new BigDecimal("0.9")));

        var forexTax = tx.getUnits().filter(u -> u.getType() == Transaction.Unit.Type.TAX && u.getForex() != null)
                        .findFirst().orElseThrow();
        assertThat(forexTax.getForex(), is(usd("1")));
        assertThat(forexTax.getAmount(), is(eur("0.90")));

        // 45 - 5 - 0.90
        assertThat(tx.getMonetaryAmount(), is(eur("39.10")));
    }

    @Test
    public void testForeignCurrencyDividendLooksUpTheRate()
    {
        var tx = created(dividend(",'grossValue':'90'").replace(f.eurSecurity.getUUID(), f.usdSecurity.getUUID()));
        var gross = tx.getUnit(Transaction.Unit.Type.GROSS_VALUE).orElseThrow();
        assertThat(gross.getExchangeRate(), comparesEqualTo(TransactionFixture.USD_EUR));
        assertThat(gross.getForex(), is(usd("100")));
    }

    @Test
    public void testInterestWithTaxes()
    {
        var tx = created("{'type':'interest','date':'2026-01-31','cashAccount':'" + f.cash.getUUID()
                        + "','amount':'75','taxes':'25','note':'January'}");

        assertThat(tx.getType(), is(AccountTransaction.Type.INTEREST));
        assertThat(tx.getMonetaryAmount(), is(eur("75")));
        assertThat(tx.getUnitSum(Transaction.Unit.Type.TAX), is(eur("25")));
        assertThat(tx.getGrossValue(), is(eur("100")));
        assertThat(tx.getNote(), is("January"));
        assertThat(tx.getSecurity(), nullValue());
    }

    @Test
    public void testFeesWithOptionalInstrument()
    {
        var tx = created("{'type':'fees','date':'2026-01-31','cashAccount':'" + f.cash.getUUID() + "','amount':'3'}");
        assertThat(tx.getType(), is(AccountTransaction.Type.FEES));
        assertThat(tx.getSecurity(), nullValue());

        tx = created("{'type':'fees','date':'2026-01-31','cashAccount':'" + f.cash.getUUID() + "','instrument':'"
                        + f.eurSecurity.getUUID() + "','amount':'3'}");
        assertThat(tx.getSecurity(), is(f.eurSecurity));
        assertThat(tx.getUnits().count(), is(0L));

        // an instrument in another currency: gross value unit, as the dialog writes it
        tx = created("{'type':'tax-refund','date':'2026-01-31','cashAccount':'" + f.cash.getUUID()
                        + "','instrument':'" + f.usdSecurity.getUUID() + "','amount':'9'}");
        assertThat(tx.getType(), is(AccountTransaction.Type.TAX_REFUND));
        var gross = tx.getUnit(Transaction.Unit.Type.GROSS_VALUE).orElseThrow();
        assertThat(gross.getAmount(), is(eur("9")));
        assertThat(gross.getForex(), is(usd("10")));
    }

    @Test
    public void testDepositRejectsInstrumentAndShares()
    {
        var e = f.createFails("{'type':'deposit','date':'2026-01-02','cashAccount':'" + f.cash.getUUID()
                        + "','amount':'100','instrument':'" + f.eurSecurity.getUUID() + "','shares':'1','taxes':'1'}");
        assertError(e, "instrument", "not-allowed-for-type");
        assertError(e, "shares", "not-allowed-for-type");
        assertError(e, "taxes", "not-allowed-for-type");
    }

    @Test
    public void testEveryCashType()
    {
        for (var type : new String[] { "deposit", "removal", "interest", "interest-charge", "fees", "fees-refund",
                        "taxes", "tax-refund" })
        {
            var tx = created("{'type':'" + type + "','date':'2026-01-02','cashAccount':'" + f.cash.getUUID()
                            + "','amount':'100'}");
            assertThat(TransactionTypes.wireType(tx), is(type));
            assertThat(tx.getMonetaryAmount(), is(eur("100")));
        }
        assertThat(f.cash.getTransactions().size(), is(8));
    }

    @Test
    public void testAmountRules()
    {
        var e = f.createFails("{'type':'deposit','date':'2026-01-02','cashAccount':'" + f.cash.getUUID() + "'}");
        assertError(e, "amount", "required");

        e = f.createFails("{'type':'deposit','date':'2026-01-02','cashAccount':'" + f.cash.getUUID()
                        + "','amount':'0'}");
        assertError(e, "amount", "must-be-positive");

        e = f.createFails("{'type':'removal','date':'2026-01-02','cashAccount':'" + f.cash.getUUID()
                        + "','amount':'-5'}");
        assertError(e, "amount", "must-be-positive");

        e = f.createFails("{'type':'deposit','date':'2026-01-02','cashAccount':'" + f.cash.getUUID()
                        + "','amount':'1.234'}");
        assertError(e, "amount", "invalid-value");

        assertThat(f.legCount(), is(0));
    }
}
