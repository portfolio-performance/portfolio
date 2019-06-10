package name.abuchen.portfolio.ui.util.viewers;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.FormatStyle;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

import com.ibm.icu.text.MessageFormat;

import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.Messages;

public class DateTimeEditingSupport extends PropertyEditingSupport
{
    private static final DateTimeFormatter[] timeFormatters = new DateTimeFormatter[] {
                    DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM),
                    DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT), //
                    DateTimeFormatter.ofLocalizedDateTime(FormatStyle.LONG), //
                    DateTimeFormatter.ofPattern("d.M.yyyy HH:mm"), //$NON-NLS-1$
                    DateTimeFormatter.ofPattern("d.M.yy HH:mm"), //$NON-NLS-1$
                    DateTimeFormatter.ISO_DATE_TIME };

    private static final DateTimeFormatter[] dateFormatters = new DateTimeFormatter[] {
                    DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM),
                    DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT), //
                    DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG), //
                    DateTimeFormatter.ofPattern("d.M.yyyy"), //$NON-NLS-1$
                    DateTimeFormatter.ofPattern("d.M.yy"), //$NON-NLS-1$
                    DateTimeFormatter.ISO_DATE };

    public DateTimeEditingSupport(Class<?> subjectType, String attributeName)
    {
        super(subjectType, attributeName);

        if (!LocalDateTime.class.isAssignableFrom(descriptor().getPropertyType()))
            throw new IllegalArgumentException(
                            String.format("Property %s needs to be of type LocalDateTime", attributeName)); //$NON-NLS-1$
    }

    @Override
    public CellEditor createEditor(Composite composite)
    {
        TextCellEditor textEditor = new TextCellEditor(composite);
        ((Text) textEditor.getControl()).setTextLimit(20);
        return textEditor;
    }

    @Override
    public final Object getValue(Object element) throws Exception
    {
        LocalDateTime date = (LocalDateTime) descriptor().getReadMethod().invoke(adapt(element));
        return Values.DateTime.format(date);
    }

    @Override
    public final void setValue(Object element, Object value) throws Exception
    {
        String inputValue = String.valueOf(value).trim();
        LocalDateTime newValue = null;

        for (DateTimeFormatter formatter : timeFormatters)
        {
            try
            {
                newValue = LocalDateTime.parse(inputValue, formatter);
                break;
            }
            catch (DateTimeParseException ignore)
            {
                // continue with next formatter
            }
        }

        if (newValue == null)
        {
            for (DateTimeFormatter formatter : dateFormatters)
            {
                try
                {
                    newValue = LocalDate.parse(inputValue, formatter).atStartOfDay();
                    break;
                }
                catch (DateTimeParseException ignore)
                {
                    // continue with next formatter
                }
            }
        }

        if (newValue == null)
            throw new IllegalArgumentException(MessageFormat.format(Messages.MsgErrorNotAValidDate, value));

        Object subject = adapt(element);

        LocalDateTime oldValue = (LocalDateTime) descriptor().getReadMethod().invoke(subject);

        if (!newValue.equals(oldValue))
        {
            descriptor().getWriteMethod().invoke(subject, newValue);
            notify(element, newValue, oldValue);
        }
    }
}
