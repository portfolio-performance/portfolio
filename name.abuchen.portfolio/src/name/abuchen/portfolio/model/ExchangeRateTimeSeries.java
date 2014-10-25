package name.abuchen.portfolio.model;

import java.util.List;

public interface ExchangeRateTimeSeries
{
    String getBaseCurrency();

    String getTermCurrency();

    ExchangeRateProvider getProvider();

    List<ExchangeRate> getRates();
}
