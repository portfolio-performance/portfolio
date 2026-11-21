package name.abuchen.portfolio.rest.spi;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;

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
     * Starts the application's own price update for exactly these securities -
     * the same job its "Update quotes" menu item schedules, with its own progress
     * dialog and its own {@code markDirty()} when a quote arrives. Called on the
     * UI thread, and returns as soon as the job is scheduled rather than when it
     * is done: the update outlives the request that asked for it, which is why the
     * caller is told 202 and not 200.
     */
    void updateQuotes(Client client, List<Security> securities);

    /**
     * The same for the whole file: everything the plain menu item covers, indices
     * and exchange rates along with the securities.
     */
    void updateAllQuotes(Client client);

    /**
     * Asks the user to approve an API access request. Called on an HTTP worker
     * thread and must not block; the decision is reported asynchronously
     * through the request object.
     */
    void requestApiAccessApproval(ApiAccessRequest request);

    /**
     * Asks the user to approve a change to a portfolio file, before anything is
     * touched. Called on an HTTP worker thread and must not block; the decision
     * is reported asynchronously through the request object, and a request left
     * unanswered ends in its expiry - the change is then refused as unanswered,
     * never applied silently. Reads never come here.
     */
    void requestModelWriteApproval(WriteConfirmation request);
}
