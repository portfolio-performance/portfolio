package name.abuchen.portfolio.money;

import java.io.IOException;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;

/**
 * Provides a number of time series of exchange rates.
 */
public interface ExchangeRateProvider
{
    /**
     * Returns a descriptive name for the source of exchange rates.
     * @return String
     */
    String getName();

    /**
     * Initializes the exchange rate provider with the factory.
     */
    void init(ExchangeRateProviderFactory factory);

    /**
     * Loads the stored exchange rates from local storage.
     */
    void load(IProgressMonitor monitor) throws IOException;

    /**
     * Performs an (online) update of the exchange rates.
     */
    void update(IProgressMonitor monitor) throws IOException;

    /**
     * Saves the exchange rates in local storage.
     */
    void save(IProgressMonitor monitor) throws IOException;

    /**
     * Returns the available exchange rates provided by this provider.
     * @return List &ltExchangeRateTimeSeries;&gt;
     */
    List<ExchangeRateTimeSeries> getAvailableTimeSeries();

    /**
     * Returns a exchange rate series for the given base and term currency if
     * available.
     * @return List &ltExchangeRateTimeSeries;&gt;
     */
    ExchangeRateTimeSeries getTimeSeries(String baseCurrency, String termCurrency);
}
