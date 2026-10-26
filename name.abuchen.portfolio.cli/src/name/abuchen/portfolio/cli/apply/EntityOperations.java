package name.abuchen.portfolio.cli.apply;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import name.abuchen.portfolio.cli.Schema;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.money.CurrencyUnit;

/**
 * The <code>add</code> and <code>update</code> operations of one kind of entity,
 * derived from a {@link Definition}. The rules every such entity shares live
 * here: the schema, unknown and mistyped fields, required fields, finding the
 * entity by UUID, one change per entity per batch, and reporting
 * <code>before</code> and <code>after</code>.
 */
final class EntityOperations<T>
{
    private static final String UUID_MEMBER = "uuid";
    private static final String RETIRED = "retired";

    /** Applies a value from the input to the entity. */
    @FunctionalInterface
    interface Setter<T>
    {
        void set(OperationContext context, T entity, Object value);
    }

    /** The checks only this kind of entity has; <code>existing</code> is null on add. */
    @FunctionalInterface
    interface Validator<T>
    {
        void validate(OperationContext context, Map<String, Object> values, T existing);
    }

    /**
     * One field of the input, named as in the read output.
     *
     * @param needs
     *            what a required field is called in the failure message, or
     *            null if the field is optional and can be cleared with null
     * @param flag
     *            whether the field is a boolean instead of a string
     */
    record Field<T>(String name, String description, String needs, boolean flag, Function<T, Object> get,
                    Setter<T> set)
    {
        static <T> Field<T> required(String name, String description, String needs, Function<T, Object> get,
                        BiConsumer<T, String> set)
        {
            return new Field<>(name, description, needs, false, get, (c, e, v) -> set.accept(e, (String) v));
        }

        static <T> Field<T> requiredWithContext(String name, String description, String needs,
                        Function<T, Object> get, Setter<T> set)
        {
            return new Field<>(name, description, needs, false, get, set);
        }

        static <T> Field<T> optional(String name, String description, Function<T, Object> get,
                        BiConsumer<T, String> set)
        {
            return new Field<>(name, description, null, false, get, (c, e, v) -> set.accept(e, (String) v));
        }

        /** The retired flag; <code>kind</code> is the entity as in "cash account". */
        static <T> Field<T> retired(String kind, Function<T, Object> get, BiConsumer<T, Boolean> set)
        {
            return new Field<>(RETIRED, "Whether the " + kind + " is retired", null, true, get,
                            (c, e, v) -> set.accept(e, (Boolean) v));
        }

        boolean isRequired()
        {
            return needs != null;
        }

        /** The field as the read output shows it: blank counts as unset. */
        Object current(T entity)
        {
            var value = get.apply(entity);
            return value instanceof String text && text.isBlank() ? null : value;
        }
    }

    /**
     * Everything that makes one kind of entity different from another.
     *
     * @param entity
     *            the name in operation names and changes, such as
     *            <code>cash-account</code>; the read output is its plural
     * @param fields
     *            in the order of the read output
     * @param initialize
     *            what a new entity needs beyond its fields, or null
     */
    record Definition<T>(String entity, String addSummary, String updateSummary, List<Field<T>> fields,
                    Function<Client, Collection<T>> all, Supplier<T> create, BiConsumer<Client, T> add,
                    Function<T, String> uuid, Validator<T> validator, Consumer<T> initialize)
    {
    }

    private final Definition<T> definition;
    private final String kind;

    EntityOperations(Definition<T> definition)
    {
        this.definition = definition;
        this.kind = definition.entity().replace('-', ' ');
    }

    OperationSpec addOperation()
    {
        return new OperationSpec(definition.entity() + ".add", definition.addSummary(), schema(false), this::add);
    }

    OperationSpec updateOperation()
    {
        return new OperationSpec(definition.entity() + ".update", definition.updateSummary(),
                        schema(true).property(UUID_MEMBER, Schema.string("UUID of the " + kind + ".")),
                        this::update);
    }

    /** Reports the currency if it is not a known ISO currency code. */
    static void validateCurrency(OperationContext context, Map<String, Object> values)
    {
        if (values.get("currencyCode") instanceof String code && !CurrencyUnit.containsCurrencyCode(code))
            context.fail("currencyCode", "invalid-currency", "'" + code + "' is not a known ISO currency code.");
    }

    /** Refuses a new currency for an entity that already has transactions. */
    static void validateCurrencyKept(OperationContext context, String kind, Map<String, Object> values,
                    Object currentCode, BooleanSupplier hasTransactions)
    {
        if (values.containsKey("currencyCode") && !Objects.equals(currentCode, values.get("currencyCode"))
                        && hasTransactions.getAsBoolean())
            context.fail("currencyCode", "currency-immutable",
                            "The currency can't change because the " + kind + " has transactions.");
    }

