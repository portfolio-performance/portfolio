package name.abuchen.portfolio.ui.dialogs.transactions;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Optional;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.PortfolioTransaction.Type;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.CurrencyConverterImpl;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.ExchangeRate;
import name.abuchen.portfolio.money.ExchangeRateTimeSeries;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.PortfolioSnapshot;
import name.abuchen.portfolio.snapshot.SecurityPosition;
import name.abuchen.portfolio.ui.Messages;

import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;

import com.ibm.icu.text.MessageFormat;

public abstract class AbstractSecurityTransactionModel extends AbstractModel
{

    public enum Properties
    {
        portfolio, security, accountName, date, shares, quote, lumpSum, exchangeRate, convertedLumpSum, //
        fees, taxes, total, note, exchangeRateCurrencies, accountCurrencyCode, securityCurrencyCode, calculationStatus;
    }

    protected final Client client;
    protected PortfolioTransaction.Type type;

    protected Portfolio portfolio;
    protected Security security;
    protected LocalDate date = LocalDate.now();
    protected long shares;
    protected long quote;
    protected long lumpSum;
    protected BigDecimal exchangeRate = BigDecimal.ONE;
    protected long convertedLumpSum;
    protected long fees;
    protected long taxes;
    protected long total;
    protected String note;
    private IStatus calculationStatus = ValidationStatus.ok();

    public AbstractSecurityTransactionModel(Client client, Type type)
    {
        this.client = client;
        this.type = type;
    }

    @Override
    public String getHeading()
    {
        return type.toString();
    }

    public abstract boolean accepts(Type type);

    public abstract void setSource(Object source);

    protected void fillFromTransaction(PortfolioTransaction transaction)
    {
        this.security = transaction.getSecurity();

        this.date = transaction.getDate();

        this.shares = transaction.getShares();
        this.total = transaction.getAmount();
        this.taxes = transaction.getTaxes();
        this.fees = transaction.getFees();
        this.note = transaction.getNote();

        this.convertedLumpSum = calcLumpSum(total, fees, taxes);

        Optional<Transaction.Unit> forex = transaction.getUnit(Transaction.Unit.Type.LUMPSUM);
        if (forex.isPresent() && forex.get().getAmount().getCurrencyCode().equals(getAccountCurrencyCode())
                        && forex.get().getForex().getCurrencyCode().equals(getSecurityCurrencyCode()))
        {
            this.exchangeRate = forex.get().getExchangeRate();
            this.lumpSum = forex.get().getForex().getAmount();
            this.quote = Math.round(this.lumpSum * Values.Share.factor() / (double) this.shares);
        }
        else
        {
            this.exchangeRate = BigDecimal.ONE;
            this.lumpSum = convertedLumpSum;
            this.quote = transaction.getActualPurchasePrice();
        }

        firePropertyChange(Properties.calculationStatus.name(), this.calculationStatus,
                        this.calculationStatus = calculateStatus());
    }

    @Override
    public IStatus getCalculationStatus()
    {
        return calculationStatus;
    }

    /**
     * Check whether calculation works out. The separate validation is needed
     * because the model does prevent negative values in methods
     * {@link #calcLumpSum(long, long, long)} and
     * {@link #calcTotal(long, long, long)}. Due to the limited precision of the
     * quote (2 digits currently) and the exchange rate (4 digits), the lump sum
     * and converted lump sum are checked against a range.
     */
    private IStatus calculateStatus()
    {
        // check whether lump sum is in range
        long lower = Math.round(shares * (quote - 1) * Values.Amount.factor()
                        / (Values.Share.divider() * Values.Quote.divider()));
        long upper = Math.round(shares * (quote + 1) * Values.Amount.factor()
                        / (Values.Share.divider() * Values.Quote.divider()));
        if (lumpSum < lower || lumpSum > upper)
            return ValidationStatus.error(Messages.MsgIncorrectSubTotal);

        // check whether converted lump sum is in range
        upper = Math.round(lumpSum * exchangeRate.add(BigDecimal.valueOf(0.0001)).doubleValue());
        lower = Math.round(lumpSum * exchangeRate.add(BigDecimal.valueOf(-0.0001)).doubleValue());
        if (convertedLumpSum < lower || convertedLumpSum > upper)
            return ValidationStatus.error(Messages.MsgIncorrectConvertedSubTotal);

        // check total
        long t = calcTotal(convertedLumpSum, fees, taxes);
        if (t != total)
            return ValidationStatus.error(Messages.MsgIncorrectTotal);

        if (total == 0L)
            return ValidationStatus.error(MessageFormat.format(Messages.MsgDialogInputRequired, Messages.ColumnTotal));

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

        if (security != null)
        {
            updateSharesAndQuote();
            updateExchangeRate();
        }
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
        updateExchangeRate();
    }

