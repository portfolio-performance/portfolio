package name.abuchen.portfolio.ui.dialogs;

import java.util.Date;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.BindingHelper;
import name.abuchen.portfolio.util.Dates;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

public class SecurityPriceDialog extends AbstractDialog
{
    static class Model extends BindingHelper.Model
    {
        private Security security;
        private Date date = Dates.today();
        private long price;

        public Model(Client client, Security security)
        {
            super(client);

            this.security = security;
        }

        public long getPrice()
        {
            return price;
        }

        public void setPrice(long price)
        {
            firePropertyChange("price", this.price, this.price = price); //$NON-NLS-1$
        }

        public Date getDate()
        {
            return date;
        }

        public void setDate(Date date)
        {
            firePropertyChange("date", this.date, this.date = date); //$NON-NLS-1$
        }

        public void applyChanges()
        {
            SecurityPrice p = new SecurityPrice(date, price);
            security.addPrice(p);
        }
    }

    public SecurityPriceDialog(Shell parentShell, Client client, Security security)
    {
        super(parentShell, Messages.LabelQuote, new Model(client, security));
    }

    @Override
    protected void createFormElements(Composite editArea)
    {
        bindings().bindDatePicker(editArea, Messages.ColumnDate, "date"); //$NON-NLS-1$
        bindings().bindMandatoryQuoteInput(editArea, Messages.ColumnQuote, "price"); //$NON-NLS-1$
    }
}
