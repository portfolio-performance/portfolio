package name.abuchen.portfolio.cli;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    private static final ReferenceOption<Portfolio> INVESTMENT_ACCOUNT = ReferenceOption.investmentAccount(
                    "investment-account", "Only records with a side in this investment account.");

    private static final ReferenceOption<Account> CASH_ACCOUNT = ReferenceOption.cashAccount("cash-account",
                    "Only records with a side in this cash account.");

    private static final ReferenceOption<Security> INSTRUMENT = ReferenceOption.instrument("instrument",
                    "Only records of this instrument, including its dividends, fees and taxes.");

    private static final OptionSpec TYPE = new OptionSpec("type", "types",
                    "Only records of these types: comma-separated, case-insensitive values of the type field.");

    private static final OptionSpec FROM = OptionSpec.date("from",
                    "First day of the range, inclusive. Without it, no lower bound.");

    private static final OptionSpec TO = OptionSpec.date("to",
                    "Last day of the range, inclusive. Defaults to today.");

    private static final OptionSpec CURRENCY = ReportingCurrency.option(
                    "Adds converted to every record: its amount in this ISO currency, with rate, date and source. amount stays in the record's currency.");

    private static Schema recordSchema()
    {
        return TransactionShape.recordSchema("One business event.")
                        .optionalProperty("converted", ReportingCurrency.convertedSchema(
                                        "The amount in the currency asked for with --currency. Absent if no rate was found."));
    }

    private static final Sorting.Spec<TransactionRecord> SORT = Sorting.Spec.of(TransactionRecord::id, "date",
                    Sorting.Field.of("date", Comparator.comparing((TransactionRecord record) -> record.primary().getDateTime())),
                    Sorting.Field.of("updatedAt", Sorting.byInstant(
                                    (TransactionRecord record) -> record.updatedAt().flatMap(Sorting::lastModified))));

    private static final Schema DATA = Paging.pageSchema("A page of the transaction records.", recordSchema());

    private static final Schema META = DateRange.declareIn(ResponseMeta.pagedSchema(SORT))
                    .optionalProperty("currency", Schema.string("The currency asked for, as given."))
                    .optionalProperty("investmentAccount", Schema.string("UUID the investment-account filter resolved to."))
                    .optionalProperty("cashAccount", Schema.string("UUID the cash-account filter resolved to."))
                    .optionalProperty("instrument", Schema.string("UUID the instrument filter resolved to."))
                    .optionalProperty("types", Schema.array("The type filter: upper case, without duplicates, in the published order.",
                                    Schema.string(null)))
                    .optionalProperty("exchangeRates", ReportingCurrency.ratesMetaSchema());

    static final CommandSpec SPEC = new CommandSpec("transactions",
                    "Business events as transaction records, each joining the sides of a buy, sell or transfer, filtered by date range, account, instrument and type.",
                    true,
                    List.of(INVESTMENT_ACCOUNT.spec(), CASH_ACCOUNT.spec(), INSTRUMENT.spec(), TYPE, FROM, TO,
                                    CURRENCY, Paging.LIMIT, Paging.OFFSET, SORT.sortOption(), Sorting.ORDER),
                    DATA, META, ReportingCurrency.WARNINGS, TransactionsCommand::execute);

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
        var sorting = SORT.parse(context);
        var range = DateRange.parse(context, FROM, TO);
        var types = parseTypes(context.text(TYPE));
        var requested = ReportingCurrency.requested(context, CURRENCY);

        var client = context.loadClient().client();
        var reporting = requested.ifGiven(context, client);

        var investmentAccount = context.reference(INVESTMENT_ACCOUNT, client);
        var cashAccount = context.reference(CASH_ACCOUNT, client);
        var instrument = context.reference(INSTRUMENT, client);

        var filter = new Filter(range, investmentAccount, cashAccount, instrument, types);
        var records = sorting.sort(client, TransactionRecord.allOf(client).stream().filter(filter::test).toList());
        var page = paging.of(records);

        var names = new AccountNames(client);
        var data = new LinkedHashMap<String, Object>();
        page.putInto(data, record -> render(record, names, reporting));

        var meta = ResponseMeta.paged(context, client, paging, sorting);
        range.putInto(meta);
        reporting.ifPresent(currency -> meta.put("currency", currency.code()));
        investmentAccount.ifPresent(account -> meta.put("investmentAccount", account.getUUID()));
        cashAccount.ifPresent(account -> meta.put("cashAccount", account.getUUID()));
        instrument.ifPresent(security -> meta.put("instrument", security.getUUID()));
        if (!types.isEmpty())
            meta.put("types", TransactionShape.FILTERABLE_TYPES.stream().filter(types::contains).toList());
        reporting.ifPresent(currency -> currency.putRatesInto(meta));

        var response = Response.of(data, meta);
        reporting.ifPresent(currency -> currency.warnings().forEach(response::warn));
        return response;
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
            if (!TransactionShape.FILTERABLE_TYPES.contains(type))
                throw CliException.usage("invalid-option-value", "Invalid option value",
                                "Option '--" + TYPE.name() + "' must be a comma-separated list of: "
                                                + String.join(", ", TransactionShape.FILTERABLE_TYPES) + ".")
                                .with("option", TYPE.name()).with("value", raw.get());
            types.add(type);
        }
        return types;
    }

    private static Map<String, Object> render(TransactionRecord record, AccountNames names,
                    Optional<ReportingCurrency> reporting)
    {
        var json = render(record, names);
        reporting.ifPresent(currency -> currency.putConvertedInto(json, figure(record, currency)));
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
                                        .map(TransactionsCommand::render).toList());

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

    /** The amount of the record on its day, with the record's own rate into the reporting currency if it has one. */
    private static ReportingCurrency.Figure figure(TransactionRecord record, ReportingCurrency reporting)
    {
        var primary = record.primary();
        return reporting.figure(primary.getDateTime().toLocalDate(), MasterData.orEmpty(primary.getCurrencyCode()),
                        primary.getAmount(), ownRate(record, reporting.code()));
    }

    /** The record's own rate into the reporting currency: that of its forex unit or of its transfer. */
    private static Optional<BigDecimal> ownRate(TransactionRecord record, String reportingCode)
    {
        var forex = record.primary().getUnits().filter(unit -> unit.getType() == Transaction.Unit.Type.GROSS_VALUE)
                        .filter(unit -> unit.getForex() != null && reportingCode.equals(unit.getForex().getCurrencyCode()))
                        .map(Transaction.Unit::getExchangeRate);

        var transfer = record.crossCurrencyIncoming().filter(incoming -> reportingCode.equals(incoming.getCurrencyCode()))
                        .flatMap(incoming -> record.primary().getUnit(Transaction.Unit.Type.GROSS_VALUE))
                        .map(Transaction.Unit::getExchangeRate);

        // the stored rate converts into the record's currency, so the inverse converts out of it
        return forex.findFirst().or(() -> transfer).filter(stored -> stored.signum() != 0)
                        .map(stored -> BigDecimal.ONE.divide(stored, 10, RoundingMode.HALF_DOWN));
    }
}
