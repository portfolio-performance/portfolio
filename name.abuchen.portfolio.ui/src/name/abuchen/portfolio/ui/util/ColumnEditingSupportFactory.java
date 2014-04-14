package name.abuchen.portfolio.ui.util;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;

import name.abuchen.portfolio.model.Attributable;
import name.abuchen.portfolio.model.AttributeType;
import name.abuchen.portfolio.model.Attributes;
import name.abuchen.portfolio.model.Values;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

public class ColumnEditingSupportFactory
{
    private static final class NumberVerifyListener implements VerifyListener
    {
        private String allowedChars = ",.0123456789"; //$NON-NLS-1$

        public void verifyText(VerifyEvent e)
        {
            for (int ii = 0; e.doit && ii < e.text.length(); ii++)
                e.doit = allowedChars.indexOf(e.text.charAt(0)) >= 0;
        }
    }

    public static class ValueEditingSupport extends ColumnEditingSupport
    {
        private final PropertyDescriptor descriptor;
        private final StringToCurrencyConverter toCurrency;
        private final CurrencyToStringConverter fromCurrency;

        public ValueEditingSupport(PropertyDescriptor descriptor, Values<Long> value)
        {
            this.descriptor = descriptor;
            this.toCurrency = new StringToCurrencyConverter(value);
            this.fromCurrency = new CurrencyToStringConverter(value);
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
        public boolean canEdit(Object element)
        {
            return true;
        }

        @Override
        public Object getValue(Object element) throws Exception
        {
            return fromCurrency.convert(descriptor.getReadMethod().invoke(element));
        }

        @Override
        public void setValue(Object element, Object value) throws Exception
        {
            Number newValue = (Number) toCurrency.convert(String.valueOf(value));
            Number oldValue = (Number) descriptor.getReadMethod().invoke(element);

            if (!newValue.equals(oldValue))
            {
                descriptor.getWriteMethod().invoke(element, newValue);
                notify(element, newValue, oldValue);
            }
        }
    }

    public static class AttributeEditingSupport extends ColumnEditingSupport
    {
        private final Class<? extends Attributable> type;
        private final AttributeType attribute;

        public AttributeEditingSupport(Class<? extends Attributable> type, AttributeType attribute)
        {
            this.type = type;
            this.attribute = attribute;
        }

        @Override
        public CellEditor createEditor(Composite composite)
        {
            TextCellEditor textEditor = new TextCellEditor(composite);
            if (attribute.isNumber())
                ((Text) textEditor.getControl()).addVerifyListener(new NumberVerifyListener());
            return textEditor;
        }

        @Override
        public boolean canEdit(Object element)
        {
            return true;
        }

        @Override
        public Object getValue(Object element) throws Exception
        {
            Attributes attribs = type.cast(element).getAttributes();
            return attribute.getConverter().toString(attribs.get(attribute));
        }

        @Override
        public void setValue(Object element, Object value) throws Exception
        {
            Attributes attribs = type.cast(element).getAttributes();

            Object newValue = attribute.getConverter().fromString(String.valueOf(value));
            Object oldValue = attribs.get(attribute);

            // update the value
            // * if it has non null value and the value actually changed
            // * or if it is null and previously existed

            if ((newValue != null && !newValue.equals(oldValue)) //
                            || (newValue == null && attribs.exists(attribute)))
            {
                attribs.put(attribute, newValue);
                notify(element, newValue, oldValue);
            }
        }
    }

    private static class StringEditingSupport extends ColumnEditingSupport
    {
        private final PropertyDescriptor descriptor;

        public StringEditingSupport(PropertyDescriptor descriptor)
        {
            this.descriptor = descriptor;
        }

        @Override
        public CellEditor createEditor(Composite composite)
        {
            return new TextCellEditor(composite);
        }

        @Override
        public boolean canEdit(Object element)
        {
            return true;
        }

        @Override
        public Object getValue(Object element) throws Exception
        {
            String v = (String) descriptor.getReadMethod().invoke(element);
            return v != null ? v : ""; //$NON-NLS-1$
        }

        @Override
        public void setValue(Object element, Object value) throws Exception
        {
            String oldValue = (String) descriptor.getReadMethod().invoke(element);

            if (!value.equals(oldValue))
            {
                descriptor.getWriteMethod().invoke(element, value);
                notify(element, value, oldValue);
            }
        }
    }

    public static ColumnEditingSupport create(Class<?> type, String attribute)
    {
        PropertyDescriptor descriptor = descriptorFor(type, attribute);

        Class<?> attributeType = descriptor.getPropertyType();

        if (attributeType == String.class)
            return new StringEditingSupport(descriptor);

        throw new RuntimeException(String.format("Attribute type %s not supported", attributeType.getName())); //$NON-NLS-1$
    }

    public static ColumnEditingSupport value(Class<?> type, String attribute, Values<Long> value)
    {
        final PropertyDescriptor descriptor = descriptorFor(type, attribute);
        if (Long.class.isAssignableFrom(descriptor.getPropertyType()))
            throw new UnsupportedOperationException(String.format(
                            "Property %s needs to be of type long to serve as decimal", attribute)); //$NON-NLS-1$

        return new ValueEditingSupport(descriptor, value);
    }

    public static ColumnEditingSupport attribute(Class<? extends Attributable> type, AttributeType attribute)
    {
        return new AttributeEditingSupport(type, attribute);
    }

    private static PropertyDescriptor descriptorFor(Class<?> subjectType, String name)
    {
        try
        {
            PropertyDescriptor[] properties = Introspector.getBeanInfo(subjectType).getPropertyDescriptors();
            for (PropertyDescriptor p : properties)
                if (name.equals(p.getName()))
                    return p;
            throw new RuntimeException(String.format("%s has no property named %s", subjectType //$NON-NLS-1$
                            .getName(), name));
        }
        catch (IntrospectionException e)
        {
            throw new RuntimeException(e);
        }
    }

}
