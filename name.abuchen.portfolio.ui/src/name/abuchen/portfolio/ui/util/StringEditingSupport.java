package name.abuchen.portfolio.ui.util;


public class StringEditingSupport extends PropertyEditingSupport
{
    private boolean isMandatory = false;

    public StringEditingSupport(Class<?> subjectType, String attributeName)
    {
        super(subjectType, attributeName);

        if (!String.class.isAssignableFrom(descriptor().getPropertyType()))
            throw new RuntimeException(String.format("Property %s needs to be of type string", attributeName)); //$NON-NLS-1$
    }

    public StringEditingSupport setMandatory(boolean isMandatory)
    {
        this.isMandatory = isMandatory;
        return this;
    }

    @Override
    public Object getValue(Object element) throws Exception
    {
        String v = (String) descriptor().getReadMethod().invoke(element);
        return v != null ? v : ""; //$NON-NLS-1$
    }

    @Override
    public void setValue(Object element, Object value) throws Exception
    {
        String newValue = (String) value;
        String oldValue = (String) descriptor().getReadMethod().invoke(element);

        if (!value.equals(oldValue) && (!isMandatory || newValue.trim().length() > 0))
        {
            descriptor().getWriteMethod().invoke(element, newValue);
            notify(element, newValue, oldValue);
        }
    }
}
