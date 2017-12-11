package name.abuchen.portfolio.money;

import java.time.LocalDate;
import java.time.LocalDateTime;

public interface CurrencyConverter
{
    String getTermCurrency();

    Money convert(LocalDateTime date, Money amount);
    
    @Deprecated
    default Money convert(LocalDate date, Money amount)
    {
        return convert(date.atStartOfDay(), amount);
    }

    @Deprecated
    default MonetaryOperator at(LocalDate date)
    {
        return m -> convert(date, m);
    }
    
    default MonetaryOperator at(LocalDateTime date)
    {
        return m -> convert(date, m);
    }

    default ExchangeRate getRate(LocalDate date, String currencyCode)
    {
        return getRate(date.atStartOfDay(), currencyCode);
    }
    
    ExchangeRate getRate(LocalDateTime date, String currencyCode);

    /**
     * Returns a CurrencyConverter with the provided term currency
     */
    CurrencyConverter with(String currencyCode);
}
