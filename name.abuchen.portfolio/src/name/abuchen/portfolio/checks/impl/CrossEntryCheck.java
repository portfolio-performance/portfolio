package name.abuchen.portfolio.checks.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import name.abuchen.portfolio.checks.Check;
import name.abuchen.portfolio.checks.Issue;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.AccountTransferEntry;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.PortfolioTransferEntry;

public class CrossEntryCheck implements Check
{

    @Override
    public List<Issue> execute(Client client)
    {
        return new CheckImpl(client).execute();
    }

    private static class AccountEntry
    {
        Account account;
        AccountTransaction transaction;

        private AccountEntry(Account owner, AccountTransaction transaction)
        {
            this.account = owner;
            this.transaction = transaction;
        }
    }

    private static class PortfolioEntry
    {
        Portfolio portfolio;
        PortfolioTransaction transaction;

        private PortfolioEntry(Portfolio owner, PortfolioTransaction transaction)
        {
            this.portfolio = owner;
            this.transaction = transaction;
        }
    }

    private static class CheckImpl
    {
        private Client client;

        private List<AccountEntry> accountTransactions = new ArrayList<AccountEntry>();
        private List<PortfolioEntry> portfolioTransactions = new ArrayList<PortfolioEntry>();

        private List<Issue> issues = new ArrayList<Issue>();

        public CheckImpl(Client client)
        {
            this.client = client;
        }

        public List<Issue> execute()
        {
            collectAccountTransactions();
            collectPortfolioTransactions();

            matchBuySell();
            matchAccountTransfers();
            matchPortfolioTransfers();

            return issues;
        }

        private void collectAccountTransactions()
        {
            for (Account account : client.getAccounts())
            {
                for (AccountTransaction t : account.getTransactions())
                {
                    if (t.getCrossEntry() != null)
                        continue;

                    switch (t.getType())
                    {
                        case BUY:
                        case SELL:
                        case TRANSFER_IN:
                        case TRANSFER_OUT:
                            accountTransactions.add(new AccountEntry(account, t));
                            break;
                        default:
                            break;
                    }
                }
            }
        }

        private void collectPortfolioTransactions()
        {
            for (Portfolio portfolio : client.getPortfolios())
            {
                for (PortfolioTransaction t : portfolio.getTransactions())
                {
                    if (t.getCrossEntry() != null)
                        continue;

                    switch (t.getType())
                    {
                        case BUY:
                        case SELL:
                        case TRANSFER_IN:
                        case TRANSFER_OUT:
                            portfolioTransactions.add(new PortfolioEntry(portfolio, t));
                            break;
                        default:
                            break;
                    }
                }
            }
        }

        private void matchBuySell()
        {
            Iterator<AccountEntry> iterAccount = accountTransactions.iterator();
            while (iterAccount.hasNext())
            {
                AccountEntry suspect = iterAccount.next();

                if (suspect.transaction.getType() != AccountTransaction.Type.BUY
                                && suspect.transaction.getType() != AccountTransaction.Type.SELL)
                    continue;

                if (suspect.transaction.getSecurity() == null)
                {
                    issues.add(new BuySellMissingSecurityIssue(client, suspect.account, suspect.transaction));
                    iterAccount.remove();
                    continue;
                }

                PortfolioTransaction.Type neededType = PortfolioTransaction.Type.valueOf(suspect.transaction.getType()
                                .name());

                PortfolioEntry match = null;
                for (PortfolioEntry candidate : portfolioTransactions)
                {
                    if (candidate.transaction.getType() != neededType)
                        continue;

                    if (!candidate.transaction.getDateTime().equals(suspect.transaction.getDateTime()))
                        continue;

                    if (candidate.transaction.getSecurity() != suspect.transaction.getSecurity())
                        continue;

                    if (candidate.transaction.getAmount() != suspect.transaction.getAmount())
                        continue;

                    match = candidate;
                    break;
                }

                if (match == null)
                {
                    issues.add(new MissingBuySellPortfolioIssue(client, suspect.account, suspect.transaction));
                    iterAccount.remove();
                }
                else
                {
                    BuySellEntry entry = new BuySellEntry(match.portfolio, suspect.account);
                    entry.setCurrencyCode(match.transaction.getCurrencyCode());
                    entry.setType(match.transaction.getType());
                    entry.setDate(match.transaction.getDateTime());
                    entry.setSecurity(match.transaction.getSecurity());
                    entry.setShares(match.transaction.getShares());
                    entry.setAmount(match.transaction.getAmount());
                    entry.getPortfolioTransaction().addUnits(match.transaction.getUnits());
                    entry.insert();

                    match.portfolio.getTransactions().remove(match.transaction);
                    suspect.account.getTransactions().remove(suspect.transaction);

                    portfolioTransactions.remove(match);
                    iterAccount.remove();
                }
            }

            // create issues for any unmatched portfolio transaction
            Iterator<PortfolioEntry> iterPorfolio = portfolioTransactions.iterator();
            while (iterPorfolio.hasNext())
            {
                PortfolioEntry t = iterPorfolio.next();

                if (t.transaction.getType() != PortfolioTransaction.Type.BUY
                                && t.transaction.getType() != PortfolioTransaction.Type.SELL)
                    continue;

                issues.add(new MissingBuySellAccountIssue(client, t.portfolio, t.transaction));
                iterPorfolio.remove();
            }
        }

