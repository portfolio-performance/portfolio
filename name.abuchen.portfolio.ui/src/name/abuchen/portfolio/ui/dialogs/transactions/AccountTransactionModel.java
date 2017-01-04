package name.abuchen.portfolio.ui.dialogs.transactions;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;

import com.ibm.icu.text.MessageFormat;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.CurrencyConverterImpl;
import name.abuchen.portfolio.money.ExchangeRate;
import name.abuchen.portfolio.money.ExchangeRateTimeSeries;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.ClientSnapshot;
import name.abuchen.portfolio.snapshot.SecurityPosition;
import name.abuchen.portfolio.ui.Messages;

public class AccountTransactionModel extends AbstractModel
{
    public enum Properties
    {
        security, account, date, shares, fxGrossAmount, dividendAmount, exchangeRate, inverseExchangeRate, grossAmount, // NOSONAR
        fxTaxes, taxes, total, note, exchangeRateCurrencies, inverseExchangeRateCurrencies, // NOSONAR
        accountCurrencyCode, securityCurrencyCode, fxCurrencyCode, calculationStatus; // NOSONAR
    }

    public static final Security EMPTY_SECURITY = new Security("", ""); //$NON-NLS-1$ //$NON-NLS-2$

    private final Client client;
    private AccountTransaction.Type type;

    private Account sourceAccount;
    private AccountTransaction sourceTransaction;

    private Security security;
    private Account account;
    private LocalDate date = LocalDate.now();
    private long shares;

    private long fxGrossAmount;
    private long dividendAmount;
    private BigDecimal exchangeRate = BigDecimal.ONE;
    private long grossAmount;

    private long fxTaxes;
    private long taxes;
    private long total;

    private String note;

    private IStatus calculationStatus = ValidationStatus.ok();

    public AccountTransactionModel(Client client, AccountTransaction.Type type)
    {
        this.client = client;
        this.type = type;

        checkType();
    }

    @Override
    public String getHeading()
    {
        return type.toString();
    }

    private void checkType()
    {
        switch (type)
        {
            case DEPOSIT:
            case REMOVAL:
            case FEES:
            case TAXES:
            case TAX_REFUND:
            case INTEREST:
            case INTEREST_CHARGE:
            case DIVIDENDS:
                return;
            case BUY:
            case SELL:
            case TRANSFER_IN:
            case TRANSFER_OUT:
            default:
                throw new UnsupportedOperationException();
        }
    }

    @Override
    public void applyChanges()
    {
        if (security == null && supportsSecurity() && !supportsOptionalSecurity())
            throw new UnsupportedOperationException(Messages.MsgMissingSecurity);
        if (account == null)
            throw new UnsupportedOperationException(Messages.MsgMissingAccount);

        AccountTransaction t;

        if (sourceTransaction != null && sourceAccount.equals(account))
        {
            // transactions stays in same account
            t = sourceTransaction;
        }
        else
        {
            if (sourceTransaction != null)
            {
                sourceAccount.deleteTransaction(sourceTransaction, client);
                sourceTransaction = null;
                sourceAccount = null;
            }

            t = new AccountTransaction();
            account.addTransaction(t);
        }

        t.setDate(date);
        t.setSecurity(!EMPTY_SECURITY.equals(security) ? security : null);
        t.setShares(supportsShares() ? shares : 0);
        t.setAmount(total);
        t.setType(type);
        t.setNote(note);
        t.setCurrencyCode(getAccountCurrencyCode());

        t.clearUnits();

        if (taxes != 0)
            t.addUnit(new Transaction.Unit(Transaction.Unit.Type.TAX, Money.of(getAccountCurrencyCode(), taxes)));

        String fxCurrencyCode = getFxCurrencyCode();
        if (!fxCurrencyCode.equals(account.getCurrencyCode()))
        {
            Transaction.Unit forex = new Transaction.Unit(Transaction.Unit.Type.GROSS_VALUE, //
                            Money.of(getAccountCurrencyCode(), grossAmount), //
                            Money.of(getSecurityCurrencyCode(), fxGrossAmount), //
                            getExchangeRate());
            t.addUnit(forex);

            if (fxTaxes != 0)
                t.addUnit(new Transaction.Unit(Transaction.Unit.Type.TAX, //
                                Money.of(getAccountCurrencyCode(), Math.round(fxTaxes * exchangeRate.doubleValue())), //
                                Money.of(getSecurityCurrencyCode(), fxTaxes), //
                                exchangeRate));
        }
    }

