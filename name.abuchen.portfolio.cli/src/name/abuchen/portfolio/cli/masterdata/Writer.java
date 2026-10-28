package name.abuchen.portfolio.cli.masterdata;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

import name.abuchen.portfolio.cli.command.EntityKind;
import name.abuchen.portfolio.cli.command.ErrorCode;
import name.abuchen.portfolio.cli.command.Schema;

/**
 * How a write sets one member of a master data kind. A writer belongs to a
 * member, so every write field is a read member. Its description is the write
 * description; the read description stays the member's own.
 */
public final class Writer<T>
{
    /** How the input may give the member. */
    enum Mode
    {
        /** A string that an add needs and an update can't clear. */
        REQUIRED,
        /** A string or null; null or a blank string clears it. */
        OPTIONAL,
        /** A boolean; an add defaults it to false. */
        FLAG
    }

    /** Applies a value that passed every check to the entity. */
    @FunctionalInterface
    interface Setter<T>
    {
        void set(Writes<T> writes, T entity, Object value);
    }

    /** A check of the member's value alone; a value that fails it is not set. */
    record Check(Predicate<String> valid, ErrorCode code, Function<String, String> message, String coreCheck)
    {
    }

    private final String description;
    private final Mode mode;
    private final String needs;
    private final Setter<T> setter;
    private final List<Check> checks;
    private final boolean reference;

    private Writer(String description, Mode mode, String needs, Setter<T> setter, List<Check> checks,
                    boolean reference)
    {
        this.description = description;
        this.mode = mode;
        this.needs = needs;
        this.setter = setter;
        this.checks = List.copyOf(checks);
        this.reference = reference;
    }

    /**
     * A string that an add needs and an update can't clear.
     *
     * @param needs
     *            what the failure calls the missing value, such as
     *            <code>a name</code>
     */
    public static <T> Writer<T> required(String description, String needs, BiConsumer<T, String> set)
    {
        return new Writer<>(description, Mode.REQUIRED, needs, (w, e, v) -> set.accept(e, (String) v), List.of(),
                        false);
    }

    /**
     * A required UUID of an existing entity of another kind. The kind's
     * validator makes sure the entity exists before the setter runs.
     */
    public static <T, A> Writer<T> requiredExisting(String description, String needs, EntityKind<A> kind,
                    BiConsumer<T, A> set)
    {
        return new Writer<>(description, Mode.REQUIRED, needs,
                        (w, e, v) -> set.accept(e, w.existing(kind, (String) v).orElseThrow()), List.of(), true);
    }

    /** A string that null or a blank string clears. */
    public static <T> Writer<T> optional(String description, BiConsumer<T, String> set)
    {
        return new Writer<>(description, Mode.OPTIONAL, null, (w, e, v) -> set.accept(e, (String) v), List.of(),
                        false);
    }

    /**
     * A boolean that an add defaults to false.
     *
     * @param description
     *            without the closing period; the add appends its default
     */
    public static <T> Writer<T> flag(String description, BiConsumer<T, Boolean> set)
    {
        return new Writer<>(description, Mode.FLAG, null, (w, e, v) -> set.accept(e, (Boolean) v), List.of(),
                        false);
    }

    /**
     * Adds a check of the value alone.
     *
     * @param message
     *            the failure message for the refused value
     * @param coreCheck
     *            the core check that decides, or null
     */
    public Writer<T> check(Predicate<String> valid, ErrorCode code, Function<String, String> message, String coreCheck)
    {
        var all = new ArrayList<>(checks);
        all.add(new Check(valid, code, message, coreCheck));
        return new Writer<>(description, mode, needs, setter, all, reference);
    }

    Mode mode()
    {
        return mode;
    }

    String needs()
    {
        return needs;
    }

    Setter<T> setter()
    {
        return setter;
    }

    List<Check> checks()
    {
        return checks;
    }

    /** The member in the input schema of an add or an update. */
    Schema schema(boolean update)
    {
        return switch (mode)
        {
            case REQUIRED -> reference ? Schema.reference(description) : Schema.string(description);
            case OPTIONAL -> Schema.string(description).nullable();
            case FLAG -> Schema.bool(description + (update ? "." : "; defaults to false."));
        };
    }

    /** Whether the input of the add or update must give the member. */
    boolean requiredIn(boolean update)
    {
        return mode == Mode.REQUIRED && !update;
    }
}
