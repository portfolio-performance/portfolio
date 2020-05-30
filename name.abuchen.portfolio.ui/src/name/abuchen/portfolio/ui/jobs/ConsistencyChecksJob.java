package name.abuchen.portfolio.ui.jobs;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
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

import name.abuchen.portfolio.checks.Checker;
import name.abuchen.portfolio.checks.Issue;
import name.abuchen.portfolio.checks.QuickFix;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.viewers.ColumnViewerSorter;

public class ConsistencyChecksJob extends AbstractClientJob
{
    private final boolean reportSuccess;

    public ConsistencyChecksJob(Client client, boolean reportSuccess)
    {
        super(client, Messages.JobMsgRunningConsistencyChecks);
        this.reportSuccess = reportSuccess;
    }

    @Override
    protected IStatus run(IProgressMonitor monitor)
    {
        monitor.beginTask(Messages.JobMsgRunningConsistencyChecks, 1);
        final List<Issue> issues = Checker.runAll(getClient());

        if (issues.isEmpty())
        {
            if (reportSuccess)
            {
                Display.getDefault()
                                .asyncExec(() -> MessageDialog.openInformation(Display.getCurrent().getActiveShell(),
                                                Messages.LabelInfo, Messages.MsgNoIssuesFound));
            }
        }
        else
        {
            Display.getDefault().asyncExec(() -> {
                SelectQuickFixDialog dialog = new SelectQuickFixDialog(Display.getCurrent().getActiveShell(),
                                getClient(), issues);
                dialog.open();
            });

        }

        return Status.OK_STATUS;
    }

    private static class SelectQuickFixDialog extends TitleAreaDialog
    {
        private final Client client;
        private final List<ReportedIssue> issues;

        private Menu contextMenu;
        private TableViewer tableViewer;

        private ReportedIssue currentIssue;

        public SelectQuickFixDialog(Shell shell, Client client, List<Issue> issues)
        {
            super(shell);
            setTitleImage(Images.BANNER.image());

            this.client = client;
            this.issues = new ArrayList<>();
            for (Issue issue : issues)
                this.issues.add(new ReportedIssue(issue));
        }

        @Override
        protected void setShellStyle(int newShellStyle)
        {
            super.setShellStyle((newShellStyle & ~SWT.APPLICATION_MODAL) | SWT.RESIZE);
            setBlockOnOpen(false);
        }

        @Override
        protected void createButtonsForButtonBar(Composite parent)
        {
            createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
            createButton(parent, IDialogConstants.RETRY_ID, Messages.LabelRefresh, false);
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

            composite.addDisposeListener(e -> SelectQuickFixDialog.this.widgetDisposed());

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
                    LocalDate date = ((ReportedIssue) element).getDate();
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
                    Object entity = ((ReportedIssue) element).getEntity();

                    if (entity instanceof Client)
                        return Messages.LabelPortfolioPerformanceFile;
                    else
                        return entity.toString();
                }

                @Override
                public Image getImage(Object element)
                {
                    ReportedIssue issue = (ReportedIssue) element;
                    if (issue.getEntity() instanceof Account)
                        return Images.ACCOUNT.image();
                    else if (issue.getEntity() instanceof Portfolio)
                        return Images.PORTFOLIO.image();
                    else if (issue.getEntity() instanceof Client)
                        return Images.LOGO_16.image();
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
                @Override
                protected void measure(Event event, Object element)
                {
                    ReportedIssue line = (ReportedIssue) element;
                    Point size = event.gc.textExtent(line.getLabel());
                    event.width = table.getColumn(event.index).getWidth();
                    event.width = size.x + 1;
                    event.height = size.y;
                }

                @Override
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
                    return issue.isFixed() ? Images.CHECK.image() : Images.QUICKFIX.image();
                }
            });
            layout.setColumnData(col.getColumn(), new ColumnPixelData(100));

            table.addMouseListener(new MouseAdapter()
            {
                @Override
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

        protected void widgetDisposed()
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
                menuMgr.addMenuListener(manager -> {
                    for (final QuickFix fix : currentIssue.getAvailableFixes())
                    {
                        if (fix == QuickFix.SEPARATOR)
                        {
                            manager.add(new Separator());
                        }
                        else
                        {
                            manager.add(new Action(fix.getLabel())
                            {
                                @Override
                                public void run()
                                {
                                    fix.execute();
                                    currentIssue.setFixedMessage(fix.getDoneLabel());
                                    if (client != null)
                                        client.markDirty();
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

        @Override
        protected void buttonPressed(int buttonId)
        {
            if (buttonId == IDialogConstants.RETRY_ID)
            {
                List<Issue> list = Checker.runAll(client);
                issues.clear();
                for (Issue issue : list)
                    this.issues.add(new ReportedIssue(issue));
                tableViewer.setInput(issues);
            }
            else
            {
                super.buttonPressed(buttonId);
            }
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

        public LocalDate getDate()
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
