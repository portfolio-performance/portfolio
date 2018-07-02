package name.abuchen.portfolio.money;

import java.io.IOException;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;

import name.abuchen.portfolio.model.Client;

/**
 * Provides a number of time series of exchange rates.
 */
public interface ExchangeRateProvider
{
    /**
     * Returns a descriptive name for the source of exchange rates.
     */
    String getName();

    /**
     * Loads the stored exchange rates from local storage.
     */
    default void load(IProgressMonitor monitor) throws IOException
    {}

    /**
     * Performs an (online) update of the exchange rates.
     */
    default void update(IProgressMonitor monitor) throws IOException
    {}

    /**
     * Saves the exchange rates in local storage.
     */
    default void save(IProgressMonitor monitor) throws IOException
    {}

    /**
     * Returns the available exchange rates provided by this provider.
     * 
     * @return available time series
     */
    List<ExchangeRateTimeSeries> getAvailableTimeSeries(Client client);
}
