package name.abuchen.portfolio.money.impl;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.eclipse.core.runtime.IProgressMonitor;

import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.ExchangeRate;
import name.abuchen.portfolio.money.ExchangeRateProvider;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.money.ExchangeRateTimeSeries;

public class GBXExchangeRateProvider implements ExchangeRateProvider
{
    private static final String GBX = "GBX"; //$NON-NLS-1$
    private static final String GBP = "GBP"; //$NON-NLS-1$

    private ExchangeRateProviderFactory factory;

    private ExchangeRateTimeSeries gbp2gbx = new GBPGBX(this);
    private ExchangeRateTimeSeries gbx2gbp = new GBXGBP(this);

    @Override
    public String getName()
    {
        return CurrencyUnit.getInstance(GBX).getDisplayName();
    }

    @Override
    public void init(ExchangeRateProviderFactory factory)
    {
        this.factory = factory;
    }

    @Override
    public void load(IProgressMonitor monitor) throws IOException
    {}

    @Override
    public void update(IProgressMonitor monitor) throws IOException
    {}

    @Override
    public void save(IProgressMonitor monitor) throws IOException
    {}

    @Override
    public List<ExchangeRateTimeSeries> getAvailableTimeSeries()
    {
        List<ExchangeRateTimeSeries> answer = new ArrayList<>();
        answer.add(new GBXGBP(this));
        return answer;
    }

    @Override
    public ExchangeRateTimeSeries getTimeSeries(String baseCurrency, String termCurrency)
    {
        if (GBX.equals(baseCurrency) && GBP.equals(termCurrency))
        {
            return gbx2gbp;
        }
        else if (GBP.equals(baseCurrency) && GBX.equals(termCurrency))
        {
            return gbp2gbx;
        }
        else if (GBX.equals(baseCurrency))
        {
            if (CurrencyUnit.EUR.equals(termCurrency))
            {
                ExchangeRateTimeSeries series = factory.getTimeSeries(GBP, CurrencyUnit.EUR);
                return new ChainedExchangeRateTimeSeries(gbx2gbp, series);
            }
            else
            {
                // We continue to hack here b/c we know that ECB provides
                // everything we know to and from EUR. However, when adding more
                // providers, this will not work anymore. We need something like
                // Dijkstra's shortest path algorithm implemented outside of
                // rate provider.

                ExchangeRateTimeSeries eur2term = factory.getTimeSeries(CurrencyUnit.EUR, termCurrency);
                if (eur2term == null)
                    return null;

                ExchangeRateTimeSeries gbp2eur = factory.getTimeSeries(GBP, CurrencyUnit.EUR);
                return new ChainedExchangeRateTimeSeries(gbx2gbp, gbp2eur, eur2term);
            }

        }
        else if (GBX.equals(termCurrency))
        {
            if (CurrencyUnit.EUR.equals(baseCurrency))
            {
                ExchangeRateTimeSeries series = factory.getTimeSeries(CurrencyUnit.EUR, GBP);
                return new ChainedExchangeRateTimeSeries(series, gbp2gbx);
            }
            else
            {
                ExchangeRateTimeSeries base2eur = factory.getTimeSeries(baseCurrency, CurrencyUnit.EUR);
                if (base2eur == null)
                    return null;

                ExchangeRateTimeSeries eur2gbp = factory.getTimeSeries(CurrencyUnit.EUR, GBP);
                return new ChainedExchangeRateTimeSeries(base2eur, eur2gbp, gbp2gbx);
            }
        }
        else
        {
            return null;
        }
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
        public ExchangeRateProvider getProvider()
        {
            return provider;
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
    }

    private static class GBPGBX implements ExchangeRateTimeSeries
    {
        private ExchangeRateProvider provider;
        private BigDecimal rate = BigDecimal.valueOf(100);

        public GBPGBX(ExchangeRateProvider provider)
        {
            this.provider = provider;
        }

        @Override
        public String getBaseCurrency()
        {
            return GBP;
        }

        @Override
        public String getTermCurrency()
        {
            return GBX;
        }

        @Override
        public ExchangeRateProvider getProvider()
        {
            return provider;
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
    }
}
