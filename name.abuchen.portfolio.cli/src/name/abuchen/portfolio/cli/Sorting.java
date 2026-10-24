package name.abuchen.portfolio.cli;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import name.abuchen.portfolio.model.Client;

/**
 * The order requested by <code>--sort</code> and <code>--order</code> on a paged
 * list. A command declares its sort fields in a {@link Spec}; the options, the
 * tie-break by id, the reversal, the validation and the <code>meta</code> come
 * from here, so paging stays stable under every field and direction.
 */
record Sorting<T>(Spec<T> spec, String field, boolean descending)
{
    static final String ASCENDING = "asc";
    static final String DESCENDING = "desc";

    static final OptionSpec ORDER = OptionSpec.choice("order", "asc|desc",
                    "Direction of the sort. desc reverses the whole order, ties by id included.",
                    List.of(ASCENDING, DESCENDING), ASCENDING);

    /** One field a list can be sorted by; the comparator needs no tie-break. */
    record Field<T>(String name, Function<Client, Comparator<T>> comparator)
    {
        static <T> Field<T> of(String name, Comparator<T> comparator)
        {
            return new Field<>(name, client -> comparator);
        }
    }

    /**
     * The sort fields of one list command.
     *
     * @param id
     *            the tie-break of every field; null counts as empty
     * @param defaultField
     *            the field of a call without <code>--sort</code>
     */
    record Spec<T>(Function<T, String> id, String defaultField, List<Field<T>> fields)
    {
        @SafeVarargs
        static <T> Spec<T> of(Function<T, String> id, String defaultField, Field<T>... fields)
        {
            return new Spec<>(id, defaultField, Arrays.asList(fields));
        }

        List<String> names()
        {
            return fields.stream().map(Field::name).toList();
        }

        OptionSpec sortOption()
        {
            return OptionSpec.choice("sort", "field", "Field to sort by.", names(), defaultField);
        }

        /** The sort and order options, to declare on a list command. */
        List<OptionSpec> options()
        {
            return List.of(sortOption(), ORDER);
        }

        Sorting<T> parse(CommandContext context) throws CliException
        {
            var field = sortOption().parseChoice(context.option("sort"));
            var order = ORDER.parseChoice(context.option(ORDER.name()));
            return new Sorting<>(this, field, DESCENDING.equals(order));
        }
    }

    /** Field, then id; the whole comparator is reversed for <code>desc</code>. */
    Comparator<T> comparator(Client client)
    {
        var chosen = spec.fields().stream().filter(candidate -> candidate.name().equals(field)).findFirst()
                        .orElseThrow();
        Comparator<T> ordering = chosen.comparator().apply(client)
                        .thenComparing(entity -> MasterData.orEmpty(spec.id().apply(entity)));
        return descending ? ordering.reversed() : ordering;
    }

    List<T> sort(Client client, Collection<T> entities)
    {
        return entities.stream().sorted(comparator(client)).toList();
    }

    /** Adds the resolved <code>sort</code> and <code>order</code>. */
    void putInto(Map<String, Object> meta)
    {
        meta.put("sort", field);
        meta.put("order", descending ? DESCENDING : ASCENDING);
    }

    /** Declares what {@link #putInto} adds. */
    static Schema declareIn(Schema meta, Spec<?> spec)
    {
        return meta.property("sort", Schema.enumOf("Field the list is sorted by.", spec.names())).property("order",
                        Schema.enumOf("Direction of the sort: asc or desc.", List.of(ASCENDING, DESCENDING)));
    }

    /** Orders by an optional instant; empty sorts before every instant. */
    static <T> Comparator<T> byInstant(Function<T, Optional<Instant>> instant)
    {
        return Comparator.comparing(instant, Comparator.comparing((Optional<Instant> value) -> value.isPresent())
                        .thenComparing(value -> value.orElse(Instant.EPOCH)));
    }

    /** The instant as published: to the second. */
    static Optional<Instant> lastModified(Instant instant)
    {
        return Optional.ofNullable(instant).map(value -> value.truncatedTo(ChronoUnit.SECONDS));
    }
}
