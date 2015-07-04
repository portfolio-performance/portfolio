package name.abuchen.portfolio.ui.dialogs.transactions;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransferEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.ForexData;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.model.TransactionOwner;
import name.abuchen.portfolio.money.ExchangeRate;
import name.abuchen.portfolio.money.ExchangeRateTimeSeries;
import name.abuchen.portfolio.ui.Messages;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;

import com.ibm.icu.text.MessageFormat;

public class AccountTransferModel extends AbstractModel
{
    public enum Properties
    {
        sourceAccount, targetAccount, date, fxAmount, exchangeRate, amount, //
        note, sourceAccountCurrency, targetAccountCurrency, exchangeRateCurrencies, calculationStatus;
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
        return Messages.AccountMenuTransfer;
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
                TransactionOwner<Transaction> owner = (TransactionOwner<Transaction>) source.getOwner(source
                                .getSourceTransaction());
                owner.deleteTransaction(source.getSourceTransaction(), client);
                source = null;
            }

            t = new AccountTransferEntry(sourceAccount, targetAccount);
            t.insert();
        }

        t.setDate(date);
        t.setNote(note);

        // if source and target account have the same currencies, no forex data
        // needs to be stored

        if (sourceAccount.getCurrencyCode().equals(targetAccount.getCurrencyCode()))
        {
            t.setAmount(amount);
            t.setCurrencyCode(sourceAccount.getCurrencyCode());
            t.getSourceTransaction().setForex(null);
            t.getTargetTransaction().setForex(null);
        }
        else
        {
            t.getSourceTransaction().setAmount(fxAmount);
            t.getSourceTransaction().setCurrencyCode(sourceAccount.getCurrencyCode());

            t.getTargetTransaction().setAmount(amount);
            t.getTargetTransaction().setCurrencyCode(targetAccount.getCurrencyCode());

            ForexData forex = new ForexData();
            forex.setBaseCurrency(sourceAccount.getCurrencyCode());
            forex.setTermCurrency(targetAccount.getCurrencyCode());
            forex.setExchangeRate(getExchangeRate());
            forex.setBaseAmount(fxAmount);
            t.getTargetTransaction().setForex(forex);
        }
    }

    public void setSource(AccountTransferEntry entry)
    {
        this.source = entry;
        this.sourceAccount = (Account) entry.getOwner(entry.getSourceTransaction());
        this.targetAccount = (Account) entry.getOwner(entry.getTargetTransaction());

        this.date = entry.getSourceTransaction().getDate();
        this.note = entry.getSourceTransaction().getNote();

        this.fxAmount = entry.getSourceTransaction().getAmount();
        this.amount = entry.getTargetTransaction().getAmount();

        ForexData forex = entry.getTargetTransaction().getForex();
        if (forex != null && forex.getBaseCurrency().equals(sourceAccount.getCurrencyCode())
                        && forex.getTermCurrency().equals(targetAccount.getCurrencyCode()))
        {
            this.exchangeRate = forex.getExchangeRate();
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
        firePropertyChange(Properties.sourceAccount.name(), this.sourceAccount, this.sourceAccount = account);
        firePropertyChange(Properties.sourceAccountCurrency.name(), oldCurrencyCode, getSourceAccountCurrency());
        firePropertyChange(Properties.exchangeRateCurrencies.name(), oldExchangeRateCurrencies,
                        getExchangeRateCurrencies());

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
        firePropertyChange(Properties.targetAccount.name(), this.targetAccount, this.targetAccount = account);
        firePropertyChange(Properties.targetAccountCurrency.name(), oldCurrencyCode, getTargetAccountCurrency());
        firePropertyChange(Properties.exchangeRateCurrencies.name(), oldExchangeRateCurrencies,
                        getExchangeRateCurrencies());

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
                            .getTimeSeries(getTargetAccountCurrency(), getSourceAccountCurrency());

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

        firePropertyChange(Properties.exchangeRate.name(), this.exchangeRate, this.exchangeRate = newRate);

        triggerAmount(Math.round(newRate.doubleValue() * fxAmount));

        firePropertyChange(Properties.calculationStatus.name(), this.calculationStatus,
                        this.calculationStatus = calculateStatus());
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
            firePropertyChange(Properties.exchangeRate.name(), this.exchangeRate, this.exchangeRate = newExchangeRate);
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

    public String getExchangeRateCurrencies()
    {
        return String.format("%s/%s", getTargetAccountCurrency(), getSourceAccountCurrency()); //$NON-NLS-1$
    }
}
