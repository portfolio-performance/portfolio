package name.abuchen.portfolio.junit;

import java.time.LocalDateTime;
import java.util.Objects;

import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.PortfolioTransaction.Type;
import name.abuchen.portfolio.model.Security;

public class PortfolioTransactionBuilder
{
    private Type txType;
    private LocalDateTime datetime;
    private String currencyCode;
    private long amount = 0;
    private long shares = 0;
    private Security security;
    private long fees = 0;
    private long taxes = 0;

    public PortfolioTransactionBuilder(Type txType)
    {
        this.txType = txType;
    }

    public PortfolioTransactionBuilder transactionAt(LocalDateTime dateTime)
    {
        datetime = dateTime;
        return this;
    }

    public PortfolioTransactionBuilder withCurrency(String code)
    {
        currencyCode = code;
        return this;
    }

    public PortfolioTransactionBuilder withAmountOf(long amount)
    {
        this.amount = amount;
        return this;
    }

    public PortfolioTransactionBuilder numberOfShares(long shares)
    {
        this.shares = shares;
        return this;
    }

    public PortfolioTransactionBuilder forSecurity(Security security)
    {
        this.security = security;
        return this;
    }

    public PortfolioTransactionBuilder withCostsOf(long fees)
    {
        this.fees = fees;
        return this;
    }

    public PortfolioTransactionBuilder withTaxAmountOf(long taxes)
    {
        this.taxes = taxes;
        return this;
    }

    public PortfolioTransaction build()
    {
        Objects.requireNonNull(datetime);
        Objects.requireNonNull(currencyCode);
        Objects.requireNonNull(amount);
        Objects.requireNonNull(security);
        Objects.requireNonNull(shares);
        return new PortfolioTransaction(datetime, currencyCode, amount, security, shares, txType, fees, taxes);
    }
}
