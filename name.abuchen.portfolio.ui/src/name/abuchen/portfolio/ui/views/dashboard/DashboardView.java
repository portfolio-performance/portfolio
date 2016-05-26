package name.abuchen.portfolio.ui.views.dashboard;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javax.inject.Inject;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
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
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;

import com.ibm.icu.text.MessageFormat;

import name.abuchen.portfolio.model.Dashboard;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.CurrencyConverterImpl;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.ui.AbstractFinanceView;

public class DashboardView extends AbstractFinanceView
{
    private static final class WidgetDragSourceAdapter extends DragSourceAdapter
    {
        private final LocalSelectionTransfer transfer;
        private final Control dragSource;

        private WidgetDragSourceAdapter(LocalSelectionTransfer transfer, Control control)
        {
            this.transfer = transfer;
            this.dragSource = control;
        }

        @Override
        public void dragSetData(DragSourceEvent event)
        {
            Control widgetComposite = dragSource;
            while (!(widgetComposite.getData() instanceof Dashboard.Widget))
                widgetComposite = widgetComposite.getParent();

            transfer.setSelection(new StructuredSelection(widgetComposite));
        }

        @Override
        public void dragStart(DragSourceEvent event)
        {
            Control control = ((DragSource) event.getSource()).getControl();

            while (!(control.getData() instanceof Dashboard.Widget))
                control = control.getParent();

            Point size = control.getSize();
            GC gc = new GC(control);
            Image image = new Image(control.getDisplay(), size.x, size.y);
            gc.copyArea(image, 0, 0);
            gc.dispose();
            event.image = image;
        }
    }

    private static final class WidgetDropTargetAdapter extends DropTargetAdapter
    {
        private final LocalSelectionTransfer transfer;
        private final Composite dropTarget;
        private final Consumer<Dashboard.Widget> listener;

        private WidgetDropTargetAdapter(LocalSelectionTransfer transfer, Composite dropTarget,
                        Consumer<Dashboard.Widget> listener)
        {
            this.transfer = transfer;
            this.dropTarget = dropTarget;
            this.listener = listener;
        }

        @Override
        public void drop(final DropTargetEvent event)
        {
            Object droppedElement = ((StructuredSelection) transfer.getSelection()).getFirstElement();

            if (!(droppedElement instanceof Composite))
                return;

            Composite droppedComposite = (Composite) droppedElement;
            if (droppedComposite.equals(dropTarget))
                return;

            Dashboard.Widget droppedWidget = (Dashboard.Widget) droppedComposite.getData();
            if (droppedWidget == null)
                throw new IllegalArgumentException();

            Composite oldParent = droppedComposite.getParent();
            Dashboard.Column oldColumn = (Dashboard.Column) oldParent.getData();
            if (oldColumn == null)
                throw new IllegalArgumentException();

            Composite newParent = dropTarget;
            while (!(newParent.getData() instanceof Dashboard.Column))
                newParent = newParent.getParent();
            Dashboard.Column newColumn = (Dashboard.Column) newParent.getData();

            droppedComposite.setParent(newParent);

            if (dropTarget.getData() instanceof Dashboard.Widget)
            {
                // dropped on another widget
                droppedComposite.moveAbove(dropTarget);

                Dashboard.Widget dropTargetWidget = (Dashboard.Widget) dropTarget.getData();
                oldColumn.getWidgets().remove(droppedWidget);
                newColumn.getWidgets().add(newColumn.getWidgets().indexOf(dropTargetWidget), droppedWidget);
            }
            else if (dropTarget.getData() instanceof Dashboard.Column)
            {
                // dropped on another column
                droppedComposite.moveBelow(null);

                oldColumn.getWidgets().remove(droppedWidget);
                newColumn.getWidgets().add(droppedWidget);
            }
            else
            {
                throw new IllegalArgumentException();
            }

            listener.accept(droppedWidget);

            oldParent.layout();
            newParent.layout();
        }

        @Override
        public void dragEnter(DropTargetEvent event)
        {
            dropTarget.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
        }

