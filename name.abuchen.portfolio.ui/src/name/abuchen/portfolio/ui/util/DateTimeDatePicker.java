package name.abuchen.portfolio.ui.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

import org.eclipse.nebula.widgets.cdatetime.CDateTime;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DateTime;

public class DateTimeDatePicker extends DateTimePicker<LocalDate>
{
    public DateTimeDatePicker(Composite parent)
    {
        super(parent);
       
    }
    
    @Override
    protected DateTime newDateTimeControl(Composite parent)
    {
        return new DateTime(parent, SWT.DATE | SWT.DROP_DOWN | SWT.BORDER);
    }

    public LocalDate getSelection()
    {
        if (control instanceof CDateTime)
        {
            Date d = ((CDateTime) control).getSelection();
            return LocalDateTime.ofInstant(d.toInstant(), ZoneId.systemDefault()).toLocalDate();
        }
        else
        {
            DateTime dateTime = (DateTime) control;
            // DateTime widget has zero-based months
            return LocalDate.of(dateTime.getYear(), dateTime.getMonth() + 1, dateTime.getDay());
        }
    }
}
