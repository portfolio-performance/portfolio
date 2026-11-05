package name.abuchen.portfolio.ui.util.viewers;

import org.eclipse.jface.layout.TreeColumnLayout;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.Widget;

/**
 * A {@link TreeColumnLayout} that does not break when SWT reports an unusable
 * column width.
 *
 * <p>
 * JFace turns every SWT {@code Resize} event of a column into a new
 * {@code ColumnPixelData(column.getWidth())} (see
 * {@code TreeColumnLayout#updateColumnData}). On GTK SWT implements
 * {@code TreeColumn.getWidth()} as {@code gtk_tree_view_column_get_fixed_width()},
 * which answers -1 whenever the column has no fixed width. SWT avoids that for
 * {@code TableColumn} by remembering whether a fixed width was set at all
 * ({@code useFixedWidth}) and falls back to the real column allocation in that
 * case - but it does not do so for {@code TreeColumn}.
 * </p>
 *
 * <p>
 * Double clicking the separator between two column headers lets GTK auto-size the
 * column without giving it a fixed width, so JFace reads -1 and
 * {@code Assert.isTrue(widthInPixels >= 0)} fails. The exception escapes into
 * SWT's size-allocate handler and leaves the view half-initialized: only the
 * heading of the view is left on screen, and the invalid width is stored in the
 * saved column configuration, which lets the view fail again as soon as that
 * configuration is restored.
 * </p>
 *
 * <p>
 * The layout used here ignores such an event and keeps the width that is already
 * stored for the column. If GTK does give the column a fixed width while it
 * settles the auto-sizing, the width is picked up afterwards.
 * </p>
 *
 * <p>
 * The same layout also breaks when a column is simply added: GTK re-allocates
 * the headers of the columns that are already there, JFace answers with a full
 * layout of the tree, and that layout asserts ("Unknown column layout data")
 * over the new column which has no layout data yet. Such an event is ignored as
 * well, see {@link SafeTableColumnLayout}.
 * </p>
 *
 * @see SafeTableColumnLayout
 * @see <a
 *      href="https://forum.portfolio-performance.info/t/nach-update-auf-0-40-1-fehler-unknown-column-layout-data/5470">Forum
 *      thread "Unknown column layout data"</a>
 */
public class SafeTreeColumnLayout extends TreeColumnLayout
{
    @Override
    protected void updateColumnData(Widget column)
    {
        if (column instanceof TreeColumn treeColumn)
        {
            // GTK did not fix the width of the column: JFace would build a
            // ColumnPixelData with a negative width and assert. Keep the
            // current layout data and ask again after GTK is done.
            if (treeColumn.getWidth() < 0)
            {
                treeColumn.getDisplay().asyncExec(() -> {
                    if (!treeColumn.isDisposed() && treeColumn.getWidth() >= 0)
                        applyColumnData(treeColumn);
                });

                return;
            }

            // a column that is currently created has no layout data: the
            // layout of JFace would assert over it
            if (hasColumnWithoutLayoutData(treeColumn.getParent().getColumns()))
                return;
        }

        applyColumnData(column);
    }

    private boolean hasColumnWithoutLayoutData(Item[] columns)
    {
        for (Item column : columns)
        {
            if (column.getData(LAYOUT_DATA) == null)
                return true;
        }
        return false;
    }

    /**
     * The width that the layout currently keeps for the column, or -1 if the
     * column has no usable layout data. Ask for it when SWT reports an unusable
     * width but the width still has to be known, for instance to store the
     * columns of a view.
     */
    static int storedWidth(TreeColumn column)
    {
        if (column.getData(LAYOUT_DATA) instanceof ColumnPixelData data)
            return data.width;

        return -1;
    }

    private void applyColumnData(Widget column)
    {
        super.updateColumnData(column);
    }
}
