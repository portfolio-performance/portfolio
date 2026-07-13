package name.abuchen.portfolio.rest.spi;

import java.util.List;
import java.util.concurrent.Callable;

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
}