    private void updateSharesAndQuote()
    {
        if (type == PortfolioTransaction.Type.SELL || type == PortfolioTransaction.Type.DELIVERY_OUTBOUND)
        {
            boolean hasPosition = false;
            if (portfolio != null)
            {
                // since the security position has always the currency of the
                // investment vehicle, actually no conversion is needed. Hence
                // we can use an arbitrary converter.
                CurrencyConverter converter = new CurrencyConverterImpl(getExchangeRateProviderFactory(),
                                CurrencyUnit.EUR);
                PortfolioSnapshot snapshot = PortfolioSnapshot.create(portfolio, converter, date);
                SecurityPosition position = snapshot.getPositionsBySecurity().get(security);
                if (position != null)
                {
                    setShares(position.getShares());
                    setTotal(position.calculateValue().getAmount());
                    hasPosition = true;
                }
            }

            if (!hasPosition)
            {
                setShares(0);
                setQuote(security.getSecurityPrice(date).getValue());
            }
        }
        else
        {
            setQuote(security.getSecurityPrice(date).getValue());
        }
    }

    private void updateExchangeRate()
    {
        if (getAccountCurrencyCode().equals(getSecurityCurrencyCode()))
        {
            setExchangeRate(BigDecimal.ONE);
        }
        else if (!getAccountCurrencyCode().isEmpty() && !getSecurityCurrencyCode().isEmpty())
        {
            ExchangeRateTimeSeries series = getExchangeRateProviderFactory() //
                            .getTimeSeries(getSecurityCurrencyCode(), getAccountCurrencyCode());

            if (series != null)
                setExchangeRate(series.lookupRate(date).orElse(new ExchangeRate(date, BigDecimal.ONE)).getValue());
            else
                setExchangeRate(BigDecimal.ONE);
        }
    }

    public LocalDate getDate()
    {
        return date;
    }

    public void setDate(LocalDate date)
    {
        firePropertyChange(Properties.date.name(), this.date, this.date = date);
        updateExchangeRate();
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
        triggerConvertedLumpSum(Math.round(exchangeRate.doubleValue() * lumpSum));
    }

    public BigDecimal getExchangeRate()
    {
        return exchangeRate;
    }

    public void setExchangeRate(BigDecimal exchangeRate)
    {
        BigDecimal newRate = exchangeRate == null ? BigDecimal.ZERO : exchangeRate;

        firePropertyChange(Properties.exchangeRate.name(), this.exchangeRate, this.exchangeRate = newRate);

        triggerConvertedLumpSum(Math.round(newRate.doubleValue() * lumpSum));

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
            BigDecimal newExchangeRate = BigDecimal.valueOf(convertedLumpSum).divide(BigDecimal.valueOf(lumpSum), 10,
                            RoundingMode.HALF_UP);
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

        firePropertyChange(Properties.lumpSum.name(), this.lumpSum,
                        this.lumpSum = Math.round(convertedLumpSum / exchangeRate.doubleValue()));

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

    public String getNote()
    {
        return note;
    }

    public void setNote(String note)
    {
        firePropertyChange(Properties.note.name(), this.note, this.note = note);
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

    protected long calcLumpSum(long total, long fees, long taxes)
    {
        switch (type)
        {
            case BUY:
            case DELIVERY_INBOUND:
                return Math.max(0, total - fees - taxes);
            case SELL:
            case DELIVERY_OUTBOUND:
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
            case DELIVERY_INBOUND:
                return lumpSum + fees + taxes;
            case SELL:
            case DELIVERY_OUTBOUND:
                return Math.max(0, lumpSum - fees - taxes);
            default:
                throw new UnsupportedOperationException();
        }
    }
}
