package name.abuchen.portfolio.cli.query;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import name.abuchen.portfolio.cli.command.CliException;
import name.abuchen.portfolio.cli.command.ClientFile;
import name.abuchen.portfolio.cli.command.CommandContext;
import name.abuchen.portfolio.cli.command.CommandSpec;
import name.abuchen.portfolio.cli.command.Decimals;
import name.abuchen.portfolio.cli.command.Facet;
import name.abuchen.portfolio.cli.command.Filters;
import name.abuchen.portfolio.cli.command.MasterData;
import name.abuchen.portfolio.cli.command.MinorUnits;
import name.abuchen.portfolio.cli.command.OptionSpec;
import name.abuchen.portfolio.cli.command.ReferenceOption;
import name.abuchen.portfolio.cli.command.Schema;
import name.abuchen.portfolio.cli.currency.ReportingCurrency;
import name.abuchen.portfolio.cli.currency.StalePrices;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.AccountSnapshot;
import name.abuchen.portfolio.snapshot.ClientSnapshot;
import name.abuchen.portfolio.snapshot.PortfolioSnapshot;
import name.abuchen.portfolio.snapshot.SecurityPosition;

/**
 * <code>holdings</code>: what is owned and what it is worth at a date. Shares
 * and values are core's client snapshot; the totals are in one currency, with
 * the rates behind every converted figure (ADR 0005).
 * <p>
 * A total is only as good as its parts: if a figure that needs a rate has none,
 * the total that contains it is left out rather than stated incompletely, and
 * the missing rate is reported as a warning.
 */
public final class HoldingsCommand
{
    private static final Facet<LocalDate> DATE = ValuationDay.facet(
                    "Day to value the holdings at. Defaults to today. Holdings use the prices on or before this day, and conversions the rates on or before it.",
                    "The day the holdings are valued at: as given, otherwise today.");

    private static final Facet<Filters.Reference<Portfolio, PortfolioSnapshot>> INVESTMENT_ACCOUNT = Filters.reference(
                    ReferenceOption.investmentAccount("investment-account",
                                    "Only the holdings of this investment account. The cash section is not affected."),
                    "investmentAccount", "UUID the investment-account filter resolved to.",
                    (account, snapshot) -> snapshot.unwrapPortfolio() == account);

    private static final Facet<Boolean> BY_ACCOUNT = Facet.option(OptionSpec.flag("by-account",
                    "Split the holdings into one block per investment account instead of one aggregate."));

    private static final Facet<Boolean> INCLUDE_ZERO = Facet.<Boolean> option(OptionSpec.flag("include-zero",
                    "Also list instruments held before the date and fully sold by it, with zero shares. They are hidden by default."))
                    .withSample("--include-zero");

    private static final Facet<ReportingCurrency> CURRENCY = ReportingCurrency.facet(
                    "ISO currency of the totals and converted amounts. Defaults to the base currency of the file. Amounts stay in their native currency as well.",
                    ReportingCurrency.Counting.ITEMS);

    private static Schema total(String description)
    {
        return Schema.object(description).property("currencyCode", Schema.string("The reporting currency."))
                        .property("amount", Schema.number("Sum of the converted amounts."));
    }

    private static Schema holding()
    {
        return Schema.object("One held instrument.")
                        .property("instrument", Schema.string("UUID of the instrument."))
                        .property("name", Schema.string("Instrument name; empty if the file has none."))
                        .property("shares", Schema.number("Number of shares held."))
                        .optionalProperty("price", Schema.object(
                                        "The price valuation used: the last one on or before the date, or the last transaction's price if there is none. Absent for a sold-out holding.")
                                        .property("date", Schema.string("ISO date of the price."))
                                        .property("value", Schema.number("Price per share in the currency of the instrument."
                                                        + MinorUnits.CONVERTED_NEXT_TO_IT)))
                        .property("amount", Schema.number("Market value in the currency of the instrument." + MinorUnits.CONVERTED_NEXT_TO_IT))
                        .property("currencyCode", Schema.string("ISO currency code of the instrument." + MinorUnits.CONVERTED_NEXT_TO_IT))
                        .optionalProperty("converted", ReportingCurrency.convertedSchema(
                                        "The market value in the reporting currency, at the rate of the date. Absent if no rate was found."));
    }

