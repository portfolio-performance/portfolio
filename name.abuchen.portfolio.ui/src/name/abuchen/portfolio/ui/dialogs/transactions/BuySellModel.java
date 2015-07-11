package name.abuchen.portfolio.ui.dialogs.transactions;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.model.TransactionOwner;
import name.abuchen.portfolio.ui.Messages;

/* package */class BuySellModel extends AbstractSecurityTransactionModel
{
    private BuySellEntry source;

    public BuySellModel(Client client, PortfolioTransaction.Type type)
    {
        super(client, type);

        if (!accepts(type))
            throw new IllegalArgumentException();
    }

    @Override
    public boolean accepts(PortfolioTransaction.Type type)
    {
        return type == PortfolioTransaction.Type.BUY || type == PortfolioTransaction.Type.SELL;
    }

    public void setSource(Object entry)
    {
        this.source = (BuySellEntry) entry;

        this.type = source.getPortfolioTransaction().getType();
        this.portfolio = (Portfolio) source.getOwner(source.getPortfolioTransaction());
        this.account = (Account) source.getOwner(source.getAccountTransaction());
        fillFromTransaction(source.getPortfolioTransaction());
    }

    public void applyChanges()
    {
        if (security == null)
            throw new UnsupportedOperationException(Messages.MsgMissingSecurity);
        if (account == null)
            throw new UnsupportedOperationException(Messages.MsgMissingReferenceAccount);

        BuySellEntry entry;

        if (source != null && source.getOwner(source.getPortfolioTransaction()).equals(portfolio)
                        && source.getOwner(source.getAccountTransaction()).equals(account))
        {
            entry = source;
        }
        else
        {
            if (source != null)
            {
                @SuppressWarnings("unchecked")
                TransactionOwner<Transaction> owner = (TransactionOwner<Transaction>) source.getOwner(source
                                .getPortfolioTransaction());
                owner.deleteTransaction(source.getPortfolioTransaction(), client);
                source = null;
            }

            entry = new BuySellEntry(portfolio, account);
            entry.insert();
        }

        entry.setDate(date);
        entry.setCurrencyCode(account.getCurrencyCode());
        entry.setSecurity(security);
        entry.setShares(shares);
        entry.setAmount(total);
        entry.setType(type);
        entry.setNote(note);

        writeToTransaction(entry.getPortfolioTransaction());
    }
}
