package name.abuchen.portfolio.ui.util.viewers;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

/**
 * Creates a cell editor with a combo box. Options must be a list of non-null
 * values. By default, the user must choose one option.
 */
public class ListEditingSupport extends PropertyEditingSupport
{
    private ComboBoxCellEditor editor;
    private List<Object> comboBoxItems;
    private List<String> comboItemsNames;

    public ListEditingSupport(Class<?> subjectType, String attributeName, List<?> options, List<String> comboItemsNames)
    {
        super(subjectType, attributeName);

        for (Object option : options)
            if (option == null)
                throw new IllegalArgumentException("option must not be null"); //$NON-NLS-1$

        this.comboBoxItems = new ArrayList<>(options);
        this.comboItemsNames = new ArrayList<>(comboItemsNames);

        if (this.comboBoxItems.size() != this.comboItemsNames.size())
            throw new IllegalArgumentException("arrays size do not match"); //$NON-NLS-1$
    }

    public ListEditingSupport(Class<?> subjectType, String attributeName, List<?> options)
    {
        this(subjectType, attributeName, options, options.stream().map(Object::toString).toList());
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
        editor.setItems(comboItemsNames.toArray(new String[0]));
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
