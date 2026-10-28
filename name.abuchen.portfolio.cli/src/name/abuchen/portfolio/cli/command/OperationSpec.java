package name.abuchen.portfolio.cli.command;

/**
 * Declares a write operation once: its name, the schema of the input an
 * agent sends and the handler that applies it. <code>--help</code> renders
 * the declaration and <code>apply</code> dispatches on it.
 *
 * @param <C>
 *            what the handler sees of the batch it runs in
 */
public record OperationSpec<C>(String name, String summary, Schema input, Handler<C> handler)
{
    /** The member of an operation's input that names the operation. */
    public static final String OPERATION_MEMBER = "operation";

    /**
     * Applies one operation to the evolving model. A handler reports every
     * problem through the context and changes the model only if it found none.
     */
    @FunctionalInterface
    public interface Handler<C>
    {
        void apply(C context);
    }
}
