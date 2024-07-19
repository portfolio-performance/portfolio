package name.abuchen.portfolio.ui.util.viewers;

import java.text.DecimalFormatSymbols;
import java.util.Locale;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

import name.abuchen.portfolio.model.Adaptor;
import name.abuchen.portfolio.model.Attributable;
import name.abuchen.portfolio.model.AttributeType;
import name.abuchen.portfolio.model.Attributes;
import name.abuchen.portfolio.ui.util.NumberVerifyListener;

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

        if (attribute.getConverter() instanceof AttributeType.LimitPriceConverter
                        && ("FR".equals(Locale.getDefault().getCountry()))) //$NON-NLS-1$
        {
            char decimalSeparator = new DecimalFormatSymbols().getDecimalSeparator();
            Text textEditorValue = (Text) textEditor.getControl();
            ((Text) textEditor.getControl()).addKeyListener(new KeyAdapter()
            {
                @Override
                public void keyPressed(KeyEvent e)
                {
                    if ((e.keyCode == SWT.KEYPAD_DECIMAL))
                    {
                        e.doit = false;
                        textEditorValue.insert(String.valueOf(decimalSeparator));
                    }
                }
            });
        }

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
