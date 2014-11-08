package name.abuchen.portfolio.ui.dialogs;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.InvestmentPlan;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.BindingHelper;
import name.abuchen.portfolio.util.Dates;

import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

public class NewPlanDialog extends AbstractDialog
{
    public static final Account DELIVERY = new Account(Messages.InvestmentPlanOptionDelivery);

    public static class Model extends BindingHelper.Model
    {
        private String name;
        private Security security;
        private Portfolio portfolio;
        private Account account;
        private Date start = Dates.today();
        private int interval = 1;
        private long amount;
        private long fees;

        public Model(Client client)
        {
            super(client);

            if (client.getPortfolios().size() == 1)
                portfolio = client.getPortfolios().get(0);

            if (!client.getSecurities().isEmpty())
                security = client.getSecurities().get(0);

            account = DELIVERY;
        }

        @Override
        public void applyChanges()
        {
            if (security == null)
                throw new UnsupportedOperationException(Messages.MsgMissingSecurity);
            if (portfolio == null)
                throw new UnsupportedOperationException(Messages.MsgMissingPortfolio);

            InvestmentPlan plan = new InvestmentPlan(name);
            plan.setSecurity(security);
            plan.setPortfolio(portfolio);
            plan.setAccount(account.equals(DELIVERY) ? null : account);
            plan.setStart(start);
            plan.setInterval(interval);
            plan.setAmount(amount);
            plan.setFees(fees);
            getClient().addPlan(plan);
        }

        public String getName()
        {
            return name;
        }

        public void setName(String name)
        {
            firePropertyChange("name", this.name, this.name = name); //$NON-NLS-1$
        }

        public Security getSecurity()
        {
            return security;
        }

        public void setSecurity(Security security)
        {
            firePropertyChange("security", this.security, this.security = security); //$NON-NLS-1$
        }

        public Portfolio getPortfolio()
        {
            return portfolio;
        }

        public void setPortfolio(Portfolio portfolio)
        {
            firePropertyChange("portfolio", this.portfolio, this.portfolio = portfolio); //$NON-NLS-1$
        }

        public Account getAccount()
        {
            return account;
        }

        public void setAccount(Account account)
        {
            firePropertyChange("account", this.account, this.account = account); //$NON-NLS-1$
        }

        public Date getStart()
        {
            return start;
        }

        public void setStart(Date start)
        {
            firePropertyChange("start", this.start, this.start = start); //$NON-NLS-1$
        }

        public int getInterval()
        {
            return interval;
        }

        public void setInterval(int interval)
        {
            this.interval = interval;
        }

        public long getAmount()
        {
            return amount;
        }

        public void setAmount(long amount)
        {
            firePropertyChange("amount", this.amount, this.amount = amount); //$NON-NLS-1$
        }

        public long getFees()
        {
            return fees;
        }

        public void setFees(long fees)
        {
            firePropertyChange("fees", this.fees, this.fees = fees); //$NON-NLS-1$
        }

    }

    public NewPlanDialog(Shell shell, Client client)
    {
        super(shell, Messages.InvestmentPlanTitleNewPlan, new Model(client));
    }

    protected void configureShell(Shell shell)
    {
        super.configureShell(shell);
        shell.setText(Messages.InvestmentPlanTitleNewPlan);
    }

    protected void createFormElements(Composite editArea)
    {
        bindings().bindMandatoryStringInput(editArea, Messages.ColumnName, "name"); //$NON-NLS-1$

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

        bindings().bindComboViewer(editArea, Messages.ColumnPortfolio, "portfolio", new LabelProvider() //$NON-NLS-1$
                        {
                            @Override
                            public String getText(Object element)
                            {
                                return ((Portfolio) element).getName();
                            }

                        }, new IValidator()
                        {
                            @Override
                            public IStatus validate(Object value)
                            {
                                return value != null ? ValidationStatus.ok() : ValidationStatus
                                                .error(Messages.MsgMissingPortfolio);
                            }
                        }, getModel().getClient().getPortfolios().toArray());

        List<Account> accounts = getModel().getClient().getActiveAccounts();
        accounts.add(0, DELIVERY);

        bindings().bindComboViewer(editArea, Messages.ColumnAccount, "account", new LabelProvider() //$NON-NLS-1$
                        {
                            @Override
                            public String getText(Object element)
                            {
                                return ((Account) element).getName();
                            }
                        }, accounts.toArray());

        bindings().bindDatePicker(editArea, Messages.ColumnStartDate, "start"); //$NON-NLS-1$

        List<Integer> available = new ArrayList<Integer>();
        for (int ii = 1; ii <= 12; ii++)
            available.add(ii);
        bindings().bindComboViewer(editArea, Messages.ColumnInterval, "interval", new LabelProvider() //$NON-NLS-1$
                        {
                            @Override
                            public String getText(Object element)
                            {
                                int interval = (Integer) element;
                                return MessageFormat.format(Messages.InvestmentPlanIntervalLabel, interval);
                            }
                        }, available);

        bindings().bindMandatoryAmountInput(editArea, Messages.ColumnAmount, "amount"); //$NON-NLS-1$
        bindings().bindAmountInput(editArea, Messages.ColumnFees, "fees"); //$NON-NLS-1$
    }
}
