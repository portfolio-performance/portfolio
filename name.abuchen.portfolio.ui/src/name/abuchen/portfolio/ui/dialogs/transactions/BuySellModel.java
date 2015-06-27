package name.abuchen.portfolio.ui.dialogs.transactions;

import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.model.TransactionOwner;
import name.abuchen.portfolio.money.Money;
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
        fillFromTransaction(source.getPortfolioTransaction());
    }

    public void applyChanges()
    {
        if (security == null)
            throw new UnsupportedOperationException(Messages.MsgMissingSecurity);
        if (portfolio.getReferenceAccount() == null)
            throw new UnsupportedOperationException(Messages.MsgMissingReferenceAccount);

        BuySellEntry entry;

        if (source != null && source.getOwner(source.getPortfolioTransaction()).equals(portfolio))
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

            entry = new BuySellEntry(portfolio, portfolio.getReferenceAccount());
            entry.insert();
        }

        entry.setDate(date);
        entry.setCurrencyCode(getAccountCurrencyCode());
        entry.setSecurity(security);
        entry.setShares(shares);
        entry.setAmount(total);
        entry.setType(type);
        entry.setNote(note);

        PortfolioTransaction transaction = entry.getPortfolioTransaction();
        transaction.clearUnits();

        if (fees != 0)
            transaction.addUnit(new Transaction.Unit(Transaction.Unit.Type.FEE, //
                            Money.of(getAccountCurrencyCode(), fees)));

        if (taxes != 0)
            transaction.addUnit(new Transaction.Unit(Transaction.Unit.Type.TAX, //
                            Money.of(getAccountCurrencyCode(), taxes)));

        if (!getAccountCurrencyCode().equals(getSecurityCurrencyCode()))
        {
            Transaction.Unit forex = new Transaction.Unit(Transaction.Unit.Type.LUMPSUM, //
                            Money.of(getAccountCurrencyCode(), convertedLumpSum), //
                            Money.of(getSecurityCurrencyCode(), lumpSum), //
                            getExchangeRate());
            transaction.addUnit(forex);
        }
    }
}
