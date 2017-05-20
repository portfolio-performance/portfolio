package name.abuchen.portfolio.online.impl;

/* package */ final class OnlineHelper
{

    @SuppressWarnings("nls")
    /* package */ static String getUserAgent()
    {
        String os = System.getProperty("os.name", "unknown").toLowerCase();
        if (os.startsWith("windows"))
            return "Mozilla/5.0 (Windows NT 6.2; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/32.0.1700.77 Safari/537.36";
        else if (os.startsWith("mac"))
            return "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_9_1) AppleWebKit/537.73.11 (KHTML, like Gecko) Version/7.0.1 Safari/537.73.11";
        else
            return "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:25.0) Gecko/20100101 Firefox/25.0";
    }

}
