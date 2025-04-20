package name.abuchen.portfolio.ui.util.searchfilter;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import org.eclipse.jface.action.ControlContribution;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;

import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.TransactionPair;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.util.TextUtil;

public class TransactionSearchField extends ControlContribution
{
    private String filterText;
    private Consumer<String> onRecalculationNeeded;

    public TransactionSearchField(Consumer<String> onRecalculationNeeded)
    {
        super("searchbox"); //$NON-NLS-1$
        this.onRecalculationNeeded = onRecalculationNeeded;
    }

    public String getFilterText()
    {
        return filterText;
    }

    @Override
    protected Control createControl(Composite parent)
    {
        final Text search = new Text(parent, SWT.SEARCH | SWT.ICON_CANCEL);
        search.setMessage(Messages.LabelSearch);
        search.setSize(300, SWT.DEFAULT);

        // reset filterText when user switch tab
        search.addDisposeListener(e -> {
            filterText = null;
            onRecalculationNeeded.accept(filterText);
        });

        search.addModifyListener(e -> {
            var text = search.getText().trim();
            if (text.isEmpty())
            {
                filterText = null;
                onRecalculationNeeded.accept(filterText);
            }
            else
            {
                filterText = text.toLowerCase();
                onRecalculationNeeded.accept(filterText);
            }
        });

        return search;
    }

    @Override
    protected int computeWidth(Control control)
    {
        return control.computeSize(100, SWT.DEFAULT, true).x;
    }

    public ViewerFilter getViewerFilter(Function<Object, TransactionPair<?>> transaction)
    {
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

        return new ViewerFilter()
        {
            @Override
            public Object[] filter(Viewer viewer, Object parent, Object[] elements)
            {
                return filterText == null ? elements : super.filter(viewer, parent, elements);
            }

            @Override
            public boolean select(Viewer viewer, Object parentElement, Object element)
            {
                TransactionPair<?> tx = transaction.apply(element);

                for (Function<TransactionPair<?>, Object> label : searchLabels)
                {
                    Object l = label.apply(tx);
                    if (l == null)
                        continue;

                    // If this is a numeric field, do a numeric comparison
                    // to handle formatting differences (commas, periods, etc.)
                    if (l instanceof Money money) // NOSONAR
                    {
                        if (TextUtil.isNumericSearchMatch(filterText, money.getAmount() / Values.Money.divider()))
                            return true;
                    }
                    else if (l instanceof Long number)
                    {
                        if (TextUtil.isNumericSearchMatch(filterText, number.doubleValue() / Values.Share.divider()))
                            return true;
                    }
                    else if (l instanceof Number number)
                    {
                        if (TextUtil.isNumericSearchMatch(filterText, number.doubleValue()))
                            return true;
                    }
                    else
                    {
                        if (l.toString().toLowerCase().indexOf(filterText) >= 0)
                            return true;
                    }
                }

                return false;
            }
        };
    }

}
