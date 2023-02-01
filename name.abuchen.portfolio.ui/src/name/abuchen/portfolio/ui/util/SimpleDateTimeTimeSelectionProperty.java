package name.abuchen.portfolio.ui.util;

import java.time.LocalTime;
import java.util.Calendar;
import java.util.Date;

import org.eclipse.jface.databinding.swt.WidgetValueProperty;
import org.eclipse.nebula.widgets.cdatetime.CDateTime;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DateTime;

public class SimpleDateTimeTimeSelectionProperty extends WidgetValueProperty<Control, LocalTime>
{
    public SimpleDateTimeTimeSelectionProperty()
    {
        super(SWT.Selection);
    }

    @Override
    public Object getValueType()
    {
        return LocalTime.class;
    }

    @Override
    protected LocalTime doGetValue(Control source)
    {
        if (source instanceof DateTime)
        {
            DateTime dateTime = (DateTime) source;

            // DateTime widget has zero-based months
            return LocalTime.of(dateTime.getHours(), dateTime.getMinutes(), dateTime.getSeconds());
        }
        else if (source instanceof CDateTime)
        {
            CDateTime dateTime = (CDateTime) source;

            Date date = dateTime.getSelection();

            if (date == null)
            {
                doSetValue(source, LocalTime.MIDNIGHT);
                return LocalTime.MIDNIGHT;
            }
            else
            {
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(date);
                return LocalTime.of(calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE));
            }
        }
        else
        {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    protected void doSetValue(Control source, LocalTime date)
    {
        if (source instanceof DateTime)
        {
            DateTime dateTime = (DateTime) source;
            dateTime.setTime(date.getHour(), date.getMinute(), date.getSecond());
        }
        else if (source instanceof CDateTime)
        {
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.HOUR_OF_DAY, date.getHour());
            calendar.set(Calendar.MINUTE, date.getMinute());
            calendar.set(Calendar.SECOND, 0);

            CDateTime dateTime = (CDateTime) source;
            dateTime.setSelection(calendar.getTime());
        }
        else
        {
            throw new UnsupportedOperationException();
        }
    }
}
