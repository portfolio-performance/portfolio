package name.abuchen.portfolio.money.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.ExchangeRateProvider;
import name.abuchen.portfolio.money.ExchangeRateTimeSeries;
import name.abuchen.portfolio.money.FixedExchangeRateTimeSeries;

public class FixedExchangeRateProvider implements ExchangeRateProvider
{   
    @Override
    public String getName()
    {
        return "-"; //$NON-NLS-1$
    }
    
    @Override
    public List<ExchangeRateTimeSeries> getAvailableTimeSeries(Client client)
    {
        LocalDate date01011999 = LocalDate.of(1999, 1, 1); // date rate was fixed for of EUR for most currencies
        List<ExchangeRateTimeSeries> series = new ArrayList<ExchangeRateTimeSeries>();
        
        series.add(new FixedExchangeRateTimeSeries(this, 2, BigDecimal.valueOf(13.7603), CurrencyUnit.EUR, "ATS", date01011999)); //$NON-NLS-1$
        series.add(new FixedExchangeRateTimeSeries(this, 2, BigDecimal.valueOf(1.9558), CurrencyUnit.EUR, "DEM", date01011999)); //$NON-NLS-1$
        
        series.add(new FixedExchangeRateTimeSeries(this, 2, BigDecimal.valueOf(3.6725), CurrencyUnit.USD, "AED", LocalDate.of(1997, 11, 1))); //$NON-NLS-1$
        series.add(new FixedExchangeRateTimeSeries(this, 2, BigDecimal.valueOf(0.01), "GBX", "GBP")); //$NON-NLS-1$ //$NON-NLS-2$
        series.add(new FixedExchangeRateTimeSeries(this, 2, BigDecimal.valueOf(0.01), "ILA", "ILS")); //$NON-NLS-1$ //$NON-NLS-2$
        
        return series;
    }
}
