package name.abuchen.portfolio.checks.impl;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.checks.QuickFix;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;

/* package */class MissingBuySellPortfolioIssue extends AbstractAccountIssue
{
    private final class CreateBuySellEntryFix implements QuickFix
    {
        private final Portfolio portfolio;

        private CreateBuySellEntryFix(Portfolio portfolio)
        {
            this.portfolio = portfolio;
        }

        @Override
        public String getLabel()
        {
            return MessageFormat.format(Messages.FixCreateCrossEntryPortfolio, portfolio.getName());
        }

        @Override
        public String getDoneLabel()
        {
            return MessageFormat.format(Messages.FixCreateCrossEntryDone, transaction.getType().toString());
        }

        @Override
        public void execute()
        {
            BuySellEntry entry = new BuySellEntry(portfolio, account);
            entry.setDate(transaction.getDate());
            entry.setType(PortfolioTransaction.Type.valueOf(transaction.getType().name()));
            entry.setSecurity(transaction.getSecurity());
            entry.setShares(1);
            entry.setFees(0);
            entry.setAmount(transaction.getAmount());
            entry.insert();

            account.getTransactions().remove(transaction);
        }
    }

    public MissingBuySellPortfolioIssue(Client client, Account account, AccountTransaction transaction)
    {
        super(client, account, transaction);
    }

    @Override
    public String getLabel()
    {
        return MessageFormat.format(Messages.IssueMissingBuySellInPortfolio, transaction.getType().toString(),
                        transaction.getSecurity().getName());
    }

    @Override
    public List<QuickFix> getAvailableFixes()
    {
        List<QuickFix> answer = new ArrayList<QuickFix>();

        answer.add(new DeleteTransactionFix(account, transaction));

        for (final Portfolio portfolio : client.getPortfolios())
            answer.add(new CreateBuySellEntryFix(portfolio));

        return answer;
    }
}
