package name.abuchen.portfolio.ui.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

import org.eclipse.nebula.widgets.cdatetime.CDateTime;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DateTime;

public class NullableDateTimeDateSelectionProperty extends SimpleDateTimeDateSelectionProperty
{
    @Override
    protected LocalDate doGetValue(Control source)
    {
        if (source instanceof CDateTime dateTime)
        {
            var date = dateTime.getSelection();
            return date == null ? null
                            : LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault()).toLocalDate();
        }

        return super.doGetValue(source);
    }

    @Override
    protected void doSetValue(Control source, LocalDate date)
    {
        if (source instanceof DateTime dateTime)
        {
            if (date == null)
            {
                // DateTime does not support null
                var now = LocalDate.now();
                dateTime.setDate(now.getYear(), now.getMonthValue() - 1, now.getDayOfMonth());
            }
            else
            {
                // DateTime widget has zero-based months
                dateTime.setDate(date.getYear(), date.getMonthValue() - 1, date.getDayOfMonth());
            }
        }
        else if (source instanceof CDateTime dateTime)
        {
            dateTime.setSelection(
                            date != null ? Date.from(date.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant())
                                            : null);
        }
        else
        {
            throw new UnsupportedOperationException();
        }
    }
}
