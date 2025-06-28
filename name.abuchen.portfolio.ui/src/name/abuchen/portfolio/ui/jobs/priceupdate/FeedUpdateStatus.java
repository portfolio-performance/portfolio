package name.abuchen.portfolio.ui.jobs.priceupdate;

public class FeedUpdateStatus
{
    private volatile UpdateStatus status = UpdateStatus.WAITING;
    private volatile String message = null;
    
    public FeedUpdateStatus(UpdateStatus status)
    {
        this.status = status;
    }

    public synchronized void setStatus(UpdateStatus status, String message)
    {
        this.status = status;
        this.message = message;
    }

    public UpdateStatus getStatus()
    {
        return status;
    }

    public String getMessage()
    {
        return message;
    }
}
