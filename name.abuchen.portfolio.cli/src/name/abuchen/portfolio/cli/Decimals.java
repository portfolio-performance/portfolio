package name.abuchen.portfolio.cli;

import java.math.BigDecimal;

import com.google.gson.JsonPrimitive;

import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

/**
 * Encodes stored fixed-point values as JSON decimals. Core's own formatting is
 * locale dependent and is deliberately avoided.
 */
final class Decimals
{
    private Decimals()
    {
    }

    /** The stored amount of the money, scaled by the precision of amounts. */
    static JsonPrimitive amount(Money money)
    {
        return of(money.getAmount(), Values.Amount);
    }

    /** The stored value scaled by the precision of the given values, e.g. a quote. */
    static JsonPrimitive of(long stored, Values<Long> values)
    {
        return of(BigDecimal.valueOf(stored, values.precision()));
    }

    /**
     * The value as it is, e.g. an exchange rate: unrounded, never in exponent
     * form. Wrapped so that Gson writes 0.00000001 rather than BigDecimal's
     * 1E-8.
     */
    static JsonPrimitive of(BigDecimal value)
    {
        return new JsonPrimitive(new PlainDecimal(value));
    }

    private static final class PlainDecimal extends Number
    {
        private static final long serialVersionUID = 1L;

        private final BigDecimal value;

        PlainDecimal(BigDecimal value)
        {
            this.value = value;
        }

        @Override
        public int intValue()
        {
            return value.intValue();
        }

        @Override
        public long longValue()
        {
            return value.longValue();
        }

        @Override
        public float floatValue()
        {
            return value.floatValue();
        }

        @Override
        public double doubleValue()
        {
            return value.doubleValue();
        }

        @Override
        public String toString()
        {
            return value.toPlainString();
        }
    }
}
