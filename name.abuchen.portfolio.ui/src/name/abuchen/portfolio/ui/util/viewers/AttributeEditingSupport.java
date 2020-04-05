package name.abuchen.portfolio.ui.util.viewers;

import java.nio.file.Path;
import java.util.stream.Stream;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.jface.viewers.DialogCellEditor;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import name.abuchen.portfolio.model.Adaptor;
import name.abuchen.portfolio.model.Attributable;
import name.abuchen.portfolio.model.AttributeType;
import name.abuchen.portfolio.model.Attributes;
import name.abuchen.portfolio.model.ClientAttribute;
import name.abuchen.portfolio.model.ClientAttribute.BooleanConverter;
import name.abuchen.portfolio.model.ClientAttribute.ListConverter;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.NumberVerifyListener;

public class AttributeEditingSupport extends ColumnEditingSupport
{
    protected final AttributeType attribute;
    protected final Stream<? extends AttributeType> stream;

    public AttributeEditingSupport(AttributeType attribute)
    {
        this.attribute = attribute;
        this.stream = null;
    }

    public AttributeEditingSupport(Stream<? extends AttributeType> stream)
    {
        this.stream = stream;
        this.attribute = null;
    }

    private AttributeType getAttributeType(Object element)
    {
        return (AttributeType) (attribute != null?this.attribute:element);
    }

    private Object getAttributable(Object element)
    {
        Attributable attributable = Adaptor.adapt(Attributable.class, (element instanceof ClientAttribute?((ClientAttribute) element).getParent():element));
        return attributable;
    }

    @Override
    public CellEditor createEditor(Composite composite)
    {
        return null;
    }

    public CellEditor createEditor(Composite composite, Object element)
    {
        AttributeType attribute = getAttributeType(element);
        if (attribute.getType() == Path.class)
        {
            DialogCellEditor dialogEditor = new DialogCellEditor(composite)
            {
                @Override
                protected Object openDialogBox(Control cellEditorWindow)
                {
                    Shell shell  = Display.getDefault().getActiveShell();
                    String path = (String) getValue();
                    
                    // taken from on http://www.java2s.com/Code/Java/SWT-JFace-Eclipse/DemonstratestheDirectoryDialogclass.htm
                    DirectoryDialog dlg = new DirectoryDialog(shell);
                    dlg.setFilterPath(path);
                    dlg.setText(Messages.TitleDirectoryDialog);
                    // Customizable message displayed in the dialog
                    //dlg.setMessage("");
                    String dir = dlg.open();
                    if (dir != null)
                        return dir;
                    else
                        return ""; //$NON-NLS-1$
                }
            };
            return dialogEditor;
        }
        else if (attribute instanceof ClientAttribute && attribute.getConverter() instanceof ListConverter)
        {
            Object value = ((ClientAttribute) attribute).getValue();
            ComboBoxCellEditor comboEditor = new ComboBoxCellEditor(composite, ((ClientAttribute) attribute).getOptions(), SWT.READ_ONLY);
            ListConverter converter = (ListConverter) ((ClientAttribute) attribute).getConverter();
            comboEditor.setValue((Integer) ((ListConverter) converter).toIndex(value));
            return comboEditor;
        }
        else
        {
            TextCellEditor textEditor = new TextCellEditor(composite);
            if (attribute.isNumber())
                ((Text) textEditor.getControl()).addVerifyListener(new NumberVerifyListener(true));
            return textEditor;
        }
    }

    @Override
    public boolean canEdit(Object element)
    {
        if (element instanceof ClientAttribute && !((ClientAttribute) element).canEdit())
           return ((ClientAttribute) element).canEdit();
        Attributable attributable = Adaptor.adapt(Attributable.class, getAttributable(element));
        if (attributable == null)
            return false;
        return getAttributeType(element).supports(attributable.getClass());
    }

    @Override
    public Object getValue(Object element) throws Exception
    {
        AttributeType elementType = getAttributeType(element);
        Attributes attribs = Adaptor.adapt(Attributable.class, getAttributable(element)).getAttributes();
        AttributeType.Converter converter = elementType.getConverter();
        Object object = attribs.get(elementType);
        if (converter instanceof ListConverter)
            return ((ListConverter) converter).toIndex(object);
        else
            return converter.toString(object);
    }

    @Override
    public void setValue(Object element, Object value) throws Exception
    {
        AttributeType elementType = getAttributeType(element);
        Object elementAttributable = getAttributable(element);
        Attributes attribs = Adaptor.adapt(Attributable.class, elementAttributable).getAttributes();

        AttributeType.Converter converter = elementType.getConverter();
        Object newValue;
        if (converter instanceof BooleanConverter)
            newValue = ((BooleanConverter) converter).fromIndex(Integer.valueOf(String.valueOf(value)));
        else
            newValue = converter.fromString(String.valueOf(value));
        Object oldValue = attribs.get(elementType);

        // update the value
        // * if it has non null value and the value actually changed
        // * or if it is null and previously existed

        if ((newValue != null && !newValue.equals(oldValue)) //
                        || (newValue == null && attribs.exists(elementType)))
        {
            attribs.put(elementType, newValue);
            notify(element, newValue, oldValue);
        }
    }
}
