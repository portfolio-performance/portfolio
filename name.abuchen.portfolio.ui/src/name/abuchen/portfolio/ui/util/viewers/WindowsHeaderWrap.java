package name.abuchen.portfolio.ui.util.viewers;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.Platform;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;

import name.abuchen.portfolio.ui.PortfolioPlugin;

/**
 * Shows wrapped column labels on two lines in the header of a table or tree on
 * Windows.
 * <p>
 * The header is one line high and draws its labels on a single line. Two
 * window procedures are chained in front of SWT's own:
 * <ul>
 * <li>on the header control: the list view, and SWT's tree, ask the header
 * for its height with {@code HDM_LAYOUT} and offset the rows by the returned
 * {@code WINDOWPOS.cy}. While a label is wrapped, the answer is enlarged by one
 * line of the header font.</li>
 * <li>on the parent of the header, which receives the header's
 * {@code NM_CUSTOMDRAW} notifications. If SWT draws the header itself (header
 * colors are set, for example by the CSS theme), SWT draws all labels in
 * {@code CDDS_POSTPAINT} from the Java field {@code Item.text}: the field of
 * wrapped columns is emptied while SWT draws, and the labels are drawn on two
 * lines afterwards. Otherwise the native header draws, and items with a
 * wrapped label are drawn like the native header does (themed background and
 * sort arrow), but with the label on two lines.</li>
 * </ul>
 * While the mouse moves over the header, the native header paints its hover
 * and pressed transitions itself, without {@code NM_CUSTOMDRAW}, so that a
 * wrapped label would fall back to one line. The header is therefore painted
 * again after each change of the hot or pressed item, once right away and once
 * after the transition.
 * <p>
 * SWT's own header drawing (used if header colors are set, for example by
 * the dark theme) does not highlight the item under the mouse. Like the
 * native header in the light theme, the hot and the pressed item are
 * highlighted by filling them with the header background mixed with the
 * header foreground before SWT draws the labels.
 * <p>
 * A double click on a header divider is left to SWT, but while SWT (or the
 * native list view) fits the column, the column label is only the longer line
 * of its most balanced two-line split (see
 * {@link ShowHideColumnHelper#headerLineToFit(Item, Control)}), so that a long
 * label keeps two lines. Afterwards the column is widened until all cells fit.
 * <p>
 * The procedures are reached through SWT's internal {@code Callback}. All SWT
 * internals are called reflectively, as in {@code GtkUnfocusedTextColor}, so
 * that the class loads on every platform.
 */
@SuppressWarnings("nls")
final class WindowsHeaderWrap
{
    // vsstyle.h, theme class HEADER
    private static final int HP_HEADERITEM = 1;
    private static final int HP_HEADERSORTARROW = 4;
    private static final int HIS_NORMAL = 1;
    private static final int HIS_HOT = 2;
    private static final int HIS_PRESSED = 3;
    private static final int HIS_SORTED_OFFSET = 3; // HIS_SORTEDNORMAL = 4, ...
    private static final int HSAS_SORTEDUP = 1;
    private static final int HSAS_SORTEDDOWN = 2;

    // commctrl.h: the parent changed the font or text color of the item
    private static final long CDRF_NEWFONT = 0x2;

    /** header window handle -> state */
    private static final Map<Long, Header> HEADERS = new HashMap<>();

    /** window handle of the header's parent -> state */
    private static final Map<Long, Header> PARENTS = new HashMap<>();

    private static Natives natives;
    private static boolean failed;

    /**
     * delays after a hover or pressed change after which the header is painted
     * again: the native transition is not drawn with custom draw
     */
    private static final int REPAINT_SOON_MS = 200;
    private static final int REPAINT_LATE_MS = 600;

    /** share of the header foreground in the highlight of SWT's header, in % */
    private static final int HOT_HIGHLIGHT_PERCENT = 15;
    private static final int PRESSED_HIGHLIGHT_PERCENT = 25;

    /** used if the margin cannot be determined, in SWT (logical) pixels */
    private static final int FALLBACK_LABEL_MARGIN = 12;

    private WindowsHeaderWrap()
    {
    }

    private static final class Header
    {
        private final Control control;
        private final long hwndHeader;
        private final long hwndParent;
        private final long oldHeaderProc;
        private final long oldParentProc;
        private boolean wrapped;

        /**
         * whether SWT asked for item notifications in the current paint of the
         * native header; if not, they are requested only for the wrapped labels
         * and must not reach SWT (which would then treat them as its own
         * header drawing)
         */
        private boolean swtWantsItemDraw;

        /** the item under the mouse, -1 if none */
        private int hotIndex = -1;

        /** the item pressed with the mouse (not its divider), -1 if none */
        private int pressedIndex = -1;

        // two instances: Display#timerExec reschedules an already scheduled runnable
        private final Runnable repaintSoon = this::repaint;
        private final Runnable repaintLate = this::repaint;

        private Header(Control control, long hwndHeader, long oldHeaderProc, long hwndParent, long oldParentProc)
        {
            this.control = control;
            this.hwndHeader = hwndHeader;
            this.oldHeaderProc = oldHeaderProc;
            this.hwndParent = hwndParent;
            this.oldParentProc = oldParentProc;
        }

        private void repaint()
        {
            if (failed || control.isDisposed())
                return;
            try
            {
                natives.invalidate(hwndHeader);
            }
            catch (ReflectiveOperationException | RuntimeException e)
            {
                fail(e);
            }
        }
    }

    /**
     * Re-evaluates whether a column label of the table or tree is wrapped and
     * adjusts the header accordingly. Does nothing on other platforms.
     */
    static void update(Control control)
    {
        if (failed || control.isDisposed() || !Platform.OS_WIN32.equals(Platform.getOS()))
            return;

        try
        {
            var wrapped = isAnyLabelWrapped(control);

            var hwndHeader = natives().headerHandle(control);
            if (hwndHeader == 0)
                return;

            var header = HEADERS.get(hwndHeader);
            if (header == null)
            {
                if (!wrapped)
                    return;
                header = install(control, hwndHeader);
            }

            if (header.wrapped == wrapped)
                return;

            header.wrapped = wrapped;
            relayout(control);
        }
        catch (ReflectiveOperationException | RuntimeException e)
        {
            fail(e);
        }
    }

