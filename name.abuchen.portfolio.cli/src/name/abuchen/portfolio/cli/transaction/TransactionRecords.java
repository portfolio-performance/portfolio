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

import com.google.gson.JsonObject;

import name.abuchen.portfolio.cli.command.Claims;
import name.abuchen.portfolio.cli.command.Decimals;
import name.abuchen.portfolio.cli.command.MasterData;
import name.abuchen.portfolio.cli.command.OperationSpec;
import name.abuchen.portfolio.cli.command.Outcome;
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

    /**
     * <code>transaction.add</code>: a thin adapter over {@link #add}. The batch
     * reports unknown members itself, so the add refuses them without
     * reporting them again.
     */
    public static final OperationSpec<OperationSpec.Context> ADD = new OperationSpec<>(TransactionAdd.OPERATION,
                    "Adds a transaction in the shape the transactions output uses. Cash-account events (deposit, removal, interest, dividend, fee, tax, refund) are one-sided. BUY and SELL create an investment-account side and a cash-account side together; TRANSFER creates both sides of a cash or security transfer; DELIVERY_INBOUND and DELIVERY_OUTBOUND move shares without a cash side. The response is one created change whose uuid is the record id. A write that would make an investment account's holding of an instrument negative at the end of any date is refused.",
                    TransactionAdd.SCHEMA, context -> context.record(
                                    open(context.client(), context::existedBeforeBatch).add(context.json(), false)));

    /** <code>transaction.update</code>: a thin adapter over {@link #update}. */
    public static final OperationSpec<OperationSpec.Context> UPDATE = new OperationSpec<>(TransactionUpdate.UPDATE,
                    "Changes a whole transaction record, found by the UUID of either of its sides. Only the fields present change; null clears an optional field. The patch is overlaid on the current record and then validated exactly like an add, so a partial edit can't bypass validation and nothing is recomputed: fees, taxes and forex units that no longer fit a changed amount or shares are refused with a field-level error. The type, and the kind of a transfer, can't change; delete the record and add a new one. Both sides stay in step and keep their UUIDs. The response is one updated change, with before and after for the changed fields, whose uuid is the record id. A change that would make an investment account's holding of an instrument negative at the end of any date is refused.",
                    TransactionUpdate.PATCH_SCHEMA,
                    context -> context.record(open(context.client(), context::existedBeforeBatch)
                                    .update(context.json(), context.claims(), false)));

    /** <code>transaction.delete</code>: a thin adapter over {@link #delete}. */
    public static final OperationSpec<OperationSpec.Context> DELETE = new OperationSpec<>(TransactionUpdate.DELETE,
                    "Deletes a whole transaction record, both sides, found by the UUID of either of its sides. The response is one deleted change whose uuid is the record id, with the record as it was in before. A delete that would make an investment account's holding of an instrument negative at the end of any date, by removing a buy or a delivery or transfer into it, is refused.",
                    TransactionUpdate.DELETE_SCHEMA,
                    context -> context.record(open(context.client(), context::existedBeforeBatch)
                                    .delete(context.json(), context.claims(), false)));

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

    /** The input of an add: a record in the shape the output has, without the members an add derives. */
    public static Schema addSchema()
    {
        return TransactionAdd.SCHEMA;
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
     * Adds the record the input describes. Every member is checked against
     * the rules of its type, and the record is checked by core's import
     * checks and against overselling an investment account's holding; only a
     * record that passes all of them is put into the model.
     *
     * @return {@link Outcome.Applied} with the record as created, or
     *         {@link Outcome.Refused} with every failure, the model untouched
     */
    public Outcome add(JsonObject input)
    {
        return add(input, true);
    }

    /**
     * @param reportUnknown
     *            whether a member the add does not declare is reported; if
     *            not, it still refuses the input, so the failures of the
     *            refusal may be empty
     */
    Outcome add(JsonObject input, boolean reportUnknown)
    {
        var result = TransactionAdd.build(client, existedBeforeBatch, input, List.of(), reportUnknown);
        if (result.built() == null)
            return new Outcome.Refused(result.failures());

        result.built().insert().run();
        var record = new Join(result.built().sides()).toRecord(result.built().primary()).orElseThrow();
        return new Outcome.Applied(Outcome.Entity.TRANSACTION, record.id(), null, render(record));
    }

    /**
     * Changes the record that has a side with the <code>uuid</code> of the
     * patch. Every side is claimed first, so a record another operation
     * changes is refused before anything is checked. The patch is laid on the
     * record as stored and then checked exactly like an add, against the
     * holdings without the record's current sides; only a record that passes
     * is copied onto the existing sides, which keep their UUIDs and move to
     * other accounts if the patch says so.
     *
     * @return {@link Outcome.Applied} with the changed members before and
     *         after, {@link Outcome.Unchanged} if the patch changes nothing,
     *         or {@link Outcome.Refused} with every failure, the model
     *         untouched
     */
    public Outcome update(JsonObject patch, Claims claims)
    {
        return update(patch, claims, true);
    }

    /**
     * @param reportUnknown
     *            whether a member the update does not declare is reported; if
     *            not, it still refuses the patch
     */
    Outcome update(JsonObject patch, Claims claims, boolean reportUnknown)
    {
        return new TransactionUpdate(this, client, existedBeforeBatch).update(patch, claims, reportUnknown);
    }

    /**
     * Deletes the whole record, every side, that has a side with the
     * <code>uuid</code> of the input, after claiming every side. A delete
     * that would oversell an investment account's holding is refused.
     *
     * @return {@link Outcome.Applied} with the record as it was in
     *         <code>before</code>, or {@link Outcome.Refused}
     */
    public Outcome delete(JsonObject input, Claims claims)
    {
        return delete(input, claims, true);
    }

    Outcome delete(JsonObject input, Claims claims, boolean reportUnknown)
    {
        return new TransactionUpdate(this, client, existedBeforeBatch).delete(input, claims, reportUnknown);
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
        var transferRate = record.transferRate();
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
    static final class Join
    {
        private final IdentityHashMap<Transaction, Account> accountOwners = new IdentityHashMap<>();
        private final IdentityHashMap<Transaction, Portfolio> portfolioOwners = new IdentityHashMap<>();

        Join(Client client)
        {
            client.getAccounts().forEach(account -> account.getTransactions().forEach(t -> accountOwners.put(t, account)));
            client.getPortfolios().forEach(
                            portfolio -> portfolio.getTransactions().forEach(t -> portfolioOwners.put(t, portfolio)));
        }

        /** The join of just these sides, such as those of a record just built. */
        Join(List<BuiltRecord.Side> sides)
        {
            for (var side : sides)
            {
                if (side.owner() instanceof Account account)
                    accountOwners.put(side.transaction(), account);
                else
                    portfolioOwners.put(side.transaction(), (Portfolio) side.owner());
            }
        }

        /** The sides of the record, in its order, with the account or investment account each is in. */
        List<BuiltRecord.Side> withOwners(TransactionRecord record)
        {
            var sides = new ArrayList<BuiltRecord.Side>();
            accountOwners.forEach((t, owner) -> sides.add(new BuiltRecord.Side(t, owner)));
            portfolioOwners.forEach((t, owner) -> sides.add(new BuiltRecord.Side(t, owner)));
            return record.sides().stream().map(uuid -> sides.stream()
                            .filter(side -> uuid.equals(side.transaction().getUUID())).findFirst().orElseThrow())
                            .toList();
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
