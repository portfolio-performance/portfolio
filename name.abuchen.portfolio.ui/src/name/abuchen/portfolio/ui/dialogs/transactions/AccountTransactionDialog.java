package name.abuchen.portfolio.ui.dialogs.transactions;

import static name.abuchen.portfolio.ui.util.FormDataFactory.startingWith;
import static name.abuchen.portfolio.ui.util.SWTHelper.amountWidth;
import static name.abuchen.portfolio.ui.util.SWTHelper.currencyWidth;
import static name.abuchen.portfolio.ui.util.SWTHelper.widest;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.core.databinding.beans.BeanProperties;
import org.eclipse.e4.core.di.extensions.Preference;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.databinding.swt.WidgetProperties;
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
import name.abuchen.portfolio.ui.util.DateTimePicker;
import name.abuchen.portfolio.ui.util.FormDataFactory;
import name.abuchen.portfolio.ui.util.LabelOnly;
import name.abuchen.portfolio.ui.util.SimpleDateTimeSelectionProperty;

public class AccountTransactionDialog extends AbstractTransactionDialog
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
    private void createModel(ExchangeRateProviderFactory factory, AccountTransaction.Type type)
    {
        AccountTransactionModel m = new AccountTransactionModel(client, type);
        m.setExchangeRateProviderFactory(factory);
        setModel(m);
    }

    private AccountTransactionModel model()
    {
        return (AccountTransactionModel) this.model;
    }

    @Override
    protected void createFormElements(Composite editArea)
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

        // date

        Label lblDate = new Label(editArea, SWT.RIGHT);
        lblDate.setText(Messages.ColumnDate);
        DateTimePicker valueDate = new DateTimePicker(editArea);
        context.bindValue(new SimpleDateTimeSelectionProperty().observe(valueDate.getControl()),
                        BeanProperties.value(Properties.date.name()).observe(model));

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

        // other input fields

        String totalLabel = getTotalLabel();
        Input fxAmount = new Input(editArea, totalLabel);
        fxAmount.bindValue(Properties.fxAmount.name(), totalLabel, Values.Amount, true);
        fxAmount.bindCurrency(Properties.fxCurrencyCode.name());

        Input exchangeRate = new Input(editArea, useIndirectQuotation ? "/ " : "x "); //$NON-NLS-1$ //$NON-NLS-2$
        exchangeRate.bindBigDecimal(
                        useIndirectQuotation ? Properties.inverseExchangeRate.name() : Properties.exchangeRate.name(),
                        Values.ExchangeRate.pattern(), Messages.ColumnExchangeRate);
        exchangeRate.bindCurrency(useIndirectQuotation ? Properties.inverseExchangeRateCurrencies.name()
                        : Properties.exchangeRateCurrencies.name());

        model().addPropertyChangeListener(Properties.exchangeRate.name(),
                        e -> exchangeRate.value.setToolTipText(AbstractModel.createCurrencyToolTip(
                                        model().getExchangeRate(), model().getAccountCurrencyCode(),
                                        model().getSecurityCurrencyCode())));

        Input amount = new Input(editArea, "="); //$NON-NLS-1$
        amount.bindValue(Properties.amount.name(), totalLabel, Values.Amount, true);
        amount.bindCurrency(Properties.accountCurrencyCode.name());

        // note
        Label lblNote = new Label(editArea, SWT.LEFT);
        lblNote.setText(Messages.ColumnNote);
        Text valueNote = new Text(editArea, SWT.BORDER);
        context.bindValue(WidgetProperties.text(SWT.Modify).observe(valueNote),
                        BeanProperties.value(Properties.note.name()).observe(model));

        //
        // form layout
        //

        int widest = widest(securities != null ? securities.label : null, accounts.label, lblDate, shares.label,
                        fxAmount.label, lblNote);

        FormDataFactory forms = null;
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

        int amountWidth = amountWidth(amount.value);
        int currencyWidth = currencyWidth(fxAmount.currency);

        forms.thenBelow(valueDate.getControl()).label(lblDate) //
                        .thenBelow(shares.value).width(amountWidth).label(shares.label).suffix(btnShares) //
                        // fxAmount - exchange rate - amount
                        .thenBelow(fxAmount.value).width(amountWidth).label(fxAmount.label) //
                        .thenRight(fxAmount.currency).width(currencyWidth) //
                        .thenRight(exchangeRate.label) //
                        .thenRight(exchangeRate.value).width(amountWidth) //
                        .thenRight(exchangeRate.currency).width(amountWidth) //
                        .thenRight(amount.label) //
                        .thenRight(amount.value).width(amountWidth) //
                        // note
                        .suffix(amount.currency) //
                        .thenBelow(valueNote).left(accounts.value.getControl()).right(amount.value).label(lblNote);

        //
        // hide / show exchange rate if necessary
        //

        model.addPropertyChangeListener(Properties.exchangeRateCurrencies.name(), new PropertyChangeListener()
        {
            @Override
            public void propertyChange(PropertyChangeEvent event)
            {
                String securityCurrency = model().getSecurityCurrencyCode();
                String accountCurrency = model().getAccountCurrencyCode();

                // make exchange rate visible if both are set but different

                boolean visible = securityCurrency.length() > 0 && accountCurrency.length() > 0
                                && !securityCurrency.equals(accountCurrency);

                exchangeRate.setVisible(visible);
                amount.setVisible(visible);
            }
        });

        WarningMessages warnings = new WarningMessages(this);
        warnings.add(() -> model().getDate().isAfter(LocalDate.now()) ? Messages.MsgDateIsInTheFuture : null);
        model.addPropertyChangeListener(Properties.date.name(), e -> warnings.check());

        model.firePropertyChange(Properties.exchangeRateCurrencies.name(), "", model().getExchangeRateCurrencies()); //$NON-NLS-1$
    }

    private ComboInput setupSecurities(Composite editArea)
    {
        ComboInput securities;
        List<Security> activeSecurities = new ArrayList<Security>();
        activeSecurities.addAll(including(client.getActiveSecurities(), model().getSecurity()));
        if (model().supportsOptionalSecurity())
            activeSecurities.add(0, AccountTransactionModel.EMPTY_SECURITY);

        securities = new ComboInput(editArea, Messages.ColumnSecurity);
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
            menuMgr.addMenuListener(new IMenuListener()
            {
                @Override
                public void menuAboutToShow(IMenuManager manager)
                {
                    sharesMenuAboutToShow(manager);
                }
            });
            contextMenu = menuMgr.createContextMenu(getShell());
        }

        contextMenu.setVisible(true);
    }

    private void sharesMenuAboutToShow(IMenuManager manager)
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
                    addAction(manager, ps, ps.getSource().getName());
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

    private String getTotalLabel()
    {
        switch (model().getType())
        {
            case TAXES:
            case FEES:
            case REMOVAL:
                return Messages.ColumnDebitNote;
            case INTEREST:
            case TAX_REFUND:
            case DIVIDENDS:
            case DEPOSIT:
                return Messages.ColumnCreditNote;
            case BUY:
            case SELL:
            case TRANSFER_IN:
            case TRANSFER_OUT:
            default:
                throw new UnsupportedOperationException();
        }
    }

    public void setAccount(Account account)
    {
        model().setAccount(account);
    }

    public void setSecurity(Security security)
    {
        model().setSecurity(security);
    }

    public void setTransaction(Account account, AccountTransaction transaction)
    {
        model().setSource(account, transaction);
    }

}
