package name.abuchen.portfolio.money;

import java.util.Date;

public interface CurrencyConverter
{
    String getTermCurrency();

    Money convert(Date date, Money amount);

    ExchangeRate getRate(Date date, String currencyCode);
}
