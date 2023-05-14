package name.abuchen.portfolio.junit;

import java.time.LocalDate;
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
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;

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

    public AccountBuilder deposit_(LocalDateTime date, long amount)
    {
        return transaction(Type.DEPOSIT, date, amount);
    }

    public AccountBuilder interest(String date, long amount)
    {
        return transaction(Type.INTEREST, date, amount);
    }

    public AccountBuilder interest(String date, long amount, long taxes)
    {
        return transaction(Type.INTEREST, date, amount, taxes, null);
    }

    public AccountBuilder interest(LocalDateTime date, long amount)
    {
        return transaction(Type.INTEREST, date, amount);
    }

    public AccountBuilder interest_charge(String date, long amount)
    {
        return transaction(Type.INTEREST_CHARGE, date, amount);
    }

    public AccountBuilder interest_charge(LocalDateTime date, long amount)
    {
        return transaction(Type.INTEREST_CHARGE, date, amount);
    }

    public AccountBuilder fees____(String date, long amount)
    {
        return transaction(Type.FEES, date, amount);
    }

    public AccountBuilder fees____(LocalDateTime date, long amount)
    {
        return transaction(Type.FEES, date, amount);
    }

    public AccountBuilder fees_refund(String date, long amount)
    {
        return transaction(Type.FEES_REFUND, date, amount);
    }

    public AccountBuilder tax_____(String date, long amount)
    {
        return transaction(Type.TAXES, date, amount);
    }

    public AccountBuilder taxrefnd(String date, long amount)
    {
        return transaction(Type.TAX_REFUND, date, amount);
    }

    public AccountBuilder withdraw(String date, long amount)
    {
        return transaction(Type.REMOVAL, date, amount);
    }

    public AccountBuilder withdraw(LocalDateTime date, long amount)
    {
        return transaction(Type.REMOVAL, date, amount);
    }

    public AccountBuilder dividend(String date, long amount, Security security)
    {
        return transaction(Type.DIVIDENDS, date, amount, 0L, security);
    }

    public AccountBuilder dividend(String date, long amount, long taxes, Security security)
    {
        return transaction(Type.DIVIDENDS, date, amount, taxes, security);
    }

    private AccountBuilder transaction(Type type, String date, long amount)
    {
        return transaction(type, asDateTime(date), amount, 0L, null);
    }

    private AccountBuilder transaction(Type type, LocalDateTime date, long amount)
    {
        return transaction(type, date, amount, 0L, null);
    }

    private AccountBuilder transaction(Type type, String date, long amount, long taxes, Security security)
    {
        return transaction(type, asDateTime(date), amount, taxes, security);
    }

    private AccountBuilder transaction(Type type, LocalDateTime date, long amount, long taxes, Security security)
    {
        AccountTransaction t = new AccountTransaction(date, account.getCurrencyCode(), amount, security, type);
        if (taxes > 0)
            t.addUnit(new Unit(Unit.Type.TAX, Money.of(account.getCurrencyCode(), taxes)));
        account.addTransaction(t);
        return this;
    }

    public AccountBuilder assign(Taxonomy taxonomy, String id, int weight)
    {
        Classification classification = taxonomy.getClassificationById(id);
        classification.addAssignment(new Assignment(account, weight));
        return this;
    }

    /* package */ static LocalDateTime asDateTime(String date)
    {
        return date.indexOf('T') < 0 ? LocalDate.parse(date).atStartOfDay() : LocalDateTime.parse(date);
    }
}
