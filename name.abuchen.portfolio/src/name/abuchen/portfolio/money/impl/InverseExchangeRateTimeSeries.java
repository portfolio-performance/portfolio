package name.abuchen.portfolio.money.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.model.Client;
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
    public Optional<ExchangeRateProvider> getProvider()
    {
        return Optional.of(new ExchangeRateProvider()
        {
            @Override
            public String getName()
            {
                Optional<ExchangeRateProvider> provider = source.getProvider();
                return provider.isPresent() ? Messages.LabelInverseExchangeRate + ": " + provider.get().getName() //$NON-NLS-1$
                                : Messages.LabelInverseExchangeRate;
            }

            @Override
            public List<ExchangeRateTimeSeries> getAvailableTimeSeries(Client client)
            {
                return Collections.emptyList();
            }
        });
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

    @Override
    public int getWeight()
    {
        return 2 + source.getWeight();
    }

    @Override
    public List<ExchangeRateTimeSeries> getComposition()
    {
        return Arrays.asList(source);
    }
}
