package name.abuchen.portfolio.ui.views.columns;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Adaptor;
import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.model.InvestmentPlan;
import name.abuchen.portfolio.model.Named;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.viewers.Column;
import name.abuchen.portfolio.ui.util.viewers.ColumnViewerSorter;
import name.abuchen.portfolio.ui.util.viewers.StringEditingSupport;
import name.abuchen.portfolio.util.TextUtil;

public class NameColumn extends Column
{
    public static class NameColumnLabelProvider extends ColumnLabelProvider
    {
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

            if (n instanceof Security)
                return Images.SECURITY.image();
            else if (n instanceof Account)
                return Images.ACCOUNT.image();
            else if (n instanceof Portfolio)
                return Images.PORTFOLIO.image();
            else if (n instanceof InvestmentPlan)
                return Images.INVESTMENTPLAN.image();
            else if (n instanceof Classification)
                return Images.CATEGORY.image();
            else
                return null;
        }

        @Override
        public String getToolTipText(Object e)
        {
            Named element = Adaptor.adapt(Named.class, e);
            if (element == null)
                return null;
            else if (element instanceof Security)
                return TextUtil.tooltip(((Security) element).toInfoString());
            else
                return TextUtil.tooltip(element.getName());
        }
    }

    public NameColumn()
    {
        this("name"); //$NON-NLS-1$
    }

    public NameColumn(String id)
    {
        this(id, Messages.ColumnName, SWT.LEFT, 300);
    }

    public NameColumn(String id, String label, int style, int defaultWidth)
    {
        super(id, label, style, defaultWidth);

        setLabelProvider(new NameColumnLabelProvider());
        setSorter(ColumnViewerSorter.create(Named.class, "name")); //$NON-NLS-1$
        new StringEditingSupport(Named.class, "name").setMandatory(true).attachTo(this); //$NON-NLS-1$
    }
}
