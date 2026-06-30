package name.abuchen.portfolio.model.ledger.compatibility;

import java.util.Objects;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.money.Money;

/**
 * Carries account cash leg data for Ledger compatibility creators, editors, or patchers.
 * This is a compatibility-layer value object. Contributor code may pass it to Ledger write
 * paths, but it does not mutate Ledger truth by itself.
 */
public final class LedgerAccountCashLeg
{
    private final Account account;
    private final Money amount;
    private final LedgerForexAmount forex;

    private LedgerAccountCashLeg(Account account, Money amount, LedgerForexAmount forex)
    {
        this.account = Objects.requireNonNull(account);
        this.amount = Objects.requireNonNull(amount);
        this.forex = Objects.requireNonNull(forex);
    }

    public static LedgerAccountCashLeg of(Account account, Money amount)
    {
        return new LedgerAccountCashLeg(account, amount, LedgerForexAmount.none());
    }

    public static LedgerAccountCashLeg of(Account account, Money amount, LedgerForexAmount forex)
    {
        return new LedgerAccountCashLeg(account, amount, forex);
    }

    public Account getAccount()
    {
        return account;
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
