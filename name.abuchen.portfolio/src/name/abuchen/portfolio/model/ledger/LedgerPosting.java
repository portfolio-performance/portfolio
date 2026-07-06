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
import name.abuchen.portfolio.model.ledger.configuration.CorporateActionLeg;
import name.abuchen.portfolio.model.ledger.configuration.LedgerPostingType;

/**
 * Represents one persisted posting inside a Ledger entry.
 * This is an internal Ledger model type for cash, security, fee, tax, and related facts.
 * Contributor code should write postings through Ledger creators, editors, or converters.
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
    private CorporateActionLeg corporateActionLeg;
    private LedgerPostingUnitRole unitRole;
    private String groupKey;
    private String localKey;
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

    public CorporateActionLeg getCorporateActionLeg()
    {
        return corporateActionLeg;
    }

    public void setCorporateActionLeg(CorporateActionLeg corporateActionLeg)
    {
        this.corporateActionLeg = corporateActionLeg;
    }

    public LedgerPostingUnitRole getUnitRole()
    {
        return unitRole;
    }

    public void setUnitRole(LedgerPostingUnitRole unitRole)
    {
        this.unitRole = unitRole;
    }

    public String getGroupKey()
    {
        return groupKey;
    }

    public void setGroupKey(String groupKey)
    {
        this.groupKey = groupKey;
    }

    public String getLocalKey()
    {
        return localKey;
    }

    public void setLocalKey(String localKey)
    {
        this.localKey = localKey;
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