    @Override
    public void resetToNewTransaction()
    {
        this.sourceAccount = null;
        this.sourceTransaction = null;

        setShares(0);
        setFxGrossAmount(0);
        setDividendAmount(0);
        setGrossAmount(0);
        setTaxes(0);
        setFxTaxes(0);
        setNote(null);
    }

    public boolean supportsShares()
    {
        return type == AccountTransaction.Type.DIVIDENDS;
    }

    public boolean supportsSecurity()
    {
        return type == AccountTransaction.Type.DIVIDENDS || type == AccountTransaction.Type.TAX_REFUND;
    }

    public boolean supportsOptionalSecurity()
    {
        return type == AccountTransaction.Type.TAX_REFUND;
    }

    public boolean supportsTaxUnits()
    {
        return type == AccountTransaction.Type.DIVIDENDS;
    }

    public void setSource(Account account, AccountTransaction transaction)
    {
        this.sourceAccount = account;
        this.sourceTransaction = transaction;

        this.security = transaction.getSecurity();
        if (this.security == null && supportsOptionalSecurity())
            this.security = EMPTY_SECURITY;

        this.account = account;
        this.date = transaction.getDate();
        this.shares = transaction.getShares();
        this.total = transaction.getAmount();

        // both will be overwritten if forex data exists
        this.exchangeRate = BigDecimal.ONE;
        this.taxes = 0;
        this.fxTaxes = 0;

        transaction.getUnits().forEach(unit -> {
            switch (unit.getType())
            {
                case GROSS_VALUE:
                    this.exchangeRate = unit.getExchangeRate();
                    this.grossAmount = unit.getAmount().getAmount();
                    this.fxGrossAmount = unit.getForex().getAmount();
                    break;
                case TAX:
                    if (unit.getForex() != null)
                        this.fxTaxes += unit.getForex().getAmount();
                    else
                        this.taxes += unit.getAmount().getAmount();
                    break;
                default:
                    throw new UnsupportedOperationException();
            }
        });

        this.grossAmount = calculateGrossAmount4Total();

        // in case units have to forex gross value
        if (exchangeRate.equals(BigDecimal.ONE))
            this.fxGrossAmount = grossAmount;

        this.dividendAmount = calculateDividendAmount();

        this.note = transaction.getNote();
    }

    @Override
    public IStatus getCalculationStatus()
    {
        return calculationStatus;
    }

    /**
     * Due to the limited precision of the exchange rate (4 digits), the amount
     * is checked against a range.
     */
    private IStatus calculateStatus()
    {
        // check whether converted amount is in range
        long upper = Math.round(fxGrossAmount * exchangeRate.add(BigDecimal.valueOf(0.0001)).doubleValue());
        long lower = Math.round(fxGrossAmount * exchangeRate.add(BigDecimal.valueOf(-0.0001)).doubleValue());

        if (grossAmount < lower || grossAmount > upper)
            return ValidationStatus.error(Messages.MsgErrorConvertedAmount);

        if (grossAmount == 0L)
            return ValidationStatus.error(MessageFormat.format(Messages.MsgDialogInputRequired, Messages.ColumnTotal));

        return ValidationStatus.ok();
    }

    public Account getAccount()
    {
        return account;
    }

    public void setAccount(Account account)
    {
        String oldCurrencyCode = getAccountCurrencyCode();
        String oldFxCurrencyCode = getFxCurrencyCode();
        String oldExchangeRateCurrencies = getExchangeRateCurrencies();
        String oldInverseExchangeRateCurrencies = getInverseExchangeRateCurrencies();

        firePropertyChange(Properties.account.name(), this.account, this.account = account);

        firePropertyChange(Properties.accountCurrencyCode.name(), oldCurrencyCode, getAccountCurrencyCode());
        firePropertyChange(Properties.fxCurrencyCode.name(), oldFxCurrencyCode, getFxCurrencyCode());
        firePropertyChange(Properties.exchangeRateCurrencies.name(), oldExchangeRateCurrencies,
                        getExchangeRateCurrencies());
        firePropertyChange(Properties.inverseExchangeRateCurrencies.name(), oldInverseExchangeRateCurrencies,
                        getInverseExchangeRateCurrencies());

        updateExchangeRate();
    }

