package name.abuchen.portfolio;

import java.util.UUID;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.AccountTransaction.Type;
import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.model.Classification.Assignment;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Taxonomy;
import name.abuchen.portfolio.money.CurrencyUnit;

import org.joda.time.DateMidnight;

public class AccountBuilder
{
    private Account account;

    public AccountBuilder()
    {
        this(CurrencyUnit.EUR);
    }

    public AccountBuilder(String currencyCode)
    {
        this.account = new Account();
        this.account.setName(UUID.randomUUID().toString());
        this.account.setCurrencyCode(currencyCode);
    }

    public Account addTo(Client client)
    {
        client.addAccount(account);
        return account;
    }

    public AccountBuilder deposit_(String date, long amount)
    {
        return transaction(Type.DEPOSIT, date, amount);
    }

    public AccountBuilder deposit_(DateMidnight date, long amount)
    {
        return transaction(Type.DEPOSIT, date, amount);
    }

    public AccountBuilder interest(String date, long amount)
    {
        return transaction(Type.INTEREST, date, amount);
    }

    public AccountBuilder interest(DateMidnight date, long amount)
    {
        return transaction(Type.INTEREST, date, amount);
    }

    public AccountBuilder fees____(String date, long amount)
    {
        return transaction(Type.FEES, date, amount);
    }

    public AccountBuilder fees____(DateMidnight date, long amount)
    {
        return transaction(Type.FEES, date, amount);
    }

    public AccountBuilder withdraw(String date, long amount)
    {
        return transaction(Type.REMOVAL, date, amount);
    }

    public AccountBuilder withdraw(DateMidnight date, long amount)
    {
        return transaction(Type.REMOVAL, date, amount);
    }

    public AccountBuilder dividend(String date, long amount, Security security)
    {
        AccountTransaction t = new AccountTransaction(new DateMidnight(date).toDate(), account.getCurrencyCode(),
                        amount, security, Type.DIVIDENDS);
        account.addTransaction(t);
        return this;
    }

    private AccountBuilder transaction(Type type, String date, long amount)
    {
        return transaction(type, new DateMidnight(date), amount);
    }

    private AccountBuilder transaction(Type type, DateMidnight date, long amount)
    {
        AccountTransaction t = new AccountTransaction(date.toDate(), account.getCurrencyCode(), amount, null, type);
        account.addTransaction(t);
        return this;
    }

    public AccountBuilder assign(Taxonomy taxonomy, String id, int weight)
    {
        Classification classification = taxonomy.getClassificationById(id);
        classification.addAssignment(new Assignment(account, weight));
        return this;
    }

}
