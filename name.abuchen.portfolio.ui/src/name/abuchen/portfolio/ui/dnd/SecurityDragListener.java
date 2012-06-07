package name.abuchen.portfolio.ui.dnd;

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
        IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
        Security security = (Security) selection.getFirstElement();
        SecurityTransfer.getTransfer().setSecurity(security);
    }

    public void dragStart(DragSourceEvent event)
    {
        event.doit = !viewer.getSelection().isEmpty();
    }
}
