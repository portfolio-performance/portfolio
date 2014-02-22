package name.abuchen.portfolio.model;

import java.util.Date;

public class AccountTransferEntry implements CrossEntry
{
    private Account accountFrom;
    private AccountTransaction transactionFrom;
    private Account accountTo;
    private AccountTransaction transactionTo;

    public AccountTransferEntry()
    {}

    public AccountTransferEntry(Account accountFrom, Account accountTo)
    {
        this.accountFrom = accountFrom;
        this.transactionFrom = new AccountTransaction();
        this.transactionFrom.setType(AccountTransaction.Type.TRANSFER_OUT);
        this.transactionFrom.setCrossEntry(this);

        this.accountTo = accountTo;
        this.transactionTo = new AccountTransaction();
        this.transactionTo.setType(AccountTransaction.Type.TRANSFER_IN);
        this.transactionTo.setCrossEntry(this);
    }

    public void setDate(Date date)
    {
        this.transactionFrom.setDate(date);
        this.transactionTo.setDate(date);
    }

    public void setAmount(long amount)
    {
        this.transactionFrom.setAmount(amount);
        this.transactionTo.setAmount(amount);
    }

    public void insert()
    {
        accountFrom.addTransaction(transactionFrom);
        accountTo.addTransaction(transactionTo);
    }

    @Override
    public void delete()
    {
        accountFrom.getTransactions().remove(transactionFrom);
        accountTo.getTransactions().remove(transactionTo);
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
        target.setDate(source.getDate());
        target.setAmount(source.getAmount());
        target.setNote(source.getNote());
    }

    @Override
    public Object getEntity(Transaction t)
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
    public Object getCrossEntity(Transaction t)
    {
        if (t.equals(transactionFrom))
            return accountTo;
        else if (t.equals(transactionTo))
            return accountFrom;
        else
            throw new UnsupportedOperationException();
    }
}
