package name.abuchen.portfolio.ui.util.viewers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
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
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.swt.widgets.Widget;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.util.ConfigurationStore;
import name.abuchen.portfolio.ui.util.ConfigurationStore.ConfigurationStoreOwner;
import name.abuchen.portfolio.util.TextUtil;

public class ShowHideColumnHelper implements IMenuListener, ConfigurationStoreOwner
{
    @FunctionalInterface
    public interface Listener
    {
        void onConfigurationPicked();
    }

    private abstract static class ViewerPolicy
    {
        /**
         * The changeListener is attached to various SWT events (ordering,
         * resizing or moving columns) in order to store the updated
         * configuration.
         */
        private org.eclipse.swt.widgets.Listener changeListener;

        abstract ColumnViewer getViewer();

        abstract int getColumnCount();

        abstract Widget[] getColumns();

        abstract Widget getColumn(int index);

        abstract Widget getSortColumn();

        abstract Widget getColumnWidget(ViewerColumn column);

        abstract int[] getColumnOrder();

        abstract int getSortDirection();

        abstract int getWidth(Widget col);

        abstract void create(Column column, Object option, Integer direction, int width);

        void setCommonParameters(Column column, ViewerColumn viewerColumn, Integer direction)
        {
            viewerColumn.setLabelProvider(column.getLabelProvider());

            if (column.getSorter() != null)
            {
                if (direction != null)
                    column.getSorter().attachTo(getViewer(), viewerColumn, direction);
                else
                    column.getSorter().attachTo(getViewer(), viewerColumn);

                // add selection listener *after* attaching the viewer sorter
                // because the viewer sorter will add a listener that actually
                // changes the sort order
                if (changeListener != null)
                    getColumnWidget(viewerColumn).addListener(SWT.Selection, changeListener);
            }

            if (column.getEditingSupport() != null)
            {
                viewerColumn.setEditingSupport(
                                new ColumnEditingSupportWrapper(getViewer(), column.getEditingSupport()));
            }

            if (changeListener != null)
            {
                getColumnWidget(viewerColumn).addListener(SWT.Resize, changeListener);
                getColumnWidget(viewerColumn).addListener(SWT.Move, changeListener);
            }
        }

        void setRedraw(boolean redraw)
        {
            getViewer().getControl().setRedraw(redraw);
        }

        public void setChangeListener(org.eclipse.swt.widgets.Listener changeListener)
        {
            this.changeListener = changeListener;
        }

