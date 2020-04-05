package name.abuchen.portfolio.model;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import name.abuchen.portfolio.model.AttributeType;
import name.abuchen.portfolio.model.ClientAttribute;

public class ClientAttribute extends AttributeType
{
    public ClientAttribute(String id)
    {
        super(id);
        optionsList = new ArrayList<AttributeFieldOption>();
    }

    public static class ListConverter implements Converter
    {
        private List<AttributeFieldOption> options;

        @Override
        public String toString(Object object)
        {
            return null;
        }

        @Override
        public Object fromString(String value)
        {
            return null;
        }

        public Integer toIndex(Object object)
        {
            Integer i = 0;
            for (AttributeFieldOption option : options)
            {
                if (option.getValue().equals(object))
                    return i;
                i++;
            }
            return -1;
        }

        public Object fromIndex(Integer value)
        {
            int index = (Integer) value;
            if (index < 0 || index >= options.size())
                return null;
            return ((AttributeFieldOption) options.toArray()[value]).getValue();
        }

        public void setOptions(List<AttributeFieldOption> options)
        {
            this.options = options;
        }

        public List<AttributeFieldOption> getOptions()
        {
            return options;
        }
    }

    public static class BooleanConverter extends ListConverter
    {
        public BooleanConverter()
        {
            List<AttributeFieldOption> options = new ArrayList<AttributeFieldOption>();
            options.add(new AttributeFieldOption(((Boolean) false).toString(), false));
            options.add(new AttributeFieldOption(((Boolean) true).toString(), true));
            setOptions(options);
        }
    }

    public static class PathConverter implements Converter
    {
        @Override
        public String toString(Object object)
        {
            return object != null ? ((Path) object).toString() : ""; //$NON-NLS-1$
        }

        @Override
        public Object fromString(String value)
        {
            if (value.trim().length() == 0)
                return null;
            return Paths.get(value);
        }
    }

    static class AttributeFieldOption
    {
        // The name of the option
        private String name;

        // The actual Value
        private Object value;

        public AttributeFieldOption(String name, Object value)
        {
          this.name = name;
          this.value = value;
        }

        public String getName()
        {
          return name;
        }

       public Object getValue()
       {
         return value;
       }
     }

    private Attributable parent;
    private boolean canEdit = false;
    List<AttributeFieldOption> optionsList = new ArrayList<AttributeFieldOption>();

    public void setParent(Attributable parent)
    {
        this.parent = (Attributable) parent;
        setTarget(parent.getClass());
    }

    public Attributable getParent()
    {
        return parent;
    }

    public void setOptions(List<AttributeFieldOption> options)
    {
        optionsList = options;
        Converter converter = super.getConverter();
        if (converter instanceof ListConverter)
            ((ListConverter) converter).setOptions(options);
    }

    public void addOptions(AttributeFieldOption option)
    {
        optionsList.add(option);
        setOptions(optionsList);
    }

    public String[] getOptions()
    {
        List<String> optionsStrings = new ArrayList<String>();
        if (getConverter() instanceof ListConverter)
            if (optionsList.size() == 0)
                optionsList = ((ListConverter) getConverter()).getOptions();
        optionsList.forEach(o ->  {
                            optionsStrings.add(o.getName());
                        });
        return optionsStrings.toArray(new String[0]);
    }

    @Override
    public void setType(Class<?> type)
    {
        super.setType(type);
        if (Path.class == type)
            setConverter(PathConverter.class);
        else if (Boolean.class == type)
            setConverter(BooleanConverter.class);
    }


    public void setValue(Object value)
    {
        if (this.getType().isInstance(value))
        {
            Object castedValue = this.getType().cast(value);
            try
            {
                PropertyDescriptor descriptor;
                descriptor = descriptorFor(getTarget(), this.getId());
                Attributable attributable = Adaptor.adapt(Attributable.class, getParent());
                if (attributable != null)
                    descriptor.getWriteMethod().invoke(attributable, castedValue);
            }
            catch (Exception e)
            {
                throw new RuntimeException(String.format("Descriptor failed with exception <%s>", e)); //$NON-NLS-1$
            }
        }
        else
            throw new IllegalArgumentException("Value " + value.getClass().toString() + " is not same class " + this.getType().getClass().toString() + " expected for AttributeType " + this.getId()); //$NON-NLS-1$  //$NON-NLS-2$ //$NON-NLS-3$

   }

    public Object getValue()
    {
        Object value;
        try
        {
            PropertyDescriptor descriptor;
            descriptor = descriptorFor(getTarget(), this.getId());
            Attributable attributable = Adaptor.adapt(Attributable.class, getParent());
            value = descriptor.getReadMethod().invoke(attributable);
        }
        catch (Exception e)
        {
            throw new RuntimeException(String.format("Descriptor failed with exception <%s>", e)); //$NON-NLS-1$
        }
        return value;
    }

    public String getString()
    {
        if (optionsList.size() > 0)
            for (AttributeFieldOption option : optionsList)
                if (option.getValue() == getValue())
                    return option.getName();
        return getValue().toString();
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