package name.abuchen.portfolio.money;

import java.util.Date;
import java.util.List;
import java.util.Optional;

public interface ExchangeRateTimeSeries
{
    String getBaseCurrency();

    String getTermCurrency();

    ExchangeRateProvider getProvider();

    List<ExchangeRate> getRates();

    Optional<ExchangeRate> lookupRate(Date requestedTime);
}
