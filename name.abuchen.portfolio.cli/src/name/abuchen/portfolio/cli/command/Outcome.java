package name.abuchen.portfolio.cli.command;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * What one write operation did. {@link Refused} means the model was not
 * touched; {@link Applied} means the change is already made.
 */
public sealed interface Outcome
{
    /** The kind of entity a change names, published as its <code>entity</code>. */
    enum Entity
    {
        INSTRUMENT("instrument"), //
        CASH_ACCOUNT("cash-account"), //
        INVESTMENT_ACCOUNT("investment-account"), //
        TRANSACTION("transaction"), //
        TAXONOMY_ASSIGNMENT("taxonomy-assignment");

        private final String id;

        Entity(String id)
        {
            this.id = id;
        }

        /** The published value, such as <code>cash-account</code>. */
        public String id()
        {
            return id;
        }

        /** The published values, in declaration order. */
        public static List<String> ids()
        {
            return Arrays.stream(values()).map(Entity::id).toList();
        }
    }

    /**
     * The entity was changed. A created entity has no <code>before</code>
     * (null), a deleted one all its fields in <code>before</code> and an empty
     * <code>after</code>, and an updated one the changed fields in both.
     */
    record Applied(Entity kind, String recordId, Map<String, Object> before, Map<String, Object> after)
                    implements Outcome
    {
    }

    /** The operation named the entity but changed nothing. */
    record Unchanged(String recordId) implements Outcome
    {
    }

    /** The operation failed the listed checks and left the model untouched. */
    record Refused(List<FieldFailure> failures) implements Outcome
    {
        public Refused
        {
            failures = List.copyOf(failures);
        }
    }
}
