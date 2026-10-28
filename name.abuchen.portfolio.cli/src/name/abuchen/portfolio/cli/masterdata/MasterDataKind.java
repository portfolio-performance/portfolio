package name.abuchen.portfolio.cli.masterdata;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import name.abuchen.portfolio.cli.command.CliException;
import name.abuchen.portfolio.cli.command.ClientFile;
import name.abuchen.portfolio.cli.command.CommandContext;
import name.abuchen.portfolio.cli.command.CommandSpec;
import name.abuchen.portfolio.cli.command.EntityKind;
import name.abuchen.portfolio.cli.command.MasterData;
import name.abuchen.portfolio.cli.command.OperationSpec;
import name.abuchen.portfolio.cli.command.Outcome;
import name.abuchen.portfolio.cli.command.Paging;
import name.abuchen.portfolio.cli.command.Reference;
import name.abuchen.portfolio.cli.command.ReferenceOption;
import name.abuchen.portfolio.cli.command.Schema;
import name.abuchen.portfolio.cli.command.Sorting;
import name.abuchen.portfolio.cli.command.TableColumn;
import name.abuchen.portfolio.model.Client;

/**
 * One kind of master data (instruments, cash accounts, investment accounts),
 * declared once. Its ordered member list is the read item: the item schema,
 * the render, the reference lookup, the paged list command, the
 * existing-entity check and the add and update operations are all derived
 * from the declaration. A writable member carries its {@link Writer}, so every
 * write field is a read member.
 */
public final class MasterDataKind<T>
{
    private static final String UUID_MEMBER = "uuid";

    /** When a member appears in the item. */
    public enum Render
    {
        /** Always present; a null name is the empty string. */
        ALWAYS,
        /** Absent if the value is null or a blank string. */
        IF_SET
    }

    /**
     * The checks of a write that look beyond one member. They run after the
     * member checks, on the values that passed them.
     */
    @FunctionalInterface
    public interface Validator<T>
    {
        void validate(Writes.Validation<T> validation);
    }

    /**
     * One member of the read item.
     *
     * @param getter
     *            prepares, per client, how the member reads an entity
     * @param perClient
     *            whether the getter needs the client; such a member can't be a
     *            lookup key or candidate detail
     * @param writer
     *            how a write sets the member, or null if it is read-only
     */
    public record Member<T>(String name, Schema schema, Render render, Function<Client, Function<T, ?>> getter,
                    boolean perClient, Writer<T> writer)
    {
        static <T> Member<T> of(String name, Schema schema, Render render, Function<T, ?> getter)
        {
            return new Member<>(name, schema, render, client -> getter, false, null);
        }

        Member<T> withWriter(Writer<T> w)
        {
            return new Member<>(name, schema, render, getter, perClient, w);
        }

        /** The value of a member that doesn't need the client, as a string. */
        String text(T entity)
        {
            var value = getter.apply(null).apply(entity);
            return value == null ? null : value.toString();
        }
    }

    /** The parts of a declaration, collected in order. */
    public static final class Builder<T>
    {
        private final String id;
        private final String noun;
        private final Function<Client, List<T>> entities;
        private final Function<T, String> uuid;
        private final Function<T, String> name;
        private final List<Member<T>> members = new ArrayList<>();
        private Schema item;
        private Predicate<T> retired;
        private Function<T, Instant> updatedAt;
        private List<String> lookupKeys = List.of();
        private List<String> candidateDetails = List.of();
        private Supplier<T> create;
        private BiConsumer<Client, T> add;
        private Consumer<T> initialize;
        private Validator<T> validator = validation -> {
        };
        private String addSummary;
        private String updateSummary;

        private Builder(String id, String noun, Function<Client, List<T>> entities, Function<T, String> uuid,
                        Function<T, String> name)
        {
            this.id = id;
            this.noun = noun;
            this.entities = entities;
            this.uuid = uuid;
            this.name = name;
        }

        /**
         * Starts the item with <code>uuid</code> and <code>name</code>.
         *
         * @param itemDescription
         *            what one item is
         */
        public Builder<T> item(String itemDescription, String nameDescription)
        {
            item = Schema.object(itemDescription);
            member(Member.of("uuid", Schema.string("Canonical UUID."), Render.ALWAYS, uuid));
            return member(Member.of("name", Schema.string(nameDescription), Render.ALWAYS,
                            entity -> MasterData.orEmpty(name.apply(entity))));
        }

        public Builder<T> always(String member, Schema schema, Function<T, ?> getter)
        {
            return member(Member.of(member, schema, Render.ALWAYS, getter));
        }

        public Builder<T> ifSet(String member, Schema schema, Function<T, ?> getter)
        {
            return member(Member.of(member, schema, Render.IF_SET, getter));
        }

        /** A member always present whose getter is prepared once per client. */
        public Builder<T> alwaysPrepared(String member, Schema schema, Function<Client, Function<T, ?>> getter)
        {
            return member(new Member<>(member, schema, Render.ALWAYS, getter, true, null));
        }

