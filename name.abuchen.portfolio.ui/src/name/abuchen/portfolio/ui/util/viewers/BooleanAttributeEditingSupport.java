package name.abuchen.portfolio.ui.util.viewers;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CheckboxCellEditor;
import org.eclipse.swt.widgets.Composite;

import name.abuchen.portfolio.model.Adaptor;
import name.abuchen.portfolio.model.Attributable;
import name.abuchen.portfolio.model.AttributeType;
import name.abuchen.portfolio.model.Attributes;

public class BooleanAttributeEditingSupport extends AttributeEditingSupport
{
    public BooleanAttributeEditingSupport(AttributeType attribute)
    {
        super(attribute);

        Class<?> type = attribute.getType();
        if (type != Boolean.class)
            throw new IllegalArgumentException("attritbute is not of type Boolean but " + type); //$NON-NLS-1$
    }

    @Override
    public CellEditor createEditor(Composite composite)
    {
        return new CheckboxCellEditor(composite);
    }

    @Override
    public Object getValue(Object element) throws Exception
    {
        Attributes attribs = Adaptor.adapt(Attributable.class, element).getAttributes();
        Boolean value = (Boolean) attribs.get(attribute);
        return value != null ? value : Boolean.FALSE;
    }
}
