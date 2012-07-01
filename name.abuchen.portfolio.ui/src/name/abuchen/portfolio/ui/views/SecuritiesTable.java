package name.abuchen.portfolio.ui.views;

import java.text.MessageFormat;
import java.util.Comparator;
import java.util.List;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.LatestSecurityPrice;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Values;
import name.abuchen.portfolio.model.Watchlist;
import name.abuchen.portfolio.ui.AbstractFinanceView;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.UpdateQuotesJob;
import name.abuchen.portfolio.ui.dialogs.BuySellSecurityDialog;
import name.abuchen.portfolio.ui.dialogs.DividendsDialog;
import name.abuchen.portfolio.ui.dnd.SecurityDragListener;
import name.abuchen.portfolio.ui.dnd.SecurityTransfer;
import name.abuchen.portfolio.ui.util.ColumnViewerSorter;
import name.abuchen.portfolio.ui.util.ShowHideColumnHelper;
import name.abuchen.portfolio.ui.util.ShowHideColumnHelper.Column;
import name.abuchen.portfolio.ui.util.SimpleListContentProvider;
import name.abuchen.portfolio.ui.util.ViewerHelper;
import name.abuchen.portfolio.ui.wizards.EditSecurityWizard;
import name.abuchen.portfolio.ui.wizards.ImportQuotesWizard;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

public class SecuritiesTable
{
    private AbstractFinanceView view;

    private Watchlist watchlist;

    private Menu contextMenu;
    private TableViewer securities;

    private ShowHideColumnHelper support;

    public SecuritiesTable(Composite parent, AbstractFinanceView view)
    {
        this.view = view;

        Composite container = new Composite(parent, SWT.NONE);
        TableColumnLayout layout = new TableColumnLayout();
        container.setLayout(layout);

        this.securities = new TableViewer(container, SWT.FULL_SELECTION);

        support = new ShowHideColumnHelper(SecuritiesTable.class.getName(), securities, layout);

        Column column = new Column(Messages.ColumnName, SWT.LEFT, 400);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                return ((Security) e).getName();
            }

