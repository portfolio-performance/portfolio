package name.abuchen.portfolio.money;

import java.util.Date;

public interface CurrencyConverter
{
    String getTermCurrency();

    Money convert(Date date, Money amount);

    default MonetaryOperator at(Date date)
    {
        return m -> convert(date, m);
    }

    ExchangeRate getRate(Date date, String currencyCode);

    /**
     * Returns a CurrencyConverter with the provided term currency
     */
    CurrencyConverter with(String currencyCode);
}
