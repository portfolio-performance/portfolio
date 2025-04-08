package name.abuchen.portfolio.ui.editor;

public interface ClientInputListener
{
    /**
     * Called if loading of the Client has started.
     */
    default void onLoading(int totalWork, int worked)
    {
    }

    /**
     * Called if the Client has been successfully loaded.
     */
    default void onLoaded()
    {
    }

    /**
     * Called with error messages when loading a Client file.
     */
    default void onError(String message)
    {
    }

    /**
     * Called if the Client has been saved.
     */
    default void onSaved()
    {
    }

    /**
     * Called whenever the Client is disposed.
     */
    default void onDisposed()
    {
    }

    /**
     * Called if the dirty state of the Client has changed.
     */
    default void onDirty(boolean isDirty)
    {
    }

    /**
     * Called if a calculation of metrics is needed. This can happen even if the
     * dirty state of the Client does not change, for example if the exchange
     * rates are updated.
     */
    default void onRecalculationNeeded()
    {
    }
}
