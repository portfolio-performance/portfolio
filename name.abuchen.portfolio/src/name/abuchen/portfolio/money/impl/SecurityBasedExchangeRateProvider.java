package name.abuchen.portfolio.money.impl;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.eclipse.core.runtime.IProgressMonitor;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.money.ExchangeRate;
import name.abuchen.portfolio.money.ExchangeRateProvider;
import name.abuchen.portfolio.money.ExchangeRateTimeSeries;
import name.abuchen.portfolio.money.Values;

/**
 * An exchange rate provider based on securities tagged with two currencies.
 */
public class SecurityBasedExchangeRateProvider implements ExchangeRateProvider
{
    private class SecurityBasedExchangeRate implements ExchangeRateTimeSeries
    {
        private final Security security;

        public SecurityBasedExchangeRate(Security security)
        {
            this.security = security;
        }

        @Override
        public String getBaseCurrency()
        {
            return security.getCurrencyCode();
        }

        @Override
        public ExchangeRateProvider getProvider()
        {
            return SecurityBasedExchangeRateProvider.this;
        }

        @Override
        public List<ExchangeRate> getRates()
        {
            List<ExchangeRate> answer = new ArrayList<>();
            // turn all security prices into exchange rate
            for (SecurityPrice price : security.getPricesIncludingLatest())
            {
                answer.add(toExchangeRate(price));
            }
            return answer;
        }

        @Override
        public String getTermCurrency()
        {
            return security.getTargetCurrencyCode();
        }

        @Override
        public int getWeight()
        {
            return 1;
        }

        @Override
        public Optional<ExchangeRate> lookupRate(LocalDate requestedTime)
        {
            SecurityPrice price = security.getSecurityPrice(requestedTime);
            if (price != null)
            { 
                return Optional.of(toExchangeRate(price)); 
            }
            return Optional.empty();
        }

        /**
         * Converts a price of a security to an {@link ExchangeRate}.
         *
         * @param price
         *            {@link SecurityPrice}
         * @return {@link ExchangeRate}
         */
        private ExchangeRate toExchangeRate(SecurityPrice price)
        {
            return new ExchangeRate(price.getDate(), BigDecimal.valueOf(price.getValue() / Values.Quote.divider()));
        }

    }

    @Override
    public List<ExchangeRateTimeSeries> getAvailableTimeSeries(Client client)
    {
        // collect all securities that are exchange rates
        List<ExchangeRateTimeSeries> answer = new ArrayList<>();
        if (client != null)
        {
            for (Security security : client.getSecurities())
            {
                if (security.isExchangeRate())
                {
                    answer.add(new SecurityBasedExchangeRate(security));
                }
            }
        }
        return answer;
    }

    @Override
    public String getName()
    {
        return name.abuchen.portfolio.Messages.SecurityBasedExchangeRateProvider;
    }

    @Override
    public void load(IProgressMonitor monitor) throws IOException
    {}

    @Override
    public void save(IProgressMonitor monitor) throws IOException
    {}

    @Override
    public void update(IProgressMonitor monitor) throws IOException
    {}
}
