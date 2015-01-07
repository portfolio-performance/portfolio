package name.abuchen.portfolio;

import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.stream.Collectors;

import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.ExchangeRate;
import name.abuchen.portfolio.money.MonetaryException;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class TestCurrencyConverter implements CurrencyConverter
{
    private final String termCurrency;
    private final Map<String, Long> rates;

    public TestCurrencyConverter()
    {
        this(CurrencyUnit.EUR, Arrays.asList(Money.of("USD", 8332)).stream()
                        .collect(Collectors.toMap(m -> m.getCurrencyCode(), m -> m.getAmount())));
    }

    public TestCurrencyConverter(String currencyCode, Map<String, Long> rates)
    {
        this.termCurrency = currencyCode;
        this.rates = rates;
    }

    @Override
    public String getTermCurrency()
    {
        return termCurrency;
    }

    @Override
    public Money convert(Date date, Money amount)
    {
        if (termCurrency.equals(amount.getCurrencyCode()))
            return amount;

        if (amount.isZero())
            return Money.of(termCurrency, 0);

        Long value = rates.get(amount.getCurrencyCode());

        if (value != null)
            return Money.of(termCurrency, Math.round((amount.getAmount() * value) / Values.ExchangeRate.divider()));

        throw new MonetaryException();
    }

    @Override
    public ExchangeRate getRate(Date date, String currencyCode)
    {
        if (termCurrency.equals(currencyCode))
            return new ExchangeRate(date, Values.ExchangeRate.factor());

        Long value = rates.get(currencyCode);

        return value != null ? new ExchangeRate(date, value) : null;
    }

    @Override
    public CurrencyConverter with(String currencyCode)
    {
        if (currencyCode.equals(termCurrency))
            return this;

        if (currencyCode.equals(CurrencyUnit.EUR))
            return new TestCurrencyConverter();

        if (currencyCode.equals("USD"))
            return new TestCurrencyConverter("USD", Arrays.asList(Money.of("EUR", 1_2141)).stream()
                            .collect(Collectors.toMap(m -> m.getCurrencyCode(), m -> m.getAmount())));

        return null;
    }
}