        @Override
        public void dragLeave(DropTargetEvent event)
        {
            dropTarget.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
        }
    }

    private static final String DELEGATE_KEY = "$delegate"; //$NON-NLS-1$

    @Inject
    private ExchangeRateProviderFactory exchangeRateFactory;

    private Composite container;

    private Dashboard dashboard;
    private DashboardData dashboardData;

    private List<WidgetDelegate> delegates = new ArrayList<>();

    @Override
    protected String getTitle()
    {
        return "Dashboard";
    }

    @Override
    public void notifyModelUpdated()
    {
        updateWidgets();
    }

    @Override
    protected Control createBody(Composite parent)
    {
        CurrencyConverter converter = new CurrencyConverterImpl(exchangeRateFactory, getClient().getBaseCurrency());
        dashboardData = new DashboardData(getClient(), converter);

        dashboard = getClient().getDashboards().findAny().orElseGet(() -> createDefaultDashboard());
        updateTitle("Dashboard: " + dashboard.getName());

        DashboardResources resources = new DashboardResources(parent);

        container = new Composite(parent, SWT.NONE);
        container.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
        GridLayoutFactory.fillDefaults().numColumns(dashboard.getColumns().size()).equalWidth(true).spacing(10, 10)
                        .applyTo(container);

        for (Dashboard.Column column : dashboard.getColumns())
        {
            Composite composite = createColumn(container);
            composite.setData(column);

            for (Dashboard.Widget widget : column.getWidgets())
            {
                WidgetFactory factory = WidgetFactory.valueOf(widget.getType());
                if (factory == null)
                    continue;

                WidgetDelegate delegate = factory.create(widget, dashboardData);

                this.delegates.add(delegate);

                Composite element = delegate.createControl(composite, resources);
                element.setData(widget);
                element.setData(DELEGATE_KEY, delegate);

                delegate.attachContextMenu(manager -> widgetMenuAboutToShow(manager, delegate));

                addDragListener(element);
                addDropListener(element);

                for (Control child : element.getChildren())
                    addDragListener(child);

                GridDataFactory.fillDefaults().grab(true, false).applyTo(element);
            }
        }

        container.layout();

        updateWidgets();

        return container;
    }

    private Dashboard createDefaultDashboard()
    {
        Dashboard newDashboard = new Dashboard();
        newDashboard.setName("Letztes Jahr");

        Dashboard.Column column = new Dashboard.Column();
        newDashboard.getColumns().add(column);

        Dashboard.Widget widget = new Dashboard.Widget();
        widget.setType(WidgetFactory.HEADING.name());
        widget.setLabel("Kennzahlen");
        column.getWidgets().add(widget);

        widget = new Dashboard.Widget();
        widget.setType(WidgetFactory.TTWROR.name());
        widget.setLabel("True-Time Weighted Rate of Return");
        column.getWidgets().add(widget);

        widget = new Dashboard.Widget();
        widget.setType(WidgetFactory.IRR.name());
        widget.setLabel("Interner Zinsfuß");
        column.getWidgets().add(widget);

        widget = new Dashboard.Widget();
        widget.setType(WidgetFactory.ABSOLUTE_CHANGE.name());
        widget.setLabel("Absolute Change");
        column.getWidgets().add(widget);

        widget = new Dashboard.Widget();
        widget.setType(WidgetFactory.DELTA.name());
        widget.setLabel("Delta");
        column.getWidgets().add(widget);

        column = new Dashboard.Column();
        newDashboard.getColumns().add(column);

        widget = new Dashboard.Widget();
        widget.setType(WidgetFactory.HEADING.name());
        widget.setLabel("Risikokennzahlen");
        column.getWidgets().add(widget);

        widget = new Dashboard.Widget();
        widget.setType(WidgetFactory.MAXDRAWDOWN.name());
        widget.setLabel("Maximaler Drawdown");
        column.getWidgets().add(widget);

        widget = new Dashboard.Widget();
        widget.setType(WidgetFactory.MAXDRAWDOWNDURATION.name());
        widget.setLabel("Maximaler Drawdown Duration");
        column.getWidgets().add(widget);

        widget = new Dashboard.Widget();
        widget.setType(WidgetFactory.VOLATILITY.name());
        widget.setLabel("Volatilität");
        column.getWidgets().add(widget);

        widget = new Dashboard.Widget();
        widget.setType(WidgetFactory.SEMIVOLATILITY.name());
        widget.setLabel("Semivolatilität");
        column.getWidgets().add(widget);

        column = new Dashboard.Column();
        newDashboard.getColumns().add(column);

        widget = new Dashboard.Widget();
        widget.setType(WidgetFactory.HEADING.name());
        widget.setLabel("Berechnung");
        column.getWidgets().add(widget);

        widget = new Dashboard.Widget();
        widget.setType(WidgetFactory.CALCULATION.name());
        widget.setLabel("Berechnung");
        column.getWidgets().add(widget);

        widget = new Dashboard.Widget();
        widget.setType(WidgetFactory.HEADING.name());
        widget.setLabel("Charts");
        column.getWidgets().add(widget);

        widget = new Dashboard.Widget();
        widget.setType(WidgetFactory.CHART.name());
        widget.setLabel("Performance Gesamtportfolio 1 Jahr");
        column.getWidgets().add(widget);

        widget = new Dashboard.Widget();
        widget.setType(WidgetFactory.CHART.name());
        widget.setLabel("Performance Gesamtportfolio 5 Jahre");
        widget.getConfiguration().put("period", "L5Y0");
        column.getWidgets().add(widget);

        return newDashboard;
    }

