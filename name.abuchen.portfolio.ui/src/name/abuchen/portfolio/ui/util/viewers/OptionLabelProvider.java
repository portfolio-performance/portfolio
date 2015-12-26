package name.abuchen.portfolio.ui.util.viewers;

import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Table;

public class OptionLabelProvider extends CellLabelProvider
{
    public String getText(Object element, Integer option)
    {
        return null;
    }

    public Color getForeground(Object element, Integer option)
    {
        return null;
    }

    @Override
    public void update(ViewerCell cell)
    {
        Table table = (Table) cell.getControl();
        int columnIndex = cell.getColumnIndex();
        Integer option = (Integer) table.getColumn(columnIndex).getData(ShowHideColumnHelper.OPTIONS_KEY);

        Object element = cell.getElement();
        cell.setText(getText(element, option));
        cell.setForeground(getForeground(element, option));
    }
}