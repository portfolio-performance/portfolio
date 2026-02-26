package name.abuchen.portfolio.ui.util.viewers;

import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.FormatStyle;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Adaptor;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.Messages;

public class ExDateEditingSupport extends ColumnEditingSupport
{
    private static final DateTimeFormatter[] dateFormatters = new DateTimeFormatter[] {
                    DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM),
                    DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT), //
                    DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG), //
                    DateTimeFormatter.ofPattern("d.M.yyyy"), //$NON-NLS-1$
                    DateTimeFormatter.ofPattern("d.M.yy"), //$NON-NLS-1$
                    DateTimeFormatter.ISO_DATE };

    @Override
    public boolean canEdit(Object element)
    {
        var tx = Adaptor.adapt(AccountTransaction.class, element);
        return tx != null && tx.getSecurity() != null;
    }

    @Override
    public Object getValue(Object element)
    {
        var tx = Adaptor.adapt(AccountTransaction.class, element);
        var exDate = tx.getExDate();
        return exDate != null ? Values.Date.format(exDate.toLocalDate()) : ""; //$NON-NLS-1$
    }

    @Override
    public void setValue(Object element, Object value)
    {
        var tx = Adaptor.adapt(AccountTransaction.class, element);

        var oldValue = tx.getExDate();

        var inputValue = ((String) value).trim();

        // ex-date is an optional value, therefore we remove the ex-date if not
        // input is given
        if (inputValue.isEmpty())
        {
            if (oldValue != null)
            {
                tx.setExDate(null);
                notify(element, null, oldValue);
            }
            return;
        }

        LocalDateTime newValue = null;
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

        // there is text input, but it cannot be parsed as a local date -> throw
        // an error
        if (newValue == null)
            throw new IllegalArgumentException(MessageFormat.format(Messages.MsgErrorNotAValidDate, value));

        if (!newValue.equals(oldValue))
        {
            tx.setExDate(newValue);
            notify(element, newValue, oldValue);
        }
    }

    @Override
    public CellEditor createEditor(Composite composite)
    {
        TextCellEditor textEditor = new TextCellEditor(composite);
        ((Text) textEditor.getControl()).setTextLimit(20);
        return textEditor;
    }
}
