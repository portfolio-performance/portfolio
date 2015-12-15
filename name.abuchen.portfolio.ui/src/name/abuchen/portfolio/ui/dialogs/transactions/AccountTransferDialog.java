package name.abuchen.portfolio.ui.dialogs.transactions;

import static name.abuchen.portfolio.ui.util.FormDataFactory.startingWith;
import static name.abuchen.portfolio.ui.util.SWTHelper.amountWidth;
import static name.abuchen.portfolio.ui.util.SWTHelper.currencyWidth;
import static name.abuchen.portfolio.ui.util.SWTHelper.widest;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.core.databinding.beans.BeanProperties;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.validation.MultiValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransferEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.dialogs.transactions.AccountTransferModel.Properties;
import name.abuchen.portfolio.ui.util.DateTimePicker;
import name.abuchen.portfolio.ui.util.SimpleDateTimeSelectionProperty;

public class AccountTransferDialog extends AbstractTransactionDialog
{
    private final class AccountsMustBeDifferentValidator extends MultiValidator
    {
        IObservableValue source;
        IObservableValue target;

        public AccountsMustBeDifferentValidator(IObservableValue source, IObservableValue target)
        {
            this.source = source;
            this.target = target;
        }

        @Override
        protected IStatus validate()
        {
            Object from = source.getValue();
            Object to = target.getValue();

            return from != null && to != null && from != to ? ValidationStatus.ok() : ValidationStatus
                            .error(Messages.MsgAccountMustBeDifferent);
        }
    }

    private Client client;

    @Inject
    public AccountTransferDialog(@Named(IServiceConstants.ACTIVE_SHELL) Shell parentShell, Client client)
    {
        super(parentShell, new AccountTransferModel(client));
        this.client = client;
    }

    private AccountTransferModel model()
    {
        return (AccountTransferModel) this.model;
    }

    @Override
    protected void createFormElements(Composite editArea)
    {
        //
        // input elements
        //

        // source account

        ComboInput source = new ComboInput(editArea, Messages.ColumnAccountFrom);
        source.value.setInput(client.getActiveAccounts());
        IObservableValue sourceObservable = source.bindValue(Properties.sourceAccount.name(),
                        Messages.MsgAccountFromMissing);
        source.bindCurrency(Properties.sourceAccountCurrency.name());

        // target account

        ComboInput target = new ComboInput(editArea, Messages.ColumnAccountTo);
        target.value.setInput(client.getActiveAccounts());
        IObservableValue targetObservable = target.bindValue(Properties.targetAccount.name(),
                        Messages.MsgAccountToMissing);
        target.bindCurrency(Properties.targetAccountCurrency.name());

        MultiValidator validator = new AccountsMustBeDifferentValidator(sourceObservable, targetObservable);
        context.addValidationStatusProvider(validator);

        // date

        Label lblDate = new Label(editArea, SWT.RIGHT);
        lblDate.setText(Messages.ColumnDate);
        DateTimePicker valueDate = new DateTimePicker(editArea);
        context.bindValue(new SimpleDateTimeSelectionProperty().observe(valueDate.getControl()),
                        BeanProperties.value(Properties.date.name()).observe(model));

        // other input fields

        Input fxAmount = new Input(editArea, Messages.ColumnAmount);
        fxAmount.bindValue(Properties.fxAmount.name(), Messages.ColumnAmount, Values.Amount, true);
        fxAmount.bindCurrency(Properties.sourceAccountCurrency.name());

        Input exchangeRate = new Input(editArea, "x "); //$NON-NLS-1$
        exchangeRate.bindExchangeRate(Properties.exchangeRate.name(), Messages.ColumnExchangeRate);
        exchangeRate.bindCurrency(Properties.exchangeRateCurrencies.name());

        Input amount = new Input(editArea, "="); //$NON-NLS-1$
        amount.bindValue(Properties.amount.name(), Messages.ColumnAmount, Values.Amount, true);
        amount.bindCurrency(Properties.targetAccountCurrency.name());

        // note
        Label lblNote = new Label(editArea, SWT.LEFT);
        lblNote.setText(Messages.ColumnNote);
        Text valueNote = new Text(editArea, SWT.BORDER);
        context.bindValue(WidgetProperties.text(SWT.Modify).observe(valueNote),
                        BeanProperties.value(Properties.note.name()).observe(model));

        //
        // form layout
        //

        int amountWidth = amountWidth(amount.value);
        int currencyWidth = currencyWidth(fxAmount.currency);

        startingWith(source.value.getControl(), source.label).suffix(source.currency)
                        .thenBelow(target.value.getControl()).label(target.label).suffix(target.currency)
                        .thenBelow(valueDate.getControl()).label(lblDate) //
                        // fxAmount - exchange rate - amount
                        .thenBelow(fxAmount.value).width(amountWidth).label(fxAmount.label) //
                        .thenRight(fxAmount.currency).width(currencyWidth) //
                        .thenRight(exchangeRate.label) //
                        .thenRight(exchangeRate.value).width(amountWidth) //
                        .thenRight(exchangeRate.currency).width(amountWidth) //
                        .thenRight(amount.label) //
                        .thenRight(amount.value).width(amountWidth) //
                        // note
                        .suffix(amount.currency, currencyWidth) //
                        .thenBelow(valueNote).left(target.value.getControl()).right(amount.value).label(lblNote);

        int widest = widest(source.label, target.label, lblDate, fxAmount.label, lblNote);
        startingWith(source.label).width(widest);

        //
        // hide / show exchange rate if necessary
        //

        model.addPropertyChangeListener(Properties.exchangeRateCurrencies.name(), new PropertyChangeListener()
        {
            @Override
            public void propertyChange(PropertyChangeEvent event)
            {
                String sourceCurrency = model().getSourceAccountCurrency();
                String targetCurrency = model().getTargetAccountCurrency();

                // make exchange rate visible if both are set but different

                boolean visible = sourceCurrency.length() > 0 && targetCurrency.length() > 0
                                && !sourceCurrency.equals(targetCurrency);

                exchangeRate.setVisible(visible);
                amount.setVisible(visible);
            }
        });

        model.firePropertyChange(Properties.exchangeRateCurrencies.name(), "", model().getExchangeRateCurrencies()); //$NON-NLS-1$
    }

    public void setAccount(Account account)
    {
        model().setSourceAccount(account);
    }

    public void setEntry(AccountTransferEntry entry)
    {
        model().setSource(entry);
    }

}
