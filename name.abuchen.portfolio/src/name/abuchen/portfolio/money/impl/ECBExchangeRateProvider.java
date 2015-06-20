package name.abuchen.portfolio.money.impl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.money.ExchangeRate;
import name.abuchen.portfolio.money.ExchangeRateProvider;
import name.abuchen.portfolio.money.ExchangeRateTimeSeries;

import org.eclipse.core.runtime.IProgressMonitor;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.basic.DateConverter;

/**
 * Load and manage exchanges rates provided by the European Central Bank (ECB).
 * <p/>
 * Because the {@link #load(IProgressMonitor)},
 * {@link #update(IProgressMonitor)}, and {@link #save(IProgressMonitor)}
 * methods are run as background operations, the following strategy is used for
 * synchronization:
 * <ul>
 * <li>all state is held in a {@link ECBData} object</li>
 * <li>all update operations manipulate a deep copy of the data object</li>
 * </ul>
 */
public class ECBExchangeRateProvider implements ExchangeRateProvider
{
    public static final String EUR = "EUR"; //$NON-NLS-1$

    private static final String FILE_STORAGE = "ecb_exchange_rates.xml"; //$NON-NLS-1$
    private static final String FILE_SUMMARY = "ecb_exchange_rates_summary.xml"; //$NON-NLS-1$

    private volatile XStream xstream;
    private ECBData data = new ECBData();

    public ECBExchangeRateProvider()
    {}

    /* testing */ECBExchangeRateProvider(ECBData data)
    {
        this.data = data;
    }

    @Override
    public String getName()
    {
        return Messages.LabelEuropeanCentralBank;
    }

    @Override
    public synchronized void load(IProgressMonitor monitor) throws IOException
    {
        monitor.beginTask(MessageFormat.format(Messages.MsgLoadingExchangeRates, getName()), 2);

        // read summary first (contains only latest rates, but is fast)
        File file = getStorageFile(FILE_SUMMARY);
        if (file.exists())
        {
            @SuppressWarnings("unchecked")
            Map<String, ExchangeRate> loaded = (Map<String, ExchangeRate>) xstream().fromXML(file);

            ECBData summary = new ECBData();
            for (Map.Entry<String, ExchangeRate> entry : loaded.entrySet())
            {
                ExchangeRateTimeSeriesImpl s = new ExchangeRateTimeSeriesImpl(this, EUR, entry.getKey());
                s.addRate(entry.getValue());
                summary.addSeries(s);
            }
            data = summary;
        }
        monitor.worked(1);

        // read all historic exchange rates
        file = getStorageFile(FILE_STORAGE);
        if (file.exists())
        {
            ECBData loaded = (ECBData) xstream().fromXML(file);
            loaded.doPostLoadProcessing(this);
            data = loaded;
        }
        monitor.worked(1);
    }

    @Override
    public synchronized void update(IProgressMonitor monitor) throws IOException
    {
        ECBData copy = this.data.copy();
        new ECBUpdater().update(this, copy);
        this.data = copy;
    }

    @Override
    public synchronized void save(IProgressMonitor monitor) throws IOException
    {
        if (!data.isDirty())
            return;

        // store latest exchange rate separately -> faster to load upon startup
        // of the application
        File file = getStorageFile(FILE_SUMMARY);

        Map<String, ExchangeRate> summary = new HashMap<String, ExchangeRate>();
        for (ExchangeRateTimeSeriesImpl s : data.getSeries())
            s.getLatest().ifPresent(rate -> summary.put(s.getTermCurrency(), rate));
        write(summary, file);

        // write the full history data
        file = getStorageFile(FILE_STORAGE);
        write(data, file);
    }

    @Override
    public List<ExchangeRateTimeSeries> getAvailableTimeSeries()
    {
        return new ArrayList<ExchangeRateTimeSeries>(data.getSeries());
    }

    @Override
    public ExchangeRateTimeSeries getTimeSeries(String baseCurrency, String termCurrency)
    {
        if (EUR.equals(baseCurrency))
        {
            return data.getCurrencyMap().get(termCurrency);
        }
        else if (EUR.equals(termCurrency))
        {
            ExchangeRateTimeSeriesImpl series = data.getCurrencyMap().get(baseCurrency);
            return series != null ? new InverseExchangeRateTimeSeries(series) : null;
        }
        else
        {
            Map<String, ExchangeRateTimeSeriesImpl> map = data.getCurrencyMap();

            ExchangeRateTimeSeriesImpl base = map.get(baseCurrency);
            ExchangeRateTimeSeriesImpl term = map.get(termCurrency);

            if (base != null && term != null)
                return new ChainedExchangeRateTimeSeries(new InverseExchangeRateTimeSeries(base), term);
            else
                return null;
        }
    }

    private File getStorageFile(String name)
    {
        Bundle bundle = FrameworkUtil.getBundle(ECBExchangeRateProvider.class);
        return bundle.getDataFile(name);
    }

    private void write(Object object, File file) throws IOException
    {
        FileOutputStream out = null;
        try
        {
            out = new FileOutputStream(file);
            xstream().toXML(object, out);
        }
        finally
        {
            if (out != null)
                out.close();
        }
    }

    @SuppressWarnings("nls")
    private XStream xstream()
    {
        if (xstream == null)
        {
            synchronized (this)
            {
                if (xstream == null)
                {
                    xstream = new XStream();

                    xstream.setClassLoader(ECBExchangeRateProvider.class.getClassLoader());

                    xstream.registerConverter(new DateConverter("yyyy-MM-dd", new String[] { "yyyy-MM-dd" }, Calendar
                                    .getInstance().getTimeZone()));

                    xstream.alias("data", ECBData.class);
                    xstream.alias("series", ExchangeRateTimeSeriesImpl.class);

                    xstream.alias("rate", ExchangeRate.class);
                    xstream.useAttributeFor(ExchangeRate.class, "time");
                    xstream.aliasField("t", ExchangeRate.class, "time");
                    xstream.useAttributeFor(ExchangeRate.class, "value");
                    xstream.aliasField("v", ExchangeRate.class, "value");

                }
            }
        }
        return xstream;
    }
}
