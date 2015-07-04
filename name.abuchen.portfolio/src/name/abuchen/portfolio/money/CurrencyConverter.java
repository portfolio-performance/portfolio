package name.abuchen.portfolio.money;

import java.time.LocalDate;

public interface CurrencyConverter
{
    String getTermCurrency();

    Money convert(LocalDate date, Money amount);

    default MonetaryOperator at(LocalDate date)
    {
        return m -> convert(date, m);
    }

    ExchangeRate getRate(LocalDate date, String currencyCode);

    /**
     * Returns a CurrencyConverter with the provided term currency
     */
    CurrencyConverter with(String currencyCode);
}