    public Security getSecurity()
    {
        return security;
    }

    public void setSecurity(Security security)
    {
        if (!supportsSecurity())
            return;

        String oldCurrencyCode = getSecurityCurrencyCode();
        String oldFxCurrencyCode = getFxCurrencyCode();
        String oldExchangeRateCurrencies = getExchangeRateCurrencies();
        String oldInverseExchangeRateCurrencies = getInverseExchangeRateCurrencies();

        firePropertyChange(Properties.security.name(), this.security, this.security = security);

        firePropertyChange(Properties.securityCurrencyCode.name(), oldCurrencyCode, getSecurityCurrencyCode());
        firePropertyChange(Properties.fxCurrencyCode.name(), oldFxCurrencyCode, getFxCurrencyCode());
        firePropertyChange(Properties.exchangeRateCurrencies.name(), oldExchangeRateCurrencies,
                        getExchangeRateCurrencies());
        firePropertyChange(Properties.inverseExchangeRateCurrencies.name(), oldInverseExchangeRateCurrencies,
                        getInverseExchangeRateCurrencies());

        updateExchangeRate();
        updateShares();
    }

    private void updateExchangeRate()
    {
        if (getAccountCurrencyCode().equals(getSecurityCurrencyCode()))
        {
            setExchangeRate(BigDecimal.ONE);
        }
        else if (!getSecurityCurrencyCode().isEmpty())
        {
            ExchangeRateTimeSeries series = getExchangeRateProviderFactory() //
                            .getTimeSeries(getSecurityCurrencyCode(), getAccountCurrencyCode());

            if (series != null)
                setExchangeRate(series.lookupRate(date).orElse(new ExchangeRate(date, BigDecimal.ONE)).getValue());
            else
                setExchangeRate(BigDecimal.ONE);
        }
    }

    private void updateShares()
    {
        if (!supportsShares() || security == null)
            return;

        CurrencyConverter converter = new CurrencyConverterImpl(getExchangeRateProviderFactory(),
                        client.getBaseCurrency());
        ClientSnapshot snapshot = ClientSnapshot.create(client, converter, date);
        SecurityPosition p = snapshot.getJointPortfolio().getPositionsBySecurity().get(security);
        setShares(p != null ? p.getShares() : 0);
    }

    public LocalDate getDate()
    {
        return date;
    }

    public void setDate(LocalDate date)
    {
        firePropertyChange(Properties.date.name(), this.date, this.date = date);
        updateShares();
        updateExchangeRate();
    }

    public long getShares()
    {
        return shares;
    }

    public void setShares(long shares)
    {
        firePropertyChange(Properties.shares.name(), this.shares, this.shares = shares);

        firePropertyChange(Properties.dividendAmount.name(), this.dividendAmount,
                        this.dividendAmount = calculateDividendAmount());
    }

    public long getFxGrossAmount()
    {
        return fxGrossAmount;
    }

    public void setFxGrossAmount(long foreignCurrencyAmount)
    {
        firePropertyChange(Properties.fxGrossAmount.name(), this.fxGrossAmount,
                        this.fxGrossAmount = foreignCurrencyAmount);

        triggerGrossAmount(Math.round(exchangeRate.doubleValue() * foreignCurrencyAmount));

        firePropertyChange(Properties.dividendAmount.name(), this.dividendAmount,
                        this.dividendAmount = calculateDividendAmount());

        firePropertyChange(Properties.calculationStatus.name(), this.calculationStatus,
                        this.calculationStatus = calculateStatus());
    }

    public long getDividendAmount()
    {
        return dividendAmount;
    }

    public void setDividendAmount(long amount)
    {
        triggerDividendAmount(amount);
        long myGrossAmount = calculateGrossAmount4Dividend();
        setFxGrossAmount(myGrossAmount);
    }

