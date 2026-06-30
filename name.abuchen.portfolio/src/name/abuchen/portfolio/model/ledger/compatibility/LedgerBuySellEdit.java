package name.abuchen.portfolio.model.ledger.compatibility;

import java.math.BigDecimal;

import name.abuchen.portfolio.model.ledger.LedgerEntryMetadataPatch;

/**
 * Carries buy/sell data for Ledger compatibility creators, editors, or patchers.
 * This is a compatibility-layer value object. Contributor code may pass it to Ledger write
 * paths, but it does not mutate Ledger truth by itself.
 */
public final class LedgerBuySellEdit
{
    private final LedgerEntryMetadataPatch metadata;
    private final LedgerPostingPatch cashPosting;
    private final LedgerPostingPatch securityPosting;
    private final LedgerUnitPostingPatch units;

    private LedgerBuySellEdit(Builder builder)
    {
        this.metadata = builder.metadata;
        this.cashPosting = builder.cashPostingBuilder.build();
        this.securityPosting = builder.securityPostingBuilder.build();
        this.units = builder.units;
    }

    public static Builder builder()
    {
        return new Builder();
    }

    LedgerEntryMetadataPatch getMetadata()
    {
        return metadata;
    }

    LedgerPostingPatch getCashPosting()
    {
        return cashPosting;
    }

    LedgerPostingPatch getSecurityPosting()
    {
        return securityPosting;
    }

    LedgerUnitPostingPatch getUnits()
    {
        return units;
    }

    public static final class Builder
    {
        private LedgerEntryMetadataPatch metadata = LedgerEntryMetadataPatch.none();
        private final LedgerPostingPatch.Builder cashPostingBuilder = LedgerPostingPatch.builder();
        private final LedgerPostingPatch.Builder securityPostingBuilder = LedgerPostingPatch.builder();
        private LedgerUnitPostingPatch units = LedgerUnitPostingPatch.none();

        private Builder()
        {
        }

        public Builder metadata(LedgerEntryMetadataPatch metadata)
        {
            this.metadata = java.util.Objects.requireNonNull(metadata);
            return this;
        }

        public Builder cashAmount(long amount)
        {
            cashPostingBuilder.amount(amount);
            return this;
        }

        public Builder cashCurrency(String currency)
        {
            cashPostingBuilder.currency(currency);
            return this;
        }

        public Builder cashForexAmount(Long forexAmount)
        {
            cashPostingBuilder.forexAmount(forexAmount);
            return this;
        }

        public Builder cashForexCurrency(String forexCurrency)
        {
            cashPostingBuilder.forexCurrency(forexCurrency);
            return this;
        }

        public Builder cashExchangeRate(BigDecimal exchangeRate)
        {
            cashPostingBuilder.exchangeRate(exchangeRate);
            return this;
        }

        public Builder securityAmount(long amount)
        {
            securityPostingBuilder.amount(amount);
            return this;
        }

        public Builder securityCurrency(String currency)
        {
            securityPostingBuilder.currency(currency);
            return this;
        }

        public Builder securityForexAmount(Long forexAmount)
        {
            securityPostingBuilder.forexAmount(forexAmount);
            return this;
        }

        public Builder securityForexCurrency(String forexCurrency)
        {
            securityPostingBuilder.forexCurrency(forexCurrency);
            return this;
        }

        public Builder securityExchangeRate(BigDecimal exchangeRate)
        {
            securityPostingBuilder.exchangeRate(exchangeRate);
            return this;
        }

        public Builder security(name.abuchen.portfolio.model.Security security)
        {
            securityPostingBuilder.security(security);
            return this;
        }

        public Builder shares(long shares)
        {
            securityPostingBuilder.shares(shares);
            return this;
        }

        public Builder units(LedgerUnitPostingPatch units)
        {
            this.units = java.util.Objects.requireNonNull(units);
            return this;
        }

        public LedgerBuySellEdit build()
        {
            return new LedgerBuySellEdit(this);
        }
    }
}