        String wordwrap(String text)
        {
            // other platforms such as Mac and Linux natively wrap tool tip
            // labels, but not Windows
            return Platform.OS_WIN32.equals(Platform.getOS()) ? TextUtil.wordwrap(text) : text;
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
        Widget getColumnWidget(ViewerColumn column)
        {
            return ((TableViewerColumn) column).getColumn();
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
            tableColumn.setMoveable(true);
            tableColumn.setWidth(width);
            tableColumn.setData(Column.class.getName(), column);

            if (column.getImage() != null)
                tableColumn.setImage(column.getImage().image());

            if (option == null)
            {
                tableColumn.setText(column.getLabel());
                tableColumn.setToolTipText(wordwrap(column.getToolTipText()));
            }
            else
            {
                tableColumn.setText(column.getOptions().getColumnLabel(option));
                String description = column.getOptions().getDescription(option);
                tableColumn.setToolTipText(wordwrap(description != null ? description : column.getToolTipText()));

                tableColumn.setData(OPTIONS_KEY, option);
            }

            layout.setColumnData(tableColumn, new ColumnPixelData(width));

            setCommonParameters(column, col, direction);

            if (column.getLabelProvider() instanceof CellItemImageClickedListener)
                setupImageClickedListener(column, tableColumn);
        }

        private void setupImageClickedListener(Column column, TableColumn tableColumn)
        {
            org.eclipse.swt.widgets.Listener listener = event -> {

                int columnIndex = table.getTable().indexOf(tableColumn);
                if (columnIndex == -1)
                    return;

                Point pt = new Point(event.x, event.y);

                TableItem tableItem = table.getTable().getItem(pt);
                if (tableItem == null)
                    return;

                Rectangle rect = tableItem.getImageBounds(columnIndex);
                if (rect.contains(pt))
                    ((CellItemImageClickedListener) column.getLabelProvider()).onImageClicked(tableItem.getData());
            };

            table.getTable().addListener(SWT.MouseUp, listener);
            tableColumn.addDisposeListener(e -> table.getTable().removeListener(SWT.MouseUp, listener));
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
        Widget getColumnWidget(ViewerColumn column)
        {
            return ((TreeViewerColumn) column).getColumn();
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
            treeColumn.setMoveable(true);
            treeColumn.setWidth(width);
            treeColumn.setData(Column.class.getName(), column);

            if (column.getImage() != null)
                treeColumn.setImage(column.getImage().image());

            if (option == null)
            {
                treeColumn.setText(column.getLabel());
                treeColumn.setToolTipText(column.getToolTipText());
            }
            else
            {
                treeColumn.setText(column.getOptions().getColumnLabel(option));
                String description = column.getOptions().getDescription(option);
                treeColumn.setToolTipText(wordwrap(description != null ? description : column.getToolTipText()));
                treeColumn.setData(OPTIONS_KEY, option);
            }

            layout.setColumnData(treeColumn, new ColumnPixelData(width));

            setCommonParameters(column, col, direction);

            if (column.getLabelProvider() instanceof CellItemImageClickedListener)
                setupImageClickedListener(column, treeColumn);
        }

        private void setupImageClickedListener(Column column, TreeColumn treeColumn)
        {
            org.eclipse.swt.widgets.Listener listener = event -> {

                int columnIndex = tree.getTree().indexOf(treeColumn);
                if (columnIndex == -1)
                    return;

                Point pt = new Point(event.x, event.y);
                TreeItem treeItem = tree.getTree().getItem(pt);
                if (treeItem == null)
                    return;

                Rectangle rect = treeItem.getImageBounds(columnIndex);
                if (rect.contains(pt))
                    ((CellItemImageClickedListener) column.getLabelProvider()).onImageClicked(treeItem.getData());
            };

            tree.getTree().addListener(SWT.MouseUp, listener);
            treeColumn.addDisposeListener(e -> tree.getTree().removeListener(SWT.MouseUp, listener));
        }

    }

    /* package */static final String OPTIONS_KEY = Column.class.getName() + "_OPTION"; //$NON-NLS-1$
    private static final Pattern CONFIG_PATTERN = Pattern.compile("^([^=]*)=(?:([^\\|]*)\\|)?(?:(\\d*)\\$)?(\\d*)$"); //$NON-NLS-1$

    private final String identifier;

    private List<Column> columns = new ArrayList<>();
    private Map<String, Column> id2column = new HashMap<>();

    private IPreferenceStore preferences;
    private ConfigurationStore store;
    private List<Listener> listeners = new ArrayList<>();

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
        {
            this.store = new ConfigurationStore(identifier, client, preferences, this);
            this.policy.setChangeListener(e -> store.updateActive(serialize()));
        }

        this.policy.getViewer().getControl().addDisposeListener(e -> ShowHideColumnHelper.this.widgetDisposed());
    }

    private void widgetDisposed()
    {
        if (store != null)
            store.updateActive(serialize());
        else
            preferences.setValue(identifier, serialize());

        if (contextMenu != null)
            contextMenu.dispose();
    }

    public String getConfigurationName()
    {
        return store != null ? store.getActiveName() : null;
    }

    public void addListener(Listener l)
    {
        this.listeners.add(l);
    }

