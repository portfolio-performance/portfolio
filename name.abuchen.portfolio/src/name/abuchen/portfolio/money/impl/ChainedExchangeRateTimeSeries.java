package name.abuchen.portfolio.money.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import name.abuchen.portfolio.money.ExchangeRate;
import name.abuchen.portfolio.money.ExchangeRateProvider;
import name.abuchen.portfolio.money.ExchangeRateTimeSeries;

public class ChainedExchangeRateTimeSeries implements ExchangeRateTimeSeries
{
    private ExchangeRateTimeSeries[] series;

    public ChainedExchangeRateTimeSeries(ExchangeRateTimeSeries... series)
    {
        if (series.length == 0)
            throw new UnsupportedOperationException();

        this.series = series;
    }

    @Override
    public String getBaseCurrency()
    {
        return series[0].getBaseCurrency();
    }

    @Override
    public String getTermCurrency()
    {
        return series[series.length - 1].getTermCurrency();
    }

    @Override
    public ExchangeRateProvider getProvider()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ExchangeRate> getRates()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<ExchangeRate> lookupRate(LocalDate requestedTime)
    {
        BigDecimal value = BigDecimal.ONE;

        for (int ii = 0; ii < series.length; ii++)
        {
            Optional<ExchangeRate> answer = series[ii].lookupRate(requestedTime);
            if (!answer.isPresent())
                return answer;

            value = value.multiply(answer.get().getValue());
        }

        return Optional.of(new ExchangeRate(requestedTime, value));
    }

}
