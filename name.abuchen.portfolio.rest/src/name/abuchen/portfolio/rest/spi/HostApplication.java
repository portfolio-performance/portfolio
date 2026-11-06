package name.abuchen.portfolio.rest.spi;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;

/**
 * Services the hosting application provides to the REST plugin. Implemented
 * by the UI plugin; the REST plugin must not depend on UI bundles.
 */
public interface HostApplication
{
    List<OpenFile> listOpenFiles();

    /**
     * Queues the callable for the UI thread and returns at once; the future
     * completes with its result, or with what it threw. Called on the UI thread
     * itself, it runs the callable right away instead - queueing it and
     * waiting would wait forever.
     * <p/>
     * Asynchronous rather than a blocking {@code syncExec}, because a caller
     * that waits on the future parks without holding a thread: SWT's
     * {@code syncExec} waits in {@code synchronized}/{@code Object.wait()},
     * which on Java 21 pins a virtual thread and so costs an OS thread for as
     * long as it waits, and keeps waiting through an interrupt. Abandoning the
     * future leaves nothing behind but the queued callable.
     */
    <T> CompletableFuture<T> asyncExec(Callable<T> callable);

    /**
     * true if the user is in the middle of an uncommitted edit — an
     * application-modal dialog is open or an in-place cell editor is active;
     * must be called on the UI thread
     */
    boolean isUserEditing();

    /**
     * Asks the user to approve an API access request. Called on an HTTP worker
     * thread and must not block; the decision is reported asynchronously
     * through the request object.
     */
    void requestApiAccessApproval(ApiAccessRequest request);
}
