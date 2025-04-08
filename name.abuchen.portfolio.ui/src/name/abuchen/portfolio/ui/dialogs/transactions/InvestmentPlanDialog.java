package name.abuchen.portfolio.ui.dialogs.transactions;

import static name.abuchen.portfolio.ui.util.FormDataFactory.startingWith;
import static name.abuchen.portfolio.ui.util.SWTHelper.amountWidth;
import static name.abuchen.portfolio.ui.util.SWTHelper.currencyWidth;
import static name.abuchen.portfolio.ui.util.SWTHelper.widest;

import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.eclipse.core.databinding.UpdateValueStrategy;
import org.eclipse.core.databinding.beans.typed.BeanProperties;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.e4.ui.services.IStylingEngine;
import org.eclipse.jface.databinding.swt.typed.WidgetProperties;
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
import name.abuchen.portfolio.model.InvestmentPlan.Type;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.dialogs.transactions.InvestmentPlanModel.Properties;
import name.abuchen.portfolio.ui.util.DatePicker;
import name.abuchen.portfolio.ui.util.FormDataFactory;
import name.abuchen.portfolio.ui.util.SecurityNameLabelProvider;
import name.abuchen.portfolio.ui.util.SimpleDateTimeDateSelectionProperty;

public class InvestmentPlanDialog extends AbstractTransactionDialog
{
    @Inject
    private IStylingEngine stylingEngine;

    private Client client;

    private final InvestmentPlan.Type planType;

    @Inject
    public InvestmentPlanDialog(@Named(IServiceConstants.ACTIVE_SHELL) Shell parentShell, Client client,
                    InvestmentPlan.Type planType)
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
        IValidator<String> validator = v -> v != null && v.trim().length() > 0 ? ValidationStatus.ok()
                        : ValidationStatus.error(
                                        MessageFormat.format(Messages.MsgDialogInputRequired, Messages.ColumnName));

        IObservableValue<String> nameTarget = WidgetProperties.text(SWT.Modify).observe(valueName);
        IObservableValue<String> nameModel = BeanProperties.value(Properties.name.name(), String.class).observe(model);
        context.bindValue(nameTarget, nameModel,
                        new UpdateValueStrategy<String, String>().setAfterConvertValidator(validator), null);

        // security + portfolio

        ComboInput securities = null;
        ComboInput portfolio = null;
        if (planType == Type.PURCHASE_OR_DELIVERY)
        {
            portfolio = new ComboInput(editArea, Messages.ColumnPortfolio);
            List<Portfolio> portfolios = including(client.getActivePortfolios(), model().getPortfolio());
            portfolio.value.setInput(portfolios);
            portfolio.bindValue(Properties.portfolio.name(), Messages.MsgMissingPortfolio);

            securities = new ComboInput(editArea, Messages.ColumnSecurity);
            securities.value.setInput(including(client.getActiveSecurities(), model().getSecurity()));
            securities.value.setLabelProvider(new SecurityNameLabelProvider(client));
            securities.bindValue(Properties.security.name(), Messages.MsgMissingSecurity);
            securities.bindCurrency(Properties.securityCurrencyCode.name());
        }

        // account

        ComboInput account = new ComboInput(editArea, Messages.ColumnAccount);
        List<Account> accounts = including(client.getActiveAccounts(), model().getAccount());
        if (planType == Type.PURCHASE_OR_DELIVERY)
            accounts = including(accounts, InvestmentPlanModel.DELIVERY);
        account.value.setInput(accounts);
        account.bindValue(Properties.account.name(), Messages.MsgMissingAccount);
        account.bindCurrency(Properties.accountCurrencyCode.name());

        // auto-generate

        Label labelAutoGenerate = new Label(editArea, SWT.NONE);
        labelAutoGenerate.setText(Messages.MsgCreateTransactionsAutomaticallyUponOpening);

        Button buttonAutoGenerate = new Button(editArea, SWT.CHECK);
        IObservableValue<?> targetAutoGenerate = WidgetProperties.buttonSelection().observe(buttonAutoGenerate);
        IObservableValue<?> modelAutoGenerate = BeanProperties.value(Properties.autoGenerate.name()).observe(model);
        context.bindValue(targetAutoGenerate, modelAutoGenerate);

        // date

        Label lblDate = new Label(editArea, SWT.RIGHT);
        lblDate.setText(Messages.ColumnDate);
        DatePicker valueDate = new DatePicker(editArea);
        IObservableValue<?> targetDate = new SimpleDateTimeDateSelectionProperty().observe(valueDate.getControl());
        IObservableValue<?> modelDate = BeanProperties.value(Properties.start.name()).observe(model);
        context.bindValue(targetDate, modelDate);

