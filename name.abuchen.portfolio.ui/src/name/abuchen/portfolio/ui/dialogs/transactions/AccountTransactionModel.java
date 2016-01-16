package name.abuchen.portfolio.ui.dialogs.transactions;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Optional;

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
import name.abuchen.portfolio.snapshot.ClientSnapshot;
import name.abuchen.portfolio.snapshot.SecurityPosition;
import name.abuchen.portfolio.ui.Messages;

public class AccountTransactionModel extends AbstractModel
{
    public enum Properties
    {
        security, account, date, shares, fxAmount, exchangeRate, inverseExchangeRate, amount, //
        note, exchangeRateCurrencies, inverseExchangeRateCurrencies, accountCurrencyCode, //
        securityCurrencyCode, fxCurrencyCode, calculationStatus;
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

    private long fxAmount;
    private BigDecimal exchangeRate = BigDecimal.ONE;
    private long amount;
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
        t.setSecurity(security);
        t.setShares(supportsShares() ? shares : 0);
        t.setAmount(amount);
        t.setType(type);
        t.setNote(note);
        t.setCurrencyCode(getAccountCurrencyCode());

        t.clearUnits();

        String fxCurrencyCode = getFxCurrencyCode();
        if (!fxCurrencyCode.equals(account.getCurrencyCode()))
        {
            Transaction.Unit forex = new Transaction.Unit(Transaction.Unit.Type.GROSS_VALUE, //
                            Money.of(getAccountCurrencyCode(), amount), //
                            Money.of(getSecurityCurrencyCode(), fxAmount), //
                            getExchangeRate());
            t.addUnit(forex);
        }
    }

    @Override
    public void resetToNewTransaction()
    {
        this.sourceAccount = null;
        this.sourceTransaction = null;

        setShares(0);
        setFxAmount(0);
        setAmount(0);
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

    public void setSource(Account account, AccountTransaction transaction)
    {
        this.sourceAccount = account;
        this.sourceTransaction = transaction;

        this.security = transaction.getSecurity();
        this.account = account;
        this.date = transaction.getDate();
        this.shares = transaction.getShares();
        this.amount = transaction.getAmount();

        Optional<Transaction.Unit> forex = transaction.getUnit(Transaction.Unit.Type.GROSS_VALUE);
        if (forex.isPresent() && forex.get().getAmount().getCurrencyCode().equals(getAccountCurrencyCode())
                        && forex.get().getForex().getCurrencyCode().equals(getFxCurrencyCode()))
        {
            this.exchangeRate = forex.get().getExchangeRate();
            this.fxAmount = forex.get().getForex().getAmount();
        }
        else
        {
            this.exchangeRate = BigDecimal.ONE;
            this.fxAmount = this.amount;
        }

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
        long upper = Math.round(fxAmount * exchangeRate.add(BigDecimal.valueOf(0.0001)).doubleValue());
        long lower = Math.round(fxAmount * exchangeRate.add(BigDecimal.valueOf(-0.0001)).doubleValue());

        if (amount < lower || amount > upper)
            return ValidationStatus.error(Messages.MsgErrorConvertedAmount);

        if (amount == 0L)
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
    }

    public long getFxAmount()
    {
        return fxAmount;
    }

    public void setFxAmount(long foreignCurrencyAmount)
    {
        firePropertyChange(Properties.fxAmount.name(), this.fxAmount, this.fxAmount = foreignCurrencyAmount);

        triggerAmount(Math.round(exchangeRate.doubleValue() * foreignCurrencyAmount));

        firePropertyChange(Properties.calculationStatus.name(), this.calculationStatus,
                        this.calculationStatus = calculateStatus());
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

        triggerAmount(Math.round(newRate.doubleValue() * fxAmount));

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

    public long getAmount()
    {
        return amount;
    }

    public void setAmount(long amount)
    {
        triggerAmount(amount);

        if (fxAmount != 0)
        {
            BigDecimal newExchangeRate = BigDecimal.valueOf(amount).divide(BigDecimal.valueOf(fxAmount), 10,
                            RoundingMode.HALF_UP);
            BigDecimal oldInverseRate = getInverseExchangeRate();
            firePropertyChange(Properties.exchangeRate.name(), this.exchangeRate, this.exchangeRate = newExchangeRate);
            firePropertyChange(Properties.inverseExchangeRate.name(), oldInverseRate, getInverseExchangeRate());
        }

        firePropertyChange(Properties.calculationStatus.name(), this.calculationStatus,
                        this.calculationStatus = calculateStatus());
    }

    public void triggerAmount(long amount)
    {
        firePropertyChange(Properties.amount.name(), this.amount, this.amount = amount);
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

    public String getExchangeRateCurrencies()
    {
        return String.format("%s/%s", getAccountCurrencyCode(), getSecurityCurrencyCode()); //$NON-NLS-1$
    }

    public String getInverseExchangeRateCurrencies()
    {
        return String.format("%s/%s", getSecurityCurrencyCode(), getAccountCurrencyCode()); //$NON-NLS-1$
    }

    public AccountTransaction.Type getType()
    {
        return type;
    }
}
