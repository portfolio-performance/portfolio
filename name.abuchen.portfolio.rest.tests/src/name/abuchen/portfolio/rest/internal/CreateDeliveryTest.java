package name.abuchen.portfolio.rest.internal;

import static name.abuchen.portfolio.rest.internal.TransactionFixture.assertError;
import static name.abuchen.portfolio.rest.internal.TransactionFixture.eur;
import static name.abuchen.portfolio.rest.internal.TransactionFixture.shares;
import static name.abuchen.portfolio.rest.internal.TransactionFixture.usd;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.is;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Transaction;

@SuppressWarnings("nls")
public class CreateDeliveryTest
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

    private String delivery(String direction, String instrument, String extra)
    {
        return "{'type':'delivery-" + direction + "','date':'2026-02-01','investmentAccount':'" + f.broker.getUUID()
                        + "','instrument':'" + instrument + "','shares':'5'" + extra + "}";
    }

    private PortfolioTransaction created(String body)
    {
        var json = f.create(body);
        assertThat(json.has("linked"), is(false));
        return (PortfolioTransaction) f.find(json.get("uuid").getAsString()).getTransaction();
    }

    @Test
    public void testInboundDeliveryInReferenceAccountCurrency()
    {
        var tx = created(delivery("inbound", f.eurSecurity.getUUID(), ",'quote':'20','fees':'1.50'"));

        assertThat(f.broker.getTransactions().contains(tx), is(true));
        assertThat(tx.getCrossEntry() == null, is(true));
        assertThat(tx.getType(), is(PortfolioTransaction.Type.DELIVERY_INBOUND));
        assertThat(tx.getShares(), is(shares("5")));
        assertThat(tx.getCurrencyCode(), is("EUR"));
        assertThat(tx.getMonetaryAmount(), is(eur("101.50")));
        assertThat(tx.getUnitSum(Transaction.Unit.Type.FEE), is(eur("1.50")));

        // no cash leg anywhere
        assertThat(f.cash.getTransactions().isEmpty(), is(true));
    }

    @Test
    public void testDeliveryWithExplicitCurrencyAndForex()
    {
        var tx = created(delivery("inbound", f.usdSecurity.getUUID(), ",'grossValue':'100','currency':'EUR'"));

        assertThat(tx.getCurrencyCode(), is("EUR"));
        var gross = tx.getUnit(Transaction.Unit.Type.GROSS_VALUE).orElseThrow();
        assertThat(gross.getForex(), is(usd("100")));
        assertThat(gross.getAmount(), is(eur("90")));
        assertThat(gross.getExchangeRate(), comparesEqualTo(TransactionFixture.USD_EUR));

        // in the instrument currency: no forex
        tx = created(delivery("inbound", f.usdSecurity.getUUID(), ",'grossValue':'100','currency':'USD'"));
        assertThat(tx.getMonetaryAmount(), is(usd("100")));
        assertThat(tx.getUnit(Transaction.Unit.Type.GROSS_VALUE).isPresent(), is(false));

        var e = f.createFails(delivery("inbound", f.usdSecurity.getUUID(), ",'grossValue':'100','currency':'XYZ'"));
        assertError(e, "currency", "unknown-currency");
    }

    @Test
    public void testOutboundDeliveryMayBeZero()
    {
        var tx = created(delivery("outbound", f.eurSecurity.getUUID(), ",'grossValue':'0'"));
        assertThat(tx.getType(), is(PortfolioTransaction.Type.DELIVERY_OUTBOUND));
        assertThat(tx.getAmount(), is(0L));

        var e = f.createFails(delivery("inbound", f.eurSecurity.getUUID(), ",'grossValue':'0'"));
        assertError(e, "grossValue", "must-be-positive");
    }

    @Test
    public void testOutboundDeliverySubtractsFees()
    {
        var tx = created(delivery("outbound", f.eurSecurity.getUUID(), ",'grossValue':'100','taxes':'10'"));
        assertThat(tx.getMonetaryAmount(), is(eur("90")));
    }

    @Test
    public void testDeliveryRejectsCashAccount()
    {
        var e = f.createFails(delivery("inbound", f.eurSecurity.getUUID(),
                        ",'quote':'1','cashAccount':'" + f.cash.getUUID() + "'"));
        assertError(e, "cashAccount", "not-allowed-for-type");
    }
}
