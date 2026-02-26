package name.abuchen.portfolio.util;

import java.util.concurrent.ThreadLocalRandom;

@SuppressWarnings("nls")
public final class OnlineHelper
{
    private OnlineHelper()
    {
    }

    public static String getUserAgent()
    {
        return "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.3.1 Safari/605.1.15"; // NOSONAR
    }

    public static String getYahooFinanceUserAgent()
    {
        return "Mozilla/5.0 (" + ThreadLocalRandom.current().nextInt(100000, 999999) + ")";
    }
}
