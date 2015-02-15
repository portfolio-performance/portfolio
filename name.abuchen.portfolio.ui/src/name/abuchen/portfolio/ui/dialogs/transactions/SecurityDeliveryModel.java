package name.abuchen.portfolio.ui.dialogs.transactions;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.ForexData;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.PortfolioTransaction.Type;
import name.abuchen.portfolio.model.TransactionPair;
import name.abuchen.portfolio.ui.Messages;

public class SecurityDeliveryModel extends AbstractSecurityTransactionModel
{
    private TransactionPair<PortfolioTransaction> source;

    public SecurityDeliveryModel(Client client, Type type)
    {
        super(client, type);

        if (!accepts(type))
            throw new IllegalArgumentException();
    }

    @Override
    public boolean accepts(Type type)
    {
        return type == PortfolioTransaction.Type.DELIVERY_INBOUND
                        || type == PortfolioTransaction.Type.DELIVERY_OUTBOUND;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void setSource(Object transaction)
    {
        this.source = (TransactionPair<PortfolioTransaction>) transaction;
        
        this.type = source.getTransaction().getType();
        this.portfolio = (Portfolio) source.getOwner();
        fillFromTransaction(source.getTransaction());
    }

    @Override
    public void applyChanges()
    {
        if (security == null)
            throw new UnsupportedOperationException(Messages.MsgMissingSecurity);
        if (portfolio.getReferenceAccount() == null)
            throw new UnsupportedOperationException(Messages.MsgMissingReferenceAccount);

        TransactionPair<PortfolioTransaction> entry;

        if (source != null && source.getOwner().equals(portfolio))
        {
            entry = source;
        }
        else
        {
            if (source != null)
            {
                source.getOwner().deleteTransaction(source.getTransaction(), client);
                source = null;
            }

            entry = new TransactionPair<>(portfolio, new PortfolioTransaction());
            portfolio.addTransaction(entry.getTransaction());
        }

        PortfolioTransaction transaction = entry.getTransaction();

        transaction.setDate(date);
        transaction.setSecurity(security);
        transaction.setShares(shares);
        transaction.setFees(fees);
        transaction.setTaxes(taxes);
        transaction.setAmount(total);
        transaction.setType(type);
        transaction.setNote(note);
        transaction.setCurrencyCode(getAccountCurrencyCode());

        if (getAccountCurrencyCode().equals(getSecurityCurrencyCode()))
        {
            transaction.setForex(null);
        }
        else
        {
            ForexData forex = new ForexData();
            forex.setBaseCurrency(getSecurityCurrencyCode());
            forex.setTermCurrency(getAccountCurrencyCode());
            forex.setExchangeRate(getExchangeRate());
            forex.setBaseAmount(lumpSum);
            transaction.setForex(forex);
        }
    }
}
