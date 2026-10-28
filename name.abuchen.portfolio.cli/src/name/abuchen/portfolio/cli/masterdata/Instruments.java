package name.abuchen.portfolio.cli.masterdata;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import name.abuchen.portfolio.cli.command.ErrorCode;
import name.abuchen.portfolio.cli.command.MasterData;
import name.abuchen.portfolio.cli.command.MinorUnits;
import name.abuchen.portfolio.cli.command.Schema;
import name.abuchen.portfolio.cli.command.TableColumn;
import name.abuchen.portfolio.model.AttributeType;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.util.Isin;

/** The instrument kind: securities, indices and exchange rates. */
public final class Instruments
{
    private static final String CURRENCY = "currencyCode";
    private static final String ISIN = "isin";

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
                    .write(Writer.required("Instrument name; never empty.", "a name", Security::setName))
                    .ifSet(CURRENCY,
                                    Schema.string("ISO currency code; absent for indices."
                                                    + MinorUnits.CONVERTED_BY_HOLDINGS),
                                    Security::getCurrencyCode)
                    .write(Writer.optional("ISO currency code; omit for an index.", Security::setCurrencyCode)
                                    .check(Checks::isCurrency, ErrorCode.INVALID_CURRENCY, Checks::notACurrency, null))
                    .ifSet(ISIN, Schema.string("ISIN, if set."), Security::getIsin)
                    .write(Writer.optional("ISIN: valid and not used by another instrument.", Security::setIsin)
                                    .check(isin -> Isin.isValid(isin) && isin.matches(Isin.PATTERN), ErrorCode.INVALID_ISIN,
                                                    isin -> "'" + isin + "' is not a valid ISIN.", "Isin.isValid"))
                    .ifSet("tickerSymbol", Schema.string("Ticker symbol, if set."), Security::getTickerSymbol)
                    .write(Writer.optional("Ticker symbol.", Security::setTickerSymbol))
                    .ifSet("wkn", Schema.string("WKN, if set."), Security::getWkn)
                    .write(Writer.optional("WKN.", Security::setWkn))
                    .ifSet("note", Schema.string("Note, if set."), Security::getNote)
                    .write(Writer.optional("Note.", Security::setNote))
                    .retired("Whether the instrument is retired.", Security::isRetired)
                    .write(Writer.flag("Whether the instrument is retired", Security::setRetired))
                    .alwaysPrepared("attributes", Schema.array("User-defined attributes that have a value.", ATTRIBUTE),
                                    Instruments::attributes)
                    .updatedAt(Security::getUpdatedAt)
                    .lookupKeys(ISIN, "tickerSymbol", "name")
                    .candidateDetails(ISIN, "tickerSymbol", CURRENCY)
                    // setName leaves it unset, and the binary format cannot store an instrument without it
                    .creates(Security::new, Client::addSecurity, security -> security.setUpdatedAt(Instant.now()))
                    .validator(Instruments::validate, ErrorCode.CURRENCY_IMMUTABLE, ErrorCode.DUPLICATE_ISIN)
                    .operations("Adds an instrument. Only the name is required.",
                                    "Updates an instrument by UUID. Only the fields present change; null or an empty string clears an optional field. The currency can't change once the instrument has transactions.")
                    .list("Instruments (securities, indices, exchange rates): UUID, name, currency, ISIN, ticker, WKN, note, retired flag and attributes",
                                    List.of(TableColumn.of("uuid", "UUID"), TableColumn.capped("name", "Name", 40),
                                                    TableColumn.of("currencyCode", "Currency"),
                                                    TableColumn.of("isin", "ISIN"), TableColumn.of("wkn", "WKN"),
                                                    TableColumn.of("tickerSymbol", "Ticker"),
                                                    TableColumn.of("retired", "Retired")));

    private Instruments()
    {
    }

    /** The currency is kept once there are transactions, and an ISIN names one instrument only. */
    private static void validate(Writes.Validation<Security> validation)
    {
        var existing = validation.existing();
        var client = validation.client();

        if (existing != null)
            Checks.currencyKept(validation, "instrument", existing.getCurrencyCode(),
                            () -> hasTransactions(client, existing));

        if (validation.values().get(ISIN) instanceof String isin)
            client.getSecurities().stream()
                            .filter(other -> other != existing && isin.equalsIgnoreCase(other.getIsin()))
                            .findFirst().ifPresent(other -> validation.fail(ISIN, ErrorCode.DUPLICATE_ISIN,
                                            "The ISIN is already used by instrument " + other.getUUID() + "."));
    }

    private static boolean hasTransactions(Client client, Security security)
    {
        return client.getAccounts().stream().flatMap(a -> a.getTransactions().stream())
                        .anyMatch(t -> t.getSecurity() == security)
                        || client.getPortfolios().stream().flatMap(p -> p.getTransactions().stream())
                                        .anyMatch(t -> t.getSecurity() == security);
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
