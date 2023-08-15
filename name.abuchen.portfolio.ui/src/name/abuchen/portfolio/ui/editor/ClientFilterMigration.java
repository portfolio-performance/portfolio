package name.abuchen.portfolio.ui.editor;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.eclipse.jface.preference.PreferenceStore;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.ConfigurationSet;
import name.abuchen.portfolio.model.ConfigurationSet.Configuration;
import name.abuchen.portfolio.model.ConfigurationSet.WellKnownConfigurationSets;
import name.abuchen.portfolio.model.Taxonomy;
import name.abuchen.portfolio.ui.util.ClientFilterMenu;
import name.abuchen.portfolio.ui.util.ClientFilterMenu.Item;

/**
 * One time migration running if loading a client file with a version lower than
 * 57 (June 2023).
 */
/* package */ class ClientFilterMigration
{
    private PreferenceStore preferenceStore;
    private Client client;

    /* package */ ClientFilterMigration(PreferenceStore preferenceStore, Client client)
    {
        this.preferenceStore = preferenceStore;
        this.client = client;
    }

    /* package */ void migrateClientFilter()
    {
        // load custom filters
        List<Configuration> newConfigurations = loadFilters();

        // always migrate selected filters as the user can select one of the
        // default filters
        migrateSelectedFiltersIntoClient(newConfigurations);

        if (!newConfigurations.isEmpty())
        {
            migrateCustomFiltersIntoClient(newConfigurations);
            migrateWidgets(newConfigurations);
            migrateCharts(newConfigurations);
        }

        // signal user to save file (even if nothing changed as we do not track
        // if an actual migration took place)
        client.touch();
    }

    private void migrateCustomFiltersIntoClient(List<Configuration> newConfigurations)
    {
        // migrate filter definitions
        ConfigurationSet set = client.getSettings()
                        .getConfigurationSet(WellKnownConfigurationSets.CLIENT_FILTER_DEFINITIONS);
        set.clear();
        newConfigurations.forEach(set::add);
    }

    private List<Configuration> loadFilters()
    {
        String json = preferenceStore.getString("ClientFilterDropDown@json"); //$NON-NLS-1$
        if (json != null && !json.isEmpty())
            return deserializeFromJSON(json);

        // legacy loading (filter w/o names)
        String legacy = preferenceStore.getString("ClientFilterDropDown"); //$NON-NLS-1$
        if (legacy != null && !legacy.isEmpty())
            return loadCustomItemsLegacy(legacy);

        return Collections.emptyList();
    }

    private List<Configuration> deserializeFromJSON(String json)
    {
        Type listType = new TypeToken<ArrayList<Item>>()
        {
        }.getType();
        List<Item> answer = new Gson().fromJson(json, listType);

        return answer.stream()
                        .map(item -> new Configuration(UUID.randomUUID().toString(), item.getLabel(), item.getUUIDs()))
                        .toList();
    }

    private List<Configuration> loadCustomItemsLegacy(String code)
    {
        List<Configuration> result = new ArrayList<>();
        String[] items = code.split(";"); //$NON-NLS-1$
        for (String uuids : items)
            ClientFilterMenu.buildItem(UUID.randomUUID().toString(), "", uuids, client) //$NON-NLS-1$
                            .ifPresent(i -> new Configuration(i.getId(), i.getLabel(), i.getUUIDs()));

        return result;
    }

    private void migrateSelectedFiltersIntoClient(List<Configuration> newConfigurations)
    {
        ConfigurationSet set = client.getSettings()
                        .getConfigurationSet(WellKnownConfigurationSets.CLIENT_FILTER_SELECTION);
        set.clear();

        loadSelectedFilters(newConfigurations).forEach(set::add);
    }

    private static final String PREF_KEY_POSTFIX = "-client-filter"; //$NON-NLS-1$

    private List<Configuration> loadSelectedFilters(List<Configuration> newConfigurations)
    {
        // create migration mapping

        // caution: The user can create multiple identical filter which result
        // in #getData not being unique
        Map<String, String> old2new = newConfigurations.stream()
                        .collect(Collectors.toMap(Configuration::getData, Configuration::getUUID, (r, l) -> r));

        // migrate only known keys in order to prevent legacy keys ending up in
        // the settings

        Set<String> keys = new HashSet<>(List.of( //
                        "AllTransactionsView" + PREF_KEY_POSTFIX, //$NON-NLS-1$
                        "SecuritiesPerformanceView" + PREF_KEY_POSTFIX, //$NON-NLS-1$
                        "PaymentsViewModel" + PREF_KEY_POSTFIX, //$NON-NLS-1$
                        "PerformanceView" + PREF_KEY_POSTFIX, //$NON-NLS-1$
                        "StatementOfAssetsView" + PREF_KEY_POSTFIX, //$NON-NLS-1$
                        "HoldingsPieChartView" + PREF_KEY_POSTFIX //$NON-NLS-1$
        ));

        for (Taxonomy t : client.getTaxonomies())
            keys.add("TaxonomyView-" + t.getId() + PREF_KEY_POSTFIX); //$NON-NLS-1$

        // migrate each selected filter in settings file and move into client
        // file

        List<Configuration> answer = new ArrayList<>();
        for (String prefKey : preferenceStore.preferenceNames())
        {
            if (!keys.contains(prefKey))
                continue;

            String oldValue = preferenceStore.getString(prefKey);

            String newValue = old2new.get(oldValue);

            // if we do not find the new value, then the selection is probably
            // one of the default filters -> keep the value
            if (newValue == null || newValue.isBlank())
                newValue = oldValue;

            answer.add(new Configuration(prefKey.substring(0, prefKey.length() - PREF_KEY_POSTFIX.length()), "", //$NON-NLS-1$
                            newValue));
        }
        return answer;
    }

    private void migrateWidgets(List<Configuration> newConfigurations)
    {
        migrateWidgetsSingleDataSeries(newConfigurations);
        migrateWidgetsMultiDataSeries(newConfigurations);
    }

    private void migrateWidgetsSingleDataSeries(List<Configuration> newConfigurations)
    {
        Map<String, String> oldToNew = new HashMap<>();
        newConfigurations.forEach(f -> {
            String oldIdWithComma = f.getData();
            String oldIdWithoutComma = oldIdWithComma.replace(",", ""); //$NON-NLS-1$ //$NON-NLS-2$
            String newId = f.getUUID();

            // For DATA_SERIES and SECONDARY_DATA_SERIES
            oldToNew.putIfAbsent("ClientFilter" + oldIdWithoutComma, //$NON-NLS-1$
                            "ClientFilter" + newId); //$NON-NLS-1$
            // For DATA_SERIES and SECONDARY_DATA_SERIES
            oldToNew.putIfAbsent("ClientFilter-PreTax" + oldIdWithoutComma, //$NON-NLS-1$
                            "ClientFilter-PreTax" + newId); //$NON-NLS-1$
            // For CLIENT_FILTER
            oldToNew.putIfAbsent(oldIdWithComma, newId);
        });

        client.getDashboards().forEach(d -> d.getColumns() //
                        .forEach(c -> c.getWidgets().stream()
                                        .filter(widget -> !"HEATMAP_YEARLY".equals(widget.getType())) //$NON-NLS-1$
                                        .forEach(widget -> {
                                            migrateWidget("DATA_SERIES", widget.getConfiguration(), oldToNew); //$NON-NLS-1$
                                            migrateWidget("SECONDARY_DATA_SERIES", widget.getConfiguration(), oldToNew); //$NON-NLS-1$
                                            migrateWidget("CLIENT_FILTER", widget.getConfiguration(), oldToNew); //$NON-NLS-1$
                                        })));

    }

    private void migrateWidgetsMultiDataSeries(List<Configuration> newConfigurations)
    {
        // the yearly heatmap widgets allows to pick multiple data series which
        // are serialized identical to the chart series configuration

        Map<String, String> oldToNew = buildChartConfigMapping(newConfigurations);

        client.getDashboards().forEach(d -> d.getColumns() //
                        .forEach(c -> c.getWidgets().stream()
                                        .filter(widget -> "HEATMAP_YEARLY".equals(widget.getType())) //$NON-NLS-1$
                                        .forEach(widget -> {

                                            String value = widget.getConfiguration().get("DATA_SERIES"); //$NON-NLS-1$
                                            if (value != null)
                                            {
                                                for (Entry<String, String> entry : oldToNew.entrySet())
                                                {
                                                    if (value.contains(entry.getKey()))
                                                    {
                                                        value = value.replace(entry.getKey(), entry.getValue());
                                                        widget.getConfiguration().put("DATA_SERIES", //$NON-NLS-1$
                                                                        value);
                                                    }
                                                }
                                            }
                                        })));
    }

    private void migrateWidget(String configName, Map<String, String> configuration,
                    Map<String, String> oldToNewIdentMap)
    {
        String config = configuration.get(configName);
        if (config != null && oldToNewIdentMap.containsKey(config))
        {
            configuration.put(configName, oldToNewIdentMap.get(config));
        }
    }

    private void migrateCharts(List<Configuration> newConfigurations)
    {
        Map<String, String> oldToNew = buildChartConfigMapping(newConfigurations);

        String[] charts = new String[] { "StatementOfAssetsHistoryView-PICKER", //$NON-NLS-1$
                        "ReturnsVolatilityChartView-PICKER", //$NON-NLS-1$
                        "PerformanceChartView-PICKER" }; //$NON-NLS-1$

        for (String chart : charts)
        {
            ConfigurationSet conf = client.getSettings().getConfigurationSet(chart);
            if (conf == null)
                continue;

            conf.getConfigurations().forEach(chartConfig -> { // NOSONAR
                // check all filters if they are present in data and replace
                // if so
                oldToNew.forEach((key, val) -> {
                    if (chartConfig.getData().contains(key))
                    {
                        chartConfig.setData(chartConfig.getData().replace(key, val));
                    }
                });
            });
        }
    }

    private Map<String, String> buildChartConfigMapping(List<Configuration> newConfigurations)
    {
        Map<String, String> oldToNew = new HashMap<>();

        // to avoid partial matches, we must replace the full identifier
        // including the trailing semicolon (which is always present and
        // separates the color). Otherwise filter 1 = {A} is matching filter2 =
        // { A, B } in the migration
        newConfigurations.forEach(f -> {
            String oldIdWithoutComma = f.getData().replace(",", ""); //$NON-NLS-1$ //$NON-NLS-2$
            String newId = f.getUUID();

            oldToNew.putIfAbsent("ClientFilter" + oldIdWithoutComma + ";", //$NON-NLS-1$ //$NON-NLS-2$
                            "ClientFilter" + newId + ";"); //$NON-NLS-1$ //$NON-NLS-2$
            oldToNew.putIfAbsent("ClientFilter-PreTax" + oldIdWithoutComma + ";", //$NON-NLS-1$ //$NON-NLS-2$
                            "ClientFilter-PreTax" + newId + ";"); //$NON-NLS-1$ //$NON-NLS-2$
        });
        return oldToNew;
    }
}
