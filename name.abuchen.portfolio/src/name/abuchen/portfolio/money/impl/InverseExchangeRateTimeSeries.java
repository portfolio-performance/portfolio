package name.abuchen.portfolio.money.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import name.abuchen.portfolio.money.ExchangeRate;
import name.abuchen.portfolio.money.ExchangeRateProvider;
import name.abuchen.portfolio.money.ExchangeRateTimeSeries;

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
    public Optional<ExchangeRate> lookupRate(LocalDate requestedTime)
    {
        Optional<ExchangeRate> answer = source.lookupRate(requestedTime);

        if (answer.isPresent())
        {
            BigDecimal reverse = BigDecimal.ONE.divide(answer.get().getValue(), 10, BigDecimal.ROUND_HALF_DOWN);
            return Optional.of(new ExchangeRate(answer.get().getTime(), reverse));
        }
        else
        {
            return answer;
        }
    }
}
