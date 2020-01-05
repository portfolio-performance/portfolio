package name.abuchen.portfolio.ui.util;

import java.util.Calendar;
import java.util.Date;

import org.eclipse.jface.databinding.swt.WidgetValueProperty;
import org.eclipse.nebula.widgets.cdatetime.CDateTime;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.DateTime;

public class SimpleDateTimeSelectionProperty extends WidgetValueProperty
{
    private static final ThreadLocal<Calendar> CALENDAR = new ThreadLocal<Calendar>()
    {
        @Override
        protected Calendar initialValue()
        {
            return Calendar.getInstance();
        };
    };

    public SimpleDateTimeSelectionProperty()
    {
        super(SWT.Selection);
    }

    public Object getValueType()
    {
        return Date.class;
    }

    @Override
    protected Object doGetValue(Object source)
    {
        if (source instanceof DateTime)
        {
            DateTime dateTime = (DateTime) source;
            Calendar cal = CALENDAR.get();

            cal.clear();
            cal.set(dateTime.getYear(), dateTime.getMonth(), dateTime.getDay());
            return cal.getTime();
        }
        else if (source instanceof CDateTime)
        {
            return ((CDateTime) source).getSelection();
        }
        else
        {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    protected void doSetValue(Object source, Object value)
    {
        if (source instanceof DateTime)
        {
            DateTime dateTime = (DateTime) source;
            Calendar cal = CALENDAR.get();

            cal.setTime((Date) value);
            dateTime.setYear(cal.get(Calendar.YEAR));
            dateTime.setMonth(cal.get(Calendar.MONTH));
            dateTime.setDay(cal.get(Calendar.DAY_OF_MONTH));
        }
        else if (source instanceof CDateTime)
        {
            ((CDateTime) source).setSelection((Date) value);
        }
        else
        {
            throw new UnsupportedOperationException();
        }
    }
}
