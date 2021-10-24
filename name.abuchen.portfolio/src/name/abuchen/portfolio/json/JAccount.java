package name.abuchen.portfolio.json;

import name.abuchen.portfolio.model.Account;

public class JAccount
{
    private String name;

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }
    
    public static JAccount from(Account account)
    {
        JAccount a = new JAccount();
        a.name = account.getName();
        return a;
    }

}
