package name.abuchen.portfolio.ui.views;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.snapshot.PortfolioSnapshot;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPart;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.util.AbstractDropDown;
import name.abuchen.portfolio.ui.util.Column;
import name.abuchen.portfolio.ui.util.ColumnEditingSupport;
import name.abuchen.portfolio.ui.util.ColumnEditingSupport.ModificationListener;
import name.abuchen.portfolio.ui.util.ColumnViewerSorter;
import name.abuchen.portfolio.ui.util.ListEditingSupport;
import name.abuchen.portfolio.ui.util.ShowHideColumnHelper;
import name.abuchen.portfolio.ui.util.SimpleListContentProvider;
import name.abuchen.portfolio.ui.util.ViewerHelper;
import name.abuchen.portfolio.ui.views.columns.NameColumn;
import name.abuchen.portfolio.ui.views.columns.NameColumn.NameColumnLabelProvider;
import name.abuchen.portfolio.ui.views.columns.NoteColumn;
import name.abuchen.portfolio.util.Dates;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
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
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ToolBar;

public class PortfolioListView extends AbstractListView implements ModificationListener
{
    private static final String FILTER_INACTIVE_PORTFOLIOS = "filter-retired-portfolios"; //$NON-NLS-1$

    private TableViewer portfolios;
    private StatementOfAssetsViewer statementOfAssets;
    private PortfolioTransactionsViewer transactions;

    private ShowHideColumnHelper portfolioColumns;

    private boolean isFiltered = false;

    @Override
    protected String getTitle()
    {
        return Messages.LabelPortfolios;
    }

    @Override
    public void init(PortfolioPart part, Object parameter)
    {
        super.init(part, parameter);

        isFiltered = part.getPreferenceStore().getBoolean(FILTER_INACTIVE_PORTFOLIOS);
    }

    private void setInput()
    {
        portfolios.setInput(isFiltered ? getClient().getActivePortfolios() : getClient().getPortfolios());
    }

    @Override
    public void notifyModelUpdated()
    {
        // actions from the security context menu (buy, sell, ...) call
        // #notifyModelUpdated when adding new transactions
        portfolios.setSelection(portfolios.getSelection());
    }

    @Override
    public void onModified(Object element, Object newValue, Object oldValue)
    {
        markDirty();
    }

    @Override
    protected void addButtons(ToolBar toolBar)
    {
        addNewPortfolioButton(toolBar);
        addFilterButton(toolBar);
        addConfigButton(toolBar);
    }

    private void addNewPortfolioButton(ToolBar toolBar)
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

                setInput();
                portfolios.editElement(portfolio, 0);
            }
        };
        action.setImageDescriptor(PortfolioPlugin.descriptor(PortfolioPlugin.IMG_PLUS));
        action.setToolTipText(Messages.PortfolioMenuAdd);

        new ActionContributionItem(action).fill(toolBar, -1);
    }

    private void addFilterButton(ToolBar toolBar)
    {
        Action filter = new Action()
        {
            @Override
            public void run()
            {
                isFiltered = isChecked();
                getPart().getPreferenceStore().setValue(FILTER_INACTIVE_PORTFOLIOS, isFiltered);
                setInput();
            }
        };
        filter.setChecked(isFiltered);
        filter.setImageDescriptor(PortfolioPlugin.descriptor(PortfolioPlugin.IMG_FILTER));
        filter.setToolTipText(Messages.PortfolioFilterRetiredPortfolios);
        new ActionContributionItem(filter).fill(toolBar, -1);
    }

    private void addConfigButton(final ToolBar toolBar)
    {
        new AbstractDropDown(toolBar, Messages.MenuShowHideColumns, //
                        PortfolioPlugin.image(PortfolioPlugin.IMG_CONFIG), SWT.NONE)
        {
            @Override
            public void menuAboutToShow(IMenuManager manager)
            {
                MenuManager m = new MenuManager(Messages.LabelPortfolios);
                portfolioColumns.menuAboutToShow(m);
                manager.add(m);

                m = new MenuManager(Messages.LabelTransactions);
                transactions.getColumnSupport().menuAboutToShow(m);
                manager.add(m);
            }
        };
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

        ColumnEditingSupport.prepare(portfolios);

        portfolioColumns = new ShowHideColumnHelper(PortfolioListView.class.getSimpleName() + "@top2", //$NON-NLS-1$
                        getPreferenceStore(), portfolios, layout);

        Column column = new NameColumn("0", Messages.ColumnPortfolio, SWT.None, 100); //$NON-NLS-1$
        column.setLabelProvider(new NameColumnLabelProvider()
        {
            @Override
            public Color getForeground(Object e)
            {
                boolean isRetired = ((Portfolio) e).isRetired();
                return isRetired ? Display.getDefault().getSystemColor(SWT.COLOR_DARK_GRAY) : null;
            }
        });
        column.getEditingSupport().addListener(this);
        portfolioColumns.addColumn(column);

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
        ColumnViewerSorter.create(Portfolio.class, "referenceAccount").attachTo(column); //$NON-NLS-1$
        new ListEditingSupport(Portfolio.class, "referenceAccount", getClient().getAccounts()) //$NON-NLS-1$
                        .addListener(this).attachTo(column);
        portfolioColumns.addColumn(column);

        column = new NoteColumn();
        column.getEditingSupport().addListener(this);
        portfolioColumns.addColumn(column);

        portfolioColumns.createColumns();

        portfolios.getTable().setHeaderVisible(true);
        portfolios.getTable().setLinesVisible(true);

        portfolios.setContentProvider(new SimpleListContentProvider());
        setInput();
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

        manager.add(new Action(portfolio.isRetired() ? Messages.PortfolioMenuActivate
                        : Messages.PortfolioMenuDeactivate)
        {
            @Override
            public void run()
            {
                portfolio.setRetired(!portfolio.isRetired());
                markDirty();
                setInput();
            }

        });

        manager.add(new Action(Messages.PortfolioMenuDelete)
        {
            @Override
            public void run()
            {
                getClient().removePortfolio(portfolio);
                markDirty();
                setInput();
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

        statementOfAssets.pack();
        transactions.pack();

        if (!getClient().getActivePortfolios().isEmpty())
            portfolios.setSelection(new StructuredSelection(portfolios.getElementAt(0)), true);
    }

}
