package name.abuchen.portfolio.ui.dialogs;

import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import org.eclipse.jface.preference.IPreferenceStore;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.UIConstants;

public class IBFlexConfigurationDialog
{
    private static final Gson GSON = new Gson();
    public record Credential(String queryId, String token, String name)
    {
        public Credential
        {
            queryId = queryId != null ? queryId.trim() : null;
            token = token != null ? token.trim() : null;
            name = name != null ? name.trim() : null;

            if (name != null && name.isBlank())
                name = null;
        }

        public Credential(String queryId, String token)
        {
            this(queryId, token, null);
        }

        public String label()
        {
            return name == null ? queryId : name + " (" + queryId + ")";
        }
    }

    public record Cutoff(String queryId, LocalDateTime date)
    {
    }

    private static final String PROPERTY_LAST_IMPORT_DATES = "ibflex-last-import-dates";

    private IBFlexConfigurationDialog()
    {
    }

    public static List<Credential> getCredentials()
    {
        return getCredentials(PortfolioPlugin.getDefault().getPreferenceStore());
    }

    static List<Credential> getCredentials(IPreferenceStore store)
    {
        var value = store.getString(UIConstants.Preferences.IBFLEX_CREDENTIALS);
        return deserialize(value);
    }

    public static void setCredentials(List<Credential> credentials)
    {
        setCredentials(PortfolioPlugin.getDefault().getPreferenceStore(), credentials);
    }

    static void setCredentials(IPreferenceStore store, List<Credential> credentials)
    {
        store.setValue(UIConstants.Preferences.IBFLEX_CREDENTIALS, serialize(credentials));
    }

    public static LocalDateTime getLastImportDate(Client client, String queryId)
    {
        if (client == null || queryId == null || queryId.isBlank())
            return null;

        var dates = loadLastImportDates(client);
        return parseDate(dates.get(queryId.trim()));
    }

    public static List<Cutoff> getLastImportDates(Client client)
    {
        if (client == null)
            return List.of();

        var dates = loadLastImportDates(client);
        return dates.entrySet().stream()
                        .map(e -> new Cutoff(e.getKey(), parseDate(e.getValue())))
                        .filter(c -> c.date() != null)
                        .toList();
    }

    public static void setLastImportDate(Client client, String queryId, LocalDateTime date)
    {
        if (client == null || queryId == null || queryId.isBlank())
            return;

        var dates = loadLastImportDates(client);

        if (date != null)
            dates.put(queryId.trim(), date.toString());
        else
            dates.remove(queryId.trim());

        storeLastImportDates(client, dates);
    }

    public static void setLastImportDates(Client client, List<Cutoff> cutoffs)
    {
        if (client == null)
            return;

        var dates = new TreeMap<String, String>();
        if (cutoffs != null)
        {
            for (var cutoff : cutoffs)
            {
                if (cutoff == null || cutoff.queryId() == null || cutoff.queryId().isBlank() || cutoff.date() == null)
                    continue;

                dates.put(cutoff.queryId().trim(), cutoff.date().toString());
            }
        }

        storeLastImportDates(client, dates);
    }

    public static void clearLastImportDate(Client client, String queryId)
    {
        setLastImportDate(client, queryId, null);
    }

    public static void clearLastImportDates(Client client)
    {
        if (client == null)
            return;

        client.removeProperty(PROPERTY_LAST_IMPORT_DATES);
    }

    private static TreeMap<String, String> loadLastImportDates(Client client)
    {
        var json = client.getProperty(PROPERTY_LAST_IMPORT_DATES);
        if (json == null || json.isBlank())
            return new TreeMap<>();

        Type mapType = new TypeToken<TreeMap<String, String>>()
        {
        }.getType();
        TreeMap<String, String> result = GSON.fromJson(json, mapType);
        return result != null ? result : new TreeMap<>();
    }

    private static void storeLastImportDates(Client client, TreeMap<String, String> dates)
    {
        if (dates.isEmpty())
            client.removeProperty(PROPERTY_LAST_IMPORT_DATES);
        else
            client.setProperty(PROPERTY_LAST_IMPORT_DATES, GSON.toJson(dates));
    }

    private static LocalDateTime parseDate(String value)
    {
        if (value == null || value.isBlank())
            return null;

        try
        {
            return LocalDateTime.parse(value);
        }
        catch (DateTimeParseException e)
        {
            return null;
        }
    }

    public static String serialize(List<Credential> credentials)
    {
        if (credentials == null || credentials.isEmpty())
            return "";

        return GSON.toJson(credentials);
    }

    public static List<Credential> deserialize(String value)
    {
        if (value == null || value.isBlank())
            return new ArrayList<>();

        Type listType = new TypeToken<ArrayList<Credential>>()
        {
        }.getType();
        List<Credential> result = GSON.fromJson(value, listType);
        return result != null ? result : new ArrayList<>();
    }
}
