package name.abuchen.portfolio.ui.views;

import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.window.ToolTip;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.TransactionPair;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.CurrencyConverterImpl;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.PortfolioSnapshot;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.ConfirmAction;
import name.abuchen.portfolio.ui.util.DropDown;
import name.abuchen.portfolio.ui.util.SimpleAction;
import name.abuchen.portfolio.ui.util.viewers.Column;
import name.abuchen.portfolio.ui.util.viewers.ColumnEditingSupport;
import name.abuchen.portfolio.ui.util.viewers.ColumnEditingSupport.ModificationListener;
import name.abuchen.portfolio.ui.util.viewers.ColumnViewerSorter;
import name.abuchen.portfolio.ui.util.viewers.ListEditingSupport;
import name.abuchen.portfolio.ui.util.viewers.ShowHideColumnHelper;
import name.abuchen.portfolio.ui.views.columns.NameColumn;
import name.abuchen.portfolio.ui.views.columns.NameColumn.NameColumnLabelProvider;
import name.abuchen.portfolio.ui.views.columns.NoteColumn;

public class PortfolioListView extends AbstractListView implements ModificationListener
{
    private static final String FILTER_INACTIVE_PORTFOLIOS = "filter-retired-portfolios"; //$NON-NLS-1$

    @Inject
    private ExchangeRateProviderFactory factory;

    private TableViewer portfolios;
    private StatementOfAssetsViewer statementOfAssets;
    private TransactionsViewer transactions;

    private ShowHideColumnHelper portfolioColumns;

    private boolean isFiltered = false;

    @Override
    protected String getDefaultTitle()
    {
        return Messages.LabelPortfolios;
    }

    @PostConstruct
    public void setup()
    {
        isFiltered = getPreferenceStore().getBoolean(FILTER_INACTIVE_PORTFOLIOS);
    }

    @Override
    protected int getSashStyle()
    {
        return SWT.VERTICAL | SWT.BEGINNING;
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
        ISelection selection = portfolios.getSelection();

        setInput();

        portfolios.setSelection(selection);
    }

    @Override
    public void onModified(Object element, Object newValue, Object oldValue)
    {
        markDirty();
    }

    @Override
    protected void addButtons(ToolBarManager toolBar)
    {
        addNewButton(toolBar);
        addFilterButton(toolBar);
        addConfigButton(toolBar);
    }

    private void addNewButton(ToolBarManager toolBar)
    {
        SimpleAction.Runnable newPortfolioAction = a -> {
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
        };

        toolBar.add(new DropDown(Messages.MenuCreatePortfolioOrTransaction, Images.PLUS, SWT.NONE, manager -> {
            manager.add(new SimpleAction(Messages.PortfolioMenuAdd, newPortfolioAction));
            manager.add(new Separator());

            Portfolio portfolio = (Portfolio) portfolios.getStructuredSelection().getFirstElement();
            new SecurityContextMenu(PortfolioListView.this).menuAboutToShow(manager, null, portfolio);
        }));
    }

    private void addFilterButton(ToolBarManager toolBar)
    {
        Action filter = new Action()
        {
            @Override
            public void run()
            {
                isFiltered = !isFiltered;
                getPart().getPreferenceStore().setValue(FILTER_INACTIVE_PORTFOLIOS, isFiltered);
                setImageDescriptor(isFiltered ? Images.FILTER_ON.descriptor() : Images.FILTER_OFF.descriptor());
                setInput();
            }
        };
        filter.setImageDescriptor(isFiltered ? Images.FILTER_ON.descriptor() : Images.FILTER_OFF.descriptor());
        filter.setToolTipText(Messages.PortfolioFilterRetiredPortfolios);
        toolBar.add(filter);
    }

    private void addConfigButton(final ToolBarManager toolBar)
    {
        toolBar.add(new DropDown(Messages.MenuShowHideColumns, Images.CONFIG, SWT.NONE, manager -> {
            MenuManager m = new MenuManager(Messages.LabelPortfolios);
            portfolioColumns.menuAboutToShow(m);
            manager.add(m);

            m = new MenuManager(Messages.LabelTransactions);
            transactions.getColumnSupport().menuAboutToShow(m);
            manager.add(m);
        }));
    }

    // //////////////////////////////////////////////////////////////
    // top table: accounts
    // //////////////////////////////////////////////////////////////

