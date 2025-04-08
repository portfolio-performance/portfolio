package name.abuchen.portfolio.snapshot.filter;

import java.util.Objects;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;

public class ReadOnlyAccount extends Account
{
    private final Account source;

    ReadOnlyAccount(Account source)
    {
        super(source.getName());
        this.setCurrencyCode(source.getCurrencyCode());
        this.source = Objects.requireNonNull(source);
        this.setAttributes(source.getAttributes());
    }

    public Account unwrap()
    {
        return source instanceof ReadOnlyAccount readOnly ? readOnly.unwrap() : source;
    }

    public Account getSource()
    {
        return source;
    }

    @Override
    public void addTransaction(AccountTransaction transaction)
    {
        throw new UnsupportedOperationException();
    }

    void internalAddTransaction(AccountTransaction transaction)
    {
        super.addTransaction(transaction);
    }

    @Override
    public void shallowDeleteTransaction(AccountTransaction transaction, Client client)
    {
        throw new UnsupportedOperationException();
    }

    public static Account unwrap(Account account)
    {
        return account instanceof ReadOnlyAccount readOnly ? unwrap(readOnly.source) : account;
    }

    @Override
    public String getName()
    {
        return source.getName();
    }

    @Override
    public void setName(String name)
    {
        source.setName(name);
    }

    @Override
    public String getNote()
    {
        return source.getNote();
    }

    @Override
    public void setNote(String note)
    {
        source.setNote(note);
    }
}
