package name.abuchen.portfolio.model;

import static name.abuchen.portfolio.util.CollectorsUtil.toMutableList;
import static name.abuchen.portfolio.util.ProtobufUtil.asDecimalValue;
import static name.abuchen.portfolio.util.ProtobufUtil.asLocalDateTime;
import static name.abuchen.portfolio.util.ProtobufUtil.asTimestamp;
import static name.abuchen.portfolio.util.ProtobufUtil.asUpdatedAtTimestamp;
import static name.abuchen.portfolio.util.ProtobufUtil.fromDecimalValue;
import static name.abuchen.portfolio.util.ProtobufUtil.fromLocalDateTime;
import static name.abuchen.portfolio.util.ProtobufUtil.fromTimestamp;
import static name.abuchen.portfolio.util.ProtobufUtil.fromUpdatedAtTimestamp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.protobuf.Any;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.PortfolioLog;
import name.abuchen.portfolio.model.AttributeType.Converter;
import name.abuchen.portfolio.model.Classification.Assignment;
import name.abuchen.portfolio.model.ClientFactory.ClientPersister;
import name.abuchen.portfolio.model.ConfigurationSet.Configuration;
import name.abuchen.portfolio.model.SecurityEvent.DividendEvent;
import name.abuchen.portfolio.model.ledger.Ledger;
import name.abuchen.portfolio.model.ledger.LedgerDiagnosticMessageFormatter;
import name.abuchen.portfolio.model.ledger.LedgerEntry;
import name.abuchen.portfolio.model.ledger.LedgerParameter;
import name.abuchen.portfolio.model.ledger.LedgerParameter.ValueKind;
import name.abuchen.portfolio.model.ledger.LedgerPosting;
import name.abuchen.portfolio.model.ledger.LedgerProjectionRef;
import name.abuchen.portfolio.model.ledger.LedgerProjectionRole;
import name.abuchen.portfolio.model.ledger.LedgerStructuralValidator;
import name.abuchen.portfolio.model.ledger.ProjectionMembership;
import name.abuchen.portfolio.model.ledger.ProjectionMembershipRole;
import name.abuchen.portfolio.model.ledger.configuration.LedgerEntryType;
import name.abuchen.portfolio.model.ledger.configuration.LedgerParameterType;
import name.abuchen.portfolio.model.ledger.configuration.LedgerPostingType;
import name.abuchen.portfolio.model.ledger.legacy.LegacyTransactionToLedgerMigrator;
import name.abuchen.portfolio.model.ledger.projection.LedgerBackedTransaction;
import name.abuchen.portfolio.model.ledger.projection.LedgerProjectionService;
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
import name.abuchen.portfolio.model.proto.v1.PInvestmentPlan.Type;
import name.abuchen.portfolio.model.proto.v1.PInvestmentPlanLedgerExecutionRef;
import name.abuchen.portfolio.model.proto.v1.PKeyValue;
import name.abuchen.portfolio.model.proto.v1.PLedger;
import name.abuchen.portfolio.model.proto.v1.PLedgerEntry;
import name.abuchen.portfolio.model.proto.v1.PLedgerParameter;
import name.abuchen.portfolio.model.proto.v1.PLedgerParameterValueKind;
import name.abuchen.portfolio.model.proto.v1.PLedgerPosting;
import name.abuchen.portfolio.model.proto.v1.PLedgerProjectionMembership;
import name.abuchen.portfolio.model.proto.v1.PLedgerProjectionMembershipRole;
import name.abuchen.portfolio.model.proto.v1.PLedgerProjectionRef;
import name.abuchen.portfolio.model.proto.v1.PLedgerProjectionRole;
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
            throw new IOException("tried to read " + SIGNATURE.length + " bytes but only got " + read); //$NON-NLS-1$ //$NON-NLS-2$
        if (!Arrays.equals(signature, SIGNATURE))
            throw new IOException(Messages.MsgNotAPortfolioFile);

        PClient newClient = PClient.parseFrom(input);

        Client client = new Client();
        client.setVersion(newClient.getVersion());

        String baseCurrency = newClient.getBaseCurrency();
        if (!baseCurrency.isEmpty())
            client.setBaseCurrency(baseCurrency);

        // load settings first because the recreation of attributes attached to
        // securities, accounts, portfolios, and investment plans needs the meta
        // data
        loadSettings(newClient, client);

        Lookup lookup = new Lookup();

        loadSecurities(newClient, client, lookup);
        loadAccounts(newClient, client, lookup);
        loadPortfolios(newClient, client, lookup);

        boolean hasLedgerTruth = hasLedgerTruth(newClient);
        Set<String> ledgerProjectionUUIDs = Collections.emptySet();
        if (hasLedgerTruth)
        {
            loadLedger(newClient.getLedger(), client, lookup);
            ledgerProjectionUUIDs = ledgerProjectionUUIDs(client.getLedger());
        }

        loadTransactions(newClient, lookup, ledgerProjectionUUIDs);

        client.getProperties().putAll(newClient.getPropertiesMap());
        loadTaxonomies(newClient, client, lookup);
        loadDashboards(newClient, client);
        loadWatchlists(newClient, client, lookup);
        loadInvestmentPlans(newClient, client, lookup);
        loadExtensions(newClient, client);

        client.getSaveFlags().add(SaveFlag.BINARY);

        ClientFactory.upgradeModel(client);

        if (hasLedgerTruth)
        {
            LedgerProjectionService.adaptLegacyScalarMemberships(client);
            LedgerProjectionService.restoreIfValid(client);
        }
        else
        {
            new LegacyTransactionToLedgerMigrator().migrate(client);
        }

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

            security.protobufSetPrices(newSecurity.getPricesList().stream()
                            .map(p -> new SecurityPrice(LocalDate.ofEpochDay(p.getDate()), p.getClose()))
                            .collect(toMutableList()));

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

                        // read legacy source, if it exists
                        if (newEvent.getDataCount() >= 4)
                        {
                            ((DividendEvent) event).setSource(newEvent.getData(3).getString());
                        }

                        break;
                    default:
                        throw new UnsupportedOperationException(
                                        "unsupported type " + newEvent.getType() + "(" + newEvent.getTypeValue() + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                }

                event.setDate(LocalDate.ofEpochDay(newEvent.getDate()));
                event.setDetails(newEvent.getDetails());

                var source = newEvent.getSource();
                if (!source.isEmpty())
                    event.setSource(newEvent.getSource());

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

    private void loadTransactions(PClient newClient, Lookup lookup, Set<String> ledgerProjectionUUIDs)
    {
        for (PTransaction newTransaction : newClient.getTransactionsList())
        {
            if (isLedgerCompatibilityShadow(newTransaction, ledgerProjectionUUIDs))
                continue;

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
                    if (newTransaction.hasSource())
                        buysell.setSource(newTransaction.getSource());

                    buysell.setShares(newTransaction.getShares());

                    if (!newTransaction.hasSecurity())
                        throw new UnsupportedOperationException();
                    buysell.setSecurity(lookup.getSecurity(newTransaction.getSecurity()));

                    loadTransactionUnits(newTransaction, portfolioTx);

                    portfolioTx.setUpdatedAt(fromUpdatedAtTimestamp(newTransaction.getUpdatedAt()));
                    accountTx.setUpdatedAt(fromUpdatedAtTimestamp(newTransaction.getOtherUpdatedAt()));

                    try
                    {
                        buysell.insert();
                    }
                    catch (IllegalArgumentException ignore)
                    {
                        PortfolioLog.error(ignore);
                    }

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
                    if (newTransaction.hasSource())
                        transfer.setSource(newTransaction.getSource());

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
                    if (newTransaction.hasSource())
                        cashTransfer.setSource(newTransaction.getSource());

                    loadTransactionUnits(newTransaction, sourceATx);

                    sourceATx.getUnit(Transaction.Unit.Type.GROSS_VALUE)
                                    .ifPresent(unit -> targetATx.setMonetaryAmount(unit.getForex()));

                    sourceATx.setUpdatedAt(fromUpdatedAtTimestamp(newTransaction.getUpdatedAt()));
                    targetATx.setUpdatedAt(fromUpdatedAtTimestamp(newTransaction.getOtherUpdatedAt()));

                    try
                    {
                        cashTransfer.insert();
                    }
                    catch (IllegalArgumentException ignore)
                    {
                        // background: due to a bug in previous versions, there
                        // might be cash transfer transactions which have been
                        // partially added, e.g. only to the source account.
                        // Because in protobuf we reconstruct the transactions
                        // from the source account, we might attempt to add the
                        // dangling transaction to the target account. This
                        // should not fail the loading of the entire file.

                        PortfolioLog.error(ignore);
                    }

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
                    if (newTransaction.hasExDate())
                        dividend.setExDate(fromLocalDateTime(newTransaction.getExDate()));
                    loadCommonTransaction(newTransaction, dividend, lookup, false);

                    // If the dividend has no instrument, convert it to an
                    // interest payment. In the past, it was possible to create
                    // such a transaction.

                    if (dividend.getSecurity() == null)
                        dividend.setType(AccountTransaction.Type.INTEREST);

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

    private boolean isLedgerCompatibilityShadow(PTransaction newTransaction, Set<String> ledgerProjectionUUIDs)
    {
        return ledgerProjectionUUIDs.contains(newTransaction.getUuid())
                        || (newTransaction.hasOtherUuid() && ledgerProjectionUUIDs.contains(newTransaction.getOtherUuid()));
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

    private boolean hasLedgerTruth(PClient newClient)
    {
        return newClient.hasLedger() && newClient.getLedger().getEntriesCount() > 0;
    }

    private Set<String> ledgerProjectionUUIDs(Ledger ledger)
    {
        return ledger.getEntries().stream() //
                        .flatMap(entry -> entry.getProjectionRefs().stream()) //
                        .map(LedgerProjectionRef::getUUID) //
                        .collect(Collectors.toCollection(HashSet::new));
    }

    private void loadLedger(PLedger newLedger, Client client, Lookup lookup)
    {
        for (PLedgerEntry newEntry : newLedger.getEntriesList())
        {
            LedgerEntry entry = LedgerModelLoadSupport.newEntry(newEntry.getUuid(),
                            LedgerEntryType.fromProtobufId(newEntry.getTypeId()),
                            fromTimestamp(newEntry.getDateTime()));

            if (newEntry.hasNote())
                LedgerModelLoadSupport.setEntryNote(entry, newEntry.getNote());
            if (newEntry.hasSource())
                LedgerModelLoadSupport.setEntrySource(entry, newEntry.getSource());
            if (newEntry.hasUpdatedAt())
                LedgerModelLoadSupport.setEntryUpdatedAt(entry, fromUpdatedAtTimestamp(newEntry.getUpdatedAt()));

            for (PLedgerParameter newParameter : newEntry.getParametersList())
                LedgerModelLoadSupport.addEntryParameter(entry, loadLedgerParameter(newParameter, lookup));

            for (PLedgerPosting newPosting : newEntry.getPostingsList())
                LedgerModelLoadSupport.addPosting(entry, loadLedgerPosting(newPosting, lookup));

            for (PLedgerProjectionRef newProjectionRef : newEntry.getProjectionRefsList())
                LedgerModelLoadSupport.addProjectionRef(entry, loadLedgerProjectionRef(newProjectionRef, lookup));

            if (newEntry.hasUpdatedAt())
                LedgerModelLoadSupport.setEntryUpdatedAt(entry, fromUpdatedAtTimestamp(newEntry.getUpdatedAt()));

            LedgerModelLoadSupport.addEntry(client.getLedger(), entry);
        }
    }

    private LedgerPosting loadLedgerPosting(PLedgerPosting newPosting, Lookup lookup)
    {
        LedgerPosting posting = LedgerModelLoadSupport.newPosting(newPosting.getUuid(),
                        LedgerPostingType.fromCode(newPosting.getTypeCode()));

        LedgerModelLoadSupport.setPostingAmount(posting, newPosting.getAmount());
        if (newPosting.hasCurrency())
            LedgerModelLoadSupport.setPostingCurrency(posting, newPosting.getCurrency());
        if (newPosting.hasForexAmount())
            LedgerModelLoadSupport.setPostingForexAmount(posting, newPosting.getForexAmount());
        if (newPosting.hasForexCurrency())
            LedgerModelLoadSupport.setPostingForexCurrency(posting, newPosting.getForexCurrency());
        if (newPosting.hasExchangeRate())
            LedgerModelLoadSupport.setPostingExchangeRate(posting,
                            fromDecimalValue(newPosting.getExchangeRate()));
        if (newPosting.hasSecurity())
            LedgerModelLoadSupport.setPostingSecurity(posting, lookup.getSecurity(newPosting.getSecurity()));
        LedgerModelLoadSupport.setPostingShares(posting, newPosting.getShares());
        if (newPosting.hasAccount())
            LedgerModelLoadSupport.setPostingAccount(posting, lookup.getAccount(newPosting.getAccount()));
        if (newPosting.hasPortfolio())
            LedgerModelLoadSupport.setPostingPortfolio(posting, lookup.getPortfolio(newPosting.getPortfolio()));

        for (PLedgerParameter newParameter : newPosting.getParametersList())
            LedgerModelLoadSupport.addPostingParameter(posting, loadLedgerParameter(newParameter, lookup));

        return posting;
    }

    private LedgerProjectionRef loadLedgerProjectionRef(PLedgerProjectionRef newProjectionRef, Lookup lookup)
    {
        LedgerProjectionRef projectionRef = LedgerModelLoadSupport.newProjectionRef(newProjectionRef.getUuid(),
                        fromProto(newProjectionRef.getRole()));

        if (newProjectionRef.hasAccount())
            LedgerModelLoadSupport.setProjectionRefAccount(projectionRef,
                            lookup.getAccount(newProjectionRef.getAccount()));
        if (newProjectionRef.hasPortfolio())
            LedgerModelLoadSupport.setProjectionRefPortfolio(projectionRef,
                            lookup.getPortfolio(newProjectionRef.getPortfolio()));

        for (PLedgerProjectionMembership newMembership : newProjectionRef.getMembershipsList())
            LedgerModelLoadSupport.addProjectionRefMembership(projectionRef, newMembership.getPostingUUID(),
                            fromProto(newMembership.getRole()));

        return projectionRef;
    }

    private LedgerParameter<?> loadLedgerParameter(PLedgerParameter newParameter, Lookup lookup)
    {
        LedgerParameterType type = LedgerParameterType.fromCode(newParameter.getTypeCode());

        switch (newParameter.getValueKind())
        {
            case LEDGER_PARAMETER_VALUE_KIND_STRING:
                return LedgerParameter.ofString(type, newParameter.getStringValue());
            case LEDGER_PARAMETER_VALUE_KIND_DECIMAL:
                return LedgerParameter.ofDecimal(type, fromDecimalValue(newParameter.getDecimalValue()));
            case LEDGER_PARAMETER_VALUE_KIND_LONG:
                return LedgerParameter.ofLong(type, newParameter.getLongValue());
            case LEDGER_PARAMETER_VALUE_KIND_MONEY:
                return LedgerParameter.ofMoney(type,
                                Money.of(newParameter.getMoneyCurrency(), newParameter.getMoneyAmount()));
            case LEDGER_PARAMETER_VALUE_KIND_SECURITY:
                return LedgerParameter.ofSecurity(type, lookup.getSecurity(newParameter.getSecurity()));
            case LEDGER_PARAMETER_VALUE_KIND_ACCOUNT:
                return LedgerParameter.ofAccount(type, lookup.getAccount(newParameter.getAccount()));
            case LEDGER_PARAMETER_VALUE_KIND_PORTFOLIO:
                return LedgerParameter.ofPortfolio(type, lookup.getPortfolio(newParameter.getPortfolio()));
            case LEDGER_PARAMETER_VALUE_KIND_BOOLEAN:
                if (!newParameter.hasBooleanValue())
                    throw new UnsupportedOperationException(LedgerDiagnosticCode.LEDGER_PERSIST_009
                                    .message(Messages.LedgerProtobufBooleanParameterMissingBooleanValue));
                return LedgerParameter.ofBoolean(type, Boolean.valueOf(newParameter.getBooleanValue()));
            case LEDGER_PARAMETER_VALUE_KIND_LOCAL_DATE:
                if (!newParameter.hasLocalDateValue())
                    throw new UnsupportedOperationException(LedgerDiagnosticCode.LEDGER_PERSIST_010
                                    .message(Messages.LedgerProtobufLocalDateParameterMissingLocalDateValue));
                return LedgerParameter.ofLocalDate(type, LocalDate.ofEpochDay(newParameter.getLocalDateValue()));
            case LEDGER_PARAMETER_VALUE_KIND_LOCAL_DATE_TIME:
                return LedgerParameter.ofLocalDateTime(type,
                                fromLocalDateTime(newParameter.getLocalDateTimeValue()));
            case LEDGER_PARAMETER_VALUE_KIND_UNSPECIFIED:
            case UNRECOGNIZED:
            default:
                throw new UnsupportedOperationException(newParameter.getValueKind().toString());
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

                classification.setProtobufData(newClassification.getDataList());

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

                    assignment.setProtobufData(newAssignment.getDataList());

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
            dashboard.setId(newDashboard.getId());
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

            client.addWatchlist(watchlist);
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
            plan.setTaxes(newPlan.getTaxes());

            switch (newPlan.getType())
            {
                case PURCHASE_OR_DELIVERY:
                    plan.setType(InvestmentPlan.Type.PURCHASE_OR_DELIVERY);
                    break;
                case DEPOSIT:
                    plan.setType(InvestmentPlan.Type.DEPOSIT);
                    break;
                case INTEREST:
                    plan.setType(InvestmentPlan.Type.INTEREST);
                    break;
                case REMOVAL:
                    plan.setType(InvestmentPlan.Type.REMOVAL);
                    break;
                default:
                    throw new UnsupportedOperationException();
            }

            for (String uuid : newPlan.getTransactionsList())
            {
                Transaction t = uuid2transaction.get(uuid);
                if (t != null)
                {
                    plan.getTransactions().add(t);
                    continue;
                }

                InvestmentPlan.LedgerExecutionRef executionRef = ledgerExecutionRefForProjectionUUID(client, uuid);
                if (executionRef != null)
                {
                    plan.addLedgerExecutionRef(executionRef);
                    continue;
                }

                throw new UnsupportedOperationException(uuid);
            }

            for (PInvestmentPlanLedgerExecutionRef newRef : newPlan.getLedgerExecutionRefsList())
            {
                plan.addLedgerExecutionRef(new InvestmentPlan.LedgerExecutionRef(newRef.getLedgerEntryUUID(),
                                newRef.hasProjectionUUID() ? newRef.getProjectionUUID() : null,
                                newRef.hasProjectionRole() ? fromProto(newRef.getProjectionRole()) : null));
            }

            client.addPlan(plan);
        }
    }

    private InvestmentPlan.LedgerExecutionRef ledgerExecutionRefForProjectionUUID(Client client, String projectionUUID)
    {
        for (LedgerEntry entry : client.getLedger().getEntries())
        {
            for (LedgerProjectionRef projectionRef : entry.getProjectionRefs())
            {
                if (projectionRef.getUUID().equals(projectionUUID))
                    return new InvestmentPlan.LedgerExecutionRef(entry.getUUID(), projectionRef.getUUID(),
                                    projectionRef.getRole());
            }
        }

        return null;
    }

    private void loadExtensions(PClient newClient, Client client)
    {
        // Load extension data from the Any fields
        // This preserves third-party extension data during load
        List<Any> extensions = new ArrayList<>();
        for (Any extension : newClient.getExtensionsList())
        {
            // Store extension data for preservation during save
            String typeUrl = extension.getTypeUrl();
            PortfolioLog.info("Loaded extension: " + typeUrl); //$NON-NLS-1$

            // Add extension to the list for storage in Client object
            extensions.add(extension);
        }

        // Store all extensions in the Client object for preservation during
        // save
        client.setExtensions(extensions);
    }

    @Override
    public void save(Client client, OutputStream output) throws IOException
    {
        PClient.Builder newClient = PClient.newBuilder();

        newClient.setVersion(client.getVersion());
        newClient.setBaseCurrency(client.getBaseCurrency());

        saveSecurities(client, newClient);
        saveAccounts(client, newClient);
        savePortfolios(client, newClient);
        saveLedger(client, newClient);
        saveTransactions(client, newClient);

        newClient.putAllProperties(client.getProperties());
        saveTaxonomies(client, newClient);
        saveSettings(client, newClient);
        saveDashboards(client, newClient);
        saveWatchlists(client, newClient);
        saveInvestmentPlans(client, newClient);
        saveExtensions(client, newClient);

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
                if (event == null)
                    return;

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
                        // Important: the 'source' attribute used to be written
                        // as 4th element
                        break;
                    default:
                        throw new UnsupportedOperationException();
                }

                newEvent.setDate(event.getDate().toEpochDay());
                if (event.getDetails() != null)
                    newEvent.setDetails(event.getDetails());
                if (event.getSource() != null)
                    newEvent.setSource(event.getSource());
                newSecurity.addEvents(newEvent);

            });

            Map<String, PMap.Builder> data = new HashMap<>();
            security.getProperties().forEach(p -> {
                if (p == null)
                    return;
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

    private void saveLedger(Client client, PClient.Builder newClient)
    {
        validateLedger(client);

        PLedger.Builder newLedger = PLedger.newBuilder();

        for (LedgerEntry entry : client.getLedger().getEntries())
            newLedger.addEntries(saveLedgerEntry(entry));

        newClient.setLedger(newLedger);
    }

    private PLedgerEntry saveLedgerEntry(LedgerEntry entry)
    {
        PLedgerEntry.Builder newEntry = PLedgerEntry.newBuilder();

        newEntry.setUuid(entry.getUUID());
        newEntry.setTypeId(entry.getType().getProtobufId());
        newEntry.setDateTime(asTimestamp(entry.getDateTime()));

        if (entry.getNote() != null)
            newEntry.setNote(entry.getNote());
        if (entry.getSource() != null)
            newEntry.setSource(entry.getSource());
        if (entry.getUpdatedAt() != null)
            newEntry.setUpdatedAt(asUpdatedAtTimestamp(entry.getUpdatedAt()));

        for (LedgerParameter<?> parameter : entry.getParameters())
            newEntry.addParameters(saveLedgerParameter(parameter));

        for (LedgerPosting posting : entry.getPostings())
            newEntry.addPostings(saveLedgerPosting(posting));

        for (LedgerProjectionRef projectionRef : entry.getProjectionRefs())
            newEntry.addProjectionRefs(saveLedgerProjectionRef(projectionRef));

        return newEntry.build();
    }

    private PLedgerPosting saveLedgerPosting(LedgerPosting posting)
    {
        PLedgerPosting.Builder newPosting = PLedgerPosting.newBuilder();

        newPosting.setUuid(posting.getUUID());
        newPosting.setTypeCode(posting.getType().getCode());
        newPosting.setAmount(posting.getAmount());

        if (posting.getCurrency() != null)
            newPosting.setCurrency(posting.getCurrency());
        if (posting.getForexAmount() != null)
            newPosting.setForexAmount(posting.getForexAmount());
        if (posting.getForexCurrency() != null)
            newPosting.setForexCurrency(posting.getForexCurrency());
        if (posting.getExchangeRate() != null)
            newPosting.setExchangeRate(asDecimalValue(posting.getExchangeRate()));
        if (posting.getSecurity() != null)
            newPosting.setSecurity(posting.getSecurity().getUUID());
        newPosting.setShares(posting.getShares());
        if (posting.getAccount() != null)
            newPosting.setAccount(posting.getAccount().getUUID());
        if (posting.getPortfolio() != null)
            newPosting.setPortfolio(posting.getPortfolio().getUUID());

        for (LedgerParameter<?> parameter : posting.getParameters())
            newPosting.addParameters(saveLedgerParameter(parameter));

        return newPosting.build();
    }

    private PLedgerProjectionRef saveLedgerProjectionRef(LedgerProjectionRef projectionRef)
    {
        PLedgerProjectionRef.Builder newProjectionRef = PLedgerProjectionRef.newBuilder();

        newProjectionRef.setUuid(projectionRef.getUUID());
        newProjectionRef.setRole(toProto(projectionRef.getRole()));
        if (projectionRef.getAccount() != null)
            newProjectionRef.setAccount(projectionRef.getAccount().getUUID());
        if (projectionRef.getPortfolio() != null)
            newProjectionRef.setPortfolio(projectionRef.getPortfolio().getUUID());
        for (ProjectionMembership membership : projectionRef.getMemberships())
            newProjectionRef.addMemberships(saveLedgerProjectionMembership(membership));

        return newProjectionRef.build();
    }

    private PLedgerProjectionMembership saveLedgerProjectionMembership(ProjectionMembership membership)
    {
        PLedgerProjectionMembership.Builder newMembership = PLedgerProjectionMembership.newBuilder();

        newMembership.setPostingUUID(membership.getPostingUUID());
        newMembership.setRole(toProto(membership.getRole()));

        return newMembership.build();
    }

    private PLedgerParameter saveLedgerParameter(LedgerParameter<?> parameter)
    {
        PLedgerParameter.Builder newParameter = PLedgerParameter.newBuilder();

        newParameter.setTypeCode(parameter.getType().getCode());
        newParameter.setValueKind(toProto(parameter.getValueKind()));

        switch (parameter.getValueKind())
        {
            case STRING:
                newParameter.setStringValue((String) parameter.getValue());
                break;
            case DECIMAL:
                newParameter.setDecimalValue(asDecimalValue((BigDecimal) parameter.getValue()));
                break;
            case LONG:
                newParameter.setLongValue((Long) parameter.getValue());
                break;
            case MONEY:
                Money money = (Money) parameter.getValue();
                newParameter.setMoneyAmount(money.getAmount());
                newParameter.setMoneyCurrency(money.getCurrencyCode());
                break;
            case SECURITY:
                newParameter.setSecurity(((Security) parameter.getValue()).getUUID());
                break;
            case ACCOUNT:
                newParameter.setAccount(((Account) parameter.getValue()).getUUID());
                break;
            case PORTFOLIO:
                newParameter.setPortfolio(((Portfolio) parameter.getValue()).getUUID());
                break;
            case BOOLEAN:
                newParameter.setBooleanValue(((Boolean) parameter.getValue()).booleanValue());
                break;
            case LOCAL_DATE:
                newParameter.setLocalDateValue(((LocalDate) parameter.getValue()).toEpochDay());
                break;
            case LOCAL_DATE_TIME:
                newParameter.setLocalDateTimeValue(asLocalDateTime((java.time.LocalDateTime) parameter.getValue()));
                break;
            default:
                throw new UnsupportedOperationException(parameter.getValueKind().toString());
        }

        return newParameter.build();
    }

    private void saveTransactions(Client client, PClient.Builder newClient)
    {
        Set<String> ledgerProjectionUUIDs = ledgerProjectionUUIDs(client.getLedger());
        saveLedgerCompatibilityShadows(client, newClient);

        for (Portfolio portfolio : client.getPortfolios())
        {
            portfolio.getTransactions().stream()
                            .filter(t -> t.getType() != PortfolioTransaction.Type.TRANSFER_IN)
                            .filter(t -> shouldSaveLegacyTransaction(t, ledgerProjectionUUIDs))
                            .forEach(t -> addTransaction(newClient, portfolio, t));
        }

        EnumSet<AccountTransaction.Type> exclude = EnumSet.of(AccountTransaction.Type.TRANSFER_IN,
                        AccountTransaction.Type.BUY, AccountTransaction.Type.SELL);

        for (Account account : client.getAccounts())
        {
            account.getTransactions().stream() //
                            .filter(t -> !exclude.contains(t.getType()))
                            .filter(t -> shouldSaveLegacyTransaction(t, ledgerProjectionUUIDs))
                            .forEach(t -> addTransaction(newClient, account, t));
        }

    }

    private void saveLedgerCompatibilityShadows(Client client, PClient.Builder newClient)
    {
        for (LedgerEntry entry : client.getLedger().getEntries())
        {
            if (!entry.getType().isLegacyFixedShape())
                continue;

            for (Transaction transaction : LedgerProjectionService.createProjections(entry))
            {
                if (!shouldSaveLedgerCompatibilityShadow(transaction))
                    continue;

                LedgerBackedTransaction ledgerBackedTransaction = (LedgerBackedTransaction) transaction;
                LedgerProjectionRef projectionRef = ledgerBackedTransaction.getLedgerProjectionRef();

                if (transaction instanceof AccountTransaction accountTransaction)
                    addTransaction(newClient, projectionRef.getAccount(), accountTransaction);
                else if (transaction instanceof PortfolioTransaction portfolioTransaction)
                    addTransaction(newClient, projectionRef.getPortfolio(), portfolioTransaction);
                else
                    throw new UnsupportedOperationException(transaction.getClass().getName());
            }
        }
    }

    private boolean shouldSaveLedgerCompatibilityShadow(Transaction transaction)
    {
        if (transaction instanceof AccountTransaction accountTransaction)
            return accountTransaction.getType() != AccountTransaction.Type.BUY
                            && accountTransaction.getType() != AccountTransaction.Type.SELL
                            && accountTransaction.getType() != AccountTransaction.Type.TRANSFER_IN;

        if (transaction instanceof PortfolioTransaction portfolioTransaction)
            return portfolioTransaction.getType() != PortfolioTransaction.Type.TRANSFER_IN;

        return false;
    }

    private boolean shouldSaveLegacyTransaction(Transaction transaction, Set<String> ledgerProjectionUUIDs)
    {
        return !(transaction instanceof LedgerBackedTransaction) && !ledgerProjectionUUIDs.contains(transaction.getUUID());
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
                if (t.getExDate() != null)
                    newTransaction.setExDate(asLocalDateTime(t.getExDate()));
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

                newClassification.addAllData(classification.getProtobufData());

                for (Assignment assignment : classification.getAssignments())
                {
                    PTaxonomy.Assignment.Builder newAssignment = PTaxonomy.Assignment.newBuilder();
                    newAssignment.setInvestmentVehicle(assignment.getInvestmentVehicle().getUUID());
                    newAssignment.setWeight(assignment.getWeight());
                    newAssignment.setRank(assignment.getRank());

                    newAssignment.addAllData(assignment.getProtobufData());

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
                    if (config.getUUID() != null)
                        newConfigurationSet.setUuid(config.getUUID());
                    if (config.getName() != null)
                        newConfigurationSet.setName(config.getName());
                    if (config.getData() != null)
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

            newDashboard.setId(dashboard.getId());
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
            newPlan.setTaxes(plan.getTaxes());

            switch (plan.getPlanType())
            {
                case PURCHASE_OR_DELIVERY:
                    newPlan.setType(Type.PURCHASE_OR_DELIVERY);
                    break;
                case DEPOSIT:
                    newPlan.setType(Type.DEPOSIT);
                    break;
                case INTEREST:
                    newPlan.setType(Type.INTEREST);
                    break;
                case REMOVAL:
                    newPlan.setType(Type.REMOVAL);
                    break;
                default:
                    throw new UnsupportedOperationException();
            }

            Set<String> ledgerExecutionRefKeys = new HashSet<>();

            plan.getTransactions().forEach(t -> {
                if (t instanceof LedgerBackedTransaction ledgerBackedTransaction)
                    addLedgerExecutionRef(newPlan, InvestmentPlan.LedgerExecutionRef.of(ledgerBackedTransaction),
                                    ledgerExecutionRefKeys);
                else
                    newPlan.addTransactions(t.getUUID());
            });

            plan.getLedgerExecutionRefs()
                            .forEach(ref -> addLedgerExecutionRef(newPlan, ref, ledgerExecutionRefKeys));

            newClient.addPlans(newPlan);
        });
    }

    private void addLedgerExecutionRef(PInvestmentPlan.Builder newPlan, InvestmentPlan.LedgerExecutionRef ref,
                    Set<String> keys)
    {
        String key = ref.getLedgerEntryUUID() + "|" + ref.getProjectionUUID() + "|" + ref.getProjectionRole(); //$NON-NLS-1$ //$NON-NLS-2$

        if (!keys.add(key))
            return;

        PInvestmentPlanLedgerExecutionRef.Builder newRef = PInvestmentPlanLedgerExecutionRef.newBuilder();
        newRef.setLedgerEntryUUID(ref.getLedgerEntryUUID());

        if (ref.getProjectionUUID() != null)
            newRef.setProjectionUUID(ref.getProjectionUUID());
        if (ref.getProjectionRole() != null)
            newRef.setProjectionRole(toProto(ref.getProjectionRole()));

        newPlan.addLedgerExecutionRefs(newRef);
    }

    private void validateLedger(Client client)
    {
        var ledger = client.getLedger();
        var result = LedgerStructuralValidator.validate(ledger);

        if (!result.isOK())
        {
            LedgerProjectionService.logSkipped(ledger, result);
            throw new UnsupportedOperationException(
                            LedgerDiagnosticCode.LEDGER_PERSIST_002
                                            .message(MessageFormat.format(
                                                            Messages.LedgerProtobufInvalidLedgerPersistenceState,
                                                            LedgerDiagnosticMessageFormatter.formatValidationResult(
                                                                            ledger, result))));
        }
    }

    private PLedgerProjectionRole toProto(LedgerProjectionRole role)
    {
        switch (role)
        {
            case ACCOUNT:
                return PLedgerProjectionRole.LEDGER_PROJECTION_ROLE_ACCOUNT;
            case PORTFOLIO:
                return PLedgerProjectionRole.LEDGER_PROJECTION_ROLE_PORTFOLIO;
            case SOURCE_ACCOUNT:
                return PLedgerProjectionRole.LEDGER_PROJECTION_ROLE_SOURCE_ACCOUNT;
            case TARGET_ACCOUNT:
                return PLedgerProjectionRole.LEDGER_PROJECTION_ROLE_TARGET_ACCOUNT;
            case SOURCE_PORTFOLIO:
                return PLedgerProjectionRole.LEDGER_PROJECTION_ROLE_SOURCE_PORTFOLIO;
            case TARGET_PORTFOLIO:
                return PLedgerProjectionRole.LEDGER_PROJECTION_ROLE_TARGET_PORTFOLIO;
            case DELIVERY:
                return PLedgerProjectionRole.LEDGER_PROJECTION_ROLE_DELIVERY;
            case DELIVERY_INBOUND:
                return PLedgerProjectionRole.LEDGER_PROJECTION_ROLE_DELIVERY_INBOUND;
            case DELIVERY_OUTBOUND:
                return PLedgerProjectionRole.LEDGER_PROJECTION_ROLE_DELIVERY_OUTBOUND;
            case CASH_COMPENSATION:
                return PLedgerProjectionRole.LEDGER_PROJECTION_ROLE_CASH_COMPENSATION;
            case OLD_SECURITY_LEG:
                return PLedgerProjectionRole.LEDGER_PROJECTION_ROLE_OLD_SECURITY_LEG;
            case NEW_SECURITY_LEG:
                return PLedgerProjectionRole.LEDGER_PROJECTION_ROLE_NEW_SECURITY_LEG;
            default:
                throw new UnsupportedOperationException(role.toString());
        }
    }

    private LedgerProjectionRole fromProto(PLedgerProjectionRole role)
    {
        switch (role)
        {
            case LEDGER_PROJECTION_ROLE_ACCOUNT:
                return LedgerProjectionRole.ACCOUNT;
            case LEDGER_PROJECTION_ROLE_PORTFOLIO:
                return LedgerProjectionRole.PORTFOLIO;
            case LEDGER_PROJECTION_ROLE_SOURCE_ACCOUNT:
                return LedgerProjectionRole.SOURCE_ACCOUNT;
            case LEDGER_PROJECTION_ROLE_TARGET_ACCOUNT:
                return LedgerProjectionRole.TARGET_ACCOUNT;
            case LEDGER_PROJECTION_ROLE_SOURCE_PORTFOLIO:
                return LedgerProjectionRole.SOURCE_PORTFOLIO;
            case LEDGER_PROJECTION_ROLE_TARGET_PORTFOLIO:
                return LedgerProjectionRole.TARGET_PORTFOLIO;
            case LEDGER_PROJECTION_ROLE_DELIVERY:
                return LedgerProjectionRole.DELIVERY;
            case LEDGER_PROJECTION_ROLE_DELIVERY_INBOUND:
                return LedgerProjectionRole.DELIVERY_INBOUND;
            case LEDGER_PROJECTION_ROLE_DELIVERY_OUTBOUND:
                return LedgerProjectionRole.DELIVERY_OUTBOUND;
            case LEDGER_PROJECTION_ROLE_CASH_COMPENSATION:
                return LedgerProjectionRole.CASH_COMPENSATION;
            case LEDGER_PROJECTION_ROLE_OLD_SECURITY_LEG:
                return LedgerProjectionRole.OLD_SECURITY_LEG;
            case LEDGER_PROJECTION_ROLE_NEW_SECURITY_LEG:
                return LedgerProjectionRole.NEW_SECURITY_LEG;
            case LEDGER_PROJECTION_ROLE_UNSPECIFIED:
            case UNRECOGNIZED:
            default:
                throw new UnsupportedOperationException(role.toString());
        }
    }

    private PLedgerProjectionMembershipRole toProto(ProjectionMembershipRole role)
    {
        switch (role)
        {
            case PRIMARY:
                return PLedgerProjectionMembershipRole.LEDGER_PROJECTION_MEMBERSHIP_ROLE_PRIMARY;
            case GROUP_ANCHOR:
                return PLedgerProjectionMembershipRole.LEDGER_PROJECTION_MEMBERSHIP_ROLE_GROUP_ANCHOR;
            case FEE_UNIT:
                return PLedgerProjectionMembershipRole.LEDGER_PROJECTION_MEMBERSHIP_ROLE_FEE_UNIT;
            case TAX_UNIT:
                return PLedgerProjectionMembershipRole.LEDGER_PROJECTION_MEMBERSHIP_ROLE_TAX_UNIT;
            case GROSS_VALUE_UNIT:
                return PLedgerProjectionMembershipRole.LEDGER_PROJECTION_MEMBERSHIP_ROLE_GROSS_VALUE_UNIT;
            case FOREX_CONTEXT:
                return PLedgerProjectionMembershipRole.LEDGER_PROJECTION_MEMBERSHIP_ROLE_FOREX_CONTEXT;
            default:
                throw new UnsupportedOperationException(role.toString());
        }
    }

    private ProjectionMembershipRole fromProto(PLedgerProjectionMembershipRole role)
    {
        switch (role)
        {
            case LEDGER_PROJECTION_MEMBERSHIP_ROLE_PRIMARY:
                return ProjectionMembershipRole.PRIMARY;
            case LEDGER_PROJECTION_MEMBERSHIP_ROLE_GROUP_ANCHOR:
                return ProjectionMembershipRole.GROUP_ANCHOR;
            case LEDGER_PROJECTION_MEMBERSHIP_ROLE_FEE_UNIT:
                return ProjectionMembershipRole.FEE_UNIT;
            case LEDGER_PROJECTION_MEMBERSHIP_ROLE_TAX_UNIT:
                return ProjectionMembershipRole.TAX_UNIT;
            case LEDGER_PROJECTION_MEMBERSHIP_ROLE_GROSS_VALUE_UNIT:
                return ProjectionMembershipRole.GROSS_VALUE_UNIT;
            case LEDGER_PROJECTION_MEMBERSHIP_ROLE_FOREX_CONTEXT:
                return ProjectionMembershipRole.FOREX_CONTEXT;
            case LEDGER_PROJECTION_MEMBERSHIP_ROLE_UNSPECIFIED:
            case UNRECOGNIZED:
            default:
                throw new UnsupportedOperationException(role.toString());
        }
    }

    private PLedgerParameterValueKind toProto(ValueKind valueKind)
    {
        switch (valueKind)
        {
            case STRING:
                return PLedgerParameterValueKind.LEDGER_PARAMETER_VALUE_KIND_STRING;
            case DECIMAL:
                return PLedgerParameterValueKind.LEDGER_PARAMETER_VALUE_KIND_DECIMAL;
            case LONG:
                return PLedgerParameterValueKind.LEDGER_PARAMETER_VALUE_KIND_LONG;
            case MONEY:
                return PLedgerParameterValueKind.LEDGER_PARAMETER_VALUE_KIND_MONEY;
            case SECURITY:
                return PLedgerParameterValueKind.LEDGER_PARAMETER_VALUE_KIND_SECURITY;
            case ACCOUNT:
                return PLedgerParameterValueKind.LEDGER_PARAMETER_VALUE_KIND_ACCOUNT;
            case PORTFOLIO:
                return PLedgerParameterValueKind.LEDGER_PARAMETER_VALUE_KIND_PORTFOLIO;
            case BOOLEAN:
                return PLedgerParameterValueKind.LEDGER_PARAMETER_VALUE_KIND_BOOLEAN;
            case LOCAL_DATE:
                return PLedgerParameterValueKind.LEDGER_PARAMETER_VALUE_KIND_LOCAL_DATE;
            case LOCAL_DATE_TIME:
                return PLedgerParameterValueKind.LEDGER_PARAMETER_VALUE_KIND_LOCAL_DATE_TIME;
            default:
                throw new UnsupportedOperationException(valueKind.toString());
        }
    }

    private void saveExtensions(Client client, PClient.Builder newClient)
    {
        // Save extension data to the Any fields
        // This preserves third-party extension data during save
        for (Any extension : client.getExtensions())
        {
            // Add each extension to the protobuf builder
            newClient.addExtensions(extension);

            String typeUrl = extension.getTypeUrl();
            PortfolioLog.info("Saved extension: " + typeUrl); //$NON-NLS-1$
        }
    }
}
