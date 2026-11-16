package name.abuchen.portfolio.ui.views.dashboard;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.Collections;
import java.util.List;

import org.junit.Test;

import name.abuchen.portfolio.junit.AccountBuilder;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.filter.PortfolioClientFilter;

public class DashboardDataTest
{
    @Test
    public void testClientFilterIsAppliedToFilteredClient()
    {
        Client client = new Client();

        Account accountA = new AccountBuilder().deposit_("2024-01-01", Values.Amount.factorize(100)).addTo(client);
        new AccountBuilder().deposit_("2024-01-01", Values.Amount.factorize(100)).addTo(client);

        var data = new DashboardData(client);

        assertThat(data.getFilteredClient(), is(client));

        data.setClientFilter(new PortfolioClientFilter(Collections.emptyList(), List.of(accountA)));

        assertThat(data.getFilteredClient().getAccounts().size(), is(1));
    }
}
