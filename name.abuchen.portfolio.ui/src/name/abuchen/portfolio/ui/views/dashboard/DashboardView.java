package name.abuchen.portfolio.ui.views.dashboard;

import java.util.function.Consumer;

import name.abuchen.portfolio.model.Dashboard;
import name.abuchen.portfolio.ui.AbstractFinanceView;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.dialogs.ReportingPeriodDialog;
import name.abuchen.portfolio.ui.util.AbstractDropDown;
import name.abuchen.portfolio.ui.util.ContextMenu;
import name.abuchen.portfolio.ui.util.LabelOnly;
import name.abuchen.portfolio.ui.util.SimpleAction;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.InputDialog;
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
import org.eclipse.swt.widgets.ToolBar;

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

            // check if dropped upon itself
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
                Composite filler = (Composite) newParent.getData(FILLER_KEY);
                droppedComposite.moveAbove(filler);

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
            Composite filler = (Composite) dropTarget.getData(FILLER_KEY);
            (filler != null ? filler : dropTarget).setBackground(Display.getDefault().getSystemColor(
                            SWT.COLOR_WIDGET_BACKGROUND));
        }

        @Override
        public void dragLeave(DropTargetEvent event)
        {
            Composite filler = (Composite) dropTarget.getData(FILLER_KEY);
            (filler != null ? filler : dropTarget).setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
        }
    }

    private static final String DELEGATE_KEY = "$delegate"; //$NON-NLS-1$
    private static final String FILLER_KEY = "$filler"; //$NON-NLS-1$

    private DashboardResources resources;
    private Composite container;

    private Dashboard dashboard;
    private DashboardData dashboardData;

    @Override
    protected String getTitle()
    {
        return "Dashboard";
    }

    @Override
    public void notifyModelUpdated()
    {
        this.dashboardData.clearCache();
        updateWidgets();
    }

    @Override
    protected void addButtons(ToolBar toolBar)
    {
        AbstractDropDown.create(toolBar, "Configure Dashboards", Images.SAVE.image(), SWT.NONE, manager -> {
            getClient().getDashboards().forEach(d -> {
                Action action = new SimpleAction(d.getName(), a -> selectDashboard(d));
                action.setChecked(d.equals(dashboard));
                manager.add(action);
            });

            manager.add(new Separator());
            manager.add(new SimpleAction(Messages.ConfigurationNew, a -> createNewDashboard(null)));
            manager.add(new SimpleAction(Messages.ConfigurationDuplicate, a -> createNewDashboard(dashboard)));
            manager.add(new SimpleAction(Messages.ConfigurationRename, a -> renameDashboard(dashboard)));
            manager.add(new SimpleAction(Messages.ConfigurationDelete, a -> deleteDashboard(dashboard)));
        });

        AbstractDropDown.create(toolBar, "Configure", Images.CONFIG.image(), SWT.NONE, manager -> {
            configMenuReportingPeriod(manager);
            manager.add(new SimpleAction("Neue Spalte", a -> createNewColumn()));
        });
    }

    private void configMenuReportingPeriod(IMenuManager manager)
    {
        MenuManager subMenu = new MenuManager("Berichtszeitraum");
        dashboardData.getDefaultReportingPeriods().stream()
                        .forEach(p -> subMenu.add(new SimpleAction(p.toString(), a -> {
                            dashboardData.setDefaultReportingPeriod(p);
                            markDirty();
                            updateWidgets();
                        })));

        subMenu.add(new Separator());
        subMenu.add(new SimpleAction("Neu...", a -> {
            ReportingPeriodDialog dialog = new ReportingPeriodDialog(Display.getDefault().getActiveShell(),
                            dashboardData.getDefaultReportingPeriod());
            if (dialog.open() == ReportingPeriodDialog.OK)
            {
                dashboardData.setDefaultReportingPeriod(dialog.getReportingPeriod());
                markDirty();
                updateWidgets();
            }
        }));

        manager.add(subMenu);
    }

    @Override
    protected Control createBody(Composite parent)
    {
        resources = new DashboardResources(parent);

        dashboardData = make(DashboardData.class);

        dashboard = getClient().getDashboards().findAny().orElseGet(() -> {
            Dashboard newDashboard = createDefaultDashboard();
            getClient().addDashboard(newDashboard);
            markDirty();
            return newDashboard;
        });

        container = new Composite(parent, SWT.NONE);
        container.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));

        selectDashboard(dashboard);

        return container;
    }

    private void buildColumns()
    {
        for (Dashboard.Column column : dashboard.getColumns())
        {
            Composite composite = buildColumn(container, column);

            for (Dashboard.Widget widget : column.getWidgets())
            {
                WidgetFactory factory = WidgetFactory.valueOf(widget.getType());
                if (factory == null)
                    continue;

                buildDelegate(composite, factory, widget);
            }
        }
    }

    private Composite buildColumn(Composite composite, Dashboard.Column column)
    {
        Composite columnControl = new Composite(composite, SWT.NONE);
        columnControl.setBackground(composite.getBackground());
        columnControl.setData(column);

        GridLayoutFactory.fillDefaults().numColumns(1).spacing(0, 0).applyTo(columnControl);
        GridDataFactory.fillDefaults().grab(true, true).applyTo(columnControl);

        addDropListener(columnControl);

        // Each column has an empty composite at the bottom to serve as target
        // for the column context menu. A separate composite is needed because
        // *all* context menus attached to nested composites are always shown.
        Composite filler = new Composite(columnControl, SWT.NONE);
        filler.setBackground(columnControl.getBackground());
        GridDataFactory.fillDefaults().grab(true, true).applyTo(filler);
        columnControl.setData(FILLER_KEY, filler);

        new ContextMenu(filler, manager -> {
            MenuManager subMenu = new MenuManager("Neues Widget");
            for (WidgetFactory type : WidgetFactory.values())
                subMenu.add(new SimpleAction(type.toString(), a -> addNewWidget(columnControl, type)));
            manager.add(subMenu);
            manager.add(new Separator());
            manager.add(new SimpleAction("Spalte löschen", a -> deleteColumn(columnControl)));
        }).hook();

        return columnControl;
    }

    private WidgetDelegate buildDelegate(Composite columnControl, WidgetFactory widgetType, Dashboard.Widget widget)
    {
        WidgetDelegate delegate = widgetType.create(widget, dashboardData);

        Composite element = delegate.createControl(columnControl, resources);
        element.setData(widget);
        element.setData(DELEGATE_KEY, delegate);

        Composite filler = (Composite) columnControl.getData(FILLER_KEY);
        element.moveAbove(filler);

        delegate.attachContextMenu(manager -> widgetMenuAboutToShow(manager, delegate));

        addDragListener(element);
        addDropListener(element);

        for (Control child : element.getChildren())
            addDragListener(child);

        GridDataFactory.fillDefaults().grab(true, false).applyTo(element);
        return delegate;
    }

    private void widgetMenuAboutToShow(IMenuManager manager, WidgetDelegate delegate)
    {
        manager.add(new LabelOnly(delegate.getWidget().getLabel()));
        manager.add(new Separator());

        manager.add(new SimpleAction("Edit label...", a -> {
            InputDialog dialog = new InputDialog(Display.getCurrent().getActiveShell(), "Label umbenennen", "Label",
                            delegate.getWidget().getLabel(), null);

            if (dialog.open() != InputDialog.OK)
                return;

            delegate.getWidget().setLabel(dialog.getValue());
            delegate.update();
        }));

        delegate.configMenuAboutToShow(manager);

        manager.add(new Separator());
        manager.add(new SimpleAction("Löschen", a -> {
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
        }));
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
        for (Control column : container.getChildren())
        {
            for (Control child : ((Composite) column).getChildren())
            {
                WidgetDelegate delegate = (WidgetDelegate) child.getData(DELEGATE_KEY);
                if (delegate != null)
                    delegate.update();
            }
        }
    }

    private void selectDashboard(Dashboard board)
    {
        this.dashboardData.setDashboard(board);
        this.dashboard = board;
        updateTitle(board.getName());

        for (Control column : container.getChildren())
            column.dispose();

        buildColumns();

        GridLayoutFactory.fillDefaults().numColumns(dashboard.getColumns().size()) //
                        .equalWidth(true).spacing(10, 10).applyTo(container);

        container.layout(true);

        updateWidgets();
    }

    private void createNewDashboard(Dashboard template)
    {
        Dashboard newDashboard = template != null ? template.copy() : createDefaultDashboard();

        InputDialog dialog = new InputDialog(Display.getCurrent().getActiveShell(), "Dashboard umbenennen", "Name",
                        newDashboard.getName(), null);

        if (dialog.open() != InputDialog.OK)
            return;

        newDashboard.setName(dialog.getValue());

        getClient().addDashboard(newDashboard);
        markDirty();
        selectDashboard(newDashboard);
    }

    private void renameDashboard(Dashboard board)
    {
        InputDialog dialog = new InputDialog(Display.getCurrent().getActiveShell(), "Dashboard umbenennen", "Name",
                        board.getName(), null);

        if (dialog.open() != InputDialog.OK)
            return;

        board.setName(dialog.getValue());
        markDirty();
        updateTitle(board.getName());
    }

    private void deleteDashboard(Dashboard board)
    {
        getClient().removeDashboard(board);
        markDirty();

        selectDashboard(getClient().getDashboards().findFirst().orElseGet(() -> {
            Dashboard newDashboard = createDefaultDashboard();
            getClient().addDashboard(newDashboard);
            markDirty();
            return newDashboard;
        }));
    }

    private void addNewWidget(Composite columnControl, WidgetFactory widgetType)
    {
        Dashboard.Column column = (Dashboard.Column) columnControl.getData();

        Dashboard.Widget widget = new Dashboard.Widget();
        widget.setLabel(widgetType.toString());
        widget.setType(widgetType.name());
        column.getWidgets().add(widget);

        WidgetDelegate delegate = buildDelegate(columnControl, widgetType, widget);

        markDirty();
        delegate.update();
        columnControl.layout(true);
    }

    private void createNewColumn()
    {
        Dashboard.Column column = new Dashboard.Column();
        dashboard.getColumns().add(column);

        buildColumn(container, column);

        GridLayoutFactory.fillDefaults().numColumns(dashboard.getColumns().size()).equalWidth(true).spacing(10, 10)
                        .applyTo(container);
        container.layout(true);
    }

    private void deleteColumn(Composite columnControl)
    {
        Dashboard.Column column = (Dashboard.Column) columnControl.getData();

        dashboard.getColumns().remove(column);
        markDirty();

        columnControl.dispose();

        GridLayoutFactory.fillDefaults().numColumns(dashboard.getColumns().size()).equalWidth(true).spacing(10, 10)
                        .applyTo(container);
        container.layout(true);
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

        widget = new Dashboard.Widget();
        widget.setType(WidgetFactory.HEADING.name());
        widget.setLabel("Performance Heatmap");
        column.getWidgets().add(widget);

        widget = new Dashboard.Widget();
        widget.setType(WidgetFactory.HEATMAP.name());
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
        widget.getConfiguration().put("REPORTING_PERIOD", "L1Y0");
        column.getWidgets().add(widget);

        widget = new Dashboard.Widget();
        widget.setType(WidgetFactory.CHART.name());
        widget.setLabel("Performance Gesamtportfolio 5 Jahre");
        widget.getConfiguration().put("REPORTING_PERIOD", "L5Y0");
        column.getWidgets().add(widget);

        return newDashboard;
    }
}
