package name.abuchen.portfolio.model.ledger.compatibility;

import java.util.Objects;

import name.abuchen.portfolio.model.LedgerDiagnosticCode;
import name.abuchen.portfolio.model.ledger.configuration.LedgerPostingType;
import name.abuchen.portfolio.money.Money;

/**
 * Carries unit posting data for Ledger compatibility creators, editors, or patchers.
 * This is a compatibility-layer value object. Contributor code may pass it to Ledger write
 * paths, but it does not mutate Ledger truth by itself.
 */
public final class LedgerUnitPostingEdit
{
    enum Operation
    {
        ADD,
        UPDATE,
        REMOVE
    }

    private final Operation operation;
    private final String postingUUID;
    private final LedgerPostingType postingType;
    private final LedgerPostingPatch postingPatch;

    private LedgerUnitPostingEdit(Operation operation, String postingUUID, LedgerPostingType postingType,
                    LedgerPostingPatch postingPatch)
    {
        this.operation = operation;
        this.postingUUID = postingUUID;
        this.postingType = postingType;
        this.postingPatch = postingPatch;
    }

    public static LedgerUnitPostingEdit add(LedgerPostingType postingType, Money amount)
    {
        return add(postingType, amount, LedgerForexAmount.none());
    }

    public static LedgerUnitPostingEdit add(LedgerPostingType postingType, Money amount, LedgerForexAmount forex)
    {
        requireUnitType(postingType);
        Objects.requireNonNull(amount);
        Objects.requireNonNull(forex);

        var builder = LedgerPostingPatch.builder().amount(amount.getAmount()).currency(amount.getCurrencyCode());

        if (forex.isPresent())
        {
            builder.forexAmount(forex.getForexAmount().getAmount());
            builder.forexCurrency(forex.getForexAmount().getCurrencyCode());
            builder.exchangeRate(forex.getExchangeRate());
        }

        return new LedgerUnitPostingEdit(Operation.ADD, null, postingType, builder.build());
    }

    public static LedgerUnitPostingEdit update(String postingUUID, Money amount)
    {
        return update(postingUUID, amount, LedgerForexAmount.none());
    }

    public static LedgerUnitPostingEdit update(String postingUUID, Money amount, LedgerForexAmount forex)
    {
        Objects.requireNonNull(amount);
        Objects.requireNonNull(forex);

        var builder = LedgerPostingPatch.builder().amount(amount.getAmount()).currency(amount.getCurrencyCode());

        if (forex.isPresent())
        {
            builder.forexAmount(forex.getForexAmount().getAmount());
            builder.forexCurrency(forex.getForexAmount().getCurrencyCode());
            builder.exchangeRate(forex.getExchangeRate());
        }

        return update(postingUUID, builder.build());
    }

    public static LedgerUnitPostingEdit clearForex(String postingUUID)
    {
        var builder = LedgerPostingPatch.builder().forexAmount(null).forexCurrency(null).exchangeRate(null);

        return update(postingUUID, builder.build());
    }

    static LedgerUnitPostingEdit update(String postingUUID, LedgerPostingPatch postingPatch)
    {
        return new LedgerUnitPostingEdit(Operation.UPDATE, Objects.requireNonNull(postingUUID), null,
                        Objects.requireNonNull(postingPatch));
    }

    public static LedgerUnitPostingEdit remove(String postingUUID)
    {
        return new LedgerUnitPostingEdit(Operation.REMOVE, Objects.requireNonNull(postingUUID), null,
                        LedgerPostingPatch.none());
    }

    Operation getOperation()
    {
        return operation;
    }

    String getPostingUUID()
    {
        return postingUUID;
    }

    LedgerPostingType getPostingType()
    {
        return postingType;
    }

    LedgerPostingPatch getPostingPatch()
    {
        return postingPatch;
    }

    static void requireUnitType(LedgerPostingType postingType)
    {
        switch (postingType)
        {
            case FEE:
            case TAX:
            case GROSS_VALUE:
                return;
            default:
                throw new IllegalArgumentException(LedgerDiagnosticCode.LEDGER_CONVERT_069.message("Unsupported unit posting type: " + postingType)); //$NON-NLS-1$
        }
    }
}
