package name.abuchen.portfolio.ui.views.payments;

import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.ToLongFunction;
import java.util.stream.IntStream;

import javax.inject.Inject;

import org.eclipse.e4.ui.services.IStylingEngine;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.FontDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.window.ToolTip;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.TableColumn;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.InvestmentVehicle;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.editor.AbstractFinanceView;
import name.abuchen.portfolio.ui.selection.SecuritySelection;
import name.abuchen.portfolio.ui.selection.SelectionService;
import name.abuchen.portfolio.ui.util.ContextMenu;
import name.abuchen.portfolio.ui.util.LogoManager;
import name.abuchen.portfolio.ui.util.SimpleAction;
import name.abuchen.portfolio.ui.util.TableViewerCSVExporter;
import name.abuchen.portfolio.ui.util.viewers.ColumnViewerSorter;
import name.abuchen.portfolio.ui.util.viewers.CopyPasteSupport;
import name.abuchen.portfolio.ui.views.AccountContextMenu;
import name.abuchen.portfolio.ui.views.SecurityContextMenu;
import name.abuchen.portfolio.ui.views.payments.PaymentsViewModel.Line;
import name.abuchen.portfolio.util.TextUtil;

public class PaymentsPerMonthMatrixTab implements PaymentsTab
{
    // Keys in PreferenceStore
    private static final String KEY_SHOW_ONE_YEAR = PaymentsPerMonthMatrixTab.class.getSimpleName()
                    + "-showOnlyOneYear"; //$NON-NLS-1$

    @Inject
    private IStylingEngine stylingEngine;

    @Inject
    private AbstractFinanceView view;

    @Inject
    private SelectionService selectionService;

    @Inject
    protected PaymentsViewModel model;

    @Inject
    protected IPreferenceStore preferences;

    private boolean showOnlyOneYear = false;
    private boolean columnsInReverseOrder = false;

    protected Font boldFont;
    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM yy"); //$NON-NLS-1$

    private TableColumnLayout tableLayout;
    private TableViewer tableViewer;

    @Override
    public String getLabel()
    {
        return Messages.LabelPaymentsByMonthAndVehicle;
    }

    @Override
    public void addExportActions(IMenuManager manager)
    {
        manager.add(new Action(MessageFormat.format(Messages.LabelExport, getLabel()))
        {
            @Override
            public void run()
            {
                new TableViewerCSVExporter(tableViewer).export(getLabel() + ".csv"); //$NON-NLS-1$
            }
        });
    }

    private String getKeyForReverseOrder()
    {
        // Separate keys for sub-classes
        return this.getClass().getSimpleName() + "-columnsInReverseOrder"; //$NON-NLS-1$
    }

    protected void addReverseColumnAction(IMenuManager manager)
    {
        Action action = new SimpleAction(Messages.LabelColumnsInReverseOrder, a -> {
            columnsInReverseOrder = !columnsInReverseOrder;
            sortColumnOrder();
            preferences.setValue(getKeyForReverseOrder(), columnsInReverseOrder);
        });
        action.setChecked(columnsInReverseOrder);
        manager.add(action);
    }

    @Override
    public void addConfigActions(IMenuManager manager)
    {
        Action action = new SimpleAction(Messages.LabelShowOnlyOneYear, a -> {
            showOnlyOneYear = !showOnlyOneYear;
            updateColumns(tableViewer, tableLayout);
            preferences.setValue(KEY_SHOW_ONE_YEAR, showOnlyOneYear);
        });
        action.setChecked(showOnlyOneYear);
        manager.add(action);

        addReverseColumnAction(manager);
    }

