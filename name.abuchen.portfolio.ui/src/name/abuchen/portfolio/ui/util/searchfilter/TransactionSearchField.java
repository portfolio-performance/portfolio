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
import name.abuchen.portfolio.ui.Messages;

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

        search.addModifyListener(e -> {
            var text = search.getText().trim();
            if (text.length() == 0)
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
                    if (l != null && l.toString().toLowerCase().indexOf(filterText) >= 0)
                        return true;
                }

                return false;
            }
        };
    }
}
