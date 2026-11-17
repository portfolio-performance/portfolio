package name.abuchen.portfolio.ui.util.viewers;

import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Widget;

/**
 * A {@link TableColumnLayout} that does not break when the table is currently
 * being rebuilt.
 *
 * <p>
 * JFace turns every SWT {@code Resize} event of a column into a new
 * {@code ColumnPixelData(column.getWidth())} and afterwards runs a full layout
 * of the table (see {@code TableColumnLayout#updateColumnData}). That layout
 * walks over all columns of the table and asserts that each of them has layout
 * data ("Unknown column layout data").
 * </p>
 *
 * <p>
 * Adding a column makes GTK re-allocate the header buttons of the columns that
 * are already there, so creating a new column can itself trigger such a layout
 * - at a moment when the new column has no layout data yet. Reproducible by
 * switching the payments view between the month, quarter and year matrix, where
 * the columns of the table are rebuilt while the old columns are still laid out
 * by this layout. Deferring the layout of the container does not help because
 * JFace triggers the layout itself from within the resize listener.
 * </p>
 *
 * <p>
 * The layout used here ignores the resize event as long as the table still
 * contains a column without layout data. The widths are picked up by the next
 * layout, once all columns have been created.
 * </p>
 *
 * @see SafeTreeColumnLayout
 * @see <a
 *      href="https://forum.portfolio-performance.info/t/nach-update-auf-0-40-1-fehler-unknown-column-layout-data/5470">Forum
 *      thread "Unknown column layout data"</a>
 */
public class SafeTableColumnLayout extends TableColumnLayout
{
    @Override
    protected void updateColumnData(Widget column)
    {
        if (column instanceof TableColumn tableColumn)
        {
            // a column that is currently created has no layout data: the
            // layout of JFace would assert over it
            if (hasColumnWithoutLayoutData(tableColumn.getParent().getColumns()))
                return;

            // GTK reports -1 for a column without fixed width (see
            // SafeTreeColumnLayout) - JFace would assert over it as well
            if (tableColumn.getWidth() < 0)
                return;
        }

        super.updateColumnData(column);
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
}