    /**
     * Chains the window procedures as soon as the wrap is attached to a column,
     * so that a double click on a divider is handled before any label wraps.
     */
    static void attach(Control control)
    {
        if (failed || control.isDisposed() || !Platform.OS_WIN32.equals(Platform.getOS()))
            return;

        try
        {
            var hwndHeader = natives().headerHandle(control);
            if (hwndHeader != 0 && !HEADERS.containsKey(hwndHeader))
                install(control, hwndHeader);
        }
        catch (ReflectiveOperationException | RuntimeException e)
        {
            fail(e);
        }
    }

    /**
     * Returns the horizontal space of a header item that is not available for
     * the label, in SWT (logical) pixels: SWT's own text insets if SWT draws the
     * header, otherwise the text margin of the native header.
     */
    static int labelMargin(Control control)
    {
        if (failed || control.isDisposed() || !Platform.OS_WIN32.equals(Platform.getOS()))
            return FALLBACK_LABEL_MARGIN;

        try
        {
            var n = natives();
            var margins = n.labelMargins(control, n.isCustomHeaderDrawing(control));
            var zoom = control.getMonitor().getZoom();
            if (zoom <= 0)
                zoom = 100;
            return (int) Math.ceil((margins[0] + margins[1]) * 100d / zoom);
        }
        catch (ReflectiveOperationException | RuntimeException e)
        {
            fail(e);
            return FALLBACK_LABEL_MARGIN;
        }
    }

    private static void fail(Exception e)
    {
        failed = true;
        PortfolioPlugin.log(e);
    }

    private static Item[] columns(Control control)
    {
        if (control instanceof Table table)
            return table.getColumns();
        if (control instanceof Tree tree)
            return tree.getColumns();
        return new Item[0];
    }

    private static boolean isWrapped(Item column)
    {
        return column.getText().indexOf('\n') >= 0 && column.getImage() == null;
    }

    private static boolean isAnyLabelWrapped(Control control)
    {
        for (Item column : columns(control))
        {
            if (isWrapped(column))
                return true;
        }
        return false;
    }

    private static Header install(Control control, long hwndHeader) throws ReflectiveOperationException
    {
        var n = natives();
        var hwndParent = n.getParent(hwndHeader);
        var header = new Header(control, hwndHeader, n.getWindowProc(hwndHeader), hwndParent,
                        n.getWindowProc(hwndParent));

        // register before chaining: the procedures may be called right away
        HEADERS.put(hwndHeader, header);
        PARENTS.put(hwndParent, header);
        n.setWindowProc(hwndHeader, n.callbackAddress);
        n.setWindowProc(hwndParent, n.callbackAddress);

        control.addDisposeListener(e -> uninstall(header));
        return header;
    }

    private static void uninstall(Header header)
    {
        try
        {
            // if another procedure was chained in front of ours, keep the entry
            // so that messages passed on to us still reach SWT
            if (natives.getWindowProc(header.hwndHeader) == natives.callbackAddress)
            {
                natives.setWindowProc(header.hwndHeader, header.oldHeaderProc);
                HEADERS.remove(header.hwndHeader);
            }
            if (natives.getWindowProc(header.hwndParent) == natives.callbackAddress)
            {
                natives.setWindowProc(header.hwndParent, header.oldParentProc);
                PARENTS.remove(header.hwndParent);
            }
        }
        catch (ReflectiveOperationException | RuntimeException e)
        {
            PortfolioPlugin.log(e);
        }
    }

    /**
     * The list view and the tree ask the header for its height only when they
     * lay out, for example after a resize.
     */
    private static void relayout(Control control)
    {
        var size = control.getSize();
        if (size.x <= 0 || size.y <= 1)
            return; // laid out anyway once the control gets its size

        control.setRedraw(false);
        try
        {
            control.setSize(size.x, size.y - 1);
            control.setSize(size);
        }
        finally
        {
            control.setRedraw(true);
        }
    }

    private static Natives natives() throws ReflectiveOperationException
    {
        if (natives == null)
            natives = new Natives();
        return natives;
    }

    // --- window procedures ---------------------------------------------------

    /**
     * Target of SWT's {@code Callback}: the window procedure of all chained
     * header controls and their parents. Never throws into native code.
     */
    private static final class WindowProc
    {
        // called from native code by SWT's Callback, which looks it up by name
        @SuppressWarnings("unused")
        long windowProc(long hwnd, long msg, long wParam, long lParam)
        {
            var header = HEADERS.get(hwnd);
            var isHeader = header != null;
            if (!isHeader)
                header = PARENTS.get(hwnd);

            try
            {
                if (header == null)
                    return natives.defWindowProc(hwnd, (int) msg, wParam, lParam);

                var active = header.wrapped && !failed && lParam != 0;

                if (!isHeader && !failed && lParam != 0 && (int) msg == natives.wmNotify)
                    return onNotify(header, hwnd, (int) msg, wParam, lParam);

                long result = natives.callWindowProc(isHeader ? header.oldHeaderProc : header.oldParentProc, hwnd,
                                (int) msg, wParam, lParam);

                if (isHeader && active && (int) msg == natives.hdmLayout)
                    addLine(hwnd, lParam);

                if (isHeader && !failed)
                    trackHover(header, (int) msg, lParam);

                return result;
            }
            catch (ReflectiveOperationException | RuntimeException e)
            {
                fail(e);
                return 0;
            }
        }
    }

    private static void addLine(long hwndHeader, long hdLayout)
    {
        try
        {
            natives.addLine(hwndHeader, hdLayout);
        }
        catch (ReflectiveOperationException | RuntimeException e)
        {
            fail(e);
        }
    }