    private Schema schema(boolean update)
    {
        var schema = Schema.object("Fields of the " + kind + "; they match the " + definition.entity()
                        + "s output.");
        var operation = definition.entity() + (update ? ".update" : ".add");
        schema.property(ApplyBatch.OPERATION_MEMBER, Schema.enumOf("Operation name.", List.of(operation)));

        for (var field : definition.fields())
        {
            if (field.flag())
            {
                var description = field.description() + (update ? "." : "; defaults to false.");
                schema.optionalProperty(field.name(), Schema.bool(description));
            }
            else if (field.isRequired())
            {
                var type = Schema.string(field.description());
                if (update)
                    schema.optionalProperty(field.name(), type);
                else
                    schema.property(field.name(), type);
            }
            else
            {
                schema.optionalProperty(field.name(), Schema.string(field.description()).nullable());
            }
        }

        return schema;
    }

    private void add(OperationContext context)
    {
        var values = read(context);

        for (var field : definition.fields())
        {
            if (field.isRequired() && values.get(field.name()) == null)
                context.fail(field.name(), "required", needs(field));
        }

        definition.validator().validate(context, values, null);
        if (context.failed())
            return;

        var entity = definition.create().get();
        values.forEach((name, value) -> field(name).set().set(context, entity, value));
        if (definition.initialize() != null)
            definition.initialize().accept(entity);
        definition.add().accept(context.client(), entity);

        var after = new LinkedHashMap<String, Object>();
        for (var field : definition.fields())
        {
            var value = field.current(entity);
            if (value != null)
                after.put(field.name(), value);
        }

        context.created(new OperationContext.Change(definition.entity(), definition.uuid().apply(entity), null,
                        after));
    }

    private void update(OperationContext context)
    {
        var entity = find(context);
        var values = read(context);

        for (var field : definition.fields())
        {
            if (field.isRequired() && values.containsKey(field.name()) && values.get(field.name()) == null)
                context.fail(field.name(), "required", needs(field));
        }

        definition.validator().validate(context, values, entity);
        if (entity == null || context.failed())
            return;

        var before = new LinkedHashMap<String, Object>();
        var after = new LinkedHashMap<String, Object>();
        values.forEach((name, value) -> {
            var old = field(name).current(entity);
            if (!Objects.equals(old, value))
            {
                before.put(name, old);
                after.put(name, value);
            }
        });

        after.forEach((name, value) -> field(name).set().set(context, entity, value));

        if (!after.isEmpty())
            context.updated(new OperationContext.Change(definition.entity(), definition.uuid().apply(entity),
                            before, after));
    }

    private Field<T> field(String name)
    {
        return definition.fields().stream().filter(f -> f.name().equals(name)).findFirst().orElseThrow();
    }

    private String needs(Field<T> field)
    {
        var article = "aeiou".indexOf(kind.charAt(0)) >= 0 ? "An " : "A ";
        return article + kind + " needs " + field.needs() + ".";
    }

    /** The entity named by <code>uuid</code>, or null after reporting why not. */
    private T find(OperationContext context)
    {
        var member = context.input().json().get(UUID_MEMBER);
        if (member == null || member.isJsonNull())
        {
            context.fail(UUID_MEMBER, "required", "An update needs the UUID of the " + kind + ".");
            return null;
        }
        if (!OperationContext.isText(member))
        {
            context.fail(UUID_MEMBER, "invalid-type", "The UUID must be a string.");
            return null;
        }

        var uuid = member.getAsString();
        var found = context.input().find(UUID_MEMBER, uuid,
                        EntityKind.of(kind, definition.all(), definition.uuid()));
        if (found == null)
            return null;

        return context.claim(UUID_MEMBER, uuid) ? found : null;
    }

    /**
     * The fields that are present in the input. A null or blank string counts
     * as unset and is read as null.
     */
    private Map<String, Object> read(OperationContext context)
    {
        var values = new LinkedHashMap<String, Object>();
        var input = context.input().json();

        for (var field : definition.fields())
        {
            var member = input.get(field.name());
            if (member == null)
                continue;

            if (field.flag())
            {
                if (member.isJsonPrimitive() && member.getAsJsonPrimitive().isBoolean())
                    values.put(field.name(), member.getAsBoolean());
                else
                    context.fail(field.name(), "invalid-type", "Must be true or false.");
            }
            else if (member.isJsonNull())
            {
                values.put(field.name(), null);
            }
            else if (OperationContext.isText(member))
            {
                values.put(field.name(), member.getAsString().isBlank() ? null : member.getAsString());
            }
            else
            {
                context.fail(field.name(), "invalid-type", "Must be a string or null.");
            }
        }

        return values;
    }
}
