package name.abuchen.portfolio.ui.dialogs;

import java.util.Date;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.util.Dates;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.BeansObservables;
import org.eclipse.core.databinding.validation.MultiValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.databinding.viewers.IViewerObservableValue;
import org.eclipse.jface.databinding.viewers.ViewersObservables;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

public class TransferDialog extends AbstractDialog
{
    static class Model extends AbstractDialog.Model
    {
        private Account accountFrom;
        private Account accountTo;
        private int amount;
        private Date date = Dates.today();

        public Model(Client client, Account accountFrom)
        {
            super(client);
            this.accountFrom = accountFrom;

            for (Account a : client.getAccounts())
            {
                if (a != accountFrom)
                {
                    this.accountTo = a;
                    break;
                }
            }
        }

        public Account getAccountFrom()
        {
            return accountFrom;
        }

        public void setAccountFrom(Account accountFrom)
        {
            firePropertyChange("accountFrom", this.accountFrom, this.accountFrom = accountFrom); //$NON-NLS-1$
        }

        public Account getAccountTo()
        {
            return accountTo;
        }

        public void setAccountTo(Account accountTo)
        {
            firePropertyChange("accountTo", this.accountTo, this.accountTo = accountTo); //$NON-NLS-1$
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
            if (accountFrom == null)
                throw new UnsupportedOperationException(Messages.MsgAccountFromMissing);
            if (accountTo == null)
                throw new UnsupportedOperationException(Messages.MsgAccountToMissing);

            AccountTransaction from = new AccountTransaction();
            from.setAmount(amount);
            from.setDate(date);
            from.setType(AccountTransaction.Type.TRANSFER_OUT);
            accountFrom.addTransaction(from);

            AccountTransaction to = new AccountTransaction();
            to.setAmount(amount);
            to.setDate(date);
            to.setType(AccountTransaction.Type.TRANSFER_IN);
            accountTo.addTransaction(to);
        }
    }

    public TransferDialog(Shell parentShell, Client client, Account from)
    {
        super(parentShell, Messages.AccountMenuTransfer, client, new Model(client, from));
    }

    @Override
    protected void createFormElements(Composite editArea)
    {
        GridDataFactory gdf = GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, false);

        // account from
        Label label = new Label(editArea, SWT.NONE);
        label.setText(Messages.ColumnAccountFrom);
        ComboViewer comboFrom = new ComboViewer(editArea, SWT.READ_ONLY);
        comboFrom.setContentProvider(ArrayContentProvider.getInstance());
        comboFrom.setLabelProvider(new LabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                return ((Account) element).getName();
            }
        });
        comboFrom.setInput(getModel().getClient().getAccounts().toArray());
        gdf.applyTo(comboFrom.getControl());
        final IViewerObservableValue observableFrom = ViewersObservables.observeSingleSelection(comboFrom);

        // account to
        label = new Label(editArea, SWT.NONE);
        label.setText(Messages.ColumnAccountTo);
        ComboViewer comboTo = new ComboViewer(editArea, SWT.READ_ONLY);
        comboTo.setContentProvider(ArrayContentProvider.getInstance());
        comboTo.setLabelProvider(new LabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                return ((Account) element).getName();
            }
        });
        comboTo.setInput(getModel().getClient().getAccounts().toArray());
        gdf.applyTo(comboTo.getControl());
        final IViewerObservableValue observableTo = ViewersObservables.observeSingleSelection(comboTo);

        // amount
        bindMandatoryPriceInput(editArea, Messages.ColumnAmount, "amount"); //$NON-NLS-1$

        // date
        bindDatePicker(editArea, Messages.ColumnDate, "date"); //$NON-NLS-1$

        //
        // Bind UI
        //

        DataBindingContext context = getBindingContext();

        // multi validator (to and from account must not be identical)
        MultiValidator validator = new MultiValidator()
        {

            @Override
            protected IStatus validate()
            {
                Object from = observableFrom.getValue();
                Object to = observableTo.getValue();

                return from != null && to != null && from != to ? ValidationStatus.ok() : ValidationStatus
                                .error(Messages.MsgAccountMustBeDifferent);
            }

        };
        context.addValidationStatusProvider(validator);

        context.bindValue(validator.observeValidatedValue(observableFrom), //
                        BeansObservables.observeValue(getModel(), "accountFrom")); //$NON-NLS-1$

        context.bindValue(validator.observeValidatedValue(observableTo), //
                        BeansObservables.observeValue(getModel(), "accountTo")); //$NON-NLS-1$

    }
}
