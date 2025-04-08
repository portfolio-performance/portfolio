package name.abuchen.portfolio.ui.views.columns;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.swt.SWT;

import name.abuchen.portfolio.model.Adaptor;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.viewers.Column;
import name.abuchen.portfolio.ui.util.viewers.ColumnViewerSorter;
import name.abuchen.portfolio.ui.util.viewers.StringEditingSupport;

public class SymbolColumn extends Column
{
    public SymbolColumn()
    {
        this("symbol"); //$NON-NLS-1$
    }

    public SymbolColumn(String id)
    {
        super(id, Messages.ColumnTicker, SWT.LEFT, 80);

        setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                return Adaptor.optionally(Security.class, e).map(Security::getTickerSymbol).orElse(null);
            }
        });

        setSorter(ColumnViewerSorter.createIgnoreCase(
                        e -> Adaptor.optionally(Security.class, e).map(Security::getTickerSymbol).orElse(null)));

        new StringEditingSupport(Security.class, "tickerSymbol") //$NON-NLS-1$
                        .setCanEditCheck(e -> Adaptor.optionally(Security.class, e).isPresent()).attachTo(this);
    }

}
