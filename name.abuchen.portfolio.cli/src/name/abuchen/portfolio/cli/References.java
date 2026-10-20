package name.abuchen.portfolio.cli;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Taxonomy;

/**
 * Resolves what an agent typed on a read to the one entity it means. A UUID is
 * canonical and wins immediately. Otherwise instruments are found by ISIN,
 * ticker or name, and accounts by name; all matching fields are pooled with no
 * precedence among them. The resolver never guesses: one entity resolves, none
 * is not found and several are ambiguous. Every comparison is case-insensitive
 * and ordinal, like the list ordering, and retired entities take part. Each
 * kind searches only its own entities.
 */
final class References
{
    /** A field a reference can match, named as in the list output. */
    private record Field<T>(String id, Function<T, String> value)
    {
    }

    /**
     * What identifies one kind of entity and how an ambiguity lists it.
     *
     * @param details
     *            adds the kind-specific fields of a candidate
     */
    private record Kind<T>(MasterDataList.Source<T> source, String label, List<Field<T>> fields,
                    Predicate<T> retired, BiConsumer<T, Map<String, Object>> details)
    {
        Kind(MasterDataList.Source<T> source, String label, List<Field<T>> fields, Predicate<T> retired)
        {
            this(source, label, fields, retired, (entity, json) -> {
            });
        }

        String uuid(T entity)
        {
            return source.uuid().apply(entity);
        }

        String name(T entity)
        {
            return source.name().apply(entity);
        }
    }

    private static final Kind<Security> INSTRUMENT = new Kind<>(InstrumentsCommand.SOURCE, "instrument",
                    List.of(new Field<>("isin", Security::getIsin),
                                    new Field<>("tickerSymbol", Security::getTickerSymbol),
                                    new Field<>("name", Security::getName)),
                    Security::isRetired, (security, json) -> {
                        MasterData.putIfSet(json, "isin", security.getIsin());
                        MasterData.putIfSet(json, "tickerSymbol", security.getTickerSymbol());
                        MasterData.putIfSet(json, "currencyCode", security.getCurrencyCode());
                    });

    private static final Kind<Portfolio> INVESTMENT_ACCOUNT = new Kind<>(InvestmentAccountsCommand.SOURCE,
                    "investment account", List.of(new Field<>("name", Portfolio::getName)), Portfolio::isRetired);

    private static final Kind<Account> CASH_ACCOUNT = new Kind<>(CashAccountsCommand.SOURCE, "cash account",
                    List.of(new Field<>("name", Account::getName)), Account::isRetired,
                    (account, json) -> MasterData.putIfSet(json, "currencyCode", account.getCurrencyCode()));

    /** Taxonomies are never retired, so their candidates carry no such flag. */
    private static final Kind<Taxonomy> TAXONOMY = new Kind<>(TaxonomiesCommand.SOURCE, "taxonomy",
                    List.of(new Field<>("name", Taxonomy::getName)), null);

    private References()
    {
    }

    /** By UUID, ISIN, ticker or name. */
    static Security instrument(Client client, String option, String value) throws CliException
    {
        return resolve(INSTRUMENT, client, option, value);
    }

    /** By UUID or name. */
    static Portfolio investmentAccount(Client client, String option, String value) throws CliException
    {
        return resolve(INVESTMENT_ACCOUNT, client, option, value);
    }

    /** By UUID or name. */
    static Account cashAccount(Client client, String option, String value) throws CliException
    {
        return resolve(CASH_ACCOUNT, client, option, value);
    }

    /** By UUID or name. */
    static Taxonomy taxonomy(Client client, String option, String value) throws CliException
    {
        return resolve(TAXONOMY, client, option, value);
    }

    private static <T> T resolve(Kind<T> kind, Client client, String option, String value) throws CliException
    {
        if (value.isBlank())
            throw CliException.usage("invalid-option-value", "Invalid option value",
                            "Option '--" + option + "' needs a reference, not a blank value.")
                            .with("option", option).with("value", value);

        var entities = kind.source().entities().apply(client);

        for (var entity : entities)
        {
            if (value.equalsIgnoreCase(kind.uuid(entity)))
                return entity;
        }

        var matches = new ArrayList<T>();
        for (var entity : entities)
        {
            if (!matchedBy(kind, entity, value).isEmpty())
                matches.add(entity);
        }

        if (matches.size() == 1)
            return matches.get(0);

        if (matches.isEmpty())
            throw CliException.failure("reference-not-found", "Reference not found",
                            "No " + kind.label() + " matches '" + value + "'.", 404).with("option", option)
                            .with("value", value);

        var candidates = matches.stream().sorted(kind.source().ordering()).map(m -> candidate(kind, m, value))
                        .toList();
        throw CliException.failure("reference-ambiguous", "Reference ambiguous", "'" + value + "' matches "
                        + candidates.size() + " " + kind.label() + "s. Use one of the candidate UUIDs.", 409)
                        .with("option", option).with("value", value).with("candidates", candidates);
    }

    private static <T> List<String> matchedBy(Kind<T> kind, T entity, String value)
    {
        return kind.fields().stream().filter(field -> value.equalsIgnoreCase(field.value().apply(entity)))
                        .map(Field::id).toList();
    }

    private static <T> Map<String, Object> candidate(Kind<T> kind, T entity, String value)
    {
        var json = new LinkedHashMap<String, Object>();
        json.put("uuid", kind.uuid(entity));
        json.put("name", MasterData.orEmpty(kind.name(entity)));
        if (kind.retired() != null)
            json.put("retired", kind.retired().test(entity));
        json.put("matchedBy", matchedBy(kind, entity, value));
        kind.details().accept(entity, json);
        return json;
    }
}
