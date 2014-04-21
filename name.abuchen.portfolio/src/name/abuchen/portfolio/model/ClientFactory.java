package name.abuchen.portfolio.model;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.model.Classification.Assignment;
import name.abuchen.portfolio.model.PortfolioTransaction.Type;
import name.abuchen.portfolio.online.impl.YahooFinanceQuoteFeed;
import name.abuchen.portfolio.util.ProgressMonitorInputStream;

import org.eclipse.core.runtime.IProgressMonitor;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.XStreamException;
import com.thoughtworks.xstream.converters.basic.DateConverter;

@SuppressWarnings("deprecation")
public class ClientFactory
{
    private static XStream xstream;

    public static Client load(File file, IProgressMonitor monitor) throws IOException
    {
        InputStream input = null;
        Client client = null;

        try
        {
            long bytesTotal = file.length();
            int increment = (int) Math.min(bytesTotal / 20L, Integer.MAX_VALUE);
            monitor.beginTask(MessageFormat.format(Messages.MsgReadingFile, file.getName()), 20);
            input = new ProgressMonitorInputStream(new FileInputStream(file), increment, monitor);

            client = (Client) xstream().fromXML(new InputStreamReader(input, Charset.forName("UTF-8"))); //$NON-NLS-1$
        }
        catch (FileNotFoundException e)
        {
            throw new IOException(MessageFormat.format(Messages.MsgFileNotFound, file.getAbsolutePath()), e);
        }
        catch (XStreamException e)
        {
            throw new IOException(MessageFormat.format(Messages.MsgXMLFormatInvalid, e.getMessage()), e);
        }
        finally
        {
            if (input != null)
                input.close();
        }

        if (client.getVersion() != Client.CURRENT_VERSION)
            throw new IOException(MessageFormat.format(Messages.MsgUnsupportedVersionClientFiled, client.getVersion()));

        client.doPostLoadInitialization();

        if (client.getVersion() == 1)
        {
            fixAssetClassTypes(client);
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
            generateUUIDs(client);
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
            fixStoredBenchmarkChartConfigurations(client);

            client.setVersion(13);
        }

        if (client.getVersion() == 13)
        {
            // introduce arbitrary taxonomies
            addAssetClassesAsTaxonomy(client);
            addIndustryClassificationAsTaxonomy(client);
            addAssetAllocationAsTaxonomy(client);
            fixStoredClassificationChartConfiguration(client);
            setDeprecatedFieldsToNull(client);

            client.setVersion(14);
        }

        if (client.getVersion() == 14)
        {
            // added shares to track dividends per share
            assignSharesToDividendTransactions(client);

            client.setVersion(15);
        }

        if (client.getVersion() == 15)
        {
            // do nothing --> added 'isRetired' property to account
            client.setVersion(16);
        }

        if (client.getVersion() == 16)
        {
            // do nothing --> added 'feedURL' property to account
            client.setVersion(17);
        }

        if (client.getVersion() == 17)
        {
            // do nothing --> added notes attribute
            client.setVersion(18);
        }

        if (client.getVersion() == 18)
        {
            // do nothing --> added events (stock split) to securities
            client.setVersion(19);
        }

        if (client.getVersion() == 19)
        {
            // do nothing --> added attribute types
            client.setVersion(20);
        }

        return client;
    }

    public static void save(Client client, File file) throws IOException
    {
        String xml = xstream().toXML(client);

        Writer writer = new OutputStreamWriter(new FileOutputStream(file), Charset.forName("UTF-8")); //$NON-NLS-1$
        writer.write(xml);
        writer.close();
    }

