package name.abuchen.portfolio.junit;

import java.math.BigDecimal;
import java.time.LocalDate;

import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.ExchangeRate;
import name.abuchen.portfolio.money.ExchangeRateTimeSeries;
import name.abuchen.portfolio.money.impl.ExchangeRateTimeSeriesImpl;
import name.abuchen.portfolio.money.impl.InverseExchangeRateTimeSeries;

@SuppressWarnings("nls")
public class TestCurrencyConverter implements CurrencyConverter
{
    private static ExchangeRateTimeSeriesImpl EUR_USD = null; // NOSONAR
    private static InverseExchangeRateTimeSeries USD_EUR = null; // NOSONAR

    static
    {
        EUR_USD = new ExchangeRateTimeSeriesImpl(null, CurrencyUnit.EUR, CurrencyUnit.USD);
        EUR_USD.addRate(new ExchangeRate(LocalDate.parse("2014-12-31"), BigDecimal.valueOf(1.2141).setScale(10)));
        EUR_USD.addRate(new ExchangeRate(LocalDate.parse("2015-01-02"), BigDecimal.valueOf(1.2043).setScale(10)));

        EUR_USD.addRate(new ExchangeRate(LocalDate.parse("2015-01-05"), BigDecimal.valueOf(1.1915).setScale(10)));
        EUR_USD.addRate(new ExchangeRate(LocalDate.parse("2015-01-06"), BigDecimal.valueOf(1.1914).setScale(10)));
        EUR_USD.addRate(new ExchangeRate(LocalDate.parse("2015-01-07"), BigDecimal.valueOf(1.1831).setScale(10)));
        EUR_USD.addRate(new ExchangeRate(LocalDate.parse("2015-01-08"), BigDecimal.valueOf(1.1768).setScale(10)));
        EUR_USD.addRate(new ExchangeRate(LocalDate.parse("2015-01-09"), BigDecimal.valueOf(1.1813).setScale(10)));

        EUR_USD.addRate(new ExchangeRate(LocalDate.parse("2015-01-12"), BigDecimal.valueOf(1.1804).setScale(10)));
        EUR_USD.addRate(new ExchangeRate(LocalDate.parse("2015-01-13"), new BigDecimal("1.1782")));
        EUR_USD.addRate(new ExchangeRate(LocalDate.parse("2015-01-14"), BigDecimal.valueOf(1.1775).setScale(10)));
        EUR_USD.addRate(new ExchangeRate(LocalDate.parse("2015-01-15"), BigDecimal.valueOf(1.1708).setScale(10)));
        EUR_USD.addRate(new ExchangeRate(LocalDate.parse("2015-01-16"), BigDecimal.valueOf(1.1588).setScale(10)));

        USD_EUR = new InverseExchangeRateTimeSeries(EUR_USD);
    }

    private final String termCurrency;

    public TestCurrencyConverter()
    {
        this(CurrencyUnit.EUR);
    }

    public TestCurrencyConverter(String currencyCode)
    {
        this.termCurrency = currencyCode;
    }

    @Override
    public String getTermCurrency()
    {
        return termCurrency;
    }

    @Override
    public ExchangeRate getRate(LocalDate date, String currencyCode)
    {
        if (termCurrency.equals(currencyCode))
            return new ExchangeRate(date, BigDecimal.ONE);

        ExchangeRateTimeSeries series;
        if (currencyCode.equals("USD") && termCurrency.equals("EUR"))
            series = USD_EUR;
        else if (currencyCode.equals("EUR") && termCurrency.equals("USD"))
            series = EUR_USD;
        else
            // testing: any other currency will be converted 1:1
            return new ExchangeRate(date, BigDecimal.ONE);

        return series.lookupRate(date).orElseThrow(IllegalArgumentException::new);
    }

    @Override
    public CurrencyConverter with(String currencyCode)
    {
        if (currencyCode.equals(termCurrency))
            return this;

        if (currencyCode.equals(CurrencyUnit.EUR)
                || currencyCode.equals(CurrencyUnit.USD))
            return new TestCurrencyConverter(currencyCode);

        return null;
    }
}
