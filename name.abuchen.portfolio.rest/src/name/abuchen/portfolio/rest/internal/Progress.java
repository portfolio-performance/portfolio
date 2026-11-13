package name.abuchen.portfolio.rest.internal;

import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;

/**
 * What a request reports while it runs, and how it learns that the caller no
 * longer wants the answer.
 * <p/>
 * Over MCP with a streamed response, a phase or a step becomes a
 * {@code notifications/progress} message, and a {@code notifications/cancelled}
 * - or a client that hung up - sets the flag. Everywhere else, and for every
 * REST request, it is {@link #NONE}: nothing is reported and nothing is ever
 * cancelled.
 * <p/>
 * A calculation finds its progress with {@link #current()}: the calculation
 * runs on a thread of its own, and passing the object through every handler
 * signature would add a parameter to methods that have nothing to report.
 */
public interface Progress
{
    Progress NONE = new Progress()
    {
        @Override
        public void phase(String message)
        {
            // nobody is listening
        }

        @Override
        public void step(int done, int total, String message)
        {
            // nobody is listening
        }

        @Override
        public boolean isCancelled()
        {
            return false;
        }
    };

    /** a stage of the request, e.g. waiting for a calculation slot */
    void phase(String message);

    /** how far a calculation over many items has come */
    void step(int done, int total, String message);

    boolean isCancelled();

    /**
     * Stops the work at this point when the caller gave up. Only the loops of
     * this API call it - the engines underneath never check, so a calculation
     * inside one of them runs to its end regardless.
     */
    default void checkCancelled()
    {
        if (isCancelled())
            throw new CancellationException("the client cancelled the request"); //$NON-NLS-1$
    }

    /** the progress of the calculation running on this thread, or {@link #NONE} */
    static Progress current()
    {
        var progress = ProgressHolder.CURRENT.get();
        return progress != null ? progress : NONE;
    }

    /** runs the work with this progress as {@link #current()}, and clears it afterwards */
    static <T> T runWith(Progress progress, Callable<T> work) throws Exception
    {
        var previous = ProgressHolder.CURRENT.get();
        ProgressHolder.CURRENT.set(progress);
        try
        {
            return work.call();
        }
        finally
        {
            if (previous == null)
                ProgressHolder.CURRENT.remove();
            else
                ProgressHolder.CURRENT.set(previous);
        }
    }
}

/** the thread's current progress; a pooled thread must not keep it past the work */
final class ProgressHolder
{
    static final ThreadLocal<Progress> CURRENT = new ThreadLocal<>();

    private ProgressHolder()
    {
    }
}
