package name.abuchen.portfolio.money;

import java.text.MessageFormat;
import java.util.Date;
import java.util.Optional;

public class CurrencyConverterImpl implements CurrencyConverter
{
    private ExchangeRateProviderFactory factory;
    private String targetCurrencyCode;
    private Date time;

    public CurrencyConverterImpl(ExchangeRateProviderFactory factory, String termCurrency, Date time)
    {
        this.factory = factory;
        this.targetCurrencyCode = termCurrency;
        this.time = time;
    }

    @Override
    public String getTermCurrency()
    {
        return targetCurrencyCode;
    }

    @Override
    public Date getTime()
    {
        return time;
    }

    @Override
    public Money convert(Money amount)
    {
        if (targetCurrencyCode.equals(amount.getCurrencyCode()))
            return amount;

        ExchangeRateTimeSeries series = factory.getTimeSeries(amount.getCurrencyCode(), targetCurrencyCode);
        if (series == null)
            throw new MonetaryException(MessageFormat.format("Unable to convert from {0} to {1}",
                            amount.getCurrencyCode(), targetCurrencyCode));

        Optional<ExchangeRate> rate = series.lookupRate(time);
        if (!rate.isPresent())
            throw new MonetaryException(MessageFormat.format("No rate availble to convert from {0} to {1}",
                            amount.getCurrencyCode(), targetCurrencyCode));

        return Money.of(targetCurrencyCode,
                        Math.round((amount.getAmount() * rate.get().getValue()) / Values.ExchangeRate.divider()));
    }
}
