package name.abuchen.portfolio.ui.util.viewers;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.TreeItem;

/**
 * Workaround for a long-standing SWT/GTK3 bug on Linux where a per-cell
 * foreground color set via {@link CellLabelProvider#getForeground} (and thus
 * {@link TableItem#setForeground} / {@link TreeItem#setForeground}) is
 * silently ignored by the native GTK3 renderer, so colored values (e.g.
 * gains/losses) always appear in the default text color.
 * <p>
 * The color is still stored correctly on the item -- only the native paint
 * ignores it -- so we suppress native foreground painting for cells with a
 * custom color and draw the text (and image) ourselves.
 * <p>
 * TODO: remove once SWT renders per-cell foreground colors on GTK3.
 * <p>
 * See https://forum.portfolio-performance.info/t/farben-rot-und-grun-in-vermogensaufstellung-depot-werden-unter-linux-mint-nicht-angezeigt/25590
 */
/* package */ final class GtkForegroundColorWorkaround
{
    private static final String ELLIPSIS = "…"; //$NON-NLS-1$

    /** horizontal padding used for the text area of a cell */
    private static final int PADDING = 4;

    /** the parts of a table or tree cell needed for painting */
    private record CellData(String text, Image image, Rectangle imageBounds, Rectangle textBounds, Font font,
                    int alignment)
    {
    }

    private GtkForegroundColorWorkaround()
    {
    }

    /* package */ static void install(Control control)
    {
        if (!Platform.OS_LINUX.equals(Platform.getOS()))
            return;

        // suppress native (broken) foreground painting only for cells that
        // actually carry a custom color; normal cells stay natively rendered
        // (correct ellipsis/truncation, less repaint work)
        control.addListener(SWT.EraseItem, event -> {
            if (getCustomForeground(event) != null)
                event.detail &= ~SWT.FOREGROUND;
        });

        control.addListener(SWT.PaintItem, GtkForegroundColorWorkaround::paint);
    }

    /**
     * Returns the cell's foreground color if -- and only if -- it differs from
     * the control's own default foreground, i.e. a custom color was actually
     * set. {@link TableItem#getForeground(int)} and
     * {@link TreeItem#getForeground(int)} never return {@code null}, they fall
     * back to the inherited item or control foreground.
     */
    private static Color getCustomForeground(Event event)
    {
        if (event.index < 0)
            return null;

        Color foreground;
        if (event.item instanceof TableItem tableItem)
            foreground = tableItem.getParent().getColumnCount() == 0 ? null : tableItem.getForeground(event.index);
        else if (event.item instanceof TreeItem treeItem)
            foreground = treeItem.getParent().getColumnCount() == 0 ? null : treeItem.getForeground(event.index);
        else
            return null;

        if (foreground == null)
            return null;

        var controlForeground = ((Control) event.widget).getForeground();
        return foreground.equals(controlForeground) ? null : foreground;
    }

    private static CellData extract(Event event)
    {
        if (event.item instanceof TableItem item)
        {
            var style = item.getParent().getColumn(event.index).getStyle();
            return new CellData(item.getText(event.index), item.getImage(event.index),
                            item.getImageBounds(event.index), item.getTextBounds(event.index),
                            item.getFont(event.index), style & (SWT.LEFT | SWT.CENTER | SWT.RIGHT));
        }

        if (event.item instanceof TreeItem item)
        {
            var style = item.getParent().getColumn(event.index).getStyle();
            return new CellData(item.getText(event.index), item.getImage(event.index),
                            item.getImageBounds(event.index), item.getTextBounds(event.index),
                            item.getFont(event.index), style & (SWT.LEFT | SWT.CENTER | SWT.RIGHT));
        }

        return null;
    }

    private static void paint(Event event)
    {
        var foreground = getCustomForeground(event);
        if (foreground == null)
            return; // no custom color -- native rendering is correct

        var cell = extract(event);
        if (cell == null)
            return;

        var gc = event.gc;

        // SWT.FOREGROUND also gates the native pixbuf renderer, so the image
        // has to be redrawn at its native position as well
        var image = cell.image();
        var hasImage = image != null && !image.isDisposed();
        if (hasImage)
            gc.drawImage(image, cell.imageBounds().x, cell.imageBounds().y);

        var text = cell.text();
        if (text == null || text.isEmpty())
            return;

        var oldForeground = gc.getForeground();
        var oldFont = gc.getFont();

        try
        {
            if (cell.font() != null)
                gc.setFont(cell.font());

            // use the native selection text color for contrast when selected
            var isSelected = (event.detail & SWT.SELECTED) != 0;
            gc.setForeground(isSelected ? event.display.getSystemColor(SWT.COLOR_LIST_SELECTION_TEXT) : foreground);

            // text area: right of the image (if any) up to the cell edge
            int left = hasImage ? cell.imageBounds().x + cell.imageBounds().width : event.x;
            int right = event.x + event.width;

            var isRight = (cell.alignment() & SWT.RIGHT) != 0;
            var isCenter = !isRight && (cell.alignment() & SWT.CENTER) != 0;

            // Determine the text area first, then shorten to fit exactly that
            // area. Do not trust getTextBounds() for right/center aligned
            // columns, as it is unclear whether GTK reports the text extent
            // or the whole renderer area.
            int start;
            int available;
            if (isRight)
            {
                start = left;
                available = right - start - PADDING;
            }
            else if (isCenter)
            {
                start = left + PADDING / 2;
                available = right - start - PADDING / 2;
            }
            else
            {
                start = !cell.textBounds().isEmpty() ? cell.textBounds().x : left;
                available = right - start - PADDING;
            }
            available = Math.max(0, available);

            text = shorten(gc, text, available);
            var extent = gc.textExtent(text);

            int x;
            if (isRight)
                x = start + Math.max(0, available - extent.x);
            else if (isCenter)
                x = start + Math.max(0, (available - extent.x) / 2);
            else
                x = start;

            var y = event.y + Math.max(0, (event.height - extent.y) / 2);
            gc.drawText(text, x, y, true);
        }
        finally
        {
            gc.setForeground(oldForeground);
            gc.setFont(oldFont);
        }
    }

    /**
     * Shortens the text (appending an ellipsis) so that it fits into the given
     * width. Returns the text unchanged if it already fits.
     */
    private static String shorten(GC gc, String text, int width)
    {
        if (gc.textExtent(text).x <= width)
            return text;

        if (gc.textExtent(ELLIPSIS).x > width)
            return ""; //$NON-NLS-1$

        int low = 0;
        int high = text.length();

        // largest prefix length that fits together with the ellipsis
        while (low < high)
        {
            int mid = (low + high + 1) >>> 1;
            if (gc.textExtent(text.substring(0, mid) + ELLIPSIS).x <= width)
                low = mid;
            else
                high = mid - 1;
        }

        // do not split a surrogate pair
        if (low > 0 && Character.isHighSurrogate(text.charAt(low - 1)))
            low--;

        return text.substring(0, low) + ELLIPSIS;
    }
}
