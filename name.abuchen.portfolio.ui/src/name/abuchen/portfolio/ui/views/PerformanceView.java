package name.abuchen.portfolio.ui.views;

import java.text.MessageFormat;
import java.util.function.Function;

import javax.inject.Inject;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.layout.TreeColumnLayout;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.ToolBar;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.CrossEntry;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.model.TransactionOwner;
import name.abuchen.portfolio.model.TransactionPair;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.CurrencyConverterImpl;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.ClientPerformanceSnapshot;
import name.abuchen.portfolio.snapshot.GroupEarningsByAccount;
import name.abuchen.portfolio.snapshot.ReportingPeriod;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.AbstractDropDown;
import name.abuchen.portfolio.ui.util.ClientFilterDropDown;
import name.abuchen.portfolio.ui.util.SimpleAction;
import name.abuchen.portfolio.ui.util.TableViewerCSVExporter;
import name.abuchen.portfolio.ui.util.TreeViewerCSVExporter;
import name.abuchen.portfolio.ui.util.viewers.Column;
import name.abuchen.portfolio.ui.util.viewers.ColumnViewerSorter;
import name.abuchen.portfolio.ui.util.viewers.ShowHideColumnHelper;
import name.abuchen.portfolio.ui.views.columns.NoteColumn;

public class PerformanceView extends AbstractHistoricView
{
    @Inject
    private ExchangeRateProviderFactory factory;

    private ClientFilterDropDown clientFilter;

    private TreeViewer calculation;
    private StatementOfAssetsViewer snapshotStart;
    private StatementOfAssetsViewer snapshotEnd;
    private TableViewer earnings;
    private TableViewer earningsByAccount;
    private TableViewer taxes;
    private TableViewer fees;

    @Override
    protected String getDefaultTitle()
    {
        return Messages.LabelPerformanceCalculation;
    }

    @Override
    protected void addButtons(ToolBar toolBar)
    {
        super.addButtons(toolBar);

        this.clientFilter = new ClientFilterDropDown(toolBar, getClient(), getPreferenceStore(),
                        filter -> notifyModelUpdated());

        new ExportDropDown(toolBar); // NOSONAR
    }

    @Override
    public void reportingPeriodUpdated()
    {
        ReportingPeriod period = getReportingPeriod();
        CurrencyConverter converter = new CurrencyConverterImpl(factory, getClient().getBaseCurrency());
        Client filteredClient = clientFilter.getSelectedFilter().filter(getClient());
        ClientPerformanceSnapshot snapshot = new ClientPerformanceSnapshot(filteredClient, converter, period);

        try
        {
            calculation.getTree().setRedraw(false);
            calculation.setInput(snapshot);
            calculation.expandAll();
            calculation.getTree().getParent().layout();
        }
        finally
        {
            calculation.getTree().setRedraw(true);
        }

        snapshotStart.setInput(snapshot.getStartClientSnapshot(), clientFilter.getSelectedFilter());
        snapshotEnd.setInput(snapshot.getEndClientSnapshot(), clientFilter.getSelectedFilter());

        earnings.setInput(snapshot.getEarnings());
        earningsByAccount.setInput(new GroupEarningsByAccount(snapshot).getItems());
        taxes.setInput(snapshot.getTaxes());
        fees.setInput(snapshot.getFees());
    }

    @Override
    public void notifyModelUpdated()
    {
        reportingPeriodUpdated();
    }

    @Override
    protected Control createBody(Composite parent)
    {
        // result tabs
        CTabFolder folder = new CTabFolder(parent, SWT.BORDER);

        createCalculationItem(folder, Messages.PerformanceTabCalculation);
        snapshotStart = createStatementOfAssetsItem(folder, Messages.PerformanceTabAssetsAtStart);
        snapshotEnd = createStatementOfAssetsItem(folder, Messages.PerformanceTabAssetsAtEnd);
        earnings = createTransactionViewer(folder, Messages.PerformanceTabEarnings);
        createEarningsByAccountsItem(folder, Messages.PerformanceTabEarningsByAccount);
        taxes = createTransactionViewer(folder, Messages.PerformanceTabTaxes);
        fees = createTransactionViewer(folder, Messages.PerformanceTabFees);

        folder.setSelection(0);

        reportingPeriodUpdated();

        return folder;
    }

