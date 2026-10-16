package name.abuchen.portfolio.cli;

/**
 * Declares a write operation once: its name and the schema of the input an
 * agent sends. <code>describe</code> renders it; the apply tickets will also
 * drive parsing and dispatch from it.
 */
record OperationSpec(String name, String summary, Schema input)
{
}
