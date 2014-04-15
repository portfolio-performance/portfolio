package name.abuchen.portfolio.ui.util;

import name.abuchen.portfolio.model.Attributable;
import name.abuchen.portfolio.model.AttributeType;
import name.abuchen.portfolio.model.Attributes;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

public class AttributeEditingSupport extends ColumnEditingSupport
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
