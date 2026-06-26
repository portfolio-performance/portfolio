package name.abuchen.portfolio.model.ledger.compatibility;

import java.util.Objects;

import name.abuchen.portfolio.model.Security;

/**
 * Carries optional security data for Ledger compatibility creators, editors, or patchers.
 * This is a compatibility-layer value object. Contributor code may pass it to Ledger write
 * paths, but it does not mutate Ledger truth by itself.
 */
public final class LedgerOptionalSecurity
{
    private static final LedgerOptionalSecurity NONE = new LedgerOptionalSecurity(null);

    private final Security security;

    private LedgerOptionalSecurity(Security security)
    {
        this.security = security;
    }

    public static LedgerOptionalSecurity none()
    {
        return NONE;
    }

    public static LedgerOptionalSecurity of(Security security)
    {
        return new LedgerOptionalSecurity(Objects.requireNonNull(security));
    }

    public boolean isPresent()
    {
        return security != null;
    }

    public Security getSecurity()
    {
        return security;
    }
}
