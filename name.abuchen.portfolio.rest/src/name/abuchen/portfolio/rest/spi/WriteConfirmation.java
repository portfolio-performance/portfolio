package name.abuchen.portfolio.rest.spi;

import java.time.Instant;

/**
 * A change a local program is about to make to a portfolio file, to be answered
 * by the user <em>before</em> anything is touched.
 *
 * What it carries are facts, not sentences: the operation, the file, the thing
 * changed and where it is going. The prompt is written by the application, which
 * is where the language lives; a caller of this interface is not asked to know
 * German. Names taken from the model travel as they are.
 *
 * The hosting application shows the request and reports the decision by calling
 * exactly one of the decision methods; late or repeated decisions are silently
 * ignored, because the request may have expired in the meantime. A request that
 * is never answered is not a veto - the change is refused as unanswered, and the
 * caller is told to try again later.
 */
public interface WriteConfirmation
{
    /**
     * What is about to happen, in the words the interface names operations with:
     * the MCP tool for a tool call, or the route for a plain request. Stable, so
     * that "allow for this session" can be remembered per operation.
     */
    String getOperation();

    /** the file the change would land in, as the user knows it */
    String getFileLabel();

    /**
     * Which kind of thing is changed, as the API names it: {@code instrument},
     * {@code cash-account}, or {@code unknown} when the request is about
     * something the API does not put a kind on.
     */
    String getSubjectType();

    /** the name the model gives that thing; never null, possibly the uuid alone */
    String getSubjectName();

    /** where the thing is going, when the change has a where: a class, a list */
    default String getTargetName()
    {
        return null;
    }

    /** one line of what else is decided: a weight, the fields a patch sets */
    default String getDetail()
    {
        return null;
    }

    /** when the request expires; the prompt should dismiss itself at this time */
    Instant getExpiresAt();

    /** apply this one change */
    void allowOnce();

    /**
     * apply this change and every further one of the same operation until the
     * application quits - the way out of being asked for every step of a long
     * sequence. It is remembered per operation, not as a blanket yes.
     */
    void allowForSession();

    /** refuse; the file is left untouched */
    void decline();
}