    @Override
    protected void createTopTable(Composite parent)
    {
        Composite container = new Composite(parent, SWT.NONE);
        TableColumnLayout layout = new TableColumnLayout();
        container.setLayout(layout);

        portfolios = new TableViewer(container, SWT.FULL_SELECTION);

        ColumnEditingSupport.prepare(portfolios);
        ColumnViewerToolTipSupport.enableFor(portfolios, ToolTip.NO_RECREATE);

        portfolioColumns = new ShowHideColumnHelper(PortfolioListView.class.getSimpleName() + "@top2", //$NON-NLS-1$
                        getPreferenceStore(), portfolios, layout);

        Column column = new NameColumn("0", Messages.ColumnPortfolio, SWT.None, 100); //$NON-NLS-1$
        column.setLabelProvider(new NameColumnLabelProvider() // NOSONAR
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

        column = new Column("1", Messages.ColumnReferenceAccount, SWT.None, 160); //$NON-NLS-1$
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

        column = new NameColumn("volume", Messages.ColumnVolumeOfSecurityDeposits, SWT.RIGHT, 100); //$NON-NLS-1$
        column.setLabelProvider(new ColumnLabelProvider()
        {
            CurrencyConverter converter = new CurrencyConverterImpl(factory, getClient().getBaseCurrency());

            @Override
            public String getText(Object element)
            {
                PortfolioSnapshot snapshot = PortfolioSnapshot.create((Portfolio) element, converter, LocalDate.now());
                return Values.Money.format(snapshot.getValue(), getClient().getBaseCurrency());
            }
        });
        portfolioColumns.addColumn(column);

        column = new NoteColumn();
        column.getEditingSupport().addListener(this);
        portfolioColumns.addColumn(column);

        portfolioColumns.createColumns();

        portfolios.getTable().setHeaderVisible(true);
        portfolios.getTable().setLinesVisible(true);

        portfolios.setContentProvider(ArrayContentProvider.getInstance());
        setInput();

        portfolios.addSelectionChangedListener(event -> {
            Portfolio portfolio = (Portfolio) ((IStructuredSelection) event.getSelection()).getFirstElement();

            if (portfolio != null)
            {
                transactions.setInput(portfolio.getTransactions().stream().map(t -> new TransactionPair<>(portfolio, t))
                                .collect(Collectors.toList()));
                transactions.refresh();
                CurrencyConverter converter = new CurrencyConverterImpl(factory,
                                portfolio.getReferenceAccount().getCurrencyCode());
                statementOfAssets.setInput(PortfolioSnapshot.create(portfolio, converter, LocalDate.now()));
            }
            else
            {
                transactions.setInput(null);
                transactions.refresh();
                statementOfAssets.setInput((PortfolioSnapshot) null);
            }
        });

        hookContextMenu(portfolios.getTable(), this::fillPortfolioContextMenu);
    }

    private void fillPortfolioContextMenu(IMenuManager manager)
    {
        final Portfolio portfolio = (Portfolio) ((IStructuredSelection) portfolios.getSelection()).getFirstElement();
        if (portfolio == null)
            return;

        new SecurityContextMenu(this).menuAboutToShow(manager, null, portfolio);

        manager.add(new Separator());

        manager.add(new SimpleAction(
                        portfolio.isRetired() ? Messages.PortfolioMenuActivate : Messages.PortfolioMenuDeactivate,
                        a -> {
                            portfolio.setRetired(!portfolio.isRetired());
                            markDirty();
                            setInput();
                        }));

        manager.add(new ConfirmAction(Messages.PortfolioMenuDelete,
                        MessageFormat.format(Messages.PortfolioMenuDeleteConfirm, portfolio.getName()), a -> {
                            getClient().removePortfolio(portfolio);
                            markDirty();
                            setInput();
                        }));
    }

    // //////////////////////////////////////////////////////////////
    // bottom table: transactions
    // //////////////////////////////////////////////////////////////

    @Override
    protected void createBottomTable(Composite parent)
    {
        CTabFolder folder = new CTabFolder(parent, SWT.BORDER);

        CTabItem item = new CTabItem(folder, SWT.NONE);
        item.setText(Messages.LabelStatementOfAssets);
        statementOfAssets = make(StatementOfAssetsViewer.class);
        item.setControl(statementOfAssets.createControl(folder));

        hookContextMenu(statementOfAssets.getTableViewer().getControl(),
                        manager -> statementOfAssets.hookMenuListener(manager, PortfolioListView.this));

        item = new CTabItem(folder, SWT.NONE);
        item.setText(Messages.TabTransactions);
        transactions = new TransactionsViewer(folder, this);
        inject(transactions);
        item.setControl(transactions.getControl());

        folder.setSelection(0);

        if (!getClient().getActivePortfolios().isEmpty())
            portfolios.setSelection(new StructuredSelection(portfolios.getElementAt(0)), true);
    }

}
