package name.abuchen.portfolio.rest.internal;

import java.math.BigDecimal;
import java.math.RoundingMode;

import name.abuchen.portfolio.money.Values;

/**
 * Converts between API decimals and the model's fixed-point longs, and
 * implements the plausibility checks of the application's transaction
 * dialogs - with {@link BigDecimal} arithmetic throughout, never binary
 * floating point.
 */
public final class Amounts
{
    /** the gross value may deviate from shares x quote by this much per share */
    private static final BigDecimal QUOTE_TOLERANCE = new BigDecimal("0.01"); //$NON-NLS-1$

    /** the converted gross value may deviate by this much of the exchange rate */
    private static final BigDecimal RATE_TOLERANCE = new BigDecimal("0.0001"); //$NON-NLS-1$

    /** scale of an inverse exchange rate, as the dialogs compute it */
    private static final int INVERSE_RATE_SCALE = 10;

    private Amounts()
    {
    }

    /** an amount of money, e.g. 12.34 → 1234 */
    public static long toAmount(BigDecimal value)
    {
        return toLong(value, Values.Amount.precision());
    }

    /** a number of shares, e.g. 1.5 → 150000000 */
    public static long toShares(BigDecimal value)
    {
        return toLong(value, Values.Share.precision());
    }

    /** a per-share quote, e.g. 101.2 → 10120000000 */
    public static long toQuote(BigDecimal value)
    {
        return toLong(value, Values.Quote.precision());
    }

    public static BigDecimal amount(long value)
    {
        return BigDecimal.valueOf(value, Values.Amount.precision());
    }

    public static BigDecimal shares(long value)
    {
        return BigDecimal.valueOf(value, Values.Share.precision());
    }

    public static BigDecimal quote(long value)
    {
        return BigDecimal.valueOf(value, Values.Quote.precision());
    }

    /** the gross value of {@code shares} at {@code quote}, in amount units, rounded half-up */
    public static long grossValue(long shares, BigDecimal quote)
    {
        return toAmount(shares(shares).multiply(quote));
    }

    /** converts an amount with the exchange rate, rounded half-up */
    public static long convert(long amount, BigDecimal exchangeRate)
    {
        return toAmount(amount(amount).multiply(exchangeRate));
    }

    /** 1 / rate with the precision the application's dialogs use */
    public static BigDecimal inverseRate(BigDecimal exchangeRate)
    {
        if (exchangeRate.signum() == 0)
            return BigDecimal.ZERO;
        return BigDecimal.ONE.divide(exchangeRate, INVERSE_RATE_SCALE, RoundingMode.HALF_DOWN);
    }

    /**
     * Whether {@code grossValue} is plausible for {@code shares} at
     * {@code quote}: within shares x (quote ± 0.01), as in the application's
     * buy/sell dialog.
     */
    public static boolean isGrossValueWithinQuoteTolerance(long shares, BigDecimal quote, long grossValue)
    {
        var lower = grossValue(shares, quote.subtract(QUOTE_TOLERANCE));
        var upper = grossValue(shares, quote.add(QUOTE_TOLERANCE));
        return grossValue >= lower && grossValue <= upper;
    }

    /**
     * Whether {@code convertedValue} is plausible for {@code grossValue}
     * converted at {@code exchangeRate}: within gross x (rate ± 0.0001), as in
     * the application's buy/sell dialog.
     */
    public static boolean isConvertedValueWithinRateTolerance(long grossValue, BigDecimal exchangeRate,
                    long convertedValue)
    {
        var lower = convert(grossValue, exchangeRate.subtract(RATE_TOLERANCE));
        var upper = convert(grossValue, exchangeRate.add(RATE_TOLERANCE));
        return convertedValue >= lower && convertedValue <= upper;
    }

    private static long toLong(BigDecimal value, int precision)
    {
        return value.movePointRight(precision).setScale(0, RoundingMode.HALF_UP).longValueExact();
    }
}
