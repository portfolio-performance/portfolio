package name.abuchen.portfolio.ui.util;

import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;

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
            int originalWidth = column.getWidth();
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
            int originalWidth = column.getWidth();
            column.pack();
            int packedWidth = column.getWidth();

            if (packedWidth < originalWidth)
                column.setWidth(originalWidth);
        }
        tree.pack();
    }

}
