package name.abuchen.portfolio.cli.command;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import com.google.gson.JsonObject;

import name.abuchen.portfolio.model.Client;

/**
 * Declares a write operation once: its name, the schema of the input an
 * agent sends, the failure codes its owning module declares and the handler
 * that applies it. <code>--help</code> renders the declaration and
 * <code>apply</code> dispatches on it. Build one with
 * {@link #of(String, String, Schema, Handler)}.
 *
 * @param <C>
 *            what the handler sees of the batch it runs in
 * @param declaredCodes
 *            the failure codes the owning module raises beyond those the
 *            input schema implies
 * @param claims
 *            whether the operation claims the entities it changes, so that
 *            a second operation on one is refused
 */
public record OperationSpec<C>(String name, String summary, Schema input, Set<ErrorCode> declaredCodes,
                boolean claims, Handler<C> handler)
{
    /** The member of an operation's input that names the operation. */
    public static final String OPERATION_MEMBER = "operation";

    public OperationSpec
    {
        declaredCodes = Set.copyOf(declaredCodes);
    }

    /** An operation that declares no codes of its own and claims nothing. */
    public static <C> OperationSpec<C> of(String name, String summary, Schema input, Handler<C> handler)
    {
        return new OperationSpec<>(name, summary, input, Set.of(), false, handler);
    }

    /** The operation, also declaring the codes its module raises. */
    public OperationSpec<C> declaring(Set<ErrorCode> codes)
    {
        var all = EnumSet.noneOf(ErrorCode.class);
        all.addAll(declaredCodes);
        all.addAll(codes);
        return new OperationSpec<>(name, summary, input, all, claims, handler);
    }

    /** The operation, also declaring the codes its module raises. */
    public OperationSpec<C> declaring(ErrorCode... codes)
    {
        return declaring(Set.of(codes));
    }

    /** The operation, claiming the entities it changes. */
    public OperationSpec<C> claiming()
    {
        return new OperationSpec<>(name, summary, input, declaredCodes, true, handler);
    }

    /** The same declaration, run by another handler; for the batch that adapts a module's operation. */
    public <D> OperationSpec<D> handledBy(Handler<D> other)
    {
        return new OperationSpec<>(name, summary, input, declaredCodes, claims, other);
    }

    /**
     * Every code a <code>failures</code> entry of this operation can carry,
     * in declaration order: <code>unknown-field</code>, which every operation
     * brings; those the input schema implies (see
     * {@link Schema#impliedFailureCodes()}); <code>duplicate-target</code> if
     * it claims; and those its module declares. It may hold a code the
     * operation can't raise, never miss one it can.
     */
    public Set<ErrorCode> failureCodes()
    {
        var codes = EnumSet.of(ErrorCode.UNKNOWN_FIELD);
        codes.addAll(input.impliedFailureCodes());
        if (claims)
            codes.add(ErrorCode.DUPLICATE_TARGET);
        codes.addAll(declaredCodes);
        return Collections.unmodifiableSet(codes);
    }

    /**
     * Applies one operation to the evolving model. A handler reports every
     * problem through the context and changes the model only if it found none.
     */
    @FunctionalInterface
    public interface Handler<C>
    {
        void apply(C context);
    }

    /**
     * What a handler declared outside <code>apply</code> sees of the batch:
     * its input, the evolving model, the claims and where its outcome goes.
     */
    public interface Context
    {
        /** The input of this operation. */
        JsonObject json();

        /** The evolving model. */
        Client client();

        /** Whether the object of the model was in the file before the batch started. */
        boolean existedBeforeBatch(Object entity);

        /** Claims entities for this operation. */
        Claims claims();

        /** Reports what the operation did: its change or its failures. */
        void record(Outcome outcome);
    }
}