    public void setToolBarManager(ToolBarManager toolBar)
    {
        if (store == null)
            throw new NullPointerException("store"); //$NON-NLS-1$

        store.setToolBarManager(toolBar);
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
    public void menuAboutToShow(final IMenuManager manager)
    {
        final Map<Column, List<Object>> visible = new HashMap<>();
        for (Widget col : policy.getColumns())
        {
            Column column = (Column) col.getData(Column.class.getName());
            visible.computeIfAbsent(column, k -> new ArrayList<Object>()).add(col.getData(OPTIONS_KEY));
        }

        Map<String, IMenuManager> groups = new HashMap<>();

        for (final Column column : columns)
        {
            IMenuManager managerToAdd = manager;

            // create a sub-menu for each group label
            if (column.getGroupLabel() != null)
            {
                managerToAdd = groups.computeIfAbsent(column.getGroupLabel(), l -> {
                    MenuManager m = new MenuManager(l);
                    manager.add(m);
                    return m;
                });
            }

            if (column.hasOptions())
            {
                List<Object> options = visible.getOrDefault(column, Collections.emptyList());

                MenuManager subMenu = new MenuManager(column.getMenuLabel());

                for (Object option : column.getOptions().getOptions())
                {
                    boolean isVisible = options.contains(option);
                    String label = column.getOptions().getMenuLabel(option);
                    addShowHideAction(subMenu, column, label, isVisible, option);

                    if (isVisible)
                        options.remove(option);
                }

                for (Object option : options)
                {
                    String label = column.getOptions().getMenuLabel(option);
                    addShowHideAction(subMenu, column, label, true, option);
                }

                if (column.getOptions().canCreateNewOptions())
                    addCreateOptionAction(subMenu, column);

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

    private void addCreateOptionAction(MenuManager manager, Column column)
    {
        manager.add(new Separator());
        manager.add(new Action(Messages.MenuCreateColumnConfig)
        {
            @Override
            public void run()
            {
                Object option = column.getOptions().createNewOption(Display.getCurrent().getActiveShell());
                if (option != null)
                {
                    policy.create(column, option, column.getDefaultSortDirection(), column.getDefaultWidth());
                    policy.getViewer().refresh(true);
                    if (store != null)
                        store.updateActive(serialize());
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
                    if (column.isRemovable())
                        destroyColumnWithOption(column, option);
                }
                else
                {
                    policy.create(column, option, column.getDefaultSortDirection(), column.getDefaultWidth());
                    policy.getViewer().refresh(true);
                }

                if (store != null)
                    store.updateActive(serialize());
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

                    Widget sortColumn = policy.getSortColumn();
                    if (widget.equals(sortColumn))
                        policy.getViewer().setComparator(null);

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
                    for (Object element : column.getOptions().getOptions())
                        policy.create(column, element, column.getDefaultSortDirection(), column.getDefaultWidth());
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

        if (policy.getColumnCount() == 0)
        {
            columns.stream().filter(c -> c.isVisible())
                            .forEach(c -> policy.create(c, null, c.getDefaultSortDirection(), c.getDefaultWidth()));
        }
    }

    private void createFromColumnConfig()
    {
        // if a configuration store is used, then migrate the preferences into
        // the store. This is done once. Unfortunately, if the user then does
        // not save the file subsequently, the configuration is lost (e.g. the
        // order and size of the displayed columns). Therefore the key is saved
        // for manual recovery.

        // if no configuration store is used (i.e. column configuration cannot
        // be saved), we continue to use the preferences to store configuration

        String configInPreferences = preferences.getString(identifier);

        if (store != null && !configInPreferences.isEmpty())
        {
            preferences.setToDefault(identifier);
            preferences.setValue("__backup__" + identifier, configInPreferences); //$NON-NLS-1$
            store.insertMigratedConfiguration(configInPreferences);
        }

        String config = store != null ? store.getActive() : configInPreferences;
        createFromColumnConfig(config);
    }

    private void createFromColumnConfig(String config)
    {
        if (config == null || config.trim().length() == 0)
            return;

        try
        {
            policy.setRedraw(false);

            // turn of sorting in case new columns define no viewer comparator
            policy.getViewer().setComparator(null);

            int count = policy.getColumnCount();

            StringTokenizer tokens = new StringTokenizer(config, ";"); //$NON-NLS-1$
            while (tokens.hasMoreTokens())
            {
                try
                {
                    Matcher matcher = CONFIG_PATTERN.matcher(tokens.nextToken());
                    if (!matcher.matches())
                        continue;

                    // index
                    Column col = id2column.get(matcher.group(1));
                    if (col == null)
                        continue;

                    // option
                    Object option = null;

                    if (col.hasOptions())
                    {
                        String o = matcher.group(2);
                        option = col.getOptions().valueOf(o);
                        if (option == null)
                            continue;
                    }

                    // direction
                    String d = matcher.group(3);
                    Integer direction = d != null ? Integer.parseInt(d) : null;

                    // width
                    int width = Integer.parseInt(matcher.group(4));

                    policy.create(col, option, direction, width);
                }
                catch (RuntimeException e)
                {
                    PortfolioPlugin.log(e);
                }
            }

            for (int ii = 0; ii < count; ii++)
                policy.getColumn(0).dispose();
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

            // turn of sorting in case new columns define no viewer comparator
            policy.getViewer().setComparator(null);

            int count = policy.getColumnCount();

            for (Column column : columns)
            {
                // columns w/ options are not created by default
                if (column.isVisible() && !column.hasOptions())
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

        if (store != null)
            store.updateActive(serialize());
    }

    private String serialize()
    {
        StringBuilder buf = new StringBuilder();

        Widget sortedColumn = policy.getSortColumn();

        for (int index : policy.getColumnOrder())
        {
            Widget col = policy.getColumn(index);
            Column column = (Column) col.getData(Column.class.getName());
            buf.append(column.getId()).append('=');

            if (column.hasOptions())
                buf.append(column.getOptions().toString(col.getData(OPTIONS_KEY))).append('|');
            if (col.equals(sortedColumn))
                buf.append(policy.getSortDirection()).append('$');

            buf.append(policy.getWidth(col)).append(';');
        }
        return buf.toString();
    }

    @Override
    public void beforeConfigurationPicked()
    {
        store.updateActive(serialize());
    }

    @Override
    public void onConfigurationPicked(String data)
    {
        if (data == null)
            doResetColumns();
        else
            createFromColumnConfig(data);

        listeners.stream().forEach(l -> l.onConfigurationPicked());

        policy.getViewer().refresh();
    }
}