    @Override
    public Control createControl(Composite parent)
    {
        Composite container = new Composite(parent, SWT.NONE);

        showOnlyOneYear = preferences.getBoolean(KEY_SHOW_ONE_YEAR);
        columnsInReverseOrder = preferences.getBoolean(getKeyForReverseOrder());

        tableLayout = new TableColumnLayout();
        container.setLayout(tableLayout);

        tableViewer = new TableViewer(container, SWT.FULL_SELECTION);
        ColumnViewerToolTipSupport.enableFor(tableViewer, ToolTip.NO_RECREATE);
        CopyPasteSupport.enableFor(tableViewer);

        // make sure to apply the styles (including font information to the
        // table) before creating the bold font. Otherwise the font does not
        // match the styles in CSS
        stylingEngine.style(tableViewer.getTable());

        LocalResourceManager resources = new LocalResourceManager(JFaceResources.getResources(), parent);
        boldFont = resources.createFont(FontDescriptor.createFrom(tableViewer.getTable().getFont()).setStyle(SWT.BOLD));

        createColumns(tableViewer, tableLayout);
        sortColumnOrder();

        tableViewer.getTable().setHeaderVisible(true);
        tableViewer.getTable().setLinesVisible(true);

        tableViewer.setContentProvider(ArrayContentProvider.getInstance());

        tableViewer.addSelectionChangedListener(event -> {
            IStructuredSelection selection = event.getStructuredSelection();
            view.setInformationPaneInput(selection.getFirstElement());
            if (!selection.isEmpty())
            {
                InvestmentVehicle vehicle = ((PaymentsViewModel.Line) selection.getFirstElement()).getVehicle();
                if (vehicle instanceof Security)
                    selectionService.setSelection(new SecuritySelection(model.getClient(), (Security) vehicle));
            }
        });

        tableViewer.setInput(model.getAllLines());

        for (TableColumn c : tableViewer.getTable().getColumns())
            c.pack();

        model.addUpdateListener(() -> updateColumns(tableViewer, tableLayout));

        new ContextMenu(tableViewer.getControl(), this::fillContextMenu).hook();

        return container;
    }

    protected void fillContextMenu(IMenuManager manager)
    {
        IStructuredSelection selection = tableViewer.getStructuredSelection();

        if (selection.isEmpty() || selection.size() > 1)
            return;

        Line line = (Line) selection.getFirstElement();
        InvestmentVehicle vehicle = line.getVehicle();
        if (vehicle instanceof Account)
        {
            new AccountContextMenu(view).menuAboutToShow(manager, (Account) vehicle, null);
        }
        else if (vehicle instanceof Security)
        {
            new SecurityContextMenu(view).menuAboutToShow(manager, (Security) vehicle);
        }
    }

    protected void sortColumnOrder()
    {
        // Keep first and last column in same position
        sortColumnOrder(1, 1);
    }

    /**
     * Set column order in a given range.
     *
     * @param startOffset
     *            Number of unchanged columns at the start
     * @param endOffset
     *            Number of unchanged columns at the end
     */
    protected void sortColumnOrder(int startOffset, int endOffset)
    {
        // Natural order for all columns
        List<Integer> columnList = new ArrayList<>();
        IntStream.range(0, tableViewer.getTable().getColumnCount()).forEachOrdered(columnList::add);

        if (columnsInReverseOrder)
        {
            // subList is view of original list => original list is changed
            Collections.reverse(columnList.subList(startOffset, columnList.size() - endOffset));
        }

        int[] newColumnOrder = columnList.stream().mapToInt(i -> i).toArray();
        tableViewer.getTable().setColumnOrder(newColumnOrder);
    }

    protected void createColumns(TableViewer records, TableColumnLayout layout)
    {
        createVehicleColumn(records, layout, true);

        // create monthly columns
        LocalDate date = LocalDate.of(model.getStartYear(), Month.JANUARY, 1);

        int noOfMonths = showOnlyOneYear ? Math.min(12, model.getNoOfMonths()) : model.getNoOfMonths();

        for (int index = 0; index < noOfMonths; index++)
        {
            createMonthColumn(records, layout, date, index);
            date = date.plusMonths(1);
        }

        createSumColumn(records, layout);

        // add security name at the end of the matrix table again because the
        // first column is most likely not visible anymore
        createVehicleColumn(records, layout, false);
    }

