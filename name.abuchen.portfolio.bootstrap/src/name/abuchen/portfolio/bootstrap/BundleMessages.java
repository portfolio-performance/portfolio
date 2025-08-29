package name.abuchen.portfolio.bootstrap;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class BundleMessages
{
    public interface Label
    {
        @SuppressWarnings("nls")
        public interface Command // NOSONAR
        {
            String updateQuotesActiveSecurities = "command.updateQuotesActiveSecurities.name"; // NOSONAR
            String updateQuotesHoldings = "command.updateQuotesHoldings.name"; // NOSONAR
            String updateQuotesWatchlist = "command.updateQuotesWatchlist.name"; // NOSONAR
            String importTaxonomy = "command.import.taxonomy"; // NOSONAR
            String exportTaxonomy = "command.export.taxonomy"; // NOSONAR
        }
    }

    private static final ResourceBundle BUNDLE = ResourceBundle.getBundle("OSGI-INF.l10n.bundle"); //$NON-NLS-1$

    private BundleMessages()
    {
    }

    public static String getString(String key)
    {
        try
        {
            return BUNDLE.getString(key);
        }
        catch (MissingResourceException e)
        {
            return '!' + key + '!';
        }
    }
}