    private static Schema cashAccount()
    {
        return Schema.object("The balance of one cash account.")
                        .property("cashAccount", Schema.string("UUID of the cash account."))
                        .property("name", Schema.string("Account name; empty if the file has none."))
                        .property("amount", Schema.number("Balance at the date in the currency of the account."))
                        .property("currencyCode", Schema.string("ISO currency code of the account."))
                        .optionalProperty("converted", ReportingCurrency.convertedSchema(
                                        "The balance in the reporting currency, at the rate of the date. Absent if no rate was found."));
    }

    private static final Schema DATA = Schema.object("What is held at the date.")
                    .property("holdings",
                                    Schema.array("One block of holdings: the aggregate, or one block per investment account.",
                                                    Schema.object("A block of holdings.")
                                                                    .optionalProperty("investmentAccount", Schema
                                                                                    .object("The investment account of the block. Absent for the aggregate of all accounts.")
                                                                                    .property("uuid", Schema.string("Canonical UUID."))
                                                                                    .property("name", Schema.string(
                                                                                                    "Account name; empty if the file has none.")))
                                                                    .property("items", Schema.array(
                                                                                    "The holdings, ordered by name then UUID.",
                                                                                    holding()))
                                                                    .optionalProperty("total", total(
                                                                                    "Value of the block in the reporting currency. Absent if a holding has no rate."))))
                    .property("cash", Schema.object("The cash accounts, whatever the investment-account filter.")
                                    .property("items", Schema.array("The balances, ordered by name then UUID.",
                                                    cashAccount()))
                                    .optionalProperty("total", total(
                                                    "Sum of the balances in the reporting currency. Absent if a balance has no rate.")));

    public static final CommandSpec SPEC = CommandSpec.of("holdings",
                    "What is held at a date and what it is worth: holdings aggregated across investment accounts, or per account, with cash balances in a separate section, in the base currency or --currency.",
                    DATA, HoldingsCommand::execute, ClientFile.READ, DATE, INVESTMENT_ACCOUNT, BY_ACCOUNT, INCLUDE_ZERO,
                    StalePrices.FACET, CURRENCY);

    private HoldingsCommand()
    {
    }

    /** The holdings of one block: an investment account, or the aggregate of all of them. */
    private record Block(Optional<Portfolio> account, List<Portfolio> scope, PortfolioSnapshot snapshot)
    {
    }

    /** A held instrument: core's position, or a sold-out one that only <code>--include-zero</code> shows. */
    private record Row(Security security, long shares, Money value, Optional<SecurityPrice> price)
    {
        static Row of(SecurityPosition position)
        {
            return new Row(position.getSecurity(), position.getShares(), position.calculateValue(),
                            Optional.of(position.getPrice()));
        }

        static Row soldOut(Security security)
        {
            return new Row(security, 0, Money.of(security.getCurrencyCode(), 0), Optional.empty());
        }
    }

    private static Map<String, Object> execute(CommandContext run) throws CliException
    {
        var date = run.get(DATE);
        var client = run.client();
        var filter = run.get(INVESTMENT_ACCOUNT);
        var reporting = run.get(CURRENCY);
        var snapshot = ClientSnapshot.create(client, reporting.coreFigures().unreported(), date);

        var blocks = new ArrayList<Block>();
        if (run.get(BY_ACCOUNT) || filter.entity().isPresent())
        {
            snapshot.getPortfolios().stream().filter(filter)
                            .sorted(MasterData.byNameThenUuid((PortfolioSnapshot s) -> s.unwrapPortfolio().getName(),
                                            s -> s.unwrapPortfolio().getUUID()))
                            .forEach(s -> blocks.add(new Block(Optional.of(s.unwrapPortfolio()),
                                            List.of(s.unwrapPortfolio()), s)));
        }
        else
        {
            blocks.add(new Block(Optional.empty(), client.getPortfolios(), snapshot.getJointPortfolio()));
        }

        var includeZero = run.get(INCLUDE_ZERO);
        var stalePrices = run.get(StalePrices.FACET);

        var holdings = new ArrayList<Map<String, Object>>();
        for (var block : blocks)
        {
            var rows = rows(block, date, includeZero);
            rows.forEach(row -> row.price().ifPresent(price -> stalePrices.note(row.security(), price, date)));
            holdings.add(render(block, rows, date, reporting));
        }

        var data = new LinkedHashMap<String, Object>();
        data.put("holdings", holdings);
        data.put("cash", renderCash(snapshot.getAccounts(), date, reporting));
        return data;
    }

