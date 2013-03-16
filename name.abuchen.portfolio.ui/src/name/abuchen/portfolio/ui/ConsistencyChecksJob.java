package name.abuchen.portfolio.ui;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import name.abuchen.portfolio.checks.Checker;
import name.abuchen.portfolio.checks.Issue;
import name.abuchen.portfolio.checks.QuickFix;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.Values;
import name.abuchen.portfolio.ui.util.ColumnViewerSorter;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.OwnerDrawLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;

public class ConsistencyChecksJob extends Job
{
    private final ClientEditor editor;
    private final Client client;
    private final boolean reportSuccess;

    public ConsistencyChecksJob(ClientEditor editor, Client client, boolean reportSuccess)
    {
        super(Messages.JobMsgRunningConsistencyChecks);
        this.editor = editor;
        this.client = client;
        this.reportSuccess = reportSuccess;
    }

    @Override
    protected IStatus run(IProgressMonitor monitor)
    {
        final List<Issue> issues = Checker.runAll(client);

        if (issues.isEmpty())
        {
            if (reportSuccess)
            {
                Display.getDefault().asyncExec(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        MessageDialog.openInformation(Display.getCurrent().getActiveShell(), Messages.LabelInfo,
                                        Messages.MsgNoIssuesFound);
                    }
                });
            }
        }
        else
        {
            Display.getDefault().asyncExec(new Runnable()
            {
                @Override
                public void run()
                {
                    SelectQuickFixDialog dialog = new SelectQuickFixDialog(Display.getCurrent().getActiveShell(),
                                    issues);
                    dialog.open();

                    for (ReportedIssue issue : dialog.issues)
                    {
                        if (issue.isFixed())
                        {
                            editor.markDirty();
                            break;
                        }
                    }
                }
            });

        }

        return Status.OK_STATUS;
    }

    private static class SelectQuickFixDialog extends TitleAreaDialog
    {
        private final List<ReportedIssue> issues;
        private Menu contextMenu;
        private TableViewer tableViewer;

        private ReportedIssue currentIssue;

        public SelectQuickFixDialog(Shell shell, List<Issue> issues)
        {
            super(shell);

            this.issues = new ArrayList<ReportedIssue>();
            for (Issue issue : issues)
                this.issues.add(new ReportedIssue(issue));
        }

        @Override
        protected void setShellStyle(int newShellStyle)
        {
            super.setShellStyle(newShellStyle | SWT.RESIZE);
        }

        protected void createButtonsForButtonBar(Composite parent)
        {
            createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
        }

        @Override
        protected Control createContents(Composite parent)
        {
            Control contents = super.createContents(parent);
            setTitle(Messages.DialogConsistencyChecksTitle);
            setMessage(Messages.DialogConssitencyChecksMessage);
            return contents;
        }

        @Override
        protected Control createDialogArea(Composite parent)
        {
            Composite composite = (Composite) super.createDialogArea(parent);

            composite.addDisposeListener(new DisposeListener()
            {
                @Override
                public void widgetDisposed(DisposeEvent e)
                {
                    SelectQuickFixDialog.this.widgetDisposed(e);
                }
            });

            Composite tableArea = new Composite(composite, SWT.NONE);
            GridDataFactory.fillDefaults().grab(true, true).applyTo(tableArea);
            tableArea.setLayout(new FillLayout());

            TableColumnLayout layout = new TableColumnLayout();
            tableArea.setLayout(layout);

            tableViewer = new TableViewer(tableArea, SWT.BORDER | SWT.FULL_SELECTION);
            final Table table = tableViewer.getTable();
            table.setHeaderVisible(true);
            table.setLinesVisible(true);

            TableViewerColumn col = new TableViewerColumn(tableViewer, SWT.NONE);
            col.getColumn().setText(Messages.ColumnDate);
            col.setLabelProvider(new ColumnLabelProvider()
            {
                @Override
                public String getText(Object element)
                {
                    Date date = ((ReportedIssue) element).getDate();
                    return date != null ? Values.Date.format(date) : null;
                }
            });
            layout.setColumnData(col.getColumn(), new ColumnPixelData(80));
            ColumnViewerSorter.create(ReportedIssue.class, "date").attachTo(tableViewer, col, true); //$NON-NLS-1$

            col = new TableViewerColumn(tableViewer, SWT.NONE);
            col.getColumn().setText(Messages.ColumnEntity);
            col.setLabelProvider(new ColumnLabelProvider()
            {
                @Override
                public String getText(Object element)
                {
                    return ((ReportedIssue) element).getEntity().toString();
                }

                @Override
                public Image getImage(Object element)
                {
                    ReportedIssue issue = (ReportedIssue) element;
                    if (issue.getEntity() instanceof Account)
                        return PortfolioPlugin.image(PortfolioPlugin.IMG_ACCOUNT);
                    else if (issue.getEntity() instanceof Portfolio)
                        return PortfolioPlugin.image(PortfolioPlugin.IMG_PORTFOLIO);
                    else
                        return null;
                }
            });
            layout.setColumnData(col.getColumn(), new ColumnPixelData(100));
            ColumnViewerSorter.create(ReportedIssue.class, "entity").attachTo(tableViewer, col); //$NON-NLS-1$

            col = new TableViewerColumn(tableViewer, SWT.RIGHT);
            col.getColumn().setText(Messages.ColumnAmount);
            col.setLabelProvider(new ColumnLabelProvider()
            {
                @Override
                public String getText(Object element)
                {
                    Long amount = ((ReportedIssue) element).getAmount();
                    return amount != null ? Values.Amount.format(amount) : null;
                }
            });
            layout.setColumnData(col.getColumn(), new ColumnPixelData(80));
            ColumnViewerSorter.create(ReportedIssue.class, "amount").attachTo(tableViewer, col); //$NON-NLS-1$

            col = new TableViewerColumn(tableViewer, SWT.NONE);
            col.getColumn().setText(Messages.ColumnIssue);
            col.setLabelProvider(new OwnerDrawLabelProvider()
            {
                protected void measure(Event event, Object element)
                {
                    ReportedIssue line = (ReportedIssue) element;
                    Point size = event.gc.textExtent(line.getLabel());
                    event.width = table.getColumn(event.index).getWidth();
                    event.width = size.x + 1;
                    event.height = size.y;
                }

                protected void paint(Event event, Object element)
                {
                    ReportedIssue entry = (ReportedIssue) element;
                    event.gc.drawText(entry.getLabel(), event.x, event.y, true);
                }
            });
            layout.setColumnData(col.getColumn(), new ColumnPixelData(300));
            ColumnViewerSorter.create(ReportedIssue.class, "label").attachTo(tableViewer, col); //$NON-NLS-1$

            col = new TableViewerColumn(tableViewer, SWT.NONE);
            col.getColumn().setText(Messages.ColumnFix);
            col.setLabelProvider(new ColumnLabelProvider()
            {
                @Override
                public String getText(Object element)
                {
                    ReportedIssue issue = (ReportedIssue) element;
                    return issue.isFixed() ? issue.getFixedMessage() : null;
                }

                @Override
                public Image getImage(Object element)
                {
                    ReportedIssue issue = (ReportedIssue) element;

                    return PortfolioPlugin.image(issue.isFixed() ? PortfolioPlugin.IMG_CHECK
                                    : PortfolioPlugin.IMG_QUICKFIX);
                }
            });
            layout.setColumnData(col.getColumn(), new ColumnPixelData(100));

            table.addMouseListener(new MouseAdapter()
            {
                public void mouseDown(MouseEvent e)
                {
                    Point point = new Point(e.x, e.y);
                    TableItem item = table.getItem(point);
                    if (item != null && item.getBounds(4).contains(point))
                    {
                        ReportedIssue issue = (ReportedIssue) item.getData();
                        if (!issue.isFixed())
                            showContextMenu(issue);
                    }
                }
            });

            tableViewer.setContentProvider(new ArrayContentProvider());
            tableViewer.setInput(issues);

            return composite;
        }

        protected void widgetDisposed(DisposeEvent e)
        {
            if (contextMenu != null)
                contextMenu.dispose();
        }

        private void showContextMenu(ReportedIssue issue)
        {
            this.currentIssue = issue;
            if (contextMenu == null)
            {
                MenuManager menuMgr = new MenuManager("#PopupMenu"); //$NON-NLS-1$
                menuMgr.setRemoveAllWhenShown(true);
                menuMgr.addMenuListener(new IMenuListener()
                {
                    @Override
                    public void menuAboutToShow(IMenuManager manager)
                    {
                        for (final QuickFix fix : currentIssue.getAvailableFixes())
                        {
                            manager.add(new Action(fix.getLabel())
                            {
                                @Override
                                public void run()
                                {
                                    fix.execute();
                                    currentIssue.setFixedMessage(fix.getDoneLabel());
                                    tableViewer.refresh(currentIssue);
                                }
                            });
                        }
                    }
                });

                contextMenu = menuMgr.createContextMenu(getShell());
            }

            contextMenu.setVisible(true);
        }
    }

    public static class ReportedIssue
    {
        private Issue delegate;
        private List<QuickFix> fixes;
        private String fixedMessage;

        public ReportedIssue(Issue delegate)
        {
            this.delegate = delegate;
            this.fixes = delegate.getAvailableFixes();
        }

        public Date getDate()
        {
            return delegate.getDate();
        }

        public Object getEntity()
        {
            return delegate.getEntity();
        }

        public Long getAmount()
        {
            return delegate.getAmount();
        }

        public String getLabel()
        {
            return delegate.getLabel();
        }

        public List<QuickFix> getAvailableFixes()
        {
            return fixes;
        }

        public void setFixedMessage(String message)
        {
            this.fixedMessage = message;
        }

        protected String getFixedMessage()
        {
            return fixedMessage;
        }

        public boolean isFixed()
        {
            return fixedMessage != null;
        }
    }
}
