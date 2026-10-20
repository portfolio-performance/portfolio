package name.abuchen.portfolio.cli;

/**
 * Declares a write operation once: its name, the schema of the input an
 * agent sends and the handler that applies it. <code>describe</code> renders
 * the declaration and <code>apply</code> dispatches on it.
 */
record OperationSpec(String name, String summary, Schema input, Handler handler)
{
    /**
     * Applies one operation to the evolving model. A handler reports every
     * problem through the context and changes the model only if it found none.
     */
    @FunctionalInterface
    interface Handler
    {
        void apply(OperationContext context);
    }
}
