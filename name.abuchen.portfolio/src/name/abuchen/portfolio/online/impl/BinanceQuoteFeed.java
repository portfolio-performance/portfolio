package name.abuchen.portfolio.online.impl;

import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.util.WebAccess;

public final class BinanceQuoteFeed extends AbstractBinanceQuoteFeed
{
    public static final String ID = "BINANCE"; //$NON-NLS-1$

    public BinanceQuoteFeed()
    {
        super(1000);
    }

    @Override
    public String getId()
    {
        return ID;
    }

    @Override
    public String getName()
    {
        return "Binance Spot"; //$NON-NLS-1$
    }

    @SuppressWarnings("nls")
    @Override
    protected WebAccess constructQuery(Security security, final Long tickerStartEpochMilliSeconds)
    {
        return new WebAccess("api.binance.com", "/api/v3/klines")
                        // Ticker: BTCEUR, BTCUSDT, ...
                        .addParameter("symbol", security.getTickerSymbol()) //
                        .addParameter("interval", "1d")
                        .addParameter("startTime", tickerStartEpochMilliSeconds.toString())
                        .addParameter("limit", "1000");
    }
}
