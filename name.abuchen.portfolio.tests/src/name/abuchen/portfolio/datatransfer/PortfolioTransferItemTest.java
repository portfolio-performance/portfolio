package name.abuchen.portfolio.datatransfer;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.time.LocalDateTime;

import org.junit.Before;
import org.junit.Test;

import name.abuchen.portfolio.datatransfer.Extractor.PortfolioTransferItem;
import name.abuchen.portfolio.datatransfer.ImportAction.Status;
import name.abuchen.portfolio.datatransfer.actions.InsertAction;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.PortfolioTransferEntry;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class PortfolioTransferItemTest
{
    private Client client;
    private Security security;

    private Portfolio portfolio;
    private Portfolio secondaryPortfolio;
    private Portfolio otherPortfolio;

    @Before
    public void prepare()
    {
        client = new Client();

        var account = new Account("EUR account");
        account.setCurrencyCode("EUR");
        client.addAccount(account);

        security = new Security("Test AG", "EUR");
        client.addSecurity(security);

        portfolio = portfolio("Portfolio", account);
        secondaryPortfolio = portfolio("Secondary portfolio", account);
        otherPortfolio = portfolio("Other portfolio", account);
    }

    @Test
    public void testOutboundTransferBooksFromPortfolioToSecondaryPortfolio()
    {
        var item = new PortfolioTransferItem(transfer());

        assertThat(item.getTypeInformation(), is(PortfolioTransaction.Type.TRANSFER_OUT.toString()));
        assertThat(item.apply(new InsertAction(client), new TestContext()).getCode(), is(Status.Code.OK));

        assertBooking(portfolio, PortfolioTransaction.Type.TRANSFER_OUT);
        assertBooking(secondaryPortfolio, PortfolioTransaction.Type.TRANSFER_IN);
    }

    @Test
    public void testInboundTransferBooksFromSecondaryPortfolioToPortfolio()
    {
        var item = new PortfolioTransferItem(transfer(), false);

        assertThat(item.getTypeInformation(), is(PortfolioTransaction.Type.TRANSFER_IN.toString()));
        assertThat(item.apply(new InsertAction(client), new TestContext()).getCode(), is(Status.Code.OK));

        assertBooking(secondaryPortfolio, PortfolioTransaction.Type.TRANSFER_OUT);
        assertBooking(portfolio, PortfolioTransaction.Type.TRANSFER_IN);
    }

    @Test
    public void testExplicitPortfolioIsTargetOfInboundTransfer()
    {
        var item = new PortfolioTransferItem(transfer(), false);
        item.setPortfolioPrimary(otherPortfolio);

        assertThat(item.apply(new InsertAction(client), new TestContext()).getCode(), is(Status.Code.OK));

        assertBooking(secondaryPortfolio, PortfolioTransaction.Type.TRANSFER_OUT);
        assertBooking(otherPortfolio, PortfolioTransaction.Type.TRANSFER_IN);
        assertThat(portfolio.getTransactions().isEmpty(), is(true));
    }

    private Portfolio portfolio(String name, Account referenceAccount)
    {
        var p = new Portfolio(name);
        p.setReferenceAccount(referenceAccount);
        client.addPortfolio(p);
        return p;
    }

    private PortfolioTransferEntry transfer()
    {
        var entry = new PortfolioTransferEntry();
        entry.setSecurity(security);
        entry.setDate(LocalDateTime.of(2021, 3, 5, 17, 7));
        entry.setShares(Values.Share.factorize(10));
        entry.setCurrencyCode("EUR");
        entry.setAmount(Values.Amount.factorize(1000));
        return entry;
    }

    private void assertBooking(Portfolio p, PortfolioTransaction.Type type)
    {
        assertThat(p.getTransactions().size(), is(1));
        assertThat(p.getTransactions().get(0).getType(), is(type));
        assertThat(p.getTransactions().get(0).getShares(), is(Values.Share.factorize(10)));
        assertThat(p.getTransactions().get(0).getSecurity(), is(security));
    }

    private class TestContext implements ImportAction.Context
    {
        @Override
        public Account getAccount(String currencyCode)
        {
            return null;
        }

        @Override
        public Account getSecondaryAccount(String currencyCode)
        {
            return null;
        }

        @Override
        public Portfolio getPortfolio()
        {
            return portfolio;
        }

        @Override
        public Portfolio getSecondaryPortfolio()
        {
            return secondaryPortfolio;
        }
    }
}
