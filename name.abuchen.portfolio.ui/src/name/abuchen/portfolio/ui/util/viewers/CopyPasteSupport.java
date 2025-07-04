package name.abuchen.portfolio.ui.util.viewers;

import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.util.TextUtil;

public class CopyPasteSupport
{
    private CopyPasteSupport()
    {
    }

    public static void enableFor(TableViewer viewer)
    {
        // apparently one cannot work with ColumnViewer directly because a) it
        // provides no access to the column count, b) no information about the
        // display order of columns, and c) no access to the actual text of the
        // cell (as set by multiple OwnerDrawLabelProvider) --> have one
        // implementation for Table and one for Tree

        Table table = viewer.getTable();

        table.addKeyListener(KeyListener.keyPressedAdapter(e -> {
            if (e.stateMask == SWT.MOD1 && e.keyCode == 'c' && table.getSelectionCount() > 0)
            {
                copyRowsToClipboard(viewer, table);
            }
        }));

        table.addMouseListener(MouseListener.mouseDoubleClickAdapter(e -> {
            if (e.stateMask == SWT.MOD3)
            {
                copyCellToClipboard(viewer, e);
            }
        }));
    }

    private static void copyRowsToClipboard(TableViewer viewer, Table table)
    {
        int columnCount = table.getColumnCount();
        int[] columnOrder = table.getColumnOrder();

        SharesLabelProvider[] labelProvider = getSharesLabelProvider(viewer, columnCount);

        StringBuilder result = new StringBuilder();

        TableItem[] rowItems = table.getSelection();
        for (int row = 0; row < rowItems.length; row++)
        {
            if (row > 0)
                result.append("\n"); //$NON-NLS-1$

            TableItem rowItem = rowItems[row];
            for (int column = 0; column < columnCount; column++)
            {
                if (column > 0)
                    result.append(SWT.TAB);

                int orderedColumn = columnOrder[column];

                if (labelProvider[orderedColumn] != null)
                {
                    Long value = labelProvider[orderedColumn].getValue(rowItem.getData());
                    result.append(value != null ? TextUtil.sanitizeFormattedNumber(Values.Share.format(value)) : ""); //$NON-NLS-1$
                }
                else
                {
                    result.append(TextUtil.sanitizeFormattedNumber(rowItem.getText(orderedColumn)));
                }
            }
        }

        Clipboard clipboard = new Clipboard(Display.getDefault());
        clipboard.setContents(new Object[] { result.toString() }, new Transfer[] { TextTransfer.getInstance() });
        clipboard.dispose();
    }

    public static void enableFor(TreeViewer viewer)
    {
        // apparently one cannot work with ColumnViewer directly because a) it
        // provides no access to the column count, b) no information about the
        // display order of columns, and c) no access to the actual text of the
        // cell (as set by multiple OwnerDrawLabelProvider) --> have one
        // implementation for Table and one for Tree

        Tree tree = viewer.getTree();

        tree.addKeyListener(KeyListener.keyPressedAdapter(e -> {
            if (e.stateMask == SWT.MOD1 && e.keyCode == 'c' && tree.getSelectionCount() > 0)
            {
                copyRowsToClipboard(viewer, tree);
            }
        }));

        tree.addMouseListener(MouseListener.mouseDoubleClickAdapter(e -> {
            if (e.stateMask == SWT.MOD3)
            {
                copyCellToClipboard(viewer, e);
            }
        }));
    }

    private static void copyRowsToClipboard(TreeViewer viewer, Tree tree)
    {
        int columnCount = tree.getColumnCount();
        int[] columnOrder = tree.getColumnOrder();

        SharesLabelProvider[] labelProvider = getSharesLabelProvider(viewer, columnCount);

        StringBuilder result = new StringBuilder();

        TreeItem[] rowItems = tree.getSelection();
        for (int row = 0; row < rowItems.length; row++)
        {
            if (row > 0)
                result.append("\n"); //$NON-NLS-1$

            TreeItem rowItem = rowItems[row];
            for (int column = 0; column < columnCount; column++)
            {
                if (column > 0)
                    result.append(SWT.TAB);

                int orderedColumn = columnOrder[column];

                if (labelProvider[orderedColumn] != null)
                {
                    Long value = labelProvider[orderedColumn].getValue(rowItem.getData());
                    result.append(value != null ? TextUtil.sanitizeFormattedNumber(Values.Share.format(value)) : ""); //$NON-NLS-1$
                }
                else
                {
                    result.append(TextUtil.sanitizeFormattedNumber(rowItem.getText(orderedColumn)));
                }
            }
        }

        Clipboard clipboard = new Clipboard(Display.getDefault());
        clipboard.setContents(new Object[] { result.toString() }, new Transfer[] { TextTransfer.getInstance() });
        clipboard.dispose();
    }

    private static void copyCellToClipboard(ColumnViewer viewer, MouseEvent event)
    {
        // get the clicked cell
        var cell = viewer.getCell(new Point(event.x, event.y));
        if (cell == null)
            return;

        String text;

        var labelProvider = viewer.getLabelProvider(cell.getColumnIndex());
        if (labelProvider instanceof SharesLabelProvider sharesLabelProvider)
        {
            Long value = sharesLabelProvider.getValue(cell.getElement());
            text = value != null ? Values.Share.format(value) : ""; //$NON-NLS-1$
        }
        else
        {
            text = cell.getText();
        }

        // remove grouping separators such as non-breaking spaces. We do not
        // know if we actually have a number here - as we are working only with
        // the formatted text - therefore we keep the spaces.
        text = TextUtil.sanitizeFormattedNumber(text);

        if (text != null && !text.isEmpty())
        {
            var clipboard = new Clipboard(Display.getCurrent());
            clipboard.setContents(new Object[] { text }, new Transfer[] { TextTransfer.getInstance() });
            clipboard.dispose();
        }
    }

    private static SharesLabelProvider[] getSharesLabelProvider(ColumnViewer viewer, int columnCount)
    {
        // check for "special" label provider
        SharesLabelProvider[] labelProvider = new SharesLabelProvider[columnCount];
        for (int ii = 0; ii < columnCount; ii++)
        {
            CellLabelProvider p = viewer.getLabelProvider(ii);
            if (p instanceof SharesLabelProvider sharesLabelProvider)
                labelProvider[ii] = sharesLabelProvider;
        }
        return labelProvider;
    }
}
