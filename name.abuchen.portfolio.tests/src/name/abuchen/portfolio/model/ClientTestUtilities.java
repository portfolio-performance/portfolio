package name.abuchen.portfolio.model;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import name.abuchen.portfolio.model.Dashboard.Widget;

public class ClientTestUtilities
{
    public static String toString(Client client)
    {
        return toString(client, false);
    }

    public static String toString(Client client, boolean withIdReferences)
    {
        // prune client - remove empty yet lazily created objects that should
        // not show up in XML

        client.getSecurities().forEach(s -> {
            if (s.getEvents().isEmpty())
                s.removeAllEvents();
        });

        normalizeClient(client);

        try
        {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            new ClientFactory.XmlSerialization(withIdReferences).save(client, stream);
            stream.close();
            return new String(stream.toByteArray());
        }
        catch (IOException e)
        {
            // should not happen with an in-memory byte array stream
            throw new IllegalArgumentException(e);
        }
    }

    public static int indexOfDifference(String left, String right)
    {
        int ii;
        for (ii = 0; ii < left.length() && ii < right.length(); ++ii)
            if (left.charAt(ii) != right.charAt(ii))
                return ii;
        return ii < right.length() || ii < left.length() ? ii : -1;
    }

    private static void normalizeClient(Client client)
    {
        // prune client - remove empty yet lazily created objects that should
        // not show up in XML

        client.getSecurities().forEach(s -> {
            if (s.getEvents().isEmpty())
                s.removeAllEvents();
        });

        client.getSecurities().forEach(Security::getAttributes);

        client.getAccounts().forEach(a -> {
            a.getTransactions().sort(Transaction.BY_DATE);
            a.getAttributes();
        });
        client.getPortfolios().forEach(p -> {
            p.getTransactions().sort(Transaction.BY_DATE);
            p.getAttributes();
        });

        // some columns have no widgets initialized
        client.getDashboards().flatMap(d -> d.getColumns().stream()).filter(c -> c.getWidgets() == null)
                        .forEach(c -> c.setWidgets(new ArrayList<>()));

        // some widgets have no or empty configuration
        client.getDashboards().flatMap(d -> d.getColumns().stream()).filter(c -> c.getWidgets() != null)
                        .flatMap(c -> c.getWidgets().stream()).forEach(Widget::getConfiguration);

        // force a common serialization order of the set by adding the values
        // sorted by key

        Map<String, ConfigurationSet> configurations = client.getSettings().getConfigurationSets().stream()
                        .sorted((r, l) -> r.getKey().compareTo(l.getKey()))
                        .collect(Collectors.toMap(Entry<String, ConfigurationSet>::getKey,
                                        Entry<String, ConfigurationSet>::getValue));
        client.getSettings().clearConfigurationSets();
        client.getSettings().putAllConfigurationSets(configurations);

        client.getSettings().getAttributeTypes().forEach(AttributeType::getProperties);

        client.getTaxonomies().forEach(t -> {
            if (t.getDimensions() == null)
                t.setDimensions(new ArrayList<>());
        });
    }
}
