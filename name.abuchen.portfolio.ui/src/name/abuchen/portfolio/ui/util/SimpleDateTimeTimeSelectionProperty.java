package name.abuchen.portfolio.ui.util;

import java.time.LocalTime;

import org.eclipse.jface.databinding.swt.WidgetValueProperty;
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
            dateTime.setTime(date.getHour(), date.getMinute(), date.getSecond());
        }
        else
        {
            throw new UnsupportedOperationException();
        }
    }
}
