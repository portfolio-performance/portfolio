package name.abuchen.portfolio.cli.command;

/**
 * One failed check of a write operation's input.
 *
 * @param field
 *            the path of the member that failed, such as
 *            <code>units[0].amount</code>, or empty for the operation itself
 * @param code
 *            the published failure code, such as <code>required</code>
 * @param check
 *            the core check that raised the failure, or null
 */
public record FieldFailure(String field, String code, String message, String check)
{
}
