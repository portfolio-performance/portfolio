package name.abuchen.portfolio.ui.util.viewers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.IntSupplier;
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
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.swt.widgets.Widget;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.preferences.Experiments;
import name.abuchen.portfolio.ui.theme.ThemePreferences;
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

    /**
     * Returns the logical label of a column, i.e. the label without any line break
     * inserted for the two-line header display. Use this instead of
     * {@link Item#getText()} wherever the label is consumed as data - for example
     * when exporting to CSV - so that a wrapped header never leaks a line break
     * into the output.
     */
    public static String getColumnLabel(Item column)
    {
        var label = (String) column.getData(WRAP_LABEL_KEY);
        return label != null ? label : column.getText();
    }

    /**
     * Re-applies the two-line header wrap after the label of a column has been
     * changed. Does nothing unless the wrap is active for this column, so callers
     * do not need to know whether the feature is enabled.
     */
    private static void reflowHeader(Item column)
    {
        if (column.getData(REFLOW_KEY) instanceof Runnable reflow)
            reflow.run();
    }

    /**
     * Returns the line by which SWT should measure the header when it fits the
     * column after a double click on the divider: the longer line of the most
     * balanced two-line split of the label. A long label then keeps two lines
     * instead of widening the column to a single line; the cell contents still
     * count, so a wider value wins.
     *
     * @return the line, or {@code null} if the column has no label that can
     *         wrap
     */
    static String headerLineToFit(Item column, Control measureControl)
    {
        if (!(column.getData(REFLOW_KEY) instanceof Runnable))
            return null;

        var label = (String) column.getData(WRAP_LABEL_KEY);
        if (label == null || !label.contains(" ")) //$NON-NLS-1$
            return null;

        var gc = new GC(measureControl);
        try
        {
            useHeaderFont(gc, measureControl);
            return HeaderLabelWrap.longerLineOfBalancedSplit(label, text -> gc.textExtent(text).x);
        }
        finally
        {
            gc.dispose();
        }
    }

    /**
     * Returns the horizontal space of a header item that is not available for
     * the label: on Windows the text margin of the header, on Linux the padding
     * of the header button and the sort indicator. On macOS the measurement
     * already includes that space (see the reflow).
     */
    private static int headerLabelMargin(Control control, Item column)
    {
        var os = Platform.getOS();
        if (Platform.OS_WIN32.equals(os))
            return WindowsHeaderWrap.labelMargin(control);
        if (Platform.OS_LINUX.equals(os))
            return LinuxHeaderWrap.labelMargin(control, column);
        return 0;
    }

    /** Returns the horizontal padding of a cell, for the fit of a column. */
    private static int cellMargin(Control control)
    {
        if (Platform.OS_LINUX.equals(Platform.getOS()))
            return LinuxHeaderWrap.cellMargin();
        return WindowsHeaderWrap.labelMargin(control);
    }

    /**
     * Selects the font in which the header label is drawn if it differs from
     * the font of the control: GTK draws it in bold.
     */
    private static void useHeaderFont(GC gc, Control control)
    {
        if (Platform.OS_LINUX.equals(Platform.getOS()))
            gc.setFont(LinuxHeaderWrap.headerFont(control));
    }

    /**
     * Completes the fit after SWT has fitted a column on a double click on its
     * divider: widens the column until every cell and the header fit - for a
     * label that can wrap the given line, otherwise the whole label, next to the
     * header's text margin; then wraps the label for the new width.
     * <p>
     * The native fit of the list view (Windows) or of the tree view (Linux)
     * does not reliably cover the cells of the tables here: it measures the cells in the font of the table only (bold
     * sums are cut off) and cuts off some cells with images and long texts.
     * Each cell is therefore measured with its own font, from the start of its
     * text (after image and indent) to the end of the text plus the margin. The
     * native fit ignores the header, too.
     * Virtual tables are left as SWT fitted them, measuring would create all
     * items.
     *
     * @param line
     *            the line returned by {@link #headerLineToFit(Item, Control)},
     *            or {@code null} if the label does not wrap
     */
    static void completeFit(Item column, Control control, String line)
    {
        if (column.isDisposed() || control.isDisposed())
            return;

        var needed = 0;

        var gc = new GC(control);
        try
        {
            var headerText = line != null ? line : getColumnLabel(column);
            if (!headerText.isEmpty())
            {
                useHeaderFont(gc, control);
                needed = gc.textExtent(headerText).x + headerLabelMargin(control, column);
            }
            if ((control.getStyle() & SWT.VIRTUAL) == 0)
                needed = Math.max(needed, widestCell(column, control, gc) + cellMargin(control));
        }
        finally
        {
            gc.dispose();
        }

        if (getWidth(column) < needed)
        {
            if (column instanceof TableColumn tableColumn)
                tableColumn.setWidth(needed);
            else if (column instanceof TreeColumn treeColumn)
                treeColumn.setWidth(needed);
        }

        if (column.getData(REFLOW_KEY) instanceof Runnable reflow)
            reflow.run();
    }

    private static int getWidth(Item column)
    {
        if (column instanceof TableColumn tableColumn)
            return tableColumn.getWidth();
        if (column instanceof TreeColumn treeColumn)
            return treeColumn.getWidth();
        return 0;
    }

    /**
     * Returns the width the widest cell of the column needs without the right
     * margin: from the left edge of the column to the end of the text in the
     * cell's own font. Trees count the expanded items only.
     */
    private static int widestCell(Item column, Control control, GC gc)
    {
        var widest = 0;

        if (control instanceof Table table && column instanceof TableColumn tableColumn)
        {
            var index = table.indexOf(tableColumn);
            var left = columnLeft(index, table.getColumnOrder(), table::getColumn, table.getHorizontalBar());
            for (TableItem item : table.getItems())
            {
                var text = item.getText(index);
                if (text.isEmpty())
                    continue;
                gc.setFont(item.getFont(index));
                widest = Math.max(widest, item.getTextBounds(index).x - left + gc.textExtent(text).x);
            }
        }
        else if (control instanceof Tree tree && column instanceof TreeColumn treeColumn)
        {
            var index = tree.indexOf(treeColumn);
            var left = columnLeft(index, tree.getColumnOrder(), tree::getColumn, tree.getHorizontalBar());
            var pending = new ArrayList<TreeItem>(Arrays.asList(tree.getItems()));
            while (!pending.isEmpty())
            {
                var item = pending.remove(pending.size() - 1);
                if (item.getExpanded())
                    pending.addAll(Arrays.asList(item.getItems()));

                var text = item.getText(index);
                if (text.isEmpty())
                    continue;
                gc.setFont(item.getFont(index));
                widest = Math.max(widest, item.getTextBounds(index).x - left + gc.textExtent(text).x);
            }
        }

        return widest;
    }

    /** the x position of the left edge of a column in the client area */
    private static int columnLeft(int index, int[] order, IntFunction<Item> columnAt,
                    ScrollBar horizontalBar)
    {
        var left = horizontalBar != null ? -horizontalBar.getSelection() : 0;
        for (int ii : order)
        {
            if (ii == index)
                break;
            left += getWidth(columnAt.apply(ii));
        }
        return left;
    }

    /**
     * Answers whether the header is drawn small enough for a second line to fit.
     * A size that cannot be determined counts as too large: wrapping a header that
     * then does not fit is worse than not wrapping one that would.
     */
    private static boolean isFontSizeSmallEnoughToWrap()
    {
        return ThemePreferences.getSessionFontSize().orElse(Integer.MAX_VALUE) <= MAX_FONT_SIZE_FOR_WRAP;
    }

    /**
     * Wraps a column header label onto a second line only when the full label does
     * not fit the column's current width; snaps back to a single line once it does.
     * Re-evaluated on every column resize, so it stays correct as the user drags.
     * The label to wrap is tracked via {@link #WRAP_LABEL_KEY} rather than closed
     * over at attach time, so a later rename or restored custom label is wrapped
     * instead of being silently overwritten by the original label on next resize.
     */
    private static void attachDynamicHeaderWrap(Item column, IntSupplier widthSupplier, Consumer<String> textSetter,
                    Control measureControl, String fullLabel)
    {
        if (!new Experiments().isEnabled(Experiments.Feature.NOV26_WRAP_COLUMN_HEADERS))
            return;

        // an embedded "\n" produces a two-line header natively on macOS and on
        // Linux (GTK lays out the header label). On Windows the native header
        // is one line high and draws one line only; WindowsHeaderWrap makes
        // room for the second line and draws wrapped labels itself
        var os = Platform.getOS();
        var isMac = Platform.OS_MACOSX.equals(os);
        var isWindows = Platform.OS_WIN32.equals(os);
        var isLinux = Platform.OS_LINUX.equals(os);
        if (!isMac && !isWindows && !isLinux)
            return;

        // on macOS the header row has a fixed height that does not grow with
        // the font size. Two lines only fit while the header is drawn in the
        // small system font; as soon as a larger font size is configured, the
        // style sheet sets that font on the table, the header cell inherits it,
        // and the second line would be cut off. Keep the single line in that
        // case. On Windows the header grows with the font.
        if (isMac && !isFontSizeSmallEnoughToWrap())
            return;

        column.setData(WRAP_LABEL_KEY, fullLabel);

        Consumer<String> setIfChanged = text -> {
            if (!text.equals(column.getText()))
            {
                textSetter.accept(text);
                WindowsHeaderWrap.update(measureControl);
            }
        };

        Runnable reflow = () -> {
            var label = (String) column.getData(WRAP_LABEL_KEY);
            if (label == null)
                label = fullLabel;

            if (!label.contains(" ")) //$NON-NLS-1$
            {
                setIfChanged.accept(label);
                return;
            }

            var gc = new GC(measureControl);
            try
            {
                // this GC inherits the table's 13pt body font, but on macOS
                // the header itself always renders in Cocoa's 11pt small
                // system font (both fixed AppKit constants) -- SWT exposes no
                // API for the header's real font or its usable width, so this
                // ~15% overestimate is used deliberately in place of an
                // explicit margin: it approximates the space actually
                // reserved by native chrome SWT doesn't expose (sort-arrow
                // icon, cell padding). Do not "fix" this by measuring with
                // the true 11pt font unless an explicit margin is added back
                // for that reserved space -- doing one without the other
                // causes real clipping on sorted columns
                var available = widthSupplier.getAsInt();

                // measure with the font of the header label and leave the
                // space of the header item that is not available for it
                available -= headerLabelMargin(measureControl, column);
                useHeaderFont(gc, measureControl);

                if (available <= 0)
                    return;

                // GTK widens the column until the longest line of the label
                // fits instead of cutting it off
                setIfChanged.accept(HeaderLabelWrap.wrap(label, column.getText(), available, UNWRAP_HYSTERESIS_PX,
                                isLinux, text -> gc.textExtent(text).x));
            }
            finally
            {
                gc.dispose();
            }
        };

        column.setData(REFLOW_KEY, reflow);

        if (isWindows)
            WindowsHeaderWrap.attach(measureControl);
        else if (isLinux)
            LinuxHeaderWrap.attach(measureControl);

        if (column instanceof TableColumn tc)
            tc.addControlListener(ControlListener.controlResizedAdapter(e -> reflow.run()));
        else if (column instanceof TreeColumn tc)
            tc.addControlListener(ControlListener.controlResizedAdapter(e -> reflow.run()));

        reflow.run();

        // the theme engine applies fonts only after the columns have been
        // created and a font change fires no resize event, so the measurement
        // above can still be based on the pre-theme font; recompute once the
        // UI has settled so that a view already wraps when it is first opened
        measureControl.getDisplay().asyncExec(() -> {
            if (!column.isDisposed() && !measureControl.isDisposed())
                reflow.run();
        });
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

            // a column configuration can carry an invalid width: a negative
            // value makes JFace assert (see SafeTableColumnLayout) and a width
            // of 0 would hide the column, so fall back to the default width
            int columnWidth = width > 0 ? width : column.getDefaultWidth();

            TableColumn tableColumn = col.getColumn();
            tableColumn.setMoveable(true);
            tableColumn.setWidth(columnWidth);
            tableColumn.setData(Column.class.getName(), column);

            // register column data immediately after creation so that
            // TableColumnLayout never encounters a column without layout data
            // (even if a later step in this method triggers a layout cascade)
            layout.setColumnData(tableColumn, new ColumnPixelData(columnWidth));

            if (column.getImage() != null)
                tableColumn.setImage(column.getImage().image());

            if (option == null)
            {
                tableColumn.setText(column.getLabel());
                tableColumn.setToolTipText(
                                createToolTip(column.getLabel(), column.getDescription(), column.getMenuLabel()));
                attachDynamicHeaderWrap(tableColumn, tableColumn::getWidth, tableColumn::setText, table.getTable(),
                                column.getLabel());
            }
            else
            {
                final String text = column.getOptions().getColumnLabel(option);
                tableColumn.setText(text);
                String description = column.getOptions().getDescription(option);
                tableColumn.setToolTipText(createToolTip(text, description, column.getMenuLabel()));

                tableColumn.setData(OPTIONS_KEY, option);
                attachDynamicHeaderWrap(tableColumn, tableColumn::getWidth, tableColumn::setText, table.getTable(),
                                text);
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
            int width = ((TreeColumn) col).getWidth();
            if (width >= 0)
                return width;

            // on GTK a tree column that has no fixed width reports -1 (see
            // SafeTreeColumnLayout). Never write such a value into the column
            // configuration, but the width the layout keeps for the column
            int storedWidth = SafeTreeColumnLayout.storedWidth((TreeColumn) col);
            if (storedWidth >= 0)
                return storedWidth;

            Object column = col.getData(Column.class.getName());
            return column instanceof Column c ? c.getDefaultWidth() : 0;
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

            // a column configuration can carry an invalid width: a negative
            // value makes JFace assert (see SafeTreeColumnLayout) and a width
            // of 0 would hide the column, so fall back to the default width
            int columnWidth = width > 0 ? width : column.getDefaultWidth();

            TreeColumn treeColumn = col.getColumn();
            treeColumn.setMoveable(true);
            treeColumn.setWidth(columnWidth);
            treeColumn.setData(Column.class.getName(), column);

            // register column data immediately after creation so that
            // TreeColumnLayout never encounters a column without layout data
            // (even if a later step in this method triggers a layout cascade)
            layout.setColumnData(treeColumn, new ColumnPixelData(columnWidth));

            if (column.getImage() != null)
                treeColumn.setImage(column.getImage().image());

            if (option == null)
            {
                treeColumn.setText(column.getLabel());
                treeColumn.setToolTipText(
                                createToolTip(column.getLabel(), column.getDescription(), column.getMenuLabel()));
                attachDynamicHeaderWrap(treeColumn, treeColumn::getWidth, treeColumn::setText, tree.getTree(),
                                column.getLabel());
            }
            else
            {
                String text = column.getOptions().getColumnLabel(option);
                treeColumn.setText(text);
                String description = column.getOptions().getDescription(option);
                treeColumn.setToolTipText(createToolTip(text, description, column.getMenuLabel()));
                treeColumn.setData(OPTIONS_KEY, option);
                attachDynamicHeaderWrap(treeColumn, treeColumn::getWidth, treeColumn::setText, tree.getTree(), text);
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
    private static final String WRAP_LABEL_KEY = "$wrap_label$"; //$NON-NLS-1$
    private static final String REFLOW_KEY = "$wrap_reflow$"; //$NON-NLS-1$
    private static final int UNWRAP_HYSTERESIS_PX = 8;
    private static final int MAX_FONT_SIZE_FOR_WRAP = 11;
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
                    var cleanDefault = viewerColumn.getData(WRAP_LABEL_KEY);
                    viewerColumn.setData(ORIGINAL_LABEL_KEY,
                                    cleanDefault != null ? cleanDefault : viewerColumn.getText());
                    viewerColumn.setText(item.getLabel());
                    viewerColumn.setData(WRAP_LABEL_KEY, item.getLabel());
                    reflowHeader(viewerColumn);
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
            {
                var wrapLabel = (String) col.getData(WRAP_LABEL_KEY);
                item.setLabel(wrapLabel != null ? wrapLabel : col.getText());
            }

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
        var wrapLabel = (String) widget.getData(WRAP_LABEL_KEY);
        var currentLabel = wrapLabel != null ? wrapLabel : widget.getText();

        manager.add(new LabelOnly(originalLabel != null ? originalLabel : currentLabel));

        manager.add(new SimpleAction(Messages.MenuRenameColumn, a -> {

            var oldLabel = currentLabel;

            InputDialog dlg = new InputDialog(Display.getCurrent().getActiveShell(), Messages.ColumnName,
                            Messages.ColumnName, oldLabel,
                            text -> text != null && text.length() > 0 ? null : Messages.LabelError);
            if (dlg.open() != InputDialog.OK) // NOSONAR
                return;

            String newLabel = dlg.getValue();
            widget.setText(newLabel);
            widget.setData(WRAP_LABEL_KEY, newLabel);
            reflowHeader(widget);

            if (originalLabel == null)
                widget.setData(ORIGINAL_LABEL_KEY, oldLabel);
        }));

        if (originalLabel != null)
        {
            manager.add(new SimpleAction(Messages.MenuResetColumnName, a -> {
                widget.setData(ORIGINAL_LABEL_KEY, null);
                widget.setText(originalLabel);
                widget.setData(WRAP_LABEL_KEY, originalLabel);
                reflowHeader(widget);
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
