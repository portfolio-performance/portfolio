package name.abuchen.portfolio.model.ledger.nativeentry;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import name.abuchen.portfolio.model.ledger.configuration.CorporateActionKind;
import name.abuchen.portfolio.model.ledger.configuration.CorporateActionSubtype;
import name.abuchen.portfolio.model.ledger.configuration.EventStage;
import name.abuchen.portfolio.model.ledger.configuration.LedgerParameterType;

/**
 * Carries native corporate action event input for ledger-native entry assembly.
 * This is internal native-entry infrastructure. It describes facts for assembly and does not
 * mutate Ledger truth by itself.
 *
 * <p>
 * This input object is not persisted directly. The assembler converts its event facts into
 * Ledger parameters whose type ids and controlled string codes carry the persisted meaning.
 * </p>
 */
public final class NativeCorporateActionEvent
{
    private final List<NativeParameterValue> parameters;

    private NativeCorporateActionEvent(List<NativeParameterValue> parameters)
    {
        this.parameters = List.copyOf(parameters);
    }

    public static Builder builder()
    {
        return new Builder();
    }

    List<NativeParameterValue> getParameters()
    {
        return parameters;
    }

    public static final class Builder
    {
        private final List<NativeParameterValue> parameters = new ArrayList<>();

        private Builder()
        {
        }

        public Builder kind(String kind)
        {
            return parameter(LedgerParameterType.CORPORATE_ACTION_KIND, kind);
        }

        public Builder kind(CorporateActionKind kind)
        {
            return kind(kind.getCode());
        }

        public Builder subtype(String subtype)
        {
            return parameter(LedgerParameterType.CORPORATE_ACTION_SUBTYPE, subtype);
        }

        public Builder subtype(CorporateActionSubtype subtype)
        {
            return subtype(subtype.getCode());
        }

        public Builder reference(String reference)
        {
            return parameter(LedgerParameterType.EVENT_REFERENCE, reference);
        }

        public Builder stage(String stage)
        {
            return parameter(LedgerParameterType.EVENT_STAGE, stage);
        }

        public Builder stage(EventStage stage)
        {
            return stage(stage.getCode());
        }

        public Builder exDate(LocalDateTime exDate)
        {
            return parameter(LedgerParameterType.EX_DATE, exDate);
        }

        public Builder recordDate(LocalDate recordDate)
        {
            return parameter(LedgerParameterType.RECORD_DATE, recordDate);
        }

        public Builder paymentDate(LocalDate paymentDate)
        {
            return parameter(LedgerParameterType.PAYMENT_DATE, paymentDate);
        }

        public Builder effectiveDate(LocalDate effectiveDate)
        {
            return parameter(LedgerParameterType.EFFECTIVE_DATE, effectiveDate);
        }

        public Builder settlementDate(LocalDate settlementDate)
        {
            return parameter(LedgerParameterType.SETTLEMENT_DATE, settlementDate);
        }

        Builder parameter(LedgerParameterType type, Object value)
        {
            parameters.add(new NativeParameterValue(type, value));
            return this;
        }

        public NativeCorporateActionEvent build()
        {
            return new NativeCorporateActionEvent(parameters);
        }
    }
}
