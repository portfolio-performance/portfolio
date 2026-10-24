package name.abuchen.portfolio.cli;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.money.Values;

/**
 * <code>transactions</code>: every business event of the client as one
 * {@link TransactionRecord}, filtered, ordered by date-time then id, paged, and
 * on request with the amount converted into another currency.
 */
final class TransactionsCommand
{
    /** The types of a record, in the order they are published. */
    private static final List<String> TYPES = List.of("BUY", "SELL", "DEPOSIT", "REMOVAL", "INTEREST",
                    "INTEREST_CHARGE", "DIVIDENDS", "FEES", "FEES_REFUND", "TAXES", "TAX_REFUND", "DELIVERY_INBOUND",
                    "DELIVERY_OUTBOUND", TransactionRecord.TRANSFER);

    /** What an orphaned side can add under its raw core type. */
    private static final List<String> ORPHAN_TYPES = List.of("TRANSFER_IN", "TRANSFER_OUT");

    private static final List<String> FILTERABLE_TYPES = Stream.concat(TYPES.stream(),
                    ORPHAN_TYPES.stream()).toList();

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    private static final OptionSpec INVESTMENT_ACCOUNT = new OptionSpec("investment-account", "ref",
                    "Only records with a side in this investment account: its UUID, or for convenience its exact name. Names are accepted on reads only.");

    private static final OptionSpec CASH_ACCOUNT = new OptionSpec("cash-account", "ref",
                    "Only records with a side in this cash account: its UUID, or for convenience its exact name. Names are accepted on reads only.");

    private static final OptionSpec INSTRUMENT = new OptionSpec("instrument", "ref",
                    "Only records of this instrument, including its dividends, fees and taxes: its UUID, or for convenience its ISIN, ticker or exact name. Name, ISIN and ticker references are accepted on reads only.");

    private static final OptionSpec TYPE = new OptionSpec("type", "types",
                    "Only records of these types: a comma-separated list, case-insensitive, of any value the type field can have: "
                                    + String.join(", ", FILTERABLE_TYPES) + ".");

    private static final OptionSpec FROM = OptionSpec.date("from",
                    "First day of the range, inclusive. Without it the range has no lower bound.");

    private static final OptionSpec TO = OptionSpec.date("to",
                    "Last day of the range, inclusive. Defaults to today, so records dated in the future are hidden unless a later date is given.");

    private static final OptionSpec CURRENCY = new OptionSpec("currency", "code",
                    "Adds to every record its amount converted into this ISO currency, with the rate, its date and its source. Amounts stay in each record's own currency.");

    private static Schema unit()
    {
        return Schema.object(
                        "A fee, tax or gross value of the record, in the shape that transaction writes accept.")
                        .property("type", Schema.enumOf("Kind of unit.", List.of("FEE", "TAX", "GROSS_VALUE")))
                        .property("amount", Schema.number("Amount in the currency of the record."))
                        .property("currencyCode", Schema.string("ISO currency code of the amount."))
                        .optionalProperty("forexAmount", Schema.number("Original amount in a foreign currency."))
                        .optionalProperty("forexCurrencyCode", Schema.string("ISO currency code of forexAmount."))
                        .optionalProperty("exchangeRate", Schema.number(
                                        "Unrounded as stored: forexAmount times exchangeRate is approximately amount."));
    }

