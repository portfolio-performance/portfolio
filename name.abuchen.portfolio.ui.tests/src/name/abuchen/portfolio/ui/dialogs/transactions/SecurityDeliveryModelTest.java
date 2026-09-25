package name.abuchen.portfolio.ui.dialogs.transactions;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.math.BigDecimal;

import org.junit.Test;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.Values;

public class SecurityDeliveryModelTest
{
    @Test
    public void testDividendInSharesWithoutReferenceAccount()
    {
        Client client = new Client();
        Portfolio portfolio = new Portfolio();
        client.addPortfolio(portfolio);

        Security security = new Security("Bitcoin", "EUR"); //$NON-NLS-1$ //$NON-NLS-2$
        client.addSecurity(security);

        var model = new SecurityDeliveryModel(client, PortfolioTransaction.Type.DIVIDENDS);
        model.setPortfolio(portfolio);
        model.setSecurity(security);
        model.setShares(Values.Share.factorize(0.0243));
        model.setQuote(BigDecimal.valueOf(100));
        model.applyChanges();

        assertThat(portfolio.getTransactions().size(), is(1));
        PortfolioTransaction transaction = portfolio.getTransactions().get(0);
        assertThat(transaction.getType(), is(PortfolioTransaction.Type.DIVIDENDS));
        assertThat(transaction.getShares(), is(Values.Share.factorize(0.0243)));
        assertThat(transaction.getAmount(), is(2_43L));
        assertThat(client.getAccounts().isEmpty(), is(true));
    }

    @Test
    public void testCurrencyResetsWhenSwitchingToPortfolioWithoutReferenceAccount()
    {
        Client client = new Client();
        Portfolio withAccount = new Portfolio();
        Account account = new Account();
        account.setCurrencyCode("USD"); //$NON-NLS-1$
        withAccount.setReferenceAccount(account);

        var model = new SecurityDeliveryModel(client, PortfolioTransaction.Type.DIVIDENDS);
        model.setPortfolio(withAccount);
        assertThat(model.getTransactionCurrencyCode(), is("USD")); //$NON-NLS-1$

        model.setPortfolio(new Portfolio());
        assertThat(model.getTransactionCurrencyCode(), is(client.getBaseCurrency()));
    }
}