    private StatementOfAssetsViewer createStatementOfAssetsItem(CTabFolder folder, String title)
    {
        StatementOfAssetsViewer viewer = make(StatementOfAssetsViewer.class);
        Control control = viewer.createControl(folder);

        CTabItem item = new CTabItem(folder, SWT.NONE);
        item.setText(title);
        item.setControl(control);

        return viewer;
    }

    private void createCalculationItem(CTabFolder folder, String title)
    {
        Composite container = new Composite(folder, SWT.NONE);
        TreeColumnLayout layout = new TreeColumnLayout();
        container.setLayout(layout);

        calculation = new TreeViewer(container, SWT.FULL_SELECTION);

        final Font boldFont = JFaceResources.getFontRegistry().getBold(container.getFont().getFontData()[0].getName());

        TreeViewerColumn column = new TreeViewerColumn(calculation, SWT.NONE);
        column.getColumn().setText(Messages.ColumnLable);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                if (element instanceof ClientPerformanceSnapshot.Category)
                {
                    ClientPerformanceSnapshot.Category cat = (ClientPerformanceSnapshot.Category) element;
                    return cat.getLabel();
                }
                else if (element instanceof ClientPerformanceSnapshot.Position)
                {
                    ClientPerformanceSnapshot.Position pos = (ClientPerformanceSnapshot.Position) element;
                    return pos.getLabel();
                }
                return null;
            }

            @Override
            public Image getImage(Object element)
            {
                if (element instanceof ClientPerformanceSnapshot.Category)
                {
                    return Images.CATEGORY.image();
                }
                else if (element instanceof ClientPerformanceSnapshot.Position)
                {
                    ClientPerformanceSnapshot.Position position = (ClientPerformanceSnapshot.Position) element;

                    if (position.getSecurity() != null)
                    {
                        ClientPerformanceSnapshot snapshot = ((PerformanceContentProvider) calculation
                                        .getContentProvider()).getSnapshot();

                        boolean hasHoldings = snapshot.getEndClientSnapshot().getPositionsByVehicle()
                                        .get(position.getSecurity()) != null;

                        return hasHoldings ? Images.SECURITY.image() : Images.SECURITY_RETIRED.image();
                    }
                    else
                    {
                        return null;
                    }
                }

                return null;
            }

