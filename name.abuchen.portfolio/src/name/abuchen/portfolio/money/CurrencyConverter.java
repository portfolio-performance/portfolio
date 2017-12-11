package name.abuchen.portfolio.money;

import java.time.LocalDate;
import java.time.LocalDateTime;

public interface CurrencyConverter
{
    String getTermCurrency();

    Money convert(LocalDate date, Money amount);
    
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
