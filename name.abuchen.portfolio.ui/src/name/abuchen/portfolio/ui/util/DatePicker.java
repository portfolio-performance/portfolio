package name.abuchen.portfolio.ui.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

import org.eclipse.core.runtime.Platform;
import org.eclipse.nebula.widgets.cdatetime.CDT;
import org.eclipse.nebula.widgets.cdatetime.CDateTime;
import org.eclipse.nebula.widgets.cdatetime.CDateTimeBuilder;
import org.eclipse.nebula.widgets.cdatetime.Footer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DateTime;

import name.abuchen.portfolio.ui.Images;

/**
 * Simple wrapper around either DateTime or CDateTime. On Linux, DateTime does
 * not work very well because of the missing support for localization. On Mac OS
 * X, CDateTime runs sluggish and does not render the drop down box correctly.
 * And on Windows, I prefer the DateTime widget. Keep in mind that the behavior
 * slightly differs because CDateTime allows a null selection.
 */
public class DatePicker
{
    protected Control control;

    public DatePicker(Composite parent)
    {
        this(parent, true);
    }

    /**
     * Creates a text-only date picker without the drop-down calendar button.
     * Use this variant if a calendar is already shown next to the field.
     */
    public static DatePicker withoutDropDown(Composite parent)
    {
        return new DatePicker(parent, false);
    }

    private DatePicker(Composite parent, boolean dropDown)
    {
        boolean isLinux = Platform.OS_LINUX.equals(Platform.getOS());

        if (isLinux)
        {
            int style = CDT.BORDER;
            if (dropDown)
                style |= CDT.DROP_DOWN;

            CDateTime boxDate = new CDateTime(parent, style);
            boxDate.setFormat(CDT.DATE_MEDIUM);

            if (dropDown)
            {
                boxDate.setButtonImage(Images.CALENDAR_OFF.image());

                CDateTimeBuilder builder = CDateTimeBuilder.getStandard();
                builder.setFooter(Footer.Today());
                boxDate.setBuilder(builder);
            }

            this.control = boxDate;
        }
        else
        {
            int style = SWT.DATE | SWT.BORDER;
            if (dropDown)
                style |= SWT.DROP_DOWN;

            this.control = new DateTime(parent, style);
        }
    }

    public Control getControl()
    {
        return control;
    }

    public void setSelection(LocalDate date)
    {
        if (control instanceof CDateTime cdatetime)
        {
            Date d = Date.from(date.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
            cdatetime.setSelection(d);
        }
        else
        {
            // DateTime widget has zero-based months
            ((DateTime) control).setDate(date.getYear(), date.getMonthValue() - 1, date.getDayOfMonth());
        }
    }

    public LocalDate getSelection()
    {
        if (control instanceof CDateTime cdatetime)
        {
            Date d = cdatetime.getSelection();

            if (d == null)
            {
                Date now = Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant());
                ((CDateTime) control).setSelection(now);
                return LocalDate.now();
            }
            else
            {
                return LocalDateTime.ofInstant(d.toInstant(), ZoneId.systemDefault()).toLocalDate();
            }
        }
        else
        {
            DateTime dateTime = (DateTime) control;
            // DateTime widget has zero-based months
            return LocalDate.of(dateTime.getYear(), dateTime.getMonth() + 1, dateTime.getDay());
        }
    }

    public void addSelectionListener(SelectionListener listener)
    {
        if (control instanceof CDateTime cdatetime)
            cdatetime.addSelectionListener(listener);
        else
            ((DateTime) control).addSelectionListener(listener);
    }

    public void setLayoutData(Object layoutData)
    {
        control.setLayoutData(layoutData);
    }
}
