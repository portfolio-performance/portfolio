package name.abuchen.portfolio.money.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.ExchangeRate;
import name.abuchen.portfolio.money.ExchangeRateProvider;
import name.abuchen.portfolio.money.ExchangeRateTimeSeries;

public class GBXExchangeRateProvider implements ExchangeRateProvider
{
    private static final String GBX = "GBX"; //$NON-NLS-1$
    private static final String GBP = "GBP"; //$NON-NLS-1$

    @Override
    public String getName()
    {
        return CurrencyUnit.getInstance(GBX).getDisplayName();
    }

    @Override
    public List<ExchangeRateTimeSeries> getAvailableTimeSeries(Client client)
    {
        List<ExchangeRateTimeSeries> answer = new ArrayList<>();
        answer.add(new GBXGBP(this));
        return answer;
    }

    private static class GBXGBP implements ExchangeRateTimeSeries
    {
        private ExchangeRateProvider provider;
        private BigDecimal rate = BigDecimal.valueOf(0.01);

        public GBXGBP(ExchangeRateProvider provider)
        {
            this.provider = provider;
        }

        @Override
        public String getBaseCurrency()
        {
            return GBX;
        }

        @Override
        public String getTermCurrency()
        {
            return GBP;
        }

        @Override
        public Optional<ExchangeRateProvider> getProvider()
        {
            return Optional.of(provider);
        }

        @Override
        public List<ExchangeRate> getRates()
        {
            List<ExchangeRate> answer = new ArrayList<>();
            answer.add(new ExchangeRate(LocalDate.now(), rate));
            return answer;
        }

        @Override
        public Optional<ExchangeRate> lookupRate(LocalDate requestedTime)
        {
            return Optional.of(new ExchangeRate(requestedTime, rate));
        }

        @Override
        public int getWeight()
        {
            return 2;
        }
    }

}
