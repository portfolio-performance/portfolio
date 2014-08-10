package name.abuchen.portfolio.ui.util;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.util.ConfigurationStore.ConfigurationStoreOwner;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.layout.TreeColumnLayout;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.ViewerColumn;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.Widget;

public class ShowHideColumnHelper implements IMenuListener, ConfigurationStoreOwner
{
    private abstract static class ViewerPolicy
    {
        abstract ColumnViewer getViewer();

        abstract int getColumnCount();

        abstract Widget[] getColumns();

        abstract Widget getColumn(int index);

        abstract Widget getSortColumn();

        abstract int[] getColumnOrder();

        abstract int getSortDirection();

        abstract int getWidth(Widget col);

        abstract void create(Column column, Object option, Integer direction, int width);

        void setCommonParameters(Column column, ViewerColumn viewerColumn, Integer direction)
        {
            viewerColumn.setLabelProvider(column.getLabelProvider());

            if (column.getSorter() != null)
            {
                column.getSorter().attachTo(getViewer(), viewerColumn);
                if (direction != null)
                    column.getSorter().setSorter(direction);
            }

            if (column.getEditingSupport() != null)
                viewerColumn.setEditingSupport(new ColumnEditingSupportWrapper(getViewer(), column.getEditingSupport()));
        }

        void setRedraw(boolean redraw)
        {
            getViewer().getControl().setRedraw(redraw);
        }
    }

    private static class TableViewerPolicy extends ViewerPolicy
    {
        private TableViewer table;
        private TableColumnLayout layout;

        public TableViewerPolicy(TableViewer table, TableColumnLayout layout)
        {
            this.table = table;
            this.layout = layout;
        }

        @Override
        public ColumnViewer getViewer()
        {
            return table;
        }

        @Override
        public int getColumnCount()
        {
            return table.getTable().getColumnCount();
        }

        @Override
        public Widget[] getColumns()
        {
            return table.getTable().getColumns();
        }

        @Override
        public Widget getColumn(int index)
        {
            return table.getTable().getColumn(index);
        }

        @Override
        public Widget getSortColumn()
        {
            return table.getTable().getSortColumn();
        }

        @Override
        public int[] getColumnOrder()
        {
            return table.getTable().getColumnOrder();
        }

        @Override
        public int getSortDirection()
        {
            return table.getTable().getSortDirection();
        }

        @Override
        public int getWidth(Widget col)
        {
            return ((TableColumn) col).getWidth();
        }

        @Override
        public void create(Column column, Object option, Integer direction, int width)
        {
            TableViewerColumn col = new TableViewerColumn(table, column.getStyle());

            TableColumn tableColumn = col.getColumn();
            tableColumn.setText(column.getText(option));
            tableColumn.setToolTipText(column.getToolTipText());
            tableColumn.setMoveable(column.isMoveable());
            tableColumn.setWidth(width);
            tableColumn.setData(Column.class.getName(), column);
            tableColumn.setData(OPTIONS_KEY, option);

            layout.setColumnData(tableColumn, new ColumnPixelData(width));

            setCommonParameters(column, col, direction);
        }
    }

    private static class TreeViewerPolicy extends ViewerPolicy
    {
        private TreeViewer tree;
        private TreeColumnLayout layout;

        public TreeViewerPolicy(TreeViewer tree, TreeColumnLayout layout)
        {
            this.tree = tree;
            this.layout = layout;
        }

        @Override
        public ColumnViewer getViewer()
        {
            return tree;
        }

        @Override
        public int getColumnCount()
        {
            return tree.getTree().getColumnCount();
        }

        @Override
        public Widget[] getColumns()
        {
            return tree.getTree().getColumns();
        }

        @Override
        public Widget getColumn(int index)
        {
            return tree.getTree().getColumn(index);
        }

        @Override
        public Widget getSortColumn()
        {
            return tree.getTree().getSortColumn();
        }

        @Override
        public int[] getColumnOrder()
        {
            return tree.getTree().getColumnOrder();
        }

        @Override
        public int getSortDirection()
        {
            return tree.getTree().getSortDirection();
        }

        @Override
        public int getWidth(Widget col)
        {
            return ((TreeColumn) col).getWidth();
        }

        @Override
        public void create(Column column, Object option, Integer direction, int width)
        {
            TreeViewerColumn col = new TreeViewerColumn(tree, column.getStyle());

            TreeColumn treeColumn = col.getColumn();
            treeColumn.setText(column.getText(option));
            treeColumn.setToolTipText(column.getToolTipText());
            treeColumn.setMoveable(column.isMoveable());
            treeColumn.setWidth(width);
            treeColumn.setData(Column.class.getName(), column);
            treeColumn.setData(OPTIONS_KEY, option);

            layout.setColumnData(treeColumn, new ColumnPixelData(width));

            setCommonParameters(column, col, direction);
        }
    }

