package name.abuchen.portfolio.model;

import static name.abuchen.portfolio.util.ProtobufUtil.asDecimalValue;
import static name.abuchen.portfolio.util.ProtobufUtil.asTimestamp;
import static name.abuchen.portfolio.util.ProtobufUtil.asUpdatedAtTimestamp;
import static name.abuchen.portfolio.util.ProtobufUtil.fromDecimalValue;
import static name.abuchen.portfolio.util.ProtobufUtil.fromTimestamp;
import static name.abuchen.portfolio.util.ProtobufUtil.fromUpdatedAtTimestamp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.model.AttributeType.Converter;
import name.abuchen.portfolio.model.Classification.Assignment;
import name.abuchen.portfolio.model.ClientFactory.ClientPersister;
import name.abuchen.portfolio.model.ConfigurationSet.Configuration;
import name.abuchen.portfolio.model.SecurityEvent.DividendEvent;
import name.abuchen.portfolio.model.proto.v1.PAccount;
import name.abuchen.portfolio.model.proto.v1.PAnyValue;
import name.abuchen.portfolio.model.proto.v1.PAttributeType;
import name.abuchen.portfolio.model.proto.v1.PBookmark;
import name.abuchen.portfolio.model.proto.v1.PClient;
import name.abuchen.portfolio.model.proto.v1.PConfigurationSet;
import name.abuchen.portfolio.model.proto.v1.PDashboard;
import name.abuchen.portfolio.model.proto.v1.PFullHistoricalPrice;
import name.abuchen.portfolio.model.proto.v1.PHistoricalPrice;
import name.abuchen.portfolio.model.proto.v1.PInvestmentPlan;
import name.abuchen.portfolio.model.proto.v1.PKeyValue;
import name.abuchen.portfolio.model.proto.v1.PMap;
import name.abuchen.portfolio.model.proto.v1.PPortfolio;
import name.abuchen.portfolio.model.proto.v1.PSecurity;
import name.abuchen.portfolio.model.proto.v1.PSecurityEvent;
import name.abuchen.portfolio.model.proto.v1.PSettings;
import name.abuchen.portfolio.model.proto.v1.PTaxonomy;
import name.abuchen.portfolio.model.proto.v1.PTransaction;
import name.abuchen.portfolio.model.proto.v1.PTransactionUnit;
import name.abuchen.portfolio.model.proto.v1.PWatchlist;
import name.abuchen.portfolio.money.Money;

