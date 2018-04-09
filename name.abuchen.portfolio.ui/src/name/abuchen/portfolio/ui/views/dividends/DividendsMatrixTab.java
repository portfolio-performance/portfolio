package name.abuchen.portfolio.ui.views.dividends;

import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.DateTimeFormatter;

import javax.inject.Inject;

import org.eclipse.e4.ui.workbench.modeling.ESelectionService;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.layout.TableColumnLayout;
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

import name.abuchen.portfolio.model.InvestmentVehicle;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.selection.SecuritySelection;
import name.abuchen.portfolio.ui.util.SimpleAction;
import name.abuchen.portfolio.ui.util.TableViewerCSVExporter;
import name.abuchen.portfolio.ui.util.viewers.ColumnViewerSorter;
import name.abuchen.portfolio.ui.views.dividends.DividendsViewModel.Line;
import name.abuchen.portfolio.util.TextUtil;

public class DividendsMatrixTab implements DividendsTab
{
    @Inject
    private ESelectionService selectionService;

    @Inject
    protected DividendsViewModel model;

    private boolean showOnlyOneYear = false;

    protected Font boldFont;
    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM yy"); //$NON-NLS-1$

    private TableColumnLayout tableLayout;
    private TableViewer tableViewer;

    @Override
    public String getLabel()
    {
        return Messages.LabelDividendsByMonthAndVehicle;
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

    @Override
    public void addConfigActions(IMenuManager manager)
    {
        Action action = new SimpleAction(Messages.LabelShowOnlyOneYear, a -> {
            showOnlyOneYear = !showOnlyOneYear;
            updateColumns(tableViewer, tableLayout);
        });
        action.setChecked(showOnlyOneYear);
        manager.add(action);
    }

    @Override
    public Control createControl(Composite parent)
    {
        LocalResourceManager resources = new LocalResourceManager(JFaceResources.getResources(), parent);
        boldFont = resources.createFont(FontDescriptor.createFrom(parent.getFont()).setStyle(SWT.BOLD));

        Composite container = new Composite(parent, SWT.NONE);

        tableLayout = new TableColumnLayout();
        container.setLayout(tableLayout);

        tableViewer = new TableViewer(container, SWT.FULL_SELECTION);
        ColumnViewerToolTipSupport.enableFor(tableViewer, ToolTip.NO_RECREATE);

        createColumns(tableViewer, tableLayout);

        tableViewer.getTable().setHeaderVisible(true);
        tableViewer.getTable().setLinesVisible(true);

        tableViewer.setContentProvider(ArrayContentProvider.getInstance());

        tableViewer.addSelectionChangedListener(event -> {
            InvestmentVehicle vehicle = ((DividendsViewModel.Line) ((IStructuredSelection) event.getSelection())
                            .getFirstElement()).getVehicle();
            if (vehicle != null && vehicle instanceof Security)
                selectionService.setSelection(new SecuritySelection(model.getClient(), (Security) vehicle));
        });

        tableViewer.setInput(model.getAllLines());

        for (TableColumn c : tableViewer.getTable().getColumns())
            c.pack();

        model.addUpdateListener(() -> updateColumns(tableViewer, tableLayout));

        return container;
    }

    protected void createColumns(TableViewer records, TableColumnLayout layout)
    {
        createSecurityColumn(records, layout, true);

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
        createSecurityColumn(records, layout, false);
    }

    protected void createSecurityColumn(TableViewer records, TableColumnLayout layout, boolean isSorted)
    {
        TableViewerColumn column = new TableViewerColumn(records, SWT.NONE);
        column.getColumn().setText(Messages.ColumnSecurity);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public Image getImage(Object element)
            {
                InvestmentVehicle vehicle = ((DividendsViewModel.Line) element).getVehicle();
                return vehicle != null ? Images.SECURITY.image() : null;
            }

            @Override
            public String getText(Object element)
            {
                InvestmentVehicle vehicle = ((DividendsViewModel.Line) element).getVehicle();
                return vehicle != null ? vehicle.getName() : Messages.ColumnSum;
            }

            @Override
            public Font getFont(Object element)
            {
                InvestmentVehicle vehicle = ((DividendsViewModel.Line) element).getVehicle();
                return vehicle != null ? null : boldFont;
            }
        });

        ColumnViewerSorter.create((o1, o2) -> {
            int direction = ColumnViewerSorter.SortingContext.getSortDirection();

            DividendsViewModel.Line line1 = (DividendsViewModel.Line) o1;
            DividendsViewModel.Line line2 = (DividendsViewModel.Line) o2;

            if (line1.getVehicle() == null)
                return direction == SWT.DOWN ? 1 : -1;
            if (line2.getVehicle() == null)
                return direction == SWT.DOWN ? -1 : 1;

            String n1 = line1.getVehicle().getName();
            String n2 = line2.getVehicle().getName();
            return n1.compareToIgnoreCase(n2);
        }).attachTo(records, column, isSorted);

        layout.setColumnData(column.getColumn(), new ColumnPixelData(200));
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
                Line line = (DividendsViewModel.Line) element;
                return line.getVehicle() != null ? Values.Amount.formatNonZero(line.getValue(index))
                                : Values.Amount.format(line.getValue(index));
            }

            @Override
            public String getToolTipText(Object element)
            {
                InvestmentVehicle vehicle = ((DividendsViewModel.Line) element).getVehicle();
                return TextUtil.tooltip(vehicle != null ? vehicle.getName() : null);
            }

            @Override
            public Font getFont(Object element)
            {
                InvestmentVehicle vehicle = ((DividendsViewModel.Line) element).getVehicle();
                return vehicle != null ? null : boldFont;
            }
        });
        layout.setColumnData(column.getColumn(), new ColumnPixelData(50));
    }

    protected void createSumColumn(TableViewer records, TableColumnLayout layout)
    {
        TableViewerColumn column;
        column = new TableViewerColumn(records, SWT.RIGHT);
        column.getColumn().setText(Messages.ColumnSum);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                DividendsViewModel.Line line = (DividendsViewModel.Line) element;

                if (showOnlyOneYear)
                {
                    int noOfMonths = Math.min(12, line.getNoOfMonths());

                    long sum = 0;
                    for (int ii = 0; ii < noOfMonths; ii++)
                        sum += line.getValue(ii);

                    return Values.Amount.formatNonZero(sum);
                }
                else
                {
                    return Values.Amount.formatNonZero(line.getSum());
                }
            }

            @Override
            public Font getFont(Object element)
            {
                return boldFont;
            }
        });
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
