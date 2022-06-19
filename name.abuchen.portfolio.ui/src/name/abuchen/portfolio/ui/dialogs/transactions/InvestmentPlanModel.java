package name.abuchen.portfolio.ui.dialogs.transactions;

import java.text.MessageFormat;
import java.time.LocalDate;

import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.InvestmentPlan;
import name.abuchen.portfolio.model.InvestmentPlan.Type;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.ui.Messages;

public class InvestmentPlanModel extends AbstractModel
{
    public enum Properties
    {
        calculationStatus, name, security, securityCurrencyCode, portfolio, account, accountCurrencyCode, start, interval, amount, fees, transactionCurrencyCode, autoGenerate; // NOSONAR
    }

    public static final Account DELIVERY = new Account(Messages.InvestmentPlanOptionDelivery);

    private final Client client;

    private InvestmentPlan source;

    private String name;
    private InvestmentPlan.Type planType;
    private Security security;
    private Portfolio portfolio;
    private Account account;

    private boolean autoGenerate;

    private LocalDate start = LocalDate.now();

    private int interval = 1;
    private long amount;
    private long fees;

    private IStatus calculationStatus = ValidationStatus.ok();

    public InvestmentPlanModel(Client client, InvestmentPlan.Type planType)
    {
        this.client = client;
        this.planType = planType;
    }

    private boolean isAccountPlan()
    {
        return planType == InvestmentPlan.Type.DEPOSIT || planType == InvestmentPlan.Type.REMOVAL;
    }

    @Override
    public String getHeading()
    {
        String additionalText = ""; //$NON-NLS-1$
        if (planType == InvestmentPlan.Type.DEPOSIT)
            additionalText = ": " + Messages.InvestmentPlanTypeDeposit; //$NON-NLS-1$
        else if (planType == InvestmentPlan.Type.REMOVAL)
            additionalText = ": " + Messages.InvestmentPlanTypeRemoval; //$NON-NLS-1$
        return (source != null ? Messages.InvestmentPlanTitleEditPlan : Messages.InvestmentPlanTitleNewPlan) + additionalText;
    }

    @Override
    public void applyChanges()
    {
        if (security == null && planType == InvestmentPlan.Type.BUY_OR_DELIVERY)
            throw new UnsupportedOperationException(Messages.MsgMissingSecurity);
        if (portfolio == null && planType == InvestmentPlan.Type.BUY_OR_DELIVERY)
            throw new UnsupportedOperationException(Messages.MsgMissingPortfolio);
        if (account == null)
            throw new UnsupportedOperationException(Messages.MsgMissingAccount);

        InvestmentPlan plan = source;

        if (plan == null)
        {
            plan = new InvestmentPlan();
            this.client.addPlan(plan);
        }

        plan.setName(name);
        plan.setSecurity(isAccountPlan() ? null : security);
        plan.setPortfolio(isAccountPlan() ? null : portfolio);
        plan.setAccount(account.equals(DELIVERY) ? null : account);
        plan.setAutoGenerate(autoGenerate);
        plan.setStart(start);
        plan.setInterval(interval);
        plan.setAmount((planType == Type.REMOVAL) ? -amount : amount);
        plan.setFees(fees);
    }

    @Override
    public void resetToNewTransaction()
    {
        this.source = null;

        setName(null);
        setAutoGenerate(false);
        setAmount(0);
        setFees(0);
    }

    public void setSource(InvestmentPlan plan)
    {
        this.source = plan;

        this.name = plan.getName();
        this.planType = plan.getPlanType();
        if (planType == Type.BUY_OR_DELIVERY)
        {
            this.account = plan.getAccount() != null ? plan.getAccount() : DELIVERY;
            this.portfolio = plan.getPortfolio();
            this.security = plan.getSecurity();
        }
        else
        {
            this.account = plan.getAccount();
            this.portfolio = null;
            this.security = null;
        }
        this.autoGenerate = plan.isAutoGenerate();
        this.start = plan.getStart();
        this.interval = plan.getInterval();
        this.amount = plan.getAmount();
        if (planType == Type.REMOVAL)
        {
            this.amount = -this.amount;
        }
        this.fees = plan.getFees();
    }

    @Override
    public IStatus getCalculationStatus()
    {
        return calculationStatus;
    }

    private IStatus calculateStatus()
    {
        if ((account == null || portfolio == null) && planType == Type.BUY_OR_DELIVERY)
            return ValidationStatus.error(MessageFormat.format(Messages.MsgDialogInputRequired, Messages.ColumnPeer));

        if (name == null || name.trim().length() == 0)
            return ValidationStatus.error(MessageFormat.format(Messages.MsgDialogInputRequired, Messages.ColumnName));

        if (security == null && planType == Type.BUY_OR_DELIVERY)
            return ValidationStatus
                            .error(MessageFormat.format(Messages.MsgDialogInputRequired, Messages.MsgMissingSecurity));

        if (amount == 0L)
            return ValidationStatus.error(MessageFormat.format(Messages.MsgDialogInputRequired, Messages.ColumnAmount));

        return ValidationStatus.ok();
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        firePropertyChange(Properties.name.name(), this.name, this.name = name); // NOSONAR
        firePropertyChange(Properties.calculationStatus.name(), this.calculationStatus,
                        this.calculationStatus = calculateStatus()); // NOSONAR
    }

