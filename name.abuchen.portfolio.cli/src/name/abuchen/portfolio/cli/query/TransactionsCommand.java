package name.abuchen.portfolio.cli.query;

import java.util.Comparator;
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
import name.abuchen.portfolio.cli.transaction.TransactionRecord;
import name.abuchen.portfolio.cli.transaction.TransactionRecords;
import name.abuchen.portfolio.cli.transaction.TransactionShape;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.Security;

/**
 * <code>transactions</code>: every business event of the client as one
 * {@link TransactionRecord}, filtered, ordered by date-time then id, paged, and
 * on request with the amount converted into another currency.
 */
public final class TransactionsCommand
{
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
        return TransactionRecords.schema("One business event.")
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

        var transactions = TransactionRecords.open(client, any -> true);
        var records = run.get(SORT).sort(client, transactions.all().stream().filter(filter).toList());
        var page = run.get(Paging.FACET).of(records);

        var reporting = run.get(CURRENCY);
        var data = new LinkedHashMap<String, Object>();
        page.putInto(data, record -> render(transactions, record, reporting));
        return data;
    }

    private static Map<String, Object> render(TransactionRecords transactions, TransactionRecord record,
                    Optional<ReportingCurrency> reporting)
    {
        var json = transactions.render(record);
        reporting.ifPresent(currency -> currency.putConvertedInto(json, figure(record, currency)));
        return json;
    }

    /** The amount of the record on its day, with the record's own rate into the reporting currency if it has one. */
    private static ReportingCurrency.Figure figure(TransactionRecord record, ReportingCurrency reporting)
    {
        var primary = record.primary();
        return reporting.figure(primary.getDateTime().toLocalDate(), MasterData.orEmpty(primary.getCurrencyCode()),
                        primary.getAmount(), record.ownRate(reporting.code()));
    }
}
