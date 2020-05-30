package name.abuchen.portfolio.ui.util.viewers;

import java.time.Month;
import java.time.format.TextStyle;
import java.util.Locale;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

public class MonthEditingSupport extends PropertyEditingSupport
{
    private String[] options;

    public MonthEditingSupport(Class<?> subjectType, String attributeName)
    {
        super(subjectType, attributeName);

        Class<?> propertyType = descriptor().getPropertyType();
        if (!int.class.isAssignableFrom(propertyType))
            throw new UnsupportedOperationException(String.format(
                            "Property %s needs to be of type int to serve as month", attributeName)); //$NON-NLS-1$

        Month[] months = Month.values();
        options = new String[months.length];
        for (int ii = 0; ii < months.length; ii++)
            options[ii] = months[ii].getDisplayName(TextStyle.FULL, Locale.getDefault());
    }

    @Override
    public CellEditor createEditor(Composite composite)
    {
        return new ComboBoxCellEditor(composite, options, SWT.READ_ONLY);
    }

    @Override
    public final Object getValue(Object element) throws Exception
    {
        // month starts with 1 (January), but list index is 0
        return (Integer) descriptor().getReadMethod().invoke(adapt(element)) - 1;
    }

    @Override
    public final void setValue(Object element, Object value) throws Exception
    {
        Object subject = adapt(element);

        Integer newValue = (Integer) value + 1;
        Integer oldValue = (Integer) descriptor().getReadMethod().invoke(subject);

        if (!newValue.equals(oldValue))
        {
            descriptor().getWriteMethod().invoke(subject, newValue);
            notify(element, newValue, oldValue);
        }
    }
}
