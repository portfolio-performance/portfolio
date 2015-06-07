package name.abuchen.portfolio.ui.util;

import java.util.Calendar;
import java.util.Date;

import name.abuchen.portfolio.util.Dates;

import org.eclipse.core.runtime.Platform;
import org.eclipse.nebula.widgets.cdatetime.CDT;
import org.eclipse.nebula.widgets.cdatetime.CDateTime;
import org.eclipse.nebula.widgets.cdatetime.CDateTimeBuilder;
import org.eclipse.nebula.widgets.cdatetime.Footer;
import org.eclipse.swt.SWT;
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
public class DateTimePicker
{
    private Control control;

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
            this.control = new DateTime(parent, SWT.DATE | SWT.DROP_DOWN | SWT.BORDER);
        }
    }

    public Control getControl()
    {
        return control;
    }

    public void setSelection(Date date)
    {
        if (control instanceof CDateTime)
        {
            ((CDateTime) control).setSelection(date);
        }
        else
        {
            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            ((DateTime) control).setDate(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH),
                            cal.get(Calendar.DAY_OF_MONTH));
        }
    }

    public Date getSelection()
    {
        if (control instanceof CDateTime)
        {
            return ((CDateTime) control).getSelection();
        }
        else
        {
            DateTime dateTime = (DateTime) control;
            return Dates.date(dateTime.getYear(), dateTime.getMonth(), dateTime.getDay());
        }
    }

    public void setLayoutData(Object layoutData)
    {
        control.setLayoutData(layoutData);
    }
}
