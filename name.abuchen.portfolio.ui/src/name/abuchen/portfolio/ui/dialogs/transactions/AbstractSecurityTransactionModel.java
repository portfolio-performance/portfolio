package name.abuchen.portfolio.ui.dialogs.transactions;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;

import com.ibm.icu.text.MessageFormat;

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
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.PortfolioSnapshot;
import name.abuchen.portfolio.snapshot.SecurityPosition;
import name.abuchen.portfolio.ui.Messages;

public abstract class AbstractSecurityTransactionModel extends AbstractModel
{

    public enum Properties
    {
        portfolio, security, account, date, time, shares, quote, grossValue, exchangeRate, inverseExchangeRate, //
        convertedGrossValue, forexFees, fees, forexTaxes, taxes, total, note, exchangeRateCurrencies, //
        inverseExchangeRateCurrencies, transactionCurrency, transactionCurrencyCode, securityCurrencyCode, //
        calculationStatus;
    }

    protected final Client client;
    protected PortfolioTransaction.Type type;

    protected Portfolio portfolio;
    protected Security security;
    protected LocalDate date = LocalDate.now();
    protected LocalTime time = LocalTime.now();
    protected long shares;
    protected BigDecimal quote = BigDecimal.ONE;
    protected long grossValue;
    protected BigDecimal exchangeRate = BigDecimal.ONE;
    protected long convertedGrossValue;
    protected long forexFees;
    protected long fees;
    protected long forexTaxes;
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
    
    public abstract boolean hasSource();

    @Override
    public void resetToNewTransaction()
    {
        setShares(0);
        setGrossValue(0);
        setConvertedGrossValue(0);
        setTotal(0);
        setFees(0);
        setTaxes(0);
        setForexFees(0);
        setForexTaxes(0);
        setNote(null);
    }

    protected void fillFromTransaction(PortfolioTransaction transaction)
    {
        this.security = transaction.getSecurity();

        LocalDateTime transactionDate = transaction.getDateTime();
        this.date = transactionDate.toLocalDate();
        this.time = transactionDate.toLocalTime();

        this.shares = transaction.getShares();
        this.total = transaction.getAmount();
        this.note = transaction.getNote();

        // will be overwritten if forex data exists
        this.exchangeRate = BigDecimal.ONE;

        transaction.getUnits().forEach(unit -> {
            switch (unit.getType())
            {
                case GROSS_VALUE:
                    this.exchangeRate = unit.getExchangeRate();
                    this.grossValue = unit.getForex().getAmount();
                    this.quote = new BigDecimal(
                                    this.grossValue * Values.Share.factor() / (this.shares * Values.Amount.divider()));
                    break;
                case FEE:
                    if (unit.getForex() != null)
                        this.forexFees += unit.getForex().getAmount();
                    else
                        this.fees += unit.getAmount().getAmount();
                    break;
                case TAX:
                    if (unit.getForex() != null)
                        this.forexTaxes += unit.getForex().getAmount();
                    else
                        this.taxes += unit.getAmount().getAmount();
                    break;
                default:
                    throw new UnsupportedOperationException();
            }
        });

        this.convertedGrossValue = calculateConvertedGrossValue();

        if (exchangeRate.equals(BigDecimal.ONE))
        {
            // units contained no information about forex
            this.grossValue = convertedGrossValue;
            this.quote = transaction.getGrossPricePerShare().toBigDecimal();
        }

        firePropertyChange(Properties.calculationStatus.name(), this.calculationStatus,
                        this.calculationStatus = calculateStatus());
    }

