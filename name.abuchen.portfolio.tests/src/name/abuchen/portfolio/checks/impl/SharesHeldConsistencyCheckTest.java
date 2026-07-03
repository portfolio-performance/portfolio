package name.abuchen.portfolio.checks.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.time.LocalDateTime;

import org.junit.Test;

import name.abuchen.portfolio.junit.SecurityBuilder;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.CorporateActionEntry;
import name.abuchen.portfolio.model.CorporateActionEntry.LegRole;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class SharesHeldConsistencyCheckTest
{
    @Test
    public void testDistributionLegsDoNotThrowAndTallyCorrectly()
    {
        // a valid spin-off: 33 whole shares delivered net (INBOUND +33.333,
        // FRACTION_SALE -0.333), so shares held stays >= 0 and no issue is
        // reported -- and, crucially, the switch must not throw on the two
        // new distribution types
        Client client = new Client();
        Security parent = new SecurityBuilder(CurrencyUnit.EUR).addTo(client);
        Security spinco = new SecurityBuilder(CurrencyUnit.EUR).addTo(client);

        Portfolio portfolio = new Portfolio();
        portfolio.setName("portfolio");
        client.addPortfolio(portfolio);
        Account account = new Account("account");
        client.addAccount(account);

        var exDate = LocalDateTime.parse("2015-01-09T00:00");

        var source = new PortfolioTransaction();
        source.setType(PortfolioTransaction.Type.DISTRIBUTION_OUTBOUND);
        source.setDateTime(exDate);
        source.setSecurity(parent);
        source.setCurrencyCode(CurrencyUnit.EUR);
        source.setShares(0);

        var target = new PortfolioTransaction();
        target.setType(PortfolioTransaction.Type.DISTRIBUTION_INBOUND);
        target.setDateTime(exDate);
        target.setSecurity(spinco);
        target.setCurrencyCode(CurrencyUnit.EUR);
        target.setShares(Values.Share.factorize(33.333));

        var fractionSale = new PortfolioTransaction();
        fractionSale.setType(PortfolioTransaction.Type.SELL);
        fractionSale.setDateTime(exDate);
        fractionSale.setSecurity(spinco);
        fractionSale.setCurrencyCode(CurrencyUnit.EUR);
        fractionSale.setShares(Values.Share.factorize(0.333));

        var cashInLieu = new AccountTransaction();
        cashInLieu.setType(AccountTransaction.Type.SELL);
        cashInLieu.setDateTime(exDate);
        cashInLieu.setSecurity(spinco);
        cashInLieu.setCurrencyCode(CurrencyUnit.EUR);

        var entry = new CorporateActionEntry();
        entry.setType(CorporateActionEntry.Type.SPIN_OFF);
        entry.addLeg(portfolio, source, LegRole.SOURCE);
        entry.addLeg(portfolio, target, LegRole.TARGET);
        entry.addLeg(portfolio, fractionSale, LegRole.FRACTION_SALE);
        entry.addLeg(account, cashInLieu, LegRole.CASH_IN_LIEU);
        entry.insert();
        client.addCorporateAction(entry);

        // must not throw, and 33.333 - 0.333 = 33.0 >= 0 -> no negative-shares issue
        assertThat(new SharesHeldConsistencyCheck().execute(client).size(), is(0));
    }
}
