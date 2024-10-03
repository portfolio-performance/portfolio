package name.abuchen.portfolio.ui.util;

import org.eclipse.jface.viewers.LabelProvider;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.ClientProperties;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityNameConfig;

public class SecurityNameLabelProvider extends LabelProvider
{
    private final SecurityNameConfig config;

    public SecurityNameLabelProvider(Client client)
    {
        this.config = new ClientProperties(client).getSecurityNameConfig();
    }

    @Override
    public String getText(Object element)
    {
        if (element instanceof Security s)
            return s.getName(config);
        else
            return String.valueOf(element);
    }
}
