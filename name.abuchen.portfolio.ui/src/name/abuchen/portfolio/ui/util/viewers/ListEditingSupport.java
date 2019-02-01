package name.abuchen.portfolio.ui.util.viewers;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

/**
 * Creates a cell editor with a combo box. Options must be a list of non-null
 * values. By default, the user must choose one option. Override the method
 * {@link #canBeNull} in order to add an additional empty element
 */
public class ListEditingSupport extends PropertyEditingSupport
{
    private ComboBoxCellEditor editor;
    private List<Object> comboBoxItems;

    public ListEditingSupport(Class<?> subjectType, String attributeName, List<?> options)
    {
        super(subjectType, attributeName);

        for (Object option : options)
            if (option == null)
                throw new IllegalArgumentException();

        this.comboBoxItems = new ArrayList<>(options);
    }

    public boolean canBeNull(Object element) // NOSONAR
    {
        return false;
    }

    @Override
    public final CellEditor createEditor(Composite composite)
    {
        editor = new ComboBoxCellEditor(composite, new String[0], SWT.READ_ONLY);
        return editor;
    }

    @Override
    public final void prepareEditor(Object element)
    {
        boolean canBeNull = canBeNull(element);

        if (canBeNull)
        {
            if (comboBoxItems.isEmpty() || comboBoxItems.get(0) != null)
                comboBoxItems.add(0, null);
        }
        else
        {
            if (!comboBoxItems.isEmpty() && comboBoxItems.get(0) == null)
                comboBoxItems.remove(0);
        }

        String[] names = new String[comboBoxItems.size()];
        int index = 0;

        for (Object item : comboBoxItems)
            names[index++] = item == null ? "" : item.toString(); //$NON-NLS-1$

        editor.setItems(names);
    }

    @Override
    public final Object getValue(Object element) throws Exception
    {
        Object property = descriptor().getReadMethod().invoke(adapt(element));

        for (int ii = 0; ii < comboBoxItems.size(); ii++)
        {
            Object item = comboBoxItems.get(ii);
            if (item != null && item.equals(property))
                return ii;
            else if (item == null && property == null)
                return ii; // NOSONAR
        }

        return 0;
    }

    @Override
    public final void setValue(Object element, Object value) throws Exception
    {
        Object subject = adapt(element);

        int index = (Integer) value;
        if (index < 0)
            return;

        Object newValue = comboBoxItems.get(index);
        Object oldValue = descriptor().getReadMethod().invoke(subject);

        if ((newValue != null && !newValue.equals(oldValue)) || (newValue == null && oldValue != null))
        {
            descriptor().getWriteMethod().invoke(subject, newValue);
            notify(element, newValue, oldValue);
        }
    }
}