    private static Schema recordSchema()
    {
        return Schema.object("One business event.")
                        .property("id", Schema.string(
                                        "UUID of the investment-account side of a buy or sell, the outgoing side of a transfer, and the transaction itself otherwise."))
                        .property("type", Schema.openEnumOf(
                                        "Core's transaction type. An open enum: new values may appear. An intact transfer is TRANSFER; a side whose partner is missing keeps its raw core type, e.g. TRANSFER_OUT.",
                                        FILTERABLE_TYPES))
                        .optionalProperty("kind", Schema.enumOf("Of a TRANSFER: cash or security.",
                                        TransactionRecord.TransferKind.ids()))
                        .property("date", Schema.string("ISO date."))
                        .optionalProperty("time", Schema.string("HH:mm; absent if the stored time is midnight."))
                        .property("sides", Schema.array("UUIDs of every side of the event, id first.",
                                        Schema.string(null)))
                        .property("amount", Schema.number(
                                        "Net amount as stored, from the investment-account side of a buy or sell and the outgoing side of a transfer."))
                        .property("currencyCode", Schema.string("ISO currency code of amount."))
                        .optionalProperty("targetAmount",
                                        Schema.number("Amount of the incoming side of a cross-currency cash transfer."))
                        .optionalProperty("targetCurrencyCode",
                                        Schema.string("ISO currency code of targetAmount."))
                        .optionalProperty("exchangeRate", Schema.number(
                                        "Of a cross-currency cash transfer with a rate: unrounded as stored, targetAmount times exchangeRate is approximately amount."))
                        .optionalProperty("instrument", Schema.string("UUID of the instrument."))
                        .optionalProperty("instrumentName", Schema.string(
                                        "Name of the instrument; empty if it has none. Present exactly when instrument is."))
                        .optionalProperty("shares", Schema.number("Number of shares."))
                        .optionalProperty("investmentAccount", Schema.string("UUID of the investment account."))
                        .optionalProperty("investmentAccountName", Schema.string(
                                        "Name of the investment account; empty if it has none. Present exactly when investmentAccount is."))
                        .optionalProperty("cashAccount", Schema.string("UUID of the cash account."))
                        .optionalProperty("cashAccountName", Schema.string(
                                        "Name of the cash account; empty if it has none. Present exactly when cashAccount is."))
                        .optionalProperty("from", Schema.string("Of a TRANSFER: UUID of the account it leaves."))
                        .optionalProperty("fromName", Schema.string(
                                        "Name of the account it leaves; empty if it has none. Present exactly when from is."))
                        .optionalProperty("to", Schema.string("Of a TRANSFER: UUID of the account it enters."))
                        .optionalProperty("toName", Schema.string(
                                        "Name of the account it enters; empty if it has none. Present exactly when to is."))
                        .optionalProperty("exDate", Schema.string("ISO ex-date of a dividend."))
                        .optionalProperty("note", Schema.string("The note."))
                        .optionalProperty("source", Schema.string("Where the record was imported from."))
                        .property("units", Schema.array("Fees, taxes and forex of the record; empty if none.", unit()))
                        .optionalProperty("converted", ExchangeRates.convertedSchema(
                                        "The amount in the currency asked for with --currency. Absent if no rate was found."));
    }

    private static final Schema DATA = Paging.pageSchema("A page of the transaction records.", recordSchema());

    private static final Schema META = DateRange.declareIn(ResponseMeta.pagedSchema())
                    .optionalProperty("currency", Schema.string("The currency asked for, as given."))
                    .optionalProperty("investmentAccount", Schema.string("UUID the investment-account filter resolved to."))
                    .optionalProperty("cashAccount", Schema.string("UUID the cash-account filter resolved to."))
                    .optionalProperty("instrument", Schema.string("UUID the instrument filter resolved to."))
                    .optionalProperty("types", Schema.array("The type filter: upper case, without duplicates, in the published order.",
                                    Schema.string(null)))
                    .optionalProperty("exchangeRates", ExchangeRates.metaSchema());

    static final CommandSpec SPEC = new CommandSpec("transactions",
                    "Business events as transaction records, each joining the sides of a buy, sell or transfer, filtered by date range, account, instrument and type, ordered by date-time then id.",
                    true,
                    List.of(INVESTMENT_ACCOUNT, CASH_ACCOUNT, INSTRUMENT, TYPE, FROM, TO, CURRENCY, Paging.LIMIT,
                                    Paging.OFFSET),
                    DATA, META, ExchangeRates.WARNINGS, TransactionsCommand::execute);

    private TransactionsCommand()
    {
    }

    /** What a record must match to be listed; absent filters match everything. */
    private record Filter(DateRange range, Optional<Portfolio> investmentAccount, Optional<Account> cashAccount,
                    Optional<Security> instrument, List<String> types)
    {
        boolean test(TransactionRecord record)
        {
            return range.contains(record.primary().getDateTime().toLocalDate())
                            && investmentAccount.map(account -> record.touchesInvestmentAccount(account.getUUID()))
                                            .orElse(true)
                            && cashAccount.map(account -> record.touchesCashAccount(account.getUUID())).orElse(true)
                            && instrument.map(security -> record.primary().getSecurity() == security).orElse(true)
                            && (types.isEmpty() || types.contains(record.type()));
        }
    }

