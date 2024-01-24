package name.abuchen.portfolio.ui.views;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ControlContribution;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Text;

import com.google.common.base.Strings;

import name.abuchen.portfolio.json.JClient;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.model.TransactionPair;
import name.abuchen.portfolio.snapshot.filter.ClientFilter;
import name.abuchen.portfolio.snapshot.filter.PortfolioClientFilter;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.editor.AbstractFinanceView;
import name.abuchen.portfolio.ui.util.ClientFilterMenu;
import name.abuchen.portfolio.ui.util.DropDown;
import name.abuchen.portfolio.ui.util.LabelOnly;
import name.abuchen.portfolio.ui.util.SimpleAction;
import name.abuchen.portfolio.ui.util.TableViewerCSVExporter;
import name.abuchen.portfolio.ui.views.panes.HistoricalPricesPane;
import name.abuchen.portfolio.ui.views.panes.InformationPanePage;
import name.abuchen.portfolio.ui.views.panes.SecurityPriceChartPane;
import name.abuchen.portfolio.ui.views.panes.TradesPane;
import name.abuchen.portfolio.ui.views.panes.TransactionsPane;
import name.abuchen.portfolio.util.TextUtil;

public class AllTransactionsView extends AbstractFinanceView
{
    private enum TransactionFilter
    {
        NONE(Messages.TransactionFilterNone, 0, tx -> true), //
        SECURITY_TRANSACTIONS(Messages.TransactionFilterSecurityRelated, 0, tx -> {
            if (tx instanceof PortfolioTransaction)
                return true;
            else if (tx instanceof AccountTransaction atx)
                return atx.getType() == AccountTransaction.Type.DIVIDENDS
                                || atx.getType() == AccountTransaction.Type.BUY
                                || atx.getType() == AccountTransaction.Type.SELL;
            else
                return false;
        }), //
        BUY_AND_SELL(Messages.TransactionFilterBuyAndSell, 1, tx -> {
            if (tx instanceof PortfolioTransaction ptx)
                return ptx.getType() == PortfolioTransaction.Type.BUY
                                || ptx.getType() == PortfolioTransaction.Type.SELL;
            else if (tx instanceof AccountTransaction atx)
                return atx.getType() == AccountTransaction.Type.BUY || atx.getType() == AccountTransaction.Type.SELL;
            else
                return false;
        }), //
        BUY(Messages.TransactionFilterBuy, 2, tx -> {
            if (tx instanceof PortfolioTransaction ptx)
                return ptx.getType() == PortfolioTransaction.Type.BUY;
            else if (tx instanceof AccountTransaction atx)
                return atx.getType() == AccountTransaction.Type.BUY;
            else
                return false;
        }), //
        SELL(Messages.TransactionFilterSell, 2, tx -> {
            if (tx instanceof PortfolioTransaction ptx)
                return ptx.getType() == PortfolioTransaction.Type.SELL;
            else if (tx instanceof AccountTransaction atx)
                return atx.getType() == AccountTransaction.Type.SELL;
            else
                return false;
        }), //
        DIVIDEND(Messages.TransactionFilterDividend, 1, tx -> {
            if (tx instanceof AccountTransaction atx)
                return atx.getType() == AccountTransaction.Type.DIVIDENDS;
            else
                return false;
        }), //
        DEPOSIT_AND_REMOVAL(Messages.TransactionFilterDepositAndRemoval, 0, tx -> {
            if (tx instanceof AccountTransaction atx)
                return atx.getType() == AccountTransaction.Type.DEPOSIT
                                || atx.getType() == AccountTransaction.Type.REMOVAL;
            else
                return false;
        }), //
        DEPOSIT(Messages.TransactionFilterDeposit, 1, tx -> {
            if (tx instanceof AccountTransaction atx)
                return atx.getType() == AccountTransaction.Type.DEPOSIT;
            else
                return false;
        }), //
        REMOVAL(Messages.TransactionFilterRemoval, 1, tx -> {
            if (tx instanceof AccountTransaction atx)
                return atx.getType() == AccountTransaction.Type.REMOVAL;
            else
                return false;
        }), //
        INTEREST(Messages.TransactionFilterInterest, 0, tx -> {
            if (tx instanceof AccountTransaction atx)
                return atx.getType() == AccountTransaction.Type.INTEREST
                                || atx.getType() == AccountTransaction.Type.INTEREST_CHARGE;
            else
                return false;
        }), //
        WITH_TAX(Messages.TransactionFilterTaxes, 0, tx -> {
            if (tx instanceof AccountTransaction atx && (atx.getType() == AccountTransaction.Type.TAXES
                            || atx.getType() == AccountTransaction.Type.TAX_REFUND))
                return true;
            return tx.getUnitSum(Transaction.Unit.Type.TAX).isPositive();
        }), //
        WITH_FEES(Messages.TransactionFilterFees, 0, tx -> {
            if (tx instanceof AccountTransaction atx && (atx.getType() == AccountTransaction.Type.FEES
                            || atx.getType() == AccountTransaction.Type.FEES_REFUND))
                return true;
            return tx.getUnitSum(Transaction.Unit.Type.FEE).isPositive();
        }), //
        TRANSFERS(Messages.TransactionFilterTransfers, 0, tx -> {
            if (tx instanceof PortfolioTransaction ptx)
                return ptx.getType() == PortfolioTransaction.Type.TRANSFER_IN
                                || ptx.getType() == PortfolioTransaction.Type.TRANSFER_OUT;
            else if (tx instanceof AccountTransaction atx)
                return atx.getType() == AccountTransaction.Type.TRANSFER_IN
                                || atx.getType() == AccountTransaction.Type.TRANSFER_OUT;
            else
                return false;
        }), //
        DELIVERIES(Messages.TransactionFilterDeliveries, 0, tx -> {
            if (tx instanceof PortfolioTransaction ptx)
                return ptx.getType() == PortfolioTransaction.Type.DELIVERY_INBOUND
                                || ptx.getType() == PortfolioTransaction.Type.DELIVERY_OUTBOUND;
            else
                return false;
        }), //
        DELIVERIES_INBOUND(PortfolioTransaction.Type.DELIVERY_INBOUND.toString(), 1, tx -> {
            if (tx instanceof PortfolioTransaction ptx)
                return ptx.getType() == PortfolioTransaction.Type.DELIVERY_INBOUND;
            else
                return false;
        }), //
        DELIVERIES_OUTBOUND(PortfolioTransaction.Type.DELIVERY_OUTBOUND.toString(), 1, tx -> {
            if (tx instanceof PortfolioTransaction ptx)
                return ptx.getType() == PortfolioTransaction.Type.DELIVERY_OUTBOUND;
            else
                return false;
        });

