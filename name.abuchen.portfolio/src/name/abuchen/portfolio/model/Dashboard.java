package name.abuchen.portfolio.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Dashboard
{
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
        private Map<String, String> configuration = new HashMap<>();

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
            return configuration;
        }

        public void setConfiguration(Map<String, String> configuration)
        {
            this.configuration = configuration;
        }
    }

    private String name;
    private List<Column> columns = new ArrayList<>();

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public List<Column> getColumns()
    {
        return columns;
    }

    public void setColumns(List<Column> columns)
    {
        this.columns = columns;
    }
}
