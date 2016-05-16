package name.abuchen.portfolio.ui.views.dashboard;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSource;
import org.eclipse.swt.dnd.DragSourceAdapter;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;

import name.abuchen.portfolio.model.Dashboard;
import name.abuchen.portfolio.ui.AbstractFinanceView;

public class DashboardView extends AbstractFinanceView
{
    @Override
    protected String getTitle()
    {
        return "Berechnung";
    }

    @Override
    protected Control createBody(Composite parent)
    {
        Composite composite = new Composite(parent, SWT.NONE);
        composite.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
        GridLayoutFactory.fillDefaults().numColumns(3).equalWidth(true).applyTo(composite);

        Composite column1 = createColumn(composite);
        createKPI(column1, "True-Time Weighted Rate of Return", "10,88%");
        createKPI(column1, "Interner Zinsfuß (IZF)", "9,88%");

        Composite column2 = createColumn(composite);
        createKPI(column2, "Maximum Drawdown", "20,16%");

        Composite column3 = createColumn(composite);
        createKPI(column3, "Volatilität", "0,39%");

        return composite;
    }

    private Composite createColumn(Composite composite)
    {
        Composite column = new Composite(composite, SWT.NONE);
        column.setData(new Dashboard.Column());
        addDropListener(column);

        column.setLayout(new RowLayout(SWT.VERTICAL));
        GridDataFactory.fillDefaults().grab(true, true).applyTo(column);
        return column;
    }

    private Composite createKPI(Composite parent, String label, String value)
    {
        Composite kpi = new Composite(parent, SWT.NONE);
        kpi.setData(new Dashboard.Widget());
        addDragListener(kpi);
        addDropListener(kpi);

        kpi.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
        kpi.setLayout(new RowLayout(SWT.VERTICAL));
        Label lbl = new Label(kpi, SWT.NONE);
        lbl.setText(label);
        Label vl = new Label(kpi, SWT.NONE);
        vl.setText(value);
        return kpi;
    }

    private void addDragListener(final Control control)
    {
        final LocalSelectionTransfer transfer = LocalSelectionTransfer.getTransfer();

        final DragSourceAdapter dragAdapter = new DragSourceAdapter()
        {
            @Override
            public void dragSetData(final DragSourceEvent event)
            {
                transfer.setSelection(new StructuredSelection(control));
            }

            @Override
            public void dragStart(DragSourceEvent dragSourceEvent)
            {
                Composite composite = (Composite) ((DragSource) dragSourceEvent.getSource()).getControl();
                Point compositeSize = composite.getSize();
                GC gc = new GC(composite);
                Image image = new Image(Display.getCurrent(), compositeSize.x, compositeSize.y);
                gc.copyArea(image, 0, 0);
                dragSourceEvent.image = image;
            }
        };

        final DragSource dragSource = new DragSource(control, DND.DROP_MOVE | DND.DROP_COPY);
        dragSource.setTransfer(new Transfer[] { transfer });
        dragSource.addDragListener(dragAdapter);
    }

    private void addDropListener(final Composite parent)
    {
        LocalSelectionTransfer transfer = LocalSelectionTransfer.getTransfer();

        DropTargetAdapter dragAdapter = new DropTargetAdapter()
        {
            @Override
            public void drop(final DropTargetEvent event)
            {
                Control droppedObj = (Control) ((StructuredSelection) transfer.getSelection()).getFirstElement();

                if (droppedObj.equals(parent))
                    return;

                Composite oldParent = droppedObj.getParent();

                Composite droppedComposite = (Composite) droppedObj;

                Composite p = parent;
                while (!(p.getData() instanceof Dashboard.Column))
                    p = p.getParent();

                droppedComposite.setParent(p);

                if (parent.getData() instanceof Dashboard.Widget)
                    droppedComposite.moveAbove(parent);

                if (parent.getData() instanceof Dashboard.Column)
                    droppedComposite.moveBelow(null);

                oldParent.layout();
                p.layout();
            }

            @Override
            public void dragEnter(DropTargetEvent event)
            {
                parent.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_GRAY));
            }

            @Override
            public void dragLeave(DropTargetEvent event)
            {
                parent.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
            }
        };

        DropTarget dropTarget = new DropTarget(parent, DND.DROP_MOVE | DND.DROP_COPY);
        dropTarget.setTransfer(new Transfer[] { transfer });
        dropTarget.addDropListener(dragAdapter);
    }

}
