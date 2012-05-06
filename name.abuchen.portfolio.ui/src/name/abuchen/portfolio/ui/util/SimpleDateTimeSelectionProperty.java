package name.abuchen.portfolio.ui.util;

import java.util.Calendar;
import java.util.Date;

import org.eclipse.jface.databinding.swt.WidgetValueProperty;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.DateTime;

/* package */class SimpleDateTimeSelectionProperty extends WidgetValueProperty
{
    private static final ThreadLocal<Calendar> calendar = new ThreadLocal<Calendar>()
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
        DateTime dateTime = (DateTime) source;
        Calendar cal = calendar.get();

        cal.clear();
        cal.set(dateTime.getYear(), dateTime.getMonth(), dateTime.getDay());
        return cal.getTime();
    }

    @Override
    protected void doSetValue(Object source, Object value)
    {
        DateTime dateTime = (DateTime) source;
        Calendar cal = calendar.get();

        cal.setTime((Date) value);
        dateTime.setYear(cal.get(Calendar.YEAR));
        dateTime.setMonth(cal.get(Calendar.MONTH));
        dateTime.setDay(cal.get(Calendar.DAY_OF_MONTH));
    }
}
