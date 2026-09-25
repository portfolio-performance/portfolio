package name.abuchen.portfolio.rest.internal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.Optional;

import org.junit.Test;

import name.abuchen.portfolio.junit.AccountBuilder;
import name.abuchen.portfolio.junit.PortfolioBuilder;
import name.abuchen.portfolio.junit.SecurityBuilder;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class IdempotencyIndexTest
{
    @Test
    public void testSourceMarksTheKey()
    {
        assertThat(IdempotencyIndex.source("broker-1"), is("api:broker-1"));
    }

    @Test
    public void testFindsDepositByClientRef()
    {
        var client = new Client();
        var account = new AccountBuilder().deposit_("2026-01-02", Values.Amount.factorize(100)).addTo(client);
        var deposit = account.getTransactions().get(0);
        deposit.setSource(IdempotencyIndex.source("ref-1"));

        var found = IdempotencyIndex.findTransaction(client, "ref-1");

        assertThat(found.isPresent(), is(true));
        assertThat(found.get().getTransaction(), is(deposit));
        assertThat(IdempotencyIndex.findTransaction(client, "ref-2"), is(Optional.empty()));
        assertThat(IdempotencyIndex.findTransaction(client, null), is(Optional.empty()));
    }

    @Test
    public void testFindsBuySellPairOnceByEitherLeg()
    {
        var client = new Client();
        var security = new SecurityBuilder().addTo(client);
        var account = new AccountBuilder().addTo(client);
        var portfolio = new PortfolioBuilder(account)
                        .buy(security, "2026-01-02", Values.Share.factorize(10), Values.Amount.factorize(1000))
                        .addTo(client);

        var buy = portfolio.getTransactions().get(0);
        var entry = (BuySellEntry) buy.getCrossEntry();
        entry.setSource(IdempotencyIndex.source("buy-1"));

        var found = IdempotencyIndex.findTransaction(client, "buy-1");

        assertThat(found.isPresent(), is(true));
        assertThat(found.get().getTransaction() instanceof PortfolioTransaction, is(true));
        assertThat(found.get().getTransaction().getUUID(), is(buy.getUUID()));
    }

    @Test
    public void testDoesNotMatchForeignSources()
    {
        var client = new Client();
        var account = new AccountBuilder().deposit_("2026-01-02", Values.Amount.factorize(100)).addTo(client);
        account.getTransactions().get(0).setSource("ref-1"); // e.g. an import, not the API

        assertThat(IdempotencyIndex.findTransaction(client, "ref-1"), is(Optional.empty()));
    }

    @Test
    public void testEntityKeysAreScopedByFileAndKind()
    {
        var index = new IdempotencyIndex();
        index.rememberEntity("file-1", "instrument", "ref", "uuid-1");

        assertThat(index.findEntity("file-1", "instrument", "ref"), is(Optional.of("uuid-1")));
        assertThat(index.findEntity("file-2", "instrument", "ref"), is(Optional.empty()));
        assertThat(index.findEntity("file-1", "cash-account", "ref"), is(Optional.empty()));
        assertThat(index.findEntity("file-1", "instrument", null), is(Optional.empty()));

        index.rememberEntity("file-1", "instrument", null, "uuid-2"); // no key, nothing to remember
        assertThat(index.findEntity("file-1", "instrument", "ref"), is(Optional.of("uuid-1")));
    }
}
