package name.abuchen.portfolio.cli.query;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import name.abuchen.portfolio.cli.command.CliException;
import name.abuchen.portfolio.cli.command.ClientFile;
import name.abuchen.portfolio.cli.command.CommandContext;
import name.abuchen.portfolio.cli.command.CommandSpec;
import name.abuchen.portfolio.cli.command.DateRange;
import name.abuchen.portfolio.cli.command.Decimals;
import name.abuchen.portfolio.cli.command.Declaration;
import name.abuchen.portfolio.cli.command.Facet;
import name.abuchen.portfolio.cli.command.MasterData;
import name.abuchen.portfolio.cli.command.MinorUnits;
import name.abuchen.portfolio.cli.command.Paging;
import name.abuchen.portfolio.cli.command.ReferenceOption;
import name.abuchen.portfolio.cli.command.Schema;
import name.abuchen.portfolio.cli.command.Sorting;
import name.abuchen.portfolio.cli.currency.Prices;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.money.Values;

public final class PricesCommand
{
    private static final Facet<Optional<Security>> INSTRUMENT = Facet
                    .option(ReferenceOption.instrument("instrument", "Instrument to read.").asRequired());

    private static final Facet<DateRange> RANGE = DateRange.facet(
                    "First day of the history, inclusive. Without it the history has no lower bound.",
                    "Last day of the history, inclusive. Defaults to today.");

    /** The handler warns of a stale latest price. */
    private static final Facet<Void> STALE_PRICE = Facet.of(Declaration.empty().warnings(Prices.STALE_PRICE),
                    run -> null);

    private static Schema price(String description)
    {
        return Schema.object(description).property("date", Schema.string("ISO 8601 date of the price."))
                        .property("value", Schema.number("Price per share as a decimal, in the currency of the instrument."
                                        + MinorUnits.CONVERTED_BY_HOLDINGS));
    }

    private static final Schema DATA = Paging.pageSchema("The price history of one instrument.", price("A price on a day."))
                    .property("instrument", Schema.object("The instrument the reference resolved to.")
                                    .property("uuid", Schema.string("Canonical UUID."))
                                    .property("name", Schema.string("Instrument name; empty if the file has none."))
                                    .optionalProperty("currencyCode",
                                                    Schema.string("ISO currency code; absent for indices."
                                                                    + MinorUnits.CONVERTED_BY_HOLDINGS)))
                    .optionalProperty("latest", price(
                                    "The price valuation uses today: the feed's latest price when it is current, otherwise the last stored price. Absent if no price is dated on or before today. Independent of from and to."));

    private static final Sorting.Spec<SecurityPrice> SORT = Sorting.Spec.of(price -> price.getDate().toString(),
                    "date", Sorting.Field.of("date", Comparator.comparing(SecurityPrice::getDate)));

    public static final CommandSpec SPEC = CommandSpec.of("prices",
                    "Price history of one instrument by date, ascending unless --order desc, within an optional date range, with its latest price.",
                    DATA, PricesCommand::execute, ClientFile.READ, INSTRUMENT, RANGE, Paging.FACET, SORT, STALE_PRICE);

    private PricesCommand()
    {
    }

    private static Map<String, Object> execute(CommandContext run) throws CliException
    {
        var client = run.client();
        var security = run.get(INSTRUMENT).orElseThrow();
        var range = run.get(RANGE);
        var asOf = run.get(ClientFile.READ).asOf();

        var history = security.getPricesIncludingLatest().stream().filter(price -> range.contains(price.getDate()))
                        .toList();
        var page = run.get(Paging.FACET).of(run.get(SORT).sort(client, history));
        var latest = Prices.latest(security, asOf);

        var data = new LinkedHashMap<String, Object>();
        data.put("instrument", instrument(security));
        latest.ifPresent(price -> data.put("latest", render(price)));
        page.putInto(data, PricesCommand::render);

        latest.flatMap(price -> Prices.staleWarning(security, price, asOf)).ifPresent(run::warn);
        return data;
    }

    private static Map<String, Object> instrument(Security security)
    {
        var json = new LinkedHashMap<String, Object>();
        json.put("uuid", security.getUUID());
        json.put("name", MasterData.orEmpty(security.getName()));
        MasterData.putIfSet(json, "currencyCode", security.getCurrencyCode());
        return json;
    }

    private static Map<String, Object> render(SecurityPrice price)
    {
        var json = new LinkedHashMap<String, Object>();
        json.put("date", price.getDate().toString());
        json.put("value", Decimals.of(price.getValue(), Values.Quote));
        return json;
    }
}