    /**
     * Paints the header again after the hot or pressed item has changed, see
     * the class comment.
     */
    private static void trackHover(Header header, int msg, long lParam)
    {
        var n = natives;
        try
        {
            if (msg == n.wmMouseMove)
            {
                var index = n.itemAt(header.hwndHeader, (short) (lParam & 0xFFFF), columns(header.control).length,
                                false);
                if (index == header.hotIndex)
                    return;
                header.hotIndex = index;
            }
            else if (msg == n.wmMouseLeave)
            {
                header.hotIndex = -1;
            }
            else if (msg == n.wmLButtonDown)
            {
                header.pressedIndex = n.itemAt(header.hwndHeader, (short) (lParam & 0xFFFF),
                                columns(header.control).length, true);
            }
            else if (msg == n.wmLButtonUp || msg == n.wmCaptureChanged)
            {
                header.pressedIndex = -1;
            }
            else
            {
                return;
            }

            n.invalidate(header.hwndHeader);
            var display = header.control.getDisplay();
            display.timerExec(REPAINT_SOON_MS, header.repaintSoon);
            display.timerExec(REPAINT_LATE_MS, header.repaintLate);
        }
        catch (ReflectiveOperationException | RuntimeException e)
        {
            fail(e);
        }
    }

    /**
     * Handles WM_NOTIFY at the parent of the header. Calls SWT's procedure
     * exactly once.
     */
    private static long onNotify(Header header, long hwnd, int msg, long wParam, long lParam)
    {
        var n = natives;

        CustomDraw draw = null;
        var swtDraws = false;
        try
        {
            var code = n.notifyCode(lParam, header.hwndHeader);
            if (code == n.hdnDividerDblClick)
                return fitColumn(header, n.readHeaderItem(lParam), hwnd, msg, wParam, lParam);

            if (code == n.nmCustomDraw)
            {
                draw = n.readCustomDraw(lParam);
                swtDraws = n.isCustomHeaderDrawing(header.control);
            }
        }
        catch (ReflectiveOperationException | RuntimeException e)
        {
            fail(e);
            draw = null;
        }

        if (draw != null && header.wrapped && swtDraws && draw.stage == n.cddsPostpaint)
            return postPaint(header, draw, hwnd, msg, wParam, lParam);

        if (draw != null && header.wrapped && !swtDraws && draw.stage == n.cddsItemPrepaint
                        && !header.swtWantsItemDraw)
            return itemPrepaintWithoutSwt(header, draw);

        long result = n.callWindowProc(header.oldParentProc, hwnd, msg, wParam, lParam);
        if (draw == null || failed)
            return result;

        if (swtDraws)
        {
            if (draw.stage == n.cddsItemPrepaint)
                highlightItem(header, draw);
            return result;
        }

        if (!header.wrapped)
            return result;

        try
        {
            if (draw.stage == n.cddsPrepaint)
            {
                header.swtWantsItemDraw = (result & n.cdrfNotifyItemDraw) != 0;
                return result | n.cdrfNotifyItemDraw;
            }

            if (draw.stage == n.cddsItemPrepaint && drawItem(header, draw, (result & CDRF_NEWFONT) != 0))
                return n.cdrfSkipDefault;
        }
        catch (ReflectiveOperationException | RuntimeException e)
        {
            fail(e);
        }
        return result;
    }

    /**
     * Highlights the hot or pressed item while SWT draws the header: SWT has
     * filled the item, and draws the labels, lines and sort arrow only later
     * in {@code CDDS_POSTPAINT}.
     */
    private static void highlightItem(Header header, CustomDraw draw)
    {
        var index = (int) draw.itemSpec;
        int percent;
        if (index == header.pressedIndex)
            percent = PRESSED_HIGHLIGHT_PERCENT;
        else if (index == header.hotIndex)
            percent = HOT_HIGHLIGHT_PERCENT;
        else
            return;

        try
        {
            var n = natives;
            var color = mix(n.headerBackgroundPixel(header.control), n.headerForegroundPixel(header.control),
                            percent);
            n.fillRect(draw.hdc, draw.left, draw.top, draw.right, draw.bottom, color);
        }
        catch (ReflectiveOperationException | RuntimeException e)
        {
            fail(e);
        }
    }

    /** mixes two COLORREF values (0x00BBGGRR) */
    private static int mix(int background, int foreground, int percent)
    {
        var mixed = 0;
        for (int shift = 0; shift <= 16; shift += 8)
        {
            var from = (background >> shift) & 0xFF;
            var to = (foreground >> shift) & 0xFF;
            mixed |= (from + (to - from) * percent / 100) << shift;
        }
        return mixed;
    }

    /**
     * Lets SWT fit a column after a double click on its divider, then makes
     * sure all cells fit, see
     * {@link ShowHideColumnHelper#completeFit(Item, Control, String)}. While SWT
     * fits the column, a wrappable label is only the longer line of its most
     * balanced two-line split. The text is set with
     * {@code setText}, not only into {@code Item.text}: depending on the table,
     * SWT packs the column (which measures {@code Item.text}) or leaves the
     * double click to the native list view (which measures the native header
     * item). Calls SWT's procedure exactly once.
     */
    private static long fitColumn(Header header, int index, long hwnd, int msg, long wParam, long lParam)
                    throws ReflectiveOperationException
    {
        var columns = columns(header.control);
        var column = index >= 0 && index < columns.length ? columns[index] : null;
        if (column == null)
            return natives.callWindowProc(header.oldParentProc, hwnd, msg, wParam, lParam);

        var line = ShowHideColumnHelper.headerLineToFit(column, header.control);
        var label = column.getText();
        if (line != null)
            column.setText(line); // may throw: SWT not called yet

        // from here on SWT has been called: never throw, never call it again
        long result;
        try
        {
            result = natives.callWindowProc(header.oldParentProc, hwnd, msg, wParam, lParam);
        }
        finally
        {
            if (line != null)
                restoreLabel(column, line, label);
        }

        try
        {
            ShowHideColumnHelper.completeFit(column, header.control, line);
        }
        catch (RuntimeException e)
        {
            fail(e);
        }
        return result;
    }

    private static void restoreLabel(Item column, String line, String label)
    {
        try
        {
            // a resize during the fit may already have set a new label
            if (!column.isDisposed() && line.equals(column.getText()))
                column.setText(label);
        }
        catch (RuntimeException e)
        {
            fail(e);
        }
    }

