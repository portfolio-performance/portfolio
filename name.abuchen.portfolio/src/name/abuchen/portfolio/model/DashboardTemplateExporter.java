package name.abuchen.portfolio.model;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Exports a dashboard as a reusable JSON template. Entity references (UUIDs
 * pointing to file-specific objects like accounts, portfolios, securities,
 * taxonomies) are removed so the template can be imported into any file.
 */
public class DashboardTemplateExporter implements Exporter
{
    /**
     * Configuration keys that reference file-specific entities via UUIDs. These
     * are stripped from both widget-level and dashboard-level configurations
     * during export.
     */
    static final Set<String> ENTITY_REFERENCE_KEYS = Set.of( //
                    Dashboard.Config.DATA_SERIES.name(), //
                    Dashboard.Config.SECONDARY_DATA_SERIES.name(), //
                    Dashboard.Config.CLIENT_DATA_SERIES.name(), //
                    Dashboard.Config.CLIENT_FILTER.name(), //
                    Dashboard.Config.TAXONOMY.name(), //
                    Dashboard.Config.CONFIG_UUID.name(), //
                    Dashboard.Config.ATTRIBUTE_UUID.name(), //
                    Dashboard.Config.EXCHANGE_RATE_SERIES.name());

    private final Dashboard dashboard;

    public DashboardTemplateExporter(Dashboard dashboard)
    {
        this.dashboard = dashboard;
    }

    @Override
    public String getName()
    {
        return dashboard.getName();
    }

    @Override
    public void export(OutputStream out) throws IOException
    {
        Map<String, Object> result = createJSONStructure();

        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        try (OutputStreamWriter writer = new OutputStreamWriter(out, StandardCharsets.UTF_8))
        {
            gson.toJson(result, writer);
        }
    }

    /* package */ Map<String, Object> createJSONStructure()
    {
        Map<String, Object> result = new LinkedHashMap<>();

        result.put("version", 1); //$NON-NLS-1$
        result.put("name", dashboard.getName()); //$NON-NLS-1$

        Map<String, String> filteredConfig = filterConfiguration(dashboard.getConfiguration());
        if (!filteredConfig.isEmpty())
            result.put("configuration", filteredConfig); //$NON-NLS-1$

        List<Map<String, Object>> columnsList = new ArrayList<>();
        for (Dashboard.Column column : dashboard.getColumns())
        {
            Map<String, Object> columnMap = new LinkedHashMap<>();
            columnMap.put("weight", column.getWeight()); //$NON-NLS-1$

            List<Map<String, Object>> widgetsList = new ArrayList<>();
            for (Dashboard.Widget widget : column.getWidgets())
            {
                Map<String, Object> widgetMap = new LinkedHashMap<>();
                widgetMap.put("type", widget.getType()); //$NON-NLS-1$
                widgetMap.put("label", widget.getLabel()); //$NON-NLS-1$

                Map<String, String> widgetConfig = filterConfiguration(widget.getConfiguration());
                if (!widgetConfig.isEmpty())
                    widgetMap.put("configuration", widgetConfig); //$NON-NLS-1$

                widgetsList.add(widgetMap);
            }
            columnMap.put("widgets", widgetsList); //$NON-NLS-1$

            columnsList.add(columnMap);
        }
        result.put("columns", columnsList); //$NON-NLS-1$

        return result;
    }

    /**
     * Returns a new map with all entity reference keys removed.
     */
    /* package */ static Map<String, String> filterConfiguration(Map<String, String> config)
    {
        if (config == null || config.isEmpty())
            return new LinkedHashMap<>();

        return config.entrySet().stream() //
                        .filter(e -> !ENTITY_REFERENCE_KEYS.contains(e.getKey())) //
                        .collect(Collectors.toMap( //
                                        Map.Entry::getKey, //
                                        Map.Entry::getValue, //
                                        (a, b) -> a, //
                                        LinkedHashMap::new));
    }
}
