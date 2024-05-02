package name.abuchen.portfolio.ui.addons;

import jakarta.inject.Inject;

import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.core.di.extensions.Preference;

import name.abuchen.portfolio.online.Factory;
import name.abuchen.portfolio.online.impl.AlphavantageQuoteFeed;
import name.abuchen.portfolio.online.impl.CoinGeckoQuoteFeed;
import name.abuchen.portfolio.online.impl.DivvyDiaryDividendFeed;
import name.abuchen.portfolio.online.impl.DivvyDiarySearchProvider;
import name.abuchen.portfolio.online.impl.EODHistoricalDataQuoteFeed;
import name.abuchen.portfolio.online.impl.EODHistoricalDataSearchProvider;
import name.abuchen.portfolio.online.impl.FinnhubQuoteFeed;
import name.abuchen.portfolio.online.impl.FinnhubSearchProvider;
import name.abuchen.portfolio.online.impl.LeewayQuoteFeed;
import name.abuchen.portfolio.online.impl.LeewaySearchProvider;
import name.abuchen.portfolio.online.impl.QuandlQuoteFeed;
import name.abuchen.portfolio.online.impl.TwelveDataQuoteFeed;
import name.abuchen.portfolio.online.impl.TwelveDataSearchProvider;
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.dialogs.transactions.PresetValues;
import name.abuchen.portfolio.util.FormatHelper;
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
        Factory.getSearchProvider(FinnhubSearchProvider.class).setApiKey(finnhubApiKey);
    }

    @Inject
    @Optional
    public void setLeewayApiKey(@Preference(value = UIConstants.Preferences.LEEWAY_API_KEY) String leewayApiKey)
    {
        Factory.getQuoteFeed(LeewayQuoteFeed.class).setApiKey(leewayApiKey);
        Factory.getSearchProvider(LeewaySearchProvider.class).setApiKey(leewayApiKey);
    }

    @Inject
    @Optional
    public void setTwelveDataApiKey(@Preference(value = UIConstants.Preferences.TWELVEDATA_API_KEY) String twelvedataApiKey)
    {
        Factory.getQuoteFeed(TwelveDataQuoteFeed.class).setApiKey(twelvedataApiKey);
        Factory.getSearchProvider(TwelveDataSearchProvider.class).setApiKey(twelvedataApiKey);
    }

    @Inject
    @Optional
    public void setDivvyDiaryApiKey(@Preference(value = UIConstants.Preferences.DIVVYDIARY_API_KEY) String divvyDiaryApiKey)
    {
        Factory.getDividendFeed(DivvyDiaryDividendFeed.class).setApiKey(divvyDiaryApiKey);
        Factory.getSearchProvider(DivvyDiarySearchProvider.class).setApiKey(divvyDiaryApiKey);
    }

    @Inject
    @Optional
    public void setEODHistoricalDataApiKey(
                    @Preference(value = UIConstants.Preferences.EOD_HISTORICAL_DATA_API_KEY) String eodhistoricialdataApiKey)
    {
        ((EODHistoricalDataQuoteFeed) Factory.getQuoteFeedProvider(EODHistoricalDataQuoteFeed.ID))
                        .setApiKey(eodhistoricialdataApiKey);
        Factory.getSearchProvider(EODHistoricalDataSearchProvider.class).setApiKey(eodhistoricialdataApiKey);
    }

    @Inject
    @Optional
    public void setCoingeckoApiKey(@Preference(value = UIConstants.Preferences.COINGECKO_API_KEY) String apiKey)
    {
        Factory.getQuoteFeed(CoinGeckoQuoteFeed.class).setApiKey(apiKey);
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
    public void setSharesPrecision(
                    @Preference(value = UIConstants.Preferences.FORMAT_SHARES_DIGITS) int sharesPrecision)
    {
        FormatHelper.setSharesDisplayPrecision(sharesPrecision);
    }

    @Inject
    public void setQuotePrecision(
                    @Preference(value = UIConstants.Preferences.FORMAT_CALCULATED_QUOTE_DIGITS) int quotePrecision)
    {
        FormatHelper.setCalculatedQuoteDisplayPrecision(quotePrecision);
    }

    @Inject
    public void setDisplayBaseCurrencyCode(
                    @Preference(value = UIConstants.Preferences.ALWAYS_DISPLAY_CURRENCY_CODE) boolean alwaysDisplayCurrencyCode)
    {
        FormatHelper.setAlwaysDisplayCurrencyCode(alwaysDisplayCurrencyCode);
    }

    @Inject
    public void setDisplayPA(
                    @Preference(value = UIConstants.Preferences.DISPLAY_PER_ANNUM) boolean displayPA)
    {
        FormatHelper.setDisplayPerAnnum(displayPA);
    }

    @Inject
    public void setTimePreset(@Preference(value = UIConstants.Preferences.PRESET_VALUE_TIME) String timePresetValue)
    {
        PresetValues.setTimePreset(timePresetValue);
    }
}
