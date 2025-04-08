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
    private static ExchangeRateTimeSeriesImpl EUR_CHF = null; // NOSONAR
    private static InverseExchangeRateTimeSeries CHF_EUR = null; // NOSONAR

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

        EUR_CHF = new ExchangeRateTimeSeriesImpl(null, CurrencyUnit.EUR, "CHF"); //$NON-NLS-1$
        EUR_CHF.addRate(new ExchangeRate(LocalDate.parse("2014-12-31"), new BigDecimal("1.2024")));
        EUR_CHF.addRate(new ExchangeRate(LocalDate.parse("2015-01-02"), new BigDecimal("1.2022")));

        EUR_CHF.addRate(new ExchangeRate(LocalDate.parse("2015-01-05"), new BigDecimal("1.2016")));
        EUR_CHF.addRate(new ExchangeRate(LocalDate.parse("2015-01-06"), new BigDecimal("1.2014")));
        EUR_CHF.addRate(new ExchangeRate(LocalDate.parse("2015-01-07"), new BigDecimal("1.2011")));
        EUR_CHF.addRate(new ExchangeRate(LocalDate.parse("2015-01-08"), new BigDecimal("1.2010")));
        EUR_CHF.addRate(new ExchangeRate(LocalDate.parse("2015-01-09"), new BigDecimal("1.2010")));

        EUR_CHF.addRate(new ExchangeRate(LocalDate.parse("2015-01-12"), new BigDecimal("1.2010")));
        EUR_CHF.addRate(new ExchangeRate(LocalDate.parse("2015-01-13"), new BigDecimal("1.2010")));
        EUR_CHF.addRate(new ExchangeRate(LocalDate.parse("2015-01-14"), new BigDecimal("1.2010")));
        // 'Francogeddon': the SNB abandons its EUR/CHF cap on 2015-01-15
        EUR_CHF.addRate(new ExchangeRate(LocalDate.parse("2015-01-15"), new BigDecimal("1.0280")));
        EUR_CHF.addRate(new ExchangeRate(LocalDate.parse("2015-01-16"), new BigDecimal("1.0128")));

        CHF_EUR = new InverseExchangeRateTimeSeries(EUR_CHF);
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
        else if (currencyCode.equals("CHF") && termCurrency.equals("EUR"))
            series = CHF_EUR;
        else if (currencyCode.equals("EUR") && termCurrency.equals("CHF"))
            series = EUR_CHF;
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
                || currencyCode.equals(CurrencyUnit.USD)
                || currencyCode.equals("CHF"))
            return new TestCurrencyConverter(currencyCode);

        return null;
    }
}