    protected void writeToTransaction(PortfolioTransaction transaction)
    {
        transaction.clearUnits();

        if (fees != 0)
            transaction.addUnit(new Transaction.Unit(Transaction.Unit.Type.FEE, //
                            Money.of(getTransactionCurrencyCode(), fees)));

        if (taxes != 0)
            transaction.addUnit(new Transaction.Unit(Transaction.Unit.Type.TAX, //
                            Money.of(getTransactionCurrencyCode(), taxes)));

        boolean hasForex = !getTransactionCurrencyCode().equals(getSecurityCurrencyCode());
        if (hasForex)
        {
            if (forexFees != 0)
                transaction.addUnit(new Transaction.Unit(Transaction.Unit.Type.FEE, //
                                Money.of(getTransactionCurrencyCode(),
                                                Math.round(forexFees * exchangeRate.doubleValue())), //
                                Money.of(getSecurityCurrencyCode(), forexFees), //
                                exchangeRate));

            if (forexTaxes != 0)
                transaction.addUnit(new Transaction.Unit(Transaction.Unit.Type.TAX, //
                                Money.of(getTransactionCurrencyCode(),
                                                Math.round(forexTaxes * exchangeRate.doubleValue())), //
                                Money.of(getSecurityCurrencyCode(), forexTaxes), //
                                exchangeRate));

            transaction.addUnit(new Transaction.Unit(Transaction.Unit.Type.GROSS_VALUE, //
                            Money.of(getTransactionCurrencyCode(), convertedGrossValue), //
                            Money.of(getSecurityCurrencyCode(), grossValue), //
                            exchangeRate));
        }

    }

    @Override
    public IStatus getCalculationStatus()
    {
        return calculationStatus;
    }

    /**
     * Check whether calculation works out. The separate validation is needed
     * because the model does prevent negative values in methods
     * {@link #calcGrossValue(long, long, long)} and
     * {@link #calcTotal(long, long, long)}. Due to the limited precision of the
     * quote (2 digits currently) and the exchange rate (4 digits), the gross
     * value and converted gross value are checked against a range.
     */
    private IStatus calculateStatus()
    {
        if (shares == 0L)
            return ValidationStatus.error(MessageFormat.format(Messages.MsgDialogInputRequired, Messages.ColumnShares));

        if ((grossValue == 0L || convertedGrossValue == 0L) && type != PortfolioTransaction.Type.DELIVERY_OUTBOUND)
            return ValidationStatus.error(MessageFormat.format(Messages.MsgDialogInputRequired, Messages.ColumnSubTotal));

        // check whether gross value is in range
        long lower = Math.round(shares * quote.add(BigDecimal.valueOf(-0.01)).doubleValue() * Values.Amount.factor()
                        / Values.Share.divider());
        long upper = Math.round(shares * quote.add(BigDecimal.valueOf(0.01)).doubleValue() * Values.Amount.factor()
                        / Values.Share.divider());
        if (grossValue < lower || grossValue > upper)
            return ValidationStatus.error(Messages.MsgIncorrectSubTotal);

        // check whether converted gross value is in range
        upper = Math.round(grossValue * exchangeRate.add(BigDecimal.valueOf(0.0001)).doubleValue());
        lower = Math.round(grossValue * exchangeRate.add(BigDecimal.valueOf(-0.0001)).doubleValue());
        if (convertedGrossValue < lower || convertedGrossValue > upper)
            return ValidationStatus.error(Messages.MsgIncorrectConvertedSubTotal);

        // check total
        long t = calculateTotal();
        if (t != total)
            return ValidationStatus.error(MessageFormat.format(Messages.MsgIncorrectTotal, Values.Amount.format(t)));

        if (total == 0L && type != PortfolioTransaction.Type.DELIVERY_OUTBOUND)
            return ValidationStatus.error(MessageFormat.format(Messages.MsgDialogInputRequired, Messages.ColumnTotal));
        
        return ValidationStatus.ok();
    }

    public Portfolio getPortfolio()
    {
        return portfolio;
    }

    public void setPortfolio(Portfolio portfolio)
    {
        firePropertyChange(Properties.portfolio.name(), this.portfolio, this.portfolio = portfolio);

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
        String oldInverseExchangeRateCurrencies = getInverseExchangeRateCurrencies();

        firePropertyChange(Properties.security.name(), this.security, this.security = security);

        firePropertyChange(Properties.securityCurrencyCode.name(), oldCurrencyCode, getSecurityCurrencyCode());
        firePropertyChange(Properties.exchangeRateCurrencies.name(), oldExchangeRateCurrencies,
                        getExchangeRateCurrencies());
        firePropertyChange(Properties.inverseExchangeRateCurrencies.name(), oldInverseExchangeRateCurrencies,
                        getInverseExchangeRateCurrencies());

        updateSharesAndQuote();
        updateExchangeRate();
    }

