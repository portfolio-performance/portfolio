package name.abuchen.portfolio.model;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.junit.Test;

@SuppressWarnings("nls")
public class DashboardTemplateImporterTest
{
    private String toJSON(Dashboard dashboard) throws IOException
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        new DashboardTemplateExporter(dashboard).export(out);
        return out.toString(StandardCharsets.UTF_8);
    }

    @Test
    public void testImportRoundTrip() throws IOException
    {
        Dashboard original = new Dashboard(UUID.randomUUID().toString());
        original.setName("My Dashboard");
        original.getConfiguration().put("SOME_OTHER_KEY", "value");
        original.getConfiguration().put(Dashboard.Config.CLIENT_FILTER.name(), "some-uuid");

        Dashboard.Column column = new Dashboard.Column();
        column.setWeight(2);

        Dashboard.Widget widget = new Dashboard.Widget();
        widget.setType("HEADING");
        widget.setLabel("My Widget");
        widget.getConfiguration().put("SOME_OTHER_KEY", "value");
        widget.getConfiguration().put(Dashboard.Config.DATA_SERIES.name(), "some-uuid");
        column.getWidgets().add(widget);

        original.getColumns().add(column);

        String json = toJSON(original);

        Dashboard imported = new DashboardTemplateImporter().importDashboard(new StringReader(json));

        assertThat(imported.getName(), is("My Dashboard"));
        assertThat(imported.getId(), is(not(original.getId())));

        assertThat(imported.getConfiguration().get("SOME_OTHER_KEY"), is("value"));
        assertThat(imported.getConfiguration().containsKey(Dashboard.Config.CLIENT_FILTER.name()), is(false));

        assertThat(imported.getColumns().size(), is(1));
        Dashboard.Column importedColumn = imported.getColumns().get(0);
        assertThat(importedColumn.getWeight(), is(2));

        assertThat(importedColumn.getWidgets().size(), is(1));
        Dashboard.Widget importedWidget = importedColumn.getWidgets().get(0);
        assertThat(importedWidget.getType(), is("HEADING"));
        assertThat(importedWidget.getLabel(), is("My Widget"));
        assertThat(importedWidget.getConfiguration().get("SOME_OTHER_KEY"), is("value"));
        assertThat(importedWidget.getConfiguration().containsKey(Dashboard.Config.DATA_SERIES.name()), is(false));
    }

    @Test
    public void testImportMinimalDashboard() throws IOException
    {
        String json = """
                        {
                          "version": 1,
                          "name": "Minimal",
                          "columns": []
                        }
                        """;

        Dashboard imported = new DashboardTemplateImporter().importDashboard(new StringReader(json));

        assertThat(imported.getName(), is("Minimal"));
        assertThat(imported.getColumns().isEmpty(), is(true));
    }

    @Test(expected = IOException.class)
    public void testImportNullJson() throws IOException
    {
        new DashboardTemplateImporter().importDashboard(new StringReader("null"));
    }

    @Test(expected = IOException.class)
    public void testImportMalformedJson() throws IOException
    {
        new DashboardTemplateImporter().importDashboard(new StringReader("{ invalid json"));
    }

    @Test(expected = IOException.class)
    public void testImportUnsupportedVersion() throws IOException
    {
        String json = """
                        {
                          "version": 99,
                          "name": "Dashboard",
                          "columns": []
                        }
                        """;

        new DashboardTemplateImporter().importDashboard(new StringReader(json));
    }

    @Test(expected = IOException.class)
    public void testImportMissingName() throws IOException
    {
        String json = """
                        {
                          "columns": []
                        }
                        """;

        new DashboardTemplateImporter().importDashboard(new StringReader(json));
    }

    @Test(expected = IOException.class)
    public void testImportMissingColumns() throws IOException
    {
        String json = """
                        {
                          "name": "Dashboard"
                        }
                        """;

        new DashboardTemplateImporter().importDashboard(new StringReader(json));
    }

    @Test(expected = IOException.class)
    public void testImportColumnsWithWrongType() throws IOException
    {
        // regression test: "columns" as an object instead of a list must not
        // throw an uncaught ClassCastException but a well-defined IOException
        String json = """
                        {
                          "name": "Dashboard",
                          "columns": {}
                        }
                        """;

        new DashboardTemplateImporter().importDashboard(new StringReader(json));
    }

    @Test(expected = IOException.class)
    public void testImportColumnEntryWithWrongType() throws IOException
    {
        String json = """
                        {
                          "name": "Dashboard",
                          "columns": [ "not-an-object" ]
                        }
                        """;

        new DashboardTemplateImporter().importDashboard(new StringReader(json));
    }

    @Test(expected = IOException.class)
    public void testImportWidgetsWithWrongType() throws IOException
    {
        // regression test: "widgets" as an object instead of a list must not
        // throw an uncaught ClassCastException but a well-defined IOException
        String json = """
                        {
                          "name": "Dashboard",
                          "columns": [
                            { "weight": 1, "widgets": {} }
                          ]
                        }
                        """;

        new DashboardTemplateImporter().importDashboard(new StringReader(json));
    }

    @Test(expected = IOException.class)
    public void testImportWidgetTypeWithWrongType() throws IOException
    {
        String json = """
                        {
                          "name": "Dashboard",
                          "columns": [
                            { "weight": 1, "widgets": [ { "type": 42 } ] }
                          ]
                        }
                        """;

        new DashboardTemplateImporter().importDashboard(new StringReader(json));
    }
}
