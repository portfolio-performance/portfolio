package name.abuchen.portfolio.cli.transaction;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import name.abuchen.portfolio.cli.command.Decimals;
import name.abuchen.portfolio.cli.command.MasterData;
import name.abuchen.portfolio.cli.command.Schema;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.AccountTransferEntry;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.PortfolioTransferEntry;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.money.Values;

/**
 * The transaction records of a client: the join of their sides, and each
 * record in the shape <code>transactions</code> shows and transaction writes
 * accept. Every call joins the sides afresh; nothing is cached between calls,
 * so nothing goes stale while a batch changes the model.
 */
public final class TransactionRecords
{
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    private final Client client;
    private final Predicate<Object> existedBeforeBatch;

    private TransactionRecords(Client client, Predicate<Object> existedBeforeBatch)
    {
        this.client = client;
        this.existedBeforeBatch = existedBeforeBatch;
    }

    /**
     * @param existedBeforeBatch
     *            whether a transaction was in the file before the batch
     *            started; only such records can be found. The read side passes
     *            <code>any -&gt; true</code>.
     */
    public static TransactionRecords open(Client client, Predicate<Object> existedBeforeBatch)
    {
        return new TransactionRecords(client, existedBeforeBatch);
    }

    /** A record as output. */
    public static Schema schema(String description)
    {
        return TransactionShape.recordSchema(description);
    }

    /** All records of the client, in no particular order. */
    public List<TransactionRecord> all()
    {
        var join = new Join(client);
        var records = new ArrayList<TransactionRecord>();
        join.accountOwners.keySet().forEach(t -> join.toRecord(t).ifPresent(records::add));
        join.portfolioOwners.keySet().forEach(t -> join.toRecord(t).ifPresent(records::add));
        return records;
    }

    /** The record that has a side with the UUID, if it was in the file before the batch. */
    public Optional<TransactionRecord> find(String sideUuid)
    {
        return all().stream().filter(record -> record.sides().contains(sideUuid)
                        && existedBeforeBatch.test(record.primary())).findFirst();
    }

    /**
     * The sides of the record, in its order, with the account or investment
     * account each is in. Transitional: the update and delete paths need the
     * owners until they move into this module.
     */
    public List<BuiltRecord.Side> sidesOf(TransactionRecord record)
    {
        var join = new Join(client);
        var sides = new ArrayList<BuiltRecord.Side>();
        join.accountOwners.forEach((t, owner) -> sides.add(new BuiltRecord.Side(t, owner)));
        join.portfolioOwners.forEach((t, owner) -> sides.add(new BuiltRecord.Side(t, owner)));
        return record.sides().stream().map(uuid -> sides.stream()
                        .filter(side -> uuid.equals(side.transaction().getUUID())).findFirst().orElseThrow())
                        .toList();
    }

