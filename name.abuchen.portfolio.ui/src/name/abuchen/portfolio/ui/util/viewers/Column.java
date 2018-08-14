package name.abuchen.portfolio.ui.util.viewers;

import java.util.Comparator;
import java.util.List;

import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.swt.widgets.Shell;

public class Column
{
    public interface Options<E>
    {
        List<E> getOptions();

        /**
         * Converts serialized option back into option instance.
         */
        E valueOf(String s);

        /**
         * Converts an option instance into a string (to store column
         * configuration).
         */
        String toString(E option);

        String getColumnLabel(E option);

        String getMenuLabel(E option);

        String getDescription(E option);

        boolean canCreateNewOptions();

        E createNewOption(Shell shell);
    }

    /**
     * Uniquely identifies a column to store/load a configuration
     */
    private String id;

    private String label;
    private int style;
    private int defaultWidth;
    private boolean isVisible = true;
    private boolean isRemovable = true;
    private ColumnViewerSorter sorter;
    private Integer defaultSortDirection;
    private CellLabelProvider labelProvider;

    private Options<Object> options;

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

    public void setRemovable(boolean isRemovable)
    {
        this.isRemovable = isRemovable;
    }

    public void setSorter(ColumnViewerSorter sorter)
    {
        this.sorter = sorter;
    }

    public void setComparator(Comparator<Object> comparator)
    {
        this.sorter = ColumnViewerSorter.create(comparator);
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

    @SuppressWarnings("unchecked")
    public void setOptions(Options<?> options)
    {
        this.options = (Options<Object>) options;
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

    public ColumnViewerSorter getSorter()
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

                    /* package */Options<Object> getOptions()
    {
        return options;
    }

                    /* package */String getGroupLabel()
    {
        return groupLabel;
    }

    public ColumnEditingSupport getEditingSupport()
    {
        return editingSupport;
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
