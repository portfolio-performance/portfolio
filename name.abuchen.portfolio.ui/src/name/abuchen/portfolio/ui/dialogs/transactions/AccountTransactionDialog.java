package name.abuchen.portfolio.ui.dialogs.transactions;

import static name.abuchen.portfolio.ui.util.FormDataFactory.startingWith;
import static name.abuchen.portfolio.ui.util.SWTHelper.amountWidth;
import static name.abuchen.portfolio.ui.util.SWTHelper.currencyWidth;
import static name.abuchen.portfolio.ui.util.SWTHelper.widest;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.core.databinding.beans.typed.BeanProperties;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.e4.core.di.extensions.Preference;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.databinding.swt.typed.WidgetProperties;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.CurrencyConverterImpl;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.ClientSnapshot;
import name.abuchen.portfolio.snapshot.PortfolioSnapshot;
import name.abuchen.portfolio.snapshot.SecurityPosition;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.dialogs.transactions.AccountTransactionModel.Properties;
import name.abuchen.portfolio.ui.util.FormDataFactory;
import name.abuchen.portfolio.ui.util.LabelOnly;
import name.abuchen.portfolio.ui.util.SWTHelper;

public class AccountTransactionDialog extends AbstractTransactionDialog // NOSONAR
{
    @Inject
    private Client client;

    @Preference(value = UIConstants.Preferences.USE_INDIRECT_QUOTATION)
    @Inject
    private boolean useIndirectQuotation = false;

    private Menu contextMenu;

    @Inject
    public AccountTransactionDialog(@Named(IServiceConstants.ACTIVE_SHELL) Shell parentShell)
    {
        super(parentShell);
    }

    @PostConstruct
    private void createModel(ExchangeRateProviderFactory factory, AccountTransaction.Type type) // NOSONAR
    {
        AccountTransactionModel m = new AccountTransactionModel(client, type);
        m.setExchangeRateProviderFactory(factory);
        setModel(m);

        // set account only if exactly one exists
        // (otherwise force user to choose)
        List<Account> activeAccounts = client.getActiveAccounts();
        if (activeAccounts.size() == 1)
            m.setAccount(activeAccounts.get(0));
    }

    private AccountTransactionModel model()
    {
        return (AccountTransactionModel) this.model;
    }

