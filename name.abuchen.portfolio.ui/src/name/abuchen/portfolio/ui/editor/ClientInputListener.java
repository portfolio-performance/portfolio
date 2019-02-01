package name.abuchen.portfolio.ui.editor;

public interface ClientInputListener
{
    /**
     * Called if loading of the Client has started.
     */
    void onLoading(int totalWork, int worked);

    /**
     * Called if the Client has been successfully loaded.
     */
    void onLoaded();

    /**
     * Called with error messages when loading a Client file.
     */
    void onError(String message);

    /**
     * Called if the Client has been saved.
     */
    void onSaved();

    /**
     * Called if the dirty state of the Client has changed.
     */
    void onDirty(boolean isDirty);

    /**
     * Called if a calculation of metrics is needed. This can happen even if the
     * dirty state of the Client does not change, for example if the exchange
     * rates are updated.
     */
    void onRecalculationNeeded();
}
