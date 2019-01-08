package name.abuchen.portfolio.datatransfer.csv;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.stream.Stream;

import javax.inject.Singleton;

import org.eclipse.e4.core.di.annotations.Creatable;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;

import name.abuchen.portfolio.PortfolioLog;

@Singleton
@Creatable
public class CSVConfigManager
{
    private List<CSVConfig> buildIn = new ArrayList<>();
    private List<CSVConfig> userSpecific = new ArrayList<>();

    public CSVConfigManager() throws IOException
    {
        try (Scanner scanner = new Scanner(getClass().getResourceAsStream("csv-config.json"), //$NON-NLS-1$
                        StandardCharsets.UTF_8.name()))
        {
            String json = scanner.useDelimiter("\\A").next(); //$NON-NLS-1$
            JSONArray array = (JSONArray) JSONValue.parseWithException(json);

            for (int ii = 0; ii < array.size(); ii++)
            {
                try // NOSONAR
                {
                    CSVConfig config = new CSVConfig();
                    config.fromJSON((JSONObject) array.get(ii));
                    buildIn.add(config);
                }
                catch (Exception e)
                {
                    PortfolioLog.error(e);
                }
            }
        }
        catch (ParseException e)
        {
            throw new IOException(e);
        }
    }

    public Stream<CSVConfig> getBuiltInConfigurations()
    {
        return buildIn.stream();
    }

    public Stream<CSVConfig> getUserSpecificConfigurations()
    {
        return userSpecific.stream();
    }

    public void addUserSpecificConfiguration(CSVConfig config)
    {
        Optional<CSVConfig> previous = userSpecific.stream().filter(c -> config.getLabel().equals(c.getLabel()))
                        .findAny();

        if (previous.isPresent())
            userSpecific.remove(previous.get());

        userSpecific.add(config);
    }

    public void removeUserSpecificConfiguration(CSVConfig config)
    {
        userSpecific.remove(config);
    }
}
