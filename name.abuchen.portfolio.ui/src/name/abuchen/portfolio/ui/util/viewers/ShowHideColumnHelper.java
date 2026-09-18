package name.abuchen.portfolio.ui.util.viewers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Stream;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.layout.TreeColumnLayout;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.ViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.swt.widgets.Widget;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.ConfigurationStore;
import name.abuchen.portfolio.ui.util.ConfigurationStore.ConfigurationStoreOwner;
import name.abuchen.portfolio.ui.util.ContextMenu;
import name.abuchen.portfolio.ui.util.LabelOnly;
import name.abuchen.portfolio.ui.util.SimpleAction;
import name.abuchen.portfolio.ui.util.action.MenuContribution;
import name.abuchen.portfolio.ui.util.viewers.Column.CacheInvalidationListener;
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
        private boolean redraw = true;

        abstract ColumnViewer getViewer();

        abstract int getColumnCount();

        abstract int getHeaderHeight();

        abstract Item[] getColumns();

        abstract Item getColumn(int index);

        abstract Item getSortColumn();

        abstract Item getColumnWidget(ViewerColumn column);

        abstract int[] getColumnOrder();

        abstract int getSortDirection();

        abstract int getWidth(Item col);

        abstract int getColumnIndex(Point pt);

        abstract Item create(Column column, Object option, Integer direction, int width);

        void setCommonParameters(Column column, ViewerColumn viewerColumn, Integer direction)
        {
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
            this.redraw = redraw;
            getViewer().getControl().setRedraw(redraw);
        }

        boolean isRedraw()
        {
            return redraw;
        }

        public void setChangeListener(org.eclipse.swt.widgets.Listener changeListener)
        {
            this.changeListener = changeListener;
        }

        String createToolTip(String label, String description, String menuLabel)
        {
            if (description != null)
                return label + "\n\n" + wordwrap(description); //$NON-NLS-1$

            if (menuLabel != null && !label.equals(menuLabel))
                return label + "\n\n" + menuLabel; //$NON-NLS-1$

            return label;
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
        int getHeaderHeight()
        {
            return table.getTable().getHeaderHeight();
        }

        @Override
        public Item[] getColumns()
        {
            return table.getTable().getColumns();
        }

        @Override
        public Item getColumn(int index)
        {
            return table.getTable().getColumn(index);
        }

        @Override
        public Item getSortColumn()
        {
            return table.getTable().getSortColumn();
        }

        @Override
        Item getColumnWidget(ViewerColumn column)
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
        public int getWidth(Item col)
        {
            return ((TableColumn) col).getWidth();
        }

        @Override
        int getColumnIndex(Point pt)
        {
            // https://stackoverflow.com/a/22197297/1158146

            int rowCount = table.getTable().getItemCount();

            TableItem item = rowCount == 0 ? new TableItem(table.getTable(), 0) : table.getTable().getItem(0);

            try
            {
                int columnCount = table.getTable().getColumnCount();
                int[] order = table.getTable().getColumnOrder();

                for (int index = 0; index < columnCount; index++)
                {
                    Rectangle rect = item.getBounds(order[index]);
                    if (pt.x >= rect.x && pt.x <= rect.x + rect.width)
                        return order[index];
                }
            }
            finally
            {
                if (rowCount == 0)
                    item.dispose();
            }

            return NO_COLUMN_SELECTED;
        }

        @Override
        public Item create(Column column, Object option, Integer direction, int width)
        {
            TableViewerColumn col = new TableViewerColumn(table, column.getStyle());

            TableColumn tableColumn = col.getColumn();
            tableColumn.setMoveable(true);
            tableColumn.setWidth(width);
            tableColumn.setData(Column.class.getName(), column);

            // register column data immediately after creation so that
            // TableColumnLayout never encounters a column without layout data
            // (even if a later step in this method triggers a layout cascade)
            layout.setColumnData(tableColumn, new ColumnPixelData(width));

            if (column.getImage() != null)
                tableColumn.setImage(column.getImage().image());

            if (option == null)
            {
                tableColumn.setText(column.getLabel());
                tableColumn.setToolTipText(
                                createToolTip(column.getLabel(), column.getDescription(), column.getMenuLabel()));
            }
            else
            {
                final String text = column.getOptions().getColumnLabel(option);
                tableColumn.setText(text);
                String description = column.getOptions().getDescription(option);
                tableColumn.setToolTipText(createToolTip(text, description, column.getMenuLabel()));

                tableColumn.setData(OPTIONS_KEY, option);
            }

            CellLabelProvider labelProvider = column.getLabelProvider().get();
            col.setLabelProvider(labelProvider);

            if (labelProvider instanceof CellItemImageClickedListener listener)
                setupImageClickedListener(col, listener);

            if (labelProvider instanceof ParameterizedColumnLabelProvider<?> parametrized)
                parametrized.setTableColumn(tableColumn);

            setCommonParameters(column, col, direction);

            return tableColumn;
        }

        private void setupImageClickedListener(TableViewerColumn viewerColumn, CellItemImageClickedListener cellImage)
        {
            org.eclipse.swt.widgets.Listener listener = event -> {

                int columnIndex = table.getTable().indexOf(viewerColumn.getColumn());
                if (columnIndex == -1)
                    return;

                Point pt = new Point(event.x, event.y);

                TableItem tableItem = table.getTable().getItem(pt);
                if (tableItem == null)
                    return;

                Rectangle rect = tableItem.getImageBounds(columnIndex);
                if (rect.contains(pt))
                    cellImage.onImageClicked(tableItem.getData());
            };

            table.getTable().addListener(SWT.MouseUp, listener);
            viewerColumn.getColumn().addDisposeListener(e -> table.getTable().removeListener(SWT.MouseUp, listener));
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
        int getHeaderHeight()
        {
            return tree.getTree().getHeaderHeight();
        }

        @Override
        public Item[] getColumns()
        {
            return tree.getTree().getColumns();
        }

        @Override
        public Item getColumn(int index)
        {
            return tree.getTree().getColumn(index);
        }

        @Override
        public Item getSortColumn()
        {
            return tree.getTree().getSortColumn();
        }

        @Override
        Item getColumnWidget(ViewerColumn column)
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
        public int getWidth(Item col)
        {
            return ((TreeColumn) col).getWidth();
        }

        @Override
        int getColumnIndex(Point pt)
        {
            int rowCount = tree.getTree().getItemCount();

            TreeItem item = rowCount == 0 ? new TreeItem(tree.getTree(), 0) : tree.getTree().getItem(0);

            try
            {
                int columnCount = tree.getTree().getColumnCount();
                int[] order = tree.getTree().getColumnOrder();

                for (int index = 0; index < columnCount; index++)
                {
                    Rectangle rect = item.getBounds(order[index]);
                    if (pt.x >= rect.x && pt.x <= rect.x + rect.width)
                        return order[index];
                }
            }
            finally
            {
                if (rowCount == 0)
                    item.dispose();
            }

            return NO_COLUMN_SELECTED;
        }

        @Override
        public Item create(Column column, Object option, Integer direction, int width)
        {
            TreeViewerColumn col = new TreeViewerColumn(tree, column.getStyle());

            TreeColumn treeColumn = col.getColumn();
            treeColumn.setMoveable(true);
            treeColumn.setWidth(width);
            treeColumn.setData(Column.class.getName(), column);

            // register column data immediately after creation so that
            // TreeColumnLayout never encounters a column without layout data
            // (even if a later step in this method triggers a layout cascade)
            layout.setColumnData(treeColumn, new ColumnPixelData(width));

            if (column.getImage() != null)
                treeColumn.setImage(column.getImage().image());

            if (option == null)
            {
                treeColumn.setText(column.getLabel());
                treeColumn.setToolTipText(
                                createToolTip(column.getLabel(), column.getDescription(), column.getMenuLabel()));
            }
            else
            {
                String text = column.getOptions().getColumnLabel(option);
                treeColumn.setText(text);
                String description = column.getOptions().getDescription(option);
                treeColumn.setToolTipText(createToolTip(text, description, column.getMenuLabel()));
                treeColumn.setData(OPTIONS_KEY, option);
            }

            CellLabelProvider labelProvider = column.getLabelProvider().get();
            col.setLabelProvider(labelProvider);

            setCommonParameters(column, col, direction);

            if (labelProvider instanceof CellItemImageClickedListener listener)
                setupImageClickedListener(col, listener);

            return treeColumn;
        }

        private void setupImageClickedListener(TreeViewerColumn viewerColumn, CellItemImageClickedListener cellImage)
        {
            org.eclipse.swt.widgets.Listener listener = event -> {

                int columnIndex = tree.getTree().indexOf(viewerColumn.getColumn());
                if (columnIndex == -1)
                    return;

                Point pt = new Point(event.x, event.y);
                TreeItem treeItem = tree.getTree().getItem(pt);
                if (treeItem == null)
                    return;

                Rectangle rect = treeItem.getImageBounds(columnIndex);
                if (rect.contains(pt))
                    cellImage.onImageClicked(treeItem.getData());
            };

            tree.getTree().addListener(SWT.MouseUp, listener);
            viewerColumn.getColumn().addDisposeListener(e -> tree.getTree().removeListener(SWT.MouseUp, listener));
        }

    }

    /* package */static final String OPTIONS_KEY = Column.class.getName() + "_OPTION"; //$NON-NLS-1$
    private static final String ORIGINAL_LABEL_KEY = "$original_label$"; //$NON-NLS-1$
    private static final int NO_COLUMN_SELECTED = -1;

    private final String identifier;

    private List<Column> columns = new ArrayList<>();
    private Map<String, Column> id2column = new HashMap<>();

    private IPreferenceStore preferences;
    private ConfigurationStore store;
    private List<Listener> listeners = new ArrayList<>();

    private ViewerPolicy policy;
    private Menu contextMenu;
    private int selectedColumnIndex = NO_COLUMN_SELECTED;

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
            this.policy.setChangeListener(e -> {
                if (this.policy.isRedraw())
                    store.updateActive(serialize());
            });
        }

        this.policy.getViewer().getControl().addDisposeListener(e -> ShowHideColumnHelper.this.widgetDisposed());

        installForegroundColorWorkaround(this.policy.getViewer().getControl());
    }

    /**
     * Workaround for a long-standing SWT/GTK3 bug on Linux where a
     * per-cell foreground color set via {@link CellLabelProvider#getForeground}
     * (and thus {@link TableItem#setForeground} / {@link TreeItem#setForeground})
     * is silently ignored by the native GTK3 renderer, so colored values
     * (e.g. gains/losses) always appear in the default text color.
     * <p>
     * The color is still stored correctly on the item -- only the native
     * paint ignores it -- so we suppress native text painting for the cell
     * and redraw the text ourselves using the already-set foreground color.
     * <p>
     * See https://bugs.eclipse.org/bugs/show_bug.cgi?id=218420 and
     * https://forum.portfolio-performance.info/t/farben-rot-und-grun-in-vermogensaufstellung-depot-werden-unter-linux-mint-nicht-angezeigt/25590
     */
    private static void installForegroundColorWorkaround(Control control)
    {
        if (!Platform.OS_LINUX.equals(Platform.getOS()))
            return;

        // suppress native (broken) foreground text painting only for cells
        // that actually carry a custom color; normal cells stay natively
        // rendered (correct ellipsis/truncation, less repaint work)
        control.addListener(SWT.EraseItem, event -> {
            if (getCustomForeground(event) != null)
                event.detail &= ~SWT.FOREGROUND;
        });

        control.addListener(SWT.PaintItem, ShowHideColumnHelper::paintForegroundWorkaround);
    }

    /**
     * Returns the cell's foreground color if -- and only if -- it differs
     * from the control's own default foreground, i.e. a custom color was
     * actually set via {@link CellLabelProvider#getForeground}. Returns
     * {@code null} for normal, non-colored cells, since
     * {@link TableItem#getForeground(int)} / {@link TreeItem#getForeground(int)}
     * never return {@code null} themselves -- they fall back to the
     * inherited item or control foreground.
     */
    private static Color getCustomForeground(Event event)
    {
        Color foreground;
        if (event.item instanceof TableItem tableItem)
            foreground = tableItem.getForeground(event.index);
        else if (event.item instanceof TreeItem treeItem)
            foreground = treeItem.getForeground(event.index);
        else
            return null;

        var controlForeground = ((Control) event.widget).getForeground();
        return foreground.equals(controlForeground) ? null : foreground;
    }
    
        
    private static void paintForegroundWorkaround(Event event)
    {
        var foreground = getCustomForeground(event);
        if (foreground == null)
            return; // no custom color -- native rendering already handled it correctly

        String text;
        Image image;
        Rectangle imageBounds;
        Rectangle textBounds;
        int alignment;

        if (event.item instanceof TableItem tableItem)
        {
            text = tableItem.getText(event.index);
            image = tableItem.getImage(event.index);
            imageBounds = tableItem.getImageBounds(event.index);
            textBounds = tableItem.getTextBounds(event.index);
            alignment = tableItem.getParent().getColumn(event.index).getStyle()
                            & (SWT.LEFT | SWT.CENTER | SWT.RIGHT);
        }
        else if (event.item instanceof TreeItem treeItem)
        {
            text = treeItem.getText(event.index);
            image = treeItem.getImage(event.index);
            imageBounds = treeItem.getImageBounds(event.index);
            textBounds = treeItem.getTextBounds(event.index);
            TreeColumn column = treeItem.getParent().getColumn(event.index);
            alignment = column.getStyle() & (SWT.LEFT | SWT.CENTER | SWT.RIGHT);
        }
        else
        {
            return;
        }

        var gc = event.gc;

        // SWT.FOREGROUND also gates the native pixbuf/icon renderer, not
        // just the text renderer -- since we suppressed it for this cell,
        // we have to redraw the (up/down arrow) image ourselves too, at
        // its native position
        if (image != null && !image.isDisposed())
            gc.drawImage(image, imageBounds.x, imageBounds.y);

        if (text == null || text.isEmpty())
            return;

        var oldForeground = gc.getForeground();

        // use the native selection text color for contrast/consistency
        // when selected, otherwise the custom (correctly stored, but
        // natively unrendered) foreground color
        var isSelected = (event.detail & SWT.SELECTED) != 0;
        gc.setForeground(isSelected ? event.display.getSystemColor(SWT.COLOR_LIST_SELECTION_TEXT) : foreground);

        var extent = gc.textExtent(text);

        // prefer SWT's own computed text origin (already accounts for the
        // image taking up space at the left), falling back to a manual
        // alignment calculation if unavailable -- getTextBounds() returns
        // an empty rectangle when the column has no pixbuf renderer set up
        int x;
        if (!textBounds.isEmpty())
        {
            x = textBounds.x;
        }
        else if ((alignment & SWT.RIGHT) != 0)
        {
            x = event.x + Math.max(0, event.width - extent.x);
        }
        else if ((alignment & SWT.CENTER) != 0)
        {
            x = event.x + Math.max(0, (event.width - extent.x) / 2);
        }
        else
        {
            x = event.x;
        }

        var y = event.y + Math.max(0, (event.height - extent.y) / 2);
        gc.drawText(text, x, y, true);

        gc.setForeground(oldForeground);
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

    public Stream<Column> getColumns()
    {
        return columns.stream();
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
    public void menuAboutToShow(final IMenuManager manager) // NOSONAR
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
                    MenuManager m = new MenuManager(TextUtil.tooltip(l));
                    manager.add(m);
                    return m;
                });
                if (column.hasHeading())
                    managerToAdd.add(new LabelOnly(TextUtil.tooltip(column.getHeading())));
            }

            if (column.hasOptions())
            {
                List<Object> options = visible.getOrDefault(column, Collections.emptyList());

                MenuManager subMenu = new MenuManager(TextUtil.tooltip(column.getMenuLabel()));

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
        manager.add(new MenuContribution(label, () -> {
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
        }, isChecked));
    }

    public void destroyColumnWithOption(Column column, Object option)
    {
        for (Item widget : policy.getColumns())
        {
            if (column.equals(widget.getData(Column.class.getName())) //
                            && (option == null || option.equals(widget.getData(OPTIONS_KEY))))
            {
                destroyColumn(widget);
                break;
            }
        }
    }

    private void destroyColumn(Item viewerColumn)
    {
        try
        {
            policy.setRedraw(false);

            Widget sortColumn = policy.getSortColumn();
            if (viewerColumn.equals(sortColumn))
                policy.getViewer().setComparator(null);

            viewerColumn.dispose();

            if (store != null)
                store.updateActive(serialize());
        }
        finally
        {
            policy.getViewer().refresh();
            policy.setRedraw(true);
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

            for (Column column : columns) // NOSONAR
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

            var sortColumn = policy.getSortColumn();

            for (Widget col : policy.getColumns())
            {
                Column column = (Column) col.getData(Column.class.getName());
                if (group.equals(column.getGroupLabel()))
                {
                    if (sortColumn == col)
                        policy.getViewer().setComparator(null);

                    col.dispose();
                }
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
        createColumns(false);
    }

    public void createColumns(boolean isEditable) // NOSONAR
    {
        createFromColumnConfig(store != null ? store.getActive() : preferences.getString(identifier));

        if (policy.getColumnCount() == 0)
        {
            columns.stream().filter(Column::isVisible)
                            .forEach(c -> policy.create(c, null, c.getDefaultSortDirection(), c.getDefaultWidth()));
        }

        if (isEditable)
        {
            var headerMenu = new ContextMenu(policy.getViewer().getControl(), manager -> {
                headerMenuAboutToShow(manager);
                manager.add(new Separator());
                menuAboutToShow(manager);
            });

            policy.getViewer().getControl().addListener(SWT.MenuDetect, event -> {
                Control control = policy.getViewer().getControl();
                Menu tableMenu = (Menu) control.getData(ContextMenu.DEFAULT_MENU);
                Point pt = event.display.map(null, control, new Point(event.x, event.y));
                Rectangle clientArea = ((Composite) control).getClientArea();
                boolean isHeader = clientArea.y <= pt.y && pt.y < (clientArea.y + policy.getHeaderHeight());

                if (isHeader)
                {
                    // remember the current column in selectedColumnIndex for
                    // later use in the context menu

                    selectedColumnIndex = policy.getColumnIndex(pt);
                    control.setMenu(headerMenu.getMenu());
                }
                else
                {
                    control.setMenu(tableMenu);
                }
            });
        }
    }

    private void createFromColumnConfig(String config)
    {
        ColumnConfiguration configuration = ColumnConfiguration.read(config);
        if (configuration == null)
            return;

        try
        {
            policy.setRedraw(false);

            // turn of sorting in case new columns define no viewer comparator
            policy.getViewer().setComparator(null);

            int oldCount = policy.getColumnCount();

            configuration.getItems().forEach(item -> {
                // index
                Column col = id2column.get(item.getId());
                if (col == null)
                    return;

                // option
                Object option = null;

                if (col.hasOptions())
                {
                    option = col.getOptions().valueOf(item.getOption());
                    if (option == null)
                        return;
                }

                Item viewerColumn = policy.create(col, option, item.getSortDirection(), item.getWidth());

                if (item.getLabel() != null)
                {
                    viewerColumn.setData(ORIGINAL_LABEL_KEY, viewerColumn.getText());
                    viewerColumn.setText(item.getLabel());
                }
            });

            for (int ii = 0; ii < oldCount; ii++)
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
        ColumnConfiguration configuration = new ColumnConfiguration();

        Widget sortedColumn = policy.getSortColumn();

        for (int index : policy.getColumnOrder())
        {
            Item col = policy.getColumn(index);
            Column column = (Column) col.getData(Column.class.getName());

            ColumnConfiguration.Item item = new ColumnConfiguration.Item(column.getId());

            if (column.hasOptions())
                item.setOption(column.getOptions().toString(col.getData(OPTIONS_KEY)));
            if (col.equals(sortedColumn))
                item.setSortDirection(policy.getSortDirection());

            item.setWidth(policy.getWidth(col));

            String originalLabel = (String) col.getData(ORIGINAL_LABEL_KEY);
            if (originalLabel != null)
                item.setLabel(col.getText());

            configuration.addItem(item);
        }

        return configuration.serialize();
    }

    @Override
    public void beforeConfigurationPicked()
    {
        store.updateActive(serialize());
    }

    @Override
    public void onConfigurationPicked(String data)
    {
        if (data == null || data.isEmpty())
            doResetColumns();
        else
            createFromColumnConfig(data);

        listeners.stream().forEach(Listener::onConfigurationPicked);

        policy.getViewer().refresh();
    }

    private void headerMenuAboutToShow(IMenuManager manager)
    {
        if (selectedColumnIndex == NO_COLUMN_SELECTED)
            return;

        Item widget = policy.getColumn(selectedColumnIndex);
        if (widget == null)
            return;

        Column column = (Column) widget.getData(Column.class.getName());
        if (column == null)
            return;

        String originalLabel = (String) widget.getData(ORIGINAL_LABEL_KEY);

        manager.add(new LabelOnly(originalLabel != null ? originalLabel : widget.getText()));

        manager.add(new SimpleAction(Messages.MenuRenameColumn, a -> {

            String oldLabel = widget.getText();

            InputDialog dlg = new InputDialog(Display.getCurrent().getActiveShell(), Messages.ColumnName,
                            Messages.ColumnName, oldLabel,
                            text -> text != null && text.length() > 0 ? null : Messages.LabelError);
            if (dlg.open() != InputDialog.OK) // NOSONAR
                return;

            String newLabel = dlg.getValue();
            widget.setText(newLabel);

            if (originalLabel == null)
                widget.setData(ORIGINAL_LABEL_KEY, oldLabel);
        }));

        if (originalLabel != null)
        {
            manager.add(new SimpleAction(Messages.MenuResetColumnName, a -> {
                widget.setData(ORIGINAL_LABEL_KEY, null);
                widget.setText(originalLabel);
            }));
        }

        if (column.isRemovable())
        {
            manager.add(new Separator());
            manager.add(new SimpleAction(Messages.MenuHideColumn, a -> destroyColumn(widget)));
        }
    }

    public void invalidateCache()
    {
        for (var c : columns)
        {
            if (c instanceof CacheInvalidationListener listener)
                listener.invalidateCache();
        }
    }
}
