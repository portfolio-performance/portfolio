package name.abuchen.portfolio.ui.util.viewers;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

import name.abuchen.portfolio.model.Adaptor;
import name.abuchen.portfolio.model.Attributable;
import name.abuchen.portfolio.model.AttributeType;
import name.abuchen.portfolio.model.Attributes;
import name.abuchen.portfolio.model.PartialExemption;

/**
 * Edits an attribute by selecting one of the statutory partial exemption rates
 * of § 20 InvStG instead of entering a free percentage.
 */
public class PartialExemptionEditingSupport extends AttributeEditingSupport
{
    private ComboBoxCellEditor editor;

    public PartialExemptionEditingSupport(AttributeType attribute)
    {
        super(attribute);

        if (attribute.getType() != Double.class)
            throw new IllegalArgumentException("attribute is not of type Double but " + attribute.getType()); //$NON-NLS-1$
    }

    @Override
    public CellEditor createEditor(Composite composite)
    {
        editor = new ComboBoxCellEditor(composite, PartialExemption.getLabels(), SWT.READ_ONLY);
        return editor;
    }

    @Override
    public Object getValue(Object element)
    {
        return PartialExemption.indexOf((Double) attributes(element).get(attribute));
    }

    @Override
    public void setValue(Object element, Object value)
    {
        var newValue = PartialExemption.valueAt((Integer) value);
        if (newValue == null)
            return;

        var attribs = attributes(element);
        var oldValue = attribs.get(attribute);

        if (!newValue.equals(oldValue))
        {
            attribs.put(attribute, newValue);
            notify(element, newValue, oldValue);
        }
    }

    private Attributes attributes(Object element)
    {
        Attributable attributable = Adaptor.adapt(Attributable.class, element);
        if (attributable == null)
            throw new IllegalArgumentException("element is not attributable: " + element); //$NON-NLS-1$

        return attributable.getAttributes();
    }
}
