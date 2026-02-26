package name.abuchen.portfolio.oauth;

public class UserInfo
{
    private final String sub;
    private final String email;

    public UserInfo(String sub, String email)
    {
        this.sub = sub;
        this.email = email;
    }

    public String getSub()
    {
        return sub;
    }

    public String getEmail()
    {
        return email;
    }
}
