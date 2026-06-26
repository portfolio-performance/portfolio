package name.abuchen.portfolio.model;

import java.time.LocalDateTime;
import java.util.Objects;

public class AccountTransferEntry implements CrossEntry, Annotated
{
    private Account accountFrom;
    private AccountTransaction transactionFrom;
    private Account accountTo;
    private AccountTransaction transactionTo;
    private boolean readOnly;

    public AccountTransferEntry()
    {
        this(null, new AccountTransaction(), null, new AccountTransaction());
    }

    public AccountTransferEntry(Account accountFrom, Account accountTo)
    {
        this(accountFrom, new AccountTransaction(), accountTo, new AccountTransaction());
    }

    /* protobuf only */ AccountTransferEntry(Account accountFrom, AccountTransaction txFrom, Account accountTo,
                    AccountTransaction txTo)
    {
        this(accountFrom, txFrom, accountTo, txTo, false);
    }

    private AccountTransferEntry(Account accountFrom, AccountTransaction txFrom, Account accountTo,
                    AccountTransaction txTo, boolean readOnly)
    {
        this.transactionFrom = txFrom;
        this.transactionTo = txTo;
        this.accountFrom = accountFrom;
        this.accountTo = accountTo;
        this.readOnly = readOnly;

        if (!readOnly)
        {
            this.transactionFrom.setType(AccountTransaction.Type.TRANSFER_OUT);
            this.transactionFrom.setCrossEntry(this);

            this.transactionTo.setType(AccountTransaction.Type.TRANSFER_IN);
            this.transactionTo.setCrossEntry(this);
        }
    }

    public static AccountTransferEntry readOnly(Account accountFrom, AccountTransaction txFrom, Account accountTo,
                    AccountTransaction txTo)
    {
        return new AccountTransferEntry(accountFrom, txFrom, accountTo, txTo, true);
    }

    public void setSourceTransaction(AccountTransaction transaction)
    {
        assertWritable();

        this.transactionFrom = transaction;
    }

    public void setTargetTransaction(AccountTransaction transaction)
    {
        assertWritable();

        this.transactionTo = transaction;
    }

    public AccountTransaction getSourceTransaction()
    {
        return this.transactionFrom;
    }

    public AccountTransaction getTargetTransaction()
    {
        return this.transactionTo;
    }

    public void setSourceAccount(Account account)
    {
        assertWritable();

        this.accountFrom = account;
    }

    public Account getSourceAccount()
    {
        return accountFrom;
    }

    public void setTargetAccount(Account account)
    {
        assertWritable();

        this.accountTo = account;
    }

    public Account getTargetAccount()
    {
        return accountTo;
    }

    public void setDate(LocalDateTime date)
    {
        assertWritable();

        this.transactionFrom.setDateTime(date);
        this.transactionTo.setDateTime(date);
    }

    public void setAmount(long amount)
    {
        assertWritable();

        this.transactionFrom.setAmount(amount);
        this.transactionTo.setAmount(amount);
    }

    public void setCurrencyCode(String currencyCode)
    {
        assertWritable();

        this.transactionFrom.setCurrencyCode(currencyCode);
        this.transactionTo.setCurrencyCode(currencyCode);
    }

    @Override
    public String getNote()
    {
        return this.transactionFrom.getNote();
    }

    @Override
    public void setNote(String note)
    {
        assertWritable();

        this.transactionFrom.setNote(note);
        this.transactionTo.setNote(note);
    }

    @Override
    public String getSource()
    {
        return this.transactionFrom.getSource();
    }

    @Override
    public void setSource(String source)
    {
        assertWritable();

        this.transactionFrom.setSource(source);
        this.transactionTo.setSource(source);
    }

    @Override
    public void insert()
    {
        assertWritable();

        // perform both currency checks *before* adding the transactions to
        // avoid partially added transfer

        if (!Objects.equals(accountFrom.getCurrencyCode(), transactionFrom.getCurrencyCode()))
        {
            throw new IllegalArgumentException(
                            "Unable to add transaction '" + transactionFrom.toString() + "' to account '" //$NON-NLS-1$ //$NON-NLS-2$
                                            + accountFrom.getName() + "' (uuid " + transactionFrom.getUUID() + "): " //$NON-NLS-1$ //$NON-NLS-2$
                                            + accountFrom.getCurrencyCode() + " <> " //$NON-NLS-1$
                                            + transactionFrom.getCurrencyCode());
        }

        if (!Objects.equals(accountTo.getCurrencyCode(), transactionTo.getCurrencyCode()))
        {
            throw new IllegalArgumentException(
                            "Unable to add transaction '" + transactionTo.toString() + "' to account '" //$NON-NLS-1$ //$NON-NLS-2$
                                            + accountTo.getName() + "' (uuid " + transactionTo.getUUID() + "): " //$NON-NLS-1$ //$NON-NLS-2$
                                            + accountTo.getCurrencyCode() + " <> " //$NON-NLS-1$
                                            + transactionTo.getCurrencyCode());
        }

        accountFrom.addTransaction(transactionFrom);
        accountTo.addTransaction(transactionTo);
    }

    @Override
    public void updateFrom(Transaction t)
    {
        if (t == transactionFrom)
        {
            if (readOnly)
                return;

            copyAttributesOver(transactionFrom, transactionTo);
        }
        else if (t == transactionTo)
        {
            if (readOnly)
                return;

            copyAttributesOver(transactionTo, transactionFrom);
        }
        else
            throw new UnsupportedOperationException();
    }

    private void copyAttributesOver(AccountTransaction source, AccountTransaction target)
    {
        target.setDateTime(source.getDateTime());
        target.setNote(source.getNote());
    }

    @Override
    public TransactionOwner<? extends Transaction> getOwner(Transaction t)
    {
        if (t.equals(transactionFrom))
            return accountFrom;
        else if (t.equals(transactionTo))
            return accountTo;
        else
            throw new UnsupportedOperationException();
    }

    @Override
    public void setOwner(Transaction t, TransactionOwner<? extends Transaction> owner)
    {
        assertWritable();

        if (!(owner instanceof Account))
            throw new IllegalArgumentException("owner isn't an account for transaction " + t); //$NON-NLS-1$

        if (t.equals(transactionFrom) && !accountTo.equals(owner))
            accountFrom = (Account) owner;
        else if (t.equals(transactionTo) && !accountFrom.equals(owner))
            accountTo = (Account) owner;
        else
            throw new IllegalArgumentException("unable to set owner for transaction " + t); //$NON-NLS-1$
    }

    @Override
    public Transaction getCrossTransaction(Transaction t)
    {
        if (t.equals(transactionFrom))
            return transactionTo;
        else if (t.equals(transactionTo))
            return transactionFrom;
        else
            throw new UnsupportedOperationException();
    }

    @Override
    public TransactionOwner<? extends Transaction> getCrossOwner(Transaction t)
    {
        if (t.equals(transactionFrom))
            return accountTo;
        else if (t.equals(transactionTo))
            return accountFrom;
        else
            throw new UnsupportedOperationException();
    }

    private void assertWritable()
    {
        if (readOnly)
            throw new UnsupportedOperationException("Ledger-backed account transfer cross entries are read-only"); //$NON-NLS-1$
    }
}
