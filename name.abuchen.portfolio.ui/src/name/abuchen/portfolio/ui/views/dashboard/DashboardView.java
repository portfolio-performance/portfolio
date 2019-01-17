package name.abuchen.portfolio.ui.views.dashboard;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.e4.ui.di.UISynchronize;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ContributionItem;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPreferenceStore;
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
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;

import name.abuchen.portfolio.model.Dashboard;
import name.abuchen.portfolio.model.Dashboard.Widget;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.jobs.AbstractClientJob;
import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.ui.util.ConfirmAction;
import name.abuchen.portfolio.ui.util.ContextMenu;
import name.abuchen.portfolio.ui.util.DropDown;
import name.abuchen.portfolio.ui.util.InfoToolTip;
import name.abuchen.portfolio.ui.util.SimpleAction;
import name.abuchen.portfolio.ui.views.AbstractHistoricView;

public class DashboardView extends AbstractHistoricView
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
            (filler != null ? filler : dropTarget)
                            .setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
        }

        @Override
        public void dragLeave(DropTargetEvent event)
        {
            Composite filler = (Composite) dropTarget.getData(FILLER_KEY);
            (filler != null ? filler : dropTarget).setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
        }
    }

    public static final String INFO_MENU_GROUP_NAME = "info"; //$NON-NLS-1$

    private static final String SELECTED_DASHBOARD_KEY = "selected-dashboard"; //$NON-NLS-1$
    private static final String DELEGATE_KEY = "$delegate"; //$NON-NLS-1$
    private static final String FILLER_KEY = "$filler"; //$NON-NLS-1$

    @Inject
    private IPreferenceStore preferences;

    @Inject
    private UISynchronize sync;

    private DashboardResources resources;
    private Composite container;

    private Dashboard dashboard;
    private DashboardData dashboardData;

    @Override
    protected String getDefaultTitle()
    {
        return Messages.LabelDashboard;
    }

    @Override
    public void notifyModelUpdated()
    {
        this.dashboardData.clearCache();
        updateWidgets();
    }

    @Override
    public void reportingPeriodUpdated()
    {
        this.dashboardData.setDefaultReportingPeriod(getReportingPeriod());
        this.dashboardData.clearResultCache();
        updateWidgets();
    }

    @Override
    protected void addViewButtons(ToolBarManager toolBar)
    {
        createDashboardToolItems(toolBar);
    }

    @Override
    protected void addButtons(ToolBarManager toolBar)
    {
        super.addButtons(toolBar);

        toolBar.add(new DropDown(Messages.MenuConfigureCurrentDashboard, Images.CONFIG, SWT.NONE, manager -> manager
                        .add(new SimpleAction(Messages.MenuNewDashboardColumn, a -> createNewColumn()))));
    }

    private void createDashboardToolItems(ToolBarManager toolBar)
    {
        int[] index = { 0 };
        getClient().getDashboards().forEach(board -> toolBar.add(createToolItem(index[0]++, board)));

        Action newAction = new SimpleAction(Messages.MenuNewDashboard, a -> createNewDashboard(null));
        newAction.setImageDescriptor(Images.VIEW_PLUS.descriptor());
        toolBar.add(newAction);
    }

    private ContributionItem createToolItem(int index, Dashboard board)
    {
        DropDown toolItem = new DropDown(board.getName(),
                        board.equals(dashboard) ? Images.VIEW_SELECTED : Images.VIEW, SWT.DROP_DOWN);

        toolItem.setMenuListener(manager -> {
            if (!board.equals(dashboard))
            {
                manager.add(new SimpleAction(Messages.MenuShow, a -> selectDashboard(board)));
                manager.add(new Separator());
            }

            manager.add(new SimpleAction(Messages.ConfigurationDuplicate, a -> createNewDashboard(board)));
            manager.add(new SimpleAction(Messages.ConfigurationRename, a -> renameDashboard(board)));
            manager.add(new ConfirmAction(Messages.ConfigurationDelete,
                            MessageFormat.format(Messages.ConfigurationDeleteConfirm, board.getName()),
                            a -> deleteDashboard(board)));

            if (index > 0)
            {
                manager.add(new Separator());
                manager.add(new SimpleAction(Messages.ChartBringToFront, a -> bringToFrontDashboard(board)));
            }
        });

        toolItem.setDefaultAction(new SimpleAction(Messages.MenuShow, a -> selectDashboard(board)));

        return toolItem;
    }

    private void recreateDashboardToolItems()
    {
        ToolBarManager toolBar = getViewToolBarManager();
        if (toolBar.getControl().isDisposed())
            return;

        toolBar.removeAll();

        createDashboardToolItems(toolBar);

        toolBar.update(true);
    }

    @Override
    protected Control createBody(Composite parent)
    {
        resources = new DashboardResources(parent);

        dashboardData = make(DashboardData.class);
        dashboardData.setDefaultReportingPeriods(getPart().getClientInput().getReportingPeriods());
        dashboardData.setDefaultReportingPeriod(getReportingPeriod());

        int indexOfSelectedDashboard = Math.max(0, preferences.getInt(SELECTED_DASHBOARD_KEY));

        dashboard = getClient().getDashboards() //
                        .skip(indexOfSelectedDashboard) //
                        .findFirst().orElseGet(() -> {
                            Dashboard newDashboard = createDefaultDashboard();
                            getClient().addDashboard(newDashboard);
                            markDirty();
                            createDashboardToolItems(getToolBarManager());
                            return newDashboard;
                        });

        container = new Composite(parent, SWT.NONE);
        container.setBackground(Colors.WHITE);

        selectDashboard(dashboard);

        container.addDisposeListener(e -> preferences.setValue(SELECTED_DASHBOARD_KEY,
                        getClient().getDashboards().collect(Collectors.toList()).indexOf(dashboard)));

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
            MenuManager subMenu = new MenuManager(Messages.MenuNewWidget);
            manager.add(subMenu);

            Map<String, MenuManager> group2menu = new HashMap<>();
            group2menu.put(null, subMenu);

            for (WidgetFactory type : WidgetFactory.values())
            {
                MenuManager mm = group2menu.computeIfAbsent(type.getGroup(), group -> {
                    MenuManager groupMenu = new MenuManager(group);
                    subMenu.add(groupMenu);
                    return groupMenu;
                });
                mm.add(new SimpleAction(type.getLabel(), a -> addNewWidget(columnControl, type)));
            }

            manager.add(new Separator());
            manager.add(new SimpleAction(Messages.MenuAddNewDashboardColumnLeft,
                            a -> createNewColumn(column, columnControl)));
            manager.add(new SimpleAction(Messages.MenuDeleteDashboardColumn, a -> deleteColumn(columnControl)));
        }).hook();

        return columnControl;
    }

    private WidgetDelegate<?> buildDelegate(Composite columnControl, WidgetFactory widgetType, Dashboard.Widget widget)
    {
        WidgetDelegate<?> delegate = widgetType.create(widget, dashboardData);

        Composite element = delegate.createControl(columnControl, resources);
        element.setData(widget);
        element.setData(DELEGATE_KEY, delegate);

        Composite filler = (Composite) columnControl.getData(FILLER_KEY);
        element.moveAbove(filler);

        new ContextMenu(delegate.getTitleControl(), manager -> widgetMenuAboutToShow(manager, delegate)).hook();
        InfoToolTip.attach(delegate.getTitleControl(), () -> buildToolTip(delegate));

        addDragListener(element);
        addDropListener(element);

        for (Control child : element.getChildren())
            addDragListener(child);

        GridDataFactory.fillDefaults().grab(true, false).applyTo(element);
        return delegate;
    }

    private String buildToolTip(WidgetDelegate<?> delegate)
    {
        StringJoiner text = new StringJoiner("\n"); //$NON-NLS-1$
        delegate.getWidgetConfigs().forEach(c -> text.add(c.getLabel()));
        return text.toString();
    }

    private void widgetMenuAboutToShow(IMenuManager manager, WidgetDelegate<?> delegate)
    {
        manager.add(new Separator(INFO_MENU_GROUP_NAME));
        manager.add(new Separator("edit")); //$NON-NLS-1$

        delegate.getWidgetConfigs().forEach(c -> c.menuAboutToShow(manager));

        manager.add(new Separator());
        manager.add(new SimpleAction(Messages.MenuDeleteWidget, a -> {
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

    private Composite findCompositeFor(WidgetDelegate<?> delegate)
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
        dragSource.setTransfer(transfer);
        dragSource.addDragListener(dragAdapter);
    }

    private void addDropListener(Composite parent)
    {
        LocalSelectionTransfer transfer = LocalSelectionTransfer.getTransfer();

        DropTargetAdapter dragAdapter = new WidgetDropTargetAdapter(transfer, parent, w -> markDirty());

        DropTarget dropTarget = new DropTarget(parent, DND.DROP_MOVE | DND.DROP_COPY);
        dropTarget.setTransfer(transfer);
        dropTarget.addDropListener(dragAdapter);
    }

    private void updateWidgets()
    {
        // use currentCache for updating the existing results of the tasks and
        // calculating new ones
        Map<Widget, Object> currentCache = this.dashboardData.getResultCache();

        Map<WidgetDelegate<Object>, Supplier<Object>> tasks = new HashMap<>();
        for (Control column : container.getChildren())
        {
            for (Control child : ((Composite) column).getChildren())
            {
                @SuppressWarnings("unchecked")
                WidgetDelegate<Object> delegate = (WidgetDelegate<Object>) child.getData(DELEGATE_KEY);
                if (delegate != null)
                {
                    Object data = currentCache.get(delegate.getWidget());

                    if (DashboardData.EMPTY_RESULT.equals(data))
                        delegate.update(null);
                    else if (data != null)
                        delegate.update(data);
                    else
                        tasks.put(delegate, delegate.getUpdateTask());
                }
            }
        }

        if (!tasks.isEmpty())
        {
            new AbstractClientJob(getClient(), Messages.MsgUpdatingDashboardData)
            {
                @Override
                protected IStatus run(IProgressMonitor monitor)
                {
                    Map<WidgetDelegate<Object>, Object> data = new HashMap<>();

                    for (Map.Entry<WidgetDelegate<Object>, Supplier<Object>> task : tasks.entrySet())
                    {
                        try
                        {
                            data.put(task.getKey(), task.getValue().get());
                        }
                        catch (Exception e)
                        {
                            // continue calculating the dashboard data
                            PortfolioPlugin.log(e);
                        }
                    }

                    sync.asyncExec(() -> data.entrySet().stream()
                                    .filter(entry -> !entry.getKey().getTitleControl().isDisposed()).forEach(entry -> {
                                        entry.getKey().update(entry.getValue());

                                        if (entry.getValue() == null)
                                            currentCache.put(entry.getKey().getWidget(), DashboardData.EMPTY_RESULT);
                                        else
                                            currentCache.put(entry.getKey().getWidget(), entry.getValue());
                                    }));

                    return Status.OK_STATUS;
                }

            }.schedule();
        }
    }

    private void selectDashboard(Dashboard board)
    {
        this.dashboardData.setDashboard(board);
        this.dashboard = board;
        updateTitle(board.getName());

        recreateDashboardToolItems();

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

        InputDialog dialog = new InputDialog(Display.getCurrent().getActiveShell(), Messages.ConfigurationNew,
                        Messages.ColumnName, newDashboard.getName(), null);

        if (dialog.open() != InputDialog.OK)
            return;

        newDashboard.setName(dialog.getValue());

        getClient().addDashboard(newDashboard);
        markDirty();

        selectDashboard(newDashboard);
    }

    private void renameDashboard(Dashboard board)
    {
        InputDialog dialog = new InputDialog(Display.getCurrent().getActiveShell(), Messages.MenuRenameDashboard,
                        Messages.ColumnName, board.getName(), null);

        if (dialog.open() != InputDialog.OK)
            return;

        board.setName(dialog.getValue());
        markDirty();
        updateTitle(board.getName());

        recreateDashboardToolItems();
    }

    private void deleteDashboard(Dashboard board)
    {
        getClient().removeDashboard(board);
        markDirty();

        recreateDashboardToolItems();

        selectDashboard(getClient().getDashboards().findFirst().orElseGet(() -> {
            Dashboard newDashboard = createDefaultDashboard();
            getClient().addDashboard(newDashboard);
            markDirty();
            return newDashboard;
        }));
    }

    private void bringToFrontDashboard(Dashboard board)
    {
        getClient().removeDashboard(board);
        getClient().addDashboard(0, board);
        markDirty();

        recreateDashboardToolItems();
    }

    private void addNewWidget(Composite columnControl, WidgetFactory widgetType)
    {
        Dashboard.Column column = (Dashboard.Column) columnControl.getData();

        Dashboard.Widget widget = new Dashboard.Widget();
        widget.setLabel(widgetType.getLabel());
        widget.setType(widgetType.name());
        column.getWidgets().add(widget);

        WidgetDelegate<?> delegate = buildDelegate(columnControl, widgetType, widget);

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

    private void createNewColumn(Dashboard.Column beforeColumn, Composite beforeColumnControl)
    {
        int index = dashboard.getColumns().indexOf(beforeColumn);

        Dashboard.Column newColumn = new Dashboard.Column();
        dashboard.getColumns().add(index, newColumn);

        buildColumn(container, newColumn).moveAbove(beforeColumnControl);

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
        newDashboard.setName(Messages.LabelDashboard);

        newDashboard.getConfiguration().put(Dashboard.Config.REPORTING_PERIOD.name(), "L1Y0"); //$NON-NLS-1$

        Dashboard.Column column = new Dashboard.Column();
        newDashboard.getColumns().add(column);

        Dashboard.Widget widget = new Dashboard.Widget();
        widget.setType(WidgetFactory.HEADING.name());
        widget.setLabel(Messages.LabelKeyIndicators);
        column.getWidgets().add(widget);

        widget = new Dashboard.Widget();
        widget.setType(WidgetFactory.TTWROR.name());
        widget.setLabel(WidgetFactory.TTWROR.getLabel());
        column.getWidgets().add(widget);

        widget = new Dashboard.Widget();
        widget.setType(WidgetFactory.IRR.name());
        widget.setLabel(WidgetFactory.IRR.getLabel());
        column.getWidgets().add(widget);

        widget = new Dashboard.Widget();
        widget.setType(WidgetFactory.ABSOLUTE_CHANGE.name());
        widget.setLabel(WidgetFactory.ABSOLUTE_CHANGE.getLabel());
        column.getWidgets().add(widget);

        widget = new Dashboard.Widget();
        widget.setType(WidgetFactory.DELTA.name());
        widget.setLabel(WidgetFactory.DELTA.getLabel());
        column.getWidgets().add(widget);

        widget = new Dashboard.Widget();
        widget.setType(WidgetFactory.HEADING.name());
        widget.setLabel(Messages.LabelTTWROROneDay);
        column.getWidgets().add(widget);

        widget = new Dashboard.Widget();
        widget.setType(WidgetFactory.TTWROR.name());
        widget.setLabel(WidgetFactory.TTWROR.getLabel());
        widget.getConfiguration().put(Dashboard.Config.REPORTING_PERIOD.name(), "T1"); //$NON-NLS-1$
        column.getWidgets().add(widget);

        widget = new Dashboard.Widget();
        widget.setType(WidgetFactory.ABSOLUTE_CHANGE.name());
        widget.setLabel(WidgetFactory.ABSOLUTE_CHANGE.getLabel());
        widget.getConfiguration().put(Dashboard.Config.REPORTING_PERIOD.name(), "T1"); //$NON-NLS-1$
        column.getWidgets().add(widget);

        column = new Dashboard.Column();
        newDashboard.getColumns().add(column);

        widget = new Dashboard.Widget();
        widget.setType(WidgetFactory.HEADING.name());
        widget.setLabel(Messages.LabelRiskIndicators);
        column.getWidgets().add(widget);

        widget = new Dashboard.Widget();
        widget.setType(WidgetFactory.MAXDRAWDOWN.name());
        widget.setLabel(WidgetFactory.MAXDRAWDOWN.getLabel());
        column.getWidgets().add(widget);

        widget = new Dashboard.Widget();
        widget.setType(WidgetFactory.MAXDRAWDOWNDURATION.name());
        widget.setLabel(WidgetFactory.MAXDRAWDOWNDURATION.getLabel());
        column.getWidgets().add(widget);

        widget = new Dashboard.Widget();
        widget.setType(WidgetFactory.VOLATILITY.name());
        widget.setLabel(WidgetFactory.VOLATILITY.getLabel());
        column.getWidgets().add(widget);

        widget = new Dashboard.Widget();
        widget.setType(WidgetFactory.SEMIVOLATILITY.name());
        widget.setLabel(WidgetFactory.SEMIVOLATILITY.getLabel());
        column.getWidgets().add(widget);

        column = new Dashboard.Column();
        newDashboard.getColumns().add(column);

        widget = new Dashboard.Widget();
        widget.setType(WidgetFactory.HEADING.name());
        widget.setLabel(Messages.PerformanceTabCalculation);
        column.getWidgets().add(widget);

        widget = new Dashboard.Widget();
        widget.setType(WidgetFactory.CALCULATION.name());
        widget.setLabel(WidgetFactory.CALCULATION.getLabel());
        column.getWidgets().add(widget);

        return newDashboard;
    }
}
