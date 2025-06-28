package name.abuchen.portfolio.ui.jobs.priceupdate;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import name.abuchen.portfolio.model.Client;

/**
 * Singleton class to manage the progress of price updates for different
 * clients. It allows registering listeners that will be notified about progress
 * updates and completion of price update jobs.
 */
public class PriceUpdateProgress // NOSONAR
{
    public interface Listener
    {
        void onProgress(PriceUpdateSnapshot status);
    }

    private static final PriceUpdateProgress INSTANCE = new PriceUpdateProgress();

    public static PriceUpdateProgress getInstance()
    {
        return INSTANCE;
    }

    // latest job per client
    private final Map<Client, UpdatePricesJob> latestJobs = new ConcurrentHashMap<>();

    // listeners per client
    private final Map<Client, Set<Listener>> listeners = new ConcurrentHashMap<>();

    private PriceUpdateProgress()
    {
    }

    public void register(Client client, Listener listener)
    {
        listeners.computeIfAbsent(client, k -> ConcurrentHashMap.newKeySet()).add(listener);
    }

    public void unregister(Client client, Listener listener)
    {
        listeners.compute(client, (key, set) -> {
            if (set == null)
                return null;
            set.remove(listener);
            return set.isEmpty() ? null : set;
        });
    }

    public void setLatestJob(Client client, UpdatePricesJob job)
    {
        latestJobs.put(client, job);
    }

    public boolean isCurrent(UpdatePricesJob job)
    {
        return job.equals(latestJobs.get(job.getClient()));
    }

    public void notifyProgress(UpdatePricesJob job, PriceUpdateSnapshot snapshot)
    {
        if (!isCurrent(job))
            return;
        Set<Listener> clientListeners = listeners.get(job.getClient());
        if (clientListeners != null)
        {
            for (Listener listener : clientListeners)
                listener.onProgress(snapshot);
        }
    }

    public void notifyFinished(UpdatePricesJob job, PriceUpdateSnapshot snapshot)
    {
        if (!isCurrent(job))
            return;
        Set<Listener> clientListeners = listeners.get(job.getClient());
        if (clientListeners != null)
        {
            for (Listener listener : clientListeners)
                listener.onProgress(snapshot);
        }

        latestJobs.compute(job.getClient(), (c, currentJob) -> (job.equals(currentJob) ? null : currentJob));
    }
}
