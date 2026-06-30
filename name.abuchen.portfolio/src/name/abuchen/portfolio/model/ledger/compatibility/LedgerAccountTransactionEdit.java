package name.abuchen.portfolio.model.ledger.compatibility;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.ledger.LedgerEntryMetadataPatch;
import name.abuchen.portfolio.model.ledger.LedgerFieldEdit;

/**
 * Carries account transaction data for Ledger compatibility creators, editors, or patchers.
 * This is a compatibility-layer value object. Contributor code may pass it to Ledger write
 * paths, but it does not mutate Ledger truth by itself.
 */
public final class LedgerAccountTransactionEdit
{
    private final LedgerEntryMetadataPatch metadata;
    private final LedgerPostingPatch posting;
    private final LedgerFieldEdit<LocalDateTime> exDate;
    private final LedgerUnitPostingPatch units;

    private LedgerAccountTransactionEdit(Builder builder)
    {
        this.metadata = builder.metadata;
        this.posting = builder.postingBuilder.build();
        this.exDate = builder.exDate;
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

    LedgerPostingPatch getPosting()
    {
        return posting;
    }

    LedgerFieldEdit<LocalDateTime> getExDate()
    {
        return exDate;
    }

    LedgerUnitPostingPatch getUnits()
    {
        return units;
    }

    public static final class Builder
    {
        private LedgerEntryMetadataPatch metadata = LedgerEntryMetadataPatch.none();
        private final LedgerPostingPatch.Builder postingBuilder = LedgerPostingPatch.builder();
        private LedgerFieldEdit<LocalDateTime> exDate = LedgerFieldEdit.omitted();
        private LedgerUnitPostingPatch units = LedgerUnitPostingPatch.none();

        private Builder()
        {
        }

        public Builder metadata(LedgerEntryMetadataPatch metadata)
        {
            this.metadata = java.util.Objects.requireNonNull(metadata);
            return this;
        }

        public Builder amount(long amount)
        {
            postingBuilder.amount(amount);
            return this;
        }

        public Builder currency(String currency)
        {
            postingBuilder.currency(currency);
            return this;
        }

        public Builder forexAmount(Long forexAmount)
        {
            postingBuilder.forexAmount(forexAmount);
            return this;
        }

        public Builder forexCurrency(String forexCurrency)
        {
            postingBuilder.forexCurrency(forexCurrency);
            return this;
        }

        public Builder exchangeRate(BigDecimal exchangeRate)
        {
            postingBuilder.exchangeRate(exchangeRate);
            return this;
        }

        public Builder security(Security security)
        {
            postingBuilder.security(security);
            return this;
        }

        public Builder shares(long shares)
        {
            postingBuilder.shares(shares);
            return this;
        }

        public Builder exDate(LocalDateTime exDate)
        {
            this.exDate = LedgerFieldEdit.set(exDate);
            return this;
        }

        public Builder clearExDate()
        {
            this.exDate = LedgerFieldEdit.clear();
            return this;
        }

        public Builder units(LedgerUnitPostingPatch units)
        {
            this.units = java.util.Objects.requireNonNull(units);
            return this;
        }

        public LedgerAccountTransactionEdit build()
        {
            return new LedgerAccountTransactionEdit(this);
        }
    }
}
