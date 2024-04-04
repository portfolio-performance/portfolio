package name.abuchen.portfolio.model;

import java.time.LocalDateTime;

public class AccountTransferEntry implements CrossEntry, Annotated
{
    private Account accountFrom;
    private AccountTransaction transactionFrom;
    private Account accountTo;
    private AccountTransaction transactionTo;

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
        this.transactionFrom = txFrom;
        this.transactionFrom.setType(AccountTransaction.Type.TRANSFER_OUT);
        this.transactionFrom.setCrossEntry(this);

        this.transactionTo = txTo;
        this.transactionTo.setType(AccountTransaction.Type.TRANSFER_IN);
        this.transactionTo.setCrossEntry(this);

        this.accountFrom = accountFrom;
        this.accountTo = accountTo;
    }

    public void setSourceTransaction(AccountTransaction transaction)
    {
        this.transactionFrom = transaction;
    }

    public void setTargetTransaction(AccountTransaction transaction)
    {
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
        this.accountFrom = account;
    }

    public Account getSourceAccount()
    {
        return accountFrom;
    }

    public void setTargetAccount(Account account)
    {
        this.accountTo = account;
    }

    public Account getTargetAccount()
    {
        return accountTo;
    }

    public void setDate(LocalDateTime date)
    {
        this.transactionFrom.setDateTime(date);
        this.transactionTo.setDateTime(date);
    }

    public void setAmount(long amount)
    {
        this.transactionFrom.setAmount(amount);
        this.transactionTo.setAmount(amount);
    }

    public void setCurrencyCode(String currencyCode)
    {
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
        this.transactionFrom.setSource(source);
        this.transactionTo.setSource(source);
    }

    @Override
    public void insert()
    {
        accountFrom.addTransaction(transactionFrom);
        accountTo.addTransaction(transactionTo);
    }

    @Override
    public void updateFrom(Transaction t)
    {
        if (t == transactionFrom)
            copyAttributesOver(transactionFrom, transactionTo);
        else if (t == transactionTo)
            copyAttributesOver(transactionTo, transactionFrom);
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
}
