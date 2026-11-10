package name.abuchen.portfolio.ui.util.viewers;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.Platform;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;

import name.abuchen.portfolio.ui.PortfolioPlugin;

/**
 * Supports the two-line column headers on Linux (GTK).
 * <p>
 * GTK lays out the header label itself, so an embedded line break already
 * gives a two-line header that grows in height. Two things remain:
 * <ul>
 * <li>GTK draws the header label in bold and with the padding of the header
 * button; the sort indicator sits next to the label. The wrap therefore
 * measures with the bold font and leaves that space, see
 * {@link #headerFont(Control)} and {@link #labelMargin(Control, Item)}.</li>
 * <li>A double click on a header divider makes GTK fit the column to its
 * single-line label and to the cells in the font of the table. As on Windows,
 * the double click is handled instead: SWT packs the column with only the
 * longer line of a wrappable label, then all cells must fit in their own font,
 * see {@link ShowHideColumnHelper#completeFit(Item, Control, String)}.</li>
 * </ul>
 * The handler is connected to the {@code button-press-event} signal of the
 * GTK 3 tree view through SWT's internal {@code Callback}. All SWT internals
 * are called reflectively, as in {@code GtkUnfocusedTextColor}, so that the
 * class loads on every platform. GTK 4 has no such signal; there the double
 * click is left to GTK.
 */
@SuppressWarnings("nls")
final class LinuxHeaderWrap
{
    /** horizontal padding of the GTK header button around the label */
    private static final int HEADER_PADDING = 16;

    /** width of the sort indicator next to the label, including spacing */
    private static final int SORT_INDICATOR_WIDTH = 22;

    /** horizontal padding of a cell */
    private static final int CELL_PADDING = 8;

    /** distance from a column edge in which a double click hits the divider */
    private static final int DIVIDER_TOLERANCE = 6;

    private static final String HEADER_FONT_KEY = "$header_font$";

    /** GTK widget handle of the tree view -> control */
    private static final Map<Long, Control> CONTROLS = new HashMap<>();

    private static Natives natives;
    private static boolean failed;

    private LinuxHeaderWrap()
    {
    }

    private static boolean isLinux()
    {
        return Platform.OS_LINUX.equals(Platform.getOS());
    }

    /**
     * Returns the font in which GTK draws the header labels: the font of the
     * control in bold. The font is created once per control and disposed with
     * it.
     */
    static Font headerFont(Control control)
    {
        if (control.getData(HEADER_FONT_KEY) instanceof Font font && !font.isDisposed())
            return font;

        var data = control.getFont().getFontData();
        for (FontData fontData : data)
            fontData.setStyle(fontData.getStyle() | SWT.BOLD);
        var font = new Font(control.getDisplay(), data);
        control.setData(HEADER_FONT_KEY, font);
        control.addDisposeListener(e -> font.dispose());
        return font;
    }

    /**
     * Returns the horizontal space of a header item that is not available for
     * the label: the padding of the header button, plus the sort indicator if
     * the column is sorted.
     */
    static int labelMargin(Control control, Item column)
    {
        return HEADER_PADDING + (isSortColumn(control, column) ? SORT_INDICATOR_WIDTH : 0);
    }

    /** Returns the horizontal padding of a cell. */
    static int cellMargin()
    {
        return CELL_PADDING;
    }

    private static boolean isSortColumn(Control control, Item column)
    {
        if (control instanceof Table table)
            return table.getSortColumn() == column && table.getSortDirection() != SWT.NONE;
        if (control instanceof Tree tree)
            return tree.getSortColumn() == column && tree.getSortDirection() != SWT.NONE;
        return false;
    }

    /**
     * Connects the double click handler to the tree view of the control, once.
     * Does nothing on other platforms and with GTK 4.
     */
    static void attach(Control control)
    {
        if (failed || control.isDisposed() || !isLinux() || CONTROLS.containsKey(control.handle))
            return;

        try
        {
            var n = natives();
            if (n == null)
                return; // GTK 4

            var handle = control.handle;
            CONTROLS.put(handle, control);
            n.connectButtonPress(handle);
            control.addDisposeListener(e -> CONTROLS.remove(handle));
        }
        catch (ReflectiveOperationException | RuntimeException e)
        {
            fail(e);
        }
    }

    private static void fail(Exception e)
    {
        failed = true;
        PortfolioPlugin.log(e);
    }

    private static Natives natives() throws ReflectiveOperationException
    {
        if (natives == null)
        {
            var gtk = Class.forName("org.eclipse.swt.internal.gtk.GTK");
            if (gtk.getField("GTK4").getBoolean(null))
                return null;
            natives = new Natives();
        }
        return natives;
    }

    /**
     * Target of SWT's {@code Callback}: the {@code button-press-event} handler
     * of the tree views. Returns 1 if the double click on a divider has been
     * handled, which stops GTK from fitting the column itself. Never throws
     * into native code.
     */
    private static final class ButtonPress
    {
        // called from native code by SWT's Callback, which looks it up by name
        @SuppressWarnings("unused")
        long buttonPress(long widget, long event, long userData)
        {
            if (failed)
                return 0;

            try
            {
                var control = CONTROLS.get(widget);
                if (control == null || control.isDisposed() || !natives.isDoubleClickInHeader(widget, event))
                    return 0;

                var x = natives.rootX(event) - control.toDisplay(0, 0).x;
                var column = dividerAt(control, x);
                if (column == null)
                    return 0;

                fitColumn(control, column);
                return 1;
            }
            catch (ReflectiveOperationException | RuntimeException e)
            {
                fail(e);
                return 0;
            }
        }
    }

