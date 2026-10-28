package name.abuchen.portfolio.cli.command;

/**
 * One failed check of a write operation's input.
 *
 * @param field
 *            the path of the member that failed, such as
 *            <code>units[0].amount</code>, or empty for the operation itself
 * @param code
 *            the failure code, one the operation declares (see
 *            {@link OperationSpec#failureCodes()})
 * @param check
 *            the core check that raised the failure, or null; it is core's
 *            name, not a declared code
 */
public record FieldFailure(String field, ErrorCode code, String message, String check)
{
}