    /** The record's fields as written, without the {@code --currency} conversion; also the shape writes return. */
    public Map<String, Object> render(TransactionRecord record)
    {
        var names = new AccountNames(client);
        var primary = record.primary();
        var dateTime = primary.getDateTime();
        var currencyCode = MasterData.orEmpty(primary.getCurrencyCode());

        var json = new LinkedHashMap<String, Object>();
        json.put(TransactionShape.ID.name(), record.id());
        json.put(TransactionShape.TYPE.name(), record.type());
        record.kind().ifPresent(kind -> json.put(TransactionShape.KIND.name(), kind.id()));
        json.put(TransactionShape.DATE.name(), dateTime.toLocalDate().toString());
        if (!dateTime.toLocalTime().equals(LocalTime.MIDNIGHT))
            json.put(TransactionShape.TIME.name(), dateTime.format(TIME));
        json.put(TransactionShape.SIDES.name(), record.sides());
        json.put(TransactionShape.AMOUNT.name(), Decimals.of(primary.getAmount(), Values.Amount));
        json.put(TransactionShape.CURRENCY_CODE.name(), currencyCode);

        var crossCurrency = record.crossCurrencyIncoming();
        var transferRate = crossCurrency.flatMap(incoming -> primary.getUnit(Transaction.Unit.Type.GROSS_VALUE)
                        .filter(unit -> unit.getForex().getCurrencyCode().equals(incoming.getCurrencyCode())));
        crossCurrency.ifPresent(incoming -> {
            json.put(TransactionShape.TARGET_AMOUNT.name(), Decimals.of(incoming.getAmount(), Values.Amount));
            json.put(TransactionShape.TARGET_CURRENCY_CODE.name(), MasterData.orEmpty(incoming.getCurrencyCode()));
        });
        transferRate.ifPresent(
                        unit -> json.put(TransactionShape.EXCHANGE_RATE.name(), Decimals.of(unit.getExchangeRate())));

        if (primary.getSecurity() != null)
        {
            TransactionShape.INSTRUMENT.put(json, primary.getSecurity().getUUID(),
                            MasterData.orEmpty(primary.getSecurity().getName()));
        }
        if (record.hasShares())
            json.put(TransactionShape.SHARES.name(), Decimals.of(primary.getShares(), Values.Share));
        putAccount(json, TransactionShape.INVESTMENT_ACCOUNT, record.investmentAccount(), names);
        putAccount(json, TransactionShape.CASH_ACCOUNT, record.cashAccount(), names);
        putAccount(json, TransactionShape.FROM, record.from(), names);
        putAccount(json, TransactionShape.TO, record.to(), names);
        if (primary instanceof AccountTransaction account && account.getExDate() != null
                        && account.getType() == AccountTransaction.Type.DIVIDENDS)
            json.put(TransactionShape.EX_DATE.name(), account.getExDate().toLocalDate().toString());
        MasterData.putIfSet(json, TransactionShape.NOTE.name(), primary.getNote());
        MasterData.putIfSet(json, TransactionShape.SOURCE.name(), primary.getSource());
        record.updatedAt().ifPresent(instant -> MasterData.putUpdatedAt(json, instant));

        json.put(TransactionShape.UNITS.name(),
                        primary.getUnits().filter(unit -> transferRate.isEmpty() || unit != transferRate.get())
                                        .map(TransactionRecords::render).toList());

        return json;
    }

    /** The UUID and the name next to it, if there is an account. */
    private static void putAccount(Map<String, Object> json, TransactionShape.NamedUuid member,
                    Optional<String> uuid, AccountNames names)
    {
        uuid.ifPresent(value -> member.put(json, value, names.of(value)));
    }

    private static Map<String, Object> render(Transaction.Unit unit)
    {
        var json = new LinkedHashMap<String, Object>();
        json.put(TransactionShape.UNIT_TYPE.name(), unit.getType().name());
        json.put(TransactionShape.UNIT_AMOUNT.name(), Decimals.amount(unit.getAmount()));
        json.put(TransactionShape.UNIT_CURRENCY_CODE.name(), unit.getAmount().getCurrencyCode());
        if (unit.getForex() != null)
        {
            json.put(TransactionShape.UNIT_FOREX_AMOUNT.name(), Decimals.amount(unit.getForex()));
            json.put(TransactionShape.UNIT_FOREX_CURRENCY_CODE.name(), unit.getForex().getCurrencyCode());
            json.put(TransactionShape.UNIT_EXCHANGE_RATE.name(), Decimals.of(unit.getExchangeRate()));
        }
        return json;
    }

    /** The names of the accounts and investment accounts of a client by UUID. */
    private static final class AccountNames
    {
        private final Map<String, String> names = new HashMap<>();

        AccountNames(Client client)
        {
            client.getAccounts().forEach(account -> names.put(account.getUUID(), account.getName()));
            client.getPortfolios().forEach(portfolio -> names.put(portfolio.getUUID(), portfolio.getName()));
        }

        String of(String uuid)
        {
            return MasterData.orEmpty(names.get(uuid));
        }
    }

    /** The sides of the client with their owners, joined into records. */
    private static final class Join
    {
        private final IdentityHashMap<Transaction, Account> accountOwners = new IdentityHashMap<>();
        private final IdentityHashMap<Transaction, Portfolio> portfolioOwners = new IdentityHashMap<>();

