package name.abuchen.portfolio.ui.views;

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
public class SecurityListViewTest
{
    @Test
    public void testSharesHeldIncludesBothFundTransferLegs()
    {
        Client client = new Client();
        Security source = new SecurityBuilder().addTo(client);
        Security target = new SecurityBuilder().addTo(client);
        Portfolio sourcePortfolio = new PortfolioBuilder() //
                        .buy(source, "2020-01-01", Values.Share.factorize(10), Values.Amount.factorize(1000)) //
                        .addTo(client);
        Portfolio targetPortfolio = new PortfolioBuilder().addTo(client);

        FundTransferEntry entry = new FundTransferEntry(sourcePortfolio, targetPortfolio);
        entry.setDate(LocalDateTime.parse("2020-06-01T00:00"));
        entry.setSourceSecurity(source);
        entry.setTargetSecurity(target);
        entry.setSourceShares(Values.Share.factorize(7));
        entry.setTargetShares(Values.Share.factorize(11));
        entry.setSourceMonetaryAmount(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1000)));
        entry.setTargetMonetaryAmount(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1000)));
        entry.insert();

        assertThat(SecurityListView.getSharesHeld(client, source), is(Values.Share.factorize(3)));
        assertThat(SecurityListView.getSharesHeld(client, target), is(Values.Share.factorize(11)));
    }
}
