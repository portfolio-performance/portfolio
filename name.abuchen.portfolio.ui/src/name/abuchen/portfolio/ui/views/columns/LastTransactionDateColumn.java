package name.abuchen.portfolio.ui.views.columns;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.SWT;

import name.abuchen.portfolio.model.Adaptor;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.model.TransactionOwner;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.viewers.Column;
import name.abuchen.portfolio.ui.util.viewers.ColumnViewerSorter;
import name.abuchen.portfolio.ui.util.viewers.DateTimeLabelProvider;

public class LastTransactionDateColumn extends Column implements Column.CacheInvalidationListener
{
    private final Map<TransactionOwner<?>, LocalDateTime> cache = new HashMap<>();

    public LastTransactionDateColumn()
    {
        super("last-date", Messages.ColumnLastTransactionDate, SWT.RIGHT, 80); //$NON-NLS-1$

        setLabelProvider(new DateTimeLabelProvider(this::getOrCompute));
        setSorter(ColumnViewerSorter.create(this::getOrCompute));
        setVisible(false);
    }

    private LocalDateTime getOrCompute(Object element)
    {
        var owner = Adaptor.adapt(TransactionOwner.class, element);
        if (owner == null)
            return null;

        return cache.computeIfAbsent(owner, o -> o.getTransactions().stream().max(Transaction.BY_DATE) //
                        .map(Transaction::getDateTime).orElse(null));
    }

    @Override
    public void invalidateCache()
    {
        cache.clear();
    }
}
