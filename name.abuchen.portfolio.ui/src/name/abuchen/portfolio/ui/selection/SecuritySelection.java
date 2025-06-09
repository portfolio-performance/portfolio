package name.abuchen.portfolio.ui.selection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.eclipse.jface.viewers.IStructuredSelection;

import name.abuchen.portfolio.model.Adaptable;
import name.abuchen.portfolio.model.Adaptor;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;

public class SecuritySelection implements Adaptable
{
    private final Client client;
    private final List<Security> securities;

    public SecuritySelection(Client client, Security security)
    {
        this.client = Objects.requireNonNull(client);
        this.securities = Arrays.asList(Objects.requireNonNull(security));
    }

    public SecuritySelection(Client client, List<Security> securities)
    {
        this.client = Objects.requireNonNull(client);
        this.securities = Objects.requireNonNull(securities);
    }

    public static SecuritySelection from(Client client, IStructuredSelection selection)
    {
        var securities = new ArrayList<Security>();
        for (Object element : selection)
        {
            var security = Adaptor.adapt(Security.class, element);
            if (security != null)
                securities.add(security);
        }

        return new SecuritySelection(client, securities);
    }

    public Client getClient()
    {
        return client;
    }

    public Security getSecurity()
    {
        return securities.get(0);
    }

    public List<Security> getSecurities()
    {
        return securities;
    }

    @Override
    public <T> T adapt(Class<T> type)
    {
        if (type == Security.class && !securities.isEmpty())
            return type.cast(securities.getFirst());
        else
            return null;
    }
}
