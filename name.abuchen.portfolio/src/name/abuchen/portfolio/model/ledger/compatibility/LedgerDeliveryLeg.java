package name.abuchen.portfolio.model.ledger.compatibility;

import java.util.Objects;

import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.money.Money;

/**
 * Carries delivery leg data for Ledger compatibility creators, editors, or patchers.
 * This is a compatibility-layer value object. Contributor code may pass it to Ledger write
 * paths, but it does not mutate Ledger truth by itself.
 */
public final class LedgerDeliveryLeg
{
    private final Portfolio portfolio;
    private final LedgerSecurityQuantity securityQuantity;
    private final Money value;
    private final LedgerForexAmount forex;
    private final LedgerCreationUnits units;

    private LedgerDeliveryLeg(Portfolio portfolio, LedgerSecurityQuantity securityQuantity, Money value,
                    LedgerForexAmount forex, LedgerCreationUnits units)
    {
        this.portfolio = Objects.requireNonNull(portfolio);
        this.securityQuantity = Objects.requireNonNull(securityQuantity);
        this.value = Objects.requireNonNull(value);
        this.forex = Objects.requireNonNull(forex);
        this.units = Objects.requireNonNull(units);
    }

    public static LedgerDeliveryLeg of(Portfolio portfolio, LedgerSecurityQuantity securityQuantity, Money value)
    {
        return of(portfolio, securityQuantity, value, LedgerForexAmount.none(), LedgerCreationUnits.none());
    }

    public static LedgerDeliveryLeg of(Portfolio portfolio, LedgerSecurityQuantity securityQuantity, Money value,
                    LedgerCreationUnits units)
    {
        return of(portfolio, securityQuantity, value, LedgerForexAmount.none(), units);
    }

    public static LedgerDeliveryLeg of(Portfolio portfolio, LedgerSecurityQuantity securityQuantity, Money value,
                    LedgerForexAmount forex, LedgerCreationUnits units)
    {
        return new LedgerDeliveryLeg(portfolio, securityQuantity, value, forex, units);
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

    public LedgerCreationUnits getUnits()
    {
        return units;
    }
}
