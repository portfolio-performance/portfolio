package name.abuchen.portfolio.checks.impl;

import java.text.MessageFormat;
import java.util.List;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.checks.QuickFix;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.money.Values;

/* package */class MissingPortfolioTransferIssue extends AbstractPortfolioIssue
{
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
                        Values.Quote.format(transaction.getGrossPricePerShare()), //
                        transaction.getSecurity().getName());
    }

    @Override
    public List<QuickFix> getAvailableFixes()
    {
        return List.of(new DeleteTransactionFix<PortfolioTransaction>(client, portfolio, transaction));
    }

}
