package name.abuchen.portfolio.ui.log;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.UIConstants;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.ui.di.Focus;
import org.eclipse.e4.ui.di.UIEventTopic;
import org.eclipse.e4.ui.di.UISynchronize;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.TreeColumnLayout;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class ErrorLogPart
{
    public static class LogEntryDetailDialog extends Dialog
    {
        private LogEntry entry;
        private Text entryText;

        protected LogEntryDetailDialog(Shell parentShell, LogEntry entry)
        {
            super(parentShell);
            this.entry = entry;
        }

        @Override
        protected boolean isResizable()
        {
            return true;
        }

        @Override
        protected void createButtonsForButtonBar(Composite parent)
        {
            createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
            Button button = createButton(parent, 9999, Messages.LabelCopyToClipboard, false);
            button.addSelectionListener(new SelectionAdapter()
            {
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                    if (entryText.isDisposed())
                        return;
                    Clipboard cb = new Clipboard(Display.getCurrent());
                    TextTransfer textTransfer = TextTransfer.getInstance();
                    cb.setContents(new Object[] { entryText.getText() }, new Transfer[] { textTransfer });
                }
            });
        }

        @Override
        protected Control createDialogArea(Composite parent)
        {
            Composite container = new Composite(parent, SWT.None);
            GridDataFactory.fillDefaults().grab(true, true).hint(600, 200).applyTo(container);
            GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);

            entryText = new Text(container, SWT.MULTI | SWT.WRAP | SWT.READ_ONLY | SWT.BORDER | SWT.V_SCROLL);
            entryText.setText(entry.getText());
            GridDataFactory.fillDefaults().grab(true, true).applyTo(entryText);
            return container;
        }
    }

    public static class LogEntryContentProvider implements ITreeContentProvider
    {
        private List<?> entries;

        @Override
        public void dispose()
        {}

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
                        return PortfolioPlugin.image(PortfolioPlugin.IMG_ERROR);
                    case IStatus.WARNING:
                        return PortfolioPlugin.image(PortfolioPlugin.IMG_WARNING);
                    default:
                        return PortfolioPlugin.image(PortfolioPlugin.IMG_INFO);
                }
            }
        });

        logViewer.getTree().setHeaderVisible(true);
        logViewer.getTree().setLinesVisible(true);

        logViewer.setContentProvider(new LogEntryContentProvider());

        logViewer.setInput(entries);

        logViewer.addDoubleClickListener(new IDoubleClickListener()
        {
            @Override
            public void doubleClick(DoubleClickEvent event)
            {
                LogEntry entry = (LogEntry) ((IStructuredSelection) event.getSelection()).getFirstElement();
                LogEntryDetailDialog dialog = new LogEntryDetailDialog(Display.getCurrent().getActiveShell(), entry);
                dialog.open();
            }
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
