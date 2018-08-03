package name.abuchen.portfolio.ui.util.viewers;

import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Table;

public class OptionLabelProvider<O> extends CellLabelProvider
{
    public String getText(Object element, O option) // NOSONAR
    {
        return null;
    }

    public Color getForeground(Object element, O option) // NOSONAR
    {
        return null;
    }

    public Image getImage(Object element, O option) // NOSONAR
    {
        return null;
    }
    
    public Font getFont(Object element, O option) // NOSONAR
    {
        return null;
    }

    @Override
    public void update(ViewerCell cell)
    {
        Table table = (Table) cell.getControl();
        int columnIndex = cell.getColumnIndex();
        @SuppressWarnings("unchecked")
        O option = (O) table.getColumn(columnIndex).getData(ShowHideColumnHelper.OPTIONS_KEY);

        Object element = cell.getElement();
        cell.setText(getText(element, option));
        cell.setForeground(getForeground(element, option));
        cell.setImage(getImage(element, option));
        cell.setFont(getFont(element, option));
    }
}