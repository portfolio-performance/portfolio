package name.abuchen.portfolio.ui.util.viewers;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.eclipse.core.runtime.Platform;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Table;

import name.abuchen.portfolio.PortfolioLog;

/**
 * Works around the white sorted column of tables on Windows (issue #1823).
 * <p>
 * SWT makes the native list view transparent (background color CLR_NONE) as
 * soon as owner-draw listeners are registered, for example by the shares
 * column. On a transparent table, SWT paints the sorted column itself.
 * However, the native highlight of the sorted column (LVM_SETSELECTEDCOLUMN)
 * can remain set. Windows then shades the column based on the system window
 * color, which shows as a white column in the dark theme.
 * <p>
 * This class clears the native highlight on transparent tables. The internal
 * SWT class and the native handle of the table (which exists only in the
 * Windows implementation of SWT) are accessed via reflection so that the code
 * compiles on all platforms.
 */
/* package */ final class SortColumnHighlightFix
{
    private static final boolean IS_WINDOWS = Platform.OS_WIN32.equals(Platform.getOS());

    private static final int LVM_FIRST = 0x1000;
    private static final int LVM_GETBKCOLOR = LVM_FIRST;
    private static final int LVM_SETSELECTEDCOLUMN = LVM_FIRST + 140;
    private static final int LVM_GETSELECTEDCOLUMN = LVM_FIRST + 174;
    private static final int CLR_NONE = 0xFFFFFFFF;
    private static final int NO_COLUMN = -1;

    private static Method sendMessage;
    private static Field handleField;
    private static boolean isUnavailable = false;

    private SortColumnHighlightFix()
    {
    }

    /**
     * Clears the native highlight of the sorted column immediately (before
     * the next paint) and once more after the pending events have been
     * processed.
     */
    /* package */ static void clear(Table table)
    {
        if (!IS_WINDOWS || table == null || table.isDisposed())
            return;

        clearNow(table);
        table.getDisplay().asyncExec(() -> clearNow(table));
    }

    private static void clearNow(Table table)
    {
        if (table.isDisposed())
            return;

        if (!initialize())
            return;

        var send = sendMessage;

        try
        {
            long handle = handleField.getLong(table);

            int bkColor = (int) (long) send.invoke(null, handle, LVM_GETBKCOLOR, 0L, 0L);
            if (bkColor != CLR_NONE)
                return;

            int selected = (int) (long) send.invoke(null, handle, LVM_GETSELECTEDCOLUMN, 0L, 0L);
            if (selected == NO_COLUMN)
                return;

            send.invoke(null, handle, LVM_SETSELECTEDCOLUMN, (long) NO_COLUMN, 0L);
            table.redraw();
        }
        catch (ReflectiveOperationException | RuntimeException e)
        {
            isUnavailable = true;
            PortfolioLog.error(e);
        }
    }

    private static boolean initialize()
    {
        if (isUnavailable)
            return false;

        if (sendMessage != null && handleField != null)
            return true;

        try
        {
            var os = Class.forName("org.eclipse.swt.internal.win32.OS", true, Table.class.getClassLoader()); //$NON-NLS-1$
            sendMessage = os.getMethod("SendMessage", long.class, int.class, long.class, long.class); //$NON-NLS-1$
            handleField = Control.class.getField("handle"); //$NON-NLS-1$
            return true;
        }
        catch (ReflectiveOperationException | RuntimeException e)
        {
            isUnavailable = true;
            PortfolioLog.error(e);
            return false;
        }
    }
}
