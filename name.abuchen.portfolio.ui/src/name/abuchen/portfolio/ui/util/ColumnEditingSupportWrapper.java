package name.abuchen.portfolio.ui.util;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.TableViewer;

public class ColumnEditingSupportWrapper extends EditingSupport
{
    private TableViewer viewer;
    private ColumnEditingSupport proxy;
    private CellEditor editor;

    public ColumnEditingSupportWrapper(TableViewer viewer, ColumnEditingSupport proxy)
    {
        super(viewer);
        this.viewer = viewer;
        this.proxy = proxy;
        this.editor = proxy.createEditor(viewer.getTable());
    }

    @Override
    protected CellEditor getCellEditor(Object element)
    {
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
            throw new RuntimeException(e);
        }
    }
}
