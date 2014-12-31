package name.abuchen.portfolio.ui.dialogs.transactions;

import static name.abuchen.portfolio.ui.util.FormDataFactory.startingWith;
import static name.abuchen.portfolio.ui.util.SWTHelper.stringWidth;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.MessageFormat;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;

import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.ExchangeRateProviderFactory;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.PortfolioTransaction.Type;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Values;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.dialogs.transactions.BuySellModel.Properties;
import name.abuchen.portfolio.ui.util.CurrencyToStringConverter;
import name.abuchen.portfolio.ui.util.SimpleDateTimeSelectionProperty;
import name.abuchen.portfolio.ui.util.StringToCurrencyConverter;

import org.eclipse.core.databinding.AggregateValidationStatus;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.UpdateValueStrategy;
import org.eclipse.core.databinding.beans.BeansObservables;
import org.eclipse.core.databinding.beans.PojoObservables;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.core.databinding.validation.MultiValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.jface.databinding.swt.SWTObservables;
import org.eclipse.jface.databinding.viewers.ViewersObservables;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class BuySellSecurityDialog extends TitleAreaDialog
{
    public class Input
    {
        public Label label;
        public Text value;
        public Label currency;

        public Input(Composite editArea, String text)
        {
            label = new Label(editArea, SWT.LEFT);
            label.setText(text);
            value = new Text(editArea, SWT.BORDER | SWT.RIGHT);
            currency = new Label(editArea, SWT.NONE);
        }

        public void bindValue(String property, String description, Values<?> values, boolean isMandatory)
        {
            UpdateValueStrategy strategy = new UpdateValueStrategy();
            strategy.setConverter(new StringToCurrencyConverter(values));
            if (isMandatory)
            {
                strategy.setAfterConvertValidator(new IValidator()
                {
                    @Override
                    public IStatus validate(Object value)
                    {
                        Long v = (Long) value;
                        return v != null && v.longValue() > 0 ? ValidationStatus.ok() : ValidationStatus
                                        .error(MessageFormat.format(Messages.MsgDialogInputRequired, description));
                    }
                });
            }

            context.bindValue(SWTObservables.observeText(value, SWT.Modify), //
                            BeansObservables.observeValue(model, property), //
                            strategy, new UpdateValueStrategy().setConverter(new CurrencyToStringConverter(values)));
        }

        public void bindCurrency(String property)
        {
            context.bindValue(SWTObservables.observeText(currency), BeansObservables.observeValue(model, property));
        }

        public void setVisible(boolean visible)
        {
            label.setVisible(visible);
            value.setVisible(visible);
            currency.setVisible(visible);
        }
    }

    class ModelStatusListener
    {
        public void setStatus(IStatus status)
        {
            setMessage(status.getSeverity() == IStatus.OK ? "" : status.getMessage()); //$NON-NLS-1$

            Control button = getButton(IDialogConstants.OK_ID);
            if (button != null)
                button.setEnabled(status.getSeverity() == IStatus.OK);
        }

        public IStatus getStatus()
        {
            // irrelevant
            return ValidationStatus.ok();
        }
    }

    private Client client;
    private BuySellModel model;
    private DataBindingContext context = new DataBindingContext();
    private ModelStatusListener status = new ModelStatusListener();

    @Inject
    public BuySellSecurityDialog(@Named(IServiceConstants.ACTIVE_SHELL) Shell parentShell, Client client,
                    PortfolioTransaction.Type type)
    {
        super(parentShell);

        this.client = client;
        this.model = new BuySellModel(client, type);
    }

    @Inject
    public void setExchangeRateProviderFactory(ExchangeRateProviderFactory factory)
    {
        this.model.setExchangeRateProviderFactory(factory);
    }

    @PostConstruct
    public void registerListeners()
    {
        // set portfolio only if exactly one exists
        // (otherwise force user to choose)
        List<Portfolio> activePortfolios = client.getActivePortfolios();
        if (activePortfolios.size() == 1)
            model.setPortfolio(activePortfolios.get(0));

        List<Security> activeSecurities = client.getActiveSecurities();
        if (!activeSecurities.isEmpty())
            model.setSecurity(activeSecurities.get(0));

        this.context.addValidationStatusProvider(new MultiValidator()
        {
            IObservableValue observable = BeansObservables.observeValue(model, Properties.calculationStatus.name());

            @Override
            protected IStatus validate()
            {
                return (IStatus) observable.getValue();
            }
        });
    }

    @Override
    public void create()
    {
        super.create();

        setTitle(model.getType().toString());
        setMessage(""); //$NON-NLS-1$

        setShellStyle(getShellStyle() | SWT.RESIZE);

        status.setStatus(AggregateValidationStatus.getStatusMaxSeverity(context.getValidationStatusProviders()));
    }

    @Override
    protected int getShellStyle()
    {
        return super.getShellStyle() | SWT.RESIZE;
    }

    @Override
    protected Control createDialogArea(Composite parent)
    {
        Composite area = (Composite) super.createDialogArea(parent);

        Composite editArea = new Composite(area, SWT.NONE);
        FormLayout layout = new FormLayout();
        layout.marginWidth = 5;
        layout.marginHeight = 5;
        editArea.setLayout(layout);

        createFormElements(editArea);

        context.bindValue(PojoObservables.observeValue(status, "status"), //$NON-NLS-1$
                        new AggregateValidationStatus(context, AggregateValidationStatus.MAX_SEVERITY));

        return editArea;
    }

    private void createFormElements(Composite editArea)
    {
        //
        // input elements
        //

        // security

        Label lblSecurity = new Label(editArea, SWT.RIGHT);
        lblSecurity.setText(Messages.ColumnSecurity);
        ComboViewer valueSecurity = new ComboViewer(editArea);
        valueSecurity.setContentProvider(new ArrayContentProvider());
        valueSecurity.setInput(client.getActiveSecurities());
        Label curSecurity = new Label(editArea, SWT.NONE);

        context.bindValue(SWTObservables.observeText(curSecurity),
                        BeansObservables.observeValue(model, Properties.securityCurrencyCode.name()));

        context.bindValue(
                        ViewersObservables.observeSingleSelection(valueSecurity), //
                        BeansObservables.observeValue(model, Properties.security.name()), new UpdateValueStrategy(),
                        null);

        // portfolio

        Label lblPortfolio = new Label(editArea, SWT.RIGHT);
        lblPortfolio.setText(Messages.ColumnPortfolio);
        ComboViewer valuePortfolio = new ComboViewer(editArea);
        valuePortfolio.setContentProvider(new ArrayContentProvider());
        valuePortfolio.setInput(client.getActivePortfolios());
        Label referencePortfolio = new Label(editArea, SWT.NONE);

        UpdateValueStrategy strategy = new UpdateValueStrategy();
        strategy.setAfterConvertValidator(value -> value != null ? ValidationStatus.ok() : ValidationStatus
                        .error(Messages.MsgMissingPortfolio));
        context.bindValue(ViewersObservables.observeSingleSelection(valuePortfolio), //
                        BeansObservables.observeValue(model, Properties.portfolio.name()), strategy, null);

        context.bindValue(SWTObservables.observeText(referencePortfolio),
                        BeansObservables.observeValue(model, Properties.accountName.name()));

        // date

        Label lblDate = new Label(editArea, SWT.RIGHT);
        lblDate.setText(Messages.ColumnDate);
        DateTime valueDate = new DateTime(editArea, SWT.DATE | SWT.DROP_DOWN | SWT.BORDER);

        context.bindValue(new SimpleDateTimeSelectionProperty().observe(valueDate),
                        BeansObservables.observeValue(model, Properties.date.name()));

        // other input fields

        Input shares = new Input(editArea, Messages.ColumnShares);
        shares.bindValue(Properties.shares.name(), Messages.ColumnShares, Values.Share, true);

        Input quote = new Input(editArea, "x " + Messages.ColumnQuote); //$NON-NLS-1$
        quote.bindValue(Properties.quote.name(), Messages.ColumnQuote, Values.Quote, true);
        quote.bindCurrency(Properties.securityCurrencyCode.name());

        Input lumpSum = new Input(editArea, "="); //$NON-NLS-1$
        lumpSum.bindValue(Properties.lumpSum.name(), Messages.ColumnSubTotal, Values.Amount, true);
        lumpSum.bindCurrency(Properties.securityCurrencyCode.name());

        final Input exchangeRate = new Input(editArea, "x " + Messages.ColumnExchangeRate); //$NON-NLS-1$
        exchangeRate.bindValue(Properties.exchangeRate.name(), Messages.ColumnExchangeRate, Values.ExchangeRate, true);
        exchangeRate.bindCurrency(Properties.exchangeRateCurrencies.name());

        final Input convertedLumpSum = new Input(editArea, "="); //$NON-NLS-1$
        convertedLumpSum.bindValue(Properties.convertedLumpSum.name(), Messages.ColumnSubTotal, Values.Amount, true);
        convertedLumpSum.bindCurrency(Properties.accountCurrencyCode.name());

        Input fees = new Input(editArea, sign() + Messages.ColumnFees);
        fees.bindValue(Properties.fees.name(), Messages.ColumnFees, Values.Amount, false);
        fees.bindCurrency(Properties.accountCurrencyCode.name());

        Input taxes = new Input(editArea, sign() + Messages.ColumnTaxes);
        taxes.bindValue(Properties.taxes.name(), Messages.ColumnTaxes, Values.Amount, false);
        taxes.bindCurrency(Properties.accountCurrencyCode.name());

        String label = model.getType() == Type.BUY ? Messages.ColumnDebitNote : Messages.ColumnCreditNote;
        Input total = new Input(editArea, "= " + label); //$NON-NLS-1$
        total.bindValue(Properties.total.name(), label, Values.Amount, true);
        total.bindCurrency(Properties.accountCurrencyCode.name());

        //
        // form layout
        //

        int width = stringWidth(total.value, "12345678,00"); //$NON-NLS-1$

        startingWith(valueSecurity.getControl(), lblSecurity).withSuffix(curSecurity)
                        .thenBelow(valuePortfolio.getControl()).withLabel(lblPortfolio).withSuffix(referencePortfolio)
                        .thenBelow(valueDate)
                        .withLabel(lblDate)
                        // shares - quote - lump sum
                        .thenBelow(shares.value).width(width).withLabel(shares.label).thenRight(quote.label)
                        .thenRight(quote.value).width(width).thenRight(quote.currency).width(width)
                        .thenRight(lumpSum.label).thenRight(lumpSum.value).width(width).thenRight(lumpSum.currency);

        startingWith(quote.value).thenBelow(exchangeRate.value).width(width).withLabel(exchangeRate.label)
                        .thenRight(exchangeRate.currency).width(width);

        startingWith(lumpSum.value)
                        // converted lump sum
                        .thenBelow(convertedLumpSum.value).width(width).withLabel(convertedLumpSum.label)
                        .withSuffix(convertedLumpSum.currency)
                        // fees
                        .thenBelow(fees.value).width(width).withLabel(fees.label).withSuffix(fees.currency)
                        // taxes
                        .thenBelow(taxes.value).width(width).withLabel(taxes.label).withSuffix(taxes.currency)
                        // total
                        .thenBelow(total.value).width(width).withLabel(total.label).withSuffix(total.currency);

        //
        // hide / show exchange rate if necessary
        //

        model.addPropertyChangeListener(Properties.exchangeRateCurrencies.name(), new PropertyChangeListener()
        {
            @Override
            public void propertyChange(PropertyChangeEvent event)
            {
                String securityCurrency = model.getSecurityCurrencyCode();
                String accountCurrency = model.getAccountCurrencyCode();

                // make exchange rate visible if both are set but different

                boolean visible = securityCurrency.length() > 0 && accountCurrency.length() > 0
                                && !securityCurrency.equals(accountCurrency);

                exchangeRate.setVisible(visible);
                convertedLumpSum.setVisible(visible);
            }
        });

        model.firePropertyChange(Properties.exchangeRateCurrencies.name(), "", model.getExchangeRateCurrencies()); //$NON-NLS-1$
    }

    private String sign()
    {
        switch (model.getType())
        {
            case BUY:
                return "+ "; //$NON-NLS-1$
            case SELL:
                return "- "; //$NON-NLS-1$
            default:
                throw new UnsupportedOperationException();
        }

    }

    @Override
    protected void okPressed()
    {
        model.applyChanges();
        super.okPressed();
    }

    public void setPortfolio(Portfolio portfolio)
    {
        model.setPortfolio(portfolio);
    }

    public void setSecurity(Security security)
    {
        model.setSecurity(security);
    }

    public void setBuySellEntry(BuySellEntry entry)
    {
        model.setBuySellEntry(entry);
    }
}
