package name.abuchen.portfolio.model.ledger.compatibility;

import java.math.BigDecimal;

import name.abuchen.portfolio.model.ledger.LedgerEntryMetadataPatch;

/**
 * Carries account transfer data for Ledger compatibility creators, editors, or patchers.
 * This is a compatibility-layer value object. Contributor code may pass it to Ledger write
 * paths, but it does not mutate Ledger truth by itself.
 */
public final class LedgerAccountTransferEdit
{
    private final LedgerEntryMetadataPatch metadata;
    private final LedgerPostingPatch sourcePosting;
    private final LedgerPostingPatch targetPosting;
    private final LedgerUnitPostingPatch units;

    private LedgerAccountTransferEdit(Builder builder)
    {
        this.metadata = builder.metadata;
        this.sourcePosting = builder.sourcePostingBuilder.build();
        this.targetPosting = builder.targetPostingBuilder.build();
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

    LedgerPostingPatch getSourcePosting()
    {
        return sourcePosting;
    }

    LedgerPostingPatch getTargetPosting()
    {
        return targetPosting;
    }

    LedgerUnitPostingPatch getUnits()
    {
        return units;
    }

    public static final class Builder
    {
        private LedgerEntryMetadataPatch metadata = LedgerEntryMetadataPatch.none();
        private final LedgerPostingPatch.Builder sourcePostingBuilder = LedgerPostingPatch.builder();
        private final LedgerPostingPatch.Builder targetPostingBuilder = LedgerPostingPatch.builder();
        private LedgerUnitPostingPatch units = LedgerUnitPostingPatch.none();

        private Builder()
        {
        }

        public Builder metadata(LedgerEntryMetadataPatch metadata)
        {
            this.metadata = java.util.Objects.requireNonNull(metadata);
            return this;
        }

        public Builder sourceAmount(long amount)
        {
            sourcePostingBuilder.amount(amount);
            return this;
        }

        public Builder sourceCurrency(String currency)
        {
            sourcePostingBuilder.currency(currency);
            return this;
        }

        public Builder sourceForexAmount(Long forexAmount)
        {
            sourcePostingBuilder.forexAmount(forexAmount);
            return this;
        }

        public Builder sourceForexCurrency(String forexCurrency)
        {
            sourcePostingBuilder.forexCurrency(forexCurrency);
            return this;
        }

        public Builder sourceExchangeRate(BigDecimal exchangeRate)
        {
            sourcePostingBuilder.exchangeRate(exchangeRate);
            return this;
        }

        public Builder targetAmount(long amount)
        {
            targetPostingBuilder.amount(amount);
            return this;
        }

        public Builder targetCurrency(String currency)
        {
            targetPostingBuilder.currency(currency);
            return this;
        }

        public Builder targetForexAmount(Long forexAmount)
        {
            targetPostingBuilder.forexAmount(forexAmount);
            return this;
        }

        public Builder targetForexCurrency(String forexCurrency)
        {
            targetPostingBuilder.forexCurrency(forexCurrency);
            return this;
        }

        public Builder targetExchangeRate(BigDecimal exchangeRate)
        {
            targetPostingBuilder.exchangeRate(exchangeRate);
            return this;
        }

        public Builder units(LedgerUnitPostingPatch units)
        {
            this.units = java.util.Objects.requireNonNull(units);
            return this;
        }

        public LedgerAccountTransferEdit build()
        {
            return new LedgerAccountTransferEdit(this);
        }
    }
}
