package name.abuchen.portfolio.ui.util.viewers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.Test;

import name.abuchen.portfolio.junit.SecurityBuilder;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.CorporateActionEntry;
import name.abuchen.portfolio.model.CorporateActionEntry.LegRole;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.util.viewers.TransactionOwnerListEditingSupport.EditMode;

public class TransactionOwnerListEditingSupportTest
{
    @Test
    public void testCanEditIsRefusedForCorporateActionLegs()
    {
        Client client = new Client();
        Security parent = new SecurityBuilder(CurrencyUnit.EUR).addTo(client);
        Security spinco = new SecurityBuilder(CurrencyUnit.EUR).addTo(client);

        Portfolio portfolio = new Portfolio();
        portfolio.setName("portfolio");
        client.addPortfolio(portfolio);

        var source = new PortfolioTransaction();
        source.setType(PortfolioTransaction.Type.DISTRIBUTION_OUTBOUND);
        source.setSecurity(parent);
        source.setCurrencyCode(CurrencyUnit.EUR);
        var target = new PortfolioTransaction();
        target.setType(PortfolioTransaction.Type.DISTRIBUTION_INBOUND);
        target.setSecurity(spinco);
        target.setCurrencyCode(CurrencyUnit.EUR);

        var entry = new CorporateActionEntry();
        entry.setType(CorporateActionEntry.Type.SPIN_OFF);
        entry.addLeg(portfolio, source, LegRole.SOURCE);
        entry.addLeg(portfolio, target, LegRole.TARGET);
        entry.insert();
        client.addCorporateAction(entry);

        var editingSupport = new TransactionOwnerListEditingSupport(client, EditMode.OWNER);

        // a spin-off leg must NOT be inline-editable (owner changes go through
        // the dialog; inline delete->insert would drop the registry entry)
        assertThat(editingSupport.canEdit(source), is(false));
    }

    @Test
    public void testCanEditStillAllowedForBuySellLegs()
    {
        Client client = new Client();
        Security security = new SecurityBuilder(CurrencyUnit.EUR).addTo(client);

        Portfolio portfolio = new Portfolio();
        portfolio.setName("portfolio");
        client.addPortfolio(portfolio);
        Account account = new Account("account");
        account.setCurrencyCode(CurrencyUnit.EUR);
        client.addAccount(account);

        var entry = new BuySellEntry(portfolio, account);
        entry.setType(PortfolioTransaction.Type.BUY);
        entry.setCurrencyCode(CurrencyUnit.EUR);
        entry.setSecurity(security);
        entry.setShares(Values.Share.factorize(1));
        entry.setAmount(Values.Amount.factorize(100));
        entry.insert();

        var editingSupport = new TransactionOwnerListEditingSupport(client, EditMode.OWNER);

        // an ordinary buy/sell leg is still inline-editable (behaviour preserved)
        assertThat(editingSupport.canEdit(entry.getPortfolioTransaction()), is(true));
    }
}
