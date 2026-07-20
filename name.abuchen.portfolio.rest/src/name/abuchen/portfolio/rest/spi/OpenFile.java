package name.abuchen.portfolio.rest.spi;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;

/**
 * A portfolio file currently open in the application. Implemented by the UI
 * plugin, backed by ClientInput.
 */
public interface OpenFile
{
    /** absolute file path; unique per machine and the identity key */
    String getPath();

    String getLabel();

    Client getClient();

    /**
     * The host's exchange rate factory for this file. The factory registers a
     * listener on the client, so its lifecycle must be owned by whoever owns
     * the file - the REST plugin must never construct (and thereby leak) one
     * per request.
     */
    ExchangeRateProviderFactory getExchangeRateProviderFactory();
}
