package name.abuchen.portfolio.ui.editor;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.eclipse.jface.preference.PreferenceStore;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.ConfigurationSet;
import name.abuchen.portfolio.model.ConfigurationSet.Configuration;
import name.abuchen.portfolio.ui.util.ClientFilterMenu;
import name.abuchen.portfolio.ui.util.ClientFilterMenu.Item;

class ClientFilterMigration
{

    private PreferenceStore preferenceStore;
    private Client client;

    ClientFilterMigration(PreferenceStore preferenceStore, Client client)
    {
        this.preferenceStore = preferenceStore;
        this.client = client;
    }

    public static void setOldSettingsToDefault(PreferenceStore pref)
    {
        pref.setToDefault("ClientFilterDropDown"); //$NON-NLS-1$
        pref.setToDefault("ClientFilterDropDown@json"); //$NON-NLS-1$
        Arrays.stream(pref.preferenceNames()).filter(name -> name.endsWith("-client-filter")) //$NON-NLS-1$
                        .forEach(name -> pref.setToDefault(name));
    }

    void migrateClientFilter()
    {
        // load custom filters
        List<ClientFilterMenu.Item> clientFilters = loadFilters();
        if (clientFilters == null || clientFilters.isEmpty())
            return;

        moveFiltersIntoClientFile(clientFilters);
        migrateFilterLinksInSettingsFileAndMoveToClient(clientFilters);
        migrateWidgets(clientFilters);
        migrateCharts(clientFilters);

        // signal user to save file
        client.touch();
    }

    private void moveFiltersIntoClientFile(List<ClientFilterMenu.Item> clientFilters)
    {
        ConfigurationSet set = client.getSettings().getConfigurationSet("ClientFilterDropDown"); //$NON-NLS-1$
        clientFilters.forEach(cf -> set.add(new Configuration(cf.getIdent(), cf.getLabel(), cf.getUUIDs())));
    }

    private List<Item> deserializeFromJSON(String json)
    {
        Type listType = new TypeToken<ArrayList<Item>>()
        {
        }.getType();
        return new Gson().fromJson(json, listType);
    }

    private List<ClientFilterMenu.Item> loadFilters()
    {
        String json = preferenceStore.getString("ClientFilterDropDown@json"); //$NON-NLS-1$
        if (json != null && !json.isEmpty())
            return deserializeFromJSON(json);

        // legacy loading
        List<ClientFilterMenu.Item> clientFilters = null;
        String legacy = preferenceStore.getString("ClientFilterDropDown"); //$NON-NLS-1$
        if (legacy != null && !legacy.isEmpty())
            return loadCustomItemsLegacy(legacy);

        return clientFilters;
    }

    private List<Item> loadCustomItemsLegacy(String code)
    {
        List<Item> result = new LinkedList<>();
        String[] items = code.split(";"); //$NON-NLS-1$
        for (String item : items)
            ClientFilterMenu.buildItem(UUID.randomUUID().toString(), "", item, client).ifPresent(result::add); //$NON-NLS-1$

        return result;
    }

    private void migrateFilterLinksInSettingsFileAndMoveToClient(List<ClientFilterMenu.Item> clientFilters)
    {
        // prepare filter
        Map<String, String> oldToNewIdentMap = new HashMap<>();
        clientFilters.forEach(f -> oldToNewIdentMap.putIfAbsent(f.getUUIDs(), f.getIdent()));

        // migrate each filter link in settings file and move into client file
        Arrays.stream(preferenceStore.preferenceNames()).filter(name -> name.endsWith("-client-filter")) //$NON-NLS-1$
                        .forEach(prefKey -> migrateSingleClientFilterAndMoveIntoClientFile(prefKey, oldToNewIdentMap));

    }

    private void migrateSingleClientFilterAndMoveIntoClientFile(String prefKey, Map<String, String> oldToNewIdentMap)
    {
        String link = preferenceStore.getString(prefKey);
        if (oldToNewIdentMap.containsKey(link))
        {
            ConfigurationSet set = client.getSettings().getConfigurationSet("client-filter-usages"); //$NON-NLS-1$
            set.add(new Configuration(prefKey, "", oldToNewIdentMap.get(link))); //$NON-NLS-1$
        }
    }

    private void migrateWidgets(List<ClientFilterMenu.Item> clientFilters)
    {
        // create map with all possible values as dashboard settings (normal
        // filter and pretax filter). Then iterate all widgets and check if
        // any of the possible value is included in widget config, and
        // migrate link if so.
        Map<String, String> oldToNewIdentMap = new HashMap<>();
        clientFilters.forEach(f -> {
            String oldIdentWithComma = f.getUUIDs();
            String oldIdentWithoutComma = oldIdentWithComma.replace(",", ""); //$NON-NLS-1$ //$NON-NLS-2$
            String newIdent = f.getIdent();
            // For DATA_SERIES and SECONDARY_DATA_SERIES
            oldToNewIdentMap.putIfAbsent("ClientFilter" + oldIdentWithoutComma, "ClientFilter" + newIdent); //$NON-NLS-1$ //$NON-NLS-2$
            // For DATA_SERIES and SECONDARY_DATA_SERIES
            oldToNewIdentMap.putIfAbsent("ClientFilter-PreTax" + oldIdentWithoutComma, //$NON-NLS-1$
                            "ClientFilter-PreTax" + newIdent); //$NON-NLS-1$
            // For CLIENT_FILTER.
            oldToNewIdentMap.putIfAbsent(oldIdentWithComma, newIdent); 
        });
        client.getDashboards().forEach(d -> d.getColumns().forEach(c -> c.getWidgets().forEach(w -> {
            migrateSingleWidgetConfig("DATA_SERIES", w.getConfiguration(), oldToNewIdentMap); //$NON-NLS-1$
            migrateSingleWidgetConfig("SECONDARY_DATA_SERIES", w.getConfiguration(), oldToNewIdentMap); //$NON-NLS-1$
            migrateSingleWidgetConfig("CLIENT_FILTER", w.getConfiguration(), oldToNewIdentMap); //$NON-NLS-1$
        })));
    }

    private void migrateSingleWidgetConfig(String configName, Map<String, String> configuration,
                    Map<String, String> oldToNewIdentMap)
    {
        String config = configuration.get(configName);
        if (config != null && oldToNewIdentMap.containsKey(config))
        {
            configuration.put(configName, oldToNewIdentMap.get(config));
        }
    }

    private void migrateCharts(List<ClientFilterMenu.Item> clientFilters)
    {
        Map<String, String> oldToNewIdentMap = new HashMap<>();
        clientFilters.forEach(f -> oldToNewIdentMap.putIfAbsent(f.getUUIDs().replace(",", ""), f.getIdent())); //$NON-NLS-1$ //$NON-NLS-2$
        String[] charts = new String[] { "StatementOfAssetsHistoryView-PICKER", "ReturnsVolatilityChartView-PICKER", //$NON-NLS-1$ //$NON-NLS-2$
                        "PerformanceChartView-PICKER" }; //$NON-NLS-1$
        for (String chart : charts)
        {
            ConfigurationSet conf = client.getSettings().getConfigurationSet(chart);
            if (conf == null)
                continue;

            conf.getConfigurations().forEach(c -> {
                // check all filters if they are present in data and replace
                // if so
                oldToNewIdentMap.forEach((key, val) -> {
                    if (c.getData().contains(key))
                    {
                        c.setData(c.getData().replace(key, val));
                    }
                });
            });
        }
    }
}
