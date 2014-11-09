package name.abuchen.portfolio.online;

import java.io.IOException;
import java.util.Date;
import java.util.List;

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
     * Update the latest quote of the given securities.
     * 
     * @param securities
     *            the securities to be updated with the latest quote.
     * @param errors
     *            any errors that occur during the update of the quotes are
     *            added to this list.
     * @return true if at least one quote was updated.
     * @throws IOException
     */
    boolean updateLatestQuotes(List<Security> securities, List<Exception> errors);

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
     * @throws IOException
     */
    boolean updateHistoricalQuotes(Security security, List<Exception> errors);

    List<LatestSecurityPrice> getHistoricalQuotes(Security security, Date start, List<Exception> errors);

    List<LatestSecurityPrice> getHistoricalQuotes(String response, List<Exception> errors);

    List<Exchange> getExchanges(Security subject, List<Exception> errors);
}
