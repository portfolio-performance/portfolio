package name.abuchen.portfolio.checks.impl;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.time.LocalDateTime;

import org.junit.Test;

import name.abuchen.portfolio.junit.PortfolioBuilder;
import name.abuchen.portfolio.junit.SecurityBuilder;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.FundTransferEntry;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class SharesHeldConsistencyCheckTest
{
    @Test
    public void testFundTransferKeepsSharesConsistent()
    {
        Client client = new Client();
        Security source = new SecurityBuilder().addTo(client);
        Security target = new SecurityBuilder().addTo(client);
        Portfolio sourcePortfolio = new PortfolioBuilder() //
                        .buy(source, "2020-01-01", Values.Share.factorize(10), Values.Amount.factorize(1000)) //
                        .addTo(client);
        Portfolio targetPortfolio = new PortfolioBuilder().addTo(client);

        addFundTransfer(sourcePortfolio, targetPortfolio, source, target, 7, 11);

        assertThat(new SharesHeldConsistencyCheck().execute(client).isEmpty(), is(true));
    }

    @Test
    public void testFundTransferReportsInsufficientSourceShares()
    {
        Client client = new Client();
        Security source = new SecurityBuilder().addTo(client);
        Security target = new SecurityBuilder().addTo(client);
        Portfolio sourcePortfolio = new PortfolioBuilder() //
                        .buy(source, "2020-01-01", Values.Share.factorize(5), Values.Amount.factorize(1000)) //
                        .addTo(client);
        Portfolio targetPortfolio = new PortfolioBuilder().addTo(client);

        addFundTransfer(sourcePortfolio, targetPortfolio, source, target, 7, 11);

        var issues = new SharesHeldConsistencyCheck().execute(client);

        assertThat(issues.size(), is(1));
        assertThat(issues.get(0).getEntity(), is(sourcePortfolio));
    }

    private void addFundTransfer(Portfolio sourcePortfolio, Portfolio targetPortfolio, Security source,
                    Security target, double sourceShares, double targetShares)
    {
        FundTransferEntry entry = new FundTransferEntry(sourcePortfolio, targetPortfolio);
        entry.setDate(LocalDateTime.parse("2020-06-01T00:00"));
        entry.setSourceSecurity(source);
        entry.setTargetSecurity(target);
        entry.setSourceShares(Values.Share.factorize(sourceShares));
        entry.setTargetShares(Values.Share.factorize(targetShares));
        entry.setSourceMonetaryAmount(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1000)));
        entry.setTargetMonetaryAmount(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1000)));
        entry.insert();
    }
}
