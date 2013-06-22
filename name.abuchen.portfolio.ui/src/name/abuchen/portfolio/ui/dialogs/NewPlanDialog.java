package name.abuchen.portfolio.ui.dialogs;

import java.util.Date;

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

    public static class Model extends BindingHelper.Model
    {
        private String name;
        private Security security;
        private Portfolio portfolio;
        private Date start = Dates.today();
        private long amount;
        private long transactionCost;
        private boolean generateAccountTransactions = false;

        public Model(Client client)
        {
            super(client);

            if (client.getPortfolios().size() == 1)
                portfolio = client.getPortfolios().get(0);

            if (!client.getSecurities().isEmpty())
                security = client.getSecurities().get(0);
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
            plan.setStart(start);
            plan.setAmount(amount);
            plan.setTransactionCost(transactionCost);
            plan.setIsBuySellEntry(generateAccountTransactions);
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

        public Portfolio getPortfolio()
        {
            return portfolio;
        }

        public void setPortfolio(Portfolio portfolio)
        {
            firePropertyChange("portfolio", this.portfolio, this.portfolio = portfolio); //$NON-NLS-1$
        }

        public Security getSecurity()
        {
            return security;
        }

        public void setSecurity(Security security)
        {
            firePropertyChange("security", this.security, this.security = security); //$NON-NLS-1$
        }

        public Date getStart()
        {
            return start;
        }

        public void setStart(Date start)
        {
            firePropertyChange("start", this.start, this.start = start); //$NON-NLS-1$
        }

        public long getAmount()
        {
            return amount;
        }

        public void setAmount(long amount)
        {
            firePropertyChange("amount", this.amount, this.amount = amount); //$NON-NLS-1$
        }

        public long getTransactionCost()
        {
            return transactionCost;
        }

        public void setTransactionCost(long cost)
        {
            firePropertyChange("transactionCost", this.transactionCost, this.transactionCost = cost); //$NON-NLS-1$
        }

        public boolean isGenerateAccountTransactions()
        {
            return generateAccountTransactions;
        }

        public void setGenerateAccountTransactions(boolean generateAccountTransaction)
        {
            firePropertyChange("generateAccountTransaction", this.generateAccountTransactions, //$NON-NLS-1$
                            this.generateAccountTransactions = generateAccountTransaction);
        }

    }

    public NewPlanDialog(Shell shell, Client client)
    {
        super(shell, "New Plan", new Model(client));
    }

    protected void configureShell(Shell shell)
    {
        super.configureShell(shell);
        shell.setText("Create new Plan");
    }

    protected void createFormElements(Composite editArea)
    {
        bindings().bindMandatoryStringInput(editArea, Messages.ColumnName, "name"); //$NON-NLS-1$

        bindings().bindComboViewer(editArea, Messages.ColumnSecurity, "security", new LabelProvider() //$NON-NLS-1$
                        {
                            @Override
                            public String getText(Object element)
                            {
                                return ((Security) element).getName();
                            }
                        }, getModel().getClient().getSecurities().toArray());

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

        bindings().bindDatePicker(editArea, "Plan Start", "start");
        bindings().bindMandatoryAmountInput(editArea, Messages.ColumnAmount, "amount"); //$NON-NLS-1$
        bindings().bindAmountInput(editArea, Messages.ColumnFees, "transactionCost"); //$NON-NLS-1$
        bindings().bindBooleanInput(editArea, "Account Transactions?", "generateAccountTransactions");
    }
}
