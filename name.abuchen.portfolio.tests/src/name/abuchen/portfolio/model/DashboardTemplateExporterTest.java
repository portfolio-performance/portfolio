package name.abuchen.portfolio.model;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.Test;

@SuppressWarnings("nls")
public class DashboardTemplateExporterTest
{
    private Dashboard.Widget buildWidget()
    {
        Dashboard.Widget widget = new Dashboard.Widget();
        widget.setType("HEADING");
        widget.setLabel("My Widget");
        widget.getConfiguration().put("SOME_OTHER_KEY", "value");
        widget.getConfiguration().put(Dashboard.Config.DATA_SERIES.name(), "some-uuid");
        return widget;
    }

    private Dashboard buildDashboard()
    {
        Dashboard dashboard = new Dashboard(UUID.randomUUID().toString());
        dashboard.setName("My Dashboard");
        dashboard.getConfiguration().put("SOME_OTHER_KEY", "value");
        dashboard.getConfiguration().put(Dashboard.Config.CLIENT_FILTER.name(), "some-uuid");

        Dashboard.Column column = new Dashboard.Column();
        column.setWeight(2);
        column.getWidgets().add(buildWidget());
        dashboard.getColumns().add(column);

        return dashboard;
    }

    @Test
    public void testFilterConfigurationRemovesEntityReferenceKeys()
    {
        Map<String, String> config = new HashMap<>();
        config.put("SOME_OTHER_KEY", "value");
        config.put(Dashboard.Config.DATA_SERIES.name(), "some-uuid");
        config.put(Dashboard.Config.TAXONOMY.name(), "some-other-uuid");

        Map<String, String> filtered = DashboardTemplateExporter.filterConfiguration(config);

        assertThat(filtered.size(), is(1));
        assertThat(filtered.get("SOME_OTHER_KEY"), is("value"));
        assertThat(filtered.containsKey(Dashboard.Config.DATA_SERIES.name()), is(false));
        assertThat(filtered.containsKey(Dashboard.Config.TAXONOMY.name()), is(false));
    }

    @Test
    public void testFilterConfigurationHandlesNullAndEmpty()
    {
        assertThat(DashboardTemplateExporter.filterConfiguration(null).isEmpty(), is(true));
        assertThat(DashboardTemplateExporter.filterConfiguration(new HashMap<>()).isEmpty(), is(true));
    }

    @Test
    public void testCreateJSONStructure()
    {
        Dashboard dashboard = buildDashboard();

        DashboardTemplateExporter exporter = new DashboardTemplateExporter(dashboard);
        assertThat(exporter.getName(), is("My Dashboard"));

        Map<String, Object> json = exporter.createJSONStructure();

        assertThat(json.get("version"), is(1));
        assertThat(json.get("name"), is("My Dashboard"));

        @SuppressWarnings("unchecked")
        Map<String, String> config = (Map<String, String>) json.get("configuration");
        assertThat(config, is(notNullValue()));
        assertThat(config.get("SOME_OTHER_KEY"), is("value"));
        assertThat(config.containsKey(Dashboard.Config.CLIENT_FILTER.name()), is(false));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> columns = (List<Map<String, Object>>) json.get("columns");
        assertThat(columns.size(), is(1));
        assertThat(columns.get(0).get("weight"), is(2));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> widgets = (List<Map<String, Object>>) columns.get(0).get("widgets");
        assertThat(widgets.size(), is(1));
        assertThat(widgets.get(0).get("type"), is("HEADING"));
        assertThat(widgets.get(0).get("label"), is("My Widget"));

        @SuppressWarnings("unchecked")
        Map<String, String> widgetConfig = (Map<String, String>) widgets.get(0).get("configuration");
        assertThat(widgetConfig.get("SOME_OTHER_KEY"), is("value"));
        assertThat(widgetConfig.containsKey(Dashboard.Config.DATA_SERIES.name()), is(false));
    }

    @Test
    public void testCreateJSONStructureOmitsEmptyConfiguration()
    {
        Dashboard dashboard = new Dashboard(UUID.randomUUID().toString());
        dashboard.setName("Empty Dashboard");

        Map<String, Object> json = new DashboardTemplateExporter(dashboard).createJSONStructure();

        assertThat(json.containsKey("configuration"), is(false));
    }
}
