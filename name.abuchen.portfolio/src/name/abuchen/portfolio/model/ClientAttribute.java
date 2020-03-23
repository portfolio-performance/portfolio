package name.abuchen.portfolio.model;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;

import name.abuchen.portfolio.model.AttributeType;
import name.abuchen.portfolio.model.ClientAttribute;

public class ClientAttribute extends AttributeType
{
    private Attributable parent;
    //private Object value;
    private boolean canEdit = false;
    
    public ClientAttribute(String id)
    {
        super(id);
    }

    public void setParent(Attributable parent)
    {
        this.parent = (Attributable) parent;
        setTarget(parent.getClass());
    }

    public Attributable getParent()
    {
        System.err.println(">>>> ClientAttribute::getParent parent " + parent.toString() + "  class: " + parent.getClass().toString());
        return parent;
    }

    public void setValue(Object value)
    {
        System.err.println(">>>> ClientAttribute::setValue value " + value.getClass().toString() + "  type: " + this.getType().toString());
        if (this.getType().isInstance(value))
        {
            Object castedValue = this.getType().cast(value);
            System.err.println("                         castedValue " + castedValue.toString() + "  class: " + castedValue.getClass().toString());
            try
            {
                PropertyDescriptor descriptor;
                descriptor = descriptorFor(getTarget(), this.getId());
                Attributable attributable = Adaptor.adapt(Attributable.class, getParent());
                System.err.println("                         descriptor " + descriptor.toString() + "  attributable: " + attributable.toString());
                if (attributable != null)
                    descriptor.getWriteMethod().invoke(attributable, castedValue);
            }
            catch (Exception e)
            {
                throw new RuntimeException(String.format("Descriptor failed with exception <%s>", e)); //$NON-NLS-1$
            }
        }
        else
            throw new IllegalArgumentException("Value " + value.getClass().toString() + " is not same class " + this.getType().getClass().toString() + " expected for AttributeType " + this.getId());

   }

    public Object getValue()
    {
        System.err.println(">>>> ClientAttribute::getValue ID: " + this.getId() + " type: " + this.getType().toString());
        Object value;
        try
        {
            PropertyDescriptor descriptor;
            descriptor = descriptorFor(getTarget(), this.getId());
            Attributable attributable = Adaptor.adapt(Attributable.class, getParent());
            System.err.println(">>>> ClientAttribute::getValue attributable: " + (attributable != null?attributable.toString():""));
            value = descriptor.getReadMethod().invoke(attributable);
        }
        catch (Exception e)
        {
            throw new RuntimeException(String.format("Descriptor failed with exception <%s>", e)); //$NON-NLS-1$
        }
        System.err.println(">>>> ClientAttribute::getValue value: " + value.toString());
        return value;
    }

    public void setEdit(boolean canEdit)
    {
        this.canEdit = canEdit;
    }

    public boolean canEdit()
    {
        return canEdit;
    }

    protected PropertyDescriptor descriptorFor(Class<?> subjectType, String attributeName)
    {
        try
        {
            PropertyDescriptor[] properties = Introspector.getBeanInfo(subjectType).getPropertyDescriptors();
            System.err.println(">>>> ClientAttribute::descriptorFor attributeName " + attributeName);
            for (PropertyDescriptor p : properties)
            {
                System.err.println("                                    property: " + p.getName());
                if (attributeName.equals(p.getName()))
                    return p;
            }
            throw new IllegalArgumentException(String.format("%s has no property named %s", subjectType //$NON-NLS-1$
                            .getName(), attributeName));
        }
        catch (IntrospectionException e)
        {
            throw new IllegalArgumentException(e);
        }
    }

}