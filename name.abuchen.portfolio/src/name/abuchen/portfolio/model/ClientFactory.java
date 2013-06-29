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
import java.util.UUID;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.model.Classification.Assignment;
import name.abuchen.portfolio.model.PortfolioTransaction.Type;
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

        client.doPostLoadInitialization();

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

        if (client.getVersion() == 4)
        {
            for (Security s : client.getSecurities())
                s.generateUUID();
            client.setVersion(5);
        }

        if (client.getVersion() == 5)
        {
            // do nothing --> save industry taxonomy in client
            client.setVersion(6);
        }

        if (client.getVersion() == 6)
        {
            // do nothing --> added WKN attribute to security
            client.setVersion(7);
        }

        if (client.getVersion() == 7)
        {
            // new portfolio transaction types:
            // DELIVERY_INBOUND, DELIVERY_OUTBOUND
            changePortfolioTransactionTypeToDelivery(client);
            client.setVersion(8);
        }

        if (client.getVersion() == 8)
        {
            // do nothing --> added 'retired' property to securities
            client.setVersion(9);
        }

        if (client.getVersion() == 9)
        {
            // do nothing --> added 'cross entries' to transactions
            client.setVersion(10);
        }

        if (client.getVersion() == 10)
        {
            for (Account a : client.getAccounts())
                a.generateUUID();
            for (Portfolio p : client.getPortfolios())
                p.generateUUID();
            for (Category c : client.getRootCategory().flatten())
                c.generateUUID();
            client.setVersion(11);
        }

        if (client.getVersion() == 11)
        {
            // do nothing --> added 'properties' to client
            client.setVersion(12);
        }

        if (client.getVersion() == 12)
        {
            // added investment plans
            // added security on chart as benchmark *and* performance
            fixStoredChartConfigurations(client);

            client.setVersion(13);
        }

        if (client.getVersion() == 13)
        {
            // introduce arbitrary taxonomies
            addAssetClassesAsTaxonomy(client);
            addIndustryClassificationAsTaxonomy(client);
            addAssetAllocationAsTaxonomy(client);

            client.setVersion(14);
        }

        if (client.getVersion() != Client.CURRENT_VERSION)
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

    private static void changePortfolioTransactionTypeToDelivery(Client client)
    {
        for (Portfolio p : client.getPortfolios())
        {
            for (PortfolioTransaction t : p.getTransactions())
            {
                if (t.getType() == Type.TRANSFER_IN)
                    t.setType(Type.DELIVERY_INBOUND);
                else if (t.getType() == Type.TRANSFER_OUT)
                    t.setType(Type.DELIVERY_OUTBOUND);
            }
        }
    }

    private static void fixStoredChartConfigurations(Client client)
    {
        // Until now, the performance chart was showing *only* the benc hmark
        // series, not the actual performance series. Change keys as benchmark
        // values are prefixed with '[b]'

        String property = "PerformanceChartView-PICKER"; //$NON-NLS-1$

        // ConsumerPriceIndex

        String value = client.getProperty(property);
        if (value != null)
            replaceAll(client, property, value);

        int index = 0;
        while (true)
        {
            String key = property + '$' + index;
            value = client.getProperty(key);
            if (value != null)
                replaceAll(client, key, value);
            else
                break;

            index++;
        }
    }

    @SuppressWarnings("nls")
    private static void replaceAll(Client client, String property, String value)
    {
        String newValue = value.replaceAll("Security", "[b]Security") //
                        .replaceAll("ConsumerPriceIndex", "[b]ConsumerPriceIndex");
        client.setProperty(property, newValue);
    }

    private static void addAssetClassesAsTaxonomy(Client client)
    {
        Taxonomy taxonomy = new Taxonomy("asset-class", Messages.LabelAssetClasses); //$NON-NLS-1$
        Classification root = new Classification(UUID.randomUUID().toString(), Messages.LabelAssetClasses);
        taxonomy.setRootNode(root);

        for (AssetClass assetClass : AssetClass.values())
        {
            Classification classification = new Classification(root, assetClass.name(), assetClass.toString());
            classification.setWeight(Classification.ONE_HUNDRED_PERCENT / AssetClass.values().length);
            classification.setRank(assetClass.ordinal());
            root.addChild(classification);

            int rank = 1;
            for (Security security : client.getSecurities())
            {
                if (security.getType() == assetClass)
                {
                    Assignment assignment = new Assignment(security);
                    assignment.setRank(rank++);
                    classification.addAssignment(assignment);
                }
            }

            if (assetClass == AssetClass.CASH)
            {
                for (Account account : client.getAccounts())
                {
                    Assignment assignment = new Assignment(account);
                    assignment.setRank(rank++);
                    classification.addAssignment(assignment);
                }
            }
        }

        client.addTaxonomy(taxonomy);
    }

    private static void addIndustryClassificationAsTaxonomy(Client client)
    {
        IndustryClassification industry = client.getIndustryTaxonomy();
        IndustryClassification.Category category = industry.getRootCategory();

        Taxonomy taxonomy = new Taxonomy(industry.getIdentifier(), Messages.LabelIndustryClassification);
        taxonomy.setDimensions(industry.getLabels());

        Classification root = new Classification(category.getId(), category.getLabel());
        root.setDescription(category.getDescription());
        taxonomy.setRootNode(root);

        buildTree(root, category);
        assignSecurities(client, taxonomy);

        client.addTaxonomy(taxonomy);
    }

    private static void buildTree(Classification node, IndustryClassification.Category category)
    {
        int weight = Classification.ONE_HUNDRED_PERCENT / category.getChildren().size();
        int rank = 0;

        for (IndustryClassification.Category child : category.getChildren())
        {
            Classification classification = new Classification(node, child.getId(), child.getLabel());
            classification.setDescription(child.getDescription());
            classification.setWeight(weight);
            classification.setRank(rank++);
            node.addChild(classification);

            if (!child.getChildren().isEmpty())
                buildTree(classification, child);
        }

        // fix weight of last child to make it 100%
        weight = Classification.ONE_HUNDRED_PERCENT - (weight * (category.getChildren().size() - 1));
        node.getChildren().get(node.getChildren().size() - 1).setWeight(weight);
    }

    private static void assignSecurities(Client client, Taxonomy taxonomy)
    {
        int rank = 0;
        for (Security security : client.getSecurities())
        {
            Classification classification = taxonomy.getClassificationById(security.getIndustryClassification());

            if (classification != null)
            {
                Assignment assignment = new Assignment(security);
                assignment.setRank(rank++);
                classification.addAssignment(assignment);
            }
        }
    }

    private static void addAssetAllocationAsTaxonomy(Client client)
    {
        Category category = client.getRootCategory();

        Taxonomy taxonomy = new Taxonomy("asset-allocation", Messages.LabelAssetAllocation); //$NON-NLS-1$
        Classification root = new Classification(category.getUUID(), Messages.LabelAssetAllocation);
        taxonomy.setRootNode(root);

        buildTree(root, category);

        client.addTaxonomy(taxonomy);
    }

    private static void buildTree(Classification node, Category category)
    {
        int rank = 0;

        for (Category child : category.getChildren())
        {
            Classification classification = new Classification(node, child.getUUID(), child.getName());
            classification.setWeight(child.getPercentage() * Values.Weight.factor());
            classification.setRank(rank++);
            node.addChild(classification);

            buildTree(classification, child);
        }

        for (Object element : category.getElements())
        {
            Assignment assignment = element instanceof Account ? new Assignment((Account) element) : new Assignment(
                            (Security) element);
            assignment.setRank(rank++);

            node.addAssignment(assignment);
        }
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
                    xstream.alias("investment-plan", InvestmentPlan.class);

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

                    xstream.alias("buysell", BuySellEntry.class);
                    xstream.alias("account-transfer", AccountTransferEntry.class);
                    xstream.alias("portfolio-transfer", PortfolioTransferEntry.class);

                    xstream.alias("taxonomy", Taxonomy.class);
                    xstream.alias("classification", Classification.class);
                    xstream.alias("assignment", Assignment.class);
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
