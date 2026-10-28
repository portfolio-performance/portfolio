package name.abuchen.portfolio.cli.query;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import name.abuchen.portfolio.cli.command.CliException;
import name.abuchen.portfolio.cli.command.ClientFile;
import name.abuchen.portfolio.cli.command.CommandContext;
import name.abuchen.portfolio.cli.command.CommandSpec;
import name.abuchen.portfolio.cli.command.DateRange;
import name.abuchen.portfolio.cli.command.Decimals;
import name.abuchen.portfolio.cli.command.Declaration;
import name.abuchen.portfolio.cli.command.Facet;
import name.abuchen.portfolio.cli.command.Filters;
import name.abuchen.portfolio.cli.command.MasterData;
import name.abuchen.portfolio.cli.command.OptionSpec;
import name.abuchen.portfolio.cli.command.Paging;
import name.abuchen.portfolio.cli.command.ReferenceOption;
import name.abuchen.portfolio.cli.command.Schema;
import name.abuchen.portfolio.cli.command.Sorting;
import name.abuchen.portfolio.cli.currency.ReportingCurrency;
import name.abuchen.portfolio.cli.masterdata.CashAccounts;
import name.abuchen.portfolio.cli.masterdata.Instruments;
import name.abuchen.portfolio.cli.masterdata.InvestmentAccounts;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.money.ExchangeRate;
import name.abuchen.portfolio.money.Values;

/**
 * <code>transactions</code>: every business event of the client as one
 * {@link TransactionRecord}, filtered, ordered by date-time then id, paged, and
 * on request with the amount converted into another currency.
 */
public final class TransactionsCommand
{
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    private static final Facet<Filters.Reference<Portfolio, TransactionRecord>> INVESTMENT_ACCOUNT = Filters
                    .reference(InvestmentAccounts.INVESTMENT_ACCOUNTS.option("investment-account",
                                    "Only records with a side in this investment account."), "investmentAccount",
                                    "UUID the investment-account filter resolved to.",
                                    (account, record) -> record.touchesInvestmentAccount(account.getUUID()));

    private static final Facet<Filters.Reference<Account, TransactionRecord>> CASH_ACCOUNT = Filters.reference(
                    CashAccounts.CASH_ACCOUNTS.option("cash-account", "Only records with a side in this cash account."),
                    "cashAccount", "UUID the cash-account filter resolved to.",
                    (account, record) -> record.touchesCashAccount(account.getUUID()));

    private static final Facet<Filters.Reference<Security, TransactionRecord>> INSTRUMENT = Filters.reference(
                    Instruments.INSTRUMENTS.option("instrument",
                                    "Only records of this instrument, including its dividends, fees and taxes."),
                    "instrument", "UUID the instrument filter resolved to.",
                    (security, record) -> record.primary().getSecurity() == security);

    private static final OptionSpec TYPE_OPTION = OptionSpec.choices("type", "types",
                    "Only records of these types: comma-separated, case-insensitive values of the type field.",
                    TransactionShape.FILTERABLE_TYPES);

    /** The types to list; empty lists every type. <code>meta</code> gets them if any are given. */
    private static final Facet<List<String>> TYPE = Facet.of(Declaration.empty().options(TYPE_OPTION)
                    .optionalMeta("types", Schema.array(
                                    "The type filter: upper case, without duplicates, in the published order.",
                                    Schema.string(null))),
                    run -> run.choices(TYPE_OPTION), (types, out) -> {
                        if (!types.isEmpty())
                            out.meta("types", types);
                    });

    private static final Facet<DateRange> RANGE = DateRange.facet(
                    "First day of the range, inclusive. Without it, no lower bound.",
                    "Last day of the range, inclusive. Defaults to today.");

    private static final Facet<Optional<ReportingCurrency>> CURRENCY = ReportingCurrency.ifGiven(
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

    public static final CommandSpec SPEC = CommandSpec.of("transactions",
                    "Business events as transaction records, each joining the sides of a buy, sell or transfer, filtered by date range, account, instrument and type.",
                    DATA, TransactionsCommand::execute, ClientFile.READ, INVESTMENT_ACCOUNT, CASH_ACCOUNT, INSTRUMENT,
                    TYPE, RANGE, CURRENCY, Paging.FACET, SORT);

    private TransactionsCommand()
    {
    }

    private static Map<String, Object> execute(CommandContext run) throws CliException
    {
        var client = run.client();
        var range = run.get(RANGE);
        var types = run.get(TYPE);
        Predicate<TransactionRecord> filter = record -> range.contains(record.primary().getDateTime().toLocalDate())
                        && (types.isEmpty() || types.contains(record.type()));
        filter = filter.and(run.get(INVESTMENT_ACCOUNT)).and(run.get(CASH_ACCOUNT)).and(run.get(INSTRUMENT));

        var records = run.get(SORT).sort(client, TransactionRecord.allOf(client).stream().filter(filter).toList());
        var page = run.get(Paging.FACET).of(records);

        var names = new AccountNames(client);
        var reporting = run.get(CURRENCY);
        var data = new LinkedHashMap<String, Object>();
        page.putInto(data, record -> render(record, names, reporting));
        return data;
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
    public static Map<String, Object> render(TransactionRecord record, Client client)
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
                        .map(stored -> ExchangeRate.inverse(stored));
    }
}
