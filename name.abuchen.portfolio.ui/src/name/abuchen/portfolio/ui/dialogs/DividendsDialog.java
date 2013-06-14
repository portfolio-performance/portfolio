package name.abuchen.portfolio.ui.dialogs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.BindingHelper;
import name.abuchen.portfolio.util.Dates;

import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

public class DividendsDialog extends AbstractDialog
{
    static class Model extends BindingHelper.Model
    {
        private Security security;
        private Account account;
        private long amount;
        private Date date = Dates.today();

        public Model(Client client, Security security)
        {
            super(client);

            this.security = security;

            if (security == null && !client.getSecurities().isEmpty())
                setSecurity(client.getSecurities().get(0));

            // set account if only one exists
            // (otherwise force user to choose)
            if (client.getAccounts().size() == 1)
                setAccount(client.getAccounts().get(0));
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

        public long getAmount()
        {
            return amount;
        }

        public void setAmount(long amount)
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

        public void applyChanges()
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
        super(parentShell, Messages.SecurityMenuDividends, new Model(client, security));
        this.allowSelectionOfSecurity = security == null;
    }

    @Override
    protected void createFormElements(Composite editArea)
    {
        // security selection
        if (!allowSelectionOfSecurity)
        {
            bindings().createLabel(editArea, ((Model) getModel()).getSecurity().getName());
        }
        else
        {
            List<Security> securities = new ArrayList<Security>();
            for (Security s : getModel().getClient().getSecurities())
                if (!s.isRetired())
                    securities.add(s);
            Collections.sort(securities, new Security.ByName());

            bindings().bindComboViewer(editArea, Messages.ColumnSecurity, "security", new LabelProvider() //$NON-NLS-1$
                            {
                                @Override
                                public String getText(Object element)
                                {
                                    return ((Security) element).getName();
                                }
                            }, securities.toArray());
        }

        // account
        bindings().bindComboViewer(editArea, Messages.ColumnAccount, "account", new LabelProvider() //$NON-NLS-1$
                        {
                            @Override
                            public String getText(Object element)
                            {
                                return ((Account) element).getName();
                            }
                        }, new IValidator()
                        {
                            @Override
                            public IStatus validate(Object value)
                            {
                                return value != null ? ValidationStatus.ok() : ValidationStatus
                                                .error(Messages.MsgMissingAccount);
                            }
                        }, getModel().getClient().getAccounts().toArray());

        // amount
        bindings().bindMandatoryAmountInput(editArea, Messages.ColumnAmount, "amount"); //$NON-NLS-1$

        // date
        bindings().bindDatePicker(editArea, Messages.ColumnDate, "date"); //$NON-NLS-1$
    }
}