        // interval

        List<Integer> available = Arrays.stream(InvestmentPlanModel.Intervals.values())
                        .map(InvestmentPlanModel.Intervals::getInterval).toList();

        ComboInput interval = new ComboInput(editArea, Messages.ColumnInterval);
        interval.value.setInput(available);
        interval.value.setLabelProvider(LabelProvider.createTextProvider(
                        element -> InvestmentPlanModel.Intervals.get((Integer) element).toString()));
        interval.bindValue(Properties.interval.name(),
                        MessageFormat.format(Messages.MsgDialogInputRequired, Messages.ColumnInterval));

        // gross amount

        Input grossAmount = null;
        if (planType == Type.INTEREST || planType == Type.PURCHASE_OR_DELIVERY)
        {
            grossAmount = new Input(editArea, Messages.ColumnGrossValue);
            grossAmount.bindValue(Properties.grossAmount.name(), Messages.ColumnGrossValue, Values.Amount, true);
            grossAmount.bindCurrency(Properties.transactionCurrencyCode.name());
        }

        // taxes

        Input taxes = null;
        if (planType == Type.INTEREST)
        {
            taxes = new Input(editArea, "- " + Messages.ColumnTaxes); //$NON-NLS-1$
            taxes.bindValue(Properties.taxes.name(), Messages.ColumnTaxes, Values.Amount, false);
            taxes.bindCurrency(Properties.transactionCurrencyCode.name());
        }

        // fees

        Input fees = null;
        if (planType == Type.PURCHASE_OR_DELIVERY)
        {
            fees = new Input(editArea, "+ " + Messages.ColumnFees); //$NON-NLS-1$
            fees.bindValue(Properties.fees.name(), Messages.ColumnFees, Values.Amount, false);
            fees.bindCurrency(Properties.transactionCurrencyCode.name());
        }

        // amount
        String label;
        switch (planType)
        {
            case DEPOSIT, INTEREST:
                label = Messages.ColumnCreditNote;
                break;
            case REMOVAL:
                label = Messages.ColumnDebitNote;
                break;
            case PURCHASE_OR_DELIVERY:
                label = Messages.ColumnAmount;
                break;
            default:
                throw new UnsupportedOperationException();
        }

        Input amount = new Input(editArea, label);
        amount.bindValue(Properties.amount.name(), label, Values.Amount, true);
        amount.bindCurrency(Properties.transactionCurrencyCode.name());

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
                        .thenBelow(valueDate.getControl(), 10).label(lblDate);

        if (grossAmount != null)
        {
            factory = factory.thenBelow(grossAmount.value, 10).width(amountWidth).label(grossAmount.label)
                            .suffix(grossAmount.currency, currencyWidth);
        }

        if (fees != null)
        {
            factory = factory.thenBelow(fees.value, 10).width(amountWidth).label(fees.label).suffix(fees.currency,
                            currencyWidth);
        }

        if (taxes != null)
        {
            factory = factory.thenBelow(taxes.value, 10).width(amountWidth).label(taxes.label).suffix(taxes.currency,
                            currencyWidth);
        }

        factory.thenBelow(amount.value, 10).width(amountWidth).label(amount.label).suffix(amount.currency,
                        currencyWidth);

        startingWith(labelAutoGenerate).thenLeft(buttonAutoGenerate);

        startingWith(valueDate.getControl()).thenRight(interval.label).thenRight(interval.value.getControl());

        // measuring the width requires that the font has been applied before
        stylingEngine.style(editArea);

        int widest = widest(lblName, securities != null ? securities.label : null,
                        portfolio != null ? portfolio.label : null, account.label, lblDate, interval.label,
                        amount.label, fees != null ? fees.label : null, taxes != null ? taxes.label : null);
        startingWith(lblName).width(widest);

        WarningMessages warnings = new WarningMessages(this);
        warnings.add(() -> model().getStart().isAfter(LocalDate.now()) ? Messages.MsgDateIsInTheFuture : null);
        warnings.add(() -> model().getSecurity() != null && model().getSecurity().getPrices().isEmpty()
                        ? Messages.MsgSecurityHasNoQuotes
                        : null);
        model.addPropertyChangeListener(Properties.start.name(), e -> warnings.check());
        model.addPropertyChangeListener(Properties.security.name(), e -> warnings.check());
    }

    public void setPlan(InvestmentPlan plan)
    {
        model().setSource(plan);
    }

    @Override
    public void setSecurity(Security security)
    {
        model().setSecurity(security);
    }

}
