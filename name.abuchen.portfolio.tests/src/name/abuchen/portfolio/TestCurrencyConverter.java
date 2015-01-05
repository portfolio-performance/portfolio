package name.abuchen.portfolio;

import java.util.Date;

import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.ExchangeRate;
import name.abuchen.portfolio.money.MonetaryException;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class TestCurrencyConverter implements CurrencyConverter
{
    @Override
    public String getTermCurrency()
    {
        return CurrencyUnit.EUR;
    }

    @Override
    public Money convert(Date date, Money amount)
    {
        if (CurrencyUnit.EUR.equals(amount.getCurrencyCode()))
            return amount;

        if ("USD".equals(amount.getCurrencyCode()))
            return Money.of(CurrencyUnit.EUR, Math.round((amount.getAmount() * 8332) / Values.ExchangeRate.divider()));

        throw new MonetaryException();
    }

    @Override
    public ExchangeRate getRate(Date date, String currencyCode)
    {
        switch (currencyCode)
        {
            case CurrencyUnit.EUR:
                return new ExchangeRate(date, Values.ExchangeRate.factor());
            case "USD":
                return new ExchangeRate(date, 8332);
            default:
                return null;
        }
    }
}
