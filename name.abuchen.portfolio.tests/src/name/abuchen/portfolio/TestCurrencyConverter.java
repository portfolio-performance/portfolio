package name.abuchen.portfolio;

import java.math.BigDecimal;
import java.time.LocalDate;

import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.ExchangeRate;
import name.abuchen.portfolio.money.ExchangeRateTimeSeries;
import name.abuchen.portfolio.money.MonetaryException;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.impl.ExchangeRateTimeSeriesImpl;
import name.abuchen.portfolio.money.impl.InverseExchangeRateTimeSeries;

@SuppressWarnings("nls")
public class TestCurrencyConverter implements CurrencyConverter
{
    private static ExchangeRateTimeSeriesImpl EUR_USD = null; // NOSONAR

    static
    {
        EUR_USD = new ExchangeRateTimeSeriesImpl(null, CurrencyUnit.EUR, "USD");
        EUR_USD.addRate(new ExchangeRate(LocalDate.parse("2014-12-31"), BigDecimal.valueOf(1.2141).setScale(10)));
        EUR_USD.addRate(new ExchangeRate(LocalDate.parse("2015-01-02"), BigDecimal.valueOf(1.2043).setScale(10)));

        EUR_USD.addRate(new ExchangeRate(LocalDate.parse("2015-01-05"), BigDecimal.valueOf(1.1915).setScale(10)));
        EUR_USD.addRate(new ExchangeRate(LocalDate.parse("2015-01-06"), BigDecimal.valueOf(1.1914).setScale(10)));
        EUR_USD.addRate(new ExchangeRate(LocalDate.parse("2015-01-07"), BigDecimal.valueOf(1.1831).setScale(10)));
        EUR_USD.addRate(new ExchangeRate(LocalDate.parse("2015-01-08"), BigDecimal.valueOf(1.1768).setScale(10)));
        EUR_USD.addRate(new ExchangeRate(LocalDate.parse("2015-01-09"), BigDecimal.valueOf(1.1813).setScale(10)));

        EUR_USD.addRate(new ExchangeRate(LocalDate.parse("2015-01-12"), BigDecimal.valueOf(1.1804).setScale(10)));
        EUR_USD.addRate(new ExchangeRate(LocalDate.parse("2015-01-14"), BigDecimal.valueOf(1.1775).setScale(10)));
        EUR_USD.addRate(new ExchangeRate(LocalDate.parse("2015-01-15"), BigDecimal.valueOf(1.1708).setScale(10)));
        EUR_USD.addRate(new ExchangeRate(LocalDate.parse("2015-01-16"), BigDecimal.valueOf(1.1588).setScale(10)));
    }

    private final String termCurrency;
    private final ExchangeRateTimeSeries series;

    public TestCurrencyConverter()
    {
        this(CurrencyUnit.EUR, new InverseExchangeRateTimeSeries(EUR_USD));
    }

    public TestCurrencyConverter(String currencyCode, ExchangeRateTimeSeries series)
    {
        this.termCurrency = currencyCode;
        this.series = series;
    }

    @Override
    public String getTermCurrency()
    {
        return termCurrency;
    }

    @Override
    public Money convert(LocalDate date, Money amount)
    {
        try
        {
            return CurrencyConverter.super.convert(date, amount);
        }
        catch (MonetaryException e)
        {
            // testing: any other currency will be converted 1:1
            if (!amount.getCurrencyCode().equals(series.getBaseCurrency()))
                return Money.of(termCurrency, amount.getAmount());
            else
                throw e;
        }
    }

    @Override
    public ExchangeRate getRate(LocalDate date, String currencyCode)
    {
        if (termCurrency.equals(currencyCode))
            return new ExchangeRate(date, BigDecimal.ONE);

        if (!currencyCode.equals(series.getBaseCurrency()))
            throw new MonetaryException();

        return series.lookupRate(date).orElseThrow(IllegalArgumentException::new);
    }

    @Override
    public CurrencyConverter with(String currencyCode)
    {
        if (currencyCode.equals(termCurrency))
            return this;

        if (currencyCode.equals(CurrencyUnit.EUR))
            return new TestCurrencyConverter();

        if (currencyCode.equals("USD"))
            return new TestCurrencyConverter("USD", EUR_USD);

        return null;
    }
}
