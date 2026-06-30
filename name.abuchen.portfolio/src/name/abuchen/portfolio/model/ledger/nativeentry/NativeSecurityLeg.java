package name.abuchen.portfolio.model.ledger.nativeentry;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.ledger.LedgerProjectionRole;
import name.abuchen.portfolio.model.ledger.configuration.CorporateActionLeg;
import name.abuchen.portfolio.model.ledger.configuration.LedgerParameterType;
import name.abuchen.portfolio.model.ledger.configuration.LedgerPostingType;
import name.abuchen.portfolio.money.Money;

/**
 * Carries native security leg input for ledger-native entry assembly.
 * This is internal native-entry infrastructure. It describes facts for assembly and does not
 * mutate Ledger truth by itself.
 *
 * <p>
 * This input object is not persisted directly. The assembler converts it into Ledger
 * security postings, controlled leg codes, and projection refs.
 * </p>
 */
public final class NativeSecurityLeg
{
    private final LedgerPostingType postingType;
    private final String legCode;
    private final Portfolio portfolio;
    private final Security security;
    private final long shares;
    private final Money amount;
    private final LedgerProjectionRole projectionRole;
    private final List<NativeParameterValue> parameters;

    private NativeSecurityLeg(Builder builder)
    {
        this.postingType = Objects.requireNonNull(builder.postingType);
        this.legCode = builder.legCode;
        this.portfolio = builder.portfolio;
        this.security = builder.security;
        this.shares = builder.shares;
        this.amount = builder.amount;
        this.projectionRole = builder.projectionRole;
        this.parameters = List.copyOf(builder.parameters);
    }

    public static Builder source()
    {
        return new Builder(LedgerPostingType.SECURITY, CorporateActionLeg.SOURCE_SECURITY.getCode(),
                        LedgerProjectionRole.OLD_SECURITY_LEG);
    }

    public static Builder target()
    {
        return new Builder(LedgerPostingType.SECURITY, CorporateActionLeg.TARGET_SECURITY.getCode(),
                        LedgerProjectionRole.NEW_SECURITY_LEG);
    }

    static Builder ofType(LedgerPostingType postingType)
    {
        return new Builder(postingType, CorporateActionLeg.SOURCE_SECURITY.getCode(),
                        LedgerProjectionRole.OLD_SECURITY_LEG);
    }

    LedgerPostingType getPostingType()
    {
        return postingType;
    }

    String getLegCode()
    {
        return legCode;
    }

    Portfolio getPortfolio()
    {
        return portfolio;
    }

    Security getSecurity()
    {
        return security;
    }

    long getShares()
    {
        return shares;
    }

    Money getAmount()
    {
        return amount;
    }

    LedgerProjectionRole getProjectionRole()
    {
        return projectionRole;
    }

    List<NativeParameterValue> getParameters()
    {
        return parameters;
    }

    public static final class Builder
    {
        private LedgerPostingType postingType;
        private String legCode;
        private Portfolio portfolio;
        private Security security;
        private long shares;
        private Money amount;
        private LedgerProjectionRole projectionRole;
        private final List<NativeParameterValue> parameters = new ArrayList<>();

        private Builder(LedgerPostingType postingType, String legCode, LedgerProjectionRole projectionRole)
        {
            this.postingType = postingType;
            this.legCode = legCode;
            this.projectionRole = projectionRole;
        }

        public Builder portfolio(Portfolio portfolio)
        {
            this.portfolio = portfolio;
            return this;
        }

        public Builder security(Security security)
        {
            this.security = security;
            return this;
        }

        public Builder shares(long shares)
        {
            this.shares = shares;
            return this;
        }

        public Builder amount(Money amount)
        {
            this.amount = amount;
            return this;
        }

        public Builder sourceSecurity(Security security)
        {
            return parameter(LedgerParameterType.SOURCE_SECURITY, security);
        }

        public Builder targetSecurity(Security security)
        {
            return parameter(LedgerParameterType.TARGET_SECURITY, security);
        }

        public Builder ratio(Ratio ratio)
        {
            Objects.requireNonNull(ratio);
            parameter(LedgerParameterType.RATIO_NUMERATOR, ratio.getNumerator());
            return parameter(LedgerParameterType.RATIO_DENOMINATOR, ratio.getDenominator());
        }

        public Builder projectAs(LedgerProjectionRole projectionRole)
        {
            this.projectionRole = projectionRole;
            return this;
        }

        Builder postingType(LedgerPostingType postingType)
        {
            this.postingType = postingType;
            return this;
        }

        Builder legCode(String legCode)
        {
            this.legCode = legCode;
            return this;
        }

        Builder parameter(LedgerParameterType type, Object value)
        {
            parameters.add(new NativeParameterValue(type, value));
            return this;
        }

        public NativeSecurityLeg build()
        {
            return new NativeSecurityLeg(this);
        }
    }
}
