package name.abuchen.portfolio.ui.views;

import java.util.Collections;
import java.util.List;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.PortfolioTransaction.Type;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Values;
import name.abuchen.portfolio.snapshot.PortfolioSnapshot;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.util.CellEditorFactory;
import name.abuchen.portfolio.ui.util.ColumnViewerSorter;
import name.abuchen.portfolio.ui.util.SharesLabelProvider;
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
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ToolBar;

public class PortfolioListView extends AbstractListView
{
    private TableViewer portfolios;
    private TableViewer transactions;
    private StatementOfAssetsViewer statementOfAssets;

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

        ShowHideColumnHelper support = new ShowHideColumnHelper(PortfolioListView.class.getSimpleName() + "@top", //$NON-NLS-1$
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

        support.createColumns();

        portfolios.getTable().setHeaderVisible(true);
        portfolios.getTable().setLinesVisible(true);

        portfolios.setContentProvider(new SimpleListContentProvider());
        portfolios.setInput(getClient().getPortfolios());
        portfolios.refresh();
        ViewerHelper.pack(portfolios);

        portfolios.addSelectionChangedListener(new ISelectionChangedListener()
        {
            public void selectionChanged(SelectionChangedEvent event)
            {
                Portfolio portfolio = (Portfolio) ((IStructuredSelection) event.getSelection()).getFirstElement();
                transactions.setData(Portfolio.class.toString(), portfolio);

                if (portfolio != null)
                {
                    transactions.setInput(portfolio.getTransactions());
                    transactions.refresh();
                    statementOfAssets.setInput(PortfolioSnapshot.create(portfolio, Dates.today()));
                }
                else
                {
                    transactions.setInput(null);
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
                                portfolios.refresh(transactions.getData(Account.class.toString()));
                            }
                        }) //
                        .editable("name") // //$NON-NLS-1$
                        .combobox("referenceAccount", getClient().getAccounts()) // //$NON-NLS-1$
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
        statementOfAssets = new StatementOfAssetsViewer(folder, getClient());
        item.setControl(statementOfAssets.getControl());

        hookContextMenu(statementOfAssets.getTableViewer().getControl(), new IMenuListener()
        {
            public void menuAboutToShow(IMenuManager manager)
            {
                statementOfAssets.hookMenuListener(manager, PortfolioListView.this);
            }
        });

        Control control = createTransactionsTable(folder);
        item = new CTabItem(folder, SWT.NONE);
        item.setText(Messages.TabTransactions);
        item.setControl(control);

        folder.setSelection(0);

        if (!getClient().getPortfolios().isEmpty())
            portfolios.setSelection(new StructuredSelection(portfolios.getElementAt(0)), true);

        statementOfAssets.pack();
        ViewerHelper.pack(transactions);
    }

    private Control createTransactionsTable(CTabFolder folder)
    {
        Composite container = new Composite(folder, SWT.NONE);
        TableColumnLayout layout = new TableColumnLayout();
        container.setLayout(layout);

        transactions = new TableViewer(container, SWT.FULL_SELECTION);

        ShowHideColumnHelper support = new ShowHideColumnHelper(PortfolioListView.class.getSimpleName() + "@bottom2", //$NON-NLS-1$
                        transactions, layout);
        support.setDoSaveState(false);

        Column column = new Column(Messages.ColumnDate, SWT.None, 80);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                return Values.Date.format(((PortfolioTransaction) element).getDate());
            }

            @Override
            public Color getForeground(Object element)
            {
                return colorFor((PortfolioTransaction) element);
            }
        });
        column.setSorter(ColumnViewerSorter.create(PortfolioTransaction.class, "date"), SWT.DOWN); //$NON-NLS-1$
        column.setMoveable(false);
        support.addColumn(column);

        column = new Column(Messages.ColumnTransactionType, SWT.None, 80);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                return ((PortfolioTransaction) element).getType().toString();
            }

            @Override
            public Color getForeground(Object element)
            {
                return colorFor((PortfolioTransaction) element);
            }
        });
        column.setSorter(ColumnViewerSorter.create(PortfolioTransaction.class, "type")); //$NON-NLS-1$
        column.setMoveable(false);
        support.addColumn(column);

        column = new Column(Messages.ColumnSecurity, SWT.None, 250);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                PortfolioTransaction t = (PortfolioTransaction) element;
                return t.getSecurity() != null ? String.valueOf(t.getSecurity()) : null;
            }

            @Override
            public Color getForeground(Object element)
            {
                return colorFor((PortfolioTransaction) element);
            }
        });
        column.setSorter(ColumnViewerSorter.create(PortfolioTransaction.class, "security")); //$NON-NLS-1$
        column.setMoveable(false);
        support.addColumn(column);

        column = new Column(Messages.ColumnShares, SWT.RIGHT, 80);
        column.setLabelProvider(new SharesLabelProvider()
        {
            @Override
            public Long getValue(Object element)
            {
                return ((PortfolioTransaction) element).getShares();
            }

            @Override
            public Color getForeground(Object element)
            {
                return colorFor((PortfolioTransaction) element);
            }
        });
        column.setSorter(ColumnViewerSorter.create(PortfolioTransaction.class, "shares")); //$NON-NLS-1$
        column.setMoveable(false);
        support.addColumn(column);

        column = new Column(Messages.ColumnPurchasePrice, SWT.RIGHT, 80);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                PortfolioTransaction t = (PortfolioTransaction) element;
                return t.getShares() != 0 ? Values.Amount.format(t.getActualPurchasePrice()) : null;
            }

            @Override
            public Color getForeground(Object element)
            {
                return colorFor((PortfolioTransaction) element);
            }
        });
        column.setSorter(ColumnViewerSorter.create(PortfolioTransaction.class, "actualPurchasePrice")); //$NON-NLS-1$
        column.setMoveable(false);
        support.addColumn(column);

        column = new Column(Messages.ColumnLumpSumPrice, SWT.RIGHT, 80);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                return Values.Amount.format(((PortfolioTransaction) element).getLumpSumPrice());
            }

            @Override
            public Color getForeground(Object element)
            {
                return colorFor((PortfolioTransaction) element);
            }
        });
        column.setSorter(ColumnViewerSorter.create(PortfolioTransaction.class, "lumpSumPrice")); //$NON-NLS-1$
        column.setMoveable(false);
        support.addColumn(column);

        column = new Column(Messages.ColumnFees, SWT.RIGHT, 80);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                return Values.Amount.format(((PortfolioTransaction) element).getFees());
            }

            @Override
            public Color getForeground(Object element)
            {
                return colorFor((PortfolioTransaction) element);
            }
        });
        column.setSorter(ColumnViewerSorter.create(PortfolioTransaction.class, "fees")); //$NON-NLS-1$
        column.setMoveable(false);
        support.addColumn(column);

        column = new Column(Messages.ColumnAmount, SWT.RIGHT, 80);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                return Values.Amount.format(((PortfolioTransaction) element).getAmount());
            }

            @Override
            public Color getForeground(Object element)
            {
                return colorFor((PortfolioTransaction) element);
            }
        });
        column.setSorter(ColumnViewerSorter.create(PortfolioTransaction.class, "amount")); //$NON-NLS-1$
        column.setMoveable(false);
        support.addColumn(column);

        column = new Column(Messages.ColumnOffsetAccount, SWT.None, 120);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                PortfolioTransaction t = (PortfolioTransaction) e;
                return t.getCrossEntry() != null ? t.getCrossEntry().getCrossEntity(t).toString() : null;
            }

            @Override
            public Color getForeground(Object element)
            {
                return colorFor((PortfolioTransaction) element);
            }
        });
        column.setMoveable(false);
        support.addColumn(column);

        support.createColumns();

        transactions.getTable().setHeaderVisible(true);
        transactions.getTable().setLinesVisible(true);

        transactions.setContentProvider(new SimpleListContentProvider());

        List<Security> securities = getClient().getSecurities();
        Collections.sort(securities, new Security.ByName());

        new CellEditorFactory(transactions, PortfolioTransaction.class) //
                        .notify(new CellEditorFactory.ModificationListener()
                        {
                            public void onModified(Object element, String property)
                            {
                                PortfolioTransaction t = (PortfolioTransaction) element;
                                if (t.getCrossEntry() != null)
                                    t.getCrossEntry().updateFrom(t);

                                markDirty();
                                Portfolio portfolio = (Portfolio) transactions.getData(Portfolio.class.toString());
                                portfolios.refresh(portfolio);
                                transactions.refresh(element);

                                statementOfAssets.setInput(PortfolioSnapshot.create(portfolio, Dates.today()));
                            }
                        }) //
                        .editable("date") // //$NON-NLS-1$
                        .readonly("type") // //$NON-NLS-1$
                        .combobox("security", securities) // //$NON-NLS-1$
                        .shares("shares") // //$NON-NLS-1$
                        .amount("amount") // //$NON-NLS-1$
                        .amount("fees") // //$NON-NLS-1$
                        .readonly("crossentry") //$NON-NLS-1$
                        .apply();

        hookContextMenu(transactions.getTable(), new IMenuListener()
        {
            public void menuAboutToShow(IMenuManager manager)
            {
                fillTransactionsContextMenu(manager);
            }
        });

        return container;
    }

    private void fillTransactionsContextMenu(IMenuManager manager)
    {
        final Portfolio portfolio = (Portfolio) transactions.getData(Portfolio.class.toString());
        if (portfolio == null)
            return;

        final PortfolioTransaction transaction = (PortfolioTransaction) ((IStructuredSelection) transactions
                        .getSelection()).getFirstElement();

        new SecurityContextMenu(this).menuAboutToShow(manager, transaction.getSecurity(), portfolio);

        if (transaction != null)
        {
            manager.add(new Separator());
            manager.add(new Action(Messages.MenuTransactionDelete)
            {
                @Override
                public void run()
                {
                    if (transaction.getCrossEntry() != null)
                        transaction.getCrossEntry().delete();
                    else
                        portfolio.getTransactions().remove(transaction);
                    markDirty();

                    portfolios.refresh(transactions.getData(Portfolio.class.toString()));
                    transactions.setInput(portfolio.getTransactions());
                    transactions.setSelection(new StructuredSelection(transaction), true);

                    statementOfAssets.setInput(PortfolioSnapshot.create(portfolio, Dates.today()));
                }
            });
        }
    }

    private Color colorFor(PortfolioTransaction t)
    {
        if (t.getType() == Type.SELL || t.getType() == Type.TRANSFER_OUT)
            return Display.getCurrent().getSystemColor(SWT.COLOR_DARK_RED);
        else
            return Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GREEN);
    }
}
