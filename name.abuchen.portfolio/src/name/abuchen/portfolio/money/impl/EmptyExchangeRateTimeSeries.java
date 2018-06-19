package name.abuchen.portfolio.money.impl;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.money.ExchangeRate;
import name.abuchen.portfolio.money.ExchangeRateProvider;
import name.abuchen.portfolio.money.ExchangeRateTimeSeries;

public class EmptyExchangeRateTimeSeries implements ExchangeRateTimeSeries
{
    private final ExchangeRate exchangeRate = new ExchangeRate(LocalDate.now(), BigDecimal.ONE);
    private final String baseCurrency;
    private final String termCurrency;
    
    public EmptyExchangeRateTimeSeries(String baseCurrency, String termCurrency)
    {
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
    public ExchangeRateProvider getProvider()
    {
        return null;
    }

    @Override
    public List<ExchangeRate> getRates()
    {
        // notify the user about fallback
        Client.logWarning(MessageFormat.format(Messages.MsgNoExchangeRateAvailableForConversion, baseCurrency, termCurrency));
        return Arrays.asList(exchangeRate);
    }

    @Override
    public Optional<ExchangeRate> lookupRate(LocalDate requestedTime)
    {
        // notify the user about fallback
        Client.logWarning(MessageFormat.format(Messages.MsgNoExchangeRateAvailableForConversion, baseCurrency, termCurrency));
        return Optional.of(exchangeRate);
    }

    @Override
    public int getWeight()
    {
        return 1;
    }
}
