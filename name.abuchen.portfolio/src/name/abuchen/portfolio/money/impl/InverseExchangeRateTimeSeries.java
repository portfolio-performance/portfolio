package name.abuchen.portfolio.money.impl;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import name.abuchen.portfolio.money.ExchangeRate;
import name.abuchen.portfolio.money.ExchangeRateProvider;
import name.abuchen.portfolio.money.ExchangeRateTimeSeries;
import name.abuchen.portfolio.money.Values;

public class InverseExchangeRateTimeSeries implements ExchangeRateTimeSeries
{
    private ExchangeRateTimeSeries source;

    public InverseExchangeRateTimeSeries(ExchangeRateTimeSeries source)
    {
        this.source = source;
    }

    @Override
    public String getBaseCurrency()
    {
        return source.getTermCurrency();
    }

    @Override
    public String getTermCurrency()
    {
        return source.getBaseCurrency();
    }

    @Override
    public ExchangeRateProvider getProvider()
    {
        return source.getProvider();
    }

    @Override
    public List<ExchangeRate> getRates()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<ExchangeRate> lookupRate(Date requestedTime)
    {
        Optional<ExchangeRate> answer = source.lookupRate(requestedTime);

        if (answer.isPresent())
        {
            return Optional.of(new ExchangeRate(answer.get().getTime(),
                            Math.round((Values.ExchangeRate.factor() / (double) answer.get().getValue())
                                            * Values.ExchangeRate.factor())));
        }
        else
        {
            return answer;
        }
    }

}