        private String name;
        private int level;
        private Predicate<Transaction> predicate;

        TransactionFilter(String name, int level, Predicate<Transaction> predicate)
        {
            this.name = name;
            this.level = level;
            this.predicate = predicate;
        }

        boolean matches(Transaction tx)
        {
            return predicate.test(tx);
        }

        String getName()
        {
            return name;
        }

        int getLevel()
        {
            return level;
        }
    }

    private static final String TRANSACTION_FILTER_PREFERENCE_NAME = AllTransactionsView.class.getSimpleName()
                    + "-transaction-type-filter"; //$NON-NLS-1$
    private static final TransactionFilter DEFAULT_TYPE_FILTER = TransactionFilter.NONE;

    private class FilterDropDown extends DropDown implements IMenuListener
    {
        private ClientFilterMenu clientFilterMenu;

        public FilterDropDown(IPreferenceStore preferenceStore)
        {
            super(Messages.SecurityFilter, Images.FILTER_OFF, SWT.NONE);

            preferenceStore.setDefault(TRANSACTION_FILTER_PREFERENCE_NAME, DEFAULT_TYPE_FILTER.name());
            TransactionFilter transactionFilter = TransactionFilter
                            .valueOf(preferenceStore.getString(TRANSACTION_FILTER_PREFERENCE_NAME));
            typeFilter = transactionFilter;

            setMenuListener(this);

            this.clientFilterMenu = new ClientFilterMenu(getClient(), getPreferenceStore());

            Consumer<ClientFilter> listener = f -> {
                setInformationPaneInput(null);
                setupFilterInView(f);
                notifyModelUpdated();
            };

            clientFilterMenu.addListener(listener);
            clientFilterMenu.addListener(f -> updateIcon());

            clientFilterMenu.trackSelectedFilterConfigurationKey(AllTransactionsView.class.getSimpleName());

            // set initial filter
            setupFilterInView(clientFilterMenu.getSelectedFilter());

            updateIcon();

            addDisposeListener(e -> preferenceStore.setValue(TRANSACTION_FILTER_PREFERENCE_NAME, typeFilter.name()));
        }

