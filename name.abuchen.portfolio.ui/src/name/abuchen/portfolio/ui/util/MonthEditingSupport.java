package name.abuchen.portfolio.ui.util;

import java.text.DateFormatSymbols;
import java.util.Calendar;

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

        // determine number of months (some calendars have 13)
        int numMonths = Calendar.getInstance().getActualMaximum(Calendar.MONTH) + 1;
        this.options = new String[numMonths];
        System.arraycopy(new DateFormatSymbols().getMonths(), 0, this.options, 0, numMonths);
    }

    @Override
    public CellEditor createEditor(Composite composite)
    {
        return new ComboBoxCellEditor(composite, options, SWT.READ_ONLY);
    }

    @Override
    public Object getValue(Object element) throws Exception
    {
        return descriptor().getReadMethod().invoke(element);
    }

    @Override
    public void setValue(Object element, Object value) throws Exception
    {
        Integer newValue = (Integer) value;
        Integer oldValue = (Integer) descriptor().getReadMethod().invoke(element);

        if (!newValue.equals(oldValue))
        {
            descriptor().getWriteMethod().invoke(element, newValue);
            notify(element, newValue, oldValue);
        }
    }
}
