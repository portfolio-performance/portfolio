package name.abuchen.portfolio.online.impl;

import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.util.WebAccess;

public final class BinanceFuturesUsdsMarginedQuoteFeed extends AbstractBinanceQuoteFeed
{
    public static final String ID = "BINANCEUSDSMFUTURE"; //$NON-NLS-1$

    public BinanceFuturesUsdsMarginedQuoteFeed()
    {
        super(1500);
    }

    @Override
    public String getId()
    {
        return ID;
    }

    @Override
    public String getName()
    {
        return "Binance USDs-M Futures"; //$NON-NLS-1$
    }

    @SuppressWarnings("nls")
    @Override
    protected WebAccess constructQuery(Security security, final Long tickerStartEpochMilliSeconds)
    {
        return new WebAccess("fapi.binance.com", "/fapi/v1/klines")
                        // Ticker: BTCUSDC, BTCUSDT, ...
                        .addParameter("symbol", security.getTickerSymbol()).addParameter("interval", "1d")
                        .addParameter("startTime", tickerStartEpochMilliSeconds.toString())
                        .addParameter("limit", "1500");
    }
}
