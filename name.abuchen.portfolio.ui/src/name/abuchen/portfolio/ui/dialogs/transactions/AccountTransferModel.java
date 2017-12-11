package name.abuchen.portfolio.ui.dialogs.transactions;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;

import com.ibm.icu.text.MessageFormat;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.AccountTransferEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.model.TransactionOwner;
import name.abuchen.portfolio.money.ExchangeRate;
import name.abuchen.portfolio.money.ExchangeRateTimeSeries;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.ui.Messages;

public class AccountTransferModel extends AbstractModel
{
    public enum Properties
    {
        sourceAccount, targetAccount, date, fxAmount, exchangeRate, inverseExchangeRate, amount, //
        note, sourceAccountCurrency, targetAccountCurrency, exchangeRateCurrencies, //
        inverseExchangeRateCurrencies, calculationStatus;
    }

    private final Client client;

    private AccountTransferEntry source;

    private Account sourceAccount;
    private Account targetAccount;
    private LocalDate date = LocalDate.now();

    private long fxAmount;
    private BigDecimal exchangeRate = BigDecimal.ONE;
    private long amount;
    private String note;

    private IStatus calculationStatus = ValidationStatus.ok();

    public AccountTransferModel(Client client)
    {
        this.client = client;
    }

    @Override
    public String getHeading()
    {
        return Messages.LabelTransfer;
    }

    @Override
    public void applyChanges()
    {
        if (sourceAccount == null)
            throw new UnsupportedOperationException(Messages.MsgAccountFromMissing);
        if (targetAccount == null)
            throw new UnsupportedOperationException(Messages.MsgAccountToMissing);

        AccountTransferEntry t;

        if (source != null && sourceAccount.equals(source.getOwner(source.getSourceTransaction()))
                        && targetAccount.equals(source.getOwner(source.getTargetTransaction())))
        {
            // transaction stays in same accounts
            t = source;
        }
        else
        {
            if (source != null)
            {
                @SuppressWarnings("unchecked")
                TransactionOwner<Transaction> owner = (TransactionOwner<Transaction>) source
                                .getOwner(source.getSourceTransaction());
                owner.deleteTransaction(source.getSourceTransaction(), client);
                source = null;
            }

            t = new AccountTransferEntry(sourceAccount, targetAccount);
            t.getSourceTransaction().setCurrencyCode(sourceAccount.getCurrencyCode());
            t.getTargetTransaction().setCurrencyCode(targetAccount.getCurrencyCode());
            t.insert();
        }

        t.setDate(date.atStartOfDay());
        t.setNote(note);

        // if source and target account have the same currencies, no forex data
        // needs to be stored

        AccountTransaction sourceTransaction = t.getSourceTransaction();

        sourceTransaction.clearUnits();

        if (sourceAccount.getCurrencyCode().equals(targetAccount.getCurrencyCode()))
        {
            sourceTransaction.setAmount(amount);
            t.getTargetTransaction().setAmount(amount);
        }
        else
        {
            // TODO improve naming of fields: the source amount is called
            // 'fxAmount' while the target amount is just called 'amount' but
            // then the source account holds the 'forex' which is switched
            sourceTransaction.setAmount(fxAmount);
            t.getTargetTransaction().setAmount(amount);

            Transaction.Unit forex = new Transaction.Unit(Transaction.Unit.Type.GROSS_VALUE, //
                            Money.of(sourceAccount.getCurrencyCode(), fxAmount), //
                            Money.of(targetAccount.getCurrencyCode(), amount), //
                            getInverseExchangeRate());

            sourceTransaction.addUnit(forex);
        }
    }

    @Override
    public void resetToNewTransaction()
    {
        this.source = null;

        setFxAmount(0);
        setAmount(0);
        setNote(null);
    }

    public void setSource(AccountTransferEntry entry)
    {
        this.source = entry;
        this.sourceAccount = (Account) entry.getOwner(entry.getSourceTransaction());
        this.targetAccount = (Account) entry.getOwner(entry.getTargetTransaction());

        LocalDateTime transactionDate = entry.getSourceTransaction().getDateTime();
        this.date = transactionDate.toLocalDate();
        this.note = entry.getSourceTransaction().getNote();

        this.fxAmount = entry.getSourceTransaction().getAmount();
        this.amount = entry.getTargetTransaction().getAmount();

        Optional<Transaction.Unit> forex = entry.getSourceTransaction().getUnit(Transaction.Unit.Type.GROSS_VALUE);

        if (forex.isPresent() && forex.get().getAmount().getCurrencyCode().equals(sourceAccount.getCurrencyCode())
                        && forex.get().getForex().getCurrencyCode().equals(targetAccount.getCurrencyCode()))
        {
            this.exchangeRate = ExchangeRate.inverse(forex.get().getExchangeRate());
        }
        else
        {
            this.exchangeRate = BigDecimal.ONE;
        }
    }

