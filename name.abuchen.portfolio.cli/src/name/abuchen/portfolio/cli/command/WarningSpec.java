package name.abuchen.portfolio.cli.command;

/** Declares a warning code a command can emit; <code>--help</code> publishes it. */
public record WarningSpec(String code, String description)
{
}
