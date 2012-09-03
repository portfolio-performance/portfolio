package name.abuchen.portfolio.model;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.text.MessageFormat;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.model.Security.AssetClass;
import name.abuchen.portfolio.online.impl.YahooFinanceQuoteFeed;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.converters.basic.DateConverter;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

public class ClientFactory
{
    private static XStream xstream;

    public static Client load(File file) throws IOException
    {
        Client client = (Client) xstream().fromXML(
                        new InputStreamReader(new FileInputStream(file), Charset.forName("UTF-8"))); //$NON-NLS-1$

        if (client.getVersion() == 1)
        {
            addFeedAndExchange(client);
            client.setVersion(2);
        }

        if (client.getVersion() == 2)
        {
            addDecimalPlaces(client);
            client.setVersion(3);
        }
        
        if (client.getVersion() == 3)
        {
            // do nothing --> added industry classification
            client.setVersion(4);
        }

        if (client.getVersion() != 4)
            throw new UnsupportedOperationException(MessageFormat.format(Messages.MsgUnsupportedVersionClientFiled,
                            client.getVersion()));

        return client;
    }

    public static void save(Client client, File file) throws IOException
    {
        String xml = xstream().toXML(client);

        Writer writer = new OutputStreamWriter(new FileOutputStream(file), Charset.forName("UTF-8")); //$NON-NLS-1$
        writer.write(xml);
        writer.close();
    }

    private static void addFeedAndExchange(Client client)
    {
        for (Security s : client.getSecurities())
            s.setFeed(YahooFinanceQuoteFeed.ID);
    }

    private static void addDecimalPlaces(Client client)
    {
        for (Portfolio p : client.getPortfolios())
            for (PortfolioTransaction t : p.getTransactions())
                t.setShares(t.getShares() * Values.Share.factor());
    }

    @SuppressWarnings("nls")
    private static XStream xstream()
    {
        if (xstream == null)
        {
            synchronized (ClientFactory.class)
            {
                if (xstream == null)
                {
                    xstream = new XStream();

                    xstream.setClassLoader(ClientFactory.class.getClassLoader());

                    xstream.alias("account", Account.class);
                    xstream.alias("client", Client.class);
                    xstream.alias("portfolio", Portfolio.class);
                    xstream.alias("account-transaction", AccountTransaction.class);
                    xstream.alias("portfolio-transaction", PortfolioTransaction.class);
                    xstream.alias("security", Security.class);
                    xstream.alias("latest", LatestSecurityPrice.class);
                    xstream.alias("category", Category.class);
                    xstream.alias("watchlist", Watchlist.class);

                    xstream.alias("price", SecurityPrice.class);
                    xstream.useAttributeFor(SecurityPrice.class, "time");
                    xstream.aliasField("t", SecurityPrice.class, "time");
                    xstream.useAttributeFor(SecurityPrice.class, "value");
                    xstream.aliasField("v", SecurityPrice.class, "value");

                    xstream.alias("cpi", ConsumerPriceIndex.class);
                    xstream.useAttributeFor(ConsumerPriceIndex.class, "year");
                    xstream.aliasField("y", ConsumerPriceIndex.class, "year");
                    xstream.useAttributeFor(ConsumerPriceIndex.class, "month");
                    xstream.aliasField("m", ConsumerPriceIndex.class, "month");
                    xstream.useAttributeFor(ConsumerPriceIndex.class, "index");
                    xstream.aliasField("i", ConsumerPriceIndex.class, "index");

                    xstream.registerConverter(new DateConverter("yyyy-MM-dd", new String[] { "yyyy-MM-dd" }));
                    xstream.registerConverter(new AssetClassConverter());
                }
            }
        }
        return xstream;
    }

    private static class AssetClassConverter implements Converter
    {

        @SuppressWarnings("rawtypes")
        @Override
        public boolean canConvert(Class type)
        {
            return Security.AssetClass.class.isAssignableFrom(type);
        }

        @Override
        public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context)
        {
            writer.setValue(((AssetClass) source).name());
        }

        @Override
        public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context)
        {
            // see #5 - renamed STOCK->EQUITY and BOND->DEBT
            String value = reader.getValue();
            if ("STOCK".equals(value)) //$NON-NLS-1$
                value = "EQUITY"; //$NON-NLS-1$
            else if ("BOND".equals(value)) //$NON-NLS-1$
                value = "DEBT"; //$NON-NLS-1$
            return AssetClass.valueOf(value);
        }

    }
}
