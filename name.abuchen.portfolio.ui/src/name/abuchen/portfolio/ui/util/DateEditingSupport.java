package name.abuchen.portfolio.ui.util;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

public class DateEditingSupport extends PropertyEditingSupport
{
    public static class DateCharacterVerifyListener implements VerifyListener
    {
        private String allowedChars = "-0123456789"; //$NON-NLS-1$

        public void verifyText(VerifyEvent e)
        {
            for (int ii = 0; e.doit && ii < e.text.length(); ii++)
                e.doit = allowedChars.indexOf(e.text.charAt(0)) >= 0;
        }
    }

    public DateEditingSupport(Class<?> subjectType, String attributeName)
    {
        super(subjectType, attributeName);

        if (!LocalDate.class.isAssignableFrom(descriptor().getPropertyType()))
            throw new RuntimeException(String.format("Property %s needs to be of type date", attributeName)); //$NON-NLS-1$
    }

    @Override
    public CellEditor createEditor(Composite composite)
    {
        TextCellEditor textEditor = new TextCellEditor(composite);
        ((Text) textEditor.getControl()).setTextLimit(10);
        ((Text) textEditor.getControl()).addVerifyListener(new DateCharacterVerifyListener());
        return textEditor;
    }

    @Override
    public final Object getValue(Object element) throws Exception
    {
        return ((LocalDate) descriptor().getReadMethod().invoke(adapt(element))).toString();
    }

    @Override
    public final void setValue(Object element, Object value) throws Exception
    {
        Object subject = adapt(element);
        LocalDate newValue = null;

        try
        {
            newValue = LocalDate.parse(String.valueOf(value));
        }
        catch (DateTimeParseException e)
        {
            newValue = LocalDate.now();
        }

        LocalDate oldValue = (LocalDate) descriptor().getReadMethod().invoke(subject);

        if (!newValue.equals(oldValue))
        {
            descriptor().getWriteMethod().invoke(subject, newValue);
            notify(element, newValue, oldValue);
        }
    }
}