    private Composite createColumn(Composite composite)
    {
        Composite column = new Composite(composite, SWT.NONE);
        GridLayoutFactory.fillDefaults().numColumns(1).spacing(0, 0).applyTo(column);
        GridDataFactory.fillDefaults().grab(true, true).applyTo(column);

        addDropListener(column);

        return column;
    }

    private void widgetMenuAboutToShow(IMenuManager manager, WidgetDelegate delegate)
    {
        manager.add(new Action(MessageFormat.format("Delete ''{0}''", delegate.getWidget().getLabel()))
        {
            @Override
            public void run()
            {
                Composite composite = findCompositeFor(delegate);
                if (composite == null)
                    throw new IllegalArgumentException();

                Composite parent = composite.getParent();
                Dashboard.Column column = (Dashboard.Column) parent.getData();

                if (!column.getWidgets().remove(delegate.getWidget()))
                    throw new IllegalArgumentException();

                composite.dispose();
                parent.layout();
                markDirty();
            }
        });
    }

    private Composite findCompositeFor(WidgetDelegate delegate)
    {
        for (Control column : container.getChildren())
        {
            if (!(column instanceof Composite))
                continue;

            for (Control child : ((Composite) column).getChildren())
            {
                if (!(child instanceof Composite))
                    continue;

                if (delegate.equals(child.getData(DELEGATE_KEY)))
                    return (Composite) child;
            }
        }

        return null;
    }

    private void addDragListener(Control control)
    {
        LocalSelectionTransfer transfer = LocalSelectionTransfer.getTransfer();

        DragSourceAdapter dragAdapter = new WidgetDragSourceAdapter(transfer, control);

        DragSource dragSource = new DragSource(control, DND.DROP_MOVE | DND.DROP_COPY);
        dragSource.setTransfer(new Transfer[] { transfer });
        dragSource.addDragListener(dragAdapter);
    }

    private void addDropListener(Composite parent)
    {
        LocalSelectionTransfer transfer = LocalSelectionTransfer.getTransfer();

        DropTargetAdapter dragAdapter = new WidgetDropTargetAdapter(transfer, parent, w -> markDirty());

        DropTarget dropTarget = new DropTarget(parent, DND.DROP_MOVE | DND.DROP_COPY);
        dropTarget.setTransfer(new Transfer[] { transfer });
        dropTarget.addDropListener(dragAdapter);
    }

    private void updateWidgets()
    {
        delegates.forEach(d -> d.update());
    }

}
