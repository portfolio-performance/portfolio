package name.abuchen.portfolio.ui.util.viewers;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CheckboxCellEditor;
import org.eclipse.swt.widgets.Composite;

public class BooleanEditingSupport extends PropertyEditingSupport
{

    public BooleanEditingSupport(Class<?> subjectType, String attributeName)
    {
        super(subjectType, attributeName);
    }

    @Override
    public CellEditor createEditor(Composite composite)
    {
        return new CheckboxCellEditor(composite);
    }

    @Override
    public Object getValue(Object element) throws Exception
    {
        Boolean v = (Boolean) descriptor().getReadMethod().invoke(adapt(element));
        return v != null ? v : ""; //$NON-NLS-1$
    }

    @Override
    public void setValue(Object element, Object value) throws Exception
    {
        Object subject = adapt(element);

        Boolean newValue = (Boolean) value;
        Boolean oldValue = (Boolean) descriptor().getReadMethod().invoke(subject);

        if (!value.equals(oldValue))
        {
            descriptor().getWriteMethod().invoke(subject, newValue);
            notify(element, newValue, oldValue);
        }
    }
}
