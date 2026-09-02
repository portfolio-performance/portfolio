package name.abuchen.portfolio.ui.update;

/**
 * The update that the background check has found. An instance is published into
 * the application context by the {@link UpdateIndicatorAddon} so that the
 * indicator in the tool bar can offer to install it.
 */
public class AvailableUpdate
{
    private final UpdateHelper updateHelper;
    private final NewVersion newVersion;

    public AvailableUpdate(UpdateHelper updateHelper, NewVersion newVersion)
    {
        this.updateHelper = updateHelper;
        this.newVersion = newVersion;
    }

    /**
     * Shows the version history found by the background check - without going
     * to the update site again, so that the dialog opens immediately - and
     * installs the update if the user confirms. Only the installation itself
     * resolves the update again, because the version found earlier may have
     * been replaced in the meantime.
     */
    public UpdateHelper.Outcome promptAndInstallLatest()
    {
        return updateHelper.promptAndInstallLatest(newVersion);
    }
}
