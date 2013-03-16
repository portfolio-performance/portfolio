package name.abuchen.portfolio.checks.impl;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.checks.QuickFix;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.PortfolioTransferEntry;
import name.abuchen.portfolio.model.Values;

/* package */class MissingPortfolioTransferIssue extends AbstractPortfolioIssue
{
    private final class CreateTransferFix implements QuickFix
    {
        private Portfolio crossPortfolio;

        private CreateTransferFix(Portfolio crossPortfolio)
        {
            this.crossPortfolio = crossPortfolio;
        }

        @Override
        public String getLabel()
        {
            return MessageFormat.format(Messages.FixCreateTransfer, crossPortfolio.getName());
        }

        @Override
        public String getDoneLabel()
        {
            PortfolioTransaction.Type target = null;
            if (transaction.getType() == PortfolioTransaction.Type.TRANSFER_IN)
                target = PortfolioTransaction.Type.TRANSFER_OUT;
            else
                target = PortfolioTransaction.Type.TRANSFER_IN;

            return MessageFormat.format(Messages.FixCreateTransferDone, target.toString());
        }

        @Override
        public void execute()
        {
            Portfolio from = null;
            Portfolio to = null;

            if (transaction.getType() == PortfolioTransaction.Type.TRANSFER_IN)
            {
                from = crossPortfolio;
                to = portfolio;
            }
            else
            {
                from = portfolio;
                to = crossPortfolio;
            }

            PortfolioTransferEntry entry = new PortfolioTransferEntry(from, to);
            entry.setDate(transaction.getDate());
            entry.setSecurity(transaction.getSecurity());
            entry.setShares(transaction.getShares());
            entry.setAmount(transaction.getAmount());
            entry.insert();

            portfolio.getTransactions().remove(transaction);
        }
    }

    public MissingPortfolioTransferIssue(Client client, Portfolio portfolio, PortfolioTransaction transaction)
    {
        super(client, portfolio, transaction);
    }

    @Override
    public String getLabel()
    {
        return MessageFormat.format(Messages.IssueMissingPortfolioTransfer, //
                        transaction.getType().toString(), //
                        Values.Share.format(transaction.getShares()), //
                        Values.Amount.format(transaction.getActualPurchasePrice()), //
                        transaction.getSecurity().getName());
    }

    @Override
    public List<QuickFix> getAvailableFixes()
    {
        List<QuickFix> answer = new ArrayList<QuickFix>();

        for (Portfolio p : client.getPortfolios())
        {
            if (p.equals(portfolio))
                continue;
            answer.add(new CreateTransferFix(p));
        }
        answer.add(new DeleteTransactionFix(portfolio, transaction));

        return answer;
    }

}
