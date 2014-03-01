package name.abuchen.portfolio.ui.dialogs;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Values;
import name.abuchen.portfolio.snapshot.ClientSnapshot;
import name.abuchen.portfolio.snapshot.PortfolioSnapshot;
import name.abuchen.portfolio.snapshot.SecurityPosition;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.BindingHelper;
import name.abuchen.portfolio.ui.util.CurrencyToStringConverter;
import name.abuchen.portfolio.ui.util.LabelOnly;
import name.abuchen.portfolio.ui.util.StringToCurrencyConverter;
import name.abuchen.portfolio.util.Dates;

import org.eclipse.core.databinding.UpdateValueStrategy;
import org.eclipse.core.databinding.beans.BeansObservables;
import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.databinding.swt.SWTObservables;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class DividendsDialog extends AbstractDialog
{
    static final class Model extends BindingHelper.Model
    {
        private Security security;
        private Account account;
        private long shares;
        private long amount;
        private Date date = Dates.today();

        private ClientSnapshot snapshot;

        public Model(Client client, Account account, Security security)
        {
            super(client);

            this.security = security;

            if (security == null && !client.getSecurities().isEmpty())
                setSecurity(client.getSecurities().get(0));

            // set account if given by context
            // *or* if only one account exists
            // *otherwise* force user to choose
            if (account != null)
                setAccount(account);
            else if (client.getAccounts().size() == 1)
                setAccount(client.getAccounts().get(0));

            updateShares();
        }

        private void updateShares()
        {
            if (security == null)
                return;

            if (snapshot == null || !snapshot.getTime().equals(date))
                snapshot = ClientSnapshot.create(getClient(), date);

            SecurityPosition p = snapshot.getJointPortfolio().getPositionsBySecurity().get(security);

            long newValue = p != null ? p.getShares() : 0;
            firePropertyChange("shares", this.shares, this.shares = newValue); //$NON-NLS-1$
        }

        public Security getSecurity()
        {
            return security;
        }

        public void setSecurity(Security security)
        {
            firePropertyChange("security", this.security, this.security = security); //$NON-NLS-1$
            updateShares();
        }

        public Account getAccount()
        {
            return account;
        }

        public void setAccount(Account account)
        {
            firePropertyChange("account", this.account, this.account = account); //$NON-NLS-1$
        }

        public long getShares()
        {
            return shares;
        }

        public void setShares(long shares)
        {
            firePropertyChange("shares", this.shares, this.shares = shares); //$NON-NLS-1$
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
            updateShares();
        }

        ClientSnapshot getSnapshot()
        {
            return snapshot;
        }

        public void applyChanges()
        {
            if (security == null)
                throw new UnsupportedOperationException(Messages.MsgMissingSecurity);

            AccountTransaction ta = new AccountTransaction();
            ta.setAmount(amount);
            ta.setShares(shares);
            ta.setDate(date);
            ta.setSecurity(security);
            ta.setType(AccountTransaction.Type.DIVIDENDS);
            account.addTransaction(ta);
        }
    }

    private Menu contextMenu;

    private boolean allowSelectionOfSecurity = false;

    public DividendsDialog(Shell parentShell, Client client, Account account, Security security)
    {
        super(parentShell, Messages.SecurityMenuDividends, new Model(client, account, security));
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
        List<Account> accounts = new ArrayList<Account>();
        for (Account a : getModel().getClient().getAccounts())
            if (!a.isRetired())
                accounts.add(a);
        Collections.sort(accounts, new Account.ByName());

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
                        }, accounts);

        // amount
        bindings().bindMandatoryAmountInput(editArea, Messages.ColumnAmount, "amount").setFocus(); //$NON-NLS-1$

        // date
        bindings().bindDatePicker(editArea, Messages.ColumnDate, "date"); //$NON-NLS-1$

        addSharesInput(editArea);
    }

    private void addSharesInput(Composite editArea)
    {
        Label l = new Label(editArea, SWT.NONE);
        l.setText(Messages.ColumnShares);

        Composite inputArea = new Composite(editArea, SWT.NONE);
        GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, false).applyTo(inputArea);

        final Text txtValue = new Text(inputArea, SWT.BORDER);
        txtValue.addFocusListener(new FocusAdapter()
        {
            @Override
            public void focusGained(FocusEvent e)
            {
                txtValue.selectAll();
            }
        });

        bindings().getBindingContext().bindValue(
                        SWTObservables.observeText(txtValue, SWT.Modify), //
                        BeansObservables.observeValue(getModel(), "shares"), //$NON-NLS-1$
                        new UpdateValueStrategy().setConverter(new StringToCurrencyConverter(Values.Share)),
                        new UpdateValueStrategy().setConverter(new CurrencyToStringConverter(Values.Share)));

        Button button = new Button(inputArea, SWT.ARROW | SWT.DOWN);
        button.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                showSharesContextMenu();
            }
        });

        GridLayoutFactory.fillDefaults().numColumns(2).applyTo(inputArea);
        GridDataFactory.fillDefaults().grab(true, false).align(SWT.FILL, SWT.CENTER).applyTo(txtValue);
        GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(button);
        editArea.addDisposeListener(new DisposeListener()
        {

            @Override
            public void widgetDisposed(DisposeEvent e)
            {
                DividendsDialog.this.widgetDisposed();
            }
        });
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
        Model model = (Model) getModel();

        manager.add(new LabelOnly(Messages.DividendsDialogTitleShares));

        ClientSnapshot snapshot = model.getSnapshot();

        if (snapshot != null && model.getSecurity() != null)
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
                ((Model) getModel()).setShares(0);
            }
        });
    }

    private void addAction(IMenuManager manager, PortfolioSnapshot portfolio, final String label)
    {
        Model model = (Model) getModel();
        final SecurityPosition position = portfolio.getPositionsBySecurity().get(model.getSecurity());
        if (position != null)
        {
            Action action = new Action(MessageFormat.format(Messages.DividendsDialogLabelPortfolioSharesHeld,
                            Values.Share.format(position.getShares()), label, portfolio.getTime()))
            {
                @Override
                public void run()
                {
                    ((Model) getModel()).setShares(position.getShares());
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
}