    protected void updateSharesAndQuote()
    {
        // do not auto-suggest shares and quote when editing an existing transaction
        if (hasSource())
            return;
        
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
                setQuote(new BigDecimal(security.getSecurityPrice(date).getValue() / Values.Quote.divider()));
            }
        }
        else
        {
            setQuote(new BigDecimal(security.getSecurityPrice(date).getValue() / Values.Quote.divider()));
        }
    }

    protected void updateExchangeRate()
    {
        if (getTransactionCurrencyCode().equals(getSecurityCurrencyCode()))
        {
            setExchangeRate(BigDecimal.ONE);
        }
        else if (!getTransactionCurrencyCode().isEmpty() && !getSecurityCurrencyCode().isEmpty())
        {
            ExchangeRateTimeSeries series = getExchangeRateProviderFactory() //
                            .getTimeSeries(getSecurityCurrencyCode(), getTransactionCurrencyCode());

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
    
    public LocalTime getTime()
    {
        return time;
    }

    public void setDate(LocalDate date)
    {
        firePropertyChange(Properties.date.name(), this.date, this.date = date);
        updateExchangeRate();
    }
    
    public void setTime(LocalTime time)
    {
        firePropertyChange(Properties.time.name(), this.time, this.time = time);
        updateExchangeRate();
    }

    public long getShares()
    {
        return shares;
    }

    public void setShares(long shares)
    {
        firePropertyChange(Properties.shares.name(), this.shares, this.shares = shares);

        if (quote.doubleValue() != 0)
        {
            triggerGrossValue(
                            Math.round(shares * quote.doubleValue() * Values.Amount.factor() / Values.Share.divider()));
        }
        else if (grossValue != 0 && shares != 0)
        {
            setQuote(new BigDecimal(grossValue * Values.Share.factor() / (shares * Values.Amount.divider())));
        }

        firePropertyChange(Properties.calculationStatus.name(), this.calculationStatus,
                        this.calculationStatus = calculateStatus());
    }

    public BigDecimal getQuote()
    {
        return quote;
    }

    public void setQuote(BigDecimal quote)
    {
        firePropertyChange(Properties.quote.name(), this.quote, this.quote = quote);

        triggerGrossValue(Math.round(shares * quote.doubleValue() * Values.Amount.factor() / Values.Share.divider()));

        firePropertyChange(Properties.calculationStatus.name(), this.calculationStatus,
                        this.calculationStatus = calculateStatus());
    }

    public long getGrossValue()
    {
        return grossValue;
    }

    public void setGrossValue(long grossValue)
    {
        triggerGrossValue(grossValue);

        if (shares != 0)
        {
            BigDecimal newQuote = new BigDecimal(
                            grossValue * Values.Share.factor() / (shares * Values.Amount.divider()));
            firePropertyChange(Properties.quote.name(), this.quote, this.quote = newQuote);
        }

        firePropertyChange(Properties.calculationStatus.name(), this.calculationStatus,
                        this.calculationStatus = calculateStatus());
    }

    public void triggerGrossValue(long grossValue)
    {
        firePropertyChange(Properties.grossValue.name(), this.grossValue, this.grossValue = grossValue);
        triggerConvertedGrossValue(Math.round(exchangeRate.doubleValue() * grossValue));
    }

    public BigDecimal getExchangeRate()
    {
        return exchangeRate;
    }

    public void setExchangeRate(BigDecimal exchangeRate)
    {
        BigDecimal newRate = exchangeRate == null ? BigDecimal.ZERO : exchangeRate;

        BigDecimal oldInverseRate = getInverseExchangeRate();
        firePropertyChange(Properties.exchangeRate.name(), this.exchangeRate, this.exchangeRate = newRate);
        firePropertyChange(Properties.inverseExchangeRate.name(), oldInverseRate, getInverseExchangeRate());

        triggerConvertedGrossValue(Math.round(newRate.doubleValue() * grossValue));

        firePropertyChange(Properties.calculationStatus.name(), this.calculationStatus,
                        this.calculationStatus = calculateStatus());
    }

    public BigDecimal getInverseExchangeRate()
    {
        return BigDecimal.ONE.divide(exchangeRate, 10, BigDecimal.ROUND_HALF_DOWN);
    }

    public void setInverseExchangeRate(BigDecimal rate)
    {
        setExchangeRate(BigDecimal.ONE.divide(rate, 10, BigDecimal.ROUND_HALF_DOWN));
    }

    public long getConvertedGrossValue()
    {
        return convertedGrossValue;
    }

    public void setConvertedGrossValue(long convertedGrossValue)
    {
        triggerConvertedGrossValue(convertedGrossValue);

        if (grossValue != 0)
        {
            BigDecimal newExchangeRate = BigDecimal.valueOf(convertedGrossValue).divide(BigDecimal.valueOf(grossValue),
                            10, RoundingMode.HALF_UP);
            BigDecimal oldInverseRate = getInverseExchangeRate();
            firePropertyChange(Properties.exchangeRate.name(), this.exchangeRate, this.exchangeRate = newExchangeRate);
            firePropertyChange(Properties.inverseExchangeRate.name(), oldInverseRate, getInverseExchangeRate());

            triggerTotal(calculateTotal()); // forex fees and taxes might change
        }

        firePropertyChange(Properties.calculationStatus.name(), this.calculationStatus,
                        this.calculationStatus = calculateStatus());
    }

    public void triggerConvertedGrossValue(long convertedGrossValue)
    {
        firePropertyChange(Properties.convertedGrossValue.name(), this.convertedGrossValue,
                        this.convertedGrossValue = convertedGrossValue);
        triggerTotal(calculateTotal());
    }

    public long getFees()
    {
        return fees;
    }

    public void setFees(long fees)
    {
        firePropertyChange(Properties.fees.name(), this.fees, this.fees = fees);
        triggerTotal(calculateTotal());

        firePropertyChange(Properties.calculationStatus.name(), this.calculationStatus,
                        this.calculationStatus = calculateStatus());
    }

    public long getForexFees()
    {
        return forexFees;
    }

    public void setForexFees(long forexFees)
    {
        firePropertyChange(Properties.forexFees.name(), this.forexFees, this.forexFees = forexFees);
        triggerTotal(calculateTotal());

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
        triggerTotal(calculateTotal());

        firePropertyChange(Properties.calculationStatus.name(), this.calculationStatus,
                        this.calculationStatus = calculateStatus());
    }

    public long getForexTaxes()
    {
        return forexTaxes;
    }

    public void setForexTaxes(long forexTaxes)
    {
        firePropertyChange(Properties.forexTaxes.name(), this.forexTaxes, this.forexTaxes = forexTaxes);
        triggerTotal(calculateTotal());

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

        firePropertyChange(Properties.convertedGrossValue.name(), this.convertedGrossValue,
                        this.convertedGrossValue = calculateConvertedGrossValue());

        firePropertyChange(Properties.grossValue.name(), this.grossValue,
                        this.grossValue = Math.round(convertedGrossValue / exchangeRate.doubleValue()));

        if (shares != 0)
            firePropertyChange(Properties.quote.name(), this.quote, this.quote = new BigDecimal(
                            grossValue * Values.Share.factor() / (shares * Values.Amount.divider())));

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

    public abstract String getTransactionCurrencyCode();

    /**
     * Returns exchange rate label in direct (price) notation.
     */
    public String getExchangeRateCurrencies()
    {
        return String.format("%s/%s", getSecurityCurrencyCode(), getTransactionCurrencyCode()); //$NON-NLS-1$
    }

    /**
     * Returns exchange rate label in indirect (quantity) notation.
     */
    public String getInverseExchangeRateCurrencies()
    {
        return String.format("%s/%s", getTransactionCurrencyCode(), getSecurityCurrencyCode()); //$NON-NLS-1$
    }

    public PortfolioTransaction.Type getType()
    {
        return type;
    }

    protected long calculateConvertedGrossValue()
    {
        long feesAndTaxes = fees + taxes + Math.round(exchangeRate.doubleValue() * (forexFees + forexTaxes));

        switch (type)
        {
            case BUY:
            case DELIVERY_INBOUND:
                return Math.max(0, total - feesAndTaxes);
            case SELL:
            case DELIVERY_OUTBOUND:
                return total + feesAndTaxes;
            default:
                throw new UnsupportedOperationException();
        }
    }

    private long calculateTotal()
    {
        long feesAndTaxes = fees + taxes + Math.round(exchangeRate.doubleValue() * (forexFees + forexTaxes));

        switch (type)
        {
            case BUY:
            case DELIVERY_INBOUND:
                return convertedGrossValue + feesAndTaxes;
            case SELL:
            case DELIVERY_OUTBOUND:
                return Math.max(0, convertedGrossValue - feesAndTaxes);
            default:
                throw new UnsupportedOperationException();
        }
    }
}
