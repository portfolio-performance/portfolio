package name.abuchen.portfolio.ui.util;

import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.Widget;

public class ViewerHelper
{
    public static void pack(TableViewer viewer)
    {
        pack(viewer.getTable());
    }

    public static void pack(Table table)
    {
        for (int i = 0, n = table.getColumnCount(); i < n; i++)
        {
            TableColumn column = table.getColumn(i);
            int originalWidth = getOriginalWidth(column, column.getWidth());

            column.pack();
            int packedWidth = column.getWidth();

            if (packedWidth < originalWidth)
                column.setWidth(originalWidth);
        }
        table.pack();
    }

    public static void pack(TreeViewer viewer)
    {
        pack(viewer.getTree());
    }

    public static void pack(Tree tree)
    {
        for (int i = 0, n = tree.getColumnCount(); i < n; i++)
        {
            TreeColumn column = tree.getColumn(i);
            int originalWidth = getOriginalWidth(column, column.getWidth());

            column.pack();
            int packedWidth = column.getWidth();

            if (packedWidth < originalWidth)
                column.setWidth(originalWidth);
        }
        tree.pack();
    }

    private static int getOriginalWidth(Widget column, int width)
    {
        if (width == 0)
        {
            Object layoutData = column.getData("org.eclipse.jface.LAYOUT_DATA"); //$NON-NLS-1$
            if (layoutData instanceof ColumnPixelData)
                return ((ColumnPixelData) layoutData).width;
        }

        return width;
    }

    public static void attachContextMenu(ColumnViewer viewer, IMenuListener listener)
    {
        MenuManager menuMgr = new MenuManager("#PopupMenu"); //$NON-NLS-1$
        menuMgr.setRemoveAllWhenShown(true);
        menuMgr.addMenuListener(listener);

        final Menu contextMenu = menuMgr.createContextMenu(viewer.getControl());
        viewer.getControl().setMenu(contextMenu);

        viewer.getControl().addDisposeListener(new DisposeListener()
        {
            @Override
            public void widgetDisposed(DisposeEvent e)
            {
                if (!contextMenu.isDisposed())
                    contextMenu.dispose();
            }
        });
    }
}
