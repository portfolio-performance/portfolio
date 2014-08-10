package name.abuchen.portfolio.checks.impl;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.checks.QuickFix;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Values;

/* package */class MissingBuySellAccountIssue extends AbstractPortfolioIssue
{
    private final class ConvertToDeliveryFix implements QuickFix
    {
        PortfolioTransaction.Type target;

        public ConvertToDeliveryFix()
        {
            if (transaction.getType() == PortfolioTransaction.Type.BUY)
                target = PortfolioTransaction.Type.DELIVERY_INBOUND;
            else if (transaction.getType() == PortfolioTransaction.Type.SELL)
                target = PortfolioTransaction.Type.DELIVERY_OUTBOUND;
            else
                throw new UnsupportedOperationException();
        }

        @Override
        public String getLabel()
        {
            return MessageFormat.format(Messages.FixConvertToDelivery, target.toString());
        }

        @Override
        public String getDoneLabel()
        {
            return MessageFormat.format(Messages.FixConvertToDeliveryDone, target.toString());
        }

        @Override
        public void execute()
        {
            PortfolioTransaction t = new PortfolioTransaction();
            t.setType(target);
            t.setDate(transaction.getDate());
            t.setSecurity(transaction.getSecurity());
            t.setShares(transaction.getShares());
            t.setFees(transaction.getFees());
            t.setTaxes(transaction.getTaxes());
            t.setAmount(transaction.getAmount());
            portfolio.addTransaction(t);

            portfolio.getTransactions().remove(transaction);
        }
    }

    private final class CreateBuySellEntryFix implements QuickFix
    {
        private Account account;

        private CreateBuySellEntryFix(Account account)
        {
            this.account = account;
        }

        @Override
        public String getLabel()
        {
            return MessageFormat.format(Messages.FixCreateCrossEntryAccount, account.getName());
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
            entry.setType(transaction.getType());
            entry.setSecurity(transaction.getSecurity());
            entry.setShares(transaction.getShares());
            entry.setFees(transaction.getFees());
            entry.setTaxes(transaction.getTaxes());
            entry.setAmount(transaction.getAmount());
            entry.insert();

            portfolio.getTransactions().remove(transaction);
        }
    }

    public MissingBuySellAccountIssue(Client client, Portfolio portfolio, PortfolioTransaction transaction)
    {
        super(client, portfolio, transaction);
    }

    @Override
    public String getLabel()
    {
        return MessageFormat.format(Messages.IssueMissingBuySellInAccount, //
                        transaction.getType().toString(), //
                        Values.Share.format(transaction.getShares()), //
                        Values.Amount.format(transaction.getActualPurchasePrice()), //
                        transaction.getSecurity().getName());
    }

    @Override
    public List<QuickFix> getAvailableFixes()
    {
        List<QuickFix> answer = new ArrayList<QuickFix>();

        answer.add(new ConvertToDeliveryFix());

        if (portfolio.getReferenceAccount() != null)
            answer.add(new CreateBuySellEntryFix(portfolio.getReferenceAccount()));

        for (final Account account : client.getAccounts())
        {
            if (account.equals(portfolio.getReferenceAccount()))
                continue;
            answer.add(new CreateBuySellEntryFix(account));
        }

        answer.add(new DeleteTransactionFix(portfolio, transaction));

        return answer;
    }
}
