package name.abuchen.portfolio.model.ledger.compatibility;

import java.util.Objects;

import name.abuchen.portfolio.model.Security;

/**
 * Carries portfolio transfer security data for Ledger compatibility creators, editors, or patchers.
 * This is a compatibility-layer value object. Contributor code may pass it to Ledger write
 * paths, but it does not mutate Ledger truth by itself.
 */
public final class LedgerPortfolioTransferSecurity
{
    private final Security security;
    private final long shares;

    private LedgerPortfolioTransferSecurity(Security security, long shares)
    {
        this.security = Objects.requireNonNull(security);
        this.shares = shares;
    }

    public static LedgerPortfolioTransferSecurity of(Security security, long shares)
    {
        return new LedgerPortfolioTransferSecurity(security, shares);
    }

    public Security getSecurity()
    {
        return security;
    }

    public long getShares()
    {
        return shares;
    }
}
