package name.abuchen.portfolio.ui.util.viewers;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.swt.widgets.TableColumn;

/**
 * ColumnLabelProvider subclass which allows to access "option value" associated
 * with a column. This can be e.g. reporting period (and there can be multiple
 * instance of the same base column with different reporting periods set).
 */
public class ParameterizedColumnLabelProvider extends ColumnLabelProvider
{
    private TableColumn tableColumn;

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
}
