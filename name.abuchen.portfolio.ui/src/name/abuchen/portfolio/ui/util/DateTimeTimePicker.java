package name.abuchen.portfolio.ui.util;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Date;

import org.eclipse.nebula.widgets.cdatetime.CDateTime;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DateTime;

public class DateTimeTimePicker extends DateTimePicker<LocalTime>
{
    public DateTimeTimePicker(Composite parent)
    {
        super(parent);
    }
    
    @Override
    protected DateTime newDateTimeControl(Composite parent)
    {
        return new DateTime(parent, SWT.TIME | SWT.DROP_DOWN | SWT.BORDER);
    }

    public LocalTime getSelection()
    {
        if (control instanceof CDateTime)
        {
            Date d = ((CDateTime) control).getSelection();
            return LocalDateTime.ofInstant(d.toInstant(), ZoneId.systemDefault()).toLocalTime();
        }
        else
        {
            DateTime dateTime = (DateTime) control;
            return LocalTime.of(dateTime.getHours(), dateTime.getMinutes(), dateTime.getSeconds());
        }
    }
}
