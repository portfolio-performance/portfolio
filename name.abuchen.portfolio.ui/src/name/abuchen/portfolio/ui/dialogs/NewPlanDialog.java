package name.abuchen.portfolio.ui.dialogs;

import java.util.Date;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.InvestmentPlan;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.BindingHelper;
import name.abuchen.portfolio.util.Dates;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

public class NewPlanDialog extends AbstractDialog
{

    public static class Model extends BindingHelper.Model
    {

        String name;
        private Portfolio portfolio;
        long amount;
        long transactionCost;
        Security security;
        Date start = Dates.today();
        int period = 1;
        boolean generateAccountTransactions = false;

        public Model(Client client)
        {
            super(client);
        }

        @Override
        public void applyChanges()
        {
            if (security == null)
                throw new UnsupportedOperationException(Messages.MsgMissingSecurity);
            if (portfolio == null)
                throw new UnsupportedOperationException("Portfolio is Missing");
            InvestmentPlan plan = new InvestmentPlan(name);
            plan.setAmount(amount);
            plan.setTransactionCost(transactionCost);
            plan.setPortfolio(portfolio);
            plan.setSecurity(security);
            plan.setStart(start);
            plan.setDayOfMonth(period);
            plan.setGenerateAccountTransactions(generateAccountTransactions);
            getClient().addPlan(plan);
        }

        public void setPortfolio(Portfolio port)
        {
            firePropertyChange("portfolio", portfolio, portfolio = port);
        }

        public Portfolio getPortfolio()
        {
            return portfolio;
        }

        public String getName()
        {
            return name;
        }

        public void setName(String name)
        {
            firePropertyChange("name", this.name, this.name = name);
        }

        public long getAmount()
        {
            return amount;
        }

        public void setAmount(long amount)
        {
            firePropertyChange("amount", this.amount, this.amount = amount);
        }

        public Security getSecurity()
        {
            return security;
        }

        public void setSecurity(Security security)
        {
            firePropertyChange("security", this.security, this.security = security);
        }

        public Date getStart()
        {
            return start;
        }

        public void setStart(Date start)
        {
            firePropertyChange("start", this.start, this.start = start);
        }

        public int getPeriod()
        {
            return period;
        }

        public void setPeriod(int period)
        {
            firePropertyChange("period", this.period, this.period = period);
        }
        
        public long getTransactionCost()
        {
            return transactionCost;
        }

        public void setTransactionCost(long cost)
        {
            firePropertyChange("transactionCost", this.transactionCost, this.transactionCost = cost);
        }

        public boolean isGenerateAccountTransactions()
        {
            return generateAccountTransactions;
        }

        public void setGenerateAccountTransactions(boolean generateAccountTransaction)
        {
            firePropertyChange("generateAccountTransaction", this.generateAccountTransactions, 
                            this.generateAccountTransactions = generateAccountTransaction);
        }

    }

    private Client client;

    public NewPlanDialog(Shell shell, Client client)
    {
        super(shell, "New Plan", new Model(client));
        this.client = client;
    }

    protected void createFormElements(Composite editArea)
    {
        bindings().bindMandatoryStringInput(editArea, "Name", "name");
        bindings().bindMandatoryAmountInput(editArea, "Amount", "amount");
        bindings().bindMandatoryAmountInput(editArea, "Transaction Cost", "transactionCost");
        bindings().bindBooleanInput(editArea, "Account Transactions?", "generateAccountTransactions");
        bindings().bindComboViewer(editArea, "Portfolio", "portfolio", new LabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                return ((Portfolio) element).getName();
            }

        }, client.getPortfolios().toArray());
        bindings().bindComboViewer(editArea, "Security", "security", new LabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                return ((Security) element).getName();
            }
        }, client.getSecurities().toArray());
        bindings().bindDatePicker(editArea, "Plan Start", "start");
        bindings().bindSpinner(editArea, "Period", "period", 1, 30, 5, 1);
    }

    protected void configureShell(Shell shell)
    {
        super.configureShell(shell);
        shell.setText("Create new Plan");
    }

}
