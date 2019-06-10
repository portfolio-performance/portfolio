package name.abuchen.portfolio.ui.views;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Function;

import org.eclipse.jface.action.ControlContribution;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.TransactionPair;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.editor.AbstractFinanceView;
import name.abuchen.portfolio.ui.util.DropDown;

public class AllTransactionsView extends AbstractFinanceView
{
    private TransactionsViewer table;

    private String filter;

    @Override
    public void notifyModelUpdated()
    {
        List<TransactionPair<?>> transactions = new ArrayList<>();

        for (Portfolio portfolio : getClient().getPortfolios())
            portfolio.getTransactions().stream().filter(t -> t.getType() != PortfolioTransaction.Type.TRANSFER_IN)
                            .map(t -> new TransactionPair<>(portfolio, t)).forEach(transactions::add);

        EnumSet<AccountTransaction.Type> exclude = EnumSet.of(AccountTransaction.Type.TRANSFER_IN,
                        AccountTransaction.Type.BUY, AccountTransaction.Type.SELL);

        for (Account account : getClient().getAccounts())
        {
            account.getTransactions().stream().filter(t -> !exclude.contains(t.getType()))
                            .map(t -> new TransactionPair<>(account, t)).forEach(transactions::add);
        }

        table.setInput(transactions);
    }

    @Override
    protected String getDefaultTitle()
    {
        return Messages.LabelAllTransactions;
    }

    @Override
    protected void addButtons(ToolBarManager toolBar)
    {
        addSearchButton(toolBar);

        toolBar.add(new Separator());

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

        notifyModelUpdated();

        return table.getControl();
    }
}
