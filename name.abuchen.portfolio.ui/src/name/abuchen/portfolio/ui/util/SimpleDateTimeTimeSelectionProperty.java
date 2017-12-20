package name.abuchen.portfolio.ui.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Date;

import org.eclipse.jface.databinding.swt.WidgetValueProperty;
import org.eclipse.nebula.widgets.cdatetime.CDateTime;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.DateTime;

public class SimpleDateTimeTimeSelectionProperty extends WidgetValueProperty
{
    public SimpleDateTimeTimeSelectionProperty()
    {
        super(SWT.Selection);
    }

    public Object getValueType()
    {
        return LocalTime.class;
    }

    @Override
    protected Object doGetValue(Object source)
    {
        if (source instanceof DateTime)
        {
            DateTime dateTime = (DateTime) source;

            // DateTime widget has zero-based months
            return LocalTime.of(dateTime.getHours(), dateTime.getMinutes(), dateTime.getSeconds());
        }
        else if (source instanceof CDateTime)
        {
            Date date = ((CDateTime) source).getSelection();
            return date == null ? null
                            : LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault()).toLocalTime();
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
            LocalTime date = (LocalTime) value;
            DateTime dateTime = (DateTime) source;
            // DateTime widget has zero-based months
            dateTime.setTime(date.getHour(), date.getMinute(), date.getSecond());
        }
        else if (source instanceof CDateTime)
        {
            LocalTime date = (LocalTime) value;
            CDateTime dateTime = (CDateTime) source;
            dateTime.setSelection(Date.from(date.atDate(LocalDate.now()).atZone(ZoneId.systemDefault()).toInstant()));
        }
        else
        {
            throw new UnsupportedOperationException();
        }
    }
}
