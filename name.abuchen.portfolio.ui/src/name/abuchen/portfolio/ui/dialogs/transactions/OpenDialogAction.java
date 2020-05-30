package name.abuchen.portfolio.ui.dialogs.transactions;

import java.util.Objects;
import java.util.function.Consumer;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.ui.editor.AbstractFinanceView;

public class OpenDialogAction extends Action
{
    private AbstractFinanceView owner;

    private Class<? extends AbstractTransactionDialog> type;
    private Consumer<? extends AbstractTransactionDialog> prepare;

    private Object[] parameters;
    private Account account;
    private Portfolio portfolio;
    private Security security;

    public OpenDialogAction(AbstractFinanceView owner, String label)
    {
        super(label);
        this.owner = owner;
    }

    public OpenDialogAction type(Class<? extends AbstractTransactionDialog> type)
    {
        this.type = type;
        this.prepare = null;
        return this;
    }

    public <D extends AbstractTransactionDialog> OpenDialogAction type(Class<D> type, Consumer<D> prepare)
    {
        this.type = type;
        this.prepare = prepare;
        return this;
    }

    public OpenDialogAction parameters(Object... parameters)
    {
        this.parameters = parameters;
        return this;
    }

    public OpenDialogAction with(Account account)
    {
        this.account = account;
        return this;
    }

    public OpenDialogAction with(Portfolio portfolio)
    {
        this.portfolio = portfolio;
        return this;
    }

    public OpenDialogAction with(Security security)
    {
        this.security = security;
        return this;
    }

    public void addTo(IMenuManager manager)
    {
        manager.add(this);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void run()
    {
        Objects.requireNonNull(type);

        AbstractTransactionDialog dialog = owner.make(type, parameters);

        if (prepare != null)
            ((Consumer<AbstractTransactionDialog>) prepare).accept(dialog);

        if (account != null)
            dialog.setAccount(account);
        if (portfolio != null)
            dialog.setPortfolio(portfolio);
        if (security != null)
            dialog.setSecurity(security);

        dialog.open();

        if (dialog.hasAtLeastOneSuccessfulEdit())
        {
            owner.markDirty();
        }
    }
}
