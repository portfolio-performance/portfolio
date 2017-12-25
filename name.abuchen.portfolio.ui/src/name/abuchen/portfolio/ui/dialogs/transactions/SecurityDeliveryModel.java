package name.abuchen.portfolio.ui.dialogs.transactions;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.PortfolioTransaction.Type;
import name.abuchen.portfolio.model.TransactionPair;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.ui.Messages;

public class SecurityDeliveryModel extends AbstractSecurityTransactionModel
{
    private TransactionPair<PortfolioTransaction> source;

    private CurrencyUnit transactionCurrency;

    public SecurityDeliveryModel(Client client, Type type)
    {
        super(client, type);

        if (!accepts(type))
            throw new IllegalArgumentException();

        this.transactionCurrency = CurrencyUnit.getInstance(client.getBaseCurrency());
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
        this.transactionCurrency = CurrencyUnit.getInstance(source.getTransaction().getCurrencyCode());
        fillFromTransaction(source.getTransaction());
    }
    
    @Override
    public boolean hasSource()
    {
        return source != null;
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
        transaction.setCurrencyCode(getTransactionCurrencyCode());
        transaction.setSecurity(security);
        transaction.setShares(shares);
        transaction.setAmount(total);
        transaction.setType(type);
        transaction.setNote(note);

        writeToTransaction(transaction);
    }

    @Override
    public void resetToNewTransaction()
    {
        this.source = null;
        super.resetToNewTransaction();
    }

    @Override
    public String getTransactionCurrencyCode()
    {
        return transactionCurrency.getCurrencyCode();
    }

    @Override
    public void setPortfolio(Portfolio portfolio)
    {
        setTransactionCurrency(CurrencyUnit.getInstance(portfolio.getReferenceAccount().getCurrencyCode()));
        super.setPortfolio(portfolio);
    }

    public CurrencyUnit getTransactionCurrency()
    {
        return transactionCurrency;
    }

    public void setTransactionCurrency(CurrencyUnit currency)
    {
        String oldCurrencyCode = getTransactionCurrencyCode();
        String oldExchangeRateCurrencies = getExchangeRateCurrencies();
        String oldInverseExchangeRateCurrencies = getInverseExchangeRateCurrencies();

        firePropertyChange(Properties.transactionCurrency.name(), transactionCurrency, transactionCurrency = currency);

        firePropertyChange(Properties.transactionCurrencyCode.name(), oldCurrencyCode, getTransactionCurrencyCode());
        firePropertyChange(Properties.exchangeRateCurrencies.name(), oldExchangeRateCurrencies,
                        getExchangeRateCurrencies());
        firePropertyChange(Properties.inverseExchangeRateCurrencies.name(), oldInverseExchangeRateCurrencies,
                        getInverseExchangeRateCurrencies());

        updateExchangeRate();
    }
}
