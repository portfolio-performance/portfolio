package name.abuchen.portfolio.ui.util.viewers;

import java.text.MessageFormat;
import java.util.function.Predicate;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.CurrencyToStringConverter;
import name.abuchen.portfolio.ui.util.NumberVerifyListener;
import name.abuchen.portfolio.ui.util.StringToCurrencyConverter;

public class ValueEditingSupport extends PropertyEditingSupport
{
    private final StringToCurrencyConverter stringToLong;
    private final CurrencyToStringConverter longToString;

    private final Predicate<Number> validator;

    public ValueEditingSupport(Class<?> subjectType, String attributeName, Values<? extends Number> valueType,
                    Predicate<Number> validator)
    {
        super(subjectType, attributeName);

        this.validator = validator;

        Class<?> propertyType = descriptor().getPropertyType();
        if (!long.class.isAssignableFrom(propertyType) && !int.class.isAssignableFrom(propertyType))
            throw new UnsupportedOperationException(String.format(
                            "Property %s needs to be of type long or int to serve as decimal", attributeName)); //$NON-NLS-1$

        this.stringToLong = new StringToCurrencyConverter(valueType);
        this.longToString = new CurrencyToStringConverter(valueType);
    }

    public ValueEditingSupport(Class<?> subjectType, String attributeName, Values<? extends Number> valueType)
    {
        this(subjectType, attributeName, valueType, null);
    }

    @Override
    public CellEditor createEditor(Composite composite)
    {
        TextCellEditor textEditor = new TextCellEditor(composite);
        ((Text) textEditor.getControl()).setTextLimit(20);
        ((Text) textEditor.getControl()).addVerifyListener(new NumberVerifyListener());
        return textEditor;
    }

    @Override
    public final Object getValue(Object element) throws Exception
    {
        Number value = (Number) descriptor().getReadMethod().invoke(adapt(element));
        return longToString.convert(value.longValue());
    }

    @Override
    public void setValue(Object element, Object value) throws Exception
    {
        Object subject = adapt(element);

        Number newValue = stringToLong.convert(String.valueOf(value));
        if (int.class.isAssignableFrom(descriptor().getPropertyType())
                        || Integer.class.isAssignableFrom(descriptor().getPropertyType()))
            newValue = Integer.valueOf(newValue.intValue());

        if (validator != null && !validator.test(newValue))
            throw new IllegalArgumentException(
                            MessageFormat.format(Messages.MsgDialogInputRequired, descriptor().getDisplayName()));

        Number oldValue = (Number) descriptor().getReadMethod().invoke(subject);

        if (!newValue.equals(oldValue))
        {
            descriptor().getWriteMethod().invoke(subject, newValue);
            notify(element, newValue, oldValue);
        }
    }
}