    /* package */static final String OPTIONS_KEY = Column.class.getName() + "_OPTION"; //$NON-NLS-1$
    private static final Pattern CONFIG_PATTERN = Pattern.compile("^([^=]*)=(?:(\\d*)\\|)?(?:(\\d*)\\$)?(\\d*)$"); //$NON-NLS-1$

    private String identifier;
    private boolean isUserConfigured = false;

    private List<Column> columns = new ArrayList<Column>();
    private Map<String, Column> id2column = new HashMap<String, Column>();

    private IPreferenceStore preferences;
    private ConfigurationStore store;

    private ViewerPolicy policy;
    private Menu contextMenu;

    public ShowHideColumnHelper(String identifier, IPreferenceStore preferences, TreeViewer viewer,
                    TreeColumnLayout layout)
    {
        this(identifier, null, preferences, new TreeViewerPolicy(viewer, layout));
    }

    public ShowHideColumnHelper(String identifier, IPreferenceStore preferences, TableViewer viewer,
                    TableColumnLayout layout)
    {
        this(identifier, null, preferences, viewer, layout);
    }

    public ShowHideColumnHelper(String identifier, Client client, IPreferenceStore preferences, TableViewer viewer,
                    TableColumnLayout layout)
    {
        this(identifier, client, preferences, new TableViewerPolicy(viewer, layout));
    }

