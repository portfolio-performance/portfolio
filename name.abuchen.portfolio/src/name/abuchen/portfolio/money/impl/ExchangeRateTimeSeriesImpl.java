package name.abuchen.portfolio.money.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import name.abuchen.portfolio.money.ExchangeRate;
import name.abuchen.portfolio.money.ExchangeRateProvider;
import name.abuchen.portfolio.money.ExchangeRateTimeSeries;

public class ExchangeRateTimeSeriesImpl implements ExchangeRateTimeSeries
{
    private transient ExchangeRateProvider provider; // NOSONAR

    private String baseCurrency;
    private String termCurrency;
    private List<ExchangeRate> rates = new ArrayList<>();

    public ExchangeRateTimeSeriesImpl()
    {
        // empty constructor needed for xstream
    }

    public ExchangeRateTimeSeriesImpl(ExchangeRateTimeSeriesImpl template)
    {
        this.provider = template.provider;
        this.baseCurrency = template.baseCurrency;
        this.termCurrency = template.termCurrency;
        this.rates.addAll(template.rates);
    }

    public ExchangeRateTimeSeriesImpl(ExchangeRateProvider provider, String baseCurrency, String termCurrency)
    {
        this.provider = provider;
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
        return Optional.of(provider);
    }

    @Override
    public List<ExchangeRate> getRates()
    {
        return new ArrayList<>(rates);
    }

    public void setProvider(ExchangeRateProvider provider)
    {
        this.provider = provider;
    }

    public void setBaseCurrency(String baseCurrency)
    {
        this.baseCurrency = baseCurrency;
    }

    public void setTermCurrency(String termCurrency)
    {
        this.termCurrency = termCurrency;
    }

    public void addRate(ExchangeRate rate)
    {
        int index = Collections.binarySearch(rates, rate);

        if (index < 0)
            rates.add(~index, rate);
        else
            rates.set(index, rate);
    }

    /* package */ void replaceAll(List<ExchangeRate> newRates)
    {
        Collections.sort(newRates, (r, l) -> r.getTime().compareTo(l.getTime()));
        this.rates = newRates;
    }

    public Optional<ExchangeRate> getLatest()
    {
        return rates.isEmpty() ? Optional.empty() : Optional.of(rates.get(rates.size() - 1));
    }

    @Override
    public Optional<ExchangeRate> lookupRate(LocalDate requestedTime)
    {
        if (rates.isEmpty())
            return Optional.empty();

        ExchangeRate r = new ExchangeRate(requestedTime, BigDecimal.ZERO);
        int index = Collections.binarySearch(rates, r);

        if (index >= 0)
            return Optional.of(rates.get(index));
        else if (index == -1) // requested is date before first rate
            return Optional.of(rates.get(0));
        else
            return Optional.of(rates.get(-index - 2));
    }

    @Override
    public int getWeight()
    {
        return 2;
    }
}