    protected void createVehicleColumn(TableViewer records, TableColumnLayout layout, boolean isSorted)
    {
        TableViewerColumn column = new TableViewerColumn(records, SWT.NONE);
        column.getColumn().setText(Messages.ColumnSecurity);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public Image getImage(Object element)
            {
                InvestmentVehicle vehicle = ((PaymentsViewModel.Line) element).getVehicle();
                return LogoManager.instance().getDefaultColumnImage(vehicle, model.getClient().getSettings());
            }

            @Override
            public String getText(Object element)
            {
                InvestmentVehicle vehicle = ((PaymentsViewModel.Line) element).getVehicle();
                return vehicle != null ? vehicle.getName()
                                : (((PaymentsViewModel.Line) element).getConsolidatedRetired()
                                                ? Messages.LabelPaymentsConsolidateRetired
                                                : Messages.ColumnSum);
            }

            @Override
            public Font getFont(Object element)
            {
                InvestmentVehicle vehicle = ((PaymentsViewModel.Line) element).getVehicle();
                return vehicle != null || ((PaymentsViewModel.Line) element).getConsolidatedRetired() ? null : boldFont;
            }
        });

        createSorter((l1, l2) -> l1.getVehicle().getName().compareToIgnoreCase(l2.getVehicle().getName()))
                        .attachTo(records, column, isSorted);

        layout.setColumnData(column.getColumn(), new ColumnPixelData(200));
    }

    protected ColumnViewerSorter createSorter(Comparator<PaymentsViewModel.Line> comparator)
    {
        return ColumnViewerSorter.create((o1, o2) -> {
            int direction = ColumnViewerSorter.SortingContext.getSortDirection();

            PaymentsViewModel.Line line1 = (PaymentsViewModel.Line) o1;
            PaymentsViewModel.Line line2 = (PaymentsViewModel.Line) o2;

            if (line1.getVehicle() == null)
                return direction == SWT.UP ? 1 : -1;
            if (line2.getVehicle() == null)
                return direction == SWT.UP ? -1 : 1;

            return comparator.compare(line1, line2);
        });
    }

    private void createMonthColumn(TableViewer records, TableColumnLayout layout, LocalDate start, int index)
    {
        TableViewerColumn column = new TableViewerColumn(records, SWT.RIGHT);
        column.getColumn().setText(formatter.format(start));
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                Line line = (PaymentsViewModel.Line) element;
                return line.getVehicle() != null ? Values.Amount.formatNonZero(line.getValue(index))
                                : Values.Amount.format(line.getValue(index));
            }

            @Override
            public String getToolTipText(Object element)
            {
                InvestmentVehicle vehicle = ((PaymentsViewModel.Line) element).getVehicle();
                return TextUtil.tooltip(vehicle != null ? vehicle.getName() : null);
            }

            @Override
            public Font getFont(Object element)
            {
                InvestmentVehicle vehicle = ((PaymentsViewModel.Line) element).getVehicle();
                return vehicle != null || ((PaymentsViewModel.Line) element).getConsolidatedRetired() ? null : boldFont;
            }
        });

        createSorter((l1, l2) -> Long.compare(l1.getValue(index), l2.getValue(index))).attachTo(records, column);

        layout.setColumnData(column.getColumn(), new ColumnPixelData(50));
    }

    protected void createSumColumn(TableViewer records, TableColumnLayout layout)
    {
        ToLongFunction<PaymentsViewModel.Line> valueFunction = line -> {
            if (showOnlyOneYear)
            {
                int noOfMonths = Math.min(12, line.getNoOfMonths());

                long sum = 0;
                for (int ii = 0; ii < noOfMonths; ii++)
                    sum += line.getValue(ii);

                return sum;
            }
            else
            {
                return line.getSum();
            }
        };

        TableViewerColumn column;
        column = new TableViewerColumn(records, SWT.RIGHT);
        column.getColumn().setText(Messages.ColumnSum);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                PaymentsViewModel.Line line = (PaymentsViewModel.Line) element;
                return Values.Amount.formatNonZero(valueFunction.applyAsLong(line));
            }

            @Override
            public Font getFont(Object element)
            {
                return ((PaymentsViewModel.Line) element).getConsolidatedRetired() ? null : boldFont;
            }
        });

        createSorter((l1, l2) -> Long.compare(valueFunction.applyAsLong(l1), valueFunction.applyAsLong(l2)))
                        .attachTo(records, column);

        layout.setColumnData(column.getColumn(), new ColumnPixelData(200));
    }

    private void updateColumns(TableViewer records, TableColumnLayout layout)
    {
        try
        {
            // first add, then remove columns
            // (otherwise rendering of first column is broken)
            records.getTable().setRedraw(false);

            int count = records.getTable().getColumnCount();

            createColumns(records, layout);

            for (int ii = 0; ii < count; ii++)
                records.getTable().getColumn(0).dispose();

            sortColumnOrder();

            records.setInput(model.getAllLines());

            for (TableColumn c : records.getTable().getColumns())
                c.pack();
        }
        finally
        {
            records.refresh();
            records.getTable().setRedraw(true);
        }
    }
}
