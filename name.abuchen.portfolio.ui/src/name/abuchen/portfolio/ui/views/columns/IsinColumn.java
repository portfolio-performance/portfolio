package name.abuchen.portfolio.ui.views.columns;

import java.text.MessageFormat;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.swt.SWT;

import name.abuchen.portfolio.model.Adaptor;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.viewers.Column;
import name.abuchen.portfolio.ui.util.viewers.ColumnEditingSupport;
import name.abuchen.portfolio.ui.util.viewers.ColumnViewerSorter;
import name.abuchen.portfolio.util.Isin;

public class IsinColumn extends Column
{
    public static class IsinEditingSupport extends ColumnEditingSupport
    {
        @Override
        public boolean canEdit(Object element)
        {
            Security s = Adaptor.adapt(Security.class, element);
            return s != null && !s.isExchangeRate() && s.getOnlineId() == null;
        }

        @Override
        public Object getValue(Object element)
        {
            Security s = Adaptor.adapt(Security.class, element);
            return s.getIsin() != null ? s.getIsin() : ""; //$NON-NLS-1$
        }

        @Override
        public void setValue(Object element, Object value)
        {
            String newValue = ((String) value).trim();

            if (newValue.length() > 0 && !Isin.isValid(newValue))
                throw new IllegalArgumentException(MessageFormat.format(Messages.MsgDialogNotAValidISIN, newValue));

            Security security = Adaptor.adapt(Security.class, element);
            String oldValue = security.getIsin();

            if (!newValue.equals(oldValue))
            {
                security.setIsin(newValue);
                notify(element, newValue, oldValue);
            }
        }
    }

    public IsinColumn()
    {
        this("isin"); //$NON-NLS-1$
    }

    public IsinColumn(String id)
    {
        super(id, Messages.ColumnISIN, SWT.LEFT, 100);

        setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                Security s = Adaptor.adapt(Security.class, e);
                return s != null ? s.getIsin() : null;
            }
        });
        setSorter(ColumnViewerSorter.create(Security.class, "isin")); //$NON-NLS-1$
        new IsinEditingSupport().attachTo(this);
    }

}
