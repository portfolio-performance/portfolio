package name.abuchen.portfolio.ui.util.viewers;

import java.io.IOException;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.DialogCellEditor;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;

import name.abuchen.portfolio.model.AttributeType;
import name.abuchen.portfolio.model.AttributeType.ImageConverter;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.util.ImageUtil;

public class ImageAttributeEditingSupport extends AttributeEditingSupport
{
    public ImageAttributeEditingSupport(AttributeType attribute)
    {
        super(attribute);

        if (!(attribute.getConverter() instanceof ImageConverter))
            throw new IllegalArgumentException();
    }

    @Override
    public CellEditor createEditor(Composite composite)
    {
        return new FileDialogCellEditor(composite);
    }

    static class FileDialogCellEditor extends DialogCellEditor
    {
        public FileDialogCellEditor(Composite parent)
        {
            super(parent);
        }

        @Override
        protected void updateContents(Object value)
        {
            // do not show text
        }

        @Override
        protected Button createButton(Composite parent)
        {
            Button button = super.createButton(parent);
            button.setText("..."); //$NON-NLS-1$
            return button;
        }

        @Override
        protected Object openDialogBox(Control cellEditorWindow)
        {
            FileDialog dial = new FileDialog(Display.getCurrent().getActiveShell());
            String filename = dial.open();
            if (filename != null)
            {
                try
                {
                    return ImageUtil.loadAndPrepare(filename, ImageConverter.MAXIMUM_SIZE_EMBEDDED_IMAGE,
                                    ImageConverter.MAXIMUM_SIZE_EMBEDDED_IMAGE);
                }
                catch (IOException ex)
                {
                    MessageDialog.openError(Display.getCurrent().getActiveShell(), Messages.LabelInfo, ex.getMessage());
                }
            }
            return null;
        }
    }
}
