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

public class ExchangeRateProviderATS implements ExchangeRateProvider
{
    private static final String ATS = "ATS"; //$NON-NLS-1$

    @Override
    public String getName()
    {
        return CurrencyUnit.getInstance(ATS).getDisplayName();
    }

    @Override
    public List<ExchangeRateTimeSeries> getAvailableTimeSeries(Client client)
    {
        List<ExchangeRateTimeSeries> answer = new ArrayList<>();
        answer.add(new EURATS(this));
        return answer;
    }

    private static class EURATS implements ExchangeRateTimeSeries
    {
        private ExchangeRateProvider provider;
        private BigDecimal rate = BigDecimal.valueOf(13.7603);

        public EURATS(ExchangeRateProvider provider)
        {
            this.provider = provider;
        }

        @Override
        public String getBaseCurrency()
        {
            return CurrencyUnit.EUR;
        }

        @Override
        public String getTermCurrency()
        {
            return ATS;
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
