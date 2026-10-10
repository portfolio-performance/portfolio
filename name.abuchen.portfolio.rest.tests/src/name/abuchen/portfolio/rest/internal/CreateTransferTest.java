package name.abuchen.portfolio.rest.internal;

import static name.abuchen.portfolio.rest.internal.TransactionFixture.assertError;
import static name.abuchen.portfolio.rest.internal.TransactionFixture.eur;
import static name.abuchen.portfolio.rest.internal.TransactionFixture.shares;
import static name.abuchen.portfolio.rest.internal.TransactionFixture.usd;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.is;

import java.math.BigDecimal;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.AccountTransferEntry;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.PortfolioTransferEntry;
import name.abuchen.portfolio.model.Transaction;

@SuppressWarnings("nls")
public class CreateTransferTest
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

    private String cashTransfer(String from, String to, String extra)
    {
        return "{'type':'cash-transfer','date':'2026-05-01','fromCashAccount':'" + from + "','toCashAccount':'" + to
                        + "','amount':'500'" + extra + "}";
    }

    @Test
    public void testSameCurrencyCashTransferCreatesBothLegs()
    {
        var json = f.create(cashTransfer(f.cash.getUUID(), f.cash2.getUUID(), ",'note':'savings'"));

        var source = (AccountTransaction) f.find(json.get("uuid").getAsString()).getTransaction();
        var entry = (AccountTransferEntry) source.getCrossEntry();
        var target = entry.getTargetTransaction();

        assertThat(json.get("type").getAsString(), is("transfer-out"));
        assertThat(json.get("linked").getAsJsonObject().get("uuid").getAsString(), is(target.getUUID()));

        assertThat(source.getType(), is(AccountTransaction.Type.TRANSFER_OUT));
        assertThat(target.getType(), is(AccountTransaction.Type.TRANSFER_IN));
        assertThat(f.cash.getTransactions().contains(source), is(true));
        assertThat(f.cash2.getTransactions().contains(target), is(true));
        assertThat(source.getMonetaryAmount(), is(eur("500")));
        assertThat(target.getMonetaryAmount(), is(eur("500")));
        assertThat(source.getUnits().count(), is(0L));
        assertThat(target.getNote(), is("savings"));
    }

    @Test
    public void testForeignCurrencyTransferPutsGrossValueOnSourceLegWithInverseRate()
    {
        var json = f.create(cashTransfer(f.cash.getUUID(), f.usdCash.getUUID(), ",'targetAmount':'550'"));

        var source = (AccountTransaction) f.find(json.get("uuid").getAsString()).getTransaction();
        var target = ((AccountTransferEntry) source.getCrossEntry()).getTargetTransaction();

        assertThat(source.getMonetaryAmount(), is(eur("500")));
        assertThat(target.getMonetaryAmount(), is(usd("550")));

        // as the transfer dialog: amount in the source currency, the target
        // amount as forex, and the inverse of the EUR/USD rate 1.1
        var gross = source.getUnit(Transaction.Unit.Type.GROSS_VALUE).orElseThrow();
        assertThat(gross.getAmount(), is(eur("500")));
        assertThat(gross.getForex(), is(usd("550")));
        assertThat(gross.getExchangeRate().doubleValue(), closeTo(1 / 1.1, 0.0000000001));
        assertThat(gross.getExchangeRate().scale(), is(10));
        assertThat(target.getUnits().count(), is(0L));
    }

    @Test
    public void testForeignCurrencyTransferRequiresTargetAmount()
    {
        var e = f.createFails(cashTransfer(f.cash.getUUID(), f.usdCash.getUUID(), ""));
        assertError(e, "targetAmount", "required");

        e = f.createFails(cashTransfer(f.cash.getUUID(), f.cash2.getUUID(), ",'targetAmount':'400'"));
        assertError(e, "targetAmount", "currency-mismatch");
    }

    @Test
    public void testSameAccountIsRejected()
    {
        var e = f.createFails(cashTransfer(f.cash.getUUID(), f.cash.getUUID(), ""));
        assertError(e, "toCashAccount", "same-account");

        e = f.createFails("{'type':'cash-transfer','date':'2026-05-01','amount':'0'}");
        assertError(e, "fromCashAccount", "required");
        assertError(e, "toCashAccount", "required");
        assertThat(f.legCount(), is(0));
    }

    @Test
    public void testSecurityTransfer()
    {
        var json = f.create("{'type':'security-transfer','date':'2026-05-01','fromInvestmentAccount':'"
                        + f.broker.getUUID() + "','toInvestmentAccount':'" + f.broker2.getUUID() + "','instrument':'"
                        + f.usdSecurity.getUUID() + "','shares':'3','amount':'301.50'}");

        var source = (PortfolioTransaction) f.find(json.get("uuid").getAsString()).getTransaction();
        var target = ((PortfolioTransferEntry) source.getCrossEntry()).getTargetTransaction();

        assertThat(source.getType(), is(PortfolioTransaction.Type.TRANSFER_OUT));
        assertThat(target.getType(), is(PortfolioTransaction.Type.TRANSFER_IN));
        assertThat(f.broker.getTransactions().contains(source), is(true));
        assertThat(f.broker2.getTransactions().contains(target), is(true));

        // both legs in the instrument currency
        assertThat(source.getMonetaryAmount(), is(usd("301.50")));
        assertThat(target.getMonetaryAmount(), is(usd("301.50")));
        assertThat(source.getShares(), is(shares("3")));
        assertThat(target.getShares(), is(shares("3")));
        assertThat(target.getSecurity(), is(f.usdSecurity));
    }

    @Test
    public void testSecurityTransferWithQuoteAndRules()
    {
        var body = "{'type':'security-transfer','date':'2026-05-01','fromInvestmentAccount':'" + f.broker.getUUID()
                        + "','toInvestmentAccount':'" + f.broker2.getUUID() + "','instrument':'"
                        + f.eurSecurity.getUUID() + "','shares':'4','quote':'25.125'}";
        var json = f.create(body);
        assertThat(f.find(json.get("uuid").getAsString()).getTransaction().getMonetaryAmount(), is(eur("100.50")));

        var e = f.createFails(body.replace(f.broker2.getUUID(), f.broker.getUUID()));
        assertError(e, "toInvestmentAccount", "same-investment-account");

        e = f.createFails(body.replace("'shares':'4'", "'shares':'0'"));
        assertError(e, "shares", "must-be-positive");

        e = f.createFails(body.replace("'quote':'25.125'", "'quote':'25','amount':'150'"));
        assertError(e, "amount", "gross-mismatch");

        e = f.createFails(body.replace(",'quote':'25.125'", ""));
        assertError(e, "amount", "required");
    }

    @Test
    public void testTransferTypeRejectsForeignFields()
    {
        var e = f.createFails(cashTransfer(f.cash.getUUID(), f.cash2.getUUID(),
                        ",'cashAccount':'" + f.cash.getUUID() + "','exchangeRate':'" + BigDecimal.ONE + "'"));
        assertError(e, "cashAccount", "not-allowed-for-type");
        assertError(e, "exchangeRate", "not-allowed-for-type");
    }
}
