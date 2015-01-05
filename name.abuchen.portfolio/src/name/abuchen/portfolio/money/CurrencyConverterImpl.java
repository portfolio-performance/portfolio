package name.abuchen.portfolio.money;

import java.text.MessageFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class CurrencyConverterImpl implements CurrencyConverter
{
    private final ExchangeRateProviderFactory factory;
    private final String termCurrency;
    private final Date time;

    private final Map<String, ExchangeRate> cache = new HashMap<String, ExchangeRate>();

    public CurrencyConverterImpl(ExchangeRateProviderFactory factory, String termCurrency, Date time)
    {
        this.factory = factory;
        this.termCurrency = termCurrency;
        this.time = time;
    }

    @Override
    public String getTermCurrency()
    {
        return termCurrency;
    }

    @Override
    public Date getTime()
    {
        return time;
    }

    @Override
    public Money convert(Money amount)
    {
        if (termCurrency.equals(amount.getCurrencyCode()))
            return amount;

        ExchangeRate rate = cache.computeIfAbsent(amount.getCurrencyCode(), currency -> lookupRate(currency));

        return Money.of(termCurrency,
                        Math.round((amount.getAmount() * rate.getValue()) / Values.ExchangeRate.divider()));
    }

    @Override
    public ExchangeRate getRate(String currencyCode)
    {
        if (termCurrency.equals(currencyCode))
            return new ExchangeRate(time, Values.ExchangeRate.factor());

        return cache.computeIfAbsent(currencyCode, currency -> lookupRate(currency));
    }

    @Override
    public CurrencyConverter with(Date time)
    {
        return this.time.equals(time) ? this : new CurrencyConverterImpl(factory, termCurrency, time);
    }

    private ExchangeRate lookupRate(String currencyCode)
    {
        ExchangeRateTimeSeries series = factory.getTimeSeries(currencyCode, termCurrency);
        if (series == null)
            throw new MonetaryException(MessageFormat.format("Unable to convert from {0} to {1}", currencyCode,
                            termCurrency));

        Optional<ExchangeRate> rate = series.lookupRate(time);
        if (!rate.isPresent())
            throw new MonetaryException(MessageFormat.format("No rate availble to convert from {0} to {1}",
                            currencyCode, termCurrency));

        return rate.get();
    }
}
