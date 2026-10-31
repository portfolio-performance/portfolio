package name.abuchen.portfolio.cli.command;

import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

/** Facets that narrow what a list command returns. */
public final class Filters
{
    /**
     * What a reference filter resolved to: the entity, or empty if the option
     * is absent. An absent filter matches every item.
     */
    public record Reference<E, R>(Optional<E> entity, BiPredicate<E, R> matches) implements Predicate<R>
    {
        @Override
        public boolean test(R item)
        {
            return entity.map(e -> matches.test(e, item)).orElse(true);
        }
    }

    private Filters()
    {
    }

    /**
     * A filter by one entity a reference option names. If it is given,
     * <code>meta</code> gets the UUID it resolved to.
     *
     * @param matches
     *            whether an item belongs to the entity
     */
    public static <E, R> Facet<Reference<E, R>> reference(ReferenceOption<E> option, String metaKey,
                    String metaDescription, BiPredicate<E, R> matches)
    {
        var declaration = Declaration.empty().options(option.spec()).optionalMeta(metaKey,
                        Schema.string(metaDescription));
        return Facet.of(declaration, run -> new Reference<>(run.reference(option, run.client()), matches),
                        (filter, out) -> filter.entity()
                                        .ifPresent(entity -> out.meta(metaKey, option.kind().uuid(entity))));
    }
}
