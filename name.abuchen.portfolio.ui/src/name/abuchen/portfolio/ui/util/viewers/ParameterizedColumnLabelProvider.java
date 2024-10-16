package name.abuchen.portfolio.ui.util.viewers;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import name.abuchen.portfolio.PortfolioLog;

/**
 * ColumnLabelProvider subclass which allows to access "option value" associated
 * with a column. This can be e.g. reporting period (and there can be multiple
 * instance of the same base column with different reporting periods set).
 */
public class ParameterizedColumnLabelProvider extends ColumnLabelProvider implements Listener
{
    private TableColumn tableColumn;

    public TableColumn getTableColumn()
    {
        return this.tableColumn;
    }

    public void setTableColumn(TableColumn tableColumn)
    {
        if (this.tableColumn != null)
            throw new IllegalStateException(
                            "ParameterizedColumnLabelProvider cannot be reused across multiple columns. Use Column#setLabelProvider(Supplier<CellLabelProvider> labelProvider) method."); //$NON-NLS-1$
        this.tableColumn = tableColumn;
    }

    public Object getOption()
    {
        return this.tableColumn.getData(ShowHideColumnHelper.OPTIONS_KEY);
    }

    // Owner draw interface

    /**
     * To enable owner-draw, call this method, passing table object.
     */
    public void enableOwnerDraw(Table table)
    {
        table.addListener(SWT.MeasureItem, this);
        table.addListener(SWT.EraseItem, this);
        table.addListener(SWT.PaintItem, this);
    }

    /**
     * Measure size of contents - optional to override.
     */
    public void measure(Event event)
    {
    }

    /**
     * Actually paint contents via event.gc, etc. Column is passed
     * to differentiate multiple columns with owner-draw contents.
     */
    public void paint(Event event)
    {
        PortfolioLog.error("paint() not implemented"); //$NON-NLS-1$
    }

    @Override
    public void handleEvent(Event event)
    {
        TableItem item = (TableItem) event.item;
        Table table = item.getParent();
        TableColumn tColumn = table.getColumn(event.index);
        if (tColumn != this.tableColumn)
            return;

        switch (event.type)
        {
            case SWT.MeasureItem:
                this.measure(event);
                break;
            case SWT.PaintItem:
                this.paint(event);
                break;
            case SWT.EraseItem:
                // We're saying that we'll draw actual things ("foreground")
                // ourselves. Things like background, selection, etc. will
                // still be handled by SWT.
                event.detail &= ~SWT.FOREGROUND;
                break;
            default:
        }
    }
}
