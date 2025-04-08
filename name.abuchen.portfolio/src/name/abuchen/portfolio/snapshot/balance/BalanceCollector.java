package name.abuchen.portfolio.snapshot.balance;

import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.MutableMoney;
import name.abuchen.portfolio.snapshot.balance.Balance.Proposal;

public class BalanceCollector
{
    private final Client client;
    private final Account account;

    public BalanceCollector(Client client, Account account)
    {
        this.client = client;
        this.account = account;
    }

    public List<Balance> computeBalances()
    {
        var transactions = account.getTransactions();

        var today = LocalDate.now();
        var todaysBalance = new Balance(today, Money.of(account.getCurrencyCode(), 0));

        var balances = new ArrayList<Balance>();

        if (transactions.isEmpty())
        {
            balances.add(todaysBalance);
            return balances;
        }

        List<AccountTransaction> tx = new ArrayList<>(transactions);
        Collections.sort(tx, Transaction.BY_DATE);

        // determine first month end
        var month = tx.get(0).getDateTime().toLocalDate().with(TemporalAdjusters.lastDayOfMonth());
        if (month.isAfter(today))
            month = today;

        MutableMoney balance = MutableMoney.of(account.getCurrencyCode());
        for (AccountTransaction t : tx)
        {
            var txDate = t.getDateTime().toLocalDate();

            while (txDate.isAfter(month))
            {
                balances.add(new Balance(month, balance.toMoney()));
                month = month.plusDays(1).with(TemporalAdjusters.lastDayOfMonth());
            }

            if (t.getType().isCredit())
            {
                balance.add(t.getMonetaryAmount());
            }
            else
            {
                balance.subtract(t.getMonetaryAmount());
            }

            if (!txDate.isAfter(today))
                todaysBalance = new Balance(today, balance.toMoney());
        }

        // add final balance only if it is not today and the balances differs
        // from today's balance (i.e. there are future transactions in the same
        // month)

        if (!today.equals(month) && !todaysBalance.getValue().equals(balance.toMoney()))
        {
            balances.add(new Balance(month, balance.toMoney()));
            month = month.plusDays(1).with(TemporalAdjusters.lastDayOfMonth());
        }

        while (month.isBefore(today))
        {
            balances.add(new Balance(month, balance.toMoney()));
            month = month.plusDays(1).with(TemporalAdjusters.lastDayOfMonth());
        }

        balances.add(todaysBalance);

        return balances;
    }

    public void updateProposals(List<Balance> balances, Map<LocalDate, Money> expectedBalances)
    {
        for (Balance balance : balances)
        {
            balance.getProposals().clear();

            var expected = expectedBalances.get(balance.getDate());
            if (expected == null || expected.equals(balance.getValue()))
                continue;

            var needsCredit = expected.isGreaterThan(balance.getValue());
            var delta = expected.subtract(balance.getValue()).absolute();

            checkFutureTransactions(balance, delta, needsCredit);
            checkOtherAccountTransactions(balance, delta, needsCredit);
        }
    }

    private void checkFutureTransactions(Balance balance, Money delta, boolean needsCredit)
    {
        var txs = account.getTransactions().stream().filter(filter(balance.getDate(), delta, needsCredit))
                        .map(t -> (Transaction) t).toList();

        if (!txs.isEmpty())
        {
            balance.addProposal(new Proposal(Messages.BalanceCheckFutureTransactionsWithMatchingValue, txs));
        }
    }

    private void checkOtherAccountTransactions(Balance balance, Money delta, boolean needsCredit)
    {
        // adapt the period to include the current month
        var date = balance.getDate().withDayOfMonth(1).minusMonths(1);

        for (Account other : client.getAccounts())
        {
            if (other.equals(account))
                continue;

            var txs = other.getTransactions().stream().filter(filter(date, delta, needsCredit))
                            .map(t -> (Transaction) t).toList();

            if (!txs.isEmpty())
            {
                balance.addProposal(new Proposal(
                                MessageFormat.format(Messages.BalanceCheckTransactionsOnOtherAccountWithMatchingValue,
                                                other.getName()),
                                txs));
            }
        }
    }

    private Predicate<AccountTransaction> filter(LocalDate date, Money delta, boolean needsCredit)
    {
        return tx -> {
            // continue if type does not match expected credit/debit type
            if (!(tx.getType().isDebit() ^ needsCredit))
                return false;

            if (tx.getDateTime().toLocalDate().isBefore(date))
                return false;

            return tx.getMonetaryAmount().equals(delta);
        };
    }
}