    /**
     * Handles an item notification of the native header that only we have
     * asked for, without passing it to SWT: draws a wrapped label and leaves
     * all other items (and a wrapped label that cannot be drawn) to the native
     * header.
     */
    private static long itemPrepaintWithoutSwt(Header header, CustomDraw draw)
    {
        try
        {
            return drawItem(header, draw, false) ? natives.cdrfSkipDefault : natives.cdrfDoDefault;
        }
        catch (ReflectiveOperationException | RuntimeException e)
        {
            fail(e);
            return natives.cdrfDoDefault;
        }
    }

    // --- SWT draws the header ------------------------------------------------

    /**
     * Lets SWT draw the header without the labels of wrapped columns, then
     * draws those labels on two lines.
     */
    private static long postPaint(Header header, CustomDraw draw, long hwnd, int msg, long wParam, long lParam)
    {
        var n = natives;
        var columns = columns(header.control);
        var labels = new String[columns.length];

        try
        {
            for (int ii = 0; ii < columns.length; ii++)
            {
                if (!isWrapped(columns[ii]))
                    continue;
                var text = columns[ii].getText();
                n.setItemText(columns[ii], "");
                labels[ii] = text;
            }
        }
        catch (ReflectiveOperationException | RuntimeException e)
        {
            fail(e);
        }

        long result;
        try
        {
            result = n.callWindowProc(header.oldParentProc, hwnd, msg, wParam, lParam);
        }
        finally
        {
            restoreLabels(columns, labels);
        }

        if (failed)
            return result;

        try
        {
            var color = n.headerForegroundPixel(header.control);
            for (int ii = 0; ii < columns.length; ii++)
            {
                if (labels[ii] != null)
                    n.drawPlainLabel(header.control, header.hwndHeader, draw.hdc, ii, labels[ii],
                                    alignment(columns[ii]), color);
            }
        }
        catch (ReflectiveOperationException | RuntimeException e)
        {
            fail(e);
        }
        return result;
    }

    private static void restoreLabels(Item[] columns, String[] labels)
    {
        for (int ii = 0; ii < columns.length; ii++)
        {
            if (labels[ii] == null)
                continue;
            try
            {
                natives.setItemText(columns[ii], labels[ii]);
            }
            catch (ReflectiveOperationException | RuntimeException e)
            {
                PortfolioPlugin.log(e);
            }
        }
    }

    // --- native header draws -------------------------------------------------

    /**
     * Draws a header item with a wrapped label like the native header does, but
     * with the label on two lines.
     *
     * @return {@code true} if the item was drawn, {@code false} if it is left
     *         to the native header
     */
    private static boolean drawItem(Header header, CustomDraw draw, boolean useDcTextColor)
                    throws ReflectiveOperationException
    {
        var control = header.control;
        if (control.isDisposed())
            return false;

        var columns = columns(control);
        var index = (int) draw.itemSpec;
        if (index < 0 || index >= columns.length || !isWrapped(columns[index]))
            return false;

        var column = columns[index];
        var sortDirection = sortDirection(control, column);
        var sorted = sortDirection == SWT.UP || sortDirection == SWT.DOWN;

        // the header does not report the hot item in the custom draw
        // notification: use the item tracked under the mouse
        int state;
        if ((draw.itemState & natives.cdisSelected) != 0)
            state = HIS_PRESSED;
        else if ((draw.itemState & natives.cdisHot) != 0 || index == header.hotIndex)
            state = HIS_HOT;
        else
            state = HIS_NORMAL;
        if (sorted)
            state += HIS_SORTED_OFFSET;

        var n = natives;
        long theme = n.openThemeData(header.hwndHeader);
        if (theme == 0)
            return false;

        try
        {
            n.drawThemeBackground(theme, draw.hdc, HP_HEADERITEM, state, draw.left, draw.top, draw.right,
                            draw.bottom);

            if (sorted)
                n.drawSortArrow(theme, draw, sortDirection == SWT.UP ? HSAS_SORTEDUP : HSAS_SORTEDDOWN);

            n.drawThemedLabel(theme, header.hwndHeader, draw, state, column.getText(), alignment(column),
                            useDcTextColor);
        }
        finally
        {
            n.closeThemeData(theme);
        }
        return true;
    }

    private static int sortDirection(Control control, Item column)
    {
        if (control instanceof Table table && table.getSortColumn() == column)
            return table.getSortDirection();
        if (control instanceof Tree tree && tree.getSortColumn() == column)
            return tree.getSortDirection();
        return SWT.NONE;
    }

    private static int alignment(Item column)
    {
        if (column instanceof TableColumn tableColumn)
            return tableColumn.getAlignment();
        if (column instanceof TreeColumn treeColumn)
            return treeColumn.getAlignment();
        return SWT.LEFT;
    }

    // --- native access ---------------------------------------------------------

    /** the fields of NMCUSTOMDRAW that are needed to draw a header item */
    private record CustomDraw(int stage, long hdc, int left, int top, int right, int bottom, long itemSpec,
                    int itemState)
    {
    }

    /** Reflective access to the SWT win32 internals. */
    private static final class Natives
    {
        // WINDOWPOS on 64-bit: hwnd, hwndInsertAfter (8 bytes each), then
        // x, y, cx, cy, flags (4 bytes each)
        private static final int WINDOWPOS_BYTES = 36;
        private static final int WINDOWPOS_CY = 7;
        // RECT: left, top, right, bottom
        private static final int RECT_BYTES = 16;
        private static final int RECT_TOP = 1;
        // HDLAYOUT: RECT *prc, WINDOWPOS *pwpos
        private static final int HDLAYOUT_BYTES = 16;
        // NMHDR on 64-bit: hwndFrom, idFrom (8 bytes each), code (4 bytes)
        private static final int NMHDR_LONGS = 3;

        private static final char[] HEADER_CLASS = { 'H', 'E', 'A', 'D', 'E', 'R', 0 };

