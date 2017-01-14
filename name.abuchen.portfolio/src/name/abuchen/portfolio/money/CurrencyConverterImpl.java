package name.abuchen.portfolio.money;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.Optional;

import name.abuchen.portfolio.Messages;

public class CurrencyConverterImpl implements CurrencyConverter
{
    private final ExchangeRateProviderFactory factory;
    private final String termCurrency;

    public CurrencyConverterImpl(ExchangeRateProviderFactory factory, String termCurrency)
    {
        this.factory = factory;
        this.termCurrency = termCurrency;
    }

    @Override
    public String getTermCurrency()
    {
        return termCurrency;
    }

    @Override
    public Money convert(LocalDate date, Money amount)
    {
        if (termCurrency.equals(amount.getCurrencyCode()))
            return amount;

        if (amount.isZero())
            return Money.of(termCurrency, 0);

        ExchangeRate rate = getRate(date, amount.getCurrencyCode());
        BigDecimal converted = rate.getValue().multiply(BigDecimal.valueOf(amount.getAmount()));
        return Money.of(termCurrency, Math.round(converted.doubleValue()));
    }

    @Override
    public ExchangeRate getRate(LocalDate date, String currencyCode)
    {
        if (termCurrency.equals(currencyCode))
            return new ExchangeRate(date, BigDecimal.ONE);

        ExchangeRateTimeSeries series = lookupSeries(currencyCode);

        Optional<ExchangeRate> rate = series.lookupRate(date);
        if (!rate.isPresent())
            throw new MonetaryException(MessageFormat.format(Messages.MsgNoExchangeRateAvailableForConversion,
                            currencyCode, termCurrency));

        return rate.get();
    }

    private ExchangeRateTimeSeries lookupSeries(String currencyCode) // NOSONAR
    {
        ExchangeRateTimeSeries series = factory.getTimeSeries(currencyCode, termCurrency);
        if (series == null)
            throw new MonetaryException(MessageFormat.format(Messages.MsgNoExchangeRateTimeSeriesFound, currencyCode,
                            termCurrency));

        return series;
    }

    @Override
    public CurrencyConverter with(String currencyCode)
    {
        if (currencyCode.equals(termCurrency))
            return this;

        return new CurrencyConverterImpl(factory, currencyCode);
    }
}
