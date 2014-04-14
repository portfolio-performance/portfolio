package name.abuchen.portfolio.ui.util;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.swt.widgets.Composite;

public abstract class ColumnEditingSupport
{
    public interface ModificationListener
    {
        public void onModified(Object element, Object newValue, Object oldValue);
    }

    private static List<ModificationListener> listeners;

    public abstract CellEditor createEditor(Composite composite);

    public boolean canEdit(Object element)
    {
        return true;
    }

    public abstract Object getValue(Object element) throws Exception;

    public abstract void setValue(Object element, Object value) throws Exception;

    public void addListener(ModificationListener listener)
    {
        if (listeners == null)
            listeners = new ArrayList<ModificationListener>();
        listeners.add(listener);
    }

    protected void notify(Object element, Object newValue, Object oldValue)
    {
        if (listeners != null)
        {
            for (ModificationListener listener : listeners)
                listener.onModified(element, newValue, oldValue);
        }
    }
}
