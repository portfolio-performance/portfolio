package name.abuchen.portfolio.rest.spi;

import java.io.IOException;
import java.nio.file.Path;
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

    /** runs the callable on the UI thread and returns its result */
    <T> T syncExec(Callable<T> callable) throws Exception;

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

    /**
     * Opens the portfolio file at the given path in the application, as the
     * File/Open command does, but without showing any dialog. Must be called
     * on the UI thread and must not block: loading may continue in the
     * background. The returned future completes with the open file once its
     * data is loaded, or exceptionally with an {@link IOException} if loading
     * fails.
     *
     * @throws PasswordRequiredException
     *             if the file is encrypted - opening it needs the user to
     *             enter the password
     * @throws IOException
     *             if the file does not exist or cannot be opened
     */
    CompletableFuture<OpenFile> openFile(Path path) throws IOException;
}
