package name.abuchen.portfolio.ui.views.panes;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import com.google.common.base.Strings;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Adaptor;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.InvestmentPlan;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.TransactionPair;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.editor.AbstractFinanceView;
import name.abuchen.portfolio.ui.editor.PortfolioPart;
import name.abuchen.portfolio.ui.util.DropDown;
import name.abuchen.portfolio.ui.util.LabelOnly;
import name.abuchen.portfolio.ui.util.SimpleAction;
import name.abuchen.portfolio.ui.util.TableViewerCSVExporter;
import name.abuchen.portfolio.ui.views.AllTransactionsView.TransactionFilter;
import name.abuchen.portfolio.ui.views.TransactionsViewer;

public class TransactionsPane implements InformationPanePage
{
    private static final String TRANSACTION_FILTER_PREFERENCE_NAME = TransactionsPane.class.getSimpleName()
                    + "-transaction-type-filter"; //$NON-NLS-1$
    private static final TransactionFilter DEFAULT_TYPE_FILTER = TransactionFilter.NONE;

    private class FilterDropDown extends DropDown implements IMenuListener
    {
        public FilterDropDown(IPreferenceStore preferenceStore)
        {
            super(Messages.SecurityFilter, Images.FILTER_OFF, SWT.NONE);

            preferenceStore.setDefault(TRANSACTION_FILTER_PREFERENCE_NAME, DEFAULT_TYPE_FILTER.name());
            TransactionFilter transactionFilter = TransactionFilter
                            .valueOf(preferenceStore.getString(TRANSACTION_FILTER_PREFERENCE_NAME));
            typeFilter = transactionFilter;

            setMenuListener(this);

            updateIcon();

            addDisposeListener(e -> preferenceStore.setValue(TRANSACTION_FILTER_PREFERENCE_NAME, typeFilter.name()));
        }

        private void updateIcon()
        {
            boolean hasActiveFilter = typeFilter != DEFAULT_TYPE_FILTER;
            setImage(hasActiveFilter ? Images.FILTER_ON : Images.FILTER_OFF);
        }

        @Override
        public void menuAboutToShow(IMenuManager manager)
        {
            manager.add(new LabelOnly(Messages.TransactionFilter));
            for (TransactionFilter f : TransactionFilter.values())
                manager.add(addTypeFilter(f));
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

    private TransactionFilter typeFilter;

    @Inject
    private PortfolioPart part;

    @Inject
    private Client client;

    @Inject
    private AbstractFinanceView view;

    private TransactionsViewer transactions;

    private Object source;

    @Override
    public String getLabel()
    {
        return Messages.SecurityTabTransactions;
    }

    @Override
    public Control createViewControl(Composite parent)
    {
        transactions = new TransactionsViewer(TransactionsPane.class.getName(), parent, view);
        view.inject(transactions);

        transactions.addFilter(new ViewerFilter()
        {

            @Override
            public boolean select(Viewer viewer, Object parentElement, Object element)
            {
                TransactionPair<?> tx = (TransactionPair<?>) element;
                return typeFilter.matches(tx.getTransaction());
            }
        });

        return transactions.getControl();
    }

    @Override
    public void addButtons(ToolBarManager toolBar)
    {
        toolBar.add(new FilterDropDown(part.getPreferenceStore()));

        toolBar.add(new SimpleAction(Messages.MenuExportData, Images.EXPORT,
                        a -> new TableViewerCSVExporter(transactions.getTableViewer()).export(getLabel(), source)));

        toolBar.add(new DropDown(Messages.MenuShowHideColumns, Images.CONFIG, SWT.NONE,
                        manager -> transactions.getColumnSupport().menuAboutToShow(manager)));
    }

    @Override
    public void setInput(Object input)
    {
        // first check for the investment plan, because a plan can also be
        // adapted to a security (if it is a regular purchase) or an account (if
        // it is a regular transfer)

        InvestmentPlan investmentPlan = Adaptor.adapt(InvestmentPlan.class, input);
        if (investmentPlan != null)
        {
            source = investmentPlan;
            transactions.setInput(investmentPlan.getTransactions(client));
            return;
        }

        Security security = Adaptor.adapt(Security.class, input);
        if (security != null)
        {
            source = security;
            transactions.setInput(security.getTransactions(client));
            return;
        }

        Account account = Adaptor.adapt(Account.class, input);
        if (account != null)
        {
            source = account;
            transactions.setInput(account.getTransactions().stream().map(t -> new TransactionPair<>(account, t))
                            .collect(Collectors.toList()));
            return;
        }

        Portfolio portfolio = Adaptor.adapt(Portfolio.class, input);
        if (portfolio != null)
        {
            source = portfolio;
            transactions.setInput(portfolio.getTransactions().stream().map(t -> new TransactionPair<>(portfolio, t))
                            .collect(Collectors.toList()));
            return;
        }

        source = null;
        transactions.setInput(Collections.emptyList());
    }

    @Override
    public void onRecalculationNeeded()
    {
        if (source != null)
            setInput(source);
    }

    public void notifyModelUpdated()
    {
        onRecalculationNeeded();
    }

    public void markTransactions(List<TransactionPair<?>> list)
    {
        transactions.markTransactions(list);
    }
}
