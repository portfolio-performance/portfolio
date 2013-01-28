package name.abuchen.portfolio.ui.dialogs;

import java.util.Date;
import java.util.EnumSet;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.BindingHelper;
import name.abuchen.portfolio.util.Dates;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

public class OtherAccountTransactionsDialog extends AbstractDialog
{
    static class Model extends BindingHelper.Model
    {
        private Account account;
        private AccountTransaction.Type type;
        private long amount;
        private Date date = Dates.today();

        public Model(Client client, Account account, AccountTransaction.Type defaultSelection)
        {
            super(client);

            this.account = account;
            this.type = defaultSelection;
        }

        public AccountTransaction.Type getType()
        {
            return type;
        }

        public void setType(AccountTransaction.Type type)
        {
            firePropertyChange("type", this.type, this.type = type); //$NON-NLS-1$
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
            if (account == null)
                throw new UnsupportedOperationException(Messages.MsgMissingAccount);

            AccountTransaction t = new AccountTransaction();
            t.setAmount(amount);
            t.setDate(date);
            t.setType(type);
            account.addTransaction(t);
        }
    }

    public OtherAccountTransactionsDialog(Shell parentShell, Client client, Account account,
                    AccountTransaction.Type defaultSelection)
    {
        super(parentShell, Messages.LabelOther, new Model(client, account, defaultSelection));
    }

    @Override
    protected void createFormElements(Composite editArea)
    {
        // type
        bindings().bindComboViewer(editArea, Messages.ColumnTransactionType, "type", new LabelProvider() //$NON-NLS-1$
                        {
                            @Override
                            public String getText(Object element)
                            {
                                return ((AccountTransaction.Type) element).toString();
                            }
                        }, EnumSet.of(AccountTransaction.Type.INTEREST, //
                                        AccountTransaction.Type.DEPOSIT, //
                                        AccountTransaction.Type.REMOVAL, //
                                        AccountTransaction.Type.TAXES, //
                                        AccountTransaction.Type.FEES).toArray());

        bindings().bindMandatoryAmountInput(editArea, Messages.ColumnAmount, "amount"); //$NON-NLS-1$

        bindings().bindDatePicker(editArea, Messages.ColumnDate, "date"); //$NON-NLS-1$

    }
}
