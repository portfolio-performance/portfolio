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
        LocalDate date01011999 = LocalDate.of(1999, 1, 1); // date rate was fixed against EUR for most currencies
        List<ExchangeRateTimeSeries> series = new ArrayList<ExchangeRateTimeSeries>();
        
        series.add(new FixedExchangeRateTimeSeries(this, 2, BigDecimal.valueOf(13.7603), CurrencyUnit.EUR, "ATS", date01011999)); //$NON-NLS-1$
        series.add(new FixedExchangeRateTimeSeries(this, 2, BigDecimal.valueOf(40.3399), CurrencyUnit.EUR, "BEF", date01011999)); //$NON-NLS-1$
        series.add(new FixedExchangeRateTimeSeries(this, 2, BigDecimal.valueOf(1.9558), CurrencyUnit.EUR, "DEM", date01011999)); //$NON-NLS-1$
        series.add(new FixedExchangeRateTimeSeries(this, 2, BigDecimal.valueOf(166.386), CurrencyUnit.EUR, "ESP", date01011999)); //$NON-NLS-1$
        series.add(new FixedExchangeRateTimeSeries(this, 2, BigDecimal.valueOf(5.94573), CurrencyUnit.EUR, "FIM", date01011999)); //$NON-NLS-1$
        series.add(new FixedExchangeRateTimeSeries(this, 2, BigDecimal.valueOf(6.55957), CurrencyUnit.EUR, "FRF", date01011999)); //$NON-NLS-1$
        series.add(new FixedExchangeRateTimeSeries(this, 2, BigDecimal.valueOf(340.75), CurrencyUnit.EUR, "GRD", LocalDate.of(2000, 6, 20))); //$NON-NLS-1$
        series.add(new FixedExchangeRateTimeSeries(this, 2, BigDecimal.valueOf(0.787564), CurrencyUnit.EUR, "IEP", date01011999)); //$NON-NLS-1$
        series.add(new FixedExchangeRateTimeSeries(this, 2, BigDecimal.valueOf(1936.27), CurrencyUnit.EUR, "ITL", date01011999)); //$NON-NLS-1$
        series.add(new FixedExchangeRateTimeSeries(this, 2, BigDecimal.valueOf(40.3399), CurrencyUnit.EUR, "LUF", date01011999)); //$NON-NLS-1$
        series.add(new FixedExchangeRateTimeSeries(this, 2, BigDecimal.valueOf(2.20371), CurrencyUnit.EUR, "NLG", date01011999)); //$NON-NLS-1$
        series.add(new FixedExchangeRateTimeSeries(this, 2, BigDecimal.valueOf(200.482), CurrencyUnit.EUR, "PTE", date01011999)); //$NON-NLS-1$
        
        series.add(new FixedExchangeRateTimeSeries(this, 2, BigDecimal.valueOf(3.6725), CurrencyUnit.USD, "AED", LocalDate.of(1997, 11, 1))); //$NON-NLS-1$
        series.add(new FixedExchangeRateTimeSeries(this, 2, BigDecimal.valueOf(0.01), "GBX", "GBP")); //$NON-NLS-1$ //$NON-NLS-2$
        series.add(new FixedExchangeRateTimeSeries(this, 2, BigDecimal.valueOf(0.01), "ILA", "ILS")); //$NON-NLS-1$ //$NON-NLS-2$
        
        return series;
    }
}
