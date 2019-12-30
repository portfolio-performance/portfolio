package name.abuchen.portfolio.money;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;

public interface CurrencyConverter
{
    String getTermCurrency();

    default Money convert(LocalDate date, Money amount)
    {
        String termCurrency = getTermCurrency();

        if (termCurrency.equals(amount.getCurrencyCode()))
            return amount;

        if (amount.isZero())
            return Money.of(termCurrency, 0);

        ExchangeRate rate = getRate(date, amount.getCurrencyCode());
        BigDecimal converted = rate.getValue().multiply(BigDecimal.valueOf(amount.getAmount())).setScale(0,
                        RoundingMode.HALF_DOWN);
        return Money.of(termCurrency, converted.longValue());
    }
    
    default Quote convert(LocalDate date, Quote quote)
    {
        String termCurrency = getTermCurrency();

        if (termCurrency.equals(quote.getCurrencyCode()))
            return quote;

        if (quote.isZero())
            return Quote.of(termCurrency, 0);

        ExchangeRate rate = getRate(date, quote.getCurrencyCode());
        BigDecimal converted = rate.getValue().multiply(BigDecimal.valueOf(quote.getAmount())).setScale(0,
                        RoundingMode.HALF_DOWN);
        return Quote.of(termCurrency, converted.longValue());
    }

    default Money convert(LocalDateTime date, Money amount)
    {
        return convert(date.toLocalDate(), amount);
    }

    default MonetaryOperator at(LocalDate date)
    {
        return m -> convert(date, m);
    }
    
    default MonetaryOperator at(LocalDateTime date)
    {
        return m -> convert(date.toLocalDate(), m);
    }

    default ExchangeRate getRate(LocalDateTime date, String currencyCode)
    {
        return getRate(date.toLocalDate(), currencyCode);
    }
    
    ExchangeRate getRate(LocalDate date, String currencyCode);

    /**
     * Returns a CurrencyConverter with the provided term currency
     */
    CurrencyConverter with(String currencyCode);
}
