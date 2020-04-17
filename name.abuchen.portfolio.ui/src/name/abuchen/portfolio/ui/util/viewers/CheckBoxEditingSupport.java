package name.abuchen.portfolio.ui.util.viewers;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CheckboxCellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

public class CheckBoxEditingSupport extends PropertyEditingSupport
{

    public CheckBoxEditingSupport(Class<?> subjectType, String attributeName)
    {
        super(subjectType, attributeName);
    }

    @Override
    public final Object getValue(Object element) throws Exception
    {
        return (boolean) descriptor().getReadMethod().invoke(adapt(element));
    }

    @Override
    public void setValue(Object element, Object value) throws Exception
    {
        Object subject = adapt(element);

        boolean oldValue = (boolean) descriptor().getReadMethod().invoke(subject);

        descriptor().getWriteMethod().invoke(subject, !oldValue);
        notify(element, !oldValue, oldValue);
    }

    @Override
    public CellEditor createEditor(Composite composite)
    {
        return new CheckboxCellEditor(composite, SWT.CHECK);
    }

    @Override
    public boolean canEdit(Object o)
    {
        return true;
    }

}
