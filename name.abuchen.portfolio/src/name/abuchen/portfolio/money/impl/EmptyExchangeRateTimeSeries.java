package name.abuchen.portfolio.money.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import name.abuchen.portfolio.money.ExchangeRate;
import name.abuchen.portfolio.money.ExchangeRateProvider;
import name.abuchen.portfolio.money.ExchangeRateTimeSeries;

public class EmptyExchangeRateTimeSeries implements ExchangeRateTimeSeries
{
    private final ExchangeRate exchangeRate = new ExchangeRate(LocalDate.now(), BigDecimal.ONE);
    private final String baseCurrency;
    private final String termCurrency;
    
    public EmptyExchangeRateTimeSeries(String baseCurrency, String termCurrency)
    {
        this.baseCurrency = baseCurrency;
        this.termCurrency = termCurrency;
    }

    @Override
    public String getBaseCurrency()
    {
        return baseCurrency;
    }

    @Override
    public String getTermCurrency()
    {
        return termCurrency;
    }

    @Override
    public Optional<ExchangeRateProvider> getProvider()
    {
        return Optional.empty();
    }

    @Override
    public List<ExchangeRate> getRates()
    {
        return Arrays.asList(exchangeRate);
    }

    @Override
    public Optional<ExchangeRate> lookupRate(LocalDate requestedTime)
    {
        return Optional.of(exchangeRate);
    }

    @Override
    public int getWeight()
    {
        return 1;
    }
}
