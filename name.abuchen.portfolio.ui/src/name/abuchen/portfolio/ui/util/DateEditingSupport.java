package name.abuchen.portfolio.ui.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import name.abuchen.portfolio.util.Dates;

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

    private SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd"); //$NON-NLS-1$

    public DateEditingSupport(Class<?> subjectType, String attributeName)
    {
        super(subjectType, attributeName);

        if (!Date.class.isAssignableFrom(descriptor().getPropertyType()))
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
        Date v = (Date) descriptor().getReadMethod().invoke(adapt(element));
        return format.format(v);
    }

    @Override
    public final void setValue(Object element, Object value) throws Exception
    {
        Object subject = adapt(element);
        Date newValue = null;

        try
        {
            newValue = format.parse(String.valueOf(value));
            Calendar cal = Calendar.getInstance();
            cal.setTime(newValue);
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            newValue = cal.getTime();
        }
        catch (ParseException e)
        {
            newValue = Dates.today();
        }

        Date oldValue = (Date) descriptor().getReadMethod().invoke(subject);

        if (!newValue.equals(oldValue))
        {
            descriptor().getWriteMethod().invoke(subject, newValue);
            notify(element, newValue, oldValue);
        }
    }
}