    @Override
    public IStatus getCalculationStatus()
    {
        return calculationStatus;
    }

    /**
     * Check whether calculation works out.
     */
    private IStatus calculateStatus()
    {
        // check whether converted amount is in range
        long upper = Math.round(fxAmount * exchangeRate.add(BigDecimal.valueOf(0.0001)).doubleValue());
        long lower = Math.round(fxAmount * exchangeRate.add(BigDecimal.valueOf(-0.0001)).doubleValue());

        if (amount < lower || amount > upper)
            return ValidationStatus.error(Messages.MsgErrorConvertedAmount);

        if (amount == 0L || fxAmount == 0L)
            return ValidationStatus.error(MessageFormat.format(Messages.MsgDialogInputRequired, Messages.ColumnTotal));

        return ValidationStatus.ok();
    }

    public Account getSourceAccount()
    {
        return sourceAccount;
    }

    public void setSourceAccount(Account account)
    {
        String oldCurrencyCode = getSourceAccountCurrency();
        String oldExchangeRateCurrencies = getExchangeRateCurrencies();
        String oldInverseExchangeRateCurrencies = getInverseExchangeRateCurrencies();

        firePropertyChange(Properties.sourceAccount.name(), this.sourceAccount, this.sourceAccount = account);

        firePropertyChange(Properties.sourceAccountCurrency.name(), oldCurrencyCode, getSourceAccountCurrency());
        firePropertyChange(Properties.exchangeRateCurrencies.name(), oldExchangeRateCurrencies,
                        getExchangeRateCurrencies());
        firePropertyChange(Properties.inverseExchangeRateCurrencies.name(), oldInverseExchangeRateCurrencies,
                        getInverseExchangeRateCurrencies());

        updateExchangeRate();
    }

    public Account getTargetAccount()
    {
        return targetAccount;
    }

    public void setTargetAccount(Account account)
    {
        String oldCurrencyCode = getTargetAccountCurrency();
        String oldExchangeRateCurrencies = getExchangeRateCurrencies();
        String oldInverseExchangeRateCurrencies = getInverseExchangeRateCurrencies();

        firePropertyChange(Properties.targetAccount.name(), this.targetAccount, this.targetAccount = account);

        firePropertyChange(Properties.targetAccountCurrency.name(), oldCurrencyCode, getTargetAccountCurrency());
        firePropertyChange(Properties.exchangeRateCurrencies.name(), oldExchangeRateCurrencies,
                        getExchangeRateCurrencies());
        firePropertyChange(Properties.inverseExchangeRateCurrencies.name(), oldInverseExchangeRateCurrencies,
                        getInverseExchangeRateCurrencies());

        updateExchangeRate();
    }

    private void updateExchangeRate()
    {
        if (getSourceAccountCurrency().equals(getTargetAccountCurrency()))
        {
            setExchangeRate(BigDecimal.ONE);
        }
        else
        {
            ExchangeRateTimeSeries series = getExchangeRateProviderFactory() //
                            .getTimeSeries(getSourceAccountCurrency(), getTargetAccountCurrency());

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

    public String getSourceAccountCurrency()
    {
        return sourceAccount != null ? sourceAccount.getCurrencyCode() : ""; //$NON-NLS-1$
    }

    public String getTargetAccountCurrency()
    {
        return targetAccount != null ? targetAccount.getCurrencyCode() : ""; //$NON-NLS-1$
    }

    /**
     * Returns exchange rate label in direct (price) notation.
     */
    public String getExchangeRateCurrencies()
    {
        return String.format("%s/%s", getSourceAccountCurrency(), getTargetAccountCurrency()); //$NON-NLS-1$
    }

    /**
     * Returns exchange rate label in indirect (quantity) notation.
     */
    public String getInverseExchangeRateCurrencies()
    {
        return String.format("%s/%s", getTargetAccountCurrency(), getSourceAccountCurrency()); //$NON-NLS-1$
    }
}
