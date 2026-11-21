package name.abuchen.portfolio.ui.wizards.datatransfer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.time.LocalDateTime;

import org.junit.Before;
import org.junit.Test;

import name.abuchen.portfolio.datatransfer.Extractor;
import name.abuchen.portfolio.datatransfer.ImportAction;
import name.abuchen.portfolio.datatransfer.ImportAction.Status;
import name.abuchen.portfolio.junit.AccountBuilder;
import name.abuchen.portfolio.junit.PortfolioBuilder;
import name.abuchen.portfolio.junit.SecurityBuilder;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransferEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransferEntry;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.Messages;

@SuppressWarnings("nls")
public class CheckTransferSourceAndTargetActionTest
{
    private static final LocalDateTime DATE = LocalDateTime.parse("2024-01-02T00:00");

    private final CheckTransferSourceAndTargetAction action = new CheckTransferSourceAndTargetAction();

    private Account account;
    private Account otherAccount;
    private Portfolio portfolio;
    private Portfolio otherPortfolio;
    private Security security;

    @Before
    public void setup()
    {
        var client = new Client();
        account = new AccountBuilder().addTo(client);
        otherAccount = new AccountBuilder().addTo(client);
        portfolio = new PortfolioBuilder(account).addTo(client);
        otherPortfolio = new PortfolioBuilder(account).addTo(client);
        security = new SecurityBuilder().addTo(client);
    }

    private static AccountTransferEntry accountTransfer()
    {
        var entry = new AccountTransferEntry();
        entry.setDate(DATE);
        entry.setCurrencyCode(CurrencyUnit.EUR);
        entry.setAmount(Values.Amount.factorize(100));
        return entry;
    }

    private PortfolioTransferEntry portfolioTransfer()
    {
        var entry = new PortfolioTransferEntry();
        entry.setSecurity(security);
        entry.setDate(DATE);
        entry.setCurrencyCode(CurrencyUnit.EUR);
        entry.setAmount(Values.Amount.factorize(100));
        entry.setShares(Values.Share.factorize(1));
        return entry;
    }

    @Test
    public void testAccountTransferToTheSameAccountIsAnError()
    {
        var status = action.process(accountTransfer(), account, account);

        assertThat(status.getCode(), is(Status.Code.ERROR));
        assertThat(status.getMessage(), is(Messages.MsgAccountMustBeDifferent));
    }

    @Test
    public void testAccountTransferBetweenDifferentAccountsIsValid()
    {
        assertThat(action.process(accountTransfer(), account, otherAccount).getCode(), is(Status.Code.OK));
    }

    @Test
    public void testPortfolioTransferToTheSamePortfolioIsAnError()
    {
        var status = action.process(portfolioTransfer(), portfolio, portfolio);

        assertThat(status.getCode(), is(Status.Code.ERROR));
        assertThat(status.getMessage(), is(Messages.MsgPortfolioMustBeDifferent));
    }

    @Test
    public void testPortfolioTransferBetweenDifferentPortfoliosIsValid()
    {
        assertThat(action.process(portfolioTransfer(), portfolio, otherPortfolio).getCode(), is(Status.Code.OK));
    }

    @Test
    public void testSameAccountSelectedForAccountAndOffsetAccountOfInboundTransfer()
    {
        // the accounts are resolved as for the import: account and offset
        // account are the same selection
        var context = new ImportAction.Context()
        {
            @Override
            public Account getAccount(String currencyCode)
            {
                return account;
            }

            @Override
            public Account getSecondaryAccount(String currencyCode)
            {
                return account;
            }

            @Override
            public Portfolio getPortfolio()
            {
                return portfolio;
            }

            @Override
            public Portfolio getSecondaryPortfolio()
            {
                return otherPortfolio;
            }
        };

        var item = new Extractor.AccountTransferItem(accountTransfer(), false);

        assertThat(item.apply(action, context).getCode(), is(Status.Code.ERROR));
    }
}