        private final MethodHandle callWindowProc;
        private final Method getWindowLongPtr;
        private final Method setWindowLongPtr;
        private final Method defWindowProc;
        private final Method getParent;
        private final Method sendMessage;
        private final Method sendMessageRect;
        private final Method getDC;
        private final Method releaseDC;
        private final Method selectObject;
        private final Method getTextMetrics;
        private final Method getSystemMetrics;
        private final Method readLongs;
        private final Method readInts;
        private final Method writeInts;
        private final Method readCustomDraw;
        private final Method openThemeData;
        private final Method closeThemeData;
        private final Method drawThemeBackground;
        private final Method getThemePartSize;
        private final Method drawThemeText;
        private final Method drawText;
        private final Method setBkMode;
        private final Method setTextColor;
        private final Method invalidateRect;
        private final Method createSolidBrush;
        private final Method fillRect;
        private final Method deleteObject;

        private final Constructor<?> newTextMetric;
        private final Field tmHeight;
        private final Field tmExternalLeading;

        private final Constructor<?> newRect;
        private final Field rectLeft;
        private final Field rectTop;
        private final Field rectRight;
        private final Field rectBottom;

        private final Constructor<?> newSize;
        private final Field sizeCx;
        private final Field sizeCy;

        private final Constructor<?> newHeaderNotification;
        private final Method readHeaderNotification;
        private final int headerNotificationSize;
        private final Field headerItem;

        private final Constructor<?> newCustomDraw;
        private final int customDrawSize;
        private final Field cdStage;
        private final Field cdHdc;
        private final Field cdLeft;
        private final Field cdTop;
        private final Field cdRight;
        private final Field cdBottom;
        private final Field cdItemSpec;
        private final Field cdItemState;

        private final Field itemText;
        private final Field tableHeader;
        private final Field treeHeader;
        private final Method tableCustomHeaderDrawing;
        private final Method treeCustomHeaderDrawing;
        private final Method tableHeaderForeground;
        private final Method treeHeaderForeground;
        private final Method tableHeaderBackground;
        private final Method treeHeaderBackground;
        private final int tableInset;
        private final int treeInset;

        private final int gwlpWndProc;
        private final int hdmLayout;
        private final int hdmGetItemRect;
        private final int wmGetFont;
        private final int wmNotify;
        private final int nmCustomDraw;
        private final int hdnDividerDblClick;
        private final int wmMouseMove;
        private final int wmMouseLeave;
        private final int wmLButtonDown;
        private final int wmLButtonUp;
        private final int wmCaptureChanged;
        private final int cddsPrepaint;
        private final int cddsPostpaint;
        private final int cddsItemPrepaint;
        private final long cdrfNotifyItemDraw;
        private final long cdrfSkipDefault;
        private final long cdrfDoDefault;
        private final int cdisSelected;
        private final int cdisHot;
        private final int dtLeft;
        private final int dtCenter;
        private final int dtRight;
        private final int dtNoPrefix;
        private final int dtEndEllipsis;
        private final int dtCalcRect;
        private final int smCxEdge;
        private final int transparent;
        private final int tsTrue;

        /** keeps the native callback alive for the lifetime of the application */
        @SuppressWarnings("unused")
        private final Object callback;
        private final long callbackAddress;

