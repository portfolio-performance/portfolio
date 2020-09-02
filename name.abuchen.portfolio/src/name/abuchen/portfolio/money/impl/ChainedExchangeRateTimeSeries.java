package name.abuchen.portfolio.money.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
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
    public Optional<ExchangeRateProvider> getProvider()
    {
        return Optional.empty();
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

    @Override
    public int getWeight()
    {
        int weight = 1;
        for (int ii = 0; ii < series.length; ii++)
            weight += series[ii].getWeight();
        return weight;
    }

    @Override
    public List<ExchangeRateTimeSeries> getComposition()
    {
        return Arrays.asList(series);
    }
}