        /** Makes the member declared last writable. */
        public Builder<T> write(Writer<T> writer)
        {
            if (members.isEmpty())
                throw new IllegalStateException("Declare a member before its writer");
            var last = members.size() - 1;
            if (members.get(last).perClient())
                throw new IllegalStateException("'" + members.get(last).name() + "' needs the client to read");
            members.set(last, members.get(last).withWriter(writer));
            return this;
        }

        /**
         * How an add creates an entity and puts it into the client.
         *
         * @param initialize
         *            what a new entity needs beyond its members
         */
        public Builder<T> creates(Supplier<T> create, BiConsumer<Client, T> add, Consumer<T> initialize)
        {
            this.create = create;
            this.add = add;
            this.initialize = initialize;
            return this;
        }

        /** The checks of a write that look beyond one member. */
        public Builder<T> validator(Validator<T> validator)
        {
            this.validator = validator;
            return this;
        }

        /** The summaries of the add and update operations. */
        public Builder<T> operations(String addSummary, String updateSummary)
        {
            this.addSummary = addSummary;
            this.updateSummary = updateSummary;
            return this;
        }

        /** The <code>retired</code> member; the flag also marks candidates. */
        public Builder<T> retired(String description, Predicate<T> isRetired)
        {
            retired = isRetired;
            return always("retired", Schema.bool(description), isRetired::test);
        }

        /** The <code>updatedAt</code> member; it also feeds the <code>updatedAt</code> sort. */
        public Builder<T> updatedAt(Function<T, Instant> lastModified)
        {
            updatedAt = lastModified;
            return ifSet(MasterData.UPDATED_AT_MEMBER, MasterData.UPDATED_AT,
                            entity -> Sorting.lastModified(lastModified.apply(entity)).map(Instant::toString)
                                            .orElse(null));
        }

        /** Members matched by a reference after the UUID. */
        public Builder<T> lookupKeys(String... members)
        {
            lookupKeys = List.of(members);
            return this;
        }

        /** Members an ambiguity lists per candidate, if set. */
        public Builder<T> candidateDetails(String... members)
        {
            candidateDetails = List.of(members);
            return this;
        }

        private Builder<T> member(Member<T> member)
        {
            if (item == null)
                throw new IllegalStateException("Declare the item before member '" + member.name() + "'");
            if (members.stream().anyMatch(m -> m.name().equals(member.name())))
                throw new IllegalStateException("Member '" + member.name() + "' is declared twice");
            members.add(member);
            item = member.render() == Render.ALWAYS ? item.property(member.name(), member.schema())
                            : item.optionalProperty(member.name(), member.schema());
            return this;
        }

        /**
         * @param summary
         *            what each list item holds; the sort fields are appended
         * @param columns
         *            the columns of <code>--format table</code>
         */
        public MasterDataKind<T> list(String summary, List<TableColumn> columns)
        {
            if (retired == null || updatedAt == null)
                throw new IllegalStateException("A master data kind needs retired and updatedAt");
            if (create == null || addSummary == null)
                throw new IllegalStateException("A master data kind needs creates and operations");
            return new MasterDataKind<>(this, summary, columns);
        }
    }

    private final String id;
    private final String noun;
    private final Outcome.Entity entity;
    private final List<Member<T>> members;
    private final Supplier<T> create;
    private final BiConsumer<Client, T> add;
    private final Consumer<T> initialize;
    private final Validator<T> validator;
    private final OperationSpec<OperationSpec.Context> addOperation;
    private final OperationSpec<OperationSpec.Context> updateOperation;
    private final Schema item;
    private final Reference<T> reference;
    private final EntityKind<T> entityKind;
    private final CommandSpec command;

    private MasterDataKind(Builder<T> builder, String summary, List<TableColumn> columns)
    {
        this.id = builder.id;
        this.noun = builder.noun;
        this.entity = Arrays.stream(Outcome.Entity.values()).filter(e -> e.id().equals(builder.id)).findFirst()
                        .orElseThrow(() -> new IllegalStateException("No change entity '" + builder.id + "'"));
        this.members = List.copyOf(builder.members);
        this.create = builder.create;
        this.add = builder.add;
        this.initialize = builder.initialize;
        this.validator = builder.validator;
        this.item = builder.item;
        this.reference = Reference.of(builder.noun, builder.entities, builder.uuid, builder.name,
                        keys(builder.lookupKeys), Optional.of(builder.retired), keys(builder.candidateDetails));
        this.entityKind = EntityKind.of(reference);

        var sort = Sorting.Spec.of(builder.uuid, "name",
                        Sorting.Field.of("name", MasterData.byName(builder.name)),
                        Sorting.Field.of("updatedAt", Sorting.byInstant(
                                        entity -> Sorting.lastModified(builder.updatedAt.apply(entity)))));
        this.command = CommandSpec.of(id + "s",
                        summary + ", sorted by name (default) or updatedAt, then UUID; --order desc reverses.",
                        Paging.pageSchema("A page of the list.", item), run -> list(run, sort), ClientFile.READ,
                        Paging.FACET, sort).withTable(columns);

        this.addOperation = new OperationSpec<>(id + ".add", builder.addSummary, input(false),
                        context -> context.record(writes(context.client(), context::existedBeforeBatch)
                                        .add(context.json())));
        this.updateOperation = new OperationSpec<>(id + ".update", builder.updateSummary,
                        input(true).property(UUID_MEMBER, Schema.string("UUID of the " + noun + ".")),
                        context -> context.record(writes(context.client(), context::existedBeforeBatch)
                                        .update(context.json(), context.claims())));
    }

