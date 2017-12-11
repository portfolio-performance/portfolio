package name.abuchen.portfolio.ui.util;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

import org.eclipse.core.runtime.Platform;
import org.eclipse.nebula.widgets.cdatetime.CDT;
import org.eclipse.nebula.widgets.cdatetime.CDateTime;
import org.eclipse.nebula.widgets.cdatetime.CDateTimeBuilder;
import org.eclipse.nebula.widgets.cdatetime.Footer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DateTime;

/**
 * Simple wrapper around either DateTime or CDateTime. On Linux, DateTime does
 * not work very well because of the missing support for localization. On Mac OS
 * X, CDateTime runs sluggish and does not render the drop down box correctly.
 * And on Windows, I prefer the DateTime widget. Keep in mind that the behavior
 * slightly differs because CDateTime allows a null selection.
 */
public abstract class DateTimePicker<T>
{
    protected Control control;

    public DateTimePicker(Composite parent)
    {
        boolean isLinux = Platform.OS_LINUX.equals(Platform.getOS());

        if (isLinux)
        {
            CDateTime boxDate = new CDateTime(parent, CDT.BORDER | CDT.DROP_DOWN);
            boxDate.setFormat(CDT.DATE_MEDIUM);

            CDateTimeBuilder builder = CDateTimeBuilder.getStandard();
            builder.setFooter(Footer.Today());
            boxDate.setBuilder(builder);

            this.control = boxDate;
        }
        else
        {
            this.control = newDateTimeControl(parent);
        }
    }

    protected abstract DateTime newDateTimeControl(Composite parent);

    public Control getControl()
    {
        return control;
    }

    public void setSelection(LocalDate date)
    {
        if (control instanceof CDateTime)
        {
            Date d = Date.from(date.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
            ((CDateTime) control).setSelection(d);
        }
        else
        {
            // DateTime widget has zero-based months
            ((DateTime) control).setDate(date.getYear(), date.getMonthValue() - 1, date.getDayOfMonth());
        }
    }

    public abstract T getSelection();

    public void setLayoutData(Object layoutData)
    {
        control.setLayoutData(layoutData);
    }
}
