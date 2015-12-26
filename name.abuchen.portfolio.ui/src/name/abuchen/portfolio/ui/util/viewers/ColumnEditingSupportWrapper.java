package name.abuchen.portfolio.ui.util.viewers;

import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

public class ColumnEditingSupportWrapper extends EditingSupport
{
    private ColumnViewer viewer;
    private ColumnEditingSupport proxy;
    private CellEditor editor;

    public ColumnEditingSupportWrapper(ColumnViewer viewer, ColumnEditingSupport proxy)
    {
        super(viewer);
        this.viewer = viewer;
        this.proxy = proxy;
    }

    @Override
    protected CellEditor getCellEditor(Object element)
    {
        if (editor == null)
            editor = proxy.createEditor((Composite) viewer.getControl());
        proxy.prepareEditor(element);
        return editor;
    }

    @Override
    protected boolean canEdit(Object element)
    {
        return proxy.canEdit(element);
    }

    @Override
    protected Object getValue(Object element)
    {
        try
        {
            return proxy.getValue(element);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void setValue(Object element, Object value)
    {
        try
        {
            proxy.setValue(element, value);
            viewer.update(element, null);
        }
        catch (Exception e)
        {
            PortfolioPlugin.log(e);
            ErrorDialog dialog = new ErrorDialog(Display.getDefault().getActiveShell(), Messages.LabelError,
                            Messages.LabelInputValidationFailed, new Status(Status.ERROR, PortfolioPlugin.PLUGIN_ID,
                                            e.getMessage(), e), IStatus.ERROR);
            dialog.setBlockOnOpen(false);
            dialog.open();
        }
    }
}
