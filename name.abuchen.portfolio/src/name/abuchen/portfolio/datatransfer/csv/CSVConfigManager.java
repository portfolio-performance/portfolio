package name.abuchen.portfolio.datatransfer.csv;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.e4.core.di.annotations.Creatable;
import org.eclipse.e4.core.di.extensions.Preference;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;
import org.osgi.service.prefs.BackingStoreException;

import name.abuchen.portfolio.PortfolioLog;

@Singleton
@Creatable
public class CSVConfigManager
{
    @Inject
    @Preference
    private IEclipsePreferences preferences;

    private List<CSVConfig> builtIn = new ArrayList<>();

    private boolean isDirty = false;
    private List<CSVConfig> userSpecific = new ArrayList<>();

    @PostConstruct
    private void loadConfigurations() throws IOException
    {
        try (Scanner scanner = new Scanner(getClass().getResourceAsStream("csv-config.json"), //$NON-NLS-1$
                        StandardCharsets.UTF_8.name()))
        {
            String json = scanner.useDelimiter("\\A").next(); //$NON-NLS-1$
            builtIn.addAll(fromJSON(json));

            String saved = preferences.get(CSVConfigManager.class.getName(), null);
            if (saved != null)
                userSpecific.addAll(fromJSON(saved));
        }
        catch (ParseException e)
        {
            throw new IOException(e);
        }
    }

    @SuppressWarnings("unchecked")
    @PreDestroy
    private void saveConfigurations()
    {
        if (!isDirty)
            return;

        try
        {
            JSONArray array = new JSONArray();
            userSpecific.forEach(config -> array.add(config.toJSON()));
            preferences.put(CSVConfigManager.class.getName(), array.toJSONString());
            preferences.flush();
        }
        catch (BackingStoreException e)
        {
            PortfolioLog.error(e);
        }
    }

    private List<CSVConfig> fromJSON(String json) throws ParseException
    {
        JSONArray array = (JSONArray) JSONValue.parseWithException(json);

        List<CSVConfig> answer = new ArrayList<>();
        for (int ii = 0; ii < array.size(); ii++)
        {
            try
            {
                CSVConfig config = new CSVConfig();
                config.fromJSON((JSONObject) array.get(ii));
                answer.add(config);
            }
            catch (Exception e)
            {
                PortfolioLog.error(e);
            }
        }

        return answer;
    }

    public List<CSVConfig> getBuiltInConfigurations()
    {
        return Collections.unmodifiableList(builtIn);
    }

    public List<CSVConfig> getUserSpecificConfigurations()
    {
        return Collections.unmodifiableList(userSpecific);
    }

    public void addUserSpecificConfiguration(CSVConfig config)
    {
        Optional<CSVConfig> previous = userSpecific.stream().filter(c -> config.getLabel().equals(c.getLabel()))
                        .findAny();

        if (previous.isPresent())
            userSpecific.remove(previous.get());

        userSpecific.add(config);

        isDirty = true;
    }

    public void removeUserSpecificConfiguration(CSVConfig config)
    {
        userSpecific.remove(config);

        isDirty = true;
    }
}
