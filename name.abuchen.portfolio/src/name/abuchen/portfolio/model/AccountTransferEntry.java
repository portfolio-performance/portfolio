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
        this.transactionFrom = new AccountTransaction();
        this.transactionFrom.setType(AccountTransaction.Type.TRANSFER_OUT);
        this.transactionFrom.setCrossEntry(this);

        this.transactionTo = new AccountTransaction();
        this.transactionTo.setType(AccountTransaction.Type.TRANSFER_IN);
        this.transactionTo.setCrossEntry(this);
    }

    public AccountTransferEntry(Account accountFrom, Account accountTo)
    {
        this();
        this.accountFrom = accountFrom;
        this.accountTo = accountTo;
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