        private void setupFilterInView(ClientFilter filter)
        {
            if (filter instanceof PortfolioClientFilter pcf)
                AllTransactionsView.this.clientFilter = pcf;
            else
                AllTransactionsView.this.clientFilter = null;
        }

        private void updateIcon()
        {
            boolean hasActiveFilter = clientFilterMenu.hasActiveFilter() || typeFilter != DEFAULT_TYPE_FILTER;
            setImage(hasActiveFilter ? Images.FILTER_ON : Images.FILTER_OFF);
        }

        @Override
        public void menuAboutToShow(IMenuManager manager)
        {
            manager.add(new LabelOnly(Messages.TransactionFilter));
            for (TransactionFilter f : TransactionFilter.values())
                manager.add(addTypeFilter(f));

            manager.add(new Separator());
            manager.add(new LabelOnly(Messages.MenuChooseClientFilter));
            clientFilterMenu.menuAboutToShow(manager);
        }

        private Action addTypeFilter(TransactionFilter filter)
        {
            Action action = new Action(Strings.repeat(" ", filter.getLevel() * 2) + filter.getName(), //$NON-NLS-1$
                            IAction.AS_CHECK_BOX)
            {
                @Override
                public void run()
                {
                    boolean isChecked = typeFilter == filter;

                    // only one TransactionFilter can be selected at a time
                    if (isChecked)
                        typeFilter = DEFAULT_TYPE_FILTER;
                    else
                        typeFilter = filter;

                    updateIcon();
                    notifyModelUpdated();
                }
            };
            action.setChecked(typeFilter == filter);
            return action;
        }
    }

    private TransactionsViewer table;

    private String filter;
    private PortfolioClientFilter clientFilter;
    private TransactionFilter typeFilter;

    @Override
    public void notifyModelUpdated()
    {
        table.setInput(getClient().getAllTransactions());
    }

