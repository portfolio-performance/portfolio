package name.abuchen.portfolio.ui.dialogs;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;

import name.abuchen.portfolio.snapshot.ReportingPeriod;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.ContextMenu;

public class EditReportingPeriodsDialog extends Dialog
{
    private TableViewer tableViewer;
    private List<ReportingPeriod> periods;

    public EditReportingPeriodsDialog(Shell parentShell)
    {
        super(parentShell);
    }

    public void setReportingPeriods(List<ReportingPeriod> periods)
    {
        this.periods = new ArrayList<>(periods);
    }

    public List<ReportingPeriod> getReportingPeriods()
    {
        return this.periods;
    }

    @Override
    protected void configureShell(Shell shell)
    {
        super.configureShell(shell);
        shell.setText(Messages.LabelReportInterval);
    }

    @Override
    protected Control createDialogArea(Composite parent)
    {
        Composite container = (Composite) super.createDialogArea(parent);

        Composite tableArea = new Composite(container, SWT.NONE);
        GridDataFactory.fillDefaults().grab(true, true).minSize(SWT.DEFAULT, 200).applyTo(tableArea);
        TableColumnLayout layout = new TableColumnLayout();
        tableArea.setLayout(layout);

        tableViewer = new TableViewer(tableArea, SWT.BORDER | SWT.MULTI);
        final Table table = tableViewer.getTable();
        table.setHeaderVisible(false);
        table.setLinesVisible(false);

        TableViewerColumn column = new TableViewerColumn(tableViewer, SWT.None);
        layout.setColumnData(column.getColumn(), new ColumnWeightData(100));

        tableViewer.setLabelProvider(new LabelProvider()
        {
            @Override
            public Image getImage(Object element)
            {
                return Images.TEXT.image();
            }
        });
        tableViewer.setContentProvider(ArrayContentProvider.getInstance());
        tableViewer.setInput(periods);

        new ContextMenu(tableViewer.getTable(), this::fillContextMenu).hook();

        return container;
    }

    private void fillContextMenu(IMenuManager manager)
    {
        manager.add(new Action(Messages.MenuReportingPeriodDelete)
        {
            @Override
            public void run()
            {
                IStructuredSelection selection = (IStructuredSelection) tableViewer.getSelection();

                for (Object o : selection.toArray())
                    periods.remove(o);

                tableViewer.refresh();
            }
        });

    }

}
