package name.abuchen.portfolio.ui.dnd;

import java.util.Objects;

import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Control;

// inspired by the Eclipse Marketplace Client
// https://github.com/eclipse/epp.mpc/blob/master/org.eclipse.epp.mpc.ui/src/org/eclipse/epp/internal/mpc/ui/wizards/MarketplaceDropAdapter.java
/* package */ abstract class AbstractDropAdapter extends DropTargetAdapter
{
    private final Transfer transfer;

    public AbstractDropAdapter(Transfer transfer, Control control)
    {
        this.transfer = Objects.requireNonNull(transfer);
        attach(control);
    }

    private void attach(Control control)
    {
        DropTarget dropTarget = findDropTarget(control);

        if (dropTarget != null)
        {
            registerWithExistingTarget(dropTarget);
        }
        else
        {
            dropTarget = new DropTarget(control, DND.DROP_MOVE | DND.DROP_COPY | DND.DROP_LINK | DND.DROP_DEFAULT);
            dropTarget.setTransfer(this.transfer);
        }

        dropTarget.addDropListener(this);
    }

    private void registerWithExistingTarget(DropTarget target)
    {
        Transfer[] transfers = target.getTransfer();
        boolean exists = false;
        if (transfers != null)
        {
            for (Transfer t : transfers)
            {
                if (t.equals(this.transfer))
                {
                    exists = true;
                    break;
                }
            }
            if (!exists)
            {
                Transfer[] newTransfers = new Transfer[transfers.length + 1];
                System.arraycopy(transfers, 0, newTransfers, 0, transfers.length);
                newTransfers[transfers.length] = this.transfer;
                target.setTransfer(newTransfers);
            }
        }
    }

    private DropTarget findDropTarget(Control control)
    {
        if (control.isDisposed())
            return null;
        Object object = control.getData(DND.DROP_TARGET_KEY);
        if (object instanceof DropTarget dropTarget)
            return dropTarget;
        return null;
    }

    @Override
    public final void dragEnter(DropTargetEvent e)
    {
        updateDragDetails(e);
    }

    @Override
    public final void dragOver(DropTargetEvent e)
    {
        updateDragDetails(e);
    }

    @Override
    public final void dragLeave(DropTargetEvent e)
    {
        if (e.detail == DND.DROP_NONE)
            setDropOperation(e);
    }

    @Override
    public final void dropAccept(DropTargetEvent e)
    {
        updateDragDetails(e);
    }

    @Override
    public final void dragOperationChanged(DropTargetEvent e)
    {
        updateDragDetails(e);
    }

    private final void setDropOperation(DropTargetEvent e)
    {
        int allowedOperations = e.operations;
        for (int op : new int[] { DND.DROP_DEFAULT, DND.DROP_COPY, DND.DROP_MOVE, DND.DROP_LINK })
        {
            if ((allowedOperations & op) != 0)
            {
                e.detail = op;
                return;
            }
        }
        e.detail = allowedOperations;
    }

    private final void updateDragDetails(DropTargetEvent e)
    {
        if (isValidEvent(e))
            setDropOperation(e);
    }

    protected boolean isValidEvent(DropTargetEvent e)
    {
        return this.transfer.isSupportedType(e.currentDataType);
    }

    @Override
    public final void drop(DropTargetEvent event)
    {
        if (!this.transfer.isSupportedType(event.currentDataType))
            return;

        if (event.data == null)
        {
            event.detail = DND.DROP_NONE;
            return;
        }

        if (!isValidEvent(event))
        {
            event.detail = DND.DROP_NONE;
            return;
        }

        doDrop(event);
    }

    protected abstract void doDrop(DropTargetEvent event);
}
