package name.abuchen.portfolio.ui.views;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.snapshot.PortfolioSnapshot;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.util.CellEditorFactory;
import name.abuchen.portfolio.ui.util.ColumnViewerSorter;
import name.abuchen.portfolio.ui.util.ShowHideColumnHelper;
import name.abuchen.portfolio.ui.util.ShowHideColumnHelper.Column;
import name.abuchen.portfolio.ui.util.SimpleListContentProvider;
import name.abuchen.portfolio.ui.util.ViewerHelper;
import name.abuchen.portfolio.util.Dates;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.ToolBar;

public class PortfolioListView extends AbstractListView
{
    private TableViewer portfolios;
    private StatementOfAssetsViewer statementOfAssets;
    private PortfolioTransactionsViewer transactions;

    @Override
    protected String getTitle()
    {
        return Messages.LabelPortfolios;
    }

    @Override
    public void notifyModelUpdated()
    {
        portfolios.setSelection(portfolios.getSelection());
    }

    @Override
    protected void addButtons(ToolBar toolBar)
    {
        Action action = new Action()
        {
            @Override
            public void run()
            {
                Portfolio portfolio = new Portfolio();
                portfolio.setName(Messages.LabelNoName);

                if (!getClient().getAccounts().isEmpty())
                {
                    portfolio.setReferenceAccount(getClient().getAccounts().get(0));
                }
                else
                {
                    Account account = new Account();
                    account.setName(Messages.LabelDefaultReferenceAccountName);
                    getClient().addAccount(account);
                    portfolio.setReferenceAccount(account);
                }

                getClient().addPortfolio(portfolio);
                markDirty();

                portfolios.setInput(getClient().getPortfolios());
                portfolios.editElement(portfolio, 0);
            }
        };
        action.setImageDescriptor(PortfolioPlugin.descriptor(PortfolioPlugin.IMG_PLUS));
        action.setToolTipText(Messages.PortfolioMenuAdd);

        new ActionContributionItem(action).fill(toolBar, -1);
    }

    // //////////////////////////////////////////////////////////////
    // top table: accounts
    // //////////////////////////////////////////////////////////////

    protected void createTopTable(Composite parent)
    {
        Composite container = new Composite(parent, SWT.NONE);
        TableColumnLayout layout = new TableColumnLayout();
        container.setLayout(layout);

        portfolios = new TableViewer(container, SWT.FULL_SELECTION);

        ShowHideColumnHelper support = new ShowHideColumnHelper(PortfolioListView.class.getSimpleName() + "@top2", //$NON-NLS-1$
                        portfolios, layout);

        Column column = new Column(Messages.ColumnPortfolio, SWT.None, 100);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                return ((Portfolio) e).getName();
            }

            @Override
            public Image getImage(Object element)
            {
                return PortfolioPlugin.image(PortfolioPlugin.IMG_PORTFOLIO);
            }
        });
        column.setSorter(ColumnViewerSorter.create(Portfolio.class, "name"), SWT.DOWN); //$NON-NLS-1$
        column.setMoveable(false);
        support.addColumn(column);

        column = new Column(Messages.ColumnReferenceAccount, SWT.None, 160);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                Portfolio p = (Portfolio) e;
                return p.getReferenceAccount() != null ? p.getReferenceAccount().getName() : null;
            }
        });
        column.setSorter(ColumnViewerSorter.create(Portfolio.class, "referenceAccount")); //$NON-NLS-1$
        column.setMoveable(false);
        support.addColumn(column);

        column = new Column("note", Messages.ColumnNote, SWT.LEFT, 200); //$NON-NLS-1$
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                return ((Portfolio) e).getNote();
            }
        });
        column.setSorter(ColumnViewerSorter.create(Portfolio.class, "note")); //$NON-NLS-1$
        column.setMoveable(false);
        support.addColumn(column);

        support.createColumns();

        portfolios.getTable().setHeaderVisible(true);
        portfolios.getTable().setLinesVisible(true);

        portfolios.setContentProvider(new SimpleListContentProvider());
        portfolios.setInput(getClient().getPortfolios());
        if (!support.isUserConfigured())
            ViewerHelper.pack(portfolios);

        portfolios.addSelectionChangedListener(new ISelectionChangedListener()
        {
            public void selectionChanged(SelectionChangedEvent event)
            {
                Portfolio portfolio = (Portfolio) ((IStructuredSelection) event.getSelection()).getFirstElement();

                if (portfolio != null)
                {
                    transactions.setInput(portfolio, portfolio.getTransactions());
                    transactions.refresh();
                    statementOfAssets.setInput(PortfolioSnapshot.create(portfolio, Dates.today()));
                }
                else
                {
                    transactions.setInput(null, null);
                    transactions.refresh();
                    statementOfAssets.setInput((PortfolioSnapshot) null);
                }
            }
        });

        new CellEditorFactory(portfolios, Portfolio.class) //
                        .notify(new CellEditorFactory.ModificationListener()
                        {
                            public void onModified(Object element, String property)
                            {
                                markDirty();
                                portfolios.refresh(transactions.getPortfolio());
                            }
                        }) //
                        .editable("name") // //$NON-NLS-1$
                        .combobox("referenceAccount", getClient().getAccounts()) //$NON-NLS-1$
                        .editable("note") //$NON-NLS-1$
                        .apply();

        hookContextMenu(portfolios.getTable(), new IMenuListener()
        {
            public void menuAboutToShow(IMenuManager manager)
            {
                fillPortfolioContextMenu(manager);
            }
        });
    }

    private void fillPortfolioContextMenu(IMenuManager manager)
    {
        final Portfolio portfolio = (Portfolio) ((IStructuredSelection) portfolios.getSelection()).getFirstElement();
        if (portfolio == null)
            return;

        new SecurityContextMenu(this).menuAboutToShow(manager, null, portfolio);

        manager.add(new Separator());
        manager.add(new Action(Messages.PortfolioMenuDelete)
        {
            @Override
            public void run()
            {
                getClient().removePortfolio(portfolio);
                markDirty();

                portfolios.setInput(getClient().getPortfolios());
            }
        });
    }

    // //////////////////////////////////////////////////////////////
    // bottom table: transactions
    // //////////////////////////////////////////////////////////////

    protected void createBottomTable(Composite parent)
    {
        CTabFolder folder = new CTabFolder(parent, SWT.BORDER);

        CTabItem item = new CTabItem(folder, SWT.NONE);
        item.setText(Messages.LabelStatementOfAssets);
        statementOfAssets = new StatementOfAssetsViewer(folder, this, getClient());
        item.setControl(statementOfAssets.getControl());

        hookContextMenu(statementOfAssets.getTableViewer().getControl(), new IMenuListener()
        {
            public void menuAboutToShow(IMenuManager manager)
            {
                statementOfAssets.hookMenuListener(manager, PortfolioListView.this);
            }
        });

        item = new CTabItem(folder, SWT.NONE);
        item.setText(Messages.TabTransactions);
        transactions = new PortfolioTransactionsViewer(folder, this);
        item.setControl(transactions.getControl());

        folder.setSelection(0);

        if (!getClient().getPortfolios().isEmpty())
            portfolios.setSelection(new StructuredSelection(portfolios.getElementAt(0)), true);

        statementOfAssets.pack();
        transactions.pack();
    }

}
