package name.abuchen.portfolio.ui.views.dividends;

import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.DateTimeFormatter;

import javax.inject.Inject;

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
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.TableViewerCSVExporter;
import name.abuchen.portfolio.ui.util.viewers.ColumnViewerSorter;
import name.abuchen.portfolio.ui.util.viewers.ColumnViewerSorter.DirectionAwareComparator;
import name.abuchen.portfolio.ui.views.dividends.DividendsViewModel.Line;

public class DividendsMatrixTab implements DividendsTab
{
    @Inject
    private DividendsViewModel model;

    private Font boldFont;
    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM yy"); //$NON-NLS-1$

    private TableViewer tableViewer;

    @Override
    public String getLabel()
    {
        return Messages.LabelDividendsByMonthAndVehicle;
    }

    @Override
    public void addExportActions(IMenuManager manager)
    {
        manager.add(new Action(MessageFormat.format(Messages.LabelExport, Messages.LabelDividendsByMonthAndVehicle))
        {
            @Override
            public void run()
            {
                new TableViewerCSVExporter(tableViewer).export(Messages.LabelDividendsByMonthAndVehicle + ".csv"); //$NON-NLS-1$
            }
        });
    }

    @Override
    public Control createControl(Composite parent)
    {
        LocalResourceManager resources = new LocalResourceManager(JFaceResources.getResources(), parent);
        boldFont = resources.createFont(FontDescriptor.createFrom(parent.getFont()).setStyle(SWT.BOLD));

        Composite container = new Composite(parent, SWT.NONE);

        TableColumnLayout layout = new TableColumnLayout();
        container.setLayout(layout);

        tableViewer = new TableViewer(container, SWT.FULL_SELECTION);
        ColumnViewerToolTipSupport.enableFor(tableViewer, ToolTip.NO_RECREATE);

        createColumns(tableViewer, layout);

        tableViewer.getTable().setHeaderVisible(true);
        tableViewer.getTable().setLinesVisible(true);

        tableViewer.setContentProvider(ArrayContentProvider.getInstance());

        tableViewer.setInput(model.getAllLines());

        for (TableColumn c : tableViewer.getTable().getColumns())
            c.pack();

        model.addUpdateListener(() -> updateColumns(tableViewer, layout));

        return container;
    }

    private void createColumns(TableViewer records, TableColumnLayout layout)
    {
        createSecurityColumn(records, layout);

        // create monthly columns
        LocalDate date = LocalDate.of(model.getStartYear(), Month.JANUARY, 1);
        for (int index = 0; index < model.getNoOfMonths(); index++)
        {
            createMonthColumn(records, layout, date, index);
            date = date.plusMonths(1);
        }

        createSumColumn(records, layout);
    }

    private void createSecurityColumn(TableViewer records, TableColumnLayout layout)
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

        ColumnViewerSorter.create(new DirectionAwareComparator()
        {
            @Override
            public int compare(int direction, Object o1, Object o2)
            {
                DividendsViewModel.Line line1 = (DividendsViewModel.Line) o1;
                DividendsViewModel.Line line2 = (DividendsViewModel.Line) o2;

                if (line1.getVehicle() == null)
                    return 1;
                if (line2.getVehicle() == null)
                    return -1;

                String n1 = line1.getVehicle().getName();
                String n2 = line2.getVehicle().getName();

                int dir = direction == SWT.DOWN ? 1 : -1;
                return dir * n1.compareToIgnoreCase(n2);
            }
        }).attachTo(records, column, true);

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
            public Font getFont(Object element)
            {
                InvestmentVehicle vehicle = ((DividendsViewModel.Line) element).getVehicle();
                return vehicle != null ? null : boldFont;
            }
        });
        layout.setColumnData(column.getColumn(), new ColumnPixelData(50));
    }

    private void createSumColumn(TableViewer records, TableColumnLayout layout)
    {
        TableViewerColumn column;
        column = new TableViewerColumn(records, SWT.RIGHT);
        column.getColumn().setText(Messages.ColumnSum);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                return Values.Amount.formatNonZero(((DividendsViewModel.Line) element).getSum());
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
