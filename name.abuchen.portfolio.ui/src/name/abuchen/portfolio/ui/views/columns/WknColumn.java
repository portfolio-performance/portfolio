package name.abuchen.portfolio.ui.views.columns;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.swt.SWT;

import name.abuchen.portfolio.model.Adaptor;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.viewers.Column;
import name.abuchen.portfolio.ui.util.viewers.ColumnViewerSorter;
import name.abuchen.portfolio.ui.util.viewers.StringEditingSupport;

public class WknColumn extends Column
{
    public WknColumn()
    {
        this("wkn"); //$NON-NLS-1$
    }

    public WknColumn(String id)
    {
        super(id, Messages.ColumnWKN, SWT.LEFT, 80);

        setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                return Adaptor.optionally(Security.class, e).map(Security::getWkn).orElse(null);
            }
        });

        setSorter(ColumnViewerSorter.createIgnoreCase(
                        e -> Adaptor.optionally(Security.class, e).map(Security::getWkn).orElse(null)));

        new StringEditingSupport(Security.class, "wkn") //$NON-NLS-1$
                        .setCanEditCheck(e -> Adaptor.optionally(Security.class, e)
                                        .map(s -> !s.isExchangeRate() && s.getOnlineId() == null).orElse(false))
                        .attachTo(this);
    }

}
