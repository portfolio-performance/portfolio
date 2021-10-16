package name.abuchen.portfolio.money.impl;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

import com.thoughtworks.xstream.XStream;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.proto.v1.PECBData;
import name.abuchen.portfolio.model.proto.v1.PExchangeRate;
import name.abuchen.portfolio.model.proto.v1.PExchangeRateTimeSeries;
import name.abuchen.portfolio.money.ExchangeRate;
import name.abuchen.portfolio.money.ExchangeRateProvider;
import name.abuchen.portfolio.money.ExchangeRateTimeSeries;
import name.abuchen.portfolio.util.ProtobufUtil;
import name.abuchen.portfolio.util.XStreamLocalDateConverter;

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

    private static final String FILE_STORAGE_PB = "ecb_exchange_rates.pb"; //$NON-NLS-1$
    private static final String FILE_STORAGE_XML = "ecb_exchange_rates.xml"; //$NON-NLS-1$
    private static final String FILE_SUMMARY_XML = "ecb_exchange_rates_summary.xml"; //$NON-NLS-1$

    private volatile XStream xstream; // NOSONAR
    private ECBData data = new ECBData();

    public ECBExchangeRateProvider()
    {
        fillInDefaultData(data);
    }

    /* testing */ ECBExchangeRateProvider(ECBData data)
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

        File file = getStorageFile(FILE_STORAGE_PB);
        if (file.exists())
        {
            PECBData binary = read(file);
            data = convert(binary);
            monitor.worked(2);
        }
        else
        {
            // for the time being, fall back to reading the XML file
            file = getStorageFile(FILE_SUMMARY_XML);
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

            file = getStorageFile(FILE_STORAGE_XML);
            if (file.exists())
            {
                ECBData loaded = (ECBData) xstream().fromXML(file);
                loaded.doPostLoadProcessing(this);
                data = loaded;
            }
            monitor.worked(1);
        }
    }

    private ECBData convert(PECBData binary)
    {
        ECBData loaded = new ECBData();

        loaded.setLastModified(binary.getLastModified());

        for (PExchangeRateTimeSeries series : binary.getSeriesList())
        {
            ExchangeRateTimeSeriesImpl s = new ExchangeRateTimeSeriesImpl(this, series.getBaseCurrency(),
                            series.getTermCurrency());

            List<ExchangeRate> rates = new ArrayList<>();
            for (PExchangeRate rate : series.getExchangeRatesList())
            {
                rates.add(new ExchangeRate(LocalDate.ofEpochDay(rate.getDate()),
                                ProtobufUtil.fromDecimalValue(rate.getValue())));
            }

            s.replaceAll(rates);

            loaded.addSeries(s);
        }

        return loaded;
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

        File file = getStorageFile(FILE_STORAGE_PB);

        PECBData.Builder binary = PECBData.newBuilder();

        binary.setLastModified(data.getLastModified());
        for (ExchangeRateTimeSeriesImpl s : data.getSeries())
        {
            PExchangeRateTimeSeries.Builder series = PExchangeRateTimeSeries.newBuilder();
            series.setBaseCurrency(s.getBaseCurrency());
            series.setTermCurrency(s.getTermCurrency());

            for (ExchangeRate r : s.getRates())
            {
                series.addExchangeRates(PExchangeRate.newBuilder().setDate(r.getTime().toEpochDay())
                                .setValue(ProtobufUtil.asDecimalValue(r.getValue())));
            }

            binary.addSeries(series);
        }

        write(binary.build(), file);
    }

    @Override
    public List<ExchangeRateTimeSeries> getAvailableTimeSeries(Client client)
    {
        return new ArrayList<>(data.getSeries());
    }

    private File getStorageFile(String name)
    {
        Bundle bundle = FrameworkUtil.getBundle(ECBExchangeRateProvider.class);
        return bundle.getDataFile(name);
    }

    private void write(PECBData ecbdata, File file) throws IOException
    {
        try (FileOutputStream out = new FileOutputStream(file))
        {
            ecbdata.writeTo(out);
        }
    }

    private PECBData read(File file) throws IOException
    {
        try (InputStream in = new BufferedInputStream(new FileInputStream(file), 65536))
        {
            return PECBData.parseFrom(in);
        }
    }

    /**
     * Make sure we have at least one exchange rate available. The program might
     * not have a connection to the Internet (run behind a proxy) and is unable
     * to load current exchange rate series from ECB.
     */
    @SuppressWarnings("nls")
    private void fillInDefaultData(ECBData summary)
    {
        String date20151218 = "2015-12-18";

        summary.addSeries(createDefault("CHF", date20151218, BigDecimal.valueOf(1.0768)));
        summary.addSeries(createDefault("HRK", date20151218, BigDecimal.valueOf(7.6495)));
        summary.addSeries(createDefault("MXN", date20151218, BigDecimal.valueOf(18.4429)));
        summary.addSeries(createDefault("LVL", "2013-12-31", BigDecimal.valueOf(0.702804)));
        summary.addSeries(createDefault("MTL", "2007-12-31", BigDecimal.valueOf(0.4293)));
        summary.addSeries(createDefault("LTL", "2014-12-31", BigDecimal.valueOf(3.4528)));
        summary.addSeries(createDefault("ZAR", "2021-02-08", BigDecimal.valueOf(17.975)));
        summary.addSeries(createDefault("INR", date20151218, BigDecimal.valueOf(71.955)));
        summary.addSeries(createDefault("CNY", date20151218, BigDecimal.valueOf(7.0274)));
        summary.addSeries(createDefault("THB", date20151218, BigDecimal.valueOf(39.175)));
        summary.addSeries(createDefault("TRL", "2004-12-31", BigDecimal.valueOf(1836200)));
        summary.addSeries(createDefault("AUD", date20151218, BigDecimal.valueOf(1.5206)));
        summary.addSeries(createDefault("ILS", date20151218, BigDecimal.valueOf(4.221)));
        summary.addSeries(createDefault("KRW", date20151218, BigDecimal.valueOf(1280.16)));
        summary.addSeries(createDefault("JPY", date20151218, BigDecimal.valueOf(131.6)));
        summary.addSeries(createDefault("PLN", date20151218, BigDecimal.valueOf(4.2806)));
        summary.addSeries(createDefault("GBP", date20151218, BigDecimal.valueOf(0.72666)));
        summary.addSeries(createDefault("IDR", date20151218, BigDecimal.valueOf(15096.1)));
        summary.addSeries(createDefault("HUF", date20151218, BigDecimal.valueOf(314.25)));
        summary.addSeries(createDefault("PHP", date20151218, BigDecimal.valueOf(51.253)));
        summary.addSeries(createDefault("TRY", date20151218, BigDecimal.valueOf(3.1581)));
        summary.addSeries(createDefault("CYP", "2007-12-31", BigDecimal.valueOf(0.585274)));
        summary.addSeries(createDefault("RUB", date20151218, BigDecimal.valueOf(77.1005)));
        summary.addSeries(createDefault("HKD", date20151218, BigDecimal.valueOf(8.4005)));
        summary.addSeries(createDefault("ISK", "2008-12-09", BigDecimal.valueOf(290)));
        summary.addSeries(createDefault("DKK", date20151218, BigDecimal.valueOf(7.4613)));
        summary.addSeries(createDefault("USD", date20151218, BigDecimal.valueOf(1.0836)));
        summary.addSeries(createDefault("CAD", date20151218, BigDecimal.valueOf(1.5123)));
        summary.addSeries(createDefault("MYR", date20151218, BigDecimal.valueOf(4.644)));
        summary.addSeries(createDefault("BGN", date20151218, BigDecimal.valueOf(1.9558)));
        summary.addSeries(createDefault("EEK", "2010-12-31", BigDecimal.valueOf(15.6466)));
        summary.addSeries(createDefault("NOK", date20151218, BigDecimal.valueOf(9.5)));
        summary.addSeries(createDefault("ROL", "2005-06-30", BigDecimal.valueOf(36030)));
        summary.addSeries(createDefault("RON", date20151218, BigDecimal.valueOf(4.516)));
        summary.addSeries(createDefault("SGD", date20151218, BigDecimal.valueOf(1.53)));
        summary.addSeries(createDefault("SKK", "2008-12-31", BigDecimal.valueOf(30.126)));
        summary.addSeries(createDefault("CZK", date20151218, BigDecimal.valueOf(27.03)));
        summary.addSeries(createDefault("SEK", date20151218, BigDecimal.valueOf(9.266)));
        summary.addSeries(createDefault("NZD", date20151218, BigDecimal.valueOf(1.616)));
        summary.addSeries(createDefault("BRL", date20151218, BigDecimal.valueOf(4.2265)));
        summary.addSeries(createDefault("SIT", "2006-12-29", BigDecimal.valueOf(239.64)));
    }

    private ExchangeRateTimeSeriesImpl createDefault(String cur, String date, BigDecimal value)
    {
        ExchangeRateTimeSeriesImpl s = new ExchangeRateTimeSeriesImpl(this, EUR, cur);
        s.addRate(new ExchangeRate(LocalDate.parse(date), value));
        return s;
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
                    XStream xstream = new XStream();

                    xstream.allowTypesByWildcard(new String[] { "name.abuchen.portfolio.money.**" });

                    xstream.setClassLoader(ECBExchangeRateProvider.class.getClassLoader());

                    xstream.registerConverter(new XStreamLocalDateConverter());

                    xstream.alias("data", ECBData.class);
                    xstream.alias("series", ExchangeRateTimeSeriesImpl.class);

                    xstream.alias("rate", ExchangeRate.class);
                    xstream.useAttributeFor(ExchangeRate.class, "time");
                    xstream.aliasField("t", ExchangeRate.class, "time");
                    xstream.useAttributeFor(ExchangeRate.class, "value");
                    xstream.aliasField("v", ExchangeRate.class, "value");

                    this.xstream = xstream;
                }
            }
        }
        return xstream;
    }

}
