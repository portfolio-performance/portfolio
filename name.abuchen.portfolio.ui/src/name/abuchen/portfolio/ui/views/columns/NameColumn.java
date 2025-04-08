package name.abuchen.portfolio.ui.views.columns;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;

import name.abuchen.portfolio.model.Adaptor;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Named;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.LogoManager;
import name.abuchen.portfolio.ui.util.viewers.Column;
import name.abuchen.portfolio.ui.util.viewers.ColumnViewerSorter;
import name.abuchen.portfolio.ui.util.viewers.StringEditingSupport;
import name.abuchen.portfolio.util.TextUtil;

public class NameColumn extends Column
{
    public static class NameColumnLabelProvider extends ColumnLabelProvider
    {
        private Client client;
        public NameColumnLabelProvider(Client client)
        {
            this.client = client;
        }
        
        @Override
        public String getText(Object e)
        {
            Named n = Adaptor.adapt(Named.class, e);
            return n != null ? n.getName() : null;
        }

        @Override
        public Image getImage(Object e)
        {
            Named n = Adaptor.adapt(Named.class, e);
            return LogoManager.instance().getDefaultColumnImage(n, client.getSettings());
        }

        @Override
        public String getToolTipText(Object e)
        {
            Named element = Adaptor.adapt(Named.class, e);
            if (element == null)
                return null;
            else if (element instanceof Security security)
                return TextUtil.wordwrap(security.toInfoString());
            else
                return TextUtil.tooltip(element.getName());
        }
    }
    
    public NameColumn(Client client)
    {
        this(client, "name"); //$NON-NLS-1$
    }

    public NameColumn(Client client, String id)
    {
        this(id, Messages.ColumnName, SWT.LEFT, 300, client);
    }

    public NameColumn(String id, String label, int style, int defaultWidth, Client client)
    {
        super(id, label, style, defaultWidth);

        setLabelProvider(new NameColumnLabelProvider(client));
        setSorter(ColumnViewerSorter.create(Named.class, "name")); //$NON-NLS-1$
        new StringEditingSupport(Named.class, "name").setMandatory(true).attachTo(this); //$NON-NLS-1$
    }
}
