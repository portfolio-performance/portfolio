package name.abuchen.portfolio.checks.impl;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.checks.Check;
import name.abuchen.portfolio.checks.Issue;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.CrossEntry;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;

/**
 * Detect dangling accounts which are not in the account list of the client but
 * still referenced as account from a portfolio or portfolio transaction. Due to
 * a bug in previous versions, when deleting an account from the account list,
 * it was not removed as reference account.
 */
public class DanglingAccountsCheck implements Check
{

    @Override
    public List<Issue> execute(Client client)
    {
        Set<Account> accounts = new HashSet<Account>(client.getAccounts());

        for (Portfolio portfolio : client.getPortfolios())
        {
            Account referenceAccount = portfolio.getReferenceAccount();
            check(client, accounts, referenceAccount);

            for (PortfolioTransaction transaction : portfolio.getTransactions())
            {
                CrossEntry entry = transaction.getCrossEntry();
                if (!(entry instanceof BuySellEntry))
                    continue;

                Account account = (Account) entry.getCrossOwner(transaction);
                check(client, accounts, account);
            }
        }

        return Collections.emptyList();
    }

    private void check(Client client, Set<Account> accounts, Account account)
    {
        if (!accounts.contains(account))
        {
            account.setName(MessageFormat.format(Messages.LabelNameReferenceAccountRecovered, account.getName()));
            accounts.add(account);
            client.addAccount(account);
        }
    }

}
