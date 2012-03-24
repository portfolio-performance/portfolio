package name.abuchen.portfolio.ui.dialogs;

import java.util.Date;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.util.Dates;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

public class DividendsDialog extends AbstractDialog
{
    static class Model extends AbstractDialog.Model
    {
        private Security security;
        private Account account;
        private int amount;
        private Date date = Dates.today();

        public Model(Client client, Security security)
        {
            super(client);

            this.security = security;

            if (security == null && !client.getSecurities().isEmpty())
                setSecurity(client.getSecurities().get(0));

            if (!client.getPortfolios().isEmpty())
                setAccount(client.getPortfolios().get(0).getReferenceAccount());
        }

        public Security getSecurity()
        {
            return security;
        }

        public void setSecurity(Security security)
        {
            firePropertyChange("security", this.security, this.security = security); //$NON-NLS-1$
        }

        public Account getAccount()
        {
            return account;
        }

        public void setAccount(Account account)
        {
            firePropertyChange("account", this.account, this.account = account); //$NON-NLS-1$
        }

        public int getAmount()
        {
            return amount;
        }

        public void setAmount(int amount)
        {
            firePropertyChange("amount", this.amount, this.amount = amount); //$NON-NLS-1$
        }

        public Date getDate()
        {
            return date;
        }

        public void setDate(Date date)
        {
            firePropertyChange("date", this.date, this.date = date); //$NON-NLS-1$
        }

        public void createChanges()
        {
            if (security == null)
                throw new UnsupportedOperationException(Messages.MsgMissingSecurity);

            AccountTransaction ta = new AccountTransaction();
            ta.setAmount(amount);
            ta.setDate(date);
            ta.setSecurity(security);
            ta.setType(AccountTransaction.Type.DIVIDENDS);
            account.addTransaction(ta);
        }
    }

    private boolean allowSelectionOfSecurity = false;

    public DividendsDialog(Shell parentShell, Client client, Security security)
    {
        super(parentShell, Messages.SecurityMenuDividends, client, new Model(client, security));
        this.allowSelectionOfSecurity = security == null;
    }

    @Override
    protected void createFormElements(Composite editArea)
    {
        // security selection
        if (!allowSelectionOfSecurity)
        {
            createLabel(editArea, ((Model) getModel()).getSecurity().getName());
        }
        else
        {
            bindComboViewer(editArea, Messages.ColumnSecurity, "security", new LabelProvider() //$NON-NLS-1$
                            {
                                @Override
                                public String getText(Object element)
                                {
                                    return ((Security) element).getName();
                                }
                            }, getModel().getClient().getSecurities().toArray());
        }

        // account
        bindComboViewer(editArea, Messages.ColumnAccount, "account", new LabelProvider() //$NON-NLS-1$
                        {
                            @Override
                            public String getText(Object element)
                            {
                                return ((Account) element).getName();
                            }
                        }, getModel().getClient().getAccounts().toArray());

        // amount
        bindMandatoryPriceInput(editArea, Messages.ColumnAmount, "amount"); //$NON-NLS-1$

        // date
        bindDatePicker(editArea, Messages.ColumnDate, "date"); //$NON-NLS-1$
    }
}
