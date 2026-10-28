package name.abuchen.portfolio.cli.masterdata;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

import name.abuchen.portfolio.cli.command.CliException;
import name.abuchen.portfolio.cli.command.ClientFile;
import name.abuchen.portfolio.cli.command.CommandContext;
import name.abuchen.portfolio.cli.command.CommandSpec;
import name.abuchen.portfolio.cli.command.EntityKind;
import name.abuchen.portfolio.cli.command.MasterData;
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
 * the render, the reference lookup, the paged list command and the
 * existing-entity check are all derived from the declaration.
 */
public final class MasterDataKind<T>
{
    /** When a member appears in the item. */
    public enum Render
    {
        /** Always present; a null name is the empty string. */
        ALWAYS,
        /** Absent if the value is null or a blank string. */
        IF_SET
    }

    /**
     * One member of the read item.
     *
     * @param getter
     *            prepares, per client, how the member reads an entity
     * @param perClient
     *            whether the getter needs the client; such a member can't be a
     *            lookup key or candidate detail
     */
    public record Member<T>(String name, Schema schema, Render render, Function<Client, Function<T, ?>> getter,
                    boolean perClient)
    {
        static <T> Member<T> of(String name, Schema schema, Render render, Function<T, ?> getter)
        {
            return new Member<>(name, schema, render, client -> getter, false);
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
            return member(new Member<>(member, schema, Render.ALWAYS, getter, true));
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
            return new MasterDataKind<>(this, summary, columns);
        }
    }

    private final String id;
    private final List<Member<T>> members;
    private final Schema item;
    private final Reference<T> reference;
    private final EntityKind<T> entityKind;
    private final CommandSpec command;

    private MasterDataKind(Builder<T> builder, String summary, List<TableColumn> columns)
    {
        this.id = builder.id;
        this.members = List.copyOf(builder.members);
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
