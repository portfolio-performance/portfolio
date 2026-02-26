package name.abuchen.portfolio.checks.impl;

import java.util.ArrayList;
import java.util.List;

import name.abuchen.portfolio.checks.Check;
import name.abuchen.portfolio.checks.Issue;
import name.abuchen.portfolio.model.Client;

public class OnlyAccountTransactionsWithSecurityCanHaveExDateCheck implements Check
{
    @Override
    public List<Issue> execute(Client client)
    {
        var answer = new ArrayList<Issue>();

        for (var account : client.getAccounts())
        {
            for (var tx : account.getTransactions())
            {
                if (tx.getExDate() != null && tx.getSecurity() == null)
                {
                    tx.setExDate(null);
                }
            }
        }

        return answer;
    }
}
