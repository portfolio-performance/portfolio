package name.abuchen.portfolio.ui.dnd;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.swt.dnd.DragSourceAdapter;
import org.eclipse.swt.dnd.DragSourceEvent;

import name.abuchen.portfolio.model.Adaptable;
import name.abuchen.portfolio.model.Security;

public class SecurityDragListener extends DragSourceAdapter
{
    private StructuredViewer viewer;

    public SecurityDragListener(StructuredViewer viewer)
    {
        this.viewer = viewer;
    }

    @Override
    public void dragSetData(DragSourceEvent event)
    {
        SecurityTransfer.getTransfer().setSecurities(getSecurities());
    }

    @Override
    public void dragStart(DragSourceEvent event)
    {
        event.doit = getSecurities() != null;
    }

    private List<Security> getSecurities()
    {
        IStructuredSelection selection = (IStructuredSelection) viewer.getStructuredSelection();
        if (selection.isEmpty())
            return null;

        List<Security> selectedSecurities = new ArrayList<>();
        Iterator<?> selectionIterator = selection.iterator();
        while (selectionIterator.hasNext())
        {
            Object object = selectionIterator.next();
            if (object instanceof Security)
            {
                selectedSecurities.add((Security) object);
            }
            else if (object instanceof Adaptable)
            {
                Security selectedSecurity = ((Adaptable) object).adapt(Security.class);
                if (selectedSecurity != null)
                {
                    selectedSecurities.add(selectedSecurity);
                }
            }
            else
            {
                // ignore elements that cannot be dragged
            }
        }

        return selectedSecurities;
    }
}
