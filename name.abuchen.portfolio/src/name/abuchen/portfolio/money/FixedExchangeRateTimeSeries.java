package name.abuchen.portfolio.money;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FixedExchangeRateTimeSeries implements ExchangeRateTimeSeries
{
    private ExchangeRateProvider provider;
    private int weight;
    private String baseCurrency;
    private String termCurrency;
    private BigDecimal rate;
    private LocalDate seriesStart;

    public FixedExchangeRateTimeSeries(ExchangeRateProvider provider, int weight, BigDecimal rate, String baseCurrency, String termCurrency)
    {
        this(provider, weight, rate, baseCurrency, termCurrency, LocalDate.of(2000, 1, 1));
    }
    public FixedExchangeRateTimeSeries(ExchangeRateProvider provider, int weight, BigDecimal rate, String baseCurrency, String termCurrency, LocalDate seriesStart)
    {
        this.provider = provider;
        this.weight = weight;
        this.rate = rate;
        this.baseCurrency = baseCurrency;
        this.termCurrency = termCurrency;
        this.seriesStart = seriesStart;
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
        List<ExchangeRate> answer = new ArrayList<>();
        answer.add(new ExchangeRate(seriesStart, rate));
        answer.add(new ExchangeRate(LocalDate.now(), rate));
        return answer;
    }

    @Override
    public Optional<ExchangeRate> lookupRate(LocalDate requestedTime)
    {
        return Optional.of(new ExchangeRate(requestedTime, rate));
    }

    @Override
    public int getWeight()
    {
        return weight;
    }
}