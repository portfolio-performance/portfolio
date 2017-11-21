package name.abuchen.portfolio.ui.addons;

import javax.inject.Inject;

import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.core.di.extensions.Preference;

import name.abuchen.portfolio.online.Factory;
import name.abuchen.portfolio.online.impl.AlphavantageQuoteFeed;
import name.abuchen.portfolio.ui.UIConstants;

@SuppressWarnings("restriction")
public class Preference2EnvAddon
{

    @Inject
    @Optional
    public void setAlphavantageApiKey(
                    @Preference(value = UIConstants.Preferences.ALPHAVANTAGE_API_KEY) String alphavantageApiKey)
    {
        // this is a hack to pass the eclipse-based preference via environment
        // to the AlphavantageQuoteFeed implementation which is not created via
        // dependency injection but via Java Service Locator.
        
        AlphavantageQuoteFeed quoteFeed = (AlphavantageQuoteFeed)Factory.getQuoteFeedProvider(AlphavantageQuoteFeed.ID);
        quoteFeed.setApiKey(alphavantageApiKey);
    }
}
