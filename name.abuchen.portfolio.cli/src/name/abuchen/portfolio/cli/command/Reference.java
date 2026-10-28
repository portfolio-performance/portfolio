package name.abuchen.portfolio.cli.command;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

import name.abuchen.portfolio.model.Client;

/**
 * What identifies one kind of entity that an agent can name on a read, and
 * how an ambiguity lists it. A UUID is canonical and wins immediately.
 * Otherwise every lookup key is matched, pooled with no precedence among them.
 * The lookup never guesses: one entity resolves, none is not found and several
 * are ambiguous. Every comparison is case-insensitive and ordinal, like the
 * list ordering, and retired entities take part. Each kind searches only its
 * own entities.
 */
public interface Reference<T>
{
    /** A field a reference can match or a candidate lists, named as in the list output. */
    record Key<T>(String name, Function<T, String> value)
    {
    }

    /** How failures name the kind, such as <code>cash account</code>. */
    String noun();

    /** All entities of the kind in the client, in any order. */
    List<T> entities(Client client);

    String uuid(T entity);

    /** The name; null counts as empty. */
    String name(T entity);

    /** The fields matched after the UUID. */
    List<Key<T>> lookupKeys();

    /** Whether an entity is retired; empty if the kind is never retired, and its candidates carry no flag. */
    Optional<Predicate<T>> retired();

    /** The kind-specific fields a candidate lists if they are set. */
    List<Key<T>> candidateDetails();

    static <T> Reference<T> of(String noun, Function<Client, List<T>> entities, Function<T, String> uuid,
                    Function<T, String> name, List<Key<T>> lookupKeys, Optional<Predicate<T>> retired,
                    List<Key<T>> candidateDetails)
    {
        record Declared<T>(String noun, Function<Client, List<T>> all, Function<T, String> uuidOf,
                        Function<T, String> nameOf, List<Key<T>> lookupKeys, Optional<Predicate<T>> retired,
                        List<Key<T>> candidateDetails) implements Reference<T>
        {
            @Override
            public List<T> entities(Client client)
            {
                return all.apply(client);
            }

            @Override
            public String uuid(T entity)
            {
                return uuidOf.apply(entity);
            }

            @Override
            public String name(T entity)
            {
                return nameOf.apply(entity);
            }
        }

        return new Declared<>(noun, entities, uuid, name, List.copyOf(lookupKeys), retired,
                        List.copyOf(candidateDetails));
    }

    /**
     * The one entity that <code>value</code>, given for <code>option</code>,
     * names.
     *
     * @throws CliException
     *             a usage error if the value is blank, or a failure if no
     *             entity or several match it
     */
    default T resolve(Client client, String option, String value) throws CliException
    {
        if (value.isBlank())
            throw CliException.usage(ErrorCode.INVALID_OPTION_VALUE, "Invalid option value",
                            "Option '--" + option + "' needs a reference, not a blank value.")
                            .with("option", option).with("value", value);

        var entities = entities(client);

        for (var entity : entities)
        {
            if (value.equalsIgnoreCase(uuid(entity)))
                return entity;
        }

        var matches = new ArrayList<T>();
        for (var entity : entities)
        {
            if (!matchedBy(entity, value).isEmpty())
                matches.add(entity);
        }

        if (matches.size() == 1)
            return matches.get(0);

        if (matches.isEmpty())
            throw CliException.failure(ErrorCode.REFERENCE_NOT_FOUND, "Reference not found",
                            "No " + noun() + " matches '" + value + "'.", 404).with("option", option)
                            .with("value", value);

        var candidates = matches.stream().sorted(MasterData.byNameThenUuid(this::name, this::uuid))
                        .map(m -> candidate(m, value)).toList();
        throw CliException.failure(ErrorCode.REFERENCE_AMBIGUOUS, "Reference ambiguous", "'" + value + "' matches "
                        + candidates.size() + " " + noun() + "s. Use one of the candidate UUIDs.", 409)
                        .with("option", option).with("value", value).with("candidates", candidates);
    }

    private List<String> matchedBy(T entity, String value)
    {
        return lookupKeys().stream().filter(key -> value.equalsIgnoreCase(key.value().apply(entity)))
                        .map(Key::name).toList();
    }

    private Map<String, Object> candidate(T entity, String value)
    {
        var json = new LinkedHashMap<String, Object>();
        json.put("uuid", uuid(entity));
        json.put("name", MasterData.orEmpty(name(entity)));
        retired().ifPresent(retired -> json.put("retired", retired.test(entity)));
        json.put("matchedBy", matchedBy(entity, value));
        for (var detail : candidateDetails())
            MasterData.putIfSet(json, detail.name(), detail.value().apply(entity));
        return json;
    }
}
