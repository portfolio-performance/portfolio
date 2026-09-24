package name.abuchen.portfolio.rest.spi;

import java.io.IOException;
import java.time.Duration;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;

/**
 * A portfolio file currently open in the application. Implemented by the UI
 * plugin, backed by ClientInput.
 */
public interface OpenFile
{
    /** absolute file path; unique per machine and the identity key */
    String getPath();

    String getLabel();

    Client getClient();

    /**
     * The host's exchange rate factory for this file. The factory registers a
     * listener on the client, so its lifecycle must be owned by whoever owns
     * the file - the REST plugin must never construct (and thereby leak) one
     * per request.
     */
    ExchangeRateProviderFactory getExchangeRateProviderFactory();

    /** true if the file has unsaved changes; must be called on the UI thread */
    boolean isDirty();

    /**
     * Persists the file in its current format at its current path, without
     * any user interaction (no "save as", no dialogs); must be called on the
     * UI thread.
     */
    void save() throws IOException;

    /**
     * Waits until the background jobs that change this file (for example the
     * online price update started when the file was opened) are done, or the
     * timeout elapsed. Must not be called on the UI thread: the jobs need it to
     * finish.
     *
     * @return true if no such job is running or waiting to run
     */
    default boolean awaitBackgroundUpdates(Duration timeout) throws InterruptedException
    {
        return true;
    }
}
