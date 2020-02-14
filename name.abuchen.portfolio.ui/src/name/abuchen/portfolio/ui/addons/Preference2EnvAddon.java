package name.abuchen.portfolio.ui.addons;

import javax.inject.Inject;

import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.core.di.extensions.Preference;

import name.abuchen.portfolio.online.Factory;
import name.abuchen.portfolio.online.impl.AlphavantageQuoteFeed;
import name.abuchen.portfolio.online.impl.QuandlQuoteFeed;
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.util.TradeCalendarManager;

@SuppressWarnings("restriction")
public class Preference2EnvAddon
{

    @Inject
    @Optional
    public void setAlphavantageApiKey(
                    @Preference(value = UIConstants.Preferences.ALPHAVANTAGE_API_KEY) String alphavantageApiKey,
                    @Preference(value = UIConstants.Preferences.ALPHAVANTAGE_CALL_FREQUENCY_LIMIT) int callFrequencyLimit)
    {
        // this is a hack to pass the eclipse-based preference via environment
        // to the AlphavantageQuoteFeed implementation which is not created via
        // dependency injection but via Java Service Locator.

        AlphavantageQuoteFeed quoteFeed = (AlphavantageQuoteFeed) Factory
                        .getQuoteFeedProvider(AlphavantageQuoteFeed.ID);
        quoteFeed.setApiKey(alphavantageApiKey);
        quoteFeed.setCallFrequencyLimit(callFrequencyLimit);
    }

    @Inject
    @Optional
    public void setQuandlApiKey(@Preference(value = UIConstants.Preferences.QUANDL_API_KEY) String quandlApiKey)
    {
        ((QuandlQuoteFeed) Factory.getQuoteFeedProvider(QuandlQuoteFeed.ID)).setApiKey(quandlApiKey);
    }

    @Inject
    @Optional
    public void setDefaultCalendar(@Preference(value = UIConstants.Preferences.CALENDAR) String defaultCalendarCode)
    {
        // pass calendar preferences into TradeCalendarManager (which is
        // statically created)

        TradeCalendarManager.setDefaultCalendarCode(defaultCalendarCode);
    }

}
