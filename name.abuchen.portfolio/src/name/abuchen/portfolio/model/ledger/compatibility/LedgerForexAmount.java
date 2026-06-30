package name.abuchen.portfolio.model.ledger.compatibility;

import java.math.BigDecimal;
import java.util.Objects;

import name.abuchen.portfolio.money.Money;

/**
 * Carries forex amount data for Ledger compatibility creators, editors, or patchers.
 * This is a compatibility-layer value object. Contributor code may pass it to Ledger write
 * paths, but it does not mutate Ledger truth by itself.
 */
public final class LedgerForexAmount
{
    private static final LedgerForexAmount NONE = new LedgerForexAmount(null, null);

    private final Money forexAmount;
    private final BigDecimal exchangeRate;

    private LedgerForexAmount(Money forexAmount, BigDecimal exchangeRate)
    {
        this.forexAmount = forexAmount;
        this.exchangeRate = exchangeRate;
    }

    public static LedgerForexAmount none()
    {
        return NONE;
    }

    public static LedgerForexAmount of(Money forexAmount, BigDecimal exchangeRate)
    {
        Objects.requireNonNull(forexAmount);
        Objects.requireNonNull(exchangeRate);

        return new LedgerForexAmount(forexAmount, exchangeRate);
    }

    public boolean isPresent()
    {
        return forexAmount != null;
    }

    public Money getForexAmount()
    {
        return forexAmount;
    }

    public BigDecimal getExchangeRate()
    {
        return exchangeRate;
    }
}
