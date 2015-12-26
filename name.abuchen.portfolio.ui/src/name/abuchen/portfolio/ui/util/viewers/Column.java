package name.abuchen.portfolio.ui.util.viewers;

import java.text.MessageFormat;

import org.eclipse.jface.viewers.CellLabelProvider;

public class Column
{
    /**
     * Uniquely identifies a column to store/load a configuration
     */
    private String id;

    private String label;
    private int style;
    private int defaultWidth;
    private boolean isMoveable = true;
    private boolean isVisible = true;
    private boolean isRemovable = true;
    private ColumnViewerSorter sorter;
    private Integer defaultSortDirection;
    private CellLabelProvider labelProvider;

    private String optionsMenuLabel;
    private String optionsColumnLabel;
    private Integer[] options;

    private String groupLabel;
    private String menuLabel;
    private String description;

    private ColumnEditingSupport editingSupport;

    public Column(String label, int style, int defaultWidth)
    {
        this(null, label, style, defaultWidth);
    }

    public Column(String id, String label, int style, int defaultWidth)
    {
        this.id = id;
        this.label = label;
        this.style = style;
        this.defaultWidth = defaultWidth;
    }

    /* package */String getId()
    {
        return id;
    }

    /* package */void setId(String id)
    {
        this.id = id;
    }

    public void setVisible(boolean isVisible)
    {
        this.isVisible = isVisible;
    }

    public void setMoveable(boolean isMoveable)
    {
        this.isMoveable = isMoveable;
    }

    public void setRemovable(boolean isRemovable)
    {
        this.isRemovable = isRemovable;
    }

    public void setSorter(ColumnViewerSorter sorter)
    {
        this.sorter = sorter;
    }

    public void setSorter(ColumnViewerSorter sorter, int direction)
    {
        setSorter(sorter);
        this.defaultSortDirection = direction;
    }

    public void setLabelProvider(CellLabelProvider labelProvider)
    {
        this.labelProvider = labelProvider;
    }

    public void setOptions(String menuPattern, String columnPattern, Integer... options)
    {
        this.optionsMenuLabel = menuPattern;
        this.optionsColumnLabel = columnPattern;
        this.options = options;
    }

    public void setGroupLabel(String groupLabel)
    {
        this.groupLabel = groupLabel;
    }

    public void setMenuLabel(String menuLabel)
    {
        this.menuLabel = menuLabel;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public void setEditingSupport(ColumnEditingSupport editingSupport)
    {
        this.editingSupport = editingSupport;
    }

    /* package */String getLabel()
    {
        return label;
    }

    /* package */String getMenuLabel()
    {
        return menuLabel != null ? menuLabel : label;
    }

    /* package */String getDescription()
    {
        return description;
    }

    /* package */int getStyle()
    {
        return style;
    }

    /* package */int getDefaultWidth()
    {
        return defaultWidth;
    }

    /* package */boolean isVisible()
    {
        return isVisible;
    }

    /* package */boolean isRemovable()
    {
        return isRemovable;
    }

    /* package */ColumnViewerSorter getSorter()
    {
        return sorter;
    }

    /* package */Integer getDefaultSortDirection()
    {
        return defaultSortDirection;
    }

    /* package */CellLabelProvider getLabelProvider()
    {
        return labelProvider;
    }

    /* package */boolean hasOptions()
    {
        return options != null;
    }

    /* package */Integer[] getOptions()
    {
        return options;
    }

    /* package */String getOptionsColumnLabel()
    {
        return optionsColumnLabel;
    }

    /* package */String getOptionsMenuLabel()
    {
        return optionsMenuLabel;
    }

    /* package */String getGroupLabel()
    {
        return groupLabel;
    }

    /* package */boolean isMoveable()
    {
        return isMoveable;
    }

    public ColumnEditingSupport getEditingSupport()
    {
        return editingSupport;
    }

    /* package */String getText(Object option)
    {
        return option == null ? getLabel() : MessageFormat.format(getOptionsColumnLabel(), option);
    }

    /* package */String getToolTipText()
    {
        if (description != null)
            return description;
        else if (menuLabel != null)
            return menuLabel;
        else
            return null;
    }
}