    /**
     * Starts a declaration.
     *
     * @param id
     *            the entity id, such as <code>cash-account</code>; its plural
     *            is the list command's name
     * @param noun
     *            how failures name the kind, such as <code>cash account</code>
     */
    public static <T> Builder<T> declare(String id, String noun, Function<Client, List<T>> entities,
                    Function<T, String> uuid, Function<T, String> name)
    {
        return new Builder<>(id, noun, entities, uuid, name);
    }

    public String id()
    {
        return id;
    }

    /** How failures name the kind, such as <code>cash account</code>. */
    public String noun()
    {
        return noun;
    }

    /** The schema of one rendered item. */
    public Schema item()
    {
        return item;
    }

    /** Prepares, per client, how one entity is rendered. */
    public Function<T, Map<String, Object>> renderer(Client client)
    {
        var getters = members.stream().map(m -> m.getter().apply(client)).toList();
        return entity -> {
            var json = new LinkedHashMap<String, Object>();
            for (var i = 0; i < members.size(); i++)
            {
                var member = members.get(i);
                Object value = getters.get(i).apply(entity);
                if (member.render() == Render.ALWAYS || isSet(value))
                    json.put(member.name(), value);
            }
            return json;
        };
    }

    public Reference<T> reference()
    {
        return reference;
    }

    /** An option that names one entity of the kind. */
    public ReferenceOption<T> option(String name, String description)
    {
        return ReferenceOption.of(name, description, reference);
    }

    /** The existing entities of the kind, as a write names them. */
    public EntityKind<T> entityKind()
    {
        return entityKind;
    }

    /** The paged, sortable list command. */
    public CommandSpec command()
    {
        return command;
    }

    /**
     * The writes on the evolving model.
     *
     * @param existedBeforeBatch
     *            whether an object of the model was in the file before the
     *            batch; only such an entity can be named
     */
    public Writes<T> writes(Client client, Predicate<Object> existedBeforeBatch)
    {
        return new Writes<>(this, client, existedBeforeBatch);
    }

    /** The <code>&lt;id&gt;.add</code> operation. */
    public OperationSpec<OperationSpec.Context> addOperation()
    {
        return addOperation;
    }

    /** The <code>&lt;id&gt;.update</code> operation. */
    public OperationSpec<OperationSpec.Context> updateOperation()
    {
        return updateOperation;
    }

    /** The add and update operations. */
    public List<OperationSpec<OperationSpec.Context>> operations()
    {
        return List.of(addOperation, updateOperation);
    }

    Outcome.Entity entity()
    {
        return entity;
    }

    Validator<T> validator()
    {
        return validator;
    }

    T create()
    {
        return create.get();
    }

    void initialize(T created)
    {
        if (initialize != null)
            initialize.accept(created);
    }

    void addTo(Client client, T created)
    {
        add.accept(client, created);
    }

    /** The writable members, in member order. */
    List<Member<T>> writable()
    {
        return members.stream().filter(m -> m.writer() != null).toList();
    }

    Member<T> member(String name)
    {
        return members.stream().filter(m -> m.name().equals(name)).findFirst().orElseThrow();
    }

    /** The input of an add or update: the operation name and the writable members. */
    private Schema input(boolean update)
    {
        var schema = Schema.object("Fields of the " + noun + "; they match the " + id + "s output.");
        schema.property(OperationSpec.OPERATION_MEMBER,
                        Schema.enumOf("Operation name.", List.of(id + (update ? ".update" : ".add"))));
        for (var member : writable())
        {
            var writer = member.writer();
            if (writer.requiredIn(update))
                schema.property(member.name(), writer.schema(update));
            else
                schema.optionalProperty(member.name(), writer.schema(update));
        }
        return schema;
    }

    private static boolean isSet(Object value)
    {
        return value instanceof String s ? MasterData.isSet(s) : value != null;
    }

    private List<Reference.Key<T>> keys(List<String> names)
    {
        return names.stream().map(this::key).toList();
    }

    private Reference.Key<T> key(String name)
    {
        var member = members.stream().filter(m -> m.name().equals(name)).findFirst()
                        .orElseThrow(() -> new IllegalStateException(
                                        "'" + name + "' is not a member of " + id));
        if (member.perClient())
            throw new IllegalStateException("'" + name + "' needs the client and can't be a lookup key or detail");
        return new Reference.Key<>(name, member::text);
    }

    private Map<String, Object> list(CommandContext run, Sorting.Spec<T> sort) throws CliException
    {
        var client = run.client();
        var ordered = run.get(sort).sort(client, reference.entities(client));
        var page = run.get(Paging.FACET).of(ordered);

        var data = new LinkedHashMap<String, Object>();
        page.putInto(data, renderer(client));
        return data;
    }
}
