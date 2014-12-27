package name.abuchen.portfolio.ui.dialogs.transactions;

import java.util.Date;
import java.util.List;

import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.PortfolioTransaction.Type;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.model.TransactionOwner;
import name.abuchen.portfolio.model.Values;
import name.abuchen.portfolio.snapshot.ClientSnapshot;
import name.abuchen.portfolio.snapshot.PortfolioSnapshot;
import name.abuchen.portfolio.snapshot.SecurityPosition;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.util.Dates;

import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;

/* package */class BuySellModel extends AbstractModel
{
    public enum Properties
    {
        portfolio, security, accountName, date, shares, quote, lumpSum, exchangeRate, convertedLumpSum, //
        fees, taxes, total, exchangeRateCurrencies, accountCurrencyCode, securityCurrencyCode, calculationStatus;
    }

    private final Client client;
    private final PortfolioTransaction.Type type;

    private BuySellEntry source;

    private Portfolio portfolio;
    private Security security;
    private Date date = Dates.today();
    private long shares;
    private long quote;
    private long lumpSum;
    private long exchangeRate = 1 * Values.ExchangeRate.factor();
    private long convertedLumpSum;
    private long fees;
    private long taxes;
    private long total;

    private IStatus calculationStatus = ValidationStatus.ok();

    public BuySellModel(Client client, Portfolio portfolio, Security security, Type type)
    {
        this.client = client;
        this.portfolio = portfolio;
        this.security = security;
        this.type = type;

        if (type == PortfolioTransaction.Type.SELL && security != null)
        {
            ClientSnapshot snapshot = ClientSnapshot.create(client, Dates.today());
            for (PortfolioSnapshot p : snapshot.getPortfolios())
            {
                SecurityPosition position = p.getPositionsBySecurity().get(security);
                if (position != null && (portfolio == null || p.getSource().equals(portfolio)))
                {
                    setPortfolio(p.getSource());
                    setShares(position.getShares());
                    setTotal(position.calculateValue());
                    break;
                }
            }
        }
        else
        {
            // set portfolio only if exactly one exists
            // (otherwise force user to choose)
            List<Portfolio> activePortfolios = client.getActivePortfolios();
            if (portfolio == null && activePortfolios.size() == 1)
                setPortfolio(activePortfolios.get(0));

            if (security == null)
            {
                List<Security> activeSecurities = client.getActiveSecurities();
                if (!activeSecurities.isEmpty())
                    setSecurity(activeSecurities.get(0));
            }
            else
            {
                setQuote(security.getSecurityPrice(date).getValue());
            }
        }
    }

    public BuySellModel(Client client, BuySellEntry entry)
    {
        this.client = client;
        this.type = entry.getPortfolioTransaction().getType();

        this.source = entry;

        this.portfolio = (Portfolio) entry.getEntity(entry.getPortfolioTransaction());
        this.security = entry.getPortfolioTransaction().getSecurity();

        this.date = entry.getPortfolioTransaction().getDate();

        this.shares = entry.getPortfolioTransaction().getShares();
        this.total = entry.getPortfolioTransaction().getAmount();
        this.taxes = entry.getPortfolioTransaction().getTaxes();
        this.fees = entry.getPortfolioTransaction().getFees();

        this.exchangeRate = 1 * Values.ExchangeRate.factor();
        this.convertedLumpSum = calcLumpSum(total, fees, taxes);
        this.lumpSum = convertedLumpSum;

        this.quote = entry.getPortfolioTransaction().getActualPurchasePrice();
    }

    public void applyChanges()
    {
        if (security == null)
            throw new UnsupportedOperationException(Messages.MsgMissingSecurity);
        if (portfolio.getReferenceAccount() == null)
            throw new UnsupportedOperationException(Messages.MsgMissingReferenceAccount);

        BuySellEntry entry;

        if (source != null && source.getEntity(source.getPortfolioTransaction()).equals(portfolio))
        {
            entry = source;
        }
        else
        {
            if (source != null)
            {
                @SuppressWarnings("unchecked")
                TransactionOwner<Transaction> owner = (TransactionOwner<Transaction>) source.getEntity(source
                                .getPortfolioTransaction());
                owner.deleteTransaction(source.getPortfolioTransaction(), client);
                source = null;
            }

            entry = new BuySellEntry(portfolio, portfolio.getReferenceAccount());
            entry.insert();
        }

        entry.setDate(date);
        entry.setSecurity(security);
        entry.setShares(shares);
        entry.setFees(fees);
        entry.setTaxes(taxes);
        entry.setAmount(total);
        entry.setType(type);

        // FIXME store (and load) forex information in transaction
    }

    public IStatus getCalculationStatus()
    {
        return calculationStatus;
    }

    /**
     * Check whether calculation works out. The separate validation is needed
     * because the model does prevent negative values in methods
     * {@link #calcLumpSum(long, long, long)} and
     * {@link #calcTotal(long, long, long)}.
     */
    private IStatus calculateStatus()
    {
        long ls = Math.round(shares * quote * Values.Amount.factor()
                        / (Values.Share.divider() * Values.Quote.divider()));
        if (ls != lumpSum)
            return ValidationStatus.error(Messages.MsgIncorrectSubTotal);

        long cls = Math.round(ls * exchangeRate / Values.ExchangeRate.divider());
        if (cls != convertedLumpSum)
            return ValidationStatus.error(Messages.MsgIncorrectConvertedSubTotal);

        long t = calcTotal(cls, fees, taxes);
        if (t != total)
            return ValidationStatus.error(Messages.MsgIncorrectTotal);

        return ValidationStatus.ok();
    }

    public Portfolio getPortfolio()
    {
        return portfolio;
    }

    public void setPortfolio(Portfolio portfolio)
    {
        String oldCurrencyCode = getAccountCurrencyCode();
        String oldAccountName = getAccountName();
        String oldExchangeRateCurrencies = getExchangeRateCurrencies();
        firePropertyChange(Properties.portfolio.name(), this.portfolio, this.portfolio = portfolio);
        firePropertyChange(Properties.accountCurrencyCode.name(), oldCurrencyCode, getAccountCurrencyCode());
        firePropertyChange(Properties.accountName.name(), oldAccountName, getAccountName());
        firePropertyChange(Properties.exchangeRateCurrencies.name(), oldExchangeRateCurrencies,
                        getExchangeRateCurrencies());

        updateSharesAndQuote();
    }

    public Security getSecurity()
    {
        return security;
    }

    public void setSecurity(Security security)
    {
        String oldCurrencyCode = getSecurityCurrencyCode();
        String oldExchangeRateCurrencies = getExchangeRateCurrencies();
        firePropertyChange(Properties.security.name(), this.security, this.security = security);
        firePropertyChange(Properties.securityCurrencyCode.name(), oldCurrencyCode, getSecurityCurrencyCode());
        firePropertyChange(Properties.exchangeRateCurrencies.name(), oldExchangeRateCurrencies,
                        getExchangeRateCurrencies());

        updateSharesAndQuote();
    }

    private void updateSharesAndQuote()
    {
        if (type == PortfolioTransaction.Type.SELL)
        {
            PortfolioSnapshot snapshot = PortfolioSnapshot.create(portfolio, date);
            SecurityPosition position = snapshot.getPositionsBySecurity().get(security);
            if (position != null)
            {
                setShares(position.getShares());
                setTotal(position.calculateValue());
            }
            else
            {
                setShares(0);
                setQuote(security.getSecurityPrice(date).getValue());
            }
        }
        else
        {
            setQuote(security.getSecurityPrice(date).getValue());
        }

        if (getAccountCurrencyCode().equals(getSecurityCurrencyCode()))
        {
            setExchangeRate(1 * Values.ExchangeRate.factor());
        }
        else
        {
            // FIXME set current exchange rate
        }

    }

    public Date getDate()
    {
        return date;
    }

    public void setDate(Date date)
    {
        firePropertyChange(Properties.date.name(), this.date, this.date = date);
    }

    public long getShares()
    {
        return shares;
    }

    public void setShares(long shares)
    {
        firePropertyChange(Properties.shares.name(), this.shares, this.shares = shares);

        if (quote != 0)
        {
            setLumpSum(Math.round(shares * quote * Values.Amount.factor()
                            / (Values.Share.divider() * Values.Quote.divider())));
        }
        else if (lumpSum != 0 && shares != 0)
        {
            setQuote(Math.round(lumpSum * Values.Share.factor() / shares));
        }

        firePropertyChange(Properties.calculationStatus.name(), this.calculationStatus,
                        this.calculationStatus = calculateStatus());
    }

    public long getQuote()
    {
        return quote;
    }

    public void setQuote(long quote)
    {
        firePropertyChange(Properties.quote.name(), this.quote, this.quote = quote);

        triggerLumpSum(Math.round(shares * quote * Values.Amount.factor()
                        / (Values.Share.divider() * Values.Quote.divider())));

        firePropertyChange(Properties.calculationStatus.name(), this.calculationStatus,
                        this.calculationStatus = calculateStatus());
    }

    public long getLumpSum()
    {
        return lumpSum;
    }

    public void setLumpSum(long lumpSum)
    {
        triggerLumpSum(lumpSum);

        if (shares != 0)
        {
            long newQuote = Math.round(lumpSum * Values.Share.factor() * Values.Quote.factor()
                            / (shares * Values.Quote.divider()));
            firePropertyChange(Properties.quote.name(), this.quote, this.quote = newQuote);
        }

        firePropertyChange(Properties.calculationStatus.name(), this.calculationStatus,
                        this.calculationStatus = calculateStatus());
    }

    public void triggerLumpSum(long lumpSum)
    {
        firePropertyChange(Properties.lumpSum.name(), this.lumpSum, this.lumpSum = lumpSum);
        triggerConvertedLumpSum(Math.round(exchangeRate * lumpSum / Values.ExchangeRate.divider()));
    }

    public long getExchangeRate()
    {
        return exchangeRate;
    }

    public void setExchangeRate(long exchangeRate)
    {
        firePropertyChange(Properties.exchangeRate.name(), this.exchangeRate, this.exchangeRate = exchangeRate);

        triggerConvertedLumpSum(Math.round(exchangeRate * lumpSum / Values.ExchangeRate.divider()));

        firePropertyChange(Properties.calculationStatus.name(), this.calculationStatus,
                        this.calculationStatus = calculateStatus());
    }

    public long getConvertedLumpSum()
    {
        return convertedLumpSum;
    }

    public void setConvertedLumpSum(long convertedLumpSum)
    {
        triggerConvertedLumpSum(convertedLumpSum);

        if (lumpSum != 0)
        {
            long newExchangeRate = Math.round(convertedLumpSum * Values.ExchangeRate.factor() / (double) lumpSum);
            firePropertyChange(Properties.exchangeRate.name(), this.exchangeRate, this.exchangeRate = newExchangeRate);
        }

        firePropertyChange(Properties.calculationStatus.name(), this.calculationStatus,
                        this.calculationStatus = calculateStatus());
    }

    public void triggerConvertedLumpSum(long convertedLumpSum)
    {
        firePropertyChange(Properties.convertedLumpSum.name(), this.convertedLumpSum,
                        this.convertedLumpSum = convertedLumpSum);
        triggerTotal(calcTotal(convertedLumpSum, fees, taxes));
    }

    public long getFees()
    {
        return fees;
    }

    public void setFees(long fees)
    {
        firePropertyChange(Properties.fees.name(), this.fees, this.fees = fees);
        triggerTotal(calcTotal(convertedLumpSum, fees, taxes));

        firePropertyChange(Properties.calculationStatus.name(), this.calculationStatus,
                        this.calculationStatus = calculateStatus());
    }

    public long getTaxes()
    {
        return taxes;
    }

    public void setTaxes(long taxes)
    {
        firePropertyChange(Properties.taxes.name(), this.taxes, this.taxes = taxes);
        triggerTotal(calcTotal(convertedLumpSum, fees, taxes));

        firePropertyChange(Properties.calculationStatus.name(), this.calculationStatus,
                        this.calculationStatus = calculateStatus());
    }

    public long getTotal()
    {
        return total;
    }

    public void setTotal(long total)
    {
        triggerTotal(total);

        firePropertyChange(Properties.convertedLumpSum.name(), this.convertedLumpSum,
                        this.convertedLumpSum = calcLumpSum(total, fees, taxes));

        firePropertyChange(
                        Properties.lumpSum.name(),
                        this.lumpSum,
                        this.lumpSum = Math.round(convertedLumpSum * Values.ExchangeRate.divider()
                                        / (double) exchangeRate));

        if (shares != 0)
            firePropertyChange(Properties.quote.name(), this.quote,
                            this.quote = Math.round(lumpSum * Values.Share.factor() / (double) shares));

        firePropertyChange(Properties.calculationStatus.name(), this.calculationStatus,
                        this.calculationStatus = calculateStatus());
    }

    public void triggerTotal(long total)
    {
        firePropertyChange(Properties.total.name(), this.total, this.total = total);
    }

    public String getSecurityCurrencyCode()
    {
        return security != null ? security.getCurrencyCode() : ""; //$NON-NLS-1$
    }

    public String getAccountCurrencyCode()
    {
        return portfolio != null ? portfolio.getReferenceAccount().getCurrencyCode() : ""; //$NON-NLS-1$
    }

    public String getExchangeRateCurrencies()
    {
        return String.format("%s/%s", getAccountCurrencyCode(), getSecurityCurrencyCode()); //$NON-NLS-1$
    }

    public String getAccountName()
    {
        return portfolio != null ? portfolio.getReferenceAccount().getName() : ""; //$NON-NLS-1$
    }

    public PortfolioTransaction.Type getType()
    {
        return type;
    }

    private long calcLumpSum(long total, long fees, long taxes)
    {
        switch (type)
        {
            case BUY:
                return Math.max(0, total - fees - taxes);
            case SELL:
                return total + fees + taxes;
            default:
                throw new UnsupportedOperationException();
        }
    }

    private long calcTotal(long lumpSum, long fees, long taxes)
    {
        switch (type)
        {
            case BUY:
                return lumpSum + fees + taxes;
            case SELL:
                return Math.max(0, lumpSum - fees - taxes);
            default:
                throw new UnsupportedOperationException();
        }
    }
}
