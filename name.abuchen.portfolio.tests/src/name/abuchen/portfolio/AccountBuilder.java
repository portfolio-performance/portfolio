package name.abuchen.portfolio;

import java.time.LocalDateTime;
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

    public AccountBuilder deposit_(LocalDateTime date, long amount)
    {
        return transaction(Type.DEPOSIT, date, amount);
    }

    public AccountBuilder interest(LocalDateTime date, long amount)
    {
        return transaction(Type.INTEREST, date, amount);
    }
    
    public AccountBuilder interest_charge(LocalDateTime date, long amount)
    {
        return transaction(Type.INTEREST_CHARGE, date, amount);
    }

    public AccountBuilder fees____(LocalDateTime date, long amount)
    {
        return transaction(Type.FEES, date, amount);
    }

    public AccountBuilder fees_refund(LocalDateTime date, long amount)
    {
        return transaction(Type.FEES_REFUND, date, amount);
    }

    public AccountBuilder withdraw(LocalDateTime date, long amount)
    {
        return transaction(Type.REMOVAL, date, amount);
    }

    public AccountBuilder dividend(LocalDateTime date, long amount, Security security)
    {
        return transaction(Type.DIVIDENDS, date, security, amount);
    }

    private AccountBuilder transaction(Type type, LocalDateTime date, long amount)
    {
        return transaction(type, date, null, amount);
    }
    
    private AccountBuilder transaction(Type type, LocalDateTime date, Security security, long amount)
    {
        AccountTransaction t = new AccountTransaction(date, account.getCurrencyCode(), amount, security, type);
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
