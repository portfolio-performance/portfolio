package name.abuchen.portfolio.cli.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * What one {@link Facet} contributes to a command's declaration: its options,
 * its members of <code>meta</code>, the warnings and error codes it can give,
 * and whether it needs the client file to resolve. Help is generated from it
 * (ADR 0006). Immutable; each method returns a copy with more declared.
 */
public record Declaration(List<OptionSpec> options, List<Member> meta, List<WarningSpec> warnings,
                Set<ErrorCode> codes, boolean needsClient, Slot slot)
{
    /** One member of <code>meta</code>. */
    public record Member(String name, Schema schema, boolean required)
    {
    }

    /**
     * Where a facet's meta goes. The envelope comes first, then paging, then
     * sorting, then every other facet in list order.
     */
    enum Slot
    {
        ENVELOPE, PAGING, SORTING, OTHER
    }

    public Declaration
    {
        options = List.copyOf(options);
        meta = List.copyOf(meta);
        warnings = List.copyOf(warnings);
        codes = Set.copyOf(codes);
    }

    /** Declares nothing; the start of every declaration. */
    public static Declaration empty()
    {
        return new Declaration(List.of(), List.of(), List.of(), Set.of(), false, Slot.OTHER);
    }

    public Declaration options(OptionSpec... more)
    {
        return new Declaration(concat(options, Arrays.asList(more)), meta, warnings, codes, needsClient, slot);
    }

    /** A member of <code>meta</code> the facet always emits. */
    public Declaration meta(String name, Schema schema)
    {
        return withMember(new Member(name, schema, true));
    }

    /** A member of <code>meta</code> the facet may leave out. */
    public Declaration optionalMeta(String name, Schema schema)
    {
        return withMember(new Member(name, schema, false));
    }

    private Declaration withMember(Member member)
    {
        return new Declaration(options, concat(meta, List.of(member)), warnings, codes, needsClient, slot);
    }

    public Declaration warnings(WarningSpec... more)
    {
        return new Declaration(options, meta, concat(warnings, Arrays.asList(more)), codes, needsClient, slot);
    }

    public Declaration codes(ErrorCode... more)
    {
        var all = EnumSet.noneOf(ErrorCode.class);
        all.addAll(codes);
        all.addAll(Arrays.asList(more));
        return new Declaration(options, meta, warnings, all, needsClient, slot);
    }

    /** The facet resolves only after the client file is loaded and may call {@link CommandContext#client()}. */
    public Declaration needingClient()
    {
        return new Declaration(options, meta, warnings, codes, true, slot);
    }

    Declaration in(Slot newSlot)
    {
        return new Declaration(options, meta, warnings, codes, needsClient, newSlot);
    }

    /** Whether the facet resolves after the client loads; a reference option implies it. */
    public boolean resolvesWithClient()
    {
        return needsClient || options.stream().anyMatch(o -> o.type() instanceof OptionSpec.Reference);
    }

    boolean declares(OptionSpec option)
    {
        return options.stream().anyMatch(o -> o.name().equals(option.name()));
    }

    boolean declaresMember(String name)
    {
        return meta.stream().anyMatch(m -> m.name().equals(name));
    }

    boolean declaresWarning(String code)
    {
        return warnings.stream().anyMatch(w -> w.code().equals(code));
    }

    /** The members as a meta schema of their own. */
    public Schema metaSchema()
    {
        return addTo(Schema.object("Assumptions behind the response."));
    }

    Schema addTo(Schema schema)
    {
        for (var member : meta)
        {
            if (member.required())
                schema.property(member.name(), member.schema());
            else
                schema.optionalProperty(member.name(), member.schema());
        }
        return schema;
    }

    private static <E> List<E> concat(List<E> first, List<E> second)
    {
        var all = new ArrayList<E>(first);
        all.addAll(second);
        return all;
    }
}
