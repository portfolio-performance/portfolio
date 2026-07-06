package name.abuchen.portfolio.ui.wizards.datatransfer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import java.time.LocalDateTime;
import java.util.Collections;

import org.junit.Before;
import org.junit.Test;

import name.abuchen.portfolio.datatransfer.Extractor;
import name.abuchen.portfolio.junit.AccountBuilder;
import name.abuchen.portfolio.junit.PortfolioBuilder;
import name.abuchen.portfolio.junit.SecurityBuilder;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.AccountTransferEntry;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.PortfolioTransferEntry;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class ShadowSessionTest
{
    private static final LocalDateTime DATE = LocalDateTime.parse("2024-01-02T00:00");

    private Client client;
    private Account account;
    private Portfolio portfolio;
    private Security security;

    @Before
    public void setup()
    {
        client = new Client();
        security = new SecurityBuilder().addTo(client);
        account = new AccountBuilder().addTo(client);
        portfolio = new PortfolioBuilder(account).addTo(client);
    }

    private ShadowSession newSession()
    {
        return ShadowSession.create(client, Collections.emptyList());
    }

    // -- harvest(): one test per transaction type, owners map back to the real
    // client and values are copied

    @Test
    public void testHarvestAccountTransaction()
    {
        var session = newSession();

        var at = new AccountTransaction(DATE, CurrencyUnit.EUR, Values.Amount.factorize(100), security,
                        AccountTransaction.Type.DIVIDENDS);
        session.toShadow(account).addTransaction(at);

        var harvested = session.harvest();

        assertThat(harvested, hasSize(1));
        var item = harvested.get(0).getItem();
        assertThat(item, instanceOf(Extractor.TransactionItem.class));
        assertThat(item.getAccountPrimary(), is(account));

        var subject = (AccountTransaction) item.getSubject();
        assertThat(subject.getType(), is(AccountTransaction.Type.DIVIDENDS));
        assertThat(subject.getAmount(), is(Values.Amount.factorize(100)));
        assertThat(subject.getSecurity(), is(security));
    }

    @Test
    public void testHarvestBuySell()
    {
        var session = newSession();

        insertBuy(session.toShadow(portfolio), session.toShadow(account));

        var harvested = session.harvest();

        // exactly one item: the portfolio side of the cross-entry must not be
        // harvested a second time (the switch handles de-duplication)
        assertThat(harvested, hasSize(1));
        var item = harvested.get(0).getItem();
        assertThat(item, instanceOf(Extractor.BuySellEntryItem.class));
        assertThat(item.getAccountPrimary(), is(account));
        assertThat(item.getPortfolioPrimary(), is(portfolio));

        var subject = (BuySellEntry) item.getSubject();
        assertThat(subject.getPortfolioTransaction().getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(subject.getPortfolioTransaction().getSecurity(), is(security));
        assertThat(subject.getPortfolioTransaction().getShares(), is(Values.Share.factorize(10)));
        assertThat(subject.getPortfolioTransaction().getAmount(), is(Values.Amount.factorize(1000)));
    }

    @Test
    public void testHarvestDelivery()
    {
        var session = newSession();

        var pt = new PortfolioTransaction(DATE, CurrencyUnit.EUR, Values.Amount.factorize(500), security,
                        Values.Share.factorize(5), PortfolioTransaction.Type.DELIVERY_INBOUND, 0, 0);
        session.toShadow(portfolio).addTransaction(pt);

        var harvested = session.harvest();

        assertThat(harvested, hasSize(1));
        var item = harvested.get(0).getItem();
        assertThat(item, instanceOf(Extractor.TransactionItem.class));
        assertThat(item.getPortfolioPrimary(), is(portfolio));

        var subject = (PortfolioTransaction) item.getSubject();
        assertThat(subject.getType(), is(PortfolioTransaction.Type.DELIVERY_INBOUND));
        assertThat(subject.getShares(), is(Values.Share.factorize(5)));
    }

    @Test
    public void testHarvestAccountTransfer()
    {
        var account2 = new AccountBuilder().addTo(client);
        var session = newSession();

        var transfer = new AccountTransferEntry(session.toShadow(account), session.toShadow(account2));
        transfer.setDate(DATE);
        transfer.setCurrencyCode(CurrencyUnit.EUR);
        transfer.setAmount(Values.Amount.factorize(250));
        transfer.insert();

        var harvested = session.harvest();

        assertThat(harvested, hasSize(1));
        var item = harvested.get(0).getItem();
        assertThat(item, instanceOf(Extractor.AccountTransferItem.class));
        assertThat(item.getAccountPrimary(), is(account));
        assertThat(item.getAccountSecondary(), is(account2));

        var subject = (AccountTransferEntry) item.getSubject();
        assertThat(subject.getSourceTransaction().getAmount(), is(Values.Amount.factorize(250)));
    }

    @Test
    public void testHarvestPortfolioTransfer()
    {
        var portfolio2 = new PortfolioBuilder(account).addTo(client);
        var session = newSession();

        var transfer = new PortfolioTransferEntry(session.toShadow(portfolio), session.toShadow(portfolio2));
        transfer.setDate(DATE);
        transfer.setCurrencyCode(CurrencyUnit.EUR);
        transfer.setSecurity(security);
        transfer.setShares(Values.Share.factorize(7));
        transfer.setAmount(Values.Amount.factorize(700));
        transfer.insert();

        var harvested = session.harvest();

        assertThat(harvested, hasSize(1));
        var item = harvested.get(0).getItem();
        assertThat(item, instanceOf(Extractor.PortfolioTransferItem.class));
        assertThat(item.getPortfolioPrimary(), is(portfolio));
        assertThat(item.getPortfolioSecondary(), is(portfolio2));

        var subject = (PortfolioTransferEntry) item.getSubject();
        assertThat(subject.getSourceTransaction().getSecurity(), is(security));
        assertThat(subject.getSourceTransaction().getShares(), is(Values.Share.factorize(7)));
    }

    // -- invariants

    @Test
    public void testMultipleTransactionsHarvestAll()
    {
        var session = newSession();

        // simulates "Save and new": one dialog session writes several
        // transactions into the shadow client
        session.toShadow(account).addTransaction(new AccountTransaction(DATE, CurrencyUnit.EUR,
                        Values.Amount.factorize(10), null, AccountTransaction.Type.DEPOSIT));
        session.toShadow(account).addTransaction(new AccountTransaction(DATE, CurrencyUnit.EUR,
                        Values.Amount.factorize(20), null, AccountTransaction.Type.DEPOSIT));

        assertThat(session.harvest(), hasSize(2));
    }

    @Test
    public void testHarvestIsEmptyWithoutTransactions()
    {
        assertThat(newSession().harvest(), hasSize(0));
    }

    // -- shadow* reconstruction for editing: owners map to the shadow client,
    // values are copied

    @Test
    public void testShadowBuySellMapsOwnersToShadow()
    {
        var realEntry = new BuySellEntry(portfolio, account);
        realEntry.setType(PortfolioTransaction.Type.BUY);
        realEntry.setDate(DATE);
        realEntry.setSecurity(security);
        realEntry.setShares(Values.Share.factorize(10));
        realEntry.setCurrencyCode(CurrencyUnit.EUR);
        realEntry.setAmount(Values.Amount.factorize(1000));

        var item = new Extractor.BuySellEntryItem(realEntry);
        item.setAccountPrimary(account);
        item.setPortfolioPrimary(portfolio);

        var session = newSession();
        var shadowEntry = session.shadowBuySell(item);

        assertThat(shadowEntry.getPortfolio(), is(session.toShadow(portfolio)));
        assertThat(shadowEntry.getAccount(), is(session.toShadow(account)));
        assertThat(shadowEntry.getPortfolioTransaction().getSecurity(), is(security));
        assertThat(shadowEntry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(10)));
        assertThat(shadowEntry.getPortfolioTransaction().getAmount(), is(Values.Amount.factorize(1000)));
    }

    @Test
    public void testShadowAccountTransferMapsOwnersToShadow()
    {
        var account2 = new AccountBuilder().addTo(client);

        var realEntry = new AccountTransferEntry(account, account2);
        realEntry.setDate(DATE);
        realEntry.setCurrencyCode(CurrencyUnit.EUR);
        realEntry.setAmount(Values.Amount.factorize(250));

        var item = new Extractor.AccountTransferItem(realEntry, true);
        item.setAccountPrimary(account);
        item.setAccountSecondary(account2);

        var session = newSession();
        var shadowEntry = session.shadowAccountTransfer(item);

        assertThat(shadowEntry.getSourceAccount(), is(session.toShadow(account)));
        assertThat(shadowEntry.getTargetAccount(), is(session.toShadow(account2)));
        assertThat(shadowEntry.getSourceTransaction().getAmount(), is(Values.Amount.factorize(250)));
    }

    @Test
    public void testShadowPortfolioTransferMapsOwnersToShadow()
    {
        var portfolio2 = new PortfolioBuilder(account).addTo(client);

        var realEntry = new PortfolioTransferEntry(portfolio, portfolio2);
        realEntry.setDate(DATE);
        realEntry.setCurrencyCode(CurrencyUnit.EUR);
        realEntry.setSecurity(security);
        realEntry.setShares(Values.Share.factorize(7));
        realEntry.setAmount(Values.Amount.factorize(700));

        var item = new Extractor.PortfolioTransferItem(realEntry);
        item.setPortfolioPrimary(portfolio);
        item.setPortfolioSecondary(portfolio2);

        var session = newSession();
        var shadowEntry = session.shadowPortfolioTransfer(item);

        assertThat(shadowEntry.getSourcePortfolio(), is(session.toShadow(portfolio)));
        assertThat(shadowEntry.getTargetPortfolio(), is(session.toShadow(portfolio2)));
        assertThat(shadowEntry.getSourceTransaction().getShares(), is(Values.Share.factorize(7)));
    }

    // -- round-trip: harvest yields real-referencing items; a fresh session can
    // map them back into shadow coordinates for editing

    @Test
    public void testEditRoundTripBuySell()
    {
        var harvestSession = newSession();
        insertBuy(harvestSession.toShadow(portfolio), harvestSession.toShadow(account));
        var item = harvestSession.harvest().get(0).getItem();

        assertThat(item.getPortfolioPrimary(), is(portfolio));
        assertThat(item.getAccountPrimary(), is(account));

        var editSession = newSession();
        var shadowEntry = editSession.shadowBuySell(item);

        assertThat(shadowEntry.getPortfolio(), is(editSession.toShadow(portfolio)));
        assertThat(shadowEntry.getAccount(), is(editSession.toShadow(account)));
        assertThat(shadowEntry.getPortfolioTransaction().getShares(), is(Values.Share.factorize(10)));
    }

    // -- toShadow mapping

    @Test
    public void testToShadowMapsAndIsNullSafe()
    {
        var session = newSession();

        assertThat(session.toShadow(account), is(notNullValue()));
        assertThat(session.toShadow(account), is(not(account)));
        assertThat(session.toShadow((Account) null), is(nullValue()));
        assertThat(session.toShadow(new Account("unknown")), is(nullValue()));
    }

    private void insertBuy(Portfolio shadowPortfolio, Account shadowAccount)
    {
        var entry = new BuySellEntry(shadowPortfolio, shadowAccount);
        entry.setType(PortfolioTransaction.Type.BUY);
        entry.setDate(DATE);
        entry.setSecurity(security);
        entry.setShares(Values.Share.factorize(10));
        entry.setCurrencyCode(CurrencyUnit.EUR);
        entry.setAmount(Values.Amount.factorize(1000));
        entry.insert();
    }
}
