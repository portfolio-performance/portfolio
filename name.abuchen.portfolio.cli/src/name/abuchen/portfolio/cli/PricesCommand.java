package name.abuchen.portfolio.cli;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.money.Values;

final class PricesCommand
{
    private static final OptionSpec INSTRUMENT = new OptionSpec("instrument", "ref",
                    "Instrument to read.")
                                    .asRequired();

    private static final OptionSpec FROM = OptionSpec.date("from",
                    "First day of the history, inclusive. Without it the history has no lower bound.");

    private static final OptionSpec TO = OptionSpec.date("to",
                    "Last day of the history, inclusive. Defaults to today.");

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

    private static final Schema META = DateRange.declareIn(ResponseMeta.pagedSchema(SORT));

    static final CommandSpec SPEC = new CommandSpec("prices",
                    "Price history of one instrument by date, ascending unless --order desc, within an optional date range, with its latest price.",
                    true, List.of(INSTRUMENT, FROM, TO, Paging.LIMIT, Paging.OFFSET, SORT.sortOption(), Sorting.ORDER), DATA, META,
                    List.of(Prices.STALE_PRICE), PricesCommand::execute);

    private PricesCommand()
    {
    }

    private static Response execute(CommandContext context) throws CliException
    {
        var paging = Paging.parse(context);
        var sorting = SORT.parse(context);
        var range = DateRange.parse(context, FROM, TO);
        var asOf = LocalDate.now(context.clock());

        var client = context.loadClient().client();
        var security = References.instrument(client, INSTRUMENT.name(), context.text(INSTRUMENT).orElseThrow());

        var history = security.getPricesIncludingLatest().stream().filter(price -> range.contains(price.getDate()))
                        .toList();
        var page = paging.of(sorting.sort(client, history));
        var latest = Prices.latest(security, asOf);

        var data = new LinkedHashMap<String, Object>();
        data.put("instrument", instrument(security));
        latest.ifPresent(price -> data.put("latest", render(price)));
        page.putInto(data, PricesCommand::render);

        var meta = ResponseMeta.paged(context, client, paging, sorting);
        range.putInto(meta);

        var response = Response.of(data, meta);
        latest.flatMap(price -> Prices.staleWarning(security, price, asOf)).ifPresent(response::warn);
        return response;
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
