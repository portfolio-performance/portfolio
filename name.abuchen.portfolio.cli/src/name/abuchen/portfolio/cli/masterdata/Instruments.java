package name.abuchen.portfolio.cli.masterdata;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import name.abuchen.portfolio.cli.command.MasterData;
import name.abuchen.portfolio.cli.command.MinorUnits;
import name.abuchen.portfolio.cli.command.Schema;
import name.abuchen.portfolio.cli.command.TableColumn;
import name.abuchen.portfolio.model.AttributeType;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;

/** The instrument kind: securities, indices and exchange rates. */
public final class Instruments
{
    private static final Schema ATTRIBUTE = Schema.object("A user-defined attribute that has a value.")
                    .property("id", Schema.string("Attribute type id."))
                    .property("name", Schema.string("Attribute type name."))
                    .property("type", Schema.enumOf("How to read the value.", AttributeCodec.TYPES))
                    .property("value", Schema.oneOfTypes(
                                    "The value: a string, a boolean, an ISO date string, a decimal number (amount, quote, shares), a fraction (percent, 0.0022 means 0.22%) or an unscaled number.",
                                    "string", "number", "boolean"));

    public static final MasterDataKind<Security> INSTRUMENTS = MasterDataKind
                    .declare("instrument", "instrument", Client::getSecurities, Security::getUUID, Security::getName)
                    .item("An instrument: a security, index or exchange rate.",
                                    "Instrument name; empty if the file has none.")
                    .ifSet("currencyCode",
                                    Schema.string("ISO currency code; absent for indices."
                                                    + MinorUnits.CONVERTED_BY_HOLDINGS),
                                    Security::getCurrencyCode)
                    .ifSet("isin", Schema.string("ISIN, if set."), Security::getIsin)
                    .ifSet("wkn", Schema.string("WKN, if set."), Security::getWkn)
                    .ifSet("tickerSymbol", Schema.string("Ticker symbol, if set."), Security::getTickerSymbol)
                    .ifSet("note", Schema.string("Note, if set."), Security::getNote)
                    .retired("Whether the instrument is retired.", Security::isRetired)
                    .alwaysPrepared("attributes", Schema.array("User-defined attributes that have a value.", ATTRIBUTE),
                                    Instruments::attributes)
                    .updatedAt(Security::getUpdatedAt)
                    .lookupKeys("isin", "tickerSymbol", "name")
                    .candidateDetails("isin", "tickerSymbol", "currencyCode")
                    .list("Instruments (securities, indices, exchange rates): UUID, name, currency, ISIN, ticker, WKN, note, retired flag and attributes",
                                    List.of(TableColumn.of("uuid", "UUID"), TableColumn.capped("name", "Name", 40),
                                                    TableColumn.of("currencyCode", "Currency"),
                                                    TableColumn.of("isin", "ISIN"), TableColumn.of("wkn", "WKN"),
                                                    TableColumn.of("tickerSymbol", "Ticker"),
                                                    TableColumn.of("retired", "Retired")));

    private Instruments()
    {
    }

    /** The attribute types are looked up once per client. */
    private static Function<Security, List<Map<String, Object>>> attributes(Client client)
    {
        var attributeTypes = client.getSettings().getAttributeTypes().filter(t -> t.supports(Security.class))
                        .toList();
        return security -> attributes(security, attributeTypes);
    }

    private static List<Map<String, Object>> attributes(Security security, List<AttributeType> attributeTypes)
    {
        var answer = new ArrayList<Map<String, Object>>();

        for (var type : attributeTypes)
        {
            AttributeCodec.encode(type, security.getAttributes().get(type)).ifPresent(encoded -> {
                var attribute = new LinkedHashMap<String, Object>();
                attribute.put("id", type.getId());
                attribute.put("name", MasterData.orEmpty(type.getName()));
                attribute.put("type", encoded.type().id());
                attribute.put("value", encoded.value());
                answer.add(attribute);
            });
        }

        return answer;
    }
}
