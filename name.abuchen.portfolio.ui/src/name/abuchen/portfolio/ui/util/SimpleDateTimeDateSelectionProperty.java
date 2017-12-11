package name.abuchen.portfolio.ui.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

import org.eclipse.jface.databinding.swt.WidgetValueProperty;
import org.eclipse.nebula.widgets.cdatetime.CDateTime;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.DateTime;

public class SimpleDateTimeDateSelectionProperty extends WidgetValueProperty
{
    public SimpleDateTimeDateSelectionProperty()
    {
        super(SWT.Selection);
    }

    public Object getValueType()
    {
        return LocalDate.class;
    }

    @Override
    protected Object doGetValue(Object source)
    {
        if (source instanceof DateTime)
        {
            DateTime dateTime = (DateTime) source;

            // DateTime widget has zero-based months
            return LocalDate.of(dateTime.getYear(), dateTime.getMonth() + 1, dateTime.getDay());
        }
        else if (source instanceof CDateTime)
        {
            Date date = ((CDateTime) source).getSelection();
            return date == null ? null
                            : LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault()).toLocalDate();
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
            LocalDate date = (LocalDate) value;
            DateTime dateTime = (DateTime) source;
            // DateTime widget has zero-based months
            dateTime.setDate(date.getYear(), date.getMonthValue() - 1, date.getDayOfMonth());
        }
        else if (source instanceof CDateTime)
        {
            LocalDate date = (LocalDate) value;
            CDateTime dateTime = (CDateTime) source;
            dateTime.setSelection(Date.from(date.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant()));
        }
        else
        {
            throw new UnsupportedOperationException();
        }
    }
}