        private Natives() throws ReflectiveOperationException
        {
            var os = Class.forName("org.eclipse.swt.internal.win32.OS");
            var rect = Class.forName("org.eclipse.swt.internal.win32.RECT");
            var size = Class.forName("org.eclipse.swt.internal.win32.SIZE");
            var textMetric = Class.forName("org.eclipse.swt.internal.win32.TEXTMETRIC");
            var customDraw = Class.forName("org.eclipse.swt.internal.win32.NMCUSTOMDRAW");
            var headerNotification = Class.forName("org.eclipse.swt.internal.win32.NMHEADER");

            callWindowProc = MethodHandles.publicLookup().unreflect(
                            os.getMethod("CallWindowProc", long.class, long.class, int.class, long.class, long.class));
            getWindowLongPtr = os.getMethod("GetWindowLongPtr", long.class, int.class);
            setWindowLongPtr = os.getMethod("SetWindowLongPtr", long.class, int.class, long.class);
            defWindowProc = os.getMethod("DefWindowProc", long.class, int.class, long.class, long.class);
            getParent = os.getMethod("GetParent", long.class);
            sendMessage = os.getMethod("SendMessage", long.class, int.class, long.class, long.class);
            sendMessageRect = os.getMethod("SendMessage", long.class, int.class, long.class, rect);
            getDC = os.getMethod("GetDC", long.class);
            releaseDC = os.getMethod("ReleaseDC", long.class, long.class);
            selectObject = os.getMethod("SelectObject", long.class, long.class);
            getTextMetrics = os.getMethod("GetTextMetrics", long.class, textMetric);
            getSystemMetrics = os.getMethod("GetSystemMetrics", int.class);
            readLongs = os.getMethod("MoveMemory", long[].class, long.class, int.class);
            readInts = os.getMethod("MoveMemory", int[].class, long.class, int.class);
            writeInts = os.getMethod("MoveMemory", long.class, int[].class, int.class);
            readCustomDraw = os.getMethod("MoveMemory", customDraw, long.class, int.class);
            openThemeData = os.getMethod("OpenThemeData", long.class, char[].class);
            closeThemeData = os.getMethod("CloseThemeData", long.class);
            drawThemeBackground = os.getMethod("DrawThemeBackground", long.class, long.class, int.class, int.class,
                            rect, rect);
            getThemePartSize = os.getMethod("GetThemePartSize", long.class, long.class, int.class, int.class, rect,
                            int.class, size);
            drawThemeText = os.getMethod("DrawThemeText", long.class, long.class, int.class, int.class, char[].class,
                            int.class, int.class, int.class, rect);
            drawText = os.getMethod("DrawText", long.class, char[].class, int.class, rect, int.class);
            setBkMode = os.getMethod("SetBkMode", long.class, int.class);
            setTextColor = os.getMethod("SetTextColor", long.class, int.class);
            invalidateRect = os.getMethod("InvalidateRect", long.class, rect, boolean.class);
            createSolidBrush = os.getMethod("CreateSolidBrush", int.class);
            fillRect = os.getMethod("FillRect", long.class, rect, long.class);
            deleteObject = os.getMethod("DeleteObject", long.class);

            newTextMetric = textMetric.getConstructor();
            tmHeight = textMetric.getField("tmHeight");
            tmExternalLeading = textMetric.getField("tmExternalLeading");

            newRect = rect.getConstructor();
            rectLeft = rect.getField("left");
            rectTop = rect.getField("top");
            rectRight = rect.getField("right");
            rectBottom = rect.getField("bottom");

            newSize = size.getConstructor();
            sizeCx = size.getField("cx");
            sizeCy = size.getField("cy");

            newHeaderNotification = headerNotification.getConstructor();
            readHeaderNotification = os.getMethod("MoveMemory", headerNotification, long.class, int.class);
            headerNotificationSize = headerNotification.getField("sizeof").getInt(null);
            headerItem = headerNotification.getField("iItem");

            newCustomDraw = customDraw.getConstructor();
            customDrawSize = customDraw.getField("sizeof").getInt(null);
            cdStage = customDraw.getField("dwDrawStage");
            cdHdc = customDraw.getField("hdc");
            cdLeft = customDraw.getField("left");
            cdTop = customDraw.getField("top");
            cdRight = customDraw.getField("right");
            cdBottom = customDraw.getField("bottom");
            cdItemSpec = customDraw.getField("dwItemSpec");
            cdItemState = customDraw.getField("uItemState");

            gwlpWndProc = os.getField("GWLP_WNDPROC").getInt(null);
            hdmLayout = os.getField("HDM_LAYOUT").getInt(null);
            hdmGetItemRect = os.getField("HDM_GETITEMRECT").getInt(null);
            wmGetFont = os.getField("WM_GETFONT").getInt(null);
            wmNotify = os.getField("WM_NOTIFY").getInt(null);
            nmCustomDraw = os.getField("NM_CUSTOMDRAW").getInt(null);
            hdnDividerDblClick = os.getField("HDN_DIVIDERDBLCLICK").getInt(null);
            wmMouseMove = os.getField("WM_MOUSEMOVE").getInt(null);
            wmMouseLeave = os.getField("WM_MOUSELEAVE").getInt(null);
            wmLButtonDown = os.getField("WM_LBUTTONDOWN").getInt(null);
            wmLButtonUp = os.getField("WM_LBUTTONUP").getInt(null);
            wmCaptureChanged = os.getField("WM_CAPTURECHANGED").getInt(null);
            cddsPrepaint = os.getField("CDDS_PREPAINT").getInt(null);
            cddsPostpaint = os.getField("CDDS_POSTPAINT").getInt(null);
            cddsItemPrepaint = os.getField("CDDS_ITEMPREPAINT").getInt(null);
            cdrfNotifyItemDraw = os.getField("CDRF_NOTIFYITEMDRAW").getInt(null);
            cdrfSkipDefault = os.getField("CDRF_SKIPDEFAULT").getInt(null);
            cdrfDoDefault = os.getField("CDRF_DODEFAULT").getInt(null);
            cdisSelected = os.getField("CDIS_SELECTED").getInt(null);
            cdisHot = os.getField("CDIS_HOT").getInt(null);
            dtLeft = os.getField("DT_LEFT").getInt(null);
            dtCenter = os.getField("DT_CENTER").getInt(null);
            dtRight = os.getField("DT_RIGHT").getInt(null);
            dtNoPrefix = os.getField("DT_NOPREFIX").getInt(null);
            dtEndEllipsis = os.getField("DT_ENDELLIPSIS").getInt(null);
            dtCalcRect = os.getField("DT_CALCRECT").getInt(null);
            smCxEdge = os.getField("SM_CXEDGE").getInt(null);
            transparent = os.getField("TRANSPARENT").getInt(null);
            tsTrue = os.getField("TS_TRUE").getInt(null);

            // SWT draws the header labels from this field (Table, Tree)
            itemText = Item.class.getDeclaredField("text");
            itemText.setAccessible(true);

            tableHeader = Table.class.getDeclaredField("hwndHeader");
            tableHeader.setAccessible(true);
            treeHeader = Tree.class.getDeclaredField("hwndHeader");
            treeHeader.setAccessible(true);

            // getHeaderBackground() never returns null, it falls back to the
            // default color: ask SWT whether it draws the header itself
            tableCustomHeaderDrawing = Table.class.getDeclaredMethod("customHeaderDrawing");
            tableCustomHeaderDrawing.setAccessible(true);
            treeCustomHeaderDrawing = Tree.class.getDeclaredMethod("customHeaderDrawing");
            treeCustomHeaderDrawing.setAccessible(true);

            // the color SWT itself uses for the header labels
            tableHeaderForeground = Table.class.getDeclaredMethod("getHeaderForegroundPixel");
            tableHeaderForeground.setAccessible(true);
            treeHeaderForeground = Tree.class.getDeclaredMethod("getHeaderForegroundPixel");
            treeHeaderForeground.setAccessible(true);
            tableHeaderBackground = Table.class.getDeclaredMethod("getHeaderBackgroundPixel");
            tableHeaderBackground.setAccessible(true);
            treeHeaderBackground = Tree.class.getDeclaredMethod("getHeaderBackgroundPixel");
            treeHeaderBackground.setAccessible(true);

            // SWT draws the header label with INSET + 2 on the left and INSET
            // on the right (unscaled, in physical pixels)
            tableInset = staticInt(Table.class, "INSET");
            treeInset = staticInt(Tree.class, "INSET");

            var callbackType = Class.forName("org.eclipse.swt.internal.Callback");
            callback = callbackType.getConstructor(Object.class, String.class, int.class)
                            .newInstance(new WindowProc(), "windowProc", 4);
            callbackAddress = (long) callbackType.getMethod("getAddress").invoke(callback);
            if (callbackAddress == 0)
                throw new IllegalStateException("SWT callback for the header procedures could not be created");
        }

        private static int staticInt(Class<?> type, String name) throws ReflectiveOperationException
        {
            var field = type.getDeclaredField(name);
            field.setAccessible(true);
            return field.getInt(null);
        }

        // --- widgets

        long headerHandle(Control control) throws IllegalAccessException
        {
            if (control instanceof Table)
                return tableHeader.getLong(control);
            if (control instanceof Tree)
                return treeHeader.getLong(control);
            return 0;
        }