    private static List<Row> rows(Block block, LocalDate date, boolean includeZero)
    {
        var rows = new ArrayList<>(block.snapshot().getPositions().stream().map(Row::of).toList());

        if (includeZero)
        {
            var held = rows.stream().map(Row::security).collect(Collectors.toSet());
            block.scope().stream().flatMap(portfolio -> portfolio.getTransactions().stream())
                            .filter(t -> !t.getDateTime().toLocalDate().isAfter(date)).map(t -> t.getSecurity())
                            .distinct().filter(security -> !held.contains(security))
                            .forEach(security -> rows.add(Row.soldOut(security)));
        }

        rows.sort(MasterData.byNameThenUuid(row -> row.security().getName(), row -> row.security().getUUID()));
        return rows;
    }

    private static Map<String, Object> render(Block block, List<Row> rows, LocalDate date,
                    ReportingCurrency reporting)
    {
        var json = new LinkedHashMap<String, Object>();
        block.account().ifPresent(account -> {
            var reference = new LinkedHashMap<String, Object>();
            reference.put("uuid", account.getUUID());
            reference.put("name", MasterData.orEmpty(account.getName()));
            json.put("investmentAccount", reference);
        });

        var figures = new ArrayList<ReportingCurrency.Figure>();
        var items = new ArrayList<Map<String, Object>>();
        for (var row : rows)
        {
            var figure = reporting.figure(date, row.value());
            figures.add(figure);
            items.add(render(row, figure, reporting));
        }
        json.put("items", items);
        reporting.total(figures).ifPresent(total -> json.put("total", total));
        return json;
    }

    private static Map<String, Object> render(Row row, ReportingCurrency.Figure figure, ReportingCurrency reporting)
    {
        var security = row.security();

        var json = new LinkedHashMap<String, Object>();
        json.put("instrument", security.getUUID());
        json.put("name", MasterData.orEmpty(security.getName()));
        json.put("shares", Decimals.of(row.shares(), Values.Share));
        row.price().ifPresent(price -> {
            var priceJson = new LinkedHashMap<String, Object>();
            priceJson.put("date", price.getDate().toString());
            priceJson.put("value", Decimals.of(price.getValue(), Values.Quote));
            json.put("price", priceJson);
        });
        json.put("amount", Decimals.amount(row.value()));
        json.put("currencyCode", row.value().getCurrencyCode());
        reporting.putConvertedInto(json, figure);
        return json;
    }

    private static Map<String, Object> renderCash(List<AccountSnapshot> snapshots, LocalDate date,
                    ReportingCurrency reporting)
    {
        var sorted = snapshots.stream().sorted(MasterData.byNameThenUuid((AccountSnapshot s) -> s.getAccount().getName(),
                        s -> s.getAccount().getUUID())).toList();

        var figures = new ArrayList<ReportingCurrency.Figure>();
        var items = new ArrayList<Map<String, Object>>();
        for (var snapshot : sorted)
        {
            var account = snapshot.getAccount();
            var funds = snapshot.getUnconvertedFunds();
            var figure = reporting.figure(date, funds);
            figures.add(figure);

            var json = new LinkedHashMap<String, Object>();
            json.put("cashAccount", account.getUUID());
            json.put("name", MasterData.orEmpty(account.getName()));
            json.put("amount", Decimals.amount(funds));
            json.put("currencyCode", funds.getCurrencyCode());
            reporting.putConvertedInto(json, figure);
            items.add(json);
        }

        var cash = new LinkedHashMap<String, Object>();
        cash.put("items", items);
        reporting.total(figures).ifPresent(total -> cash.put("total", total));
        return cash;
    }
}
