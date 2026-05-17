package name.abuchen.portfolio.model;

import java.io.IOException;
import java.io.Reader;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.MalformedJsonException;

import name.abuchen.portfolio.Messages;

/**
 * Imports a dashboard from a JSON template file. The template format matches the
 * output of {@link DashboardTemplateExporter}. A new UUID is assigned to the
 * imported dashboard so it can be safely added to any client.
 */
public class DashboardTemplateImporter
{
    private static final int CURRENT_VERSION = 1;

    /**
     * Reads a JSON dashboard template from the given reader, validates its
     * structure, and returns a new {@link Dashboard} object with a fresh UUID.
     *
     * @param reader
     *            the reader providing the JSON content
     * @return a new Dashboard object populated from the template
     * @throws IOException
     *             if the JSON is invalid, required fields are missing, or the
     *             version is unsupported
     */
    public Dashboard importDashboard(Reader reader) throws IOException
    {
        try
        {
            Gson gson = new Gson();
            Map<String, Object> json = gson.fromJson(reader, new TypeToken<Map<String, Object>>()
            {
            }.getType());

            if (json == null)
                throw new IOException(MessageFormat.format(Messages.MsgJSONFormatInvalid, "null")); //$NON-NLS-1$

            validateTemplate(json);

            return buildDashboard(json);
        }
        catch (JsonParseException e)
        {
            if (e.getCause() instanceof MalformedJsonException mfe)
                throw mfe;
            else
                throw new IOException(e.getMessage());
        }
    }

    private void validateTemplate(Map<String, Object> json) throws IOException
    {
        // validate version: must be 1 or absent (defaults to 1)
        Object versionObj = json.get("version"); //$NON-NLS-1$
        if (versionObj != null)
        {
            int version;
            if (versionObj instanceof Number number)
                version = number.intValue();
            else
                throw new IOException(MessageFormat.format(Messages.MsgJSONFormatInvalid, "version")); //$NON-NLS-1$

            if (version != CURRENT_VERSION)
                throw new IOException(MessageFormat.format(Messages.MsgJSONFormatInvalid,
                                "version " + version)); //$NON-NLS-1$
        }

        // validate required fields
        if (!json.containsKey("name") || json.get("name") == null) //$NON-NLS-1$ //$NON-NLS-2$
            throw new IOException(MessageFormat.format(Messages.MsgJSONFormatInvalid, "name")); //$NON-NLS-1$

        if (!json.containsKey("columns") || json.get("columns") == null) //$NON-NLS-1$ //$NON-NLS-2$
            throw new IOException(MessageFormat.format(Messages.MsgJSONFormatInvalid, "columns")); //$NON-NLS-1$
    }

    @SuppressWarnings("unchecked")
    private Dashboard buildDashboard(Map<String, Object> json)
    {
        Dashboard dashboard = new Dashboard(UUID.randomUUID().toString());
        dashboard.setName((String) json.get("name")); //$NON-NLS-1$

        // dashboard-level configuration
        Object configObj = json.get("configuration"); //$NON-NLS-1$
        if (configObj instanceof Map<?, ?> configMap)
        {
            for (Map.Entry<?, ?> entry : configMap.entrySet())
                dashboard.getConfiguration().put(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
        }

        // columns
        List<Map<String, Object>> columnsList = (List<Map<String, Object>>) json.get("columns"); //$NON-NLS-1$
        List<Dashboard.Column> columns = new ArrayList<>();

        for (Map<String, Object> columnMap : columnsList)
        {
            Dashboard.Column column = new Dashboard.Column();

            Object weightObj = columnMap.get("weight"); //$NON-NLS-1$
            if (weightObj instanceof Number number)
                column.setWeight(number.intValue());

            // widgets
            List<Map<String, Object>> widgetsList = (List<Map<String, Object>>) columnMap.get("widgets"); //$NON-NLS-1$
            if (widgetsList != null)
            {
                for (Map<String, Object> widgetMap : widgetsList)
                {
                    Dashboard.Widget widget = new Dashboard.Widget();
                    widget.setType((String) widgetMap.get("type")); //$NON-NLS-1$
                    widget.setLabel((String) widgetMap.get("label")); //$NON-NLS-1$

                    Object widgetConfigObj = widgetMap.get("configuration"); //$NON-NLS-1$
                    if (widgetConfigObj instanceof Map<?, ?> widgetConfigMap)
                    {
                        for (Map.Entry<?, ?> entry : widgetConfigMap.entrySet())
                            widget.getConfiguration().put(String.valueOf(entry.getKey()),
                                            String.valueOf(entry.getValue()));
                    }

                    column.getWidgets().add(widget);
                }
            }

            columns.add(column);
        }

        dashboard.setColumns(columns);
        return dashboard;
    }
}
