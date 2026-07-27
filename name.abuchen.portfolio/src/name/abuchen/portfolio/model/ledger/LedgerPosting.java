package name.abuchen.portfolio.model.ledger;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.ledger.configuration.LedgerPostingType;

/**
 * One leg of a {@link LedgerEntry}: an unsigned amount - and, for instruments, a
 * number of shares - attributed to a cash account, an investment account, or an
 * instrument. Optionally it carries the forex amount and the exchange rate the
 * fact was recorded in.
 * <p>
 * The {@link LedgerPostingType type} says what kind of fact this is: cash,
 * instrument, fee, tax, or gross value. Because amounts are unsigned, the
 * movement is not carried by a sign but by the {@link LedgerPostingDirection};
 * how the posting relates to the rest of the entry is given by its
 * {@link LedgerPostingSemanticRole} and {@link LedgerPostingUnitRole}.
 */
public class LedgerPosting
{
    private String uuid;
    private LedgerPostingType type;
    private long amount;
    private String currency;
    private Long forexAmount;
    private String forexCurrency;
    private BigDecimal exchangeRate;
    private Security security;
    private long shares;
    private Account account;
    private Portfolio portfolio;
    private LedgerPostingSemanticRole semanticRole;
    private LedgerPostingDirection direction;
    private LedgerPostingUnitRole unitRole;
    private final List<LedgerParameter<?>> parameters = new ArrayList<>();

    public LedgerPosting()
    {
        this(UUID.randomUUID().toString());
    }

    public LedgerPosting(String uuid)
    {
        this.uuid = Objects.requireNonNull(uuid);
    }

    public String getUUID()
    {
        return uuid;
    }

    public void setUUID(String uuid)
    {
        this.uuid = Objects.requireNonNull(uuid);
    }

    public LedgerPostingType getType()
    {
        return type;
    }

    public void setType(LedgerPostingType type)
    {
        this.type = type;
    }

    public long getAmount()
    {
        return amount;
    }

    public void setAmount(long amount)
    {
        this.amount = amount;
    }

    public String getCurrency()
    {
        return currency;
    }

    public void setCurrency(String currency)
    {
        this.currency = currency;
    }

    public Long getForexAmount()
    {
        return forexAmount;
    }

    public void setForexAmount(Long forexAmount)
    {
        this.forexAmount = forexAmount;
    }

    public String getForexCurrency()
    {
        return forexCurrency;
    }

    public void setForexCurrency(String forexCurrency)
    {
        this.forexCurrency = forexCurrency;
    }

    public BigDecimal getExchangeRate()
    {
        return exchangeRate;
    }

    public void setExchangeRate(BigDecimal exchangeRate)
    {
        this.exchangeRate = exchangeRate;
    }

    public Security getSecurity()
    {
        return security;
    }

    public void setSecurity(Security security)
    {
        this.security = security;
    }

    public long getShares()
    {
        return shares;
    }

    public void setShares(long shares)
    {
        this.shares = shares;
    }

    public Account getAccount()
    {
        return account;
    }

    public void setAccount(Account account)
    {
        this.account = account;
    }

    public Portfolio getPortfolio()
    {
        return portfolio;
    }

    public void setPortfolio(Portfolio portfolio)
    {
        this.portfolio = portfolio;
    }

    public LedgerPostingSemanticRole getSemanticRole()
    {
        return semanticRole;
    }

    public void setSemanticRole(LedgerPostingSemanticRole semanticRole)
    {
        this.semanticRole = semanticRole;
    }

    public LedgerPostingDirection getDirection()
    {
        return direction;
    }

    public void setDirection(LedgerPostingDirection direction)
    {
        this.direction = direction;
    }

    public LedgerPostingUnitRole getUnitRole()
    {
        return unitRole;
    }

    public void setUnitRole(LedgerPostingUnitRole unitRole)
    {
        this.unitRole = unitRole;
    }

    public List<LedgerParameter<?>> getParameters()
    {
        return Collections.unmodifiableList(parameters);
    }

    public void addParameter(LedgerParameter<?> parameter)
    {
        parameters.add(Objects.requireNonNull(parameter));
    }

    public boolean removeParameter(LedgerParameter<?> parameter)
    {
        return parameters.remove(parameter);
    }
}
