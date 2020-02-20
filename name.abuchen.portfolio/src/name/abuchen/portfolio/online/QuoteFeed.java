package name.abuchen.portfolio.online;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import name.abuchen.portfolio.model.Exchange;
import name.abuchen.portfolio.model.LatestSecurityPrice;
import name.abuchen.portfolio.model.Security;

public interface QuoteFeed
{
    String MANUAL = "MANUAL"; //$NON-NLS-1$

    /**
     * Returns the technical identifier of the quote feed.
     */
    String getId();

    /**
     * Returns the display name of the quote feed.
     */
    String getName();

    /**
     * Returns the help URL to be shown to the user.
     */
    default Optional<String> getHelpURL()
    {
        return Optional.empty();
    }

    /**
     * Update the latest quote of the given securities.
     * 
     * @param securities
     *            the securities to be updated with the latest quote.
     * @param errors
     *            any errors that occur during the update of the quotes are
     *            added to this list.
     * @return true if at least one quote was updated.
     */
    boolean updateLatestQuotes(Security security, List<Exception> errors);

    /**
     * Update the historical quotes of the given security.
     * 
     * @param securities
     *            the security for which the historical quotes are to be
     *            updated.
     * @param errors
     *            any errors that occur during the update of the quotes are
     *            added to this list.
     * @return true if at least one quote was updated.
     */
    boolean updateHistoricalQuotes(Security security, List<Exception> errors);

    List<LatestSecurityPrice> getHistoricalQuotes(Security security, LocalDate start, List<Exception> errors);

    List<LatestSecurityPrice> getHistoricalQuotes(String response, List<Exception> errors);

    List<Exchange> getExchanges(Security subject, List<Exception> errors);
}