            @Override
            public Image getImage(Object e)
            {
                return PortfolioPlugin.image(PortfolioPlugin.IMG_SECURITY);
            }
        });
        column.setSorter(ColumnViewerSorter.create(Security.class, "name"), SWT.DOWN); //$NON-NLS-1$
        support.addColumn(column);

        column = new Column(Messages.ColumnISIN, SWT.LEFT, 100);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                return ((Security) e).getIsin();
            }
        });
        column.setSorter(ColumnViewerSorter.create(Security.class, "isin")); //$NON-NLS-1$
        support.addColumn(column);

        column = new Column(Messages.ColumnTicker, SWT.LEFT, 80);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                return ((Security) e).getTickerSymbol();
            }
        });
        column.setSorter(ColumnViewerSorter.create(Security.class, "tickerSymbol")); //$NON-NLS-1$
        support.addColumn(column);

        column = new Column(Messages.ColumnSecurityType, SWT.LEFT, 80);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                return ((Security) e).getType().toString();
            }
        });
        column.setSorter(ColumnViewerSorter.create(Security.class, "type")); //$NON-NLS-1$
        support.addColumn(column);

        column = new Column(Messages.ColumnLatest, SWT.RIGHT, 60);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                LatestSecurityPrice latest = ((Security) e).getLatest();
                return latest != null ? Values.Quote.format(latest.getValue()) : null;
            }
        });
        column.setSorter(ColumnViewerSorter.create(new Comparator<Object>()
        {
            @Override
            public int compare(Object o1, Object o2)
            {
                LatestSecurityPrice p1 = ((Security) o1).getLatest();
                LatestSecurityPrice p2 = ((Security) o2).getLatest();

                if (p1 == null)
                    return p2 == null ? 0 : -1;
                if (p2 == null)
                    return 1;

                long v1 = p1.getValue();
                long v2 = p2.getValue();
                return v1 > v2 ? 1 : v1 == v2 ? 0 : -1;
            }
        }));
        support.addColumn(column);

        column = new Column(Messages.ColumnDelta, SWT.RIGHT, 60);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                LatestSecurityPrice latest = ((Security) e).getLatest();
                return latest != null ? String.format("%,.2f %%", //$NON-NLS-1$
                                ((double) (latest.getValue() - latest.getPreviousClose()) //
                                / (double) latest.getPreviousClose()) * 100) : null;
            }

            @Override
            public Color getForeground(Object element)
            {
                Security p = (Security) element;
                LatestSecurityPrice latest = p.getLatest();
                if (latest == null)
                    return null;

                return latest.getValue() >= latest.getPreviousClose() ? Display.getCurrent().getSystemColor(
                                SWT.COLOR_DARK_GREEN) : Display.getCurrent().getSystemColor(SWT.COLOR_DARK_RED);
            }
        });
        column.setSorter(ColumnViewerSorter.create(new Comparator<Object>()
        {
            @Override
            public int compare(Object o1, Object o2)
            {
                LatestSecurityPrice p1 = ((Security) o1).getLatest();
                LatestSecurityPrice p2 = ((Security) o2).getLatest();

                if (p1 == null)
                    return p2 == null ? 0 : -1;
                if (p2 == null)
                    return 1;

                double v1 = (((double) (p1.getValue() - p1.getPreviousClose())) / p1.getPreviousClose() * 100);
                double v2 = (((double) (p2.getValue() - p2.getPreviousClose())) / p2.getPreviousClose() * 100);
                return Double.compare(v1, v2);
            }
        }));
        support.addColumn(column);

        support.createColumns();

        securities.getTable().setHeaderVisible(true);
        securities.getTable().setLinesVisible(true);

        securities.setContentProvider(new SimpleListContentProvider());

        securities.addDragSupport(DND.DROP_MOVE, //
                        new Transfer[] { SecurityTransfer.getTransfer() }, //
                        new SecurityDragListener(securities));

        ViewerHelper.pack(securities);
        securities.refresh();

        securities.getTable().addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetDefaultSelected(SelectionEvent event)
            {
                Security security = (Security) ((IStructuredSelection) securities.getSelection()).getFirstElement();
                if (security == null)
                    return;

                Dialog dialog = new WizardDialog(getShell(), new EditSecurityWizard(getClient(), security));
                if (dialog.open() != Dialog.OK)
                    return;

                markDirty();
                if (!securities.getControl().isDisposed())
                {
                    refresh(security);
                    updateQuotes(security);
                }
            }
        });

        hookContextMenu();
    }

    public void addSelectionChangedListener(ISelectionChangedListener listener)
    {
        this.securities.addSelectionChangedListener(listener);
    }

    public void addFilter(ViewerFilter filter)
    {
        this.securities.addFilter(filter);
    }

    public void setInput(List<Security> securities)
    {
        this.securities.setInput(securities);
        this.watchlist = null;
    }

    public void setInput(Watchlist watchlist)
    {
        this.securities.setInput(watchlist.getSecurities());
        this.watchlist = watchlist;
    }

    public void refresh(Security security)
    {
        this.securities.refresh(security, true);
    }

    public void refresh()
    {
        try
        {
            securities.getControl().setRedraw(false);
            securities.refresh();
        }
        finally
        {
            securities.getControl().setRedraw(true);
        }
    }

    public void updateQuotes(Security security)
    {
        new UpdateQuotesJob(security)
        {
            @Override
            protected void notifyFinished()
            {
                PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable()
                {
                    public void run()
                    {
                        markDirty();
                        securities.refresh();
                        securities.setSelection(securities.getSelection());
                    }
                });
            }

        }.schedule();
    }

    public TableViewer getTableViewer()
    {
        return securities;
    }

    public ShowHideColumnHelper getColumnHelper()
    {
        return support;
    }

    //
    // private
    //

    private Client getClient()
    {
        return view.getClient();
    }

    private Shell getShell()
    {
        return securities.getTable().getShell();
    }

    private void markDirty()
    {
        view.markDirty();
    }

    private void hookContextMenu()
    {
        MenuManager menuMgr = new MenuManager("#PopupMenu"); //$NON-NLS-1$
        menuMgr.setRemoveAllWhenShown(true);
        menuMgr.addMenuListener(new IMenuListener()
        {
            public void menuAboutToShow(IMenuManager manager)
            {
                fillContextMenu(manager);
            }
        });

        contextMenu = menuMgr.createContextMenu(securities.getTable());
        securities.getTable().setMenu(contextMenu);

        securities.getTable().addDisposeListener(new DisposeListener()
        {
            @Override
            public void widgetDisposed(DisposeEvent e)
            {
                if (contextMenu != null)
                    contextMenu.dispose();
            }
        });
    }

    private void fillContextMenu(IMenuManager manager)
    {
        if (!(((IStructuredSelection) securities.getSelection()).getFirstElement() != null))
            return;

        manager.add(new AbstractDialogAction(Messages.SecurityMenuBuy)
        {
            @Override
            Dialog createDialog(Security security)
            {
                return new BuySellSecurityDialog(getShell(), getClient(), security, PortfolioTransaction.Type.BUY);
            }
        });

        manager.add(new AbstractDialogAction(Messages.SecurityMenuSell)
        {
            @Override
            Dialog createDialog(Security security)
            {
                return new BuySellSecurityDialog(getShell(), getClient(), security, PortfolioTransaction.Type.SELL);
            }
        });

        manager.add(new AbstractDialogAction(Messages.SecurityMenuDividends)
        {
            @Override
            Dialog createDialog(Security security)
            {
                return new DividendsDialog(getShell(), getClient(), security);
            }
        });
        manager.add(new Separator());

        manager.add(new AbstractDialogAction(Messages.SecurityMenuEditSecurity)
        {
            @Override
            Dialog createDialog(Security security)
            {
                return new WizardDialog(getShell(), new EditSecurityWizard(getClient(), security));
            }

            @Override
            protected void performFinish(Security security)
            {
                super.performFinish(security);
                updateQuotes(security);
            }
        });
        manager.add(new Separator());

        manager.add(new Action(Messages.SecurityMenuUpdateQuotes)
        {
            @Override
            public void run()
            {
                Security security = (Security) ((IStructuredSelection) securities.getSelection()).getFirstElement();
                updateQuotes(security);
            }
        });
        manager.add(new AbstractDialogAction(Messages.SecurityMenuImportQuotes)
        {
            @Override
            Dialog createDialog(Security security)
            {
                return new WizardDialog(getShell(), new ImportQuotesWizard(security));
            }
        });

        manager.add(new Separator());
        if (watchlist == null)
        {
            manager.add(new Action(Messages.SecurityMenuDeleteSecurity)
            {
                @Override
                public void run()
                {
                    Security security = (Security) ((IStructuredSelection) securities.getSelection()).getFirstElement();

                    if (security == null)
                        return;

                    if (!security.getTransactions(getClient()).isEmpty())
                    {
                        MessageDialog.openError(getShell(), Messages.MsgDeletionNotPossible,
                                        MessageFormat.format(Messages.MsgDeletionNotPossibleDetail, security.getName()));
                    }
                    else if (getClient().getRootCategory().getTreeElements().contains(security))
                    {
                        MessageDialog.openError(getShell(), Messages.MsgDeletionNotPossible, MessageFormat.format(
                                        Messages.MsgDeletionNotPossibleAssignedInAllocation, security.getName()));
                    }
                    else
                    {

                        getClient().removeSecurity(security);
                        markDirty();

                        securities.setInput(getClient().getSecurities());
                    }
                }
            });
        }
        else
        {
            manager.add(new Action(MessageFormat.format(Messages.SecurityMenuRemoveFromWatchlist, watchlist.getName()))
            {
                @Override
                public void run()
                {
                    Security security = (Security) ((IStructuredSelection) securities.getSelection()).getFirstElement();

                    if (security == null)
                        return;

                    watchlist.getSecurities().remove(security);
                    markDirty();

                    securities.setInput(watchlist.getSecurities());
                }
            });
        }
    }

    private abstract class AbstractDialogAction extends Action
    {

        public AbstractDialogAction(String text)
        {
            super(text);
        }

        @Override
        public final void run()
        {
            Security security = (Security) ((IStructuredSelection) securities.getSelection()).getFirstElement();

            if (security == null)
                return;

            Dialog dialog = createDialog(security);
            if (dialog.open() == Dialog.OK)
                performFinish(security);
        }

        protected void performFinish(Security security)
        {
            markDirty();
            if (!securities.getControl().isDisposed())
            {
                securities.refresh(security, true);
                securities.setSelection(securities.getSelection());
            }
        }

        abstract Dialog createDialog(Security security);
    }
}
