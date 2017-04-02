package name.abuchen.portfolio.money;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ExchangeRateTimeSeries
{
    String getBaseCurrency();

    String getTermCurrency();

    ExchangeRateProvider getProvider();

    List<ExchangeRate> getRates();

    Optional<ExchangeRate> lookupRate(LocalDate requestedTime);

    int getWeight();

    default String getLabel()
    {
        return getBaseCurrency() + '/' + getTermCurrency();
    }
}