    private ShowHideColumnHelper(String identifier, Client client, IPreferenceStore preferences, ViewerPolicy policy)
    {
        this.identifier = identifier;
        this.policy = policy;
        this.preferences = preferences;

        if (client != null)
            this.store = new ConfigurationStore(identifier, client, this);

        this.policy.getViewer().getControl().addDisposeListener(new DisposeListener()
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
        this.preferences.setValue(identifier, getCurrentConfiguration());

        if (contextMenu != null)
            contextMenu.dispose();

        if (store != null)
            store.dispose();
    }

    public void showSaveMenu(Shell shell)
    {
        if (store == null)
            throw new UnsupportedOperationException();

        store.showSaveMenu(shell);
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
        for (Widget col : policy.getColumns())
        {
            Column column = (Column) col.getData(Column.class.getName());
            if (column.hasOptions())
            {
                List<Object> options = visible.get(column);
                if (options == null)
                {
                    options = new ArrayList<Object>();
                    visible.put(column, options);
                }
                options.add(col.getData(OPTIONS_KEY));
            }
            else
            {
                visible.put(column, null);
            }
        }

        Map<String, IMenuManager> groups = new HashMap<String, IMenuManager>();

        for (final Column column : columns)
        {
            IMenuManager managerToAdd = manager;

            // create a sub-menu for each group label
            if (column.getGroupLabel() != null)
            {
                managerToAdd = groups.get(column.getGroupLabel());

                if (managerToAdd == null)
                {
                    managerToAdd = new MenuManager(column.getGroupLabel());
                    groups.put(column.getGroupLabel(), managerToAdd);
                    manager.add(managerToAdd);
                }
            }

            if (column.hasOptions())
            {
                List<Object> options = visible.get(column);

                MenuManager subMenu = new MenuManager(column.getMenuLabel());

                for (final Object option : column.getOptions())
                {
                    boolean isVisible = options != null && options.contains(option);
                    String label = MessageFormat.format(column.getOptionsMenuLabel(), option);
                    addShowHideAction(subMenu, column, label, isVisible, option);
                }

                managerToAdd.add(subMenu);
            }
            else
            {
                addShowHideAction(managerToAdd, column, column.getMenuLabel(), visible.containsKey(column), null);
            }
        }

        addMenuAddGroup(groups, visible);

        manager.add(new Separator());

        manager.add(new Action(Messages.MenuResetColumns)
        {
            @Override
            public void run()
            {
                doResetColumns();
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
                    if (column.isRemovable())
                        destroyColumnWithOption(column, option);
                }
                else
                {
                    policy.create(column, option, column.getDefaultSortDirection(), column.getDefaultWidth());
                    policy.getViewer().refresh(true);
                }
            }
        };
        action.setChecked(isChecked);
        manager.add(action);
    }

    public void destroyColumnWithOption(Column column, Object option)
    {
        for (Widget widget : policy.getColumns())
        {
            if (column.equals(widget.getData(Column.class.getName())) //
                            && (option == null || option.equals(widget.getData(OPTIONS_KEY))))
            {
                try
                {
                    policy.setRedraw(false);
                    widget.dispose();
                }
                finally
                {
                    policy.getViewer().refresh();
                    policy.setRedraw(true);
                }
                break;
            }
        }
    }

    private void addMenuAddGroup(Map<String, IMenuManager> groups, final Map<Column, List<Object>> visible)
    {
        for (final Entry<String, IMenuManager> entry : groups.entrySet())
        {
            IMenuManager manager = entry.getValue();
            manager.add(new Separator());
            manager.add(new Action(Messages.MenuAddAll)
            {
                @Override
                public void run()
                {
                    doAddGroup(entry.getKey(), visible);
                }
            });
            manager.add(new Action(Messages.MenuRemoveAll)
            {
                @Override
                public void run()
                {
                    doRemoveGroup(entry.getKey());
                }
            });
        }
    }

    private void doAddGroup(String group, Map<Column, List<Object>> visible)
    {
        try
        {
            policy.setRedraw(false);

            for (Column column : columns)
            {
                if (!group.equals(column.getGroupLabel()))
                    continue;
                if (visible.containsKey(column))
                    continue;

                if (column.hasOptions())
                {
                    for (Integer option : column.getOptions())
                        policy.create(column, option, column.getDefaultSortDirection(), column.getDefaultWidth());
                }
                else
                {
                    policy.create(column, null, column.getDefaultSortDirection(), column.getDefaultWidth());
                }
            }
        }
        finally
        {
            policy.getViewer().refresh();
            policy.setRedraw(true);
        }
    }

    private void doRemoveGroup(String group)
    {
        try
        {
            policy.setRedraw(false);

            for (Widget col : policy.getColumns())
            {
                Column column = (Column) col.getData(Column.class.getName());
                if (group.equals(column.getGroupLabel()))
                    col.dispose();
            }
        }
        finally
        {
            policy.getViewer().refresh();
            policy.setRedraw(true);
        }
    }

    public boolean isUserConfigured()
    {
        return isUserConfigured;
    }

    public void addColumn(Column column)
    {
        // columns used to be identified by index only
        if (column.getId() == null)
            column.setId(Integer.toString(columns.size()));

        columns.add(column);
        id2column.put(column.getId(), column);
    }

    public void createColumns()
    {
        createFromColumnConfig();

        if (policy.getColumnCount() > 0)
        {
            isUserConfigured = true;
        }
        else
        {
            for (Column column : columns)
            {
                if (column.isVisible())
                    policy.create(column, null, column.getDefaultSortDirection(), column.getDefaultWidth());
            }
        }
    }

    private void createFromColumnConfig()
    {
        String config = this.preferences.getString(identifier);

        // fallback to previous store
        if (config == null || config.trim().length() == 0)
            config = PortfolioPlugin.getDefault().getPreferenceStore().getString(identifier);
        createFromColumnConfig(config);
    }

    private void createFromColumnConfig(String config)
    {
        if (config == null || config.trim().length() == 0)
            return;

        try
        {
            policy.setRedraw(false);
            int count = policy.getColumnCount();

            StringTokenizer tokens = new StringTokenizer(config, ";"); //$NON-NLS-1$
            while (tokens.hasMoreTokens())
            {
                Matcher matcher = CONFIG_PATTERN.matcher(tokens.nextToken());
                if (!matcher.matches())
                    continue;

                // index
                Column col = id2column.get(matcher.group(1));
                if (col == null)
                    continue;

                // option
                String o = matcher.group(2);
                Integer option = o != null ? Integer.parseInt(o) : null;

                // direction
                String d = matcher.group(3);
                Integer direction = d != null ? Integer.parseInt(d) : null;

                // width
                int width = Integer.parseInt(matcher.group(4));

                policy.create(col, option, direction, width);
            }

            for (int ii = 0; ii < count; ii++)
                policy.getColumn(0).dispose();
        }
        catch (NumberFormatException e)
        {
            PortfolioPlugin.log(e);
        }
        finally
        {
            policy.setRedraw(true);
        }
    }

    private void doResetColumns()
    {
        try
        {
            // first add, then remove columns
            // (otherwise rendering of first column is broken)
            policy.setRedraw(false);

            int count = policy.getColumnCount();

            for (Column column : columns)
            {
                if (column.isVisible())
                    policy.create(column, null, column.getDefaultSortDirection(), column.getDefaultWidth());
            }

            for (int ii = 0; ii < count; ii++)
                policy.getColumn(0).dispose();
        }
        finally
        {
            policy.getViewer().refresh();
            policy.setRedraw(true);
        }
    }

    @Override
    public String getCurrentConfiguration()
    {
        StringBuilder buf = new StringBuilder();

        Widget sortedColumn = policy.getSortColumn();

        for (int index : policy.getColumnOrder())
        {
            Widget col = policy.getColumn(index);
            Column column = (Column) col.getData(Column.class.getName());
            buf.append(column.getId()).append('=');

            Object option = col.getData(OPTIONS_KEY);
            if (option != null)
                buf.append(option).append('|');
            if (col.equals(sortedColumn))
                buf.append(policy.getSortDirection()).append('$');

            buf.append(policy.getWidth(col)).append(';');
        }
        return buf.toString();
    }

    @Override
    public void handleConfigurationReset()
    {
        doResetColumns();
    }

    @Override
    public void handleConfigurationPicked(String data)
    {
        createFromColumnConfig(data);
        policy.getViewer().refresh();
    }
}
