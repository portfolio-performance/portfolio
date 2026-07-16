package name.abuchen.portfolio.model.ledger.configuration;

import java.util.Objects;
import java.util.Optional;

import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.ledger.LedgerProjectionRole;

/**
 * Defines how a native Ledger leg becomes visible in legacy transaction views.
 * This is Java configuration only; it is not persisted as transaction truth.
 */
public final class LedgerLegProjection
{
    public enum Target
    {
        NONE,
        ACCOUNT_TRANSACTION,
        PORTFOLIO_TRANSACTION
    }

    private static final LedgerLegProjection NONE = new LedgerLegProjection(Target.NONE, null, null, null, false,
                    false);

    private final Target target;
    private final LedgerProjectionRole role;
    private final AccountTransaction.Type accountTransactionType;
    private final PortfolioTransaction.Type portfolioTransactionType;
    private final boolean primaryPostingExpected;
    private final boolean postingGroupExpected;

    private LedgerLegProjection(Target target, LedgerProjectionRole role,
                    AccountTransaction.Type accountTransactionType,
                    PortfolioTransaction.Type portfolioTransactionType, boolean primaryPostingExpected,
                    boolean postingGroupExpected)
    {
        this.target = Objects.requireNonNull(target);
        this.role = role;
        this.accountTransactionType = accountTransactionType;
        this.portfolioTransactionType = portfolioTransactionType;
        this.primaryPostingExpected = primaryPostingExpected;
        this.postingGroupExpected = postingGroupExpected;
    }

    public static LedgerLegProjection none()
    {
        return NONE;
    }

    public static LedgerLegProjection account(LedgerProjectionRole role, AccountTransaction.Type type,
                    boolean primaryPostingExpected, boolean postingGroupExpected)
    {
        return new LedgerLegProjection(Target.ACCOUNT_TRANSACTION, Objects.requireNonNull(role),
                        Objects.requireNonNull(type), null, primaryPostingExpected, postingGroupExpected);
    }

    public static LedgerLegProjection portfolio(LedgerProjectionRole role, PortfolioTransaction.Type type,
                    boolean primaryPostingExpected, boolean postingGroupExpected)
    {
        return new LedgerLegProjection(Target.PORTFOLIO_TRANSACTION, Objects.requireNonNull(role), null,
                        Objects.requireNonNull(type), primaryPostingExpected, postingGroupExpected);
    }

    public Target getTarget()
    {
        return target;
    }

    public Optional<LedgerProjectionRole> getRole()
    {
        return Optional.ofNullable(role);
    }

    public Optional<AccountTransaction.Type> getAccountTransactionType()
    {
        return Optional.ofNullable(accountTransactionType);
    }

    public Optional<PortfolioTransaction.Type> getPortfolioTransactionType()
    {
        return Optional.ofNullable(portfolioTransactionType);
    }

    public boolean isProjecting()
    {
        return target != Target.NONE;
    }

    public boolean isAccountProjection()
    {
        return target == Target.ACCOUNT_TRANSACTION;
    }

    public boolean isPortfolioProjection()
    {
        return target == Target.PORTFOLIO_TRANSACTION;
    }

    public boolean isPrimaryPostingExpected()
    {
        return primaryPostingExpected;
    }

    public boolean isPostingGroupExpected()
    {
        return postingGroupExpected;
    }
}
