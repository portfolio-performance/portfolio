package name.abuchen.portfolio.ui.jobs.priceupdate;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import name.abuchen.portfolio.online.AuthenticationExpiredException;
import name.abuchen.portfolio.online.FeedConfigurationException;
import name.abuchen.portfolio.online.RateLimitExceededException;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;

/* package */ final class RunTaskGroupJob extends Job
{
    private final List<Task> tasks;
    private final PriceUpdateRequest request;

    RunTaskGroupJob(String groupingCriterion, List<Task> tasks, PriceUpdateRequest request)
    {
        super(groupingCriterion);
        this.tasks = tasks;
        this.request = request;

        setRule(new GroupingCriterionSchedulingRule(groupingCriterion));
    }

    @Override
    protected IStatus run(IProgressMonitor monitor)
    {
        // create a mutable copy
        var candidates = new ArrayList<Task>(tasks);

        // number of attempts before failing permanently

        // this is not 100% correct because the list of tasks could contain
        // different feeds. However, for now only CoinGecko has a different
        // number of attempts and CoinGecko is grouped into one list
        int maxAttempts = tasks.getFirst().getFeed().getMaxRateLimitAttempts();

        while (!candidates.isEmpty())
        {
            var task = candidates.removeFirst();

            task.status.setStatus(UpdateStatus.LOADING, null);

            try
            {
                var status = task.update();

                task.status.setStatus(status, null);

                task.security.getEphemeralData().touchFeedLastUpdate();

                if (status == UpdateStatus.MODIFIED)
                    request.markDirty();
            }
            catch (AuthenticationExpiredException e)
            {
                task.status.setStatus(UpdateStatus.ERROR, Messages.MsgAuthenticationExpired);
                for (var c : candidates)
                    c.status.setStatus(UpdateStatus.ERROR, Messages.MsgAuthenticationExpired);

                // stop processing further tasks
                return Status.OK_STATUS;
            }
            catch (FeedConfigurationException e)
            {
                task.security.getEphemeralData().setHasPermanentError();
                task.status.setStatus(UpdateStatus.ERROR, e.getMessage());

                PortfolioPlugin.log(MessageFormat.format(Messages.MsgInstrumentWithConfigurationIssue,
                                task.security.getName()), e);
            }
            catch (RateLimitExceededException e)
            {
                maxAttempts--;

                if (maxAttempts >= 0 && e.getRetryAfter().isPositive())
                {
                    // add candidate back to the list to retry later
                    candidates.addFirst(task);

                    task.status.setStatus(UpdateStatus.WAITING, MessageFormat.format(
                                    Messages.MsgRateLimitExceededAndRetrying, task.security.getName(), maxAttempts));

                    try
                    {
                        Thread.sleep(e.getRetryAfter().toMillis());
                    }
                    catch (InterruptedException ie)
                    {
                        Thread.currentThread().interrupt();
                    }
                }
                else
                {
                    task.status.setStatus(UpdateStatus.ERROR,
                                    MessageFormat.format(Messages.MsgRateLimitExceeded, task.security.getName()));
                    for (var c : candidates)
                        c.status.setStatus(UpdateStatus.ERROR,
                                        MessageFormat.format(Messages.MsgRateLimitExceeded, c.security.getName()));

                    // stop processing further tasks
                    return Status.OK_STATUS;
                }
            }
            catch (Exception e)
            {
                task.status.setStatus(UpdateStatus.ERROR, e.getMessage());
                PortfolioPlugin.log(e);
            }
        }

        return Status.OK_STATUS;
    }
}
