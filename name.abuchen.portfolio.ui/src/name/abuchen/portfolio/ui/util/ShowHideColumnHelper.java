package name.abuchen.portfolio.ui.util;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import name.abuchen.portfolio.ui.PortfolioPlugin;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

public class ShowHideColumnHelper implements IMenuListener
{
    public static class OptionLabelProvider extends CellLabelProvider
    {
        public String getText(Object element, Integer option)
        {
            return null;
        }

        public Color getForeground(Object element, Integer option)
        {
            return null;
        }

        @Override
        public void update(ViewerCell cell)
        {
            Table table = (Table) cell.getControl();
            int columnIndex = cell.getColumnIndex();
            Integer option = (Integer) table.getColumn(columnIndex).getData(OPTIONS_KEY);

            Object element = cell.getElement();
            cell.setText(getText(element, option));
            cell.setForeground(getForeground(element, option));
        }
    }

    public static class Column
    {
        private String label;
        private int style;
        private int defaultWidth;
        private boolean isVisible = true;
        private ColumnViewerSorter sorter;
        private CellLabelProvider labelProvider;

        private String optionsMenuLabel;
        private String optionsColumnLabel;
        private Integer[] options;

        public Column(String label, int style, int defaultWidth)
        {
            this.label = label;
            this.style = style;
            this.defaultWidth = defaultWidth;
        }

        public void setVisible(boolean isVisible)
        {
            this.isVisible = isVisible;
        }

        public void setSorter(ColumnViewerSorter sorter)
        {
            this.sorter = sorter;
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

        String getLabel()
        {
            return label;
        }

        int getStyle()
        {
            return style;
        }

        int getDefaultWidth()
        {
            return defaultWidth;
        }

        boolean isVisible()
        {
            return isVisible;
        }

        ColumnViewerSorter getSorter()
        {
            return sorter;
        }

        CellLabelProvider getLabelProvider()
        {
            return labelProvider;
        }

        boolean hasOptions()
        {
            return options != null;
        }

        Integer[] getOptions()
        {
            return options;
        }

        String getOptionsColumnLabel()
        {
            return optionsColumnLabel;
        }

        String getOptionsMenuLabel()
        {
            return optionsMenuLabel;
        }

        private void create(TableViewer viewer, TableColumnLayout layout, Object option)
        {
            create(viewer, layout, option, getDefaultWidth());
        }

        private void create(TableViewer viewer, TableColumnLayout layout, Object option, int width)
        {
            TableViewerColumn col = new TableViewerColumn(viewer, getStyle());
            col.getColumn().setText(option == null ? getLabel() : MessageFormat.format(optionsColumnLabel, option));
            col.getColumn().setMoveable(true);
            col.setLabelProvider(getLabelProvider());

            layout.setColumnData(col.getColumn(), new ColumnPixelData(width));
            col.getColumn().setWidth(width);

            if (sorter != null)
                sorter.attachTo(viewer, col);

            col.getColumn().setData(Column.class.getName(), this);
            col.getColumn().setData(OPTIONS_KEY, option);
        }

        public void destroy(TableViewer viewer, Object option)
        {
            for (TableColumn column : viewer.getTable().getColumns())
            {
                if (column.getData(Column.class.getName()) == this //
                                && (option == null || option.equals(column.getData(OPTIONS_KEY))))
                {
                    try
                    {
                        viewer.getTable().setRedraw(false);
                        column.dispose();
                    }
                    finally
                    {
                        viewer.getTable().setRedraw(true);
                    }
                    break;
                }
            }
        }
    }

    private static final String OPTIONS_KEY = Column.class.getName() + "_OPTION"; //$NON-NLS-1$

    private String identifier;
    private boolean isUserConfigured = false;

    private List<Column> columns = new ArrayList<Column>();

    private TableViewer viewer;
    private TableColumnLayout layout;
    private Menu contextMenu;

    public ShowHideColumnHelper(String identifier, TableViewer viewer, TableColumnLayout layout)
    {
        this.identifier = identifier;
        this.viewer = viewer;
        this.layout = layout;

        this.viewer.getTable().addDisposeListener(new DisposeListener()
        {
            @Override
            public void widgetDisposed(DisposeEvent e)
            {
                ShowHideColumnHelper.this.widgetDisposed();
            }
        });
    }

