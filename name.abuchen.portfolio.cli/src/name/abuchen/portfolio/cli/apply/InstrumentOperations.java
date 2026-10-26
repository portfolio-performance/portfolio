package name.abuchen.portfolio.cli.apply;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.util.Isin;

/** The write operations on instruments. Fields are named as in the <code>instruments</code> output. */
public final class InstrumentOperations
{
    private static final EntityOperations<Security> OPERATIONS = new EntityOperations<>(
                    new EntityOperations.Definition<Security>("instrument", //
                                    "Adds an instrument. Only the name is required.",
                                    "Updates an instrument by UUID. Only the fields present change; null or an empty string clears an optional field. The currency can't change once the instrument has transactions.",
                                    List.of(EntityOperations.Field.required("name", "Instrument name; never empty.",
                                                    "a name", Security::getName, Security::setName),
                                                    EntityOperations.Field.optional("currencyCode",
                                                                    "ISO currency code; omit for an index.",
                                                                    Security::getCurrencyCode,
                                                                    Security::setCurrencyCode),
                                                    EntityOperations.Field.optional("isin",
                                                                    "ISIN: valid and not used by another instrument.",
                                                                    Security::getIsin, Security::setIsin),
                                                    EntityOperations.Field.optional("tickerSymbol", "Ticker symbol.",
                                                                    Security::getTickerSymbol,
                                                                    Security::setTickerSymbol),
                                                    EntityOperations.Field.optional("wkn", "WKN.", Security::getWkn,
                                                                    Security::setWkn),
                                                    EntityOperations.Field.optional("note", "Note.",
                                                                    Security::getNote, Security::setNote),
                                                    EntityOperations.Field.retired("instrument", Security::isRetired,
                                                                    Security::setRetired)),
                                    Client::getSecurities, Security::new, Client::addSecurity, Security::getUUID,
                                    InstrumentOperations::validate,
                                    // setName leaves it unset, and the binary format cannot store an instrument without it
                                    security -> security.setUpdatedAt(Instant.now())));

    static final OperationSpec ADD = OPERATIONS.addOperation();
    static final OperationSpec UPDATE = OPERATIONS.updateOperation();
    public static final List<OperationSpec> ALL = List.of(ADD, UPDATE);

    private InstrumentOperations()
    {
    }

    private static void validate(OperationContext context, Map<String, Object> values, Security existing)
    {
        EntityOperations.validateCurrency(context, values);

        if (existing != null)
            EntityOperations.validateCurrencyKept(context, "instrument", values, existing.getCurrencyCode(),
                            () -> hasTransactions(context.client(), existing));

        if (values.get("isin") instanceof String isin)
        {
            if (!Isin.isValid(isin) || !isin.matches(Isin.PATTERN))
                context.fail("isin", "invalid-isin", "'" + isin + "' is not a valid ISIN.", "Isin.isValid");
            else
                context.client().getSecurities().stream()
                                .filter(other -> other != existing && isin.equalsIgnoreCase(other.getIsin()))
                                .findFirst().ifPresent(other -> context.fail("isin", "duplicate-isin",
                                                "The ISIN is already used by instrument " + other.getUUID() + "."));
        }
    }

    private static boolean hasTransactions(Client client, Security security)
    {
        return client.getAccounts().stream().flatMap(a -> a.getTransactions().stream())
                        .anyMatch(t -> t.getSecurity() == security)
                        || client.getPortfolios().stream().flatMap(p -> p.getTransactions().stream())
                                        .anyMatch(t -> t.getSecurity() == security);
    }
}