        private void matchAccountTransfers()
        {
            Set<AccountEntry> matched = new HashSet<AccountEntry>();

            for (AccountEntry suspect : accountTransactions)
            {
                if (matched.contains(suspect))
                    continue;

                AccountTransaction.Type neededType = null;
                if (suspect.transaction.getType() == AccountTransaction.Type.TRANSFER_IN)
                    neededType = AccountTransaction.Type.TRANSFER_OUT;
                else if (suspect.transaction.getType() == AccountTransaction.Type.TRANSFER_OUT)
                    neededType = AccountTransaction.Type.TRANSFER_IN;

                if (neededType == null)
                    continue;

                AccountEntry match = null;
                for (AccountEntry candidate : accountTransactions)
                {
                    if (matched.contains(candidate))
                        continue;

                    if (candidate.account.equals(suspect.account))
                        continue;

                    if (candidate.transaction.getType() != neededType)
                        continue;

                    if (!candidate.transaction.getDateTime().equals(suspect.transaction.getDateTime()))
                        continue;

                    if (candidate.transaction.getAmount() != suspect.transaction.getAmount())
                        continue;

                    match = candidate;
                    break;
                }

                if (match == null)
                {
                    matched.add(suspect);
                    issues.add(new MissingAccountTransferIssue(client, suspect.account, suspect.transaction));
                }
                else
                {
                    AccountTransferEntry crossentry = null;

                    if (suspect.transaction.getType() == AccountTransaction.Type.TRANSFER_IN)
                        crossentry = new AccountTransferEntry(match.account, suspect.account);
                    else
                        crossentry = new AccountTransferEntry(suspect.account, match.account);

                    crossentry.setDate(match.transaction.getDateTime());
                    crossentry.setAmount(match.transaction.getAmount());
                    crossentry.setCurrencyCode(match.transaction.getCurrencyCode());
                    crossentry.insert();

                    suspect.account.getTransactions().remove(suspect.transaction);
                    match.account.getTransactions().remove(match.transaction);

                    matched.add(suspect);
                    matched.add(match);
                }
            }

            accountTransactions.removeAll(matched);
        }

        private void matchPortfolioTransfers()
        {
            Set<PortfolioEntry> matched = new HashSet<PortfolioEntry>();

            for (PortfolioEntry suspect : portfolioTransactions)
            {
                if (matched.contains(suspect))
                    continue;

                PortfolioTransaction.Type neededType = null;
                if (suspect.transaction.getType() == PortfolioTransaction.Type.TRANSFER_IN)
                    neededType = PortfolioTransaction.Type.TRANSFER_OUT;
                else if (suspect.transaction.getType() == PortfolioTransaction.Type.TRANSFER_OUT)
                    neededType = PortfolioTransaction.Type.TRANSFER_IN;

                if (neededType == null)
                    continue;

                PortfolioEntry match = null;
                for (PortfolioEntry possibleMatch : portfolioTransactions)
                {
                    if (matched.contains(possibleMatch))
                        continue;

                    if (possibleMatch.portfolio.equals(suspect.portfolio))
                        continue;

                    if (possibleMatch.transaction.getType() != neededType)
                        continue;

                    if (!possibleMatch.transaction.getDateTime().equals(suspect.transaction.getDateTime()))
                        continue;

                    if (!possibleMatch.transaction.getSecurity().equals(suspect.transaction.getSecurity()))
                        continue;

                    if (possibleMatch.transaction.getShares() != suspect.transaction.getShares())
                        continue;

                    if (possibleMatch.transaction.getAmount() != suspect.transaction.getAmount())
                        continue;

                    match = possibleMatch;
                    break;
                }

                if (match == null)
                {
                    matched.add(suspect);
                    issues.add(new MissingPortfolioTransferIssue(client, suspect.portfolio, suspect.transaction));
                }
                else
                {
                    PortfolioTransferEntry crossentry = null;

                    if (suspect.transaction.getType() == PortfolioTransaction.Type.TRANSFER_IN)
                        crossentry = new PortfolioTransferEntry(match.portfolio, suspect.portfolio);
                    else
                        crossentry = new PortfolioTransferEntry(suspect.portfolio, match.portfolio);

                    crossentry.setDate(match.transaction.getDateTime());
                    crossentry.setSecurity(match.transaction.getSecurity());
                    crossentry.setShares(match.transaction.getShares());
                    crossentry.setAmount(match.transaction.getAmount());
                    crossentry.setCurrencyCode(match.transaction.getCurrencyCode());
                    crossentry.insert();

                    suspect.portfolio.getTransactions().remove(suspect.transaction);
                    match.portfolio.getTransactions().remove(match.transaction);

                    matched.add(suspect);
                    matched.add(match);
                }
            }

            portfolioTransactions.removeAll(matched);
        }
    }
}
