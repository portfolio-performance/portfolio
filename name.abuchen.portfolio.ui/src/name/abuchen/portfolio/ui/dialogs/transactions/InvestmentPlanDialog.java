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
import org.eclipse.core.databinding.observable.value.IObservableValue;
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
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.dialogs.transactions.InvestmentPlanModel.Properties;
import name.abuchen.portfolio.ui.util.DatePicker;
import name.abuchen.portfolio.ui.util.FormDataFactory;
import name.abuchen.portfolio.ui.util.SimpleDateTimeDateSelectionProperty;

public class InvestmentPlanDialog extends AbstractTransactionDialog
{
    private Client client;

    private final Class<? extends Transaction> planType;

    @Inject
    public InvestmentPlanDialog(@Named(IServiceConstants.ACTIVE_SHELL) Shell parentShell, Client client,
                    Class<? extends Transaction> planType)
    {
        super(parentShell);
        this.client = client;
        this.planType = planType;

        setModel(new InvestmentPlanModel(client, planType));
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
        @SuppressWarnings("unchecked")
        IObservableValue<?> nameObservable = BeanProperties.value(Properties.name.name()).observe(model);
        context.bindValue(WidgetProperties.text(SWT.Modify).observe(valueName), nameObservable,
                        new UpdateValueStrategy().setAfterConvertValidator(validator), null);

        // security + portfolio

        ComboInput securities = null;
        ComboInput portfolio = null;
        if (planType == PortfolioTransaction.class)
        {
            portfolio = new ComboInput(editArea, Messages.ColumnPortfolio);
            List<Portfolio> portfolios = including(client.getActivePortfolios(), model().getPortfolio());
            portfolio.value.setInput(portfolios);
            portfolio.bindValue(Properties.portfolio.name(), Messages.MsgMissingPortfolio);

            securities = new ComboInput(editArea, Messages.ColumnSecurity);
            securities.value.setInput(including(client.getActiveSecurities(), model().getSecurity()));
            securities.bindValue(Properties.security.name(), Messages.MsgMissingSecurity);
            securities.bindCurrency(Properties.securityCurrencyCode.name());
        }

        // account

        ComboInput account = new ComboInput(editArea, Messages.ColumnAccount);
        List<Account> accounts = including(client.getActiveAccounts(), model().getAccount());
        if (planType == PortfolioTransaction.class)
            accounts = including(accounts, InvestmentPlanModel.DELIVERY);
        account.value.setInput(accounts);
        account.bindValue(Properties.account.name(), Messages.MsgMissingAccount);
        account.bindCurrency(Properties.accountCurrencyCode.name());

        // auto-generate

        Label labelAutoGenerate = new Label(editArea, SWT.NONE);
        labelAutoGenerate.setText(Messages.MsgCreateTransactionsAutomaticallyUponOpening);

        Button buttonAutoGenerate = new Button(editArea, SWT.CHECK);
        @SuppressWarnings("unchecked")
        IObservableValue<?> generateObservable = BeanProperties.value(Properties.autoGenerate.name()).observe(model);
        context.bindValue(WidgetProperties.selection().observe(buttonAutoGenerate), generateObservable);

        // date

        Label lblDate = new Label(editArea, SWT.RIGHT);
        lblDate.setText(Messages.ColumnDate);
        DatePicker valueDate = new DatePicker(editArea);
        @SuppressWarnings("unchecked")
        IObservableValue<?> dateObservable = BeanProperties.value(Properties.start.name()).observe(model);
        context.bindValue(new SimpleDateTimeDateSelectionProperty().observe(valueDate.getControl()), dateObservable);

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

        Input fees = null;
        if (planType == PortfolioTransaction.class)
        {
            fees = new Input(editArea, Messages.ColumnFees);
            fees.bindValue(Properties.fees.name(), Messages.ColumnAmount, Values.Amount, false);
            fees.bindCurrency(Properties.transactionCurrencyCode.name());
        }

        //
        // form layout
        //

        int amountWidth = amountWidth(amount.value);
        int currencyWidth = currencyWidth(amount.currency);

        FormDataFactory factory = startingWith(valueName, lblName).width(3 * amountWidth);

        if (portfolio != null)
        {
            factory = factory.thenBelow(portfolio.value.getControl()).label(portfolio.label)
                            .thenBelow(securities.value.getControl()).label(securities.label)
                            .suffix(securities.currency, currencyWidth);
        }

        factory = factory.thenBelow(account.value.getControl()).label(account.label)
                        .suffix(account.currency, currencyWidth) //
                        .thenBelow(labelAutoGenerate, 10) //
                        .thenBelow(valueDate.getControl(), 10).label(lblDate) //
                        .thenBelow(amount.value, 10).width(amountWidth).label(amount.label)
                        .suffix(amount.currency, currencyWidth);

        if (fees != null)
        {
            factory.thenBelow(fees.value).width(amountWidth).label(fees.label).suffix(fees.currency, currencyWidth); //
        }

        startingWith(labelAutoGenerate).thenLeft(buttonAutoGenerate);

        startingWith(valueDate.getControl()).thenRight(interval.label).thenRight(interval.value.getControl());

        int widest = widest(lblName, securities != null ? securities.label : null,
                        portfolio != null ? portfolio.label : null, account.label, lblDate, interval.label,
                        amount.label, fees != null ? fees.label : null);
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
