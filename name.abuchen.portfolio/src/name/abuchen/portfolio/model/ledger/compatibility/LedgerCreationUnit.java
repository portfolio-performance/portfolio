package name.abuchen.portfolio.model.ledger.compatibility;

import java.util.Objects;

import name.abuchen.portfolio.model.ledger.configuration.LedgerPostingType;
import name.abuchen.portfolio.money.Money;

/**
 * Carries creation unit data for Ledger compatibility creators, editors, or patchers.
 * This is a compatibility-layer value object. Contributor code may pass it to Ledger write
 * paths, but it does not mutate Ledger truth by itself.
 */
public final class LedgerCreationUnit
{
    private final LedgerPostingType postingType;
    private final Money amount;
    private final LedgerForexAmount forex;

    private LedgerCreationUnit(LedgerPostingType postingType, Money amount, LedgerForexAmount forex)
    {
        this.postingType = Objects.requireNonNull(postingType);
        this.amount = Objects.requireNonNull(amount);
        this.forex = Objects.requireNonNull(forex);
    }

    public static LedgerCreationUnit fee(Money amount)
    {
        return new LedgerCreationUnit(LedgerPostingType.FEE, amount, LedgerForexAmount.none());
    }

    public static LedgerCreationUnit fee(Money amount, LedgerForexAmount forex)
    {
        return new LedgerCreationUnit(LedgerPostingType.FEE, amount, forex);
    }

    public static LedgerCreationUnit tax(Money amount)
    {
        return new LedgerCreationUnit(LedgerPostingType.TAX, amount, LedgerForexAmount.none());
    }

    public static LedgerCreationUnit tax(Money amount, LedgerForexAmount forex)
    {
        return new LedgerCreationUnit(LedgerPostingType.TAX, amount, forex);
    }

    public static LedgerCreationUnit grossValue(Money amount, LedgerForexAmount forex)
    {
        return new LedgerCreationUnit(LedgerPostingType.GROSS_VALUE, amount, forex);
    }

    public LedgerPostingType getPostingType()
    {
        return postingType;
    }

    public Money getAmount()
    {
        return amount;
    }

    public LedgerForexAmount getForex()
    {
        return forex;
    }
}
