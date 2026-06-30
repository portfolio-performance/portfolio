package name.abuchen.portfolio.model.ledger.compatibility;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Carries dividend data for Ledger compatibility creators, editors, or patchers.
 * This is a compatibility-layer value object. Contributor code may pass it to Ledger write
 * paths, but it does not mutate Ledger truth by itself.
 */
public final class LedgerDividend
{
    private final LedgerAccountCashLeg cashLeg;
    private final LedgerOptionalSecurity security;
    private final LedgerCreationUnits units;
    private final long shares;
    private final LocalDateTime exDate;

    private LedgerDividend(LedgerAccountCashLeg cashLeg, LedgerOptionalSecurity security, LedgerCreationUnits units,
                    long shares, LocalDateTime exDate)
    {
        this.cashLeg = Objects.requireNonNull(cashLeg);
        this.security = Objects.requireNonNull(security);
        this.units = Objects.requireNonNull(units);
        this.shares = shares;
        this.exDate = exDate;
    }

    public static LedgerDividend withoutExDate(LedgerAccountCashLeg cashLeg, LedgerOptionalSecurity security,
                    LedgerCreationUnits units)
    {
        return withoutExDate(cashLeg, security, units, 0);
    }

    public static LedgerDividend withoutExDate(LedgerAccountCashLeg cashLeg, LedgerOptionalSecurity security,
                    LedgerCreationUnits units, long shares)
    {
        return new LedgerDividend(cashLeg, security, units, shares, null);
    }

    public static LedgerDividend withExDate(LedgerAccountCashLeg cashLeg, LedgerOptionalSecurity security,
                    LedgerCreationUnits units, LocalDateTime exDate)
    {
        return withExDate(cashLeg, security, units, 0, exDate);
    }

    public static LedgerDividend withExDate(LedgerAccountCashLeg cashLeg, LedgerOptionalSecurity security,
                    LedgerCreationUnits units, long shares, LocalDateTime exDate)
    {
        return new LedgerDividend(cashLeg, security, units, shares, Objects.requireNonNull(exDate));
    }

    public LedgerAccountCashLeg getCashLeg()
    {
        return cashLeg;
    }

    public LedgerOptionalSecurity getSecurity()
    {
        return security;
    }

    public LedgerCreationUnits getUnits()
    {
        return units;
    }

    public long getShares()
    {
        return shares;
    }

    public boolean hasExDate()
    {
        return exDate != null;
    }

    public LocalDateTime getExDate()
    {
        return exDate;
    }
}
