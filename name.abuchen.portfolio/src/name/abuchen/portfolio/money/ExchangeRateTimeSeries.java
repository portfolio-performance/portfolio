package name.abuchen.portfolio.money;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public interface ExchangeRateTimeSeries
{
    String getBaseCurrency();

    String getTermCurrency();

    Optional<ExchangeRateProvider> getProvider();

    List<ExchangeRate> getRates();

    Optional<ExchangeRate> lookupRate(LocalDate requestedTime);

    default Optional<ExchangeRate> lookupRate(LocalDateTime requestedTime)
    {
        return lookupRate(requestedTime.toLocalDate());
    }

    int getWeight();

    default String getLabel()
    {
        return getBaseCurrency() + '/' + getTermCurrency();
    }

    /**
     * Returns a list of exchange rate time series that are used to build this
     * series. Returns an empty list if this is an original time series.
     */
    default List<ExchangeRateTimeSeries> getComposition()
    {
        return Collections.emptyList();
    }
}
