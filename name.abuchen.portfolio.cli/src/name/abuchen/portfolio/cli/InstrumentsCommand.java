package name.abuchen.portfolio.cli;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import name.abuchen.portfolio.model.AttributeType;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;

final class InstrumentsCommand
{
    private static final Schema ATTRIBUTE = Schema.object("A user-defined attribute that has a value.")
                    .property("id", Schema.string("Attribute type id."))
                    .property("name", Schema.string("Attribute type name."))
                    .property("type", Schema.enumOf("How to read the value.", AttributeCodec.TYPES))
                    .property("value", Schema.oneOfTypes(
                                    "The value: a string, a boolean, an ISO date string, a decimal number (amount, quote, shares), a fraction (percent, 0.0022 means 0.22%) or an unscaled number.",
                                    "string", "number", "boolean"));

    private static final Schema ITEM = Schema.object("An instrument: a security, index or exchange rate.")
                    .property("uuid", Schema.string("Canonical UUID."))
                    .property("name", Schema.string("Instrument name; empty if the file has none."))
                    .optionalProperty("currencyCode", Schema.string("ISO currency code; absent for indices."))
                    .optionalProperty("isin", Schema.string("ISIN, if set."))
                    .optionalProperty("wkn", Schema.string("WKN, if set."))
                    .optionalProperty("tickerSymbol", Schema.string("Ticker symbol, if set."))
                    .optionalProperty("note", Schema.string("Note, if set."))
                    .property("retired", Schema.bool("Whether the instrument is retired."))
                    .property("attributes", Schema.array("User-defined attributes that have a value.", ATTRIBUTE))
                    .optionalProperty("updatedAt", MasterData.UPDATED_AT);

    static final MasterDataList.Source<Security> SOURCE = new MasterDataList.Source<Security>(Client::getSecurities,
                    Security::getUUID, Security::getName, InstrumentsCommand::renderer);

    static final CommandSpec SPEC = MasterDataList.command("instruments",
                    "Instruments (securities, indices, exchange rates): UUID, name, currency, ISIN, ticker, WKN, note, retired flag and attributes",
                    ITEM,
                    List.of(TableColumn.of("uuid", "UUID"), TableColumn.capped("name", "Name", 40),
                                    TableColumn.of("currencyCode", "Currency"), TableColumn.of("isin", "ISIN"),
                                    TableColumn.of("wkn", "WKN"), TableColumn.of("tickerSymbol", "Ticker"),
                                    TableColumn.of("retired", "Retired")),
                    SOURCE, Security::getUpdatedAt);

    private InstrumentsCommand()
    {
    }

    private static Function<Security, Map<String, Object>> renderer(Client client)
    {
        var attributeTypes = client.getSettings().getAttributeTypes().filter(t -> t.supports(Security.class))
                        .toList();
        return security -> render(security, attributeTypes);
    }

    private static Map<String, Object> render(Security security, List<AttributeType> attributeTypes)
    {
        var json = new LinkedHashMap<String, Object>();
        json.put("uuid", security.getUUID());
        json.put("name", MasterData.orEmpty(security.getName()));
        MasterData.putIfSet(json, "currencyCode", security.getCurrencyCode());
        MasterData.putIfSet(json, "isin", security.getIsin());
        MasterData.putIfSet(json, "wkn", security.getWkn());
        MasterData.putIfSet(json, "tickerSymbol", security.getTickerSymbol());
        MasterData.putIfSet(json, "note", security.getNote());
        json.put("retired", security.isRetired());
        json.put("attributes", attributes(security, attributeTypes));
        MasterData.putUpdatedAt(json, security.getUpdatedAt());
        return json;
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