    private void widgetDisposed()
    {
        persistColumnConfig();

        if (contextMenu != null)
            contextMenu.dispose();
    }

    public void showHideShowColumnsMenu(Shell shell)
    {
        if (contextMenu == null)
        {
            MenuManager menuMgr = new MenuManager("#PopupMenu"); //$NON-NLS-1$
            menuMgr.setRemoveAllWhenShown(true);
            menuMgr.addMenuListener(this);

            contextMenu = menuMgr.createContextMenu(shell);
        }

        contextMenu.setVisible(true);
    }

    @Override
    public void menuAboutToShow(IMenuManager manager)
    {
        final Map<Column, List<Object>> visible = new HashMap<Column, List<Object>>();
        for (TableColumn col : viewer.getTable().getColumns())
        {
            Column column = (Column) col.getData(Column.class.getName());
            if (column.hasOptions())
            {
                List<Object> options = visible.get(column);
                if (options == null)
                    visible.put(column, options = new ArrayList<Object>());
                options.add(col.getData(OPTIONS_KEY));
            }
            else
            {
                visible.put(column, null);
            }
        }

        for (final Column column : columns)
        {
            if (column.hasOptions())
            {
                List<Object> options = visible.get(column);

                MenuManager subMenu = new MenuManager(column.getLabel());

                for (final Object option : column.getOptions())
                {
                    boolean isVisible = options != null && options.contains(option);
                    String label = MessageFormat.format(column.getOptionsMenuLabel(), option);
                    addShowHideAction(subMenu, column, label, isVisible, option);
                }

                manager.add(subMenu);
            }
            else
            {
                addShowHideAction(manager, column, column.getLabel(), visible.containsKey(column), null);
            }
        }
    }

    private void addShowHideAction(IMenuManager manager, final Column column, String label, final boolean isChecked,
                    final Object option)
    {
        Action action = new Action(label)
        {
            @Override
            public void run()
            {
                if (isChecked)
                {
                    column.destroy(viewer, option);
                }
                else
                {
                    column.create(viewer, layout, option);
                    viewer.refresh(true);
                }
            }
        };
        action.setChecked(isChecked);
        manager.add(action);
    }

    public boolean isUserConfigured()
    {
        return isUserConfigured;
    }

    public void addColumn(Column column)
    {
        columns.add(column);
    }

    public void createColumns()
    {
        createFromColumnConfig();

        if (viewer.getTable().getColumnCount() > 0)
        {
            isUserConfigured = true;
        }
        else
        {
            for (Column column : columns)
            {
                if (column.isVisible())
                    column.create(viewer, layout, null);
            }
        }
    }

    private void createFromColumnConfig()
    {
        String config = PortfolioPlugin.getDefault().getPreferenceStore().getString(identifier);
        if (config == null || config.trim().length() == 0)
            return;

        try
        {
            StringTokenizer tokens = new StringTokenizer(config, ";"); //$NON-NLS-1$
            while (tokens.hasMoreTokens())
            {
                String def = tokens.nextToken();
                int p = def.indexOf('=');
                int o = def.indexOf('|', p);

                Column col = columns.get(Integer.parseInt(def.substring(0, p)));

                int width = 0;
                Integer option = null;

                if (o < 0)
                {
                    width = Integer.parseInt(def.substring(p + 1));
                }
                else
                {
                    option = Integer.parseInt(def.substring(p + 1, o));
                    width = Integer.parseInt(def.substring(o + 1));
                }
                col.create(viewer, layout, option, width);
            }
        }
        catch (NumberFormatException e)
        {
            PortfolioPlugin.log(e);
        }
    }

    private void persistColumnConfig()
    {
        StringBuilder buf = new StringBuilder();

        for (int index : viewer.getTable().getColumnOrder())
        {
            TableColumn col = viewer.getTable().getColumn(index);
            Column column = (Column) col.getData(Column.class.getName());
            buf.append(columns.indexOf(column)).append('=');

            Object option = col.getData(OPTIONS_KEY);
            if (option != null)
                buf.append(option).append('|');

            buf.append(col.getWidth()).append(';');
        }
        PortfolioPlugin.getDefault().getPreferenceStore().setValue(identifier, buf.toString());
    }

}