    @Override
    protected void createFormElements(Composite editArea) // NOSONAR
    {
        //
        // input elements
        //

        // security

        ComboInput securities = null;
        if (model().supportsSecurity())
            securities = setupSecurities(editArea);

        // account

        ComboInput accounts = new ComboInput(editArea, Messages.ColumnAccount);
        accounts.value.setInput(including(client.getActiveAccounts(), model().getAccount()));
        accounts.bindValue(Properties.account.name(), Messages.MsgMissingAccount);
        accounts.bindCurrency(Properties.accountCurrencyCode.name());

        // date & time

        DateTimeInput dateTime = new DateTimeInput(editArea, Messages.ColumnDate);
        dateTime.bindDate(Properties.date.name());
        dateTime.bindTime(Properties.time.name());
        dateTime.bindButton(() -> model().getTime(), time -> model().setTime(time));

        // shares

        Input shares = new Input(editArea, Messages.ColumnShares);
        shares.bindValue(Properties.shares.name(), Messages.ColumnShares, Values.Share, false);
        shares.setVisible(model().supportsShares());

        Button btnShares = new Button(editArea, SWT.ARROW | SWT.DOWN);
        btnShares.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                showSharesContextMenu();
            }
        });
        btnShares.setVisible(model().supportsShares());
        editArea.addDisposeListener(e -> AccountTransactionDialog.this.widgetDisposed());

        Input dividendAmount = new Input(editArea, Messages.LabelDividendPerShare);
        dividendAmount.bindBigDecimal(Properties.dividendAmount.name(), "#,##0.0000"); //$NON-NLS-1$
        dividendAmount.bindCurrency(Properties.fxCurrencyCode.name());
        dividendAmount.setVisible(model().supportsShares());

        // other input fields

        String totalLabel = model().supportsTaxUnits() ? Messages.ColumnGrossValue : getTotalLabel();
        Input fxGrossAmount = new Input(editArea, totalLabel);
        fxGrossAmount.bindValue(Properties.fxGrossAmount.name(), totalLabel, Values.Amount, true);
        fxGrossAmount.bindCurrency(Properties.fxCurrencyCode.name());

        ExchangeRateInput exchangeRate = new ExchangeRateInput(editArea, useIndirectQuotation ? "/ " : "x "); //$NON-NLS-1$ //$NON-NLS-2$
        exchangeRate.bindBigDecimal(
                        useIndirectQuotation ? Properties.inverseExchangeRate.name() : Properties.exchangeRate.name(),
                        Values.ExchangeRate.pattern());
        exchangeRate.bindCurrency(useIndirectQuotation ? Properties.inverseExchangeRateCurrencies.name()
                        : Properties.exchangeRateCurrencies.name());
        exchangeRate.bindInvertAction(() -> model()
                        .setExchangeRate(BigDecimal.ONE.divide(model().getExchangeRate(), 10, RoundingMode.HALF_DOWN)));

        model().addPropertyChangeListener(Properties.exchangeRate.name(),
                        e -> exchangeRate.value.setToolTipText(AbstractModel.createCurrencyToolTip(
                                        model().getExchangeRate(), model().getAccountCurrencyCode(),
                                        model().getSecurityCurrencyCode())));

        Input grossAmount = new Input(editArea, "="); //$NON-NLS-1$
        grossAmount.bindValue(Properties.grossAmount.name(), totalLabel, Values.Amount, true);
        grossAmount.bindCurrency(Properties.accountCurrencyCode.name());

        // fees

        Label plusForexFees = new Label(editArea, SWT.NONE);
        plusForexFees.setText("+"); //$NON-NLS-1$
        plusForexFees.setVisible(model().supportsFees());

        Input forexFees = new Input(editArea, Messages.ColumnFees);
        forexFees.bindValue(Properties.fxFees.name(), Messages.ColumnFees, Values.Amount, false);
        forexFees.bindCurrency(Properties.securityCurrencyCode.name());
        forexFees.setVisible(model().supportsFees());

        Input fees = new Input(editArea, Messages.ColumnFees);
        fees.bindValue(Properties.fees.name(), Messages.ColumnFees, Values.Amount, false);
        fees.bindCurrency(Properties.accountCurrencyCode.name());
        fees.setVisible(model().supportsFees());
        fees.label.setVisible(false); // will only show if no fx available

        // taxes

        Label plusForexTaxes = new Label(editArea, SWT.NONE);
        plusForexTaxes.setText("+"); //$NON-NLS-1$
        plusForexTaxes.setVisible(model().supportsTaxUnits());

        Input forexTaxes = new Input(editArea, Messages.ColumnTaxes);
        forexTaxes.bindValue(Properties.fxTaxes.name(), Messages.ColumnTaxes, Values.Amount, false);
        forexTaxes.bindCurrency(Properties.fxCurrencyCode.name());
        forexTaxes.setVisible(model().supportsTaxUnits());

        Input taxes = new Input(editArea, Messages.ColumnTaxes);
        taxes.bindValue(Properties.taxes.name(), Messages.ColumnTaxes, Values.Amount, false);
        taxes.bindCurrency(Properties.accountCurrencyCode.name());
        taxes.setVisible(model().supportsTaxUnits());
        taxes.label.setVisible(false); // will only show if no fx available

        // total

        Input total = new Input(editArea, getTotalLabel());
        total.bindValue(Properties.total.name(), Messages.ColumnTotal, Values.Amount, false);
        total.bindCurrency(Properties.accountCurrencyCode.name());
        total.setVisible(model().supportsTaxUnits() || model().supportsFees());

        // note

        Label lblNote = new Label(editArea, SWT.LEFT);
        lblNote.setText(Messages.ColumnNote);
        Text valueNote = new Text(editArea, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
        IObservableValue<?> targetNote = WidgetProperties.text(SWT.Modify).observe(valueNote);
        IObservableValue<?> noteObservable = BeanProperties.value(Properties.note.name()).observe(model);
        context.bindValue(targetNote, noteObservable);

        //
        // form layout
        //

        int widest = widest(securities != null ? securities.label : null, accounts.label, dateTime.label, shares.label,
                        taxes.label, fees.label, total.label, lblNote, fxGrossAmount.label);

        FormDataFactory forms;
        if (securities != null)
        {
            forms = startingWith(securities.value.getControl(), securities.label).suffix(securities.currency)
                            .thenBelow(accounts.value.getControl()).label(accounts.label).suffix(accounts.currency);
            startingWith(securities.label).width(widest);
        }
        else
        {
            forms = startingWith(accounts.value.getControl(), accounts.label).suffix(accounts.currency);
            startingWith(accounts.label).width(widest);
        }

        int amountWidth = amountWidth(grossAmount.value);
        int currencyWidth = currencyWidth(fxGrossAmount.currency);

        // date
        // shares
        forms = forms.thenBelow(dateTime.date.getControl()).label(dateTime.label);

        startingWith(dateTime.date.getControl()).thenRight(dateTime.time).thenRight(dateTime.button, 0);

        // shares [- amount per share]
        forms.thenBelow(shares.value).width(amountWidth).label(shares.label).suffix(btnShares) //
                        // fxAmount - exchange rate - amount
                        .thenBelow(fxGrossAmount.value).width(amountWidth).label(fxGrossAmount.label) //
                        .thenRight(fxGrossAmount.currency).width(currencyWidth) //
                        .thenRight(exchangeRate.label) //
                        .thenRight(exchangeRate.value).width(amountWidth) //
                        .thenRight(exchangeRate.buttonInvertExchangeRate, 0) //
                        .thenRight(exchangeRate.currency).width(amountWidth) //
                        .thenRight(grossAmount.label) //
                        .thenRight(grossAmount.value).width(amountWidth) //
                        .thenRight(grossAmount.currency).width(currencyWidth);

        if (model().supportsShares())
        {
            // shares [- amount per share]
            startingWith(btnShares).thenRight(dividendAmount.label) //
                            .thenRight(dividendAmount.value).width(amountWidth) //
                            .thenRight(dividendAmount.currency).width(currencyWidth); //
        }

        forms = startingWith(grossAmount.value);

        // fees
        if (model().supportsFees())
        {
            forms.thenBelow(fees.value).width(amountWidth).label(fees.label).suffix(fees.currency);

            startingWith(fees.value).thenLeft(plusForexFees).thenLeft(forexFees.currency).width(currencyWidth)
                            .thenLeft(forexFees.value).width(amountWidth).thenLeft(forexFees.label);

            forms = startingWith(fees.value);
        }

        // forexTaxes - taxes
        if (model().supportsTaxUnits())
        {
            forms.thenBelow(taxes.value).width(amountWidth).label(taxes.label).suffix(taxes.currency);

            startingWith(taxes.value).thenLeft(plusForexTaxes).thenLeft(forexTaxes.currency).width(currencyWidth)
                            .thenLeft(forexTaxes.value).width(amountWidth).thenLeft(forexTaxes.label);

            forms = startingWith(taxes.value);
        }

        // total
        if (model().supportsFees() || model().supportsTaxUnits())
        {
            forms = forms.thenBelow(total.value).width(amountWidth).label(total.label).thenRight(total.currency)
                            .width(currencyWidth);
        }

        // note
        forms.thenBelow(valueNote).height(SWTHelper.lineHeight(valueNote) * 3).left(accounts.value.getControl())
                        .right(grossAmount.value).label(lblNote);

        //
        // hide / show exchange rate if necessary
        //

        model.addPropertyChangeListener(Properties.exchangeRateCurrencies.name(), event -> { // NOSONAR
            String securityCurrency = model().getSecurityCurrencyCode();
            String accountCurrency = model().getAccountCurrencyCode();

            // make exchange rate visible if both are set but different
            boolean isFxVisible = securityCurrency.length() > 0 && accountCurrency.length() > 0
                            && !securityCurrency.equals(accountCurrency);

            exchangeRate.setVisible(isFxVisible);
            grossAmount.setVisible(isFxVisible);

            plusForexFees.setVisible(isFxVisible && model().supportsShares());
            forexFees.setVisible(isFxVisible && model().supportsShares());
            fees.label.setVisible(!isFxVisible && model().supportsFees());

            plusForexTaxes.setVisible(isFxVisible && model().supportsShares());
            forexTaxes.setVisible(isFxVisible && model().supportsShares());
            taxes.label.setVisible(!isFxVisible && model().supportsTaxUnits());

            // in case fx taxes have been entered
            if (!isFxVisible)
            {
                model().setFxTaxes(0);
                model().setFxFees(0);
            }

            // move input fields to have a nicer layout
            if (isFxVisible)
                startingWith(grossAmount.value).thenBelow(model().supportsFees() ? fees.value : taxes.value);
            else
                startingWith(fxGrossAmount.value).thenBelow(model().supportsFees() ? fees.value : taxes.value);
            editArea.layout();
        });

        WarningMessages warnings = new WarningMessages(this);
        warnings.add(() -> model().getDate().isAfter(LocalDate.now()) ? Messages.MsgDateIsInTheFuture : null);
        model.addPropertyChangeListener(Properties.date.name(), e -> warnings.check());

        model.firePropertyChange(Properties.exchangeRateCurrencies.name(), "", model().getExchangeRateCurrencies()); //$NON-NLS-1$
    }

    private ComboInput setupSecurities(Composite editArea)
    {
        List<Security> activeSecurities = new ArrayList<>();
        activeSecurities.addAll(including(client.getActiveSecurities(), model().getSecurity()));

        // add empty security only if it has not been added previously
        // --> happens when editing an existing transaction
        if (model().supportsOptionalSecurity() && !activeSecurities.contains(AccountTransactionModel.EMPTY_SECURITY))
        {
            activeSecurities.add(0, AccountTransactionModel.EMPTY_SECURITY);

            if (model().getSecurity() == null)
                model().setSecurity(AccountTransactionModel.EMPTY_SECURITY);
        }

        ComboInput securities = new ComboInput(editArea, Messages.ColumnSecurity);
        securities.value.setInput(activeSecurities);
        securities.bindValue(Properties.security.name(), Messages.MsgMissingSecurity);
        securities.bindCurrency(Properties.securityCurrencyCode.name());
        return securities;
    }

    private void showSharesContextMenu()
    {
        if (contextMenu == null)
        {
            MenuManager menuMgr = new MenuManager("#PopupMenu"); //$NON-NLS-1$
            menuMgr.setRemoveAllWhenShown(true);
            menuMgr.addMenuListener(this::sharesMenuAboutToShow);
            contextMenu = menuMgr.createContextMenu(getShell());
        }

        contextMenu.setVisible(true);
    }

    private void sharesMenuAboutToShow(IMenuManager manager) // NOSONAR
    {
        manager.add(new LabelOnly(Messages.DividendsDialogTitleShares));

        CurrencyConverter converter = new CurrencyConverterImpl(model.getExchangeRateProviderFactory(),
                        client.getBaseCurrency());
        ClientSnapshot snapshot = ClientSnapshot.create(client, converter, model().getDate());

        if (snapshot != null && model().getSecurity() != null)
        {
            PortfolioSnapshot jointPortfolio = snapshot.getJointPortfolio();
            addAction(manager, jointPortfolio, Messages.ColumnSharesOwned);

            List<PortfolioSnapshot> list = snapshot.getPortfolios();
            if (list.size() > 1)
            {
                for (PortfolioSnapshot ps : list)
                    addAction(manager, ps, ps.getPortfolio().getName());
            }
        }

        manager.add(new Action(Messages.DividendsDialogLabelSpecialDistribution)
        {
            @Override
            public void run()
            {
                model().setShares(0);
            }
        });
    }

    private void addAction(IMenuManager manager, PortfolioSnapshot portfolio, final String label)
    {
        final SecurityPosition position = portfolio.getPositionsBySecurity().get(model().getSecurity());
        if (position != null)
        {
            Action action = new Action(MessageFormat.format(Messages.DividendsDialogLabelPortfolioSharesHeld,
                            Values.Share.format(position.getShares()), label, Values.Date.format(portfolio.getTime())))
            {
                @Override
                public void run()
                {
                    model().setShares(position.getShares());
                }
            };
            manager.add(action);
        }
    }

    private void widgetDisposed()
    {
        if (contextMenu != null && !contextMenu.isDisposed())
            contextMenu.dispose();
    }

    private String getTotalLabel() // NOSONAR
    {
        switch (model().getType())
        {
            case TAXES:
            case FEES:
            case INTEREST_CHARGE:
            case REMOVAL:
                return Messages.ColumnDebitNote;
            case INTEREST:
            case TAX_REFUND:
            case DIVIDENDS:
            case DEPOSIT:
            case FEES_REFUND:
                return Messages.ColumnCreditNote;
            case BUY:
            case SELL:
            case TRANSFER_IN:
            case TRANSFER_OUT:
            default:
                throw new UnsupportedOperationException();
        }
    }

    @Override
    public void setAccount(Account account)
    {
        model().setAccount(account);
    }

    @Override
    public void setSecurity(Security security)
    {
        model().setSecurity(security);
    }

    public void setTransaction(Account account, AccountTransaction transaction)
    {
        model().setSource(account, transaction);
    }

}
