package name.abuchen.portfolio.ui.util.viewers;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import org.eclipse.jface.viewers.CellEditor;
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
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.ClientAttribute;
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
        System.err.println(">>>> AttributeEditingSupport::getAttributeType: element " + ((ClientAttribute) element).toString());
        return (AttributeType) (attribute != null?this.attribute:element);
    }

    private Object getAttributable(Object element)
    {
        System.err.println(">>>> AttributeEditingSupport::getAttributable: element " + element.toString());
        if (element instanceof ClientAttribute)
            System.err.println(">>>> AttributeEditingSupport::getAttributable: parent " + ((ClientAttribute) element).getParent().toString());

        Attributable attributable = Adaptor.adapt(Attributable.class, (element instanceof ClientAttribute?((ClientAttribute) element).getParent():element));
        if (attributable != null)
            System.err.println(">>>> AttributeEditingSupport::getAttributable: attributable " + attributable.toString());
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
        System.err.println(">>>> AttributeEditingSupport::createEditor: Attribute " + attribute.toString() + " Id: " + attribute.getId());
        System.err.println("                                              element " + element.toString() + " Class: " + element.getClass().toString());
        if (attribute.getType() == Path.class)
        {
            System.err.println(">>>> AttributeEditingSupport::createEditor-DialogCellEditor: composite " + composite.toString());
            DialogCellEditor dialogEditor = new DialogCellEditor(composite) {
                protected Object openDialogBox(Control cellEditorWindow) {
                    Shell shell  = Display.getDefault().getActiveShell();
                    System.err.println(">>>> AttributeEditingSupport::DialogCellEditor: oldValue " + getValue());
                    String path = (String) getValue();
                    
                    // taken from on http://www.java2s.com/Code/Java/SWT-JFace-Eclipse/DemonstratestheDirectoryDialogclass.htm
                    DirectoryDialog dlg = new DirectoryDialog(shell);

                    // Set the initial filter path according
                    // to anything they've selected or typed in
                    dlg.setFilterPath(path);

                    // Change the title bar text
                    dlg.setText("SWT's DirectoryDialog");

                    // Customizable message displayed in the dialog
                    dlg.setMessage("Select a directory");

                    // Calling open() will open and run the dialog.
                    // It will return the selected directory, or
                    // null if user cancels
                    String dir = dlg.open();
                    if (dir != null) {
                      // Set the text box to the new selection
                        System.err.println(">>>> AttributeEditingSupport::getDirectory "  + dir);
                        return dir;
                    }
                    else
                    {
                        return "<none>";
                    }
                }
            };
            return dialogEditor;
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
        System.err.println(">>>> AttributeEditingSupport::canEdit: element " + element.toString());
        System.err.println(">>>> AttributeEditingSupport::canEdit: class " + element.getClass().toString());
        Attributable attributable = Adaptor.adapt(Attributable.class, getAttributable(element));
        if (attributable == null)
            return false;

        return getAttributeType(element).supports(attributable.getClass());
    }

    @Override
    public Object getValue(Object element) throws Exception
    {
        System.err.println(">>>> AttributeEditingSupport::getValue: element " + element.toString());
        Attributes attribs = Adaptor.adapt(Attributable.class, getAttributable(element)).getAttributes();
        return getAttributeType(element).getConverter().toString(attribs.get(getAttributeType(element)));
    }

    @Override
    public void setValue(Object element, Object value) throws Exception
    {
        AttributeType elementType = getAttributeType(element);
        Object elementAttributable = getAttributable(element);
        System.err.println(">>>> AttributeEditingSupport::setValue: element " + element.toString() + " value: " + value.toString());
        Attributes attribs = Adaptor.adapt(Attributable.class, elementAttributable).getAttributes();

        Object newValue = elementType.getConverter().fromString(String.valueOf(value));
        Object oldValue = attribs.get(elementType);

        System.err.println(">>>> AttributeEditingSupport::setValue: oldValue " + (oldValue == null?"<null>":oldValue.toString()) + " ==> newValue: " + (newValue == null?"<null>":newValue.toString()));

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
