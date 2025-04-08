package name.abuchen.portfolio.ui.views.panes;

import static name.abuchen.portfolio.util.CollectorsUtil.toMutableList;

import java.util.Collections;
import java.util.List;

import jakarta.inject.Inject;

import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

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
import name.abuchen.portfolio.ui.util.DropDown;
import name.abuchen.portfolio.ui.util.SimpleAction;
import name.abuchen.portfolio.ui.util.TableViewerCSVExporter;
import name.abuchen.portfolio.ui.util.searchfilter.TransactionFilterDropDown;
import name.abuchen.portfolio.ui.util.searchfilter.TransactionSearchField;
import name.abuchen.portfolio.ui.views.TransactionsViewer;

public class TransactionsPane implements InformationPanePage
{

    @Inject
    private Client client;

    @Inject
    private AbstractFinanceView view;

    private TransactionsViewer transactions;
    private TransactionSearchField textFilter;
    private TransactionFilterDropDown transactionFilter;

    private Object source;

    @Inject
    public TransactionsPane(IPreferenceStore preferenceStore)
    {
        transactionFilter = new TransactionFilterDropDown(preferenceStore,
                        TransactionsPane.class.getSimpleName() + "-transaction-type-filter", //$NON-NLS-1$
                        criteria -> onRecalculationNeeded());

        textFilter = new TransactionSearchField(text -> onRecalculationNeeded());
    }

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

        transactions.addFilter(textFilter.getViewerFilter(element -> (TransactionPair<?>) element));

        transactions.addFilter(new ViewerFilter()
        {
            @Override
            public boolean select(Viewer viewer, Object parentElement, Object element)
            {
                TransactionPair<?> tx = (TransactionPair<?>) element;
                return transactionFilter.getFilterCriteria().matches(tx.getTransaction());
            }
        });

        return transactions.getControl();
    }

    @Override
    public void addButtons(ToolBarManager toolBar)
    {
        toolBar.add(textFilter);

        toolBar.add(new Separator());

        transactionFilter.dispose();
        toolBar.add(transactionFilter);

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
                            .collect(toMutableList()));
            return;
        }

        Portfolio portfolio = Adaptor.adapt(Portfolio.class, input);
        if (portfolio != null)
        {
            source = portfolio;
            transactions.setInput(portfolio.getTransactions().stream().map(t -> new TransactionPair<>(portfolio, t))
                            .collect(toMutableList()));
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