    /** the resizable column whose right edge is at x, or null */
    private static Item dividerAt(Control control, int x)
    {
        int[] order;
        Item[] columns;
        int scroll;
        if (control instanceof Table table)
        {
            order = table.getColumnOrder();
            columns = table.getColumns();
            scroll = table.getHorizontalBar() != null ? table.getHorizontalBar().getSelection() : 0;
        }
        else if (control instanceof Tree tree)
        {
            order = tree.getColumnOrder();
            columns = tree.getColumns();
            scroll = tree.getHorizontalBar() != null ? tree.getHorizontalBar().getSelection() : 0;
        }
        else
        {
            return null;
        }

        var right = -scroll;
        for (int index : order)
        {
            var column = columns[index];
            right += width(column);
            if (Math.abs(x - right) <= DIVIDER_TOLERANCE)
                return isResizable(column) ? column : null;
        }
        return null;
    }

    private static int width(Item column)
    {
        if (column instanceof TableColumn tableColumn)
            return tableColumn.getWidth();
        if (column instanceof TreeColumn treeColumn)
            return treeColumn.getWidth();
        return 0;
    }

    private static boolean isResizable(Item column)
    {
        if (column instanceof TableColumn tableColumn)
            return tableColumn.getResizable();
        if (column instanceof TreeColumn treeColumn)
            return treeColumn.getResizable();
        return false;
    }

    /**
     * Packs the column with only the longer line of a wrappable label as header
     * text, then completes the fit.
     */
    private static void fitColumn(Control control, Item column)
    {
        var line = ShowHideColumnHelper.headerLineToFit(column, control);
        var label = column.getText();
        if (line != null)
            column.setText(line);

        try
        {
            if (column instanceof TableColumn tableColumn)
                tableColumn.pack();
            else if (column instanceof TreeColumn treeColumn)
                treeColumn.pack();
        }
        finally
        {
            // a resize during the pack may already have set a new label
            if (line != null && !column.isDisposed() && line.equals(column.getText()))
                column.setText(label);
        }

        ShowHideColumnHelper.completeFit(column, control, line);
    }

    /** Reflective access to the SWT GTK internals. */
    private static final class Natives
    {
        private static final byte[] BUTTON_PRESS_EVENT = "button-press-event\0".getBytes(StandardCharsets.US_ASCII);

        private final Method signalConnect;
        private final Method eventType;
        private final Method eventButton;
        private final Method eventWindow;
        private final Method eventRootCoords;
        private final Method binWindow;
        private final int doubleButtonPress;

        /** keeps the native callback alive for the lifetime of the application */
        @SuppressWarnings("unused")
        private final Object callback;
        private final long callbackAddress;

        private Natives() throws ReflectiveOperationException
        {
            var os = Class.forName("org.eclipse.swt.internal.gtk.OS");
            var gdk = Class.forName("org.eclipse.swt.internal.gtk.GDK");
            var gtk3 = Class.forName("org.eclipse.swt.internal.gtk3.GTK3");

            signalConnect = os.getMethod("g_signal_connect", long.class, byte[].class, long.class, long.class);
            eventType = gdk.getMethod("gdk_event_get_event_type", long.class);
            eventButton = gdk.getMethod("gdk_event_get_button", long.class, int[].class);
            eventWindow = gdk.getMethod("gdk_event_get_window", long.class);
            eventRootCoords = gdk.getMethod("gdk_event_get_root_coords", long.class, double[].class, double[].class);
            binWindow = gtk3.getMethod("gtk_tree_view_get_bin_window", long.class);
            doubleButtonPress = gdk.getField("GDK_2BUTTON_PRESS").getInt(null);

            var callbackType = Class.forName("org.eclipse.swt.internal.Callback");
            callback = callbackType.getConstructor(Object.class, String.class, int.class)
                            .newInstance(new ButtonPress(), "buttonPress", 3);
            callbackAddress = (long) callbackType.getMethod("getAddress").invoke(callback);
            if (callbackAddress == 0)
                throw new IllegalStateException("SWT callback for the header double click could not be created");
        }

        void connectButtonPress(long widget) throws ReflectiveOperationException
        {
            signalConnect.invoke(null, widget, BUTTON_PRESS_EVENT, callbackAddress, 0L);
        }

        /**
         * true for a double click with the first button outside of the rows,
         * that is in the header
         */
        boolean isDoubleClickInHeader(long treeView, long event) throws ReflectiveOperationException
        {
            if ((int) eventType.invoke(null, event) != doubleButtonPress)
                return false;

            var button = new int[1];
            eventButton.invoke(null, event, button);
            if (button[0] != 1)
                return false;

            return (long) eventWindow.invoke(null, event) != (long) binWindow.invoke(null, treeView);
        }

        int rootX(long event) throws ReflectiveOperationException
        {
            var x = new double[1];
            var y = new double[1];
            eventRootCoords.invoke(null, event, x, y);
            return (int) Math.round(x[0]);
        }
    }
}
