package name.abuchen.portfolio.snapshot.filter;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.MatcherAssert.assertThat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;

import name.abuchen.portfolio.AccountBuilder;
import name.abuchen.portfolio.PortfolioBuilder;
import name.abuchen.portfolio.SecurityBuilder;
import name.abuchen.portfolio.TestCurrencyConverter;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.PerformanceIndex;
import name.abuchen.portfolio.util.Interval;

@SuppressWarnings("nls")
public class ClientSecurityFilterTest
{
    private Client client;
    private Security securityUSD;
    private Security securityEUR;
    private Account accountEUR;
    private Account accountUSD;

    @Before
    public void setupClient()
    {
        client = new Client();

        securityUSD = new SecurityBuilder() //
                        .addPrice("2016-06-01", Values.Quote.factorize(100)) //
                        .addTo(client);
        securityEUR = new SecurityBuilder(CurrencyUnit.USD) //
                        .addPrice("2016-06-01", Values.Quote.factorize(100)) //
                        .addTo(client);

        accountEUR = new AccountBuilder(CurrencyUnit.EUR) //
                        .dividend("2017-01-01", Values.Amount.factorize(20), securityEUR) //
                        .addTo(client);

        accountUSD = new AccountBuilder(CurrencyUnit.USD) //
                        .dividend("2017-01-01", Values.Amount.factorize(20), securityUSD) //
                        .addTo(client);

        new PortfolioBuilder(accountEUR) //
                        .inbound_delivery(securityEUR, "2016-06-01", Values.Share.factorize(20),
                                        Values.Amount.factorize(2000)) //
                        .inbound_delivery(securityUSD, "2016-06-01", Values.Share.factorize(20),
                                        Values.Amount.factorize(2000)) //
                        .addTo(client);
    }

    @Test
    public void testThatForexTransactionsAreAddedToTheCorrectAccount()
    {
        Client filtered = new ClientSecurityFilter(securityUSD).filter(client);

        assertThat(filtered.getAccounts(), hasSize(2));
        assertThat(filtered.getPortfolios(), hasSize(1));

        // no transactions in EUR account (but exists b/c it is reference
        // account for the portfolio)
        assertThat(filtered.getAccounts().stream() //
                        .filter(a -> accountEUR.getName().equals(a.getName())).findAny()
                        .orElseThrow(IllegalArgumentException::new).getTransactions(), empty());

        // only one transaction on portfolio
        assertThat(filtered.getPortfolios().get(0).getTransactions(), hasSize(1));

    }

    @Test
    public void testThatDividendsAreOnlyAddedToTheReferenceAccount()
    {
        Client filtered = new ClientSecurityFilter(securityEUR).filter(client);

        assertThat(filtered.getAccounts(), hasSize(1));
        assertThat(filtered.getPortfolios(), hasSize(1));

        // check that USD account is not included
        assertThat(filtered.getAccounts().stream().filter(a -> accountUSD.getName().equals(a.getName()))
                        .collect(Collectors.toList()), empty());
    }

    @Test
    public void testThatAllSecuritiesHaveIdendicalPerformanceToClient()
    {
        Client filtered = new ClientSecurityFilter(securityEUR, securityUSD).filter(client);

        assertThat(filtered.getAccounts(), hasSize(2));
        assertThat(filtered.getPortfolios(), hasSize(1));

        List<Exception> warnings = new ArrayList<>();
        TestCurrencyConverter converter = new TestCurrencyConverter();
        Interval interval = Interval.of(LocalDate.parse("2015-12-31"), LocalDate.parse("2017-01-31"));

        PerformanceIndex all = PerformanceIndex.forClient(client, converter, interval, warnings);
        assertThat(warnings, empty());

        PerformanceIndex filteredAll = PerformanceIndex.forClient(filtered, converter, interval, warnings);
        assertThat(warnings, empty());

        assertThat(all.getFinalAccumulatedPercentage(), is(filteredAll.getFinalAccumulatedPercentage()));
        assertThat(all.getDeltaPercentage(), is(filteredAll.getDeltaPercentage()));
    }
}
