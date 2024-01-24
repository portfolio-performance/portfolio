package name.abuchen.portfolio.ui.log;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.ui.di.Focus;
import org.eclipse.e4.ui.di.UIEventTopic;
import org.eclipse.e4.ui.di.UISynchronize;
import org.eclipse.jface.layout.TreeColumnLayout;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.dialogs.DisplayTextDialog;
import name.abuchen.portfolio.ui.util.viewers.CopyPasteSupport;

public class ErrorLogPart
{
    public static class LogEntryContentProvider implements ITreeContentProvider
    {
        private List<?> entries;

        @Override
        public void dispose()
        {
        }

        @Override
        public void inputChanged(Viewer viewer, Object oldInput, Object newInput)
        {
            this.entries = (List<?>) newInput;
        }

        @Override
        public Object[] getElements(Object inputElement)
        {
            return entries.toArray();
        }

        @Override
        public boolean hasChildren(Object element)
        {
            return ((LogEntry) element).getChildren() != null;
        }

        @Override
        public Object[] getChildren(Object parentElement)
        {
            return ((LogEntry) parentElement).getChildren().toArray();
        }

        @Override
        public Object getParent(Object element)
        {
            return null;
        }
    }

    private List<LogEntry> entries = new ArrayList<LogEntry>();

    private TreeViewer logViewer;

    @Inject
    UISynchronize sync;

    @PostConstruct
    public void createComposite(Composite parent, LogEntryCache cache)
    {
        this.entries = cache.getEntries();

        Composite container = new Composite(parent, SWT.NONE);
        TreeColumnLayout layout = new TreeColumnLayout();
        container.setLayout(layout);

        logViewer = new TreeViewer(container, SWT.FULL_SELECTION);

        CopyPasteSupport.enableFor(logViewer);

        TreeViewerColumn column = new TreeViewerColumn(logViewer, SWT.NONE);
        column.getColumn().setText(Messages.ColumnDate);
        layout.setColumnData(column.getColumn(), new ColumnPixelData(140));
        column.setLabelProvider(new ColumnLabelProvider()
        {
            private DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);

            @Override
            public String getText(Object element)
            {
                return dateFormat.format(((LogEntry) element).getDate());
            }
        });

        column = new TreeViewerColumn(logViewer, SWT.NONE);
        column.getColumn().setText(Messages.ColumnMessage);
        layout.setColumnData(column.getColumn(), new ColumnPixelData(500));
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                return ((LogEntry) element).getMessage();
            }

            @Override
            public Image getImage(Object element)
            {
                LogEntry entry = (LogEntry) element;

                switch (entry.getSeverity())
                {
                    case IStatus.ERROR:
                        return Images.ERROR.image();
                    case IStatus.WARNING:
                        return Images.WARNING.image();
                    default:
                        return Images.INFO.image();
                }
            }
        });

        logViewer.getTree().setHeaderVisible(true);
        logViewer.getTree().setLinesVisible(true);

        logViewer.setContentProvider(new LogEntryContentProvider());

        logViewer.setInput(entries);

        logViewer.addDoubleClickListener(event -> {
            LogEntry entry = (LogEntry) ((IStructuredSelection) event.getSelection()).getFirstElement();
            DisplayTextDialog dialog = new DisplayTextDialog(Display.getCurrent().getActiveShell(), entry.getText());
            dialog.setDialogTitle(Messages.LabelErrorProtocolDetails);
            dialog.open();
        });
    }

    @Focus
    public void setFocus()
    {
        logViewer.getTree().setFocus();
    }

    @Inject
    @Optional
    public void onLogEntryCreated(@UIEventTopic(UIConstants.Event.Log.CREATED) LogEntry entry)
    {
        entries.add(entry);
        logViewer.refresh();
    }

    @Inject
    @Optional
    public void onLogEntriesDeleted(@UIEventTopic(UIConstants.Event.Log.CLEARED) LogEntry entry)
    {
        entries.clear();
        logViewer.refresh();
    }

}