/* package */ class ProtobufWriter implements ClientPersister
{
    private static class Lookup
    {
        private Map<String, Security> uuid2security = new HashMap<>();

        private Map<String, Portfolio> uuid2portfolio = new HashMap<>();

        private Map<String, Account> uuid2account = new HashMap<>();

        Security getSecurity(String id)
        {
            return uuid2security.get(id);
        }

        void add(Security security)
        {
            uuid2security.put(security.getUUID(), security);
        }

        Portfolio getPortfolio(String id)
        {
            return uuid2portfolio.get(id);
        }

        void add(Portfolio portfolio)
        {
            uuid2portfolio.put(portfolio.getUUID(), portfolio);
        }

        Account getAccount(String id)
        {
            return uuid2account.get(id);
        }

        void add(Account account)
        {
            uuid2account.put(account.getUUID(), account);
        }
    }

    private static final byte[] SIGNATURE = new byte[] { 'P', 'P', 'P', 'B', 'V', '1' };

    @Override
    public Client load(InputStream input) throws IOException
    {
        // check signature
        byte[] signature = new byte[SIGNATURE.length];
        int read = input.read(signature);
        if (read != SIGNATURE.length)
            throw new IOException();
        if (!Arrays.equals(signature, SIGNATURE))
            throw new IOException(Messages.MsgNotAPortflioFile);

        PClient newClient = PClient.parseFrom(input);

        Client client = new Client();
        client.setVersion(newClient.getVersion());

        // load settings first because the recreation of attributes attached to
        // securities, accounts, portfolios, and investment plans needs the meta
        // data
        loadSettings(newClient, client);

        Lookup lookup = new Lookup();

        loadSecurities(newClient, client, lookup);
        loadAccounts(newClient, client, lookup);
        loadPortfolios(newClient, client, lookup);
        loadTransactions(newClient, lookup);

        client.getProperties().putAll(newClient.getPropertiesMap());
        loadTaxonomies(newClient, client, lookup);
        loadDashboards(newClient, client);
        loadWatchlists(newClient, client, lookup);
        loadInvestmentPlans(newClient, client, lookup);

        client.getSaveFlags().add(SaveFlag.BINARY);

        ClientFactory.upgradeModel(client);

        return client;
    }

    private void loadSecurities(PClient newClient, Client client, Lookup lookup)
    {
        for (PSecurity newSecurity : newClient.getSecuritiesList())
        {
            Security security = new Security(newSecurity.getUuid());

            if (newSecurity.hasOnlineId())
                security.setOnlineId(newSecurity.getOnlineId());

            security.setName(newSecurity.getName());
            security.setCurrencyCode(newSecurity.hasCurrencyCode() ? newSecurity.getCurrencyCode() : null);
            if (newSecurity.hasTargetCurrencyCode())
                security.setTargetCurrencyCode(newSecurity.getTargetCurrencyCode());

            if (newSecurity.hasNote())
                security.setNote(newSecurity.getNote());

            if (newSecurity.hasIsin())
                security.setIsin(newSecurity.getIsin());
            if (newSecurity.hasTickerSymbol())
                security.setTickerSymbol(newSecurity.getTickerSymbol());
            if (newSecurity.hasWkn())
                security.setWkn(newSecurity.getWkn());
            if (newSecurity.hasCalendar())
                security.setCalendar(newSecurity.getCalendar());

            if (newSecurity.hasFeed())
                security.setFeed(newSecurity.getFeed());
            if (newSecurity.hasFeedURL())
                security.setFeedURL(newSecurity.getFeedURL());

            security.addAllPrices(newSecurity.getPricesList().stream()
                            .map(p -> new SecurityPrice(LocalDate.ofEpochDay(p.getDate()), p.getClose()))
                            .collect(Collectors.toList()));

            if (newSecurity.hasLatestFeed())
                security.setLatestFeed(newSecurity.getLatestFeed());
            if (newSecurity.hasLatestFeedURL())
                security.setLatestFeedURL(newSecurity.getLatestFeedURL());

            if (newSecurity.hasLatest())
            {
                PFullHistoricalPrice latest = newSecurity.getLatest();
                security.setLatest(new LatestSecurityPrice(LocalDate.ofEpochDay(latest.getDate()), latest.getClose(),
                                latest.getHigh(), latest.getLow(), latest.getVolume()));
            }

            security.getAttributes().fromProto(newSecurity.getAttributesList(), client);

            for (PSecurityEvent newEvent : newSecurity.getEventsList())
            {
                SecurityEvent event;

                switch (newEvent.getTypeValue())
                {
                    case PSecurityEvent.Type.STOCK_SPLIT_VALUE:
                        event = new SecurityEvent();
                        event.setType(SecurityEvent.Type.STOCK_SPLIT);
                        break;
                    case PSecurityEvent.Type.NOTE_VALUE:
                        event = new SecurityEvent();
                        event.setType(SecurityEvent.Type.NOTE);
                        break;
                    case PSecurityEvent.Type.DIVIDEND_PAYMENT_VALUE:
                        event = new DividendEvent();
                        ((DividendEvent) event).setPaymentDate(LocalDate.ofEpochDay(newEvent.getData(0).getInt64()));
                        ((DividendEvent) event).setAmount(
                                        Money.of(newEvent.getData(1).getString(), newEvent.getData(2).getInt64()));
                        ((DividendEvent) event).setSource(newEvent.getData(3).getString());
                        break;
                    default:
                        throw new UnsupportedOperationException();
                }

                event.setDate(LocalDate.ofEpochDay(newEvent.getDate()));
                event.setDetails(newEvent.getDetails());

                security.addEvent(event);
            }

            for (PKeyValue newProperties : newSecurity.getPropertiesList())
            {
                String key = newProperties.getKey();

                try
                {
                    SecurityProperty.Type type = SecurityProperty.Type.valueOf(key);

                    for (PKeyValue entry : newProperties.getValue().getMap().getEntriesList())
                    {
                        security.addProperty(new SecurityProperty(type, entry.getKey(), entry.getValue().getString()));
                    }
                }
                catch (IllegalArgumentException e)
                {
                    throw new UnsupportedOperationException(key);
                }
            }

            security.setRetired(newSecurity.getIsRetired());

            security.setUpdatedAt(fromUpdatedAtTimestamp(newSecurity.getUpdatedAt()));

            client.addSecurity(security);
            lookup.add(security);
        }
    }

    private void loadAccounts(PClient newClient, Client client, Lookup lookup)
    {
        for (PAccount newAccount : newClient.getAccountsList())
        {
            Account account = new Account(newAccount.getUuid(), newAccount.getName());
            account.setCurrencyCode(newAccount.getCurrencyCode());
            if (newAccount.hasNote())
                account.setNote(newAccount.getNote());
            account.setRetired(newAccount.getIsRetired());

            account.getAttributes().fromProto(newAccount.getAttributesList(), client);

            account.setUpdatedAt(fromUpdatedAtTimestamp(newAccount.getUpdatedAt()));

            client.addAccount(account);
            lookup.add(account);
        }
    }

    private void loadPortfolios(PClient newClient, Client client, Lookup lookup)
    {
        for (PPortfolio newPortfolio : newClient.getPortfoliosList())
        {
            Portfolio portfolio = new Portfolio(newPortfolio.getUuid(), newPortfolio.getName());
            if (newPortfolio.hasNote())
                portfolio.setNote(newPortfolio.getNote());
            portfolio.setRetired(newPortfolio.getIsRetired());

            if (newPortfolio.hasReferenceAccount())
                client.getAccounts().stream().filter(a -> a.getUUID().equals(newPortfolio.getReferenceAccount()))
                                .findAny().ifPresent(portfolio::setReferenceAccount);

            portfolio.getAttributes().fromProto(newPortfolio.getAttributesList(), client);

            portfolio.setUpdatedAt(fromUpdatedAtTimestamp(newPortfolio.getUpdatedAt()));

            client.addPortfolio(portfolio);
            lookup.add(portfolio);
        }
    }

    private void loadTransactions(PClient newClient, Lookup lookup)
    {
        for (PTransaction newTransaction : newClient.getTransactionsList())
        {
            PTransaction.Type type = newTransaction.getType();

            switch (type)
            {
                case PURCHASE:
                case SALE:
                    Portfolio portfolio = lookup.getPortfolio(newTransaction.getPortfolio());
                    PortfolioTransaction portfolioTx = new PortfolioTransaction(newTransaction.getUuid());

                    Account account = lookup.getAccount(newTransaction.getAccount());
                    if (!newTransaction.hasOtherUuid())
                        throw new UnsupportedOperationException();
                    AccountTransaction accountTx = new AccountTransaction(newTransaction.getOtherUuid());

                    BuySellEntry buysell = new BuySellEntry(portfolio, portfolioTx, account, accountTx);

                    buysell.setType(type == PTransaction.Type.PURCHASE ? PortfolioTransaction.Type.BUY
                                    : PortfolioTransaction.Type.SELL);

                    buysell.setDate(fromTimestamp(newTransaction.getDate()));
                    buysell.setCurrencyCode(newTransaction.getCurrencyCode());
                    buysell.setAmount(newTransaction.getAmount());

                    if (newTransaction.hasNote())
                        buysell.setNote(newTransaction.getNote());

                    buysell.setShares(newTransaction.getShares());

                    if (!newTransaction.hasSecurity())
                        throw new UnsupportedOperationException();
                    buysell.setSecurity(lookup.getSecurity(newTransaction.getSecurity()));

                    loadTransactionUnits(newTransaction, portfolioTx);

                    portfolioTx.setUpdatedAt(fromUpdatedAtTimestamp(newTransaction.getUpdatedAt()));
                    accountTx.setUpdatedAt(fromUpdatedAtTimestamp(newTransaction.getOtherUpdatedAt()));

                    buysell.insert();

                    break;

                case SECURITY_TRANSFER:
                    Portfolio source = lookup.getPortfolio(newTransaction.getPortfolio());
                    PortfolioTransaction sourceTx = new PortfolioTransaction(newTransaction.getUuid());

                    if (!newTransaction.hasOtherUuid())
                        throw new UnsupportedOperationException();
                    PortfolioTransaction targetTx = new PortfolioTransaction(newTransaction.getOtherUuid());
                    Portfolio target = lookup.getPortfolio(newTransaction.getOtherPortfolio());

                    PortfolioTransferEntry transfer = new PortfolioTransferEntry(source, sourceTx, target, targetTx);

                    transfer.setDate(fromTimestamp(newTransaction.getDate()));
                    transfer.setCurrencyCode(newTransaction.getCurrencyCode());
                    transfer.setAmount(newTransaction.getAmount());

                    if (newTransaction.hasNote())
                        transfer.setNote(newTransaction.getNote());

                    transfer.setShares(newTransaction.getShares());

                    if (!newTransaction.hasSecurity())
                        throw new UnsupportedOperationException();
                    transfer.setSecurity(lookup.getSecurity(newTransaction.getSecurity()));

                    loadTransactionUnits(newTransaction, sourceTx);

                    sourceTx.setUpdatedAt(fromUpdatedAtTimestamp(newTransaction.getUpdatedAt()));
                    targetTx.setUpdatedAt(fromUpdatedAtTimestamp(newTransaction.getOtherUpdatedAt()));

                    transfer.insert();

                    break;

                case INBOUND_DELIVERY:
                    PortfolioTransaction inbound = new PortfolioTransaction(newTransaction.getUuid());
                    inbound.setType(PortfolioTransaction.Type.DELIVERY_INBOUND);
                    loadCommonTransaction(newTransaction, inbound, lookup, true);
                    lookup.getPortfolio(newTransaction.getPortfolio()).addTransaction(inbound);

                    break;

                case OUTBOUND_DELIVERY:
                    PortfolioTransaction outbound = new PortfolioTransaction(newTransaction.getUuid());
                    outbound.setType(PortfolioTransaction.Type.DELIVERY_OUTBOUND);
                    loadCommonTransaction(newTransaction, outbound, lookup, true);
                    lookup.getPortfolio(newTransaction.getPortfolio()).addTransaction(outbound);

                    break;

                case CASH_TRANSFER:
                    Account sourceAccount = lookup.getAccount(newTransaction.getAccount());
                    AccountTransaction sourceATx = new AccountTransaction(newTransaction.getUuid());

                    Account targetAccount = lookup.getAccount(newTransaction.getOtherAccount());
                    if (!newTransaction.hasOtherUuid())
                        throw new UnsupportedOperationException();
                    AccountTransaction targetATx = new AccountTransaction(newTransaction.getOtherUuid());

                    AccountTransferEntry cashTransfer = new AccountTransferEntry(sourceAccount, sourceATx,
                                    targetAccount, targetATx);

                    cashTransfer.setDate(fromTimestamp(newTransaction.getDate()));
                    cashTransfer.setCurrencyCode(newTransaction.getCurrencyCode());
                    cashTransfer.setAmount(newTransaction.getAmount());

                    if (newTransaction.hasNote())
                        cashTransfer.setNote(newTransaction.getNote());

                    loadTransactionUnits(newTransaction, sourceATx);

                    sourceATx.getUnit(Transaction.Unit.Type.GROSS_VALUE)
                                    .ifPresent(unit -> targetATx.setMonetaryAmount(unit.getForex()));

                    sourceATx.setUpdatedAt(fromUpdatedAtTimestamp(newTransaction.getUpdatedAt()));
                    targetATx.setUpdatedAt(fromUpdatedAtTimestamp(newTransaction.getOtherUpdatedAt()));

                    cashTransfer.insert();

                    break;

                case DEPOSIT:
                    AccountTransaction deposit = new AccountTransaction(newTransaction.getUuid());
                    deposit.setType(AccountTransaction.Type.DEPOSIT);
                    loadCommonTransaction(newTransaction, deposit, lookup, false);
                    lookup.getAccount(newTransaction.getAccount()).addTransaction(deposit);

                    break;

                case REMOVAL:
                    AccountTransaction removal = new AccountTransaction(newTransaction.getUuid());
                    removal.setType(AccountTransaction.Type.REMOVAL);
                    loadCommonTransaction(newTransaction, removal, lookup, false);
                    lookup.getAccount(newTransaction.getAccount()).addTransaction(removal);

                    break;

                case DIVIDEND:
                    AccountTransaction dividend = new AccountTransaction(newTransaction.getUuid());
                    dividend.setType(AccountTransaction.Type.DIVIDENDS);
                    loadCommonTransaction(newTransaction, dividend, lookup, true);
                    lookup.getAccount(newTransaction.getAccount()).addTransaction(dividend);

                    break;

                case INTEREST:
                    AccountTransaction interest = new AccountTransaction(newTransaction.getUuid());
                    interest.setType(AccountTransaction.Type.INTEREST);
                    loadCommonTransaction(newTransaction, interest, lookup, false);
                    lookup.getAccount(newTransaction.getAccount()).addTransaction(interest);

                    break;

                case INTEREST_CHARGE:
                    AccountTransaction interestCharge = new AccountTransaction(newTransaction.getUuid());
                    interestCharge.setType(AccountTransaction.Type.INTEREST_CHARGE);
                    loadCommonTransaction(newTransaction, interestCharge, lookup, false);
                    lookup.getAccount(newTransaction.getAccount()).addTransaction(interestCharge);

                    break;

                case TAX:
                    AccountTransaction tax = new AccountTransaction(newTransaction.getUuid());
                    tax.setType(AccountTransaction.Type.TAXES);
                    loadCommonTransaction(newTransaction, tax, lookup, false);
                    lookup.getAccount(newTransaction.getAccount()).addTransaction(tax);

                    break;

                case TAX_REFUND:
                    AccountTransaction taxRefund = new AccountTransaction(newTransaction.getUuid());
                    taxRefund.setType(AccountTransaction.Type.TAX_REFUND);
                    loadCommonTransaction(newTransaction, taxRefund, lookup, false);
                    lookup.getAccount(newTransaction.getAccount()).addTransaction(taxRefund);

                    break;

                case FEE:
                    AccountTransaction fee = new AccountTransaction(newTransaction.getUuid());
                    fee.setType(AccountTransaction.Type.FEES);
                    loadCommonTransaction(newTransaction, fee, lookup, false);
                    lookup.getAccount(newTransaction.getAccount()).addTransaction(fee);

                    break;

                case FEE_REFUND:
                    AccountTransaction feeRefund = new AccountTransaction(newTransaction.getUuid());
                    feeRefund.setType(AccountTransaction.Type.FEES_REFUND);
                    loadCommonTransaction(newTransaction, feeRefund, lookup, false);
                    lookup.getAccount(newTransaction.getAccount()).addTransaction(feeRefund);

                    break;

                default:
                    throw new UnsupportedOperationException(type.toString());
            }
        }
    }

    private void loadCommonTransaction(PTransaction newTransaction, Transaction t, Lookup lookup,
                    boolean requiresSecurity)
    {
        t.setDateTime(fromTimestamp(newTransaction.getDate()));
        t.setCurrencyCode(newTransaction.getCurrencyCode());
        t.setAmount(newTransaction.getAmount());

        if (newTransaction.hasNote())
            t.setNote(newTransaction.getNote());
        if (newTransaction.hasSource())
            t.setSource(newTransaction.getSource());

        t.setShares(newTransaction.getShares());

        boolean hasSecurity = newTransaction.hasSecurity();
        if (requiresSecurity && !hasSecurity)
            throw new UnsupportedOperationException();
        if (hasSecurity)
            t.setSecurity(lookup.getSecurity(newTransaction.getSecurity()));

        loadTransactionUnits(newTransaction, t);

        t.setUpdatedAt(fromUpdatedAtTimestamp(newTransaction.getUpdatedAt()));
    }

    private void loadTransactionUnits(PTransaction newTransaction, Transaction t)
    {
        List<PTransactionUnit> newUnits = newTransaction.getUnitsList();

        for (PTransactionUnit newUnit : newUnits)
        {
            Transaction.Unit.Type type;

            switch (newUnit.getTypeValue())
            {
                case PTransactionUnit.Type.GROSS_VALUE_VALUE:
                    type = Transaction.Unit.Type.GROSS_VALUE;
                    break;
                case PTransactionUnit.Type.TAX_VALUE:
                    type = Transaction.Unit.Type.TAX;
                    break;
                case PTransactionUnit.Type.FEE_VALUE:
                    type = Transaction.Unit.Type.FEE;
                    break;
                default:
                    throw new UnsupportedOperationException();
            }

            Money amount = Money.of(newUnit.getCurrencyCode(), newUnit.getAmount());

            if (newUnit.hasFxAmount())
            {
                Money forex = Money.of(newUnit.getFxCurrencyCode(), newUnit.getFxAmount());
                BigDecimal exchangeRate = fromDecimalValue(newUnit.getFxRateToBase());
                t.addUnit(new Transaction.Unit(type, amount, forex, exchangeRate));
            }
            else
            {
                t.addUnit(new Transaction.Unit(type, amount));
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void loadSettings(PClient newClient, Client client) throws IOException
    {
        PSettings newSettings = newClient.getSettings();

        ClientSettings settings = client.getSettings();

        // remove all auto-created bookmarks
        settings.clearBookmarks();

        for (PBookmark newBookmark : newSettings.getBookmarksList())
        {
            settings.getBookmarks().add(new Bookmark(newBookmark.getLabel(), newBookmark.getPattern()));
        }

        // remove all auto-created attributes
        settings.clearAttributeTypes();

        for (PAttributeType newAttributeType : newSettings.getAttributeTypesList())
        {
            try
            {
                AttributeType attributeType = new AttributeType(newAttributeType.getId());
                attributeType.setName(newAttributeType.getName());
                attributeType.setColumnLabel(newAttributeType.getColumnLabel());
                if (newAttributeType.hasSource())
                    attributeType.setSource(newAttributeType.getSource());
                attributeType.setTarget((Class<? extends Attributable>) Class.forName(newAttributeType.getTarget()));
                attributeType.setType(Class.forName(newAttributeType.getType()));
                attributeType.setConverter(
                                (Class<? extends Converter>) Class.forName(newAttributeType.getConverterClass()));

                attributeType.getProperties().fromProto(newAttributeType.getProperties());

                settings.addAttributeType(attributeType);
            }
            catch (ClassNotFoundException e)
            {
                throw new IOException(e);
            }
        }

        for (PConfigurationSet newConfigurationSet : newSettings.getConfigurationSetsList())
        {
            String key = newConfigurationSet.getKey();

            Configuration config = new Configuration(newConfigurationSet.getUuid());
            config.setName(newConfigurationSet.getName());
            config.setData(newConfigurationSet.getData());

            settings.getConfigurationSet(key).add(config);
        }
    }

    private void loadTaxonomies(PClient newClient, Client client, Lookup lookup)
    {
        for (PTaxonomy newTaxonomy : newClient.getTaxonomiesList())
        {

            Taxonomy taxonomy = new Taxonomy(newTaxonomy.getId(), newTaxonomy.getName());
            if (newTaxonomy.hasSource())
                taxonomy.setSource(newTaxonomy.getSource());
            taxonomy.setDimensions(new ArrayList<>(newTaxonomy.getDimensionsList()));

            Map<String, Classification> uuid2classification = new HashMap<>();

            for (PTaxonomy.Classification newClassification : newTaxonomy.getClassificationsList())
            {
                Classification classification = new Classification(newClassification.getId(),
                                newClassification.getName());
                if (newClassification.hasNote())
                    classification.setNote(newClassification.getNote());
                classification.setColor(newClassification.getColor());
                classification.setWeight(newClassification.getWeight());
                classification.setRank(newClassification.getRank());

                classification.setData(newClassification.getDataList());

                for (PTaxonomy.Assignment newAssignment : newClassification.getAssignmentsList())
                {
                    InvestmentVehicle vehicle = lookup.getSecurity(newAssignment.getInvestmentVehicle());
                    if (vehicle == null)
                        vehicle = lookup.getAccount(newAssignment.getInvestmentVehicle());
                    if (vehicle == null)
                        throw new UnsupportedOperationException();

                    Assignment assignment = new Assignment(vehicle);
                    assignment.setWeight(newAssignment.getWeight());
                    assignment.setRank(newAssignment.getRank());

                    assignment.setData(newAssignment.getDataList());

                    classification.addAssignment(assignment);
                }

                if (!newClassification.hasParentId())
                {
                    taxonomy.setRootNode(classification);
                }
                else
                {
                    Classification parent = uuid2classification.get(newClassification.getParentId());
                    if (parent == null)
                        throw new UnsupportedOperationException(newClassification.getParentId());
                    classification.setParent(parent);
                    parent.addChild(classification);
                }

                uuid2classification.put(classification.getId(), classification);
            }

            client.addTaxonomy(taxonomy);
        }
    }

    private void loadDashboards(PClient newClient, Client client)
    {
        for (PDashboard newDashboard : newClient.getDashboardsList())
        {
            Dashboard dashboard = new Dashboard();
            dashboard.setName(newDashboard.getName());
            dashboard.getConfiguration().putAll(newDashboard.getConfigurationMap());

            for (PDashboard.Column newColumn : newDashboard.getColumnsList())
            {
                Dashboard.Column column = new Dashboard.Column();
                column.setWeight(newColumn.getWeight());

                for (PDashboard.Widget newWidget : newColumn.getWidgetsList())
                {
                    Dashboard.Widget widget = new Dashboard.Widget();
                    widget.setType(newWidget.getType());
                    widget.setLabel(newWidget.getLabel());
                    widget.getConfiguration().putAll(newWidget.getConfigurationMap());
                    column.getWidgets().add(widget);
                }
                dashboard.getColumns().add(column);
            }

            client.addDashboard(dashboard);
        }
    }

    private void loadWatchlists(PClient newClient, Client client, Lookup lookup)
    {
        for (PWatchlist newWatchlist : newClient.getWatchlistsList())
        {
            Watchlist watchlist = new Watchlist();
            watchlist.setName(newWatchlist.getName());

            for (String uuid : newWatchlist.getSecuritiesList())
            {
                Security security = lookup.getSecurity(uuid);
                if (security == null)
                    throw new UnsupportedOperationException(uuid);
                watchlist.addSecurity(security);
            }

            client.getWatchlists().add(watchlist);
        }
    }

    private void loadInvestmentPlans(PClient newClient, Client client, Lookup lookup)
    {
        if (newClient.getPlansCount() == 0)
            return;

        Map<String, Transaction> uuid2transaction = new HashMap<>();
        client.getAccounts().stream().flatMap(a -> a.getTransactions().stream())
                        .forEach(t -> uuid2transaction.put(t.getUUID(), t));
        client.getPortfolios().stream().flatMap(p -> p.getTransactions().stream())
                        .forEach(t -> uuid2transaction.put(t.getUUID(), t));

        for (PInvestmentPlan newPlan : newClient.getPlansList())
        {
            InvestmentPlan plan = new InvestmentPlan();
            plan.setName(newPlan.getName());
            if (newPlan.hasNote())
                plan.setNote(newPlan.getNote());

            if (newPlan.hasSecurity())
                plan.setSecurity(lookup.getSecurity(newPlan.getSecurity()));
            if (newPlan.hasPortfolio())
                plan.setPortfolio(lookup.getPortfolio(newPlan.getPortfolio()));
            if (newPlan.hasAccount())
                plan.setAccount(lookup.getAccount(newPlan.getAccount()));

            plan.getAttributes().fromProto(newPlan.getAttributesList(), client);

            plan.setAutoGenerate(newPlan.getAutoGenerate());
            plan.setStart(LocalDate.ofEpochDay(newPlan.getDate()));
            plan.setInterval(newPlan.getInterval());
            plan.setAmount(newPlan.getAmount());
            plan.setFees(newPlan.getFees());

            for (String uuid : newPlan.getTransactionsList())
            {
                Transaction t = uuid2transaction.get(uuid);
                if (t == null)
                    throw new UnsupportedOperationException(uuid);
                plan.getTransactions().add(t);
            }

            client.addPlan(plan);
        }
    }

    @Override
    public void save(Client client, OutputStream output) throws IOException
    {
        PClient.Builder newClient = PClient.newBuilder();

        newClient.setVersion(client.getVersion());

        saveSecurities(client, newClient);
        saveAccounts(client, newClient);
        savePortfolios(client, newClient);
        saveTransactions(client, newClient);

        newClient.putAllProperties(client.getProperties());
        saveTaxonomies(client, newClient);
        saveSettings(client, newClient);
        saveDashboards(client, newClient);
        saveWatchlists(client, newClient);
        saveInvestmentPlans(client, newClient);

        // write signature
        output.write(SIGNATURE);

        newClient.build().writeTo(output);
    }

    private void saveSecurities(Client client, PClient.Builder newClient)
    {
        for (Security security : client.getSecurities())
        {
            PSecurity.Builder newSecurity = PSecurity.newBuilder();
            newSecurity.setUuid(security.getUUID());

            if (security.getOnlineId() != null)
                newSecurity.setOnlineId(security.getOnlineId());

            newSecurity.setName(security.getName());
            if (security.getCurrencyCode() != null)
                newSecurity.setCurrencyCode(security.getCurrencyCode());
            if (security.getTargetCurrencyCode() != null)
                newSecurity.setTargetCurrencyCode(security.getTargetCurrencyCode());

            if (security.getNote() != null)
                newSecurity.setNote(security.getNote());

            if (security.getIsin() != null)
                newSecurity.setIsin(security.getIsin());
            if (security.getTickerSymbol() != null)
                newSecurity.setTickerSymbol(security.getTickerSymbol());
            if (security.getWkn() != null)
                newSecurity.setWkn(security.getWkn());
            if (security.getCalendar() != null)
                newSecurity.setCalendar(security.getCalendar());

            if (security.getFeed() != null)
                newSecurity.setFeed(security.getFeed());
            if (security.getFeedURL() != null)
                newSecurity.setFeedURL(security.getFeedURL());

            for (SecurityPrice price : security.getPrices())
            {
                newSecurity.addPrices(PHistoricalPrice.newBuilder().setDate(price.getDate().toEpochDay())
                                .setClose(price.getValue()).build());
            }

            if (security.getLatestFeed() != null)
                newSecurity.setLatestFeed(security.getLatestFeed());
            if (security.getLatestFeedURL() != null)
                newSecurity.setLatestFeedURL(security.getLatestFeedURL());

            LatestSecurityPrice latest = security.getLatest();
            if (latest != null)
            {
                newSecurity.setLatest(PFullHistoricalPrice.newBuilder() //
                                .setDate(latest.getDate().toEpochDay()) //
                                .setClose(latest.getValue()) //
                                .setHigh(latest.getHigh()) //
                                .setLow(latest.getLow()) //
                                .setVolume(latest.getVolume()) //
                                .build());
            }

            newSecurity.addAllAttributes(security.getAttributes().toProto(client));

            security.getEvents().forEach(event -> {
                PSecurityEvent.Builder newEvent = PSecurityEvent.newBuilder();

                switch (event.getType())
                {
                    case STOCK_SPLIT:
                        newEvent.setTypeValue(PSecurityEvent.Type.STOCK_SPLIT_VALUE);
                        break;
                    case NOTE:
                        newEvent.setTypeValue(PSecurityEvent.Type.NOTE_VALUE);
                        break;
                    case DIVIDEND_PAYMENT:
                        newEvent.setTypeValue(PSecurityEvent.Type.DIVIDEND_PAYMENT_VALUE);

                        DividendEvent dividendEvent = (DividendEvent) event;
                        newEvent.addData(PAnyValue.newBuilder().setInt64(dividendEvent.getPaymentDate().toEpochDay()));
                        newEvent.addData(PAnyValue.newBuilder().setString(dividendEvent.getAmount().getCurrencyCode()));
                        newEvent.addData(PAnyValue.newBuilder().setInt64(dividendEvent.getAmount().getAmount()));
                        newEvent.addData(PAnyValue.newBuilder().setString(dividendEvent.getSource()));
                        break;
                    default:
                        throw new UnsupportedOperationException();
                }

                newEvent.setDate(event.getDate().toEpochDay());
                if (event.getDetails() != null)
                    newEvent.setDetails(event.getDetails());
                newSecurity.addEvents(newEvent);

            });

            Map<String, PMap.Builder> data = new HashMap<>();
            security.getProperties().forEach(p -> {
                PMap.Builder map = data.computeIfAbsent(p.getType().name(), k -> PMap.newBuilder());
                map.addEntries(PKeyValue.newBuilder().setKey(p.getName())
                                .setValue(PAnyValue.newBuilder().setString(p.getValue())).build());
            });

            if (!data.isEmpty())
            {
                data.forEach((k, v) -> newSecurity.addProperties(
                                PKeyValue.newBuilder().setKey(k).setValue(PAnyValue.newBuilder().setMap(v))));
            }

            newSecurity.setIsRetired(security.isRetired());

            newSecurity.setUpdatedAt(asUpdatedAtTimestamp(security.getUpdatedAt()));

            newClient.addSecurities(newSecurity.build());
        }
    }

    private void saveAccounts(Client client, PClient.Builder newClient)
    {
        for (Account account : client.getAccounts())
        {
            PAccount.Builder newAccount = PAccount.newBuilder();

            newAccount.setUuid(account.getUUID());
            newAccount.setName(account.getName());
            newAccount.setCurrencyCode(account.getCurrencyCode());
            if (account.getNote() != null)
                newAccount.setNote(account.getNote());
            newAccount.setIsRetired(account.isRetired());

            newAccount.addAllAttributes(account.getAttributes().toProto(client));

            newAccount.setUpdatedAt(asUpdatedAtTimestamp(account.getUpdatedAt()));

            newClient.addAccounts(newAccount);
        }
    }

    private void savePortfolios(Client client, PClient.Builder newClient)
    {
        for (Portfolio portfolio : client.getPortfolios())
        {
            PPortfolio.Builder newPortfolio = PPortfolio.newBuilder();

            newPortfolio.setUuid(portfolio.getUUID());
            newPortfolio.setName(portfolio.getName());
            if (portfolio.getNote() != null)
                newPortfolio.setNote(portfolio.getNote());
            newPortfolio.setIsRetired(portfolio.isRetired());

            if (portfolio.getReferenceAccount() != null)
                newPortfolio.setReferenceAccount(portfolio.getReferenceAccount().getUUID());

            newPortfolio.addAllAttributes(portfolio.getAttributes().toProto(client));

            newPortfolio.setUpdatedAt(asUpdatedAtTimestamp(portfolio.getUpdatedAt()));

            newClient.addPortfolios(newPortfolio);
        }
    }

    private void saveTransactions(Client client, PClient.Builder newClient)
    {
        for (Portfolio portfolio : client.getPortfolios())
        {
            portfolio.getTransactions().stream().filter(t -> t.getType() != PortfolioTransaction.Type.TRANSFER_IN)
                            .forEach(t -> addTransaction(newClient, portfolio, t));
        }

        EnumSet<AccountTransaction.Type> exclude = EnumSet.of(AccountTransaction.Type.TRANSFER_IN,
                        AccountTransaction.Type.BUY, AccountTransaction.Type.SELL);

        for (Account account : client.getAccounts())
        {
            account.getTransactions().stream().filter(t -> !exclude.contains(t.getType()))
                            .forEach(t -> addTransaction(newClient, account, t));
        }

    }

    private void addTransaction(PClient.Builder newClient, Portfolio portfolio, PortfolioTransaction t)
    {
        PTransaction.Builder newTransaction = PTransaction.newBuilder();
        newTransaction.setPortfolio(portfolio.getUUID());
        newTransaction.setUuid(t.getUUID());

        switch (t.getType())
        {
            case BUY:
                newTransaction.setTypeValue(PTransaction.Type.PURCHASE_VALUE);
                newTransaction.setAccount(t.getCrossEntry().getCrossOwner(t).getUUID());
                newTransaction.setOtherUuid(t.getCrossEntry().getCrossTransaction(t).getUUID());
                newTransaction.setOtherUpdatedAt(
                                asUpdatedAtTimestamp(t.getCrossEntry().getCrossTransaction(t).getUpdatedAt()));
                break;
            case SELL:
                newTransaction.setTypeValue(PTransaction.Type.SALE_VALUE);
                newTransaction.setAccount(t.getCrossEntry().getCrossOwner(t).getUUID());
                newTransaction.setOtherUuid(t.getCrossEntry().getCrossTransaction(t).getUUID());
                newTransaction.setOtherUpdatedAt(
                                asUpdatedAtTimestamp(t.getCrossEntry().getCrossTransaction(t).getUpdatedAt()));
                break;
            case TRANSFER_OUT:
                newTransaction.setTypeValue(PTransaction.Type.SECURITY_TRANSFER_VALUE);
                newTransaction.setOtherPortfolio(t.getCrossEntry().getCrossOwner(t).getUUID());
                newTransaction.setOtherUuid(t.getCrossEntry().getCrossTransaction(t).getUUID());
                newTransaction.setOtherUpdatedAt(
                                asUpdatedAtTimestamp(t.getCrossEntry().getCrossTransaction(t).getUpdatedAt()));
                break;
            case DELIVERY_INBOUND:
                newTransaction.setTypeValue(PTransaction.Type.INBOUND_DELIVERY_VALUE);
                break;
            case DELIVERY_OUTBOUND:
                newTransaction.setTypeValue(PTransaction.Type.OUTBOUND_DELIVERY_VALUE);
                break;
            case TRANSFER_IN:
            default:
                throw new UnsupportedOperationException();
        }

        saveCommonTransaction(t, newTransaction);

        newClient.addTransactions(newTransaction.build());
    }

    private void addTransaction(PClient.Builder newClient, Account account, AccountTransaction t)
    {
        PTransaction.Builder newTransaction = PTransaction.newBuilder();
        newTransaction.setAccount(account.getUUID());
        newTransaction.setUuid(t.getUUID());

        switch (t.getType())
        {
            case DEPOSIT:
                newTransaction.setTypeValue(PTransaction.Type.DEPOSIT_VALUE);
                break;
            case REMOVAL:
                newTransaction.setTypeValue(PTransaction.Type.REMOVAL_VALUE);
                break;
            case INTEREST:
                newTransaction.setTypeValue(PTransaction.Type.INTEREST_VALUE);
                break;
            case INTEREST_CHARGE:
                newTransaction.setTypeValue(PTransaction.Type.INTEREST_CHARGE_VALUE);
                break;
            case DIVIDENDS:
                newTransaction.setTypeValue(PTransaction.Type.DIVIDEND_VALUE);
                break;
            case FEES:
                newTransaction.setTypeValue(PTransaction.Type.FEE_VALUE);
                break;
            case FEES_REFUND:
                newTransaction.setTypeValue(PTransaction.Type.FEE_REFUND_VALUE);
                break;
            case TAXES:
                newTransaction.setTypeValue(PTransaction.Type.TAX_VALUE);
                break;
            case TAX_REFUND:
                newTransaction.setTypeValue(PTransaction.Type.TAX_REFUND_VALUE);
                break;
            case TRANSFER_OUT:
                newTransaction.setTypeValue(PTransaction.Type.CASH_TRANSFER_VALUE);
                newTransaction.setOtherAccount(t.getCrossEntry().getCrossOwner(t).getUUID());
                newTransaction.setOtherUuid(t.getCrossEntry().getCrossTransaction(t).getUUID());
                newTransaction.setOtherUpdatedAt(
                                asUpdatedAtTimestamp(t.getCrossEntry().getCrossTransaction(t).getUpdatedAt()));
                break;
            case BUY:
            case SELL:
            case TRANSFER_IN:
                // nothing to do - the cross entry is serialized
                break;
            default:
                throw new UnsupportedOperationException();
        }

        saveCommonTransaction(t, newTransaction);

        newClient.addTransactions(newTransaction.build());
    }

    private void saveCommonTransaction(Transaction t, PTransaction.Builder newTransaction)
    {
        newTransaction.setDate(asTimestamp(t.getDateTime()));
        newTransaction.setCurrencyCode(t.getCurrencyCode());
        newTransaction.setAmount(t.getAmount());

        if (t.getNote() != null)
            newTransaction.setNote(t.getNote());
        if (t.getSource() != null)
            newTransaction.setSource(t.getSource());

        newTransaction.setShares(t.getShares());

        if (t.getSecurity() != null)
            newTransaction.setSecurity(t.getSecurity().getUUID());

        t.getUnits().forEach(unit -> addUnit(unit, newTransaction));

        newTransaction.setUpdatedAt(asUpdatedAtTimestamp(t.getUpdatedAt()));
    }

    private void addUnit(Transaction.Unit unit, PTransaction.Builder newTransaction)
    {
        PTransactionUnit.Builder newUnit = PTransactionUnit.newBuilder();

        switch (unit.getType())
        {
            case GROSS_VALUE:
                newUnit.setTypeValue(PTransactionUnit.Type.GROSS_VALUE_VALUE);
                break;
            case TAX:
                newUnit.setTypeValue(PTransactionUnit.Type.TAX_VALUE);
                break;
            case FEE:
                newUnit.setTypeValue(PTransactionUnit.Type.FEE_VALUE);
                break;
            default:
                throw new UnsupportedOperationException();
        }

        newUnit.setAmount(unit.getAmount().getAmount());
        newUnit.setCurrencyCode(unit.getAmount().getCurrencyCode());

        if (unit.getForex() != null)
        {
            newUnit.setFxAmount(unit.getForex().getAmount());
            newUnit.setFxCurrencyCode(unit.getForex().getCurrencyCode());
            newUnit.setFxRateToBase(asDecimalValue(unit.getExchangeRate()));
        }

        newTransaction.addUnits(newUnit);
    }

    private void saveTaxonomies(Client client, PClient.Builder newClient)
    {
        for (Taxonomy taxonomy : client.getTaxonomies())
        {
            PTaxonomy.Builder newTaxonomy = PTaxonomy.newBuilder();
            newTaxonomy.setId(taxonomy.getId());
            newTaxonomy.setName(taxonomy.getName());
            if (taxonomy.getSource() != null)
                newTaxonomy.setSource(taxonomy.getSource());
            if (taxonomy.getDimensions() != null)
                newTaxonomy.addAllDimensions(taxonomy.getDimensions());

            // flat list of all classifications

            for (Classification classification : taxonomy.getAllClassifications())
            {
                PTaxonomy.Classification.Builder newClassification = PTaxonomy.Classification.newBuilder();

                newClassification.setId(classification.getId());
                if (classification.getParent() != null)
                    newClassification.setParentId(classification.getParent().getId());

                newClassification.setName(classification.getName());
                if (classification.getNote() != null)
                    newClassification.setNote(classification.getNote());
                newClassification.setColor(classification.getColor());
                newClassification.setRank(classification.getRank());
                newClassification.setWeight(classification.getWeight());

                newClassification.addAllData(classification.getData());

                for (Assignment assignment : classification.getAssignments())
                {
                    PTaxonomy.Assignment.Builder newAssignment = PTaxonomy.Assignment.newBuilder();
                    newAssignment.setInvestmentVehicle(assignment.getInvestmentVehicle().getUUID());
                    newAssignment.setWeight(assignment.getWeight());
                    newAssignment.setRank(assignment.getRank());

                    newAssignment.addAllData(assignment.getData());

                    newClassification.addAssignments(newAssignment);
                }

                newTaxonomy.addClassifications(newClassification);
            }

            newClient.addTaxonomies(newTaxonomy);
        }
    }

    private void saveSettings(Client client, PClient.Builder newClient)
    {
        PSettings.Builder newSettings = PSettings.newBuilder();

        ClientSettings settings = client.getSettings();

        settings.getBookmarks().forEach(b -> {
            PBookmark.Builder newBookmark = PBookmark.newBuilder();
            newBookmark.setLabel(b.getLabel());
            newBookmark.setPattern(b.getPattern());
            newSettings.addBookmarks(newBookmark);
        });

        settings.getAttributeTypes().forEach(t -> {
            PAttributeType.Builder newAttributeType = PAttributeType.newBuilder();
            newAttributeType.setId(t.getId());
            newAttributeType.setName(t.getName());
            newAttributeType.setColumnLabel(t.getColumnLabel());
            if (t.getSource() != null)
                newAttributeType.setSource(t.getSource());
            newAttributeType.setTarget(t.getTarget().getName());
            newAttributeType.setType(t.getType().getName());
            newAttributeType.setConverterClass(t.getConverter().getClass().getName());

            newAttributeType.setProperties(t.getProperties().toProto());

            newSettings.addAttributeTypes(newAttributeType);
        });

        for (Map.Entry<String, ConfigurationSet> entry : settings.getConfigurationSets())
        {
            entry.getValue().getConfigurations().forEach(config -> {

                // configuration data can be null if the file is empty with no
                // securities
                if (config.getData() != null)
                {
                    PConfigurationSet.Builder newConfigurationSet = PConfigurationSet.newBuilder();
                    newConfigurationSet.setKey(entry.getKey());
                    newConfigurationSet.setUuid(config.getUUID());
                    newConfigurationSet.setName(config.getName());
                    newConfigurationSet.setData(config.getData());
                    newSettings.addConfigurationSets(newConfigurationSet);
                }

            });
        }

        newClient.setSettings(newSettings);
    }

    private void saveDashboards(Client client, PClient.Builder newClient)
    {
        client.getDashboards().forEach(dashboard -> {
            PDashboard.Builder newDashboard = PDashboard.newBuilder();

            newDashboard.setName(dashboard.getName());
            newDashboard.putAllConfiguration(dashboard.getConfiguration());

            for (Dashboard.Column column : dashboard.getColumns())
            {
                PDashboard.Column.Builder newColumn = PDashboard.Column.newBuilder();
                newColumn.setWeight(column.getWeight());

                if (column.getWidgets() != null)
                {
                    for (Dashboard.Widget widget : column.getWidgets())
                    {
                        PDashboard.Widget.Builder newWidget = PDashboard.Widget.newBuilder();
                        newWidget.setType(widget.getType());
                        newWidget.setLabel(widget.getLabel());
                        newWidget.putAllConfiguration(widget.getConfiguration());
                        newColumn.addWidgets(newWidget);
                    }
                }

                newDashboard.addColumns(newColumn);
            }

            newClient.addDashboards(newDashboard);
        });
    }

    private void saveWatchlists(Client client, PClient.Builder newClient)
    {
        client.getWatchlists().forEach(watchlist -> {
            PWatchlist.Builder newWatchlist = PWatchlist.newBuilder();
            newWatchlist.setName(watchlist.getName());
            newWatchlist.addAllSecurities(
                            watchlist.getSecurities().stream().map(Security::getUUID).collect(Collectors.toList()));

            newClient.addWatchlists(newWatchlist);
        });
    }

    private void saveInvestmentPlans(Client client, PClient.Builder newClient)
    {
        client.getPlans().forEach(plan -> {
            PInvestmentPlan.Builder newPlan = PInvestmentPlan.newBuilder();
            newPlan.setName(plan.getName());
            if (plan.getNote() != null)
                newPlan.setNote(plan.getNote());

            if (plan.getSecurity() != null)
                newPlan.setSecurity(plan.getSecurity().getUUID());
            if (plan.getPortfolio() != null)
                newPlan.setPortfolio(plan.getPortfolio().getUUID());
            if (plan.getAccount() != null)
                newPlan.setAccount(plan.getAccount().getUUID());

            newPlan.addAllAttributes(plan.getAttributes().toProto(client));

            newPlan.setAutoGenerate(plan.isAutoGenerate());
            newPlan.setDate(plan.getStart().toEpochDay());
            newPlan.setInterval(plan.getInterval());
            newPlan.setAmount(plan.getAmount());
            newPlan.setFees(plan.getFees());

            plan.getTransactions().forEach(t -> newPlan.addTransactions(t.getUUID()));

            newClient.addPlans(newPlan);
        });
    }
}
