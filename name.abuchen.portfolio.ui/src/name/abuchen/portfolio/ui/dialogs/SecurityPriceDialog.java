package name.abuchen.portfolio.ui.dialogs;

import java.time.LocalDate;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.BindingHelper;

public class SecurityPriceDialog extends AbstractDialog
{
    static class SecurityPriceModel extends BindingHelper.Model
    {
        private final Security security;
        private LocalDate date = LocalDate.now();
        private long price;

        public SecurityPriceModel(Client client, Security security)
        {
            super(client);

            this.security = security;
        }

        public Security getSecurity()
        {
            return security;
        }

        public long getPrice()
        {
            return price;
        }

        public void setPrice(long price)
        {
            firePropertyChange("price", this.price, this.price = price); // NOSONAR //$NON-NLS-1$
        }

        public LocalDate getDate()
        {
            return date;
        }

        public void setDate(LocalDate date)
        {
            firePropertyChange("date", this.date, this.date = date); // NOSONAR //$NON-NLS-1$
        }

        @Override
        public void applyChanges()
        {
            SecurityPrice p = new SecurityPrice(date, price);
            security.addPrice(p);
        }
    }

    public SecurityPriceDialog(Shell parentShell, Client client, Security security)
    {
        super(parentShell, Messages.LabelQuote, new SecurityPriceModel(client, security));
    }

    @Override
    protected void createFormElements(Composite editArea)
    {
        bindings().bindDatePicker(editArea, Messages.ColumnDate, "date"); //$NON-NLS-1$
        bindings().bindMandatoryQuoteInput(editArea, Messages.ColumnQuote, "price"); //$NON-NLS-1$
    }

    public void setDate(LocalDate date)
    {
        ((SecurityPriceModel) getModel()).setDate(date);
    }
}
