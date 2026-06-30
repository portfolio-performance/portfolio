package name.abuchen.portfolio.model.ledger.nativeentry;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.ledger.configuration.CashCompensationKind;
import name.abuchen.portfolio.model.ledger.configuration.FractionTreatment;
import name.abuchen.portfolio.model.ledger.configuration.LedgerParameterType;
import name.abuchen.portfolio.model.ledger.configuration.RoundingModeCode;
import name.abuchen.portfolio.money.Money;

/**
 * Carries native cash compensation input for ledger-native entry assembly.
 * This is internal native-entry infrastructure. It describes facts for assembly and does not
 * mutate Ledger truth by itself.
 *
 * <p>
 * This input object is not persisted directly. The assembler converts it into Ledger
 * postings and parameters using stable posting type and parameter type codes.
 * </p>
 */
public final class NativeCashCompensation
{
    private final Account account;
    private final Money amount;
    private final List<NativeParameterValue> parameters;

    private NativeCashCompensation(Builder builder)
    {
        this.account = builder.account;
        this.amount = builder.amount;
        this.parameters = List.copyOf(builder.parameters);
    }

    public static Builder builder()
    {
        return new Builder();
    }

    Account getAccount()
    {
        return account;
    }

    Money getAmount()
    {
        return amount;
    }

    List<NativeParameterValue> getParameters()
    {
        return parameters;
    }

    public static final class Builder
    {
        private Account account;
        private Money amount;
        private final List<NativeParameterValue> parameters = new ArrayList<>();

        private Builder()
        {
        }

        public Builder account(Account account)
        {
            this.account = account;
            return this;
        }

        public Builder amount(Money amount)
        {
            this.amount = amount;
            return this;
        }

        public Builder kind(String kind)
        {
            return parameter(LedgerParameterType.CASH_COMPENSATION_KIND, kind);
        }

        public Builder kind(CashCompensationKind kind)
        {
            return kind(kind.getCode());
        }

        public Builder applied(boolean applied)
        {
            return parameter(LedgerParameterType.CASH_IN_LIEU_APPLIED, Boolean.valueOf(applied));
        }

        public Builder fractionQuantity(BigDecimal quantity)
        {
            return parameter(LedgerParameterType.FRACTION_QUANTITY, quantity);
        }

        public Builder fractionTreatment(String treatment)
        {
            return parameter(LedgerParameterType.FRACTION_TREATMENT, treatment);
        }

        public Builder fractionTreatment(FractionTreatment treatment)
        {
            return fractionTreatment(treatment.getCode());
        }

        public Builder roundingMode(String roundingMode)
        {
            return parameter(LedgerParameterType.ROUNDING_MODE, roundingMode);
        }

        public Builder roundingMode(RoundingModeCode roundingMode)
        {
            return roundingMode(roundingMode.getCode());
        }

        Builder parameter(LedgerParameterType type, Object value)
        {
            parameters.add(new NativeParameterValue(type, value));
            return this;
        }

        public NativeCashCompensation build()
        {
            return new NativeCashCompensation(this);
        }
    }
}
