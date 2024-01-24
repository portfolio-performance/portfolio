package name.abuchen.portfolio.money.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import name.abuchen.portfolio.PortfolioLog;
import name.abuchen.portfolio.money.ExchangeRate;
import name.abuchen.portfolio.money.ExchangeRateProvider;
import name.abuchen.portfolio.util.Dates;

/**
 * Opens a connection to the ECB server, parses XML files and updates the given
 * {@link ECBData} object.
 */
/* package */class ECBUpdater
{
    private static final String SOURCE_URL = "https://www.ecb.europa.eu/stats/eurofxref/"; //$NON-NLS-1$

    private enum Feeds
    {
        HISTORIC("eurofxref-hist.xml"), //$NON-NLS-1$
        LAST_90_DAYS("eurofxref-hist-90d.xml"), //$NON-NLS-1$
        DAILY("eurofxref-daily.xml"); //$NON-NLS-1$

        private String xmlFileName;

        private Feeds(String xmlFileName)
        {
            this.xmlFileName = xmlFileName;
        }

        public String getXmlFileName()
        {
            return this.xmlFileName;
        }
    }

    public void update(ExchangeRateProvider provider, ECBData data) throws IOException
    {
        // determine which files must be loaded: full, last 90 days, daily
        Feeds f = Feeds.HISTORIC;
        if (data.getLastModified() != 0)
        {
            LocalDate lastModified = LocalDate
                            .from(Instant.ofEpochMilli(data.getLastModified()).atZone(ZoneId.systemDefault()));
            int days = Dates.daysBetween(lastModified, LocalDate.now());

            if (days <= 1)
                f = Feeds.DAILY;
            else if (days <= 90)
                f = Feeds.LAST_90_DAYS;
            else
                f = Feeds.HISTORIC;
        }

        // download feed
        InputStream input = null;
        HttpURLConnection connection = null;

        try // NOSONAR
        {
            URL feedUrl = new URI(SOURCE_URL + f.getXmlFileName()).toURL();

            connection = (HttpURLConnection) feedUrl.openConnection();

            // fortunately, the last modified date for all three feeds is
            // identical on the server. If nothing changed, parse nothing.
            long lastModified = connection.getLastModified();
            if (lastModified <= data.getLastModified())
                return;

            PortfolioLog.info("ECB: updating exchange rates " + feedUrl.toExternalForm()); //$NON-NLS-1$

            input = connection.getInputStream();

            XMLInputFactory factory = XMLInputFactory.newInstance();

            factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, Boolean.FALSE);
            factory.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);

            XMLStreamReader reader = factory
                            .createXMLStreamReader(new InputStreamReader(input, StandardCharsets.UTF_8));
            readCubes(provider, data, reader);
            data.setDirty(true);
            data.setLastModified(lastModified);
        }
        catch (XMLStreamException | URISyntaxException e)
        {
            throw new IOException(e);
        }
        finally
        {
            try
            {
                if (input != null)
                    input.close();
            }
            catch (IOException ignore)
            {
                // ignore exception on closing stream
            }

            if (connection != null)
                connection.disconnect();
        }
    }

    private void readCubes(ExchangeRateProvider provider, ECBData data, XMLStreamReader reader)
                    throws XMLStreamException
    {
        // lookup map by term currency
        Map<String, ExchangeRateTimeSeriesImpl> currency2series = new HashMap<>();
        for (ExchangeRateTimeSeriesImpl series : data.getSeries())
            currency2series.put(series.getTermCurrency(), series);

        LocalDate currentDate = null;

        while (reader.hasNext()) // NOSONAR readability
        {
            int event = reader.next();

            if (event != XMLStreamConstants.START_ELEMENT)
                continue;

            if (!"Cube".equals(reader.getLocalName())) //$NON-NLS-1$
                continue;

            // lookup currency first: it is more likely to show up
            String termCurrency = reader.getAttributeValue(null, "currency"); //$NON-NLS-1$
            if (termCurrency != null)
            {
                ExchangeRateTimeSeriesImpl series = currency2series.get(termCurrency); // NOSONAR
                if (series == null)
                {
                    series = new ExchangeRateTimeSeriesImpl();
                    series.setProvider(provider);
                    series.setBaseCurrency(ECBExchangeRateProvider.EUR);
                    series.setTermCurrency(termCurrency);
                    currency2series.put(termCurrency, series);
                    data.addSeries(series);
                }

                String rateValue = reader.getAttributeValue(null, "rate"); //$NON-NLS-1$
                BigDecimal rateNumber = new BigDecimal(rateValue);

                ExchangeRate rate = new ExchangeRate(currentDate, rateNumber);

                series.addRate(rate);
            }
            else
            {
                String time = reader.getAttributeValue(null, "time"); //$NON-NLS-1$
                if (time != null)
                    currentDate = LocalDate.parse(time);
            }
        }
    }
}
