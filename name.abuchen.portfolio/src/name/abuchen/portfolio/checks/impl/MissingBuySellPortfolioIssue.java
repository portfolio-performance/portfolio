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
import name.abuchen.portfolio.money.Values;

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
            entry.setDate(transaction.getDateTime());
            entry.setType(PortfolioTransaction.Type.valueOf(transaction.getType().name()));
            entry.setSecurity(transaction.getSecurity());
            entry.setShares(Values.Share.factor());
            entry.setAmount(transaction.getAmount());
            entry.setCurrencyCode(transaction.getCurrencyCode());
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
        answer.add(new DeleteTransactionFix<AccountTransaction>(client, account, transaction));
        client.getPortfolios().stream().forEach(p -> answer.add(new CreateBuySellEntryFix(p)));
        return answer;
    }
}
