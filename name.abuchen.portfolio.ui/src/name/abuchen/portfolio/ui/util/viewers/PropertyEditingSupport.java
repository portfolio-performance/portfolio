package name.abuchen.portfolio.ui.util.viewers;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;

import name.abuchen.portfolio.model.Adaptor;

public abstract class PropertyEditingSupport extends ColumnEditingSupport
{
    private Class<?> subjectType;
    private PropertyDescriptor descriptor;

    public PropertyEditingSupport(Class<?> subjectType, String attributeName)
    {
        this.subjectType = subjectType;
        this.descriptor = descriptorFor(subjectType, attributeName);
    }

    protected PropertyDescriptor descriptor()
    {
        return descriptor;
    }

    @Override
    public boolean canEdit(Object element)
    {
        return adapt(element) != null;
    }

    protected Object adapt(Object element)
    {
        return Adaptor.adapt(subjectType, element);
    }

    private PropertyDescriptor descriptorFor(Class<?> subjectType, String attributeName)
    {
        try
        {
            PropertyDescriptor[] properties = Introspector.getBeanInfo(subjectType).getPropertyDescriptors();
            for (PropertyDescriptor p : properties)
                if (attributeName.equals(p.getName()))
                    return p;
            throw new RuntimeException(String.format("%s has no property named %s", subjectType //$NON-NLS-1$
                            .getName(), attributeName));
        }
        catch (IntrospectionException e)
        {
            throw new RuntimeException(e);
        }
    }

}