    @Override
    protected String getDefaultTitle()
    {
        return Messages.LabelAllTransactions;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void addButtons(ToolBarManager toolBar)
    {
        addSearchButton(toolBar);

        toolBar.add(new Separator());

        toolBar.add(new FilterDropDown(getPreferenceStore()));

        toolBar.add(new DropDown(Messages.MenuExportData, Images.EXPORT, SWT.NONE, manager -> {
            manager.add(new SimpleAction(Messages.LabelAllTransactions + " (CSV)", //$NON-NLS-1$
                            a -> new TableViewerCSVExporter(table.getTableViewer())
                                            .export(Messages.LabelAllTransactions + ".csv"))); //$NON-NLS-1$

            manager.add(new SimpleAction(Messages.LabelAllTransactions + " (JSON)", //$NON-NLS-1$
                            exportToJSON(Messages.LabelAllTransactions + ".json", //$NON-NLS-1$
                                            (List<TransactionPair<?>>) table.getTableViewer().getInput())));

            IStructuredSelection selection = table.getTableViewer().getStructuredSelection();
            if (!selection.isEmpty())
            {
                manager.add(new SimpleAction(Messages.LabelSelectedTransactions + " (CSV)", //$NON-NLS-1$
                                a -> new TableViewerCSVExporter(table.getTableViewer(), true)
                                                .export(Messages.LabelSelectedTransactions + ".csv"))); //$NON-NLS-1$

                manager.add(new SimpleAction(Messages.LabelSelectedTransactions + " (JSON)", //$NON-NLS-1$
                                exportToJSON(Messages.LabelSelectedTransactions + ".json", selection.toList()))); //$NON-NLS-1$
            }

        }));

        toolBar.add(new DropDown(Messages.MenuShowHideColumns, Images.CONFIG, SWT.NONE,
                        manager -> table.getColumnSupport().menuAboutToShow(manager)));
    }

    private void addSearchButton(ToolBarManager toolBar)
    {
        toolBar.add(new ControlContribution("searchbox") //$NON-NLS-1$
        {
            @Override
            protected Control createControl(Composite parent)
            {
                final Text search = new Text(parent, SWT.SEARCH | SWT.ICON_CANCEL);
                search.setMessage(Messages.LabelSearch);
                search.setSize(300, SWT.DEFAULT);

                search.addModifyListener(e -> {
                    String filterText = search.getText().trim();
                    if (filterText.length() == 0)
                    {
                        filter = null;
                        table.refresh(false);
                    }
                    else
                    {
                        filter = filterText.toLowerCase();
                        table.refresh(false);
                    }
                });

                return search;
            }

            @Override
            protected int computeWidth(Control control)
            {
                return control.computeSize(100, SWT.DEFAULT, true).x;
            }
        });
    }

    @Override
    protected Control createBody(Composite parent)
    {
        table = new TransactionsViewer(AllTransactionsView.class.getName(), parent, this);
        inject(table);

        List<Function<TransactionPair<?>, Object>> searchLabels = new ArrayList<>();
        searchLabels.add(tx -> tx.getTransaction().getSecurity());
        searchLabels.add(tx -> tx.getTransaction().getOptionalSecurity().map(Security::getIsin).orElse(null));
        searchLabels.add(tx -> tx.getTransaction().getOptionalSecurity().map(Security::getWkn).orElse(null));
        searchLabels.add(tx -> tx.getTransaction().getOptionalSecurity().map(Security::getTickerSymbol).orElse(null));
        searchLabels.add(TransactionPair::getOwner);
        searchLabels.add(tx -> tx.getTransaction().getCrossEntry() != null
                        ? tx.getTransaction().getCrossEntry().getCrossOwner(tx.getTransaction())
                        : null);
        searchLabels.add(tx -> tx.getTransaction() instanceof AccountTransaction
                        ? ((AccountTransaction) tx.getTransaction()).getType()
                        : ((PortfolioTransaction) tx.getTransaction()).getType());
        searchLabels.add(tx -> tx.getTransaction().getNote());
        searchLabels.add(tx -> tx.getTransaction().getShares());
        searchLabels.add(tx -> tx.getTransaction().getMonetaryAmount());

        table.addFilter(new ViewerFilter()
        {
            @Override
            public Object[] filter(Viewer viewer, Object parent, Object[] elements)
            {
                return filter == null ? elements : super.filter(viewer, parent, elements);
            }

            @Override
            public boolean select(Viewer viewer, Object parentElement, Object element)
            {
                TransactionPair<?> tx = (TransactionPair<?>) element;

                for (Function<TransactionPair<?>, Object> label : searchLabels)
                {
                    Object l = label.apply(tx);
                    if (l != null && l.toString().toLowerCase().indexOf(filter) >= 0)
                        return true;
                }

                return false;
            }
        });

        table.addFilter(new ViewerFilter()
        {

            @Override
            public boolean select(Viewer viewer, Object parentElement, Object element)
            {
                TransactionPair<?> tx = (TransactionPair<?>) element;
                return typeFilter.matches(tx.getTransaction());
            }
        });

        // We filter the table like this and not by using the
        // clientFilter#filter(Client) method, because that modifies some
        // transactions (e.g. removes the cross entry for purchases/sells and
        // adds fictitious removals for dividend payments when only the
        // portfolio without the account passes the filter)
        table.addFilter(new ViewerFilter()
        {

            @Override
            public boolean select(Viewer viewer, Object parentElement, Object element)
            {
                if (clientFilter == null)
                    return true;
                TransactionPair<?> tx = (TransactionPair<?>) element;
                // check owner and cross owner
                return clientFilter.hasElement(tx.getOwner())
                                || (tx.getTransaction().getCrossEntry() != null && clientFilter.hasElement(tx
                                                .getTransaction().getCrossEntry() //
                                                .getCrossOwner(tx.getTransaction())));
            }
        });

        table.getTableViewer().addSelectionChangedListener(
                        e -> setInformationPaneInput(e.getStructuredSelection().getFirstElement()));

        notifyModelUpdated();

        return table.getControl();
    }

    @Override
    protected void addPanePages(List<InformationPanePage> pages)
    {
        super.addPanePages(pages);
        pages.add(make(SecurityPriceChartPane.class));
        pages.add(make(HistoricalPricesPane.class));
        pages.add(make(TransactionsPane.class));
        pages.add(make(TradesPane.class));
    }

    private SimpleAction.Runnable exportToJSON(String filename, List<TransactionPair<?>> transactions)
    {
        return action -> {
            FileDialog dialog = new FileDialog(Display.getDefault().getActiveShell(), SWT.SAVE);
            dialog.setFileName(TextUtil.sanitizeFilename(filename));
            dialog.setOverwrite(true);
            String name = dialog.open();
            if (name == null)
                return;

            File file = new File(name);

            try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))
            {
                writer.append(JClient.from(transactions).toJson());
            }
            catch (IOException e)
            {
                PortfolioPlugin.log(e);
                MessageDialog.openError(Display.getDefault().getActiveShell(), Messages.LabelError, e.getMessage());
            }
        };
    }
}
