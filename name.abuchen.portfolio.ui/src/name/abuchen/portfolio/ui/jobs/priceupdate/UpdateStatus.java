package name.abuchen.portfolio.ui.jobs.priceupdate;

public enum UpdateStatus
{
    WAITING(false), LOADING(false), MODIFIED(true), UNMODIFIED(true), ERROR(true), SKIPPED(true);

    public final boolean isTerminal;

    UpdateStatus(boolean isTerminal)
    {
        this.isTerminal = isTerminal;
    }
}
