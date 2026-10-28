package name.abuchen.portfolio.cli.command;

import java.util.Optional;

/**
 * One concern of a command, such as a date range, paging or the client file:
 * it declares its options, meta members, warnings and error codes, resolves
 * its value from the command line before the handler runs, and reports its
 * meta and warnings after it. A command is composed of facets with
 * {@link CommandSpec#of}; its handler reads their values with
 * {@link CommandContext#get}.
 */
public interface Facet<T>
{
    /** Resolves the value of a facet; a rule violation is a {@link CliException}. */
    @FunctionalInterface
    interface Resolver<T>
    {
        T resolve(CommandContext run) throws CliException;
    }

    /** Reports the meta members and warnings of a resolved value. */
    @FunctionalInterface
    interface Reporter<T>
    {
        void report(T value, Report out);
    }

    /** Where a facet puts its meta members and warnings. */
    interface Report
    {
        /**
         * @throws IllegalStateException
         *             if the facet does not declare the member
         */
        void meta(String member, Object value);

        /**
         * @throws IllegalStateException
         *             if the facet does not declare the warning's code
         */
        void warn(Warning warning);
    }

    Declaration declaration();

    T resolve(CommandContext run) throws CliException;

    /** Runs after the handler, in facet order. */
    default void report(T value, Report out)
    {
    }

    /** The same facet with more sample arguments; see {@link Declaration#sample}. */
    default Facet<T> withSample(String... arguments)
    {
        return of(declaration().sample(arguments), this::resolve, this::report);
    }

    static <T> Facet<T> of(Declaration declaration, Resolver<T> resolver)
    {
        return of(declaration, resolver, (value, out) -> {
        });
    }

    static <T> Facet<T> of(Declaration declaration, Resolver<T> resolver, Reporter<T> reporter)
    {
        return new Facet<>()
        {
            @Override
            public Declaration declaration()
            {
                return declaration;
            }

            @Override
            public T resolve(CommandContext run) throws CliException
            {
                return resolver.resolve(run);
            }

            @Override
            public void report(T value, Report out)
            {
                reporter.report(value, out);
            }
        };
    }

    /**
     * A lone option. Its value is the parsed option: a <code>Boolean</code>
     * for a switch, an <code>Optional&lt;LocalDate&gt;</code> for a date, a
     * <code>Long</code> for an integer, a <code>String</code> for a choice, a
     * <code>List&lt;String&gt;</code> for a multi-choice and an
     * <code>Optional&lt;String&gt;</code> for a currency or free text. A reference
     * option goes through {@link #option(ReferenceOption)}.
     */
    @SuppressWarnings("unchecked")
    static <V> Facet<V> option(OptionSpec spec)
    {
        if (spec.type() instanceof OptionSpec.Reference)
            throw new IllegalArgumentException("Option '--" + spec.name() + "' is a reference: use ReferenceOption");

        return of(Declaration.empty().options(spec), run -> (V) spec.type().value(run, spec));
    }

    /** A lone reference option; its value is the entity it names, or empty if it is absent. */
    static <E> Facet<Optional<E>> option(ReferenceOption<E> option)
    {
        return of(Declaration.empty().options(option.spec()), run -> run.reference(option, run.client()));
    }
}
