package name.abuchen.portfolio.ui.wizards.security;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.online.SecuritySearchProvider.ResultItem;

public class SearchSecurityDataModel
{
    private final Client client;

    private ResultItem item;

    public SearchSecurityDataModel(Client client)
    {
        this.client = client;
    }

    public Client getClient()
    {
        return client;
    }

    public ResultItem getSelectedItem()
    {
        return item;
    }

    public void setSelectedItem(ResultItem item)
    {
        this.item = item;
    }

    public void clearSelectedItem()
    {
        this.item = null;
    }
}
