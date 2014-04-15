package name.abuchen.portfolio.ui.util;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

public class ListEditingSupport extends PropertyEditingSupport
{
    private List<Object> comboBoxItems;

    public ListEditingSupport(Class<?> subjectType, String attributeName, List<?> options)
    {
        super(subjectType, attributeName);

        this.comboBoxItems = new ArrayList<Object>(options);
    }

    @Override
    public CellEditor createEditor(Composite composite)
    {
        String[] names = new String[comboBoxItems.size()];
        int index = 0;
        for (Object item : comboBoxItems)
            names[index++] = item == null ? "" : item.toString(); //$NON-NLS-1$

        return new ComboBoxCellEditor(composite, names, SWT.READ_ONLY);
    }

    @Override
    public Object getValue(Object element) throws Exception
    {
        Object property = descriptor().getReadMethod().invoke(element);

        for (int ii = 0; ii < comboBoxItems.size(); ii++)
        {
            Object item = comboBoxItems.get(ii);
            if (item != null && item.equals(property))
                return ii;
            else if (item == null && property == null)
                return ii;
        }

        return 0;
    }

    @Override
    public void setValue(Object element, Object value) throws Exception
    {
        Object newValue = comboBoxItems.get((Integer) value);
        Object oldValue = descriptor().getReadMethod().invoke(element);

        if (!newValue.equals(oldValue))
        {
            descriptor().getWriteMethod().invoke(element, newValue);
            notify(element, newValue, oldValue);
        }
    }
}
