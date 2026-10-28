package name.abuchen.portfolio.cli.masterdata;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

import com.google.gson.JsonObject;

import name.abuchen.portfolio.cli.command.Claims;
import name.abuchen.portfolio.cli.command.EntityKind;
import name.abuchen.portfolio.cli.command.ErrorCode;
import name.abuchen.portfolio.cli.command.FieldFailure;
import name.abuchen.portfolio.cli.command.OperationInput;
import name.abuchen.portfolio.cli.command.Outcome;
import name.abuchen.portfolio.model.Client;

/**
 * The writes of one master data kind on one evolving model: add and update.
 * An entity can only be named if it was in the file before the batch.
 */
public final class Writes<T>
{
    private static final String UUID_MEMBER = "uuid";

    /** What a kind's validator sees: the values that passed every member check. */
    public static final class Validation<T>
    {
        private final Writes<T> writes;
        private final OperationInput input;
        private final Map<String, Object> values;
        private final T existing;

        private Validation(Writes<T> writes, OperationInput input, Map<String, Object> values, T existing)
        {
            this.writes = writes;
            this.input = input;
            this.values = values;
            this.existing = existing;
        }

        /** The members given, by name; null clears a member. */
        public Map<String, Object> values()
        {
            return values;
        }

        /** The entity being updated, or null on an add or if there is none. */
        public T existing()
        {
            return existing;
        }

        public Client client()
        {
            return writes.client;
        }

        public void fail(String field, ErrorCode code, String message)
        {
            input.fail(field, code, message);
        }

        /** Reports <code>reference-not-found</code> unless the UUID names an existing entity of the kind. */
        public void requireExisting(String field, String uuid, EntityKind<?> kind)
        {
            input.find(field, uuid, kind);
        }
    }

    private final MasterDataKind<T> kind;
    private final Client client;
    private final Predicate<Object> existedBeforeBatch;

    Writes(MasterDataKind<T> kind, Client client, Predicate<Object> existedBeforeBatch)
    {
        this.kind = kind;
        this.client = client;
        this.existedBeforeBatch = existedBeforeBatch;
    }

    /** Adds an entity with the members of the input. */
    public Outcome add(JsonObject json)
    {
        var failures = new ArrayList<FieldFailure>();
        var input = new OperationInput(json, client, existedBeforeBatch, failures::add);

        var values = read(input, failures, false);
        kind.validator().validate(new Validation<>(this, input, values, null));
        if (!failures.isEmpty())
            return new Outcome.Refused(failures);

        var entity = kind.create();
        values.forEach((name, value) -> writer(name).setter().set(this, entity, value));
        kind.initialize(entity);
        kind.addTo(client, entity);

        var after = new LinkedHashMap<String, Object>();
        for (var member : kind.writable())
        {
            var value = current(member, entity);
            if (value != null)
                after.put(member.name(), value);
        }
        return new Outcome.Applied(kind.entity(), kind.reference().uuid(entity), null, after);
    }

    /**
     * Changes the members present in the input of the entity its
     * <code>uuid</code> names, which this operation claims.
     */
    public Outcome update(JsonObject json, Claims claims)
    {
        var failures = new ArrayList<FieldFailure>();
        var input = new OperationInput(json, client, existedBeforeBatch, failures::add);

        var entity = find(input, claims);
        var values = read(input, failures, true);
        kind.validator().validate(new Validation<>(this, input, values, entity));
        if (entity == null || !failures.isEmpty())
            return new Outcome.Refused(failures);

        var before = new LinkedHashMap<String, Object>();
        var after = new LinkedHashMap<String, Object>();
        values.forEach((name, value) -> {
            var old = current(kind.member(name), entity);
            if (!Objects.equals(old, value))
            {
                before.put(name, old);
                after.put(name, value);
            }
        });

        var uuid = kind.reference().uuid(entity);
        if (after.isEmpty())
            return new Outcome.Unchanged(uuid);

        after.forEach((name, value) -> writer(name).setter().set(this, entity, value));
        return new Outcome.Applied(kind.entity(), uuid, before, after);
    }

