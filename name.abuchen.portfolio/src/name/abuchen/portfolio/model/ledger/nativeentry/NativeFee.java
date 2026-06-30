package name.abuchen.portfolio.model.ledger.nativeentry;

import java.util.ArrayList;
import java.util.List;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.ledger.configuration.FeeReason;
import name.abuchen.portfolio.model.ledger.configuration.LedgerParameterType;
import name.abuchen.portfolio.money.Money;

/**
 * Carries native fee input for ledger-native entry assembly.
 * This is internal native-entry infrastructure. It describes facts for assembly and does not
 * mutate Ledger truth by itself.
 *
 * <p>
 * This input object is not persisted directly. The assembler converts it into Ledger
 * postings and parameters using stable posting type and parameter type codes.
 * </p>
 */
public final class NativeFee
{
    private final Account account;
    private final Money amount;
    private final List<NativeParameterValue> parameters;

    private NativeFee(Builder builder)
    {
        this.account = builder.account;
        this.amount = builder.amount;
        this.parameters = List.copyOf(builder.parameters);
    }

    public static Builder builder()
    {
        return new Builder();
    }

    public static NativeFee of(Account account, Money amount, String reason)
    {
        return builder().account(account).amount(amount).reason(reason).build();
    }

    public static NativeFee of(Account account, Money amount, FeeReason reason)
    {
        return builder().account(account).amount(amount).reason(reason).build();
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

        public Builder reason(String reason)
        {
            return parameter(LedgerParameterType.FEE_REASON, reason);
        }

        public Builder reason(FeeReason reason)
        {
            return reason(reason.getCode());
        }

        public Builder stampDuty(boolean stampDuty)
        {
            return parameter(LedgerParameterType.STAMP_DUTY, Boolean.valueOf(stampDuty));
        }

        Builder parameter(LedgerParameterType type, Object value)
        {
            parameters.add(new NativeParameterValue(type, value));
            return this;
        }

        public NativeFee build()
        {
            return new NativeFee(this);
        }
    }
}