    private static Response execute(CommandContext context) throws CliException
    {
        var paging = Paging.parse(context);
        var range = DateRange.parse(context, FROM, TO);
        var types = parseTypes(context.option(TYPE.name()));
        var currency = ExchangeRates.parseCurrency(CURRENCY, context.option(CURRENCY.name()));

        var client = context.loadClient().client();

        var investmentAccount = resolve(context, INVESTMENT_ACCOUNT, client, References::investmentAccount);
        var cashAccount = resolve(context, CASH_ACCOUNT, client, References::cashAccount);
        var instrument = resolve(context, INSTRUMENT, client, References::instrument);

        var filter = new Filter(range, investmentAccount, cashAccount, instrument, types);
        var records = TransactionRecord.allOf(client).stream().filter(filter::test)
                        .sorted(Comparator.comparing((TransactionRecord record) -> record.primary().getDateTime())
                                        .thenComparing(TransactionRecord::id))
                        .toList();
        var page = paging.of(records);

        var rates = new ExchangeRates(context, client);
        var names = new AccountNames(client);
        var data = new LinkedHashMap<String, Object>();
        page.putInto(data, record -> render(record, names, currency, rates));

        var meta = ResponseMeta.paged(context, client, paging);
        range.putInto(meta);
        currency.ifPresent(code -> meta.put("currency", code));
        investmentAccount.ifPresent(account -> meta.put("investmentAccount", account.getUUID()));
        cashAccount.ifPresent(account -> meta.put("cashAccount", account.getUUID()));
        instrument.ifPresent(security -> meta.put("instrument", security.getUUID()));
        if (!types.isEmpty())
            meta.put("types", FILTERABLE_TYPES.stream().filter(types::contains).toList());
        rates.putCacheInto(meta);

        var response = Response.of(data, meta);
        rates.warnings().forEach(response::warn);
        return response;
    }

    @FunctionalInterface
    private interface Resolver<T>
    {
        T resolve(Client client, String option, String value) throws CliException;
    }

    /** What the option refers to, or empty if the option is absent. */
    private static <T> Optional<T> resolve(CommandContext context, OptionSpec option, Client client,
                    Resolver<T> resolver) throws CliException
    {
        var value = context.option(option.name());
        return value.isPresent() ? Optional.of(resolver.resolve(client, option.name(), value.get()))
                        : Optional.empty();
    }

    /** The upper-cased types of the comma-separated list; empty if the option is absent. */
    private static List<String> parseTypes(Optional<String> raw) throws CliException
    {
        if (raw.isEmpty())
            return List.of();

        var types = new ArrayList<String>();
        for (var item : raw.get().split(",", -1))
        {
            var type = item.trim().toUpperCase(Locale.ROOT);
            if (!FILTERABLE_TYPES.contains(type))
                throw CliException.usage("invalid-option-value", "Invalid option value",
                                "Option '--" + TYPE.name() + "' must be a comma-separated list of: "
                                                + String.join(", ", FILTERABLE_TYPES) + ".")
                                .with("option", TYPE.name()).with("value", raw.get());
            types.add(type);
        }
        return types;
    }

    private static Map<String, Object> render(TransactionRecord record, AccountNames names,
                    Optional<String> currency, ExchangeRates rates)
    {
        var currencyCode = MasterData.orEmpty(record.primary().getCurrencyCode());
        var json = render(record, names);
        currency.flatMap(target -> convert(record, currencyCode, target, rates))
                        .ifPresent(converted -> json.put("converted", converted));
        return json;
    }

    /** The names of the accounts and investment accounts of a client by UUID. */
    static final class AccountNames
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

    /** The record's fields as written, without the {@code --currency} conversion; also the shape writes return. */
    static Map<String, Object> render(TransactionRecord record, Client client)
    {
        return render(record, new AccountNames(client));
    }