        /** true if SWT draws the header itself because header colors are set */
        boolean isCustomHeaderDrawing(Control control) throws ReflectiveOperationException
        {
            if (control instanceof Table)
                return (boolean) tableCustomHeaderDrawing.invoke(control);
            if (control instanceof Tree)
                return (boolean) treeCustomHeaderDrawing.invoke(control);
            return false;
        }

        int headerForegroundPixel(Control control) throws ReflectiveOperationException
        {
            if (control instanceof Table)
                return (int) tableHeaderForeground.invoke(control);
            return (int) treeHeaderForeground.invoke(control);
        }

        int headerBackgroundPixel(Control control) throws ReflectiveOperationException
        {
            if (control instanceof Table)
                return (int) tableHeaderBackground.invoke(control);
            return (int) treeHeaderBackground.invoke(control);
        }

        void fillRect(long hdc, int left, int top, int right, int bottom, int color)
                        throws ReflectiveOperationException
        {
            long brush = (long) createSolidBrush.invoke(null, color);
            try
            {
                fillRect.invoke(null, hdc, rect(left, top, right, bottom), brush);
            }
            finally
            {
                deleteObject.invoke(null, brush);
            }
        }

        /**
         * The left and right margin of the label in physical pixels: SWT's
         * insets if SWT draws the header, otherwise the native text margin.
         */
        int[] labelMargins(Control control, boolean swtDraws) throws ReflectiveOperationException
        {
            if (swtDraws)
            {
                var inset = control instanceof Tree ? treeInset : tableInset;
                return new int[] { inset + 2, inset };
            }
            var margin = 3 * (int) getSystemMetrics.invoke(null, smCxEdge);
            return new int[] { margin, margin };
        }

        void invalidate(long hwnd) throws ReflectiveOperationException
        {
            invalidateRect.invoke(null, hwnd, null, false);
        }

        /**
         * Returns the index of the header item at x, -1 if none.
         *
         * @param excludeDivider
         *            whether to return -1 next to a divider, where a press
         *            resizes the column instead of pressing the item
         */
        int itemAt(long hwndHeader, int x, int count, boolean excludeDivider) throws ReflectiveOperationException
        {
            var item = newRect.newInstance();
            for (int ii = 0; ii < count; ii++)
            {
                sendMessageRect.invoke(null, hwndHeader, hdmGetItemRect, (long) ii, item);
                var left = rectLeft.getInt(item);
                var right = rectRight.getInt(item);
                if (x < left || x >= right)
                    continue;
                if (excludeDivider)
                {
                    var tolerance = 3 * (int) getSystemMetrics.invoke(null, smCxEdge);
                    if (x - left < tolerance || right - x <= tolerance)
                        return -1;
                }
                return ii;
            }
            return -1;
        }

        /** sets the label SWT draws without touching the native header item */
        void setItemText(Item item, String text) throws IllegalAccessException
        {
            itemText.set(item, text);
        }

        // --- window handles and procedures

        long getParent(long hwnd) throws ReflectiveOperationException
        {
            return (long) getParent.invoke(null, hwnd);
        }

        long getWindowProc(long hwnd) throws ReflectiveOperationException
        {
            return (long) getWindowLongPtr.invoke(null, hwnd, gwlpWndProc);
        }

        void setWindowProc(long hwnd, long proc) throws ReflectiveOperationException
        {
            setWindowLongPtr.invoke(null, hwnd, gwlpWndProc, proc);
        }

        long callWindowProc(long proc, long hwnd, int msg, long wParam, long lParam)
        {
            try
            {
                return (long) callWindowProc.invokeExact(proc, hwnd, msg, wParam, lParam);
            }
            catch (RuntimeException | Error e)
            {
                throw e;
            }
            catch (Throwable t)
            {
                throw new IllegalStateException(t);
            }
        }

        long defWindowProc(long hwnd, int msg, long wParam, long lParam) throws ReflectiveOperationException
        {
            return (long) defWindowProc.invoke(null, hwnd, msg, wParam, lParam);
        }

        // --- header height

        /**
         * Enlarges the header height (pwpos->cy) and moves the remaining area
         * (prc->top) down by one line of the header font.
         */
        void addLine(long hwndHeader, long hdLayout) throws ReflectiveOperationException
        {
            var pointers = new long[2];
            readLongs.invoke(null, pointers, hdLayout, HDLAYOUT_BYTES);
            long prc = pointers[0];
            long pwpos = pointers[1];

            var pos = new int[WINDOWPOS_BYTES / Integer.BYTES];
            readInts.invoke(null, pos, pwpos, WINDOWPOS_BYTES);
            if (pos[WINDOWPOS_CY] <= 0)
                return; // header hidden

            var area = new int[RECT_BYTES / Integer.BYTES];
            readInts.invoke(null, area, prc, RECT_BYTES);

            var extra = lineHeight(hwndHeader);
            pos[WINDOWPOS_CY] += extra;
            area[RECT_TOP] += extra;

            writeInts.invoke(null, pwpos, pos, WINDOWPOS_BYTES);
            writeInts.invoke(null, prc, area, RECT_BYTES);
        }

        /** height of one text line of the header font in physical pixels */
        private int lineHeight(long hwndHeader) throws ReflectiveOperationException
        {
            long hdc = (long) getDC.invoke(null, hwndHeader);
            try
            {
                long oldFont = selectHeaderFont(hwndHeader, hdc);
                var metric = newTextMetric.newInstance();
                getTextMetrics.invoke(null, hdc, metric);
                restoreFont(hdc, oldFont);
                return tmHeight.getInt(metric) + tmExternalLeading.getInt(metric);
            }
            finally
            {
                releaseDC.invoke(null, hwndHeader, hdc);
            }
        }

        private long selectHeaderFont(long hwndHeader, long hdc) throws ReflectiveOperationException
        {
            long font = (long) sendMessage.invoke(null, hwndHeader, wmGetFont, 0L, 0L);
            return font != 0 ? (long) selectObject.invoke(null, hdc, font) : 0;
        }

        private void restoreFont(long hdc, long oldFont) throws ReflectiveOperationException
        {
            if (oldFont != 0)
                selectObject.invoke(null, hdc, oldFont);
        }

        // --- custom draw

