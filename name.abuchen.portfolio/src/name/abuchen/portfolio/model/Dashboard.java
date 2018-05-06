package name.abuchen.portfolio.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Dashboard
{
    public enum Config
    {
        REPORTING_PERIOD, DATA_SERIES, CONFIG_UUID, AGGREGATION, EXCHANGE_RATE_SERIES, COLOR_SCHEMA, LAYOUT;
    }
    
    public static class Column
    {
        private List<Widget> widgets = new ArrayList<>();

        public List<Widget> getWidgets()
        {
            return widgets;
        }

        public void setWidgets(List<Widget> widgets)
        {
            this.widgets = widgets;
        }
    }

    public static class Widget
    {
        private String type;
        private String label;
        private Map<String, String> configuration;

        public String getType()
        {
            return type;
        }

        public void setType(String type)
        {
            this.type = type;
        }

        public String getLabel()
        {
            return label;
        }

        public void setLabel(String label)
        {
            this.label = label;
        }

        public Map<String, String> getConfiguration()
        {
            if (configuration == null)
                configuration = new HashMap<>();

            return configuration;
        }
    }

    private String name;
    private Map<String, String> configuration;
    private List<Column> columns;

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public Map<String, String> getConfiguration()
    {
        if (configuration == null)
            configuration = new HashMap<>();

        return configuration;
    }

    public List<Column> getColumns()
    {
        if (columns == null)
            columns = new ArrayList<>();

        return columns;
    }

    public void setColumns(List<Column> columns)
    {
        this.columns = columns;
    }

    public Dashboard copy()
    {
        Dashboard copy = new Dashboard();
        copy.setName(this.name);
        copy.getConfiguration().putAll(this.getConfiguration());

        for (Column column : columns)
        {
            Column copyColumn = new Column();
            column.getWidgets().stream().map(w -> {
                Widget c = new Widget();
                c.setLabel(w.getLabel());
                c.setType(w.getType());
                c.getConfiguration().putAll(w.getConfiguration());
                return c;
            }).forEach(copyColumn.getWidgets()::add);
            copy.getColumns().add(copyColumn);
        }
        return copy;
    }
}
