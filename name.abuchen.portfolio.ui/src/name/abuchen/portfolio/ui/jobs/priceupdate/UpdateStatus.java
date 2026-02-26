package name.abuchen.portfolio.ui.jobs.priceupdate;

import name.abuchen.portfolio.ui.Messages;

public enum UpdateStatus
{
    WAITING(Messages.StatusWaiting, false), //
    LOADING(Messages.StatusLoading, false), //
    MODIFIED(Messages.StatusUpdated, true), //
    UNMODIFIED(Messages.StatusNoChange, true), //
    ERROR(Messages.LabelError, true), //
    SKIPPED(Messages.StatusSkipped, true);

    public final String label;
    public final boolean isTerminal;

    UpdateStatus(String label, boolean isTerminal)
    {
        this.label = label;
        this.isTerminal = isTerminal;
    }

    @Override
    public String toString()
    {
        return label;
    }
}
