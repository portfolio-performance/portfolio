package name.abuchen.portfolio.rest.internal.mcp;

/**
 * The {@code instructions} string returned at {@code initialize}. Sent although
 * only half-honoured: Claude Desktop discards it, Claude Code truncates it at
 * 2,048 characters, and this endpoint's client is unmeasured - which is a
 * reason to send it, not to rely on it.
 * <p/>
 * So every rule here is a duplicate, carried again where it bites, in a tool
 * description or in {@link McpEnvelope}'s notes. Ordered by what goes wrong
 * when the truncation cuts it: a lost edit first, then a return misreported by
 * two orders of magnitude, and vocabulary last.
 */
@SuppressWarnings("nls")
public final class McpInstructions
{
    /** Claude Code's truncation point; a test enforces it rather than trusting anyone to count */
    public static final int MAX_CHARS = 2048;

    public static final String INSTRUCTIONS = """
                    Read and edit the portfolio files open in the user's running Portfolio Performance desktop application \
                    — a live application on this machine, not a database. If it is closed or its REST API is off, nothing is reachable.

                    Start with pp_list_files. Every other tool needs a `file`, as an `id` or `alias` from that list; there is \
                    no default file. An empty list is not an error: only the user can share a file, under Preferences → MCP Server & REST API.

                    **Writes are not saved to disk.** pp_update_instrument and pp_delete_instrument change the file open in \
                    the application and leave it unsaved, indistinguishable from an edit the user made by hand. There is no \
                    save operation here — tell the user to save, or to close without saving to discard.

                    **Rates and weights are fractions, never percentages.** A `ttwror` of 0.0723 is 7.23%; a `weight` of \
                    0.6875 is 68.75%. Multiply by 100 before showing one.

                    A `null` figure is undefined for that interval, not zero — say it is unavailable. Date-times such as a \
                    trade's `start` carry no timezone and are local to the user; never convert them or read them as UTC. \
                    Numbers arrive unrounded, as the application computes them.

                    A `warnings` array that is not empty means part of the computation failed. Name what was skipped rather \
                    than presenting the totals as whole.

                    "Not found" does not mean "does not exist": a file or entity the user has not shared answers exactly like \
                    one that never existed. Ask them to enable it rather than concluding it is gone.

                    Vocabulary: an *instrument* is what the application calls a security; a *cash account* is what it calls \
                    an account; an *investment account* is what it calls a portfolio, and holds positions only, with its \
                    money in a separate cash account it points to as `referenceCashAccount`.""";

    private McpInstructions()
    {
    }
}