    public Security getSecurity()
    {
        return security;
    }

    public void setSecurity(Security security)
    {
        String oldSecurityCurrency = getSecurityCurrencyCode();
        String oldTransactionCurrency = getTransactionCurrencyCode();
        firePropertyChange(Properties.security.name(), this.security, this.security = security); // NOSONAR
        firePropertyChange(Properties.securityCurrencyCode.name(), oldSecurityCurrency, getSecurityCurrencyCode());
        firePropertyChange(Properties.transactionCurrencyCode.name(), oldTransactionCurrency,
                        getTransactionCurrencyCode());
        firePropertyChange(Properties.calculationStatus.name(), this.calculationStatus,
                        this.calculationStatus = calculateStatus()); // NOSONAR
    }

    public Portfolio getPortfolio()
    {
        return portfolio;
    }

    public void setPortfolio(Portfolio portfolio)
    {
        String oldTransactionCurrency = getTransactionCurrencyCode();
        if (isAccountPlan())
            firePropertyChange(Properties.security.name(), this.security, this.security = null); // NOSONAR
        firePropertyChange(Properties.portfolio.name(), this.portfolio, this.portfolio = portfolio); // NOSONAR
        firePropertyChange(Properties.transactionCurrencyCode.name(), oldTransactionCurrency,
                        getTransactionCurrencyCode());
        firePropertyChange(Properties.calculationStatus.name(), this.calculationStatus,
                        this.calculationStatus = calculateStatus()); // NOSONAR
    }

    public Account getAccount()
    {
        return account;
    }

    public void setAccount(Account account)
    {
        String oldAccountCurrency = getAccountCurrencyCode();
        String oldTransactionCurrency = getTransactionCurrencyCode();
        firePropertyChange(Properties.account.name(), this.account, this.account = account); // NOSONAR
        firePropertyChange(Properties.accountCurrencyCode.name(), oldAccountCurrency, getAccountCurrencyCode());
        firePropertyChange(Properties.transactionCurrencyCode.name(), oldTransactionCurrency,
                        getTransactionCurrencyCode());
        firePropertyChange(Properties.calculationStatus.name(), this.calculationStatus,
                        this.calculationStatus = calculateStatus()); // NOSONAR
    }

    public boolean isAutoGenerate()
    {
        return autoGenerate;
    }

    public void setAutoGenerate(boolean autoGenerate)
    {
        firePropertyChange(Properties.autoGenerate.name(), this.autoGenerate, this.autoGenerate = autoGenerate); // NOSONAR
    }

    public LocalDate getStart()
    {
        return start;
    }

    public void setStart(LocalDate start)
    {
        firePropertyChange(Properties.start.name(), this.start, this.start = start); // NOSONAR
    }

    public int getInterval()
    {
        return interval;
    }

    public void setInterval(int interval)
    {
        firePropertyChange(Properties.interval.name(), this.interval, this.interval = interval); // NOSONAR
    }

    public long getAmount()
    {
        return amount;
    }

    public void setAmount(long amount)
    {
        firePropertyChange(Properties.amount.name(), this.amount, this.amount = amount); // NOSONAR
        firePropertyChange(Properties.calculationStatus.name(), this.calculationStatus,
                        this.calculationStatus = calculateStatus()); // NOSONAR
    }

    public long getFees()
    {
        return fees;
    }

    public void setFees(long fees)
    {
        firePropertyChange(Properties.fees.name(), this.fees, this.fees = fees); // NOSONAR
    }

    public String getSecurityCurrencyCode()
    {
        return security != null ? security.getCurrencyCode() : ""; //$NON-NLS-1$
    }

    public String getAccountCurrencyCode()
    {
        return account != null && !DELIVERY.equals(account) ? account.getCurrencyCode() : ""; //$NON-NLS-1$
    }

    public String getReferenceAccountCurrencyCode()
    {
        return portfolio != null && planType == Type.BUY_OR_DELIVERY ? portfolio.getReferenceAccount().getCurrencyCode()
                        : ""; //$NON-NLS-1$
    }

    public String getTransactionCurrencyCode()
    {
        // transactions will be generated in currency of the account unless it
        // is an inbound delivery (which will be created in the currency of the
        // reference account)
        return account != null && !DELIVERY.equals(account) ? account.getCurrencyCode()
                        : getReferenceAccountCurrencyCode();
    }
}
