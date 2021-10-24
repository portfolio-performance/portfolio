package name.abuchen.portfolio.json;

import name.abuchen.portfolio.model.Account;

public class JAccount
{
    private String name;
    private String uuid;

    public String getName()
    {
        return name;
    }

    public String getUUID() 
    {
        return uuid;
    }
    
    public static JAccount from(Account account)
    {
        JAccount a = new JAccount();
        a.name = account.getName();
        a.uuid = account.getUUID();
        return a;
    }

}
