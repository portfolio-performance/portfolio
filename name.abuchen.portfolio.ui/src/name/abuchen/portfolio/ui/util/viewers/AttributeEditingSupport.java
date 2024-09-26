package name.abuchen.portfolio.ui.util.viewers;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

import name.abuchen.portfolio.model.Adaptor;
import name.abuchen.portfolio.model.Attributable;
import name.abuchen.portfolio.model.AttributeType;
import name.abuchen.portfolio.model.Attributes;
import name.abuchen.portfolio.ui.util.NumberVerifyListener;
import name.abuchen.portfolio.ui.util.text.FrenchKeypadSupport;

public class AttributeEditingSupport extends ColumnEditingSupport
{
    protected final AttributeType attribute;

    public AttributeEditingSupport(AttributeType attribute)
    {
        this.attribute = attribute;
    }

    @Override
    public CellEditor createEditor(Composite composite)
    {
        TextCellEditor textEditor = new TextCellEditor(composite);
        if (attribute.isNumber())
            ((Text) textEditor.getControl()).addVerifyListener(new NumberVerifyListener(true));

        if (attribute.isNumber() || attribute.getConverter() instanceof AttributeType.LimitPriceConverter)
            FrenchKeypadSupport.configure((Text) textEditor.getControl());

        return textEditor;
    }

    @Override
    public boolean canEdit(Object element)
    {
        Attributable attributable = Adaptor.adapt(Attributable.class, element);
        if (attributable == null)
            return false;

        return attribute.supports(attributable.getClass());
    }

    @Override
    public Object getValue(Object element) throws Exception
    {
        Attributes attribs = Adaptor.adapt(Attributable.class, element).getAttributes();
        return attribute.getConverter().toString(attribs.get(attribute));
    }

    @Override
    public void setValue(Object element, Object value) throws Exception
    {
        Attributes attribs = Adaptor.adapt(Attributable.class, element).getAttributes();

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
