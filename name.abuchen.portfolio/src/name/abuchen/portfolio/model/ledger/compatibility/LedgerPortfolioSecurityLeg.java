package name.abuchen.portfolio.model.ledger.compatibility;

import java.util.Objects;

import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.money.Money;

/**
 * Carries portfolio security leg data for Ledger compatibility creators, editors, or patchers.
 * This is a compatibility-layer value object. Contributor code may pass it to Ledger write
 * paths, but it does not mutate Ledger truth by itself.
 */
public final class LedgerPortfolioSecurityLeg
{
    private final Portfolio portfolio;
    private final LedgerSecurityQuantity securityQuantity;
    private final Money value;
    private final LedgerForexAmount forex;

    private LedgerPortfolioSecurityLeg(Portfolio portfolio, LedgerSecurityQuantity securityQuantity, Money value,
                    LedgerForexAmount forex)
    {
        this.portfolio = Objects.requireNonNull(portfolio);
        this.securityQuantity = Objects.requireNonNull(securityQuantity);
        this.value = Objects.requireNonNull(value);
        this.forex = Objects.requireNonNull(forex);
    }

    public static LedgerPortfolioSecurityLeg of(Portfolio portfolio, LedgerSecurityQuantity securityQuantity,
                    Money value)
    {
        return new LedgerPortfolioSecurityLeg(portfolio, securityQuantity, value, LedgerForexAmount.none());
    }

    public static LedgerPortfolioSecurityLeg of(Portfolio portfolio, LedgerSecurityQuantity securityQuantity,
                    Money value, LedgerForexAmount forex)
    {
        return new LedgerPortfolioSecurityLeg(portfolio, securityQuantity, value, forex);
    }

    public Portfolio getPortfolio()
    {
        return portfolio;
    }

    public LedgerSecurityQuantity getSecurityQuantity()
    {
        return securityQuantity;
    }

    public Money getValue()
    {
        return value;
    }

    public LedgerForexAmount getForex()
    {
        return forex;
    }
}
