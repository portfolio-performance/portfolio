package name.abuchen.portfolio.ui.dialogs;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.ViewerDropAdapter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSourceAdapter;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;

import name.abuchen.portfolio.snapshot.ReportingPeriod;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.ContextMenu;
import name.abuchen.portfolio.util.TextUtil;

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
        GridDataFactory.fillDefaults().grab(true, true).hint(SWT.DEFAULT, 300).applyTo(tableArea);
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

        setupDnD();

        new ContextMenu(tableViewer.getTable(), this::fillContextMenu).hook();
        
        Label info = new Label(container, SWT.NONE);
        info.setText(TextUtil.tooltip(Messages.LabelReportingPeriodEditTooltip));

        return container;
    }

    private void setupDnD()
    {
        Transfer[] types = new Transfer[] { LocalSelectionTransfer.getTransfer() };
        tableViewer.addDragSupport(DND.DROP_MOVE, types, new DragSourceAdapter()
        {
            @Override
            public void dragSetData(DragSourceEvent event)
            {
                IStructuredSelection selection = (IStructuredSelection) tableViewer.getSelection();
                event.doit = (selection.size() < tableViewer.getTable().getItemCount());

                if (event.doit)
                {
                    LocalSelectionTransfer.getTransfer().setSelection(selection);
                    LocalSelectionTransfer.getTransfer().setSelectionSetTime(event.time & 0xFFFFFFFFL);
                }
            }
        });

        tableViewer.addDropSupport(DND.DROP_MOVE, types, new ViewerDropAdapter(tableViewer)
        {
            @Override
            public boolean validateDrop(Object target, int operation, TransferData transferType)
            {
                return true;
            }

            @Override
            public boolean performDrop(Object data)
            {
                IStructuredSelection selection = (IStructuredSelection) data;

                List<ReportingPeriod> movedItems = new ArrayList<>();
                for (Object o : selection.toList())
                    movedItems.add((ReportingPeriod) o);

                periods.removeAll(movedItems);

                Object destination = getCurrentTarget();
                int index = periods.indexOf(destination);
                if (index >= 0)
                {

                    int location = getCurrentLocation();
                    if (location == ViewerDropAdapter.LOCATION_ON || location == ViewerDropAdapter.LOCATION_AFTER)
                        index++;

                    periods.addAll(index, movedItems);
                }

                tableViewer.refresh();

                return true;
            }
        });
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
