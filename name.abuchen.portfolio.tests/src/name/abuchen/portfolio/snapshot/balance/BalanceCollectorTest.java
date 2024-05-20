package name.abuchen.portfolio.snapshot.balance;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.junit.AccountBuilder;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class BalanceCollectorTest
{

    @Test
    public void testProposalsWithFutureTransactions()
    {
        var today = LocalDate.of(2024, 5, 10);

        try (MockedStatic<LocalDate> mockedStatic = Mockito.mockStatic(LocalDate.class, invocation -> {
            if (invocation.getMethod().getName().equals("now"))
                return today;
            else
                return invocation.callRealMethod();
        }))
        {
            var client = new Client();

            var account = new AccountBuilder() //
                            .deposit_("2024-04-12", 100_00) //
                            .interest("2024-05-02", 10_00) //
                            .interest("2024-05-12", 10_00) //
                            .fees____("2024-05-12", 10_00) //
                            .fees____("2024-05-12", 5_00) //
                            .addTo(client);

            var other = new AccountBuilder() //
                            .interest("2024-05-02", 10_00) //
                            .addTo(client);

            var collector = new BalanceCollector(client, account);
            var balances = collector.computeBalances();

            // 3 items: April, Today, May
            assertThat(balances.size(), is(3));

            Collections.sort(balances, (r, l) -> r.getDate().compareTo(l.getDate()));
            assertThat(balances.get(0).getValue(), is(Money.of(CurrencyUnit.EUR, Values.Money.factorize(100))));
            assertThat(balances.get(1).getValue(), is(Money.of(CurrencyUnit.EUR, Values.Money.factorize(110))));
            assertThat(balances.get(2).getValue(), is(Money.of(CurrencyUnit.EUR, Values.Money.factorize(105))));

            // no expectations, no proposals
            collector.updateProposals(balances, new HashMap<>());
            assertThat(balances.stream().map(b -> b.getProposals()).flatMap(l -> l.stream()).count(), is(0L));

            // check that the interest transactions are matched, but not the
            // fee transaction
            collector.updateProposals(balances, Map.of(today, Money.of(CurrencyUnit.EUR, Values.Money.factorize(120))));
            assertThat(balances.stream().map(b -> b.getProposals()).flatMap(l -> l.stream()).count(), is(2L));
            var proposals = balances.get(1).getProposals();
            assertThat(proposals.size(), is(2));

            assertThat(proposals.get(0).getCategory(), is(Messages.BalanceCheckFutureTransactionsWithMatchingValue));
            assertThat(proposals.get(0).getCandidates().size(), is(1));
            assertThat(proposals.get(0).getCandidates().get(0), is(account.getTransactions().get(2)));

            assertThat(proposals.get(1).getCategory(),
                            is(MessageFormat.format(Messages.BalanceCheckTransactionsOnOtherAccountWithMatchingValue,
                                            other.getName())));
            assertThat(proposals.get(1).getCandidates().size(), is(1));
            assertThat(proposals.get(1).getCandidates().get(0), is(other.getTransactions().get(0)));
        }
    }

    /**
     * Test if the balances include all months until today even if no further
     * transactions are recorded.
     */
    @Test
    public void testBalancesAreFilledUntilToday()
    {
        var today = LocalDate.of(2024, 5, 10);

        try (MockedStatic<LocalDate> mockedStatic = Mockito.mockStatic(LocalDate.class, invocation -> {
            if (invocation.getMethod().getName().equals("now"))
                return today;
            else
                return invocation.callRealMethod();
        }))
        {
            var client = new Client();

            var account = new AccountBuilder() //
                            .deposit_("2024-01-12", 100_00) //
                            .addTo(client);

            var collector = new BalanceCollector(client, account);
            var balances = collector.computeBalances();

            // 5 items: Jan, Feb, Mar, Apr, May
            assertThat(balances.size(), is(5));

            for (Balance balance : balances)
                assertThat(balance.getValue(), is(Money.of(CurrencyUnit.EUR, Values.Money.factorize(100))));
        }
    }

    @Test
    public void testEmptyAccount()
    {
        var client = new Client();

        var account = new AccountBuilder() //
                        .addTo(client);

        var collector = new BalanceCollector(client, account);
        var balances = collector.computeBalances();

        assertThat(balances.size(), is(1));
        assertThat(balances.get(0).getDate(), is(LocalDate.now()));
        assertThat(balances.get(0).getValue(), is(Money.of(CurrencyUnit.EUR, 0)));
    }

    @Test
    public void testIfTodayIsAlsoEndOfMonth()
    {
        var today = LocalDate.of(2024, 5, 31);

        try (MockedStatic<LocalDate> mockedStatic = Mockito.mockStatic(LocalDate.class, invocation -> {
            if (invocation.getMethod().getName().equals("now"))
                return today;
            else
                return invocation.callRealMethod();
        }))
        {
            var client = new Client();

            var account = new AccountBuilder() //
                            .deposit_("2024-05-01", 100_00) //
                            .addTo(client);

            var collector = new BalanceCollector(client, account);
            var balances = collector.computeBalances();

            assertThat(balances.size(), is(1));

            assertThat(balances.get(0).getDate(), is(today));
            assertThat(balances.get(0).getValue(), is(Money.of(CurrencyUnit.EUR, Values.Money.factorize(100))));
        }
    }
}
