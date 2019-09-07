package name.abuchen.portfolio.util.webaccess;

public class WebAccessParameter
{
    private String param;
    private String value;

    public static WebParameterBuilder builder()
    {
        return new WebAccessParameter.WebParameterBuilder();
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

    public static class WebParameterBuilder
    {
        private WebAccessParameter managedInstance = new WebAccessParameter();

        public WebParameterBuilder()
        {
        }

        public WebAccessParameter addParameter(String param, String value)
        {
            managedInstance.param = param;
            managedInstance.value = value;
            return managedInstance;
        }
    }
}
