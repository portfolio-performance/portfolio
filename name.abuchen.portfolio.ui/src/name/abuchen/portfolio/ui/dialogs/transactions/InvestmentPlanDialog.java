package name.abuchen.portfolio.ui.dialogs.transactions;

import static name.abuchen.portfolio.ui.util.FormDataFactory.startingWith;
import static name.abuchen.portfolio.ui.util.SWTHelper.amountWidth;
import static name.abuchen.portfolio.ui.util.SWTHelper.currencyWidth;
import static name.abuchen.portfolio.ui.util.SWTHelper.widest;

import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.core.databinding.UpdateValueStrategy;
import org.eclipse.core.databinding.beans.BeanProperties;
import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.InvestmentPlan;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.dialogs.transactions.InvestmentPlanModel.Properties;
import name.abuchen.portfolio.ui.util.DatePicker;
import name.abuchen.portfolio.ui.util.SimpleDateTimeDateSelectionProperty;

public class InvestmentPlanDialog extends AbstractTransactionDialog
{
    private Client client;

    @Inject
    public InvestmentPlanDialog(@Named(IServiceConstants.ACTIVE_SHELL) Shell parentShell, Client client)
    {
        super(parentShell);
        this.client = client;
        setModel(new InvestmentPlanModel(client));
    }

    private InvestmentPlanModel model()
    {
        return (InvestmentPlanModel) this.model;
    }

    @Override
    protected void createFormElements(Composite editArea)
    {
        //
        // input elements
        //

        // name

        Label lblName = new Label(editArea, SWT.RIGHT);
        lblName.setText(Messages.ColumnName);
        Text valueName = new Text(editArea, SWT.BORDER);
        IValidator validator = value -> {
            String v = (String) value;
            return v != null && v.trim().length() > 0 ? ValidationStatus.ok()
                            : ValidationStatus.error(
                                            MessageFormat.format(Messages.MsgDialogInputRequired, Messages.ColumnName));
        };
        context.bindValue(WidgetProperties.text(SWT.Modify).observe(valueName),
                        BeanProperties.value(Properties.name.name()).observe(model),
                        new UpdateValueStrategy().setAfterConvertValidator(validator), null);

        // security

        ComboInput securities = new ComboInput(editArea, Messages.ColumnSecurity);
        securities.value.setInput(including(client.getActiveSecurities(), model().getSecurity()));
        securities.bindValue(Properties.security.name(), Messages.MsgMissingSecurity);
        securities.bindCurrency(Properties.securityCurrencyCode.name());

        // portfolio

        ComboInput portfolio = new ComboInput(editArea, Messages.ColumnPortfolio);
        portfolio.value.setInput(including(client.getActivePortfolios(), model().getPortfolio()));
        portfolio.bindValue(Properties.portfolio.name(), Messages.MsgMissingPortfolio);

        // account

        ComboInput account = new ComboInput(editArea, Messages.ColumnAccount);
        List<Account> accounts = including(client.getActiveAccounts(), model().getAccount());
        accounts.add(0, InvestmentPlanModel.DELIVERY);
        account.value.setInput(accounts);
        account.bindValue(Properties.account.name(), Messages.MsgMissingAccount);
        account.bindCurrency(Properties.accountCurrencyCode.name());

        // auto-generate

        Label labelAutoGenerate = new Label(editArea, SWT.NONE);
        labelAutoGenerate.setText(Messages.MsgCreateTransactionsAutomaticallyUponOpening);

        Button buttonAutoGenerate = new Button(editArea, SWT.CHECK);
        context.bindValue(WidgetProperties.selection().observe(buttonAutoGenerate), //
                        BeanProperties.value(Properties.autoGenerate.name()).observe(model));

        // date

        Label lblDate = new Label(editArea, SWT.RIGHT);
        lblDate.setText(Messages.ColumnDate);
        DatePicker valueDate = new DatePicker(editArea);
        context.bindValue(new SimpleDateTimeDateSelectionProperty().observe(valueDate.getControl()),
                        BeanProperties.value(Properties.start.name()).observe(model));

        // interval

        List<Integer> available = new ArrayList<>();
        for (int ii = 1; ii <= 12; ii++)
            available.add(ii);

        ComboInput interval = new ComboInput(editArea, Messages.ColumnInterval);
        interval.value.setInput(available);
        interval.value.setLabelProvider(new LabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                int interval = (Integer) element;
                return MessageFormat.format(Messages.InvestmentPlanIntervalLabel, interval);
            }
        });
        interval.bindValue(Properties.interval.name(),
                        MessageFormat.format(Messages.MsgDialogInputRequired, Messages.ColumnInterval));

        // amount

        Input amount = new Input(editArea, Messages.ColumnAmount);
        amount.bindValue(Properties.amount.name(), Messages.ColumnAmount, Values.Amount, true);
        amount.bindCurrency(Properties.transactionCurrencyCode.name());

        // fees

        Input fees = new Input(editArea, Messages.ColumnFees);
        fees.bindValue(Properties.fees.name(), Messages.ColumnAmount, Values.Amount, false);
        fees.bindCurrency(Properties.transactionCurrencyCode.name());

        //
        // form layout
        //

        int amountWidth = amountWidth(amount.value);
        int currencyWidth = currencyWidth(amount.currency);

        startingWith(valueName, lblName).width(3 * amountWidth)
                        //
                        .thenBelow(securities.value.getControl()).label(securities.label)
                        .suffix(securities.currency, currencyWidth) //
                        .thenBelow(portfolio.value.getControl()).label(portfolio.label) //
                        .thenBelow(account.value.getControl()).label(account.label)
                        .suffix(account.currency, currencyWidth) //
                        .thenBelow(labelAutoGenerate, 10) //
                        .thenBelow(valueDate.getControl(), 10).label(lblDate) //
                        .thenBelow(amount.value, 10).width(amountWidth).label(amount.label)
                        .suffix(amount.currency, currencyWidth) //
                        .thenBelow(fees.value).width(amountWidth).label(fees.label)
                        .suffix(fees.currency, currencyWidth); //

        startingWith(labelAutoGenerate).thenLeft(buttonAutoGenerate);
        
        startingWith(valueDate.getControl()).thenRight(interval.label).thenRight(interval.value.getControl());

        int widest = widest(lblName, securities.label, portfolio.label, account.label, lblDate, interval.label,
                        amount.label, fees.label);
        startingWith(lblName).width(widest);

        WarningMessages warnings = new WarningMessages(this);
        warnings.add(() -> model().getStart().isAfter(LocalDate.now()) ? Messages.MsgDateIsInTheFuture : null);
        model.addPropertyChangeListener(Properties.start.name(), e -> warnings.check());
    }

    public void setPlan(InvestmentPlan plan)
    {
        model().setSource(plan);
    }
}
