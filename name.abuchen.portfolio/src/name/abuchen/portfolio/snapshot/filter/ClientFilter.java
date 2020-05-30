package name.abuchen.portfolio.snapshot.filter;

import name.abuchen.portfolio.model.Client;

/**
 * Filters accounts, portfolios, or transactions in order to calculate
 * performance indicators on a sub set of the whole client. Use
 * {@link ReadOnlyClient}, {@link ReadOnlyPortfolio}, and
 * {@link ReadOnlyAccount} to make sure that any model objects accidentally
 * leaked to the UI cannot be used.
 */
@FunctionalInterface
public interface ClientFilter
{
    static final ClientFilter NO_FILTER = client -> client;

    Client filter(Client client);
}
