package name.abuchen.portfolio.model.ledger.nativeentry;

import java.util.ArrayList;
import java.util.List;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.ledger.configuration.LedgerParameterType;
import name.abuchen.portfolio.model.ledger.configuration.TaxReason;
import name.abuchen.portfolio.money.Money;

/**
 * Carries native tax input for ledger-native entry assembly.
 * This is internal native-entry infrastructure. It describes facts for assembly and does not
 * mutate Ledger truth by itself.
 *
 * <p>
 * This input object is not persisted directly. The assembler converts it into Ledger
 * postings and parameters using stable posting type and parameter type codes.
 * </p>
 */
public final class NativeTax
{
    private final Account account;
    private final Money amount;
    private final List<NativeParameterValue> parameters;

    private NativeTax(Builder builder)
    {
        this.account = builder.account;
        this.amount = builder.amount;
        this.parameters = List.copyOf(builder.parameters);
    }

    public static Builder builder()
    {
        return new Builder();
    }

    public static NativeTax withholding(Account account, Money amount)
    {
        return builder().account(account).amount(amount).reason(TaxReason.WITHHOLDING_TAX)
                        .withholdingTax(true).build();
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
            return parameter(LedgerParameterType.TAX_REASON, reason);
        }

        public Builder reason(TaxReason reason)
        {
            return reason(reason.getCode());
        }

        public Builder taxableDistribution(boolean taxableDistribution)
        {
            return parameter(LedgerParameterType.TAXABLE_DISTRIBUTION, Boolean.valueOf(taxableDistribution));
        }

        public Builder withholdingTax(boolean withholdingTax)
        {
            return parameter(LedgerParameterType.WITHHOLDING_TAX, Boolean.valueOf(withholdingTax));
        }

        public Builder transactionTax(boolean transactionTax)
        {
            return parameter(LedgerParameterType.TRANSACTION_TAX, Boolean.valueOf(transactionTax));
        }

        public Builder reclaimableTax(boolean reclaimableTax)
        {
            return parameter(LedgerParameterType.RECLAIMABLE_TAX, Boolean.valueOf(reclaimableTax));
        }

        Builder parameter(LedgerParameterType type, Object value)
        {
            parameters.add(new NativeParameterValue(type, value));
            return this;
        }

        public NativeTax build()
        {
            return new NativeTax(this);
        }
    }
}
