package name.abuchen.portfolio.cli;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Taxonomy;

/**
 * An option whose value is a reference to one entity of a kind, read with
 * {@link CommandContext#reference}. Commands declare its {@link #spec()}; a
 * command that declares one can fail with the reference error codes.
 */
public record ReferenceOption<T>(OptionSpec spec, References.Kind<T> kind)
{
    public static ReferenceOption<Security> instrument(String name, String description)
    {
        return of(name, description, References.INSTRUMENT);
    }

    public static ReferenceOption<Portfolio> investmentAccount(String name, String description)
    {
        return of(name, description, References.INVESTMENT_ACCOUNT);
    }

    public static ReferenceOption<Account> cashAccount(String name, String description)
    {
        return of(name, description, References.CASH_ACCOUNT);
    }

    static ReferenceOption<Taxonomy> taxonomy(String name, String description)
    {
        return of(name, description, References.TAXONOMY);
    }

    private static <T> ReferenceOption<T> of(String name, String description, References.Kind<T> kind)
    {
        return new ReferenceOption<>(
                        new OptionSpec(name, "ref", description, new OptionSpec.Reference(kind.label()), false),
                        kind);
    }

    /** The same option, but the parser rejects a command line without it. */
    ReferenceOption<T> asRequired()
    {
        return new ReferenceOption<>(spec.asRequired(), kind);
    }

    public String name()
    {
        return spec.name();
    }
}