    private static Map<String, Object> render(TransactionRecord record, AccountNames names)
    {
        var primary = record.primary();
        var dateTime = primary.getDateTime();
        var currencyCode = MasterData.orEmpty(primary.getCurrencyCode());

        var json = new LinkedHashMap<String, Object>();
        json.put("id", record.id());
        json.put("type", record.type());
        record.kind().ifPresent(kind -> json.put("kind", kind.id()));
        json.put("date", dateTime.toLocalDate().toString());
        if (!dateTime.toLocalTime().equals(LocalTime.MIDNIGHT))
            json.put("time", dateTime.format(TIME));
        json.put("sides", record.sides());
        json.put("amount", Decimals.of(primary.getAmount(), Values.Amount));
        json.put("currencyCode", currencyCode);

        var crossCurrency = record.crossCurrencyIncoming();
        var transferRate = crossCurrency.flatMap(incoming -> primary.getUnit(Transaction.Unit.Type.GROSS_VALUE)
                        .filter(unit -> unit.getForex().getCurrencyCode().equals(incoming.getCurrencyCode())));
        crossCurrency.ifPresent(incoming -> {
            json.put("targetAmount", Decimals.of(incoming.getAmount(), Values.Amount));
            json.put("targetCurrencyCode", MasterData.orEmpty(incoming.getCurrencyCode()));
        });
        transferRate.ifPresent(unit -> json.put("exchangeRate", Decimals.of(unit.getExchangeRate())));

        if (primary.getSecurity() != null)
        {
            json.put("instrument", primary.getSecurity().getUUID());
            json.put("instrumentName", MasterData.orEmpty(primary.getSecurity().getName()));
        }
        if (record.hasShares())
            json.put("shares", Decimals.of(primary.getShares(), Values.Share));
        putAccount(json, "investmentAccount", record.investmentAccount(), names);
        putAccount(json, "cashAccount", record.cashAccount(), names);
        putAccount(json, "from", record.from(), names);
        putAccount(json, "to", record.to(), names);
        if (primary instanceof AccountTransaction account && account.getExDate() != null
                        && account.getType() == AccountTransaction.Type.DIVIDENDS)
            json.put("exDate", account.getExDate().toLocalDate().toString());
        MasterData.putIfSet(json, "note", primary.getNote());
        MasterData.putIfSet(json, "source", primary.getSource());

        json.put("units", primary.getUnits().filter(unit -> transferRate.isEmpty() || unit != transferRate.get())
                        .map(TransactionsCommand::render).toList());

        return json;
    }

    /** The UUID under the member and the name next to it, if there is an account. */
    private static void putAccount(Map<String, Object> json, String member, Optional<String> uuid,
                    AccountNames names)
    {
        uuid.ifPresent(value -> {
            json.put(member, value);
            json.put(member + "Name", names.of(value));
        });
    }

    private static Map<String, Object> render(Transaction.Unit unit)
    {
        var json = new LinkedHashMap<String, Object>();
        json.put("type", unit.getType().name());
        json.put("amount", Decimals.amount(unit.getAmount()));
        json.put("currencyCode", unit.getAmount().getCurrencyCode());
        if (unit.getForex() != null)
        {
            json.put("forexAmount", Decimals.amount(unit.getForex()));
            json.put("forexCurrencyCode", unit.getForex().getCurrencyCode());
            json.put("exchangeRate", Decimals.of(unit.getExchangeRate()));
        }
        return json;
    }

    /**
     * The amount in the target currency, or empty if no rate was found. Rates
     * are tried in order: the same currency, the record's own rate, then the
     * rate series of {@link ExchangeRates}.
     */
    private static Optional<Map<String, Object>> convert(TransactionRecord record, String currencyCode, String target,
                    ExchangeRates rates)
    {
        var day = record.primary().getDateTime().toLocalDate();

        var rate = ownRate(record, currencyCode, target, day).or(() -> rates.series(day, currencyCode, target));

        return rate.map(r -> ExchangeRates.converted(target, r.convert(record.primary().getAmount()), r));
    }

    /** The rate that needs no series: same currency, or the record's own forex unit or transfer rate. */
    private static Optional<ExchangeRates.Rate> ownRate(TransactionRecord record, String currencyCode, String target,
                    LocalDate day)
    {
        if (currencyCode.equals(target))
            return Optional.of(new ExchangeRates.Rate(BigDecimal.ONE, day, ExchangeRates.Source.IDENTITY));

        var forex = record.primary().getUnits().filter(unit -> unit.getType() == Transaction.Unit.Type.GROSS_VALUE)
                        .filter(unit -> unit.getForex() != null && target.equals(unit.getForex().getCurrencyCode()))
                        .map(Transaction.Unit::getExchangeRate);

        var transfer = record.crossCurrencyIncoming().filter(incoming -> target.equals(incoming.getCurrencyCode()))
                        .flatMap(incoming -> record.primary().getUnit(Transaction.Unit.Type.GROSS_VALUE))
                        .map(Transaction.Unit::getExchangeRate);

        return forex.findFirst().or(() -> transfer).filter(stored -> stored.signum() != 0)
                        .map(stored -> new ExchangeRates.Rate(BigDecimal.ONE.divide(stored, 10, RoundingMode.HALF_DOWN),
                                        day, ExchangeRates.Source.TRANSACTION));
    }
}
