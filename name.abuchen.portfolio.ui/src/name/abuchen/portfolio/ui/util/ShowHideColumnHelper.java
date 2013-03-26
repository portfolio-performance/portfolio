package name.abuchen.portfolio.ui.util;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
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
        private boolean isMoveable = true;
        private boolean isVisible = true;
        private ColumnViewerSorter sorter;
        private Integer defaultSortDirection;
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

        public void setMoveable(boolean isMoveable)
        {
            this.isMoveable = isMoveable;
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
            create(viewer, layout, option, defaultSortDirection, getDefaultWidth());
        }

        private void create(TableViewer viewer, TableColumnLayout layout, Object option, Integer direction, int width)
        {
            TableViewerColumn col = new TableViewerColumn(viewer, getStyle());
            col.getColumn().setText(option == null ? getLabel() : MessageFormat.format(optionsColumnLabel, option));
            col.getColumn().setMoveable(isMoveable);
            col.setLabelProvider(getLabelProvider());

            layout.setColumnData(col.getColumn(), new ColumnPixelData(width));
            col.getColumn().setWidth(width);

            if (sorter != null)
            {
                sorter.attachTo(viewer, col);
                if (direction != null)
                    sorter.setSorter(direction);
            }

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
    private static final Pattern CONFIG_PATTERN = Pattern.compile("^(\\d*)=(?:(\\d*)\\|)?(?:(\\d*)\\$)?(\\d*)$"); //$NON-NLS-1$

    private String identifier;
    private boolean isUserConfigured = false;
    private boolean doSaveState = true;

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
        if (doSaveState)
            persistColumnConfig();

        if (contextMenu != null)
            contextMenu.dispose();
    }

    public void setDoSaveState(boolean doSaveState)
    {
        this.doSaveState = doSaveState;
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

        manager.add(new Separator());

        manager.add(new Action(Messages.MenuResetColumns)
        {
            @Override
            public void run()
            {
                try
                {
                    // first add, then remove columns
                    // (otherwise rendering of first column is broken)
                    viewer.getTable().setRedraw(false);

                    int count = viewer.getTable().getColumnCount();

                    for (Column column : columns)
                    {
                        if (column.isVisible())
                            column.create(viewer, layout, null);
                    }

                    for (int ii = 0; ii < count; ii++)
                        viewer.getTable().getColumn(0).dispose();
                }
                finally
                {
                    viewer.refresh();
                    viewer.getTable().setRedraw(true);
                }
            }
        });
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
        if (doSaveState)
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
                Matcher matcher = CONFIG_PATTERN.matcher(tokens.nextToken());
                if (!matcher.matches())
                    continue;

                // index
                Column col = columns.get(Integer.parseInt(matcher.group(1)));

                // option
                String o = matcher.group(2);
                Integer option = o != null ? Integer.parseInt(o) : null;

                // direction
                String d = matcher.group(3);
                Integer direction = d != null ? Integer.parseInt(d) : null;

                // width
                int width = Integer.parseInt(matcher.group(4));

                col.create(viewer, layout, option, direction, width);
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

        TableColumn sortedColumn = viewer.getTable().getSortColumn();

        for (int index : viewer.getTable().getColumnOrder())
        {
            TableColumn col = viewer.getTable().getColumn(index);
            Column column = (Column) col.getData(Column.class.getName());
            buf.append(columns.indexOf(column)).append('=');

            Object option = col.getData(OPTIONS_KEY);
            if (option != null)
                buf.append(option).append('|');
            if (col == sortedColumn)
                buf.append(viewer.getTable().getSortDirection()).append('$');

            buf.append(col.getWidth()).append(';');
        }
        PortfolioPlugin.getDefault().getPreferenceStore().setValue(identifier, buf.toString());
    }
}