        Join(Client client)
        {
            client.getAccounts().forEach(account -> account.getTransactions().forEach(t -> accountOwners.put(t, account)));
            client.getPortfolios().forEach(
                            portfolio -> portfolio.getTransactions().forEach(t -> portfolioOwners.put(t, portfolio)));
        }

        /** The record this side starts, or empty if its partner starts it. */
        Optional<TransactionRecord> toRecord(Transaction side)
        {
            var partner = partnerOf(side);
            if (partner == null)
                return Optional.of(orphan(side));

            return switch (side.getCrossEntry())
            {
                case BuySellEntry entry -> side == entry.getPortfolioTransaction()
                                ? Optional.of(buySell(entry.getPortfolioTransaction(), entry.getAccountTransaction()))
                                : Optional.empty();
                case AccountTransferEntry entry -> side == entry.getSourceTransaction()
                                ? Optional.of(cashTransfer(entry.getSourceTransaction(), entry.getTargetTransaction()))
                                : Optional.empty();
                case PortfolioTransferEntry entry -> side == entry.getSourceTransaction()
                                ? Optional.of(securityTransfer(entry.getSourceTransaction(),
                                                entry.getTargetTransaction()))
                                : Optional.empty();
                default -> Optional.of(orphan(side));
            };
        }

        /** The other side, or null if there is none or it is in no account or portfolio. */
        private Transaction partnerOf(Transaction side)
        {
            var entry = side.getCrossEntry();
            if (entry == null)
                return null;

            Transaction partner;
            try
            {
                partner = entry.getCrossTransaction(side);
            }
            catch (UnsupportedOperationException e)
            {
                // the entry doesn't know this side: damaged file
                return null;
            }

            return partner != null && (accountOwners.containsKey(partner) || portfolioOwners.containsKey(partner))
                            ? partner
                            : null;
        }

        private TransactionRecord buySell(PortfolioTransaction portfolioSide, AccountTransaction accountSide)
        {
            return new TransactionRecord(portfolioSide.getUUID(), portfolioSide.getType().name(), Optional.empty(),
                            portfolioSide, List.of(portfolioSide.getUUID(), accountSide.getUUID()),
                            uuid(portfolioOwners.get(portfolioSide)), uuid(accountOwners.get(accountSide)),
                            Optional.empty(), Optional.empty(), Optional.empty());
        }

        private TransactionRecord cashTransfer(AccountTransaction outgoing, AccountTransaction incoming)
        {
            return new TransactionRecord(outgoing.getUUID(), TransactionRecord.TRANSFER,
                            Optional.of(TransactionRecord.TransferKind.CASH), outgoing,
                            List.of(outgoing.getUUID(), incoming.getUUID()), Optional.empty(), Optional.empty(),
                            uuid(accountOwners.get(outgoing)), uuid(accountOwners.get(incoming)),
                            Optional.of(incoming));
        }

        private TransactionRecord securityTransfer(PortfolioTransaction outgoing, PortfolioTransaction incoming)
        {
            return new TransactionRecord(outgoing.getUUID(), TransactionRecord.TRANSFER,
                            Optional.of(TransactionRecord.TransferKind.SECURITY), outgoing,
                            List.of(outgoing.getUUID(), incoming.getUUID()), Optional.empty(), Optional.empty(),
                            uuid(portfolioOwners.get(outgoing)), uuid(portfolioOwners.get(incoming)),
                            Optional.of(incoming));
        }

        private TransactionRecord orphan(Transaction side)
        {
            var type = side instanceof AccountTransaction account ? account.getType().name()
                            : ((PortfolioTransaction) side).getType().name();

            return new TransactionRecord(side.getUUID(), type, Optional.empty(), side, List.of(side.getUUID()),
                            side instanceof PortfolioTransaction ? uuid(portfolioOwners.get(side)) : Optional.empty(),
                            side instanceof AccountTransaction ? uuid(accountOwners.get(side)) : Optional.empty(),
                            Optional.empty(), Optional.empty(), Optional.empty());
        }

        private static Optional<String> uuid(Portfolio portfolio)
        {
            return Optional.ofNullable(portfolio).map(Portfolio::getUUID);
        }

        private static Optional<String> uuid(Account account)
        {
            return Optional.ofNullable(account).map(Account::getUUID);
        }
    }
}