    public void triggerDividendAmount(long amount)
    {
        firePropertyChange(Properties.dividendAmount.name(), this.dividendAmount, this.dividendAmount = amount);
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

        triggerGrossAmount(Math.round(newRate.doubleValue() * fxGrossAmount));

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

    public long getGrossAmount()
    {
        return grossAmount;
    }

    public void setGrossAmount(long amount)
    {
        triggerGrossAmount(amount);

        if (fxGrossAmount != 0)
        {
            BigDecimal newExchangeRate = BigDecimal.valueOf(amount).divide(BigDecimal.valueOf(fxGrossAmount), 10,
                            RoundingMode.HALF_UP);
            BigDecimal oldInverseRate = getInverseExchangeRate();
            firePropertyChange(Properties.exchangeRate.name(), this.exchangeRate, this.exchangeRate = newExchangeRate);
            firePropertyChange(Properties.inverseExchangeRate.name(), oldInverseRate, getInverseExchangeRate());
        }

        firePropertyChange(Properties.calculationStatus.name(), this.calculationStatus,
                        this.calculationStatus = calculateStatus());
    }

    public void triggerGrossAmount(long amount)
    {
        firePropertyChange(Properties.grossAmount.name(), this.grossAmount, this.grossAmount = amount);
        triggerTotal(calculateTotal());
    }

    public long getFxTaxes()
    {
        return fxTaxes;
    }

    public void setFxTaxes(long fxTaxes)
    {
        firePropertyChange(Properties.fxTaxes.name(), this.fxTaxes, this.fxTaxes = fxTaxes);
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

    public long getTotal()
    {
        return total;
    }

    public void setTotal(long total)
    {
        triggerTotal(total);

        firePropertyChange(Properties.grossAmount.name(), this.grossAmount,
                        this.grossAmount = calculateGrossAmount4Total());

        firePropertyChange(Properties.fxGrossAmount.name(), this.fxGrossAmount,
                        this.fxGrossAmount = Math.round(grossAmount / exchangeRate.doubleValue()));

        firePropertyChange(Properties.dividendAmount.name(), this.dividendAmount,
                        this.dividendAmount = calculateDividendAmount());

        firePropertyChange(Properties.calculationStatus.name(), this.calculationStatus,
                        this.calculationStatus = calculateStatus());
    }

    public void triggerTotal(long total)
    {
        firePropertyChange(Properties.total.name(), this.total, this.total = total);
    }

    protected long calculateDividendAmount()
    {
        if (shares > 0)
            return Math.round((fxGrossAmount * Values.Share.factor()) / (double) shares);
        else
            return 0L;
    }

    protected long calculateGrossAmount4Total()
    {
        long totalTaxes = taxes + Math.round(exchangeRate.doubleValue() * fxTaxes);
        return total + totalTaxes;
    }

    protected long calculateGrossAmount4Dividend()
    {
        return Math.round((shares * dividendAmount) / (double) Values.Share.factor());
    }

    private long calculateTotal()
    {
        long totalTaxes = taxes + Math.round(exchangeRate.doubleValue() * fxTaxes);
        return Math.max(0, grossAmount - totalTaxes);
    }

    public String getNote()
    {
        return note;
    }

    public void setNote(String note)
    {
        firePropertyChange(Properties.note.name(), this.note, this.note = note);
    }

    public String getAccountCurrencyCode()
    {
        return account != null ? account.getCurrencyCode() : ""; //$NON-NLS-1$
    }

    public String getSecurityCurrencyCode()
    {
        return security != null ? security.getCurrencyCode() : ""; //$NON-NLS-1$
    }

    public String getFxCurrencyCode()
    {
        return security != null && !security.getCurrencyCode().isEmpty() ? security.getCurrencyCode()
                        : getAccountCurrencyCode();
    }

    /**
     * Returns exchange rate label in direct (price) notation.
     */
    public String getExchangeRateCurrencies()
    {
        return String.format("%s/%s", getSecurityCurrencyCode(), getAccountCurrencyCode()); //$NON-NLS-1$
    }

    /**
     * Returns exchange rate label in indirect (quantity) notation.
     */
    public String getInverseExchangeRateCurrencies()
    {
        return String.format("%s/%s", getAccountCurrencyCode(), getSecurityCurrencyCode()); //$NON-NLS-1$
    }

    public AccountTransaction.Type getType()
    {
        return type;
    }
}