        /**
         * Returns the notification code if the notification comes from the
         * header, otherwise 0 (all codes used here are negative).
         */
        int notifyCode(long nmhdr, long hwndHeader) throws ReflectiveOperationException
        {
            var header = new long[NMHDR_LONGS];
            readLongs.invoke(null, header, nmhdr, NMHDR_LONGS * Long.BYTES);
            return header[0] == hwndHeader ? (int) header[2] : 0;
        }

        /** the item index of a NMHEADER notification */
        int readHeaderItem(long nmheader) throws ReflectiveOperationException
        {
            var notification = newHeaderNotification.newInstance();
            readHeaderNotification.invoke(null, notification, nmheader, headerNotificationSize);
            return headerItem.getInt(notification);
        }

        CustomDraw readCustomDraw(long lParam) throws ReflectiveOperationException
        {
            var nmcd = newCustomDraw.newInstance();
            readCustomDraw.invoke(null, nmcd, lParam, customDrawSize);
            return new CustomDraw(cdStage.getInt(nmcd), cdHdc.getLong(nmcd), cdLeft.getInt(nmcd),
                            cdTop.getInt(nmcd), cdRight.getInt(nmcd), cdBottom.getInt(nmcd),
                            cdItemSpec.getLong(nmcd), cdItemState.getInt(nmcd));
        }

        long openThemeData(long hwnd) throws ReflectiveOperationException
        {
            return (long) openThemeData.invoke(null, hwnd, HEADER_CLASS);
        }

        void closeThemeData(long theme) throws ReflectiveOperationException
        {
            closeThemeData.invoke(null, theme);
        }

        void drawThemeBackground(long theme, long hdc, int part, int state, int left, int top, int right,
                        int bottom) throws ReflectiveOperationException
        {
            drawThemeBackground.invoke(null, theme, hdc, part, state, rect(left, top, right, bottom), null);
        }

        /** draws the sort arrow centered at the top, where the native header puts it */
        void drawSortArrow(long theme, CustomDraw draw, int arrowState) throws ReflectiveOperationException
        {
            var arrowSize = newSize.newInstance();
            getThemePartSize.invoke(null, theme, draw.hdc, HP_HEADERSORTARROW, arrowState, null, tsTrue, arrowSize);
            int width = sizeCx.getInt(arrowSize);
            int height = sizeCy.getInt(arrowSize);
            if (width <= 0 || height <= 0)
                return;

            int left = draw.left + (draw.right - draw.left - width) / 2;
            drawThemeBackground(theme, draw.hdc, HP_HEADERSORTARROW, arrowState, left, draw.top, left + width,
                            draw.top + height);
        }

        /**
         * Draws the label of a native header item, with the text color of the
         * native header: the theme's text color unless the parent has set its
         * own color into the device context.
         */
        void drawThemedLabel(long theme, long hwndHeader, CustomDraw draw, int state, String text, int alignment,
                        boolean useDcTextColor) throws ReflectiveOperationException
        {
            var chars = text.toCharArray();
            int flags = labelFlags(alignment);

            long oldFont = selectHeaderFont(hwndHeader, draw.hdc);
            try
            {
                var margins = labelMargins(null, false);
                var bounds = labelBounds(draw.hdc, chars, draw.left + margins[0], draw.top,
                                draw.right - margins[1], draw.bottom);
                if (useDcTextColor)
                {
                    setBkMode.invoke(null, draw.hdc, transparent);
                    drawText.invoke(null, draw.hdc, chars, chars.length, bounds, flags);
                }
                else
                {
                    drawThemeText.invoke(null, theme, draw.hdc, HP_HEADERITEM, state, chars, chars.length, flags, 0,
                                    bounds);
                }
            }
            finally
            {
                restoreFont(draw.hdc, oldFont);
            }
        }

        /**
         * Draws the label of a header item that SWT draws itself, in SWT's
         * header foreground color.
         */
        void drawPlainLabel(Control control, long hwndHeader, long hdc, int index, String text, int alignment,
                        int color) throws ReflectiveOperationException
        {
            var item = newRect.newInstance();
            sendMessageRect.invoke(null, hwndHeader, hdmGetItemRect, (long) index, item);

            var chars = text.toCharArray();
            long oldFont = selectHeaderFont(hwndHeader, hdc);
            int oldColor = (int) setTextColor.invoke(null, hdc, color);
            int oldMode = (int) setBkMode.invoke(null, hdc, transparent);
            try
            {
                var margins = labelMargins(control, true);
                var bounds = labelBounds(hdc, chars, rectLeft.getInt(item) + margins[0], rectTop.getInt(item),
                                rectRight.getInt(item) - margins[1], rectBottom.getInt(item));
                drawText.invoke(null, hdc, chars, chars.length, bounds, labelFlags(alignment));
            }
            finally
            {
                setBkMode.invoke(null, hdc, oldMode);
                setTextColor.invoke(null, hdc, oldColor);
                restoreFont(hdc, oldFont);
            }
        }

        private int labelFlags(int alignment)
        {
            return dtNoPrefix | dtEndEllipsis | switch (alignment)
            {
                case SWT.RIGHT -> dtRight;
                case SWT.CENTER -> dtCenter;
                default -> dtLeft;
            };
        }

        /**
         * The rectangle of the label between the given left and right
         * position, vertically centered. Expects the header font to be
         * selected into the device context.
         */
        private Object labelBounds(long hdc, char[] chars, int left, int top, int right, int bottom)
                        throws ReflectiveOperationException
        {
            var measure = rect(left, top, right, bottom);
            drawText.invoke(null, hdc, chars, chars.length, measure, dtNoPrefix | dtCalcRect);
            int textHeight = rectBottom.getInt(measure) - rectTop.getInt(measure);

            int textTop = top + Math.max(0, (bottom - top - textHeight) / 2);
            return rect(left, textTop, right, Math.min(bottom, textTop + textHeight));
        }

        private Object rect(int left, int top, int right, int bottom) throws ReflectiveOperationException
        {
            var rect = newRect.newInstance();
            rectLeft.setInt(rect, left);
            rectTop.setInt(rect, top);
            rectRight.setInt(rect, right);
            rectBottom.setInt(rect, bottom);
            return rect;
        }
    }
}
