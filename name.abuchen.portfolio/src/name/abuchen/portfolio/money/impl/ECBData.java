package name.abuchen.portfolio.money.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Holds theExchangeRateTimeSeries time series that are managed by
 * {@link ECBExchangeRateProvider}.
 */
/* package */class ECBData
{
    private transient boolean isDirty = false; // NOSONAR

    private long lastModified;
    private List<ExchangeRateTimeSeriesImpl> timeSeries = new ArrayList<>();

    private transient Map<String, ExchangeRateTimeSeriesImpl> currency2series = new HashMap<>(); // NOSONAR

    public long getLastModified()
    {
        return lastModified;
    }

    public void setLastModified(long lastModified)
    {
        this.lastModified = lastModified;
    }

    public boolean isDirty()
    {
        return isDirty;
    }

    public void setDirty(boolean isDirty)
    {
        this.isDirty = isDirty;
    }

    public void addSeries(ExchangeRateTimeSeriesImpl series)
    {
        // base currency of ECB is always EUR
        if (!ECBExchangeRateProvider.EUR.equals(series.getBaseCurrency()))
            throw new UnsupportedOperationException();

        this.timeSeries.add(series);
        this.currency2series.put(series.getTermCurrency(), series);
    }

    public List<ExchangeRateTimeSeriesImpl> getSeries()
    {
        return timeSeries;
    }

    public Map<String, ExchangeRateTimeSeriesImpl> getCurrencyMap()
    {
        return currency2series;
    }

    public ECBData copy()
    {
        ECBData copy = new ECBData();
        copy.lastModified = this.lastModified;
        for (ExchangeRateTimeSeriesImpl series : timeSeries)
            copy.addSeries(new ExchangeRateTimeSeriesImpl(series));
        return copy;
    }

    public void doPostLoadProcessing(ECBExchangeRateProvider provider)
    {
        currency2series = new HashMap<>();

        for (ExchangeRateTimeSeriesImpl series : timeSeries)
        {
            series.setProvider(provider);
            currency2series.put(series.getTermCurrency(), series);
        }
    }
}
