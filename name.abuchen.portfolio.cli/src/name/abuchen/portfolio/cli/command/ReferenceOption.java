package name.abuchen.portfolio.cli.command;

/**
 * An option whose value is a reference to one entity of a kind, read with
 * {@link CommandContext#reference}. Commands declare its {@link #spec()}; a
 * command that declares one can fail with the reference error codes.
 */
public record ReferenceOption<T>(OptionSpec spec, Reference<T> kind)
{
    public static <T> ReferenceOption<T> of(String name, String description, Reference<T> kind)
    {
        return new ReferenceOption<>(
                        new OptionSpec(name, "ref", description, new OptionSpec.Reference(kind.noun()), false),
                        kind);
    }

    /** The same option, but the parser rejects a command line without it. */
    public ReferenceOption<T> asRequired()
    {
        return new ReferenceOption<>(spec.asRequired(), kind);
    }

    public String name()
    {
        return spec.name();
    }
}
