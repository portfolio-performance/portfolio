package name.abuchen.portfolio.cli.masterdata;

import java.util.Objects;
import java.util.function.BooleanSupplier;

import name.abuchen.portfolio.cli.command.ErrorCode;
import name.abuchen.portfolio.money.CurrencyUnit;

/** The checks the master data kinds share. */
final class Checks
{
    private static final String CURRENCY = "currencyCode";

    private Checks()
    {
    }

    static boolean isCurrency(String code)
    {
        return CurrencyUnit.containsCurrencyCode(code);
    }

    static String notACurrency(String code)
    {
        return "'" + code + "' is not a known ISO currency code.";
    }

    /**
     * Refuses a new currency for an entity that already has transactions.
     *
     * @param noun
     *            how the message names the entity
     */
    static void currencyKept(Writes.Validation<?> validation, String noun, String currentCode,
                    BooleanSupplier hasTransactions)
    {
        var values = validation.values();
        if (values.containsKey(CURRENCY) && !Objects.equals(currentCode, values.get(CURRENCY))
                        && hasTransactions.getAsBoolean())
            validation.fail(CURRENCY, ErrorCode.CURRENCY_IMMUTABLE,
                            "The currency can't change because the " + noun + " has transactions.");
    }
}
