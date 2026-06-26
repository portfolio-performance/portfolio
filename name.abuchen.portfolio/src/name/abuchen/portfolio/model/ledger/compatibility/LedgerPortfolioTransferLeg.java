package name.abuchen.portfolio.model.ledger.compatibility;

import java.util.Objects;

import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.money.Money;

/**
 * Carries portfolio transfer leg data for Ledger compatibility creators, editors, or patchers.
 * This is a compatibility-layer value object. Contributor code may pass it to Ledger write
 * paths, but it does not mutate Ledger truth by itself.
 */
public final class LedgerPortfolioTransferLeg
{
    private final Portfolio portfolio;
    private final Money value;
    private final LedgerForexAmount forex;

    private LedgerPortfolioTransferLeg(Portfolio portfolio, Money value, LedgerForexAmount forex)
    {
        this.portfolio = Objects.requireNonNull(portfolio);
        this.value = Objects.requireNonNull(value);
        this.forex = Objects.requireNonNull(forex);
    }

    public static LedgerPortfolioTransferLeg of(Portfolio portfolio, Money value)
    {
        return of(portfolio, value, LedgerForexAmount.none());
    }

    public static LedgerPortfolioTransferLeg of(Portfolio portfolio, Money value, LedgerForexAmount forex)
    {
        return new LedgerPortfolioTransferLeg(portfolio, value, forex);
    }

    public Portfolio getPortfolio()
    {
        return portfolio;
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