    /** The existing entity of the kind with the UUID, if there is one. */
    <A> Optional<A> existing(EntityKind<A> other, String uuid)
    {
        return other.find(client, existedBeforeBatch, uuid);
    }

    /** The entity the <code>uuid</code> names, claimed, or null after reporting why not. */
    private T find(OperationInput input, Claims claims)
    {
        var member = input.json().get(UUID_MEMBER);
        if (member == null || member.isJsonNull())
        {
            input.fail(UUID_MEMBER, ErrorCode.REQUIRED, "An update needs the UUID of the " + kind.noun() + ".");
            return null;
        }
        if (!OperationInput.isText(member))
        {
            input.fail(UUID_MEMBER, ErrorCode.INVALID_TYPE, "The UUID must be a string.");
            return null;
        }

        var uuid = member.getAsString();
        var found = input.find(UUID_MEMBER, uuid, kind.entityKind());
        if (found == null)
            return null;

        var earlier = claims.claim(uuid);
        if (earlier.isPresent())
        {
            input.fail(UUID_MEMBER, ErrorCode.DUPLICATE_TARGET, "Operation " + earlier.getAsInt() + " already changes "
                            + uuid + "; one batch may change an entity only once.");
            return null;
        }
        return found;
    }

    /**
     * The writable members present in the input, after the type, required and
     * member checks. A null or blank string is read as null; a value that
     * fails a check is left out.
     */
    private Map<String, Object> read(OperationInput input, List<FieldFailure> failures, boolean update)
    {
        var values = new LinkedHashMap<String, Object>();
        var json = input.json();

        for (var member : kind.writable())
        {
            var name = member.name();
            var writer = member.writer();
            var element = json.get(name);

            if (element == null)
            {
                if (writer.requiredIn(update))
                    input.fail(name, ErrorCode.REQUIRED, needs(writer));
                continue;
            }

            if (writer.mode() == Writer.Mode.FLAG)
            {
                if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isBoolean())
                    values.put(name, element.getAsBoolean());
                else
                    input.fail(name, ErrorCode.INVALID_TYPE, "Must be true or false.");
                continue;
            }

            if (!element.isJsonNull() && !OperationInput.isText(element))
            {
                input.fail(name, ErrorCode.INVALID_TYPE, "Must be a string or null.");
                continue;
            }

            var text = element.isJsonNull() || element.getAsString().isBlank() ? null : element.getAsString();
            if (text == null && writer.mode() == Writer.Mode.REQUIRED)
            {
                input.fail(name, ErrorCode.REQUIRED, needs(writer));
                continue;
            }

            if (text != null && !passes(input, failures, name, writer, text))
                continue;

            values.put(name, text);
        }

        return values;
    }

    private boolean passes(OperationInput input, List<FieldFailure> failures, String name, Writer<T> writer,
                    String text)
    {
        for (var check : writer.checks())
        {
            if (!check.valid().test(text))
            {
                failures.add(new FieldFailure(input.field(name), check.code(), check.message().apply(text),
                                check.coreCheck()));
                return false;
            }
        }
        return true;
    }

    private String needs(Writer<T> writer)
    {
        var noun = kind.noun();
        var article = "aeiou".indexOf(noun.charAt(0)) >= 0 ? "An " : "A ";
        return article + noun + " needs " + writer.needs() + ".";
    }

    private Writer<T> writer(String name)
    {
        return kind.member(name).writer();
    }

    /** The member as the read output shows it, with a blank string read as unset. */
    private Object current(MasterDataKind.Member<T> member, T entity)
    {
        var value = member.getter().apply(client).apply(entity);
        return value instanceof String text && text.isBlank() ? null : value;
    }
}
