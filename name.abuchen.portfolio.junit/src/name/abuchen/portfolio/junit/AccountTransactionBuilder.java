package name.abuchen.portfolio.junit;

import java.time.LocalDateTime;
import java.util.Objects;

import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.AccountTransaction.Type;
import name.abuchen.portfolio.model.Security;

public class AccountTransactionBuilder
{
    private Type txType;
    private LocalDateTime dateTime;
    private String currencyCode;
    private long amount;
    private Security security;

    public AccountTransactionBuilder(Type txType)
    {
        this.txType = txType;
    }

    public AccountTransactionBuilder transactionAt(LocalDateTime dateTime)
    {
        this.dateTime = dateTime;
        return this;
    }

    public AccountTransactionBuilder withCurrency(String currencyCode)
    {
        this.currencyCode = currencyCode;
        return this;
    }

    public AccountTransactionBuilder withAmountOf(long amount)
    {
        this.amount = amount;
        return this;
    }

    public AccountTransactionBuilder forSecurity(Security security)
    {
        this.security = security;
        return this;
    }

    public AccountTransaction build()
    {
        Objects.requireNonNull(dateTime);
        Objects.requireNonNull(currencyCode);
        Objects.requireNonNull(amount);
        Objects.requireNonNull(security);
        return new AccountTransaction(dateTime, currencyCode, amount, security, txType);
    }

}
