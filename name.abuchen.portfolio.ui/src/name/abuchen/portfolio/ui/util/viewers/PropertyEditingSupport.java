package name.abuchen.portfolio.ui.util.viewers;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.util.function.Predicate;

import name.abuchen.portfolio.model.Adaptor;

public abstract class PropertyEditingSupport extends ColumnEditingSupport
{
    private Class<?> subjectType;
    private PropertyDescriptor descriptor;
    private Predicate<Object> canEditCheck;

    public PropertyEditingSupport(Class<?> subjectType, String attributeName)
    {
        this.subjectType = subjectType;
        this.descriptor = descriptorFor(subjectType, attributeName);
    }

    public PropertyEditingSupport setCanEditCheck(Predicate<Object> canEditCheck)
    {
        this.canEditCheck = canEditCheck;
        return this;
    }

    protected PropertyDescriptor descriptor()
    {
        return descriptor;
    }

    @Override
    public boolean canEdit(Object element)
    {
        return adapt(element) != null && (canEditCheck == null || canEditCheck.test(element));
    }

    protected Object adapt(Object element)
    {
        return Adaptor.adapt(subjectType, element);
    }

    protected PropertyDescriptor descriptorFor(Class<?> subjectType, String attributeName)
    {
        try
        {
            PropertyDescriptor[] properties = Introspector.getBeanInfo(subjectType).getPropertyDescriptors();
            for (PropertyDescriptor p : properties)
                if (attributeName.equals(p.getName()))
                    return p;
            throw new IllegalArgumentException(String.format("%s has no property named %s", subjectType //$NON-NLS-1$
                            .getName(), attributeName));
        }
        catch (IntrospectionException e)
        {
            throw new IllegalArgumentException(e);
        }
    }

}
