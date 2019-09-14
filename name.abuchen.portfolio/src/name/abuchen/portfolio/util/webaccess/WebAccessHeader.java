package name.abuchen.portfolio.util.webaccess;

public class WebAccessHeader
{
    private String param;
    private String value;

    public static WebHeaderBuilder builder()
    {
        return new WebAccessHeader.WebHeaderBuilder();
    }

    public String getParam()
    {
        return param;
    }

    public void setParam(String param)
    {
        this.param = param;
    }

    public String getValue()
    {
        return value;
    }

    public void setValue(String value)
    {
        this.value = value;
    }

    public static class WebHeaderBuilder
    {
        private WebAccessHeader managedInstance = new WebAccessHeader();

        public WebHeaderBuilder()
        {
        }

        public WebAccessHeader addHeaders(String param, String value)
        {
            managedInstance.param = param;
            managedInstance.value = value;
            return managedInstance;
        }
    }
}
