package name.abuchen.portfolio.model.ledger.compatibility;

import java.util.Objects;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.money.Money;

/**
 * Carries cash transfer leg data for Ledger compatibility creators, editors, or patchers.
 * This is a compatibility-layer value object. Contributor code may pass it to Ledger write
 * paths, but it does not mutate Ledger truth by itself.
 */
public final class LedgerCashTransferLeg
{
    private final Account account;
    private final Money amount;
    private final LedgerForexAmount forex;

    private LedgerCashTransferLeg(Account account, Money amount, LedgerForexAmount forex)
    {
        this.account = Objects.requireNonNull(account);
        this.amount = Objects.requireNonNull(amount);
        this.forex = Objects.requireNonNull(forex);
    }

    public static LedgerCashTransferLeg of(Account account, Money amount)
    {
        return new LedgerCashTransferLeg(account, amount, LedgerForexAmount.none());
    }

    public static LedgerCashTransferLeg of(Account account, Money amount, LedgerForexAmount forex)
    {
        return new LedgerCashTransferLeg(account, amount, forex);
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
