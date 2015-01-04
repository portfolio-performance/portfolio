package name.abuchen.portfolio.money;

import java.util.Date;

public interface CurrencyConverter
{
    String getTermCurrency();

    Date getTime();

    Money convert(Money amount);
}
