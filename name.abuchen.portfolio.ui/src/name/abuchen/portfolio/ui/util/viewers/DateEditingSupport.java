package name.abuchen.portfolio.ui.util.viewers;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.FormatStyle;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

import com.ibm.icu.text.MessageFormat;

import name.abuchen.portfolio.ui.Messages;

public class DateEditingSupport extends PropertyEditingSupport
{
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM);

    public DateEditingSupport(Class<?> subjectType, String attributeName)
    {
        super(subjectType, attributeName);

        if (!LocalDate.class.isAssignableFrom(descriptor().getPropertyType()))
            throw new RuntimeException(String.format("Property %s needs to be of type date", attributeName)); //$NON-NLS-1$
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
        LocalDate date = (LocalDate) descriptor().getReadMethod().invoke(adapt(element));
        return formatter.format(date);
    }

    @Override
    public final void setValue(Object element, Object value) throws Exception
    {
        Object subject = adapt(element);
        LocalDate newValue = null;

        try
        {
            newValue = LocalDate.parse(String.valueOf(value), formatter);
        }
        catch (DateTimeParseException e)
        {
            throw new IllegalArgumentException(MessageFormat.format(Messages.MsgErrorNotAValidDate, value), e);
        }

        LocalDate oldValue = (LocalDate) descriptor().getReadMethod().invoke(subject);

        if (!newValue.equals(oldValue))
        {
            descriptor().getWriteMethod().invoke(subject, newValue);
            notify(element, newValue, oldValue);
        }
    }
}
