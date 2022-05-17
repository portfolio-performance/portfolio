package name.abuchen.portfolio.datatransfer.pdf;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.MessageFormat;
import java.util.Objects;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

/**
 * Helper class to handle exchange rates when parsing PDF documents.
 * 
 * <pre>
 * amount in base currency x exchange rate = amount in term currency
 * </pre>
 */
/* package */ class PDFExchangeRate
{
    private final BigDecimal rate;
    private final String baseCurrency;
    private final String termCurrency;

    public PDFExchangeRate(BigDecimal rate, String baseCurrency, String termCurrency)
    {
        this.rate = Objects.requireNonNull(rate);
        this.baseCurrency = Objects.requireNonNull(baseCurrency);
        this.termCurrency = Objects.requireNonNull(termCurrency);

        if (baseCurrency.equals(termCurrency) && rate.compareTo(BigDecimal.ONE) != 0)
            throw new IllegalArgumentException(
                            MessageFormat.format(Messages.MsgErrorBaseAndTermCurrencyAreEqualWithInvalidExchangeRate,
                                            baseCurrency, Values.ExchangeRate.format(rate)));
    }

    public BigDecimal getRate()
    {
        return rate;
    }

    /**
     * Returns the exchange rate to convert a value into the target currency.
     * 
     * @throws IllegalArgumentException
     *             if the exchange rate does not support the conversion
     */
    public BigDecimal getRate(String targetCurrencyCode)
    {
        if (termCurrency.equals(targetCurrencyCode))
            return rate;
        else if (baseCurrency.equals(targetCurrencyCode))
            return BigDecimal.ONE.divide(rate, 10, RoundingMode.HALF_DOWN);
        else
            throw new IllegalArgumentException(MessageFormat
                            .format(Messages.MsgErrorCannotRetrieveExchangeRateForCurrency, targetCurrencyCode, this));
    }

    public String getBaseCurrency()
    {
        return baseCurrency;
    }

    public String getTermCurrency()
    {
        return termCurrency;
    }

    /**
     * Converts the given amount into the target currency.
     * 
     * @throws IllegalArgumentException
     *             if the exchange rate does not support the conversion
     */
    public Money convert(String targetCurrencyCode, Money money)
    {
        if (termCurrency.equals(targetCurrencyCode))
        {
            if (!baseCurrency.equals(money.getCurrencyCode()))
                throw new IllegalArgumentException(
                                MessageFormat.format(Messages.MsgErrorCannotConvertToRequestedCurrency,
                                                Values.Money.format(money), targetCurrencyCode, this));

            return Money.of(targetCurrencyCode, BigDecimal.valueOf(money.getAmount()).multiply(rate)
                            .setScale(0, RoundingMode.HALF_UP).longValue());

        }
        else if (baseCurrency.equals(targetCurrencyCode))
        {
            if (!termCurrency.equals(money.getCurrencyCode()))
                throw new IllegalArgumentException(
                                MessageFormat.format(Messages.MsgErrorCannotConvertToRequestedCurrency,
                                                Values.Money.format(money), targetCurrencyCode, this));

            return Money.of(targetCurrencyCode, BigDecimal.valueOf(money.getAmount()).divide(rate, Values.MC)
                            .setScale(0, RoundingMode.HALF_UP).longValue());
        }
        else
        {
            throw new IllegalArgumentException(MessageFormat.format(Messages.MsgErrorCannotConvertToRequestedCurrency,
                            Values.Money.format(money), targetCurrencyCode, this));
        }
    }

    @Override
    public String toString()
    {
        return String.format("%s x %s = %s", baseCurrency, Values.ExchangeRate.format(rate), termCurrency); //$NON-NLS-1$
    }
}