            @Override
            public Font getFont(Object element)
            {
                if (element instanceof ClientPerformanceSnapshot.Category)
                    return boldFont;
                return null;
            }
        });
        layout.setColumnData(column.getColumn(), new ColumnPixelData(350));

        column = new TreeViewerColumn(calculation, SWT.RIGHT);
        column.getColumn().setText(Messages.ColumnValue);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                if (element instanceof ClientPerformanceSnapshot.Category)
                {
                    ClientPerformanceSnapshot.Category cat = (ClientPerformanceSnapshot.Category) element;
                    return Values.Money.format(cat.getValuation(), getClient().getBaseCurrency());
                }
                else if (element instanceof ClientPerformanceSnapshot.Position)
                {
                    ClientPerformanceSnapshot.Position pos = (ClientPerformanceSnapshot.Position) element;
                    return Values.Money.format(pos.getValuation(), getClient().getBaseCurrency());
                }
                return null;
            }

            @Override
            public Font getFont(Object element)
            {
                if (element instanceof ClientPerformanceSnapshot.Category)
                    return boldFont;
                return null;
            }
        });

        layout.setColumnData(column.getColumn(), new ColumnPixelData(80));

        calculation.getTree().setHeaderVisible(true);
        calculation.getTree().setLinesVisible(true);

        calculation.setContentProvider(new PerformanceContentProvider());

        CTabItem item = new CTabItem(folder, SWT.NONE);
        item.setText(title);
        item.setControl(container);

        hookContextMenu(calculation.getTree(), this::fillContextMenu);
    }

    private void fillContextMenu(IMenuManager manager) // NOSONAR
    {
        Object selection = ((IStructuredSelection) calculation.getSelection()).getFirstElement();
        if (!(selection instanceof ClientPerformanceSnapshot.Position))
            return;

        Security security = ((ClientPerformanceSnapshot.Position) selection).getSecurity();
        new SecurityContextMenu(this).menuAboutToShow(manager, security);
    }

    private TableViewer createTransactionViewer(CTabFolder folder, String title)
    {
        Composite container = new Composite(folder, SWT.NONE);
        TableColumnLayout layout = new TableColumnLayout();
        container.setLayout(layout);

        TableViewer transactionViewer = new TableViewer(container, SWT.FULL_SELECTION);

        ShowHideColumnHelper support = new ShowHideColumnHelper(PerformanceView.class.getSimpleName() + "@2" + title, //$NON-NLS-1$
                        getPreferenceStore(), transactionViewer, layout);

        Column column = new Column(Messages.ColumnDate, SWT.None, 80);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                return Values.Date.format(((TransactionPair<?>) element).getTransaction().getDate());
            }
        });
        column.setSorter(ColumnViewerSorter.create(e -> ((TransactionPair<?>) e).getTransaction().getDate()), SWT.UP);
        support.addColumn(column);

        column = new Column(Messages.ColumnTransactionType, SWT.LEFT, 100);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                Transaction t = ((TransactionPair<?>) element).getTransaction();
                return t instanceof AccountTransaction ? ((AccountTransaction) t).getType().toString()
                                : ((PortfolioTransaction) t).getType().toString();
            }
        });
        column.setSorter(ColumnViewerSorter.create(e -> {
            Transaction t = ((TransactionPair<?>) e).getTransaction();
            return t instanceof AccountTransaction ? ((AccountTransaction) t).getType().toString()
                            : ((PortfolioTransaction) t).getType().toString();
        }));
        support.addColumn(column);

        column = new Column(Messages.ColumnAmount, SWT.RIGHT, 80);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                return Values.Money.format(((TransactionPair<?>) element).getTransaction().getMonetaryAmount(),
                                getClient().getBaseCurrency());
            }
        });
        column.setSorter(ColumnViewerSorter.create(e -> ((TransactionPair<?>) e).getTransaction().getMonetaryAmount()));
        support.addColumn(column);

        addTaxesColumn(support);
        addFeesColumn(support);
        addSecurityColumn(support);
        addPortfolioColumn(support);
        addAccountColumn(support);

        column = new NoteColumn();
        column.setEditingSupport(null);
        support.addColumn(column);

        support.createColumns();

        transactionViewer.getTable().setHeaderVisible(true);
        transactionViewer.getTable().setLinesVisible(true);

        transactionViewer.setContentProvider(ArrayContentProvider.getInstance());

        CTabItem item = new CTabItem(folder, SWT.NONE);
        item.setText(title);
        item.setControl(container);

        return transactionViewer;
    }

    private void addTaxesColumn(ShowHideColumnHelper support)
    {
        Column column = new Column(Messages.ColumnTaxes, SWT.RIGHT, 80);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                Transaction t = ((TransactionPair<?>) element).getTransaction();
                if (t instanceof AccountTransaction)
                {
                    AccountTransaction at = (AccountTransaction) t;

                    switch (at.getType())
                    {
                        case TAXES:
                            return Values.Money.format(at.getMonetaryAmount(), getClient().getBaseCurrency());
                        case TAX_REFUND:
                            return Values.Money.format(at.getMonetaryAmount().multiply(-1),
                                            getClient().getBaseCurrency());
                        default:
                            // do nothing -> print unit sum
                    }
                }

                return Values.Money.format(t.getUnitSum(Unit.Type.TAX), getClient().getBaseCurrency());
            }
        });
        column.setSorter(ColumnViewerSorter
                        .create(e -> ((TransactionPair<?>) e).getTransaction().getUnitSum(Unit.Type.TAX)));
        support.addColumn(column);
    }

    private void addFeesColumn(ShowHideColumnHelper support)
    {
        Column column = new Column(Messages.ColumnFees, SWT.RIGHT, 80);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                Transaction t = ((TransactionPair<?>) element).getTransaction();

                if (t instanceof AccountTransaction)
                {
                    AccountTransaction at = (AccountTransaction) t;
                    switch (at.getType())
                    {
                        case FEES:
                            return Values.Money.format(t.getMonetaryAmount(), getClient().getBaseCurrency());
                        case FEES_REFUND:
                            return Values.Money.format(t.getMonetaryAmount().multiply(-1),
                                            getClient().getBaseCurrency());
                        default:
                            // do nothing --> print unit sum
                    }
                }

                return Values.Money.format(t.getUnitSum(Unit.Type.FEE), getClient().getBaseCurrency());
            }
        });
        column.setSorter(ColumnViewerSorter
                        .create(e -> ((TransactionPair<?>) e).getTransaction().getUnitSum(Unit.Type.FEE)));
        support.addColumn(column);
    }

    private void addSecurityColumn(ShowHideColumnHelper support)
    {
        Column column = new Column(Messages.ColumnSecurity, SWT.LEFT, 250);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                Security security = ((TransactionPair<?>) element).getTransaction().getSecurity();
                return security != null ? security.getName() : null;
            }

            @Override
            public Image getImage(Object element)
            {
                Security security = ((TransactionPair<?>) element).getTransaction().getSecurity();
                return security != null ? Images.SECURITY.image() : null;
            }
        });
        column.setSorter(ColumnViewerSorter.create(e -> {
            Security security = ((TransactionPair<?>) e).getTransaction().getSecurity();
            return security != null ? security.getName() : null;
        }));
        support.addColumn(column);
    }

    private void addPortfolioColumn(ShowHideColumnHelper support)
    {
        Column column = new Column(Messages.ColumnPortfolio, SWT.LEFT, 100);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                Portfolio portfolio = ((TransactionPair<?>) element).getOwner() instanceof Portfolio
                                ? (Portfolio) ((TransactionPair<?>) element).getOwner() : null;
                return portfolio != null ? portfolio.getName() : null;
            }

            @Override
            public Image getImage(Object element)
            {
                Portfolio portfolio = ((TransactionPair<?>) element).getOwner() instanceof Portfolio
                                ? (Portfolio) ((TransactionPair<?>) element).getOwner() : null;
                return portfolio != null ? Images.PORTFOLIO.image() : null;
            }
        });
        column.setSorter(ColumnViewerSorter.create(e -> {
            Portfolio portfolio = ((TransactionPair<?>) e).getOwner() instanceof Portfolio
                            ? (Portfolio) ((TransactionPair<?>) e).getOwner() : null;
            return portfolio != null ? portfolio.getName() : null;
        }));
        support.addColumn(column);
    }

    private void addAccountColumn(ShowHideColumnHelper support)
    {
        Column column = new Column(Messages.ColumnAccount, SWT.LEFT, 100);

        Function<Object, Account> getAccount = element -> {
            TransactionPair<?> pair = (TransactionPair<?>) element;

            if (pair.getOwner() instanceof Account)
                return (Account) pair.getOwner();

            CrossEntry crossEntry = pair.getTransaction().getCrossEntry();
            if (crossEntry == null)
                return null;

            TransactionOwner<?> other = crossEntry.getCrossOwner(pair.getTransaction());
            return other instanceof Account ? ((Account) other) : null;
        };

        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                Account account = getAccount.apply(element);
                return account != null ? account.getName() : null;
            }

            @Override
            public Image getImage(Object element)
            {
                Account account = getAccount.apply(element);
                return account != null ? Images.ACCOUNT.image() : null;
            }
        });
        column.setSorter(ColumnViewerSorter.create(e -> {
            Account account = getAccount.apply(e);
            return account != null ? account.getName() : null;
        }));
        support.addColumn(column);
    }

    private void createEarningsByAccountsItem(CTabFolder folder, String title)
    {
        Composite container = new Composite(folder, SWT.NONE);
        TableColumnLayout layout = new TableColumnLayout();
        container.setLayout(layout);

        earningsByAccount = new TableViewer(container, SWT.FULL_SELECTION);

        ShowHideColumnHelper support = new ShowHideColumnHelper(PerformanceView.class.getSimpleName() + "@byaccounts", //$NON-NLS-1$
                        getPreferenceStore(), earningsByAccount, layout);

        Column column = new Column(Messages.ColumnSource, SWT.LEFT, 400);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                GroupEarningsByAccount.Item item = (GroupEarningsByAccount.Item) element;
                return item.getAccount().getName();
            }

            @Override
            public Image getImage(Object element)
            {
                return Images.ACCOUNT.image();
            }
        });
        column.setSorter(ColumnViewerSorter.create(GroupEarningsByAccount.Item.class, "account")); //$NON-NLS-1$
        support.addColumn(column);

        column = new Column(Messages.ColumnAmount, SWT.RIGHT, 80);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                GroupEarningsByAccount.Item item = (GroupEarningsByAccount.Item) element;
                return Values.Money.format(item.getSum(), getClient().getBaseCurrency());
            }
        });
        column.setSorter(ColumnViewerSorter.create(GroupEarningsByAccount.Item.class, "sum")); //$NON-NLS-1$
        support.addColumn(column);

        support.createColumns();

        earningsByAccount.getTable().setHeaderVisible(true);
        earningsByAccount.getTable().setLinesVisible(true);

        earningsByAccount.setContentProvider(ArrayContentProvider.getInstance());

        CTabItem item = new CTabItem(folder, SWT.NONE);
        item.setText(title);
        item.setControl(container);
    }

    private static class PerformanceContentProvider implements ITreeContentProvider
    {
        private ClientPerformanceSnapshot snapshot;
        private ClientPerformanceSnapshot.Category[] categories;

        @Override
        public void inputChanged(Viewer viewer, Object oldInput, Object newInput)
        {
            if (newInput == null)
            {
                this.snapshot = null;
                this.categories = new ClientPerformanceSnapshot.Category[0];
            }
            else if (newInput instanceof ClientPerformanceSnapshot)
            {
                this.snapshot = (ClientPerformanceSnapshot) newInput;
                this.categories = snapshot.getCategories().toArray(new ClientPerformanceSnapshot.Category[0]);
            }
            else
            {
                throw new IllegalArgumentException("Unsupported type: " + newInput.getClass().getName()); //$NON-NLS-1$
            }
        }

        @Override
        public Object[] getElements(Object inputElement)
        {
            return this.categories;
        }

        @Override
        public Object[] getChildren(Object parentElement)
        {
            if (parentElement instanceof ClientPerformanceSnapshot.Category)
                return ((ClientPerformanceSnapshot.Category) parentElement).getPositions()
                                .toArray(new ClientPerformanceSnapshot.Position[0]);
            return new Object[0];
        }

        @Override
        public Object getParent(Object element)
        {
            if (element instanceof ClientPerformanceSnapshot.Position)
            {
                for (ClientPerformanceSnapshot.Category c : categories)
                {
                    if (c.getPositions().contains(element))
                        return c;
                }

            }
            return null;
        }

        @Override
        public boolean hasChildren(Object element)
        {
            return element instanceof ClientPerformanceSnapshot.Category
                            && !((ClientPerformanceSnapshot.Category) element).getPositions().isEmpty();
        }

        @Override
        public void dispose()
        {
            // no resources to dispose
        }

        public ClientPerformanceSnapshot getSnapshot()
        {
            return snapshot;
        }
    }

    private final class ExportDropDown extends AbstractDropDown
    {
        private ExportDropDown(ToolBar toolBar)
        {
            super(toolBar, Messages.MenuExportData, Images.EXPORT.image(), SWT.NONE);
        }

        @Override
        public void menuAboutToShow(IMenuManager manager)
        {
            final String fileSuffix = ".csv"; //$NON-NLS-1$

            manager.add(new SimpleAction(MessageFormat.format(Messages.LabelExport, Messages.PerformanceTabCalculation),
                            a -> new TreeViewerCSVExporter(calculation)
                                            .export(Messages.PerformanceTabCalculation + fileSuffix)));

            manager.add(new SimpleAction(
                            MessageFormat.format(Messages.LabelExport, Messages.PerformanceTabAssetsAtStart),
                            a -> new TableViewerCSVExporter(snapshotStart.getTableViewer())
                                            .export(Messages.PerformanceTabAssetsAtStart + fileSuffix)));

            manager.add(new SimpleAction(MessageFormat.format(Messages.LabelExport, Messages.PerformanceTabAssetsAtEnd),
                            a -> new TableViewerCSVExporter(snapshotEnd.getTableViewer())
                                            .export(Messages.PerformanceTabAssetsAtEnd + fileSuffix)));

            manager.add(new SimpleAction(MessageFormat.format(Messages.LabelExport, Messages.PerformanceTabEarnings),
                            a -> new TableViewerCSVExporter(earnings)
                                            .export(Messages.PerformanceTabEarnings + fileSuffix)));

            manager.add(new SimpleAction(
                            MessageFormat.format(Messages.LabelExport, Messages.PerformanceTabEarningsByAccount),
                            a -> new TableViewerCSVExporter(earningsByAccount)
                                            .export(Messages.PerformanceTabEarningsByAccount + fileSuffix)));

            manager.add(new SimpleAction(MessageFormat.format(Messages.LabelExport, Messages.PerformanceTabTaxes),
                            a -> new TableViewerCSVExporter(taxes).export(Messages.PerformanceTabTaxes + fileSuffix)));

            manager.add(new SimpleAction(MessageFormat.format(Messages.LabelExport, Messages.PerformanceTabFees),
                            a -> new TableViewerCSVExporter(fees).export(Messages.PerformanceTabFees + fileSuffix)));

        }
    }

}
