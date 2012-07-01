package name.abuchen.portfolio.ui.dnd;

import name.abuchen.portfolio.model.Adaptable;
import name.abuchen.portfolio.model.Security;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.swt.dnd.DragSourceAdapter;
import org.eclipse.swt.dnd.DragSourceEvent;

public class SecurityDragListener extends DragSourceAdapter
{
    private StructuredViewer viewer;

    public SecurityDragListener(StructuredViewer viewer)
    {
        this.viewer = viewer;
    }

    public void dragFinished(DragSourceEvent event)
    {
        if (!event.doit)
            return;

        // TODO: remove from watch list if security was moved
    }

    public void dragSetData(DragSourceEvent event)
    {
        SecurityTransfer.getTransfer().setSecurity(getSecurity());
    }

    public void dragStart(DragSourceEvent event)
    {
        event.doit = getSecurity() != null;
    }

    private Security getSecurity()
    {
        IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
        if (selection.isEmpty())
            return null;

        Object element = selection.getFirstElement();

        if (element instanceof Security)
        {
            return (Security) element;
        }
        else if (element instanceof Adaptable)
        {
            return ((Adaptable) element).adapt(Security.class);
        }
        else
        {
            return null;
        }
    }
}
