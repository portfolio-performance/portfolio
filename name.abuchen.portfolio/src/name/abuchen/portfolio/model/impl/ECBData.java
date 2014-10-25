package name.abuchen.portfolio.model.impl;

import java.util.ArrayList;
import java.util.List;

import name.abuchen.portfolio.model.ExchangeRateTimeSeries;

/**
 * Holds the {@link ExchangeRateTimeSeries} time series that are managed by
 * {@link ECBExchangeRateProvider}.
 */
/* package */class ECBData
{
    private transient boolean isDirty = false;
    private long lastModified;
    private List<ExchangeRateTimeSeriesImpl> timeSeries = new ArrayList<ExchangeRateTimeSeriesImpl>();

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

    public List<ExchangeRateTimeSeriesImpl> getSeries()
    {
        return timeSeries;
    }

    public ECBData copy()
    {
        ECBData copy = new ECBData();
        copy.lastModified = this.lastModified;
        for (ExchangeRateTimeSeriesImpl series : timeSeries)
            copy.timeSeries.add(new ExchangeRateTimeSeriesImpl(series));
        return copy;
    }
}
