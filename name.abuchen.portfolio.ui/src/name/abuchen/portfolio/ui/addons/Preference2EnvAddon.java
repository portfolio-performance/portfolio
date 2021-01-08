package name.abuchen.portfolio.ui.addons;

import javax.inject.Inject;

import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.core.di.extensions.Preference;

import name.abuchen.portfolio.online.Factory;
import name.abuchen.portfolio.online.impl.AlphavantageQuoteFeed;
import name.abuchen.portfolio.online.impl.DivvyDiaryDividendFeed;
import name.abuchen.portfolio.online.impl.FinnhubQuoteFeed;
import name.abuchen.portfolio.online.impl.QuandlQuoteFeed;
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.util.FormatHelper;
import name.abuchen.portfolio.util.TradeCalendarManager;

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
    public void setFinnhubApiKey(@Preference(value = UIConstants.Preferences.FINNHUB_API_KEY) String finnhubApiKey)
    {
        ((FinnhubQuoteFeed) Factory.getQuoteFeedProvider(FinnhubQuoteFeed.ID)).setApiKey(finnhubApiKey);
    }

    @Inject
    @Optional
    public void setDivvyDiaryApiKey(
                    @Preference(value = UIConstants.Preferences.DIVVYDIARY_API_KEY) String divvyDiaryApiKey)
    {
        Factory.getDividendFeed(DivvyDiaryDividendFeed.class).setApiKey(divvyDiaryApiKey);
    }

    @Inject
    @Optional
    public void setDefaultCalendar(@Preference(value = UIConstants.Preferences.CALENDAR) String defaultCalendarCode)
    {
        // pass calendar preferences into TradeCalendarManager (which is
        // statically created)

        TradeCalendarManager.setDefaultCalendarCode(defaultCalendarCode);
    }

    @Inject
    public void setSharesPrecision(@Preference(value = UIConstants.Preferences.FORMAT_SHARES_DIGITS) int sharesPrecision)
    {
        FormatHelper.setSharesDisplayPrecision(sharesPrecision);
    }
}
