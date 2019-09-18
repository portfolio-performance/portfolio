package name.abuchen.portfolio.util.webaccess;

import java.io.IOException;

public interface IWebAccess
{
    public IWebAccess document(String scheme, String host, String path);

    public IWebAccess addParameter(String param, String value);

    public IWebAccess addHeader(String param, String value);

    public String get() throws IOException;

}
