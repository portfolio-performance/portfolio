package name.abuchen.portfolio.model.ledger.compatibility;

import java.math.BigDecimal;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.ledger.LedgerFieldEdit;
import name.abuchen.portfolio.model.ledger.LedgerPosting;

/**
 * Carries posting data for Ledger compatibility creators, editors, or patchers.
 * This is a compatibility-layer value object. Contributor code may pass it to Ledger write
 * paths, but it does not mutate Ledger truth by itself.
 */
final class LedgerPostingPatch
{
    private final LedgerFieldEdit<Long> amount;
    private final LedgerFieldEdit<String> currency;
    private final LedgerFieldEdit<Long> forexAmount;
    private final LedgerFieldEdit<String> forexCurrency;
    private final LedgerFieldEdit<BigDecimal> exchangeRate;
    private final LedgerFieldEdit<Security> security;
    private final LedgerFieldEdit<Long> shares;
    private final LedgerFieldEdit<Account> account;
    private final LedgerFieldEdit<Portfolio> portfolio;

    private LedgerPostingPatch(Builder builder)
    {
        this.amount = builder.amount;
        this.currency = builder.currency;
        this.forexAmount = builder.forexAmount;
        this.forexCurrency = builder.forexCurrency;
        this.exchangeRate = builder.exchangeRate;
        this.security = builder.security;
        this.shares = builder.shares;
        this.account = builder.account;
        this.portfolio = builder.portfolio;
    }

    static LedgerPostingPatch none()
    {
        return builder().build();
    }

    static Builder builder()
    {
        return new Builder();
    }

    void applyTo(LedgerPosting posting)
    {
        if (!amount.isOmitted())
            posting.setAmount(amount.isClear() ? 0L : amount.getValue());
        if (!currency.isOmitted())
            posting.setCurrency(currency.isClear() ? null : currency.getValue());
        if (!forexAmount.isOmitted())
            posting.setForexAmount(forexAmount.isClear() ? null : forexAmount.getValue());
        if (!forexCurrency.isOmitted())
            posting.setForexCurrency(forexCurrency.isClear() ? null : forexCurrency.getValue());
        if (!exchangeRate.isOmitted())
            posting.setExchangeRate(exchangeRate.isClear() ? null : exchangeRate.getValue());
        if (!security.isOmitted())
            posting.setSecurity(security.isClear() ? null : security.getValue());
        if (!shares.isOmitted())
            posting.setShares(shares.isClear() ? 0L : shares.getValue());
        if (!account.isOmitted())
            posting.setAccount(account.isClear() ? null : account.getValue());
        if (!portfolio.isOmitted())
            posting.setPortfolio(portfolio.isClear() ? null : portfolio.getValue());
    }

    static final class Builder
    {
        private LedgerFieldEdit<Long> amount = LedgerFieldEdit.omitted();
        private LedgerFieldEdit<String> currency = LedgerFieldEdit.omitted();
        private LedgerFieldEdit<Long> forexAmount = LedgerFieldEdit.omitted();
        private LedgerFieldEdit<String> forexCurrency = LedgerFieldEdit.omitted();
        private LedgerFieldEdit<BigDecimal> exchangeRate = LedgerFieldEdit.omitted();
        private LedgerFieldEdit<Security> security = LedgerFieldEdit.omitted();
        private LedgerFieldEdit<Long> shares = LedgerFieldEdit.omitted();
        private LedgerFieldEdit<Account> account = LedgerFieldEdit.omitted();
        private LedgerFieldEdit<Portfolio> portfolio = LedgerFieldEdit.omitted();

        private Builder()
        {
        }

        Builder amount(long amount)
        {
            this.amount = LedgerFieldEdit.set(amount);
            return this;
        }

        Builder currency(String currency)
        {
            this.currency = currency != null ? LedgerFieldEdit.set(currency) : LedgerFieldEdit.clear();
            return this;
        }

        Builder forexAmount(Long forexAmount)
        {
            this.forexAmount = forexAmount != null ? LedgerFieldEdit.set(forexAmount) : LedgerFieldEdit.clear();
            return this;
        }

        Builder forexCurrency(String forexCurrency)
        {
            this.forexCurrency = forexCurrency != null ? LedgerFieldEdit.set(forexCurrency) : LedgerFieldEdit.clear();
            return this;
        }

        Builder exchangeRate(BigDecimal exchangeRate)
        {
            this.exchangeRate = exchangeRate != null ? LedgerFieldEdit.set(exchangeRate) : LedgerFieldEdit.clear();
            return this;
        }

        Builder security(Security security)
        {
            this.security = security != null ? LedgerFieldEdit.set(security) : LedgerFieldEdit.clear();
            return this;
        }

        Builder shares(long shares)
        {
            this.shares = LedgerFieldEdit.set(shares);
            return this;
        }

        Builder account(Account account)
        {
            this.account = account != null ? LedgerFieldEdit.set(account) : LedgerFieldEdit.clear();
            return this;
        }

        Builder portfolio(Portfolio portfolio)
        {
            this.portfolio = portfolio != null ? LedgerFieldEdit.set(portfolio) : LedgerFieldEdit.clear();
            return this;
        }

        LedgerPostingPatch build()
        {
            return new LedgerPostingPatch(this);
        }
    }
}