    private static void fixAssetClassTypes(Client client)
    {
        for (Security security : client.getSecurities())
        {
            if ("STOCK".equals(security.getType())) //$NON-NLS-1$
                security.setType("EQUITY"); //$NON-NLS-1$
            else if ("BOND".equals(security.getType())) //$NON-NLS-1$
                security.setType("DEBT"); //$NON-NLS-1$
        }
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

    private static void generateUUIDs(Client client)
    {
        for (Account a : client.getAccounts())
            a.generateUUID();
        for (Portfolio p : client.getPortfolios())
            p.generateUUID();
        for (Category c : client.getRootCategory().flatten())
            c.generateUUID();
    }

    @SuppressWarnings("nls")
    private static void fixStoredBenchmarkChartConfigurations(Client client)
    {
        // Until now, the performance chart was showing *only* the benchmark
        // series, not the actual performance series. Change keys as benchmark
        // values are prefixed with '[b]'

        replace(client, "PerformanceChartView-PICKER", //
                        "Security", "[b]Security", //
                        "ConsumerPriceIndex", "[b]ConsumerPriceIndex");
    }

    private static void addAssetClassesAsTaxonomy(Client client)
    {
        TaxonomyTemplate template = TaxonomyTemplate.byId("assetclasses"); //$NON-NLS-1$
        Taxonomy taxonomy = template.buildFromTemplate();
        taxonomy.setId("assetclasses"); //$NON-NLS-1$

        int rank = 1;

        Classification cash = taxonomy.getClassificationById("CASH"); //$NON-NLS-1$
        for (Account account : client.getAccounts())
        {
            Assignment assignment = new Assignment(account);
            assignment.setRank(rank++);
            cash.addAssignment(assignment);
        }

        for (Security security : client.getSecurities())
        {
            Classification classification = taxonomy.getClassificationById(security.getType());

            if (classification != null)
            {
                Assignment assignment = new Assignment(security);
                assignment.setRank(rank++);
                classification.addAssignment(assignment);
            }
        }

        client.addTaxonomy(taxonomy);
    }

    private static void addIndustryClassificationAsTaxonomy(Client client)
    {
        String oldIndustryId = client.getIndustryTaxonomy();

        Taxonomy taxonomy = null;

        if ("simple2level".equals(oldIndustryId)) //$NON-NLS-1$
            taxonomy = TaxonomyTemplate.byId(TaxonomyTemplate.INDUSTRY_SIMPLE2LEVEL).buildFromTemplate();
        else
            taxonomy = TaxonomyTemplate.byId(TaxonomyTemplate.INDUSTRY_GICS).buildFromTemplate();

        taxonomy.setId("industries"); //$NON-NLS-1$

        // add industry taxonomy only if at least one security has been assigned
        if (assignSecurities(client, taxonomy))
            client.addTaxonomy(taxonomy);
    }

    private static boolean assignSecurities(Client client, Taxonomy taxonomy)
    {
        boolean hasAssignments = false;

        int rank = 0;
        for (Security security : client.getSecurities())
        {
            Classification classification = taxonomy.getClassificationById(security.getIndustryClassification());

            if (classification != null)
            {
                Assignment assignment = new Assignment(security);
                assignment.setRank(rank++);
                classification.addAssignment(assignment);

                hasAssignments = true;
            }
        }

        return hasAssignments;
    }

    private static void addAssetAllocationAsTaxonomy(Client client)
    {
        Category category = client.getRootCategory();

        Taxonomy taxonomy = new Taxonomy("assetallocation", Messages.LabelAssetAllocation); //$NON-NLS-1$
        Classification root = new Classification(category.getUUID(), Messages.LabelAssetAllocation);
        taxonomy.setRootNode(root);

        buildTree(root, category);

        root.assignRandomColors();

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
    private static void fixStoredClassificationChartConfiguration(Client client)
    {
        String name = Classification.class.getSimpleName();
        replace(client, "PerformanceChartView-PICKER", //
                        "AssetClass", name, //
                        "Category", name);

        replace(client, "StatementOfAssetsHistoryView-PICKER", //
                        "AssetClass", name, //
                        "Category", name);
    }

    private static void replace(Client client, String property, String... replacements)
    {
        if (replacements.length % 2 != 0)
            throw new UnsupportedOperationException();

        String value = client.getProperty(property);
        if (value != null)
            replaceAll(client, property, value, replacements);

        int index = 0;
        while (true)
        {
            String key = property + '$' + index;
            value = client.getProperty(key);
            if (value != null)
                replaceAll(client, key, value, replacements);
            else
                break;

            index++;
        }
    }

    private static void replaceAll(Client client, String key, String value, String[] replacements)
    {
        String newValue = value;
        for (int ii = 0; ii < replacements.length; ii += 2)
            newValue = newValue.replaceAll(replacements[ii], replacements[ii + 1]);
        client.setProperty(key, newValue);
    }

    private static void setDeprecatedFieldsToNull(Client client)
    {
        client.setRootCategory(null);
        client.setIndustryTaxonomy(null);

        for (Security security : client.getSecurities())
        {
            security.setIndustryClassification(null);
            security.setType(null);
        }
    }

    private static void assignSharesToDividendTransactions(Client client)
    {
        for (Security security : client.getSecurities())
        {
            List<TransactionPair<?>> transactions = security.getTransactions(client);

            // sort by date of transaction
            Collections.sort(transactions, new Comparator<TransactionPair<?>>()
            {
                @Override
                public int compare(TransactionPair<?> one, TransactionPair<?> two)
                {
                    return one.getTransaction().getDate().compareTo(two.getTransaction().getDate());
                }
            });

            // count and assign number of shares by account
            Map<Account, Long> account2shares = new HashMap<Account, Long>();
            for (TransactionPair<? extends Transaction> t : transactions)
            {
                if (t.getTransaction() instanceof AccountTransaction)
                {
                    AccountTransaction accountTransaction = (AccountTransaction) t.getTransaction();

                    switch (accountTransaction.getType())
                    {
                        case DIVIDENDS:
                        case INTEREST:
                            Long shares = account2shares.get(t.getOwner());
                            accountTransaction.setShares(shares != null ? shares : 0);
                            break;
                        default:
                    }
                }
                else if (t.getTransaction() instanceof PortfolioTransaction)
                {
                    PortfolioTransaction portfolioTransaction = (PortfolioTransaction) t.getTransaction();

                    // determine account: if it exists, take the cross entry;
                    // otherwise the reference account
                    Account account = null;
                    switch (portfolioTransaction.getType())
                    {
                        case BUY:
                        case SELL:
                            if (portfolioTransaction.getCrossEntry() != null)
                                account = (Account) portfolioTransaction.getCrossEntry().getCrossEntity(
                                                portfolioTransaction);
                        case TRANSFER_IN:
                        case TRANSFER_OUT:
                        default:
                            if (account == null)
                                account = ((Portfolio) t.getOwner()).getReferenceAccount();
                    }

                    long delta = 0;
                    switch (portfolioTransaction.getType())
                    {
                        case BUY:
                        case TRANSFER_IN:
                            delta = portfolioTransaction.getShares();
                            break;
                        case SELL:
                        case TRANSFER_OUT:
                            delta = -portfolioTransaction.getShares();
                            break;
                        default:
                            break;
                    }

                    Long shares = account2shares.get(account);
                    account2shares.put(account, shares != null ? shares + delta : delta);
                }
            }
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

                    xstream.alias("buysell", BuySellEntry.class);
                    xstream.alias("account-transfer", AccountTransferEntry.class);
                    xstream.alias("portfolio-transfer", PortfolioTransferEntry.class);

                    xstream.alias("taxonomy", Taxonomy.class);
                    xstream.alias("classification", Classification.class);
                    xstream.alias("assignment", Assignment.class);

                    xstream.alias("event", SecurityEvent.class);
                }
            }
        }
        return xstream;
    }
}
